/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.phenixcore.repositories.usermedia

import android.os.Handler
import android.os.Looper
import android.view.SurfaceView
import android.widget.ImageView
import com.phenixrts.common.RequestStatus
import com.phenixrts.express.PCastExpress
import com.phenixrts.media.video.android.AndroidVideoFrame
import com.phenixrts.pcast.FacingMode
import com.phenixrts.pcast.Renderer
import com.phenixrts.pcast.UserMediaStream
import com.phenixrts.pcast.android.AndroidReadVideoFrameCallback
import com.phenixrts.pcast.android.AndroidVideoRenderSurface
import com.phenixrts.suite.phenixcore.common.ConsumableSharedFlow
import com.phenixrts.suite.phenixcore.repositories.core.common.FAILURE_TIMEOUT
import com.phenixrts.suite.phenixcore.repositories.core.common.drawFrameBitmap
import com.phenixrts.suite.phenixcore.repositories.core.common.getUserMedia
import com.phenixrts.suite.phenixcore.repositories.core.common.getUserMediaOptions
import com.phenixrts.suite.phenixcore.repositories.core.common.prepareBitmap
import com.phenixrts.suite.phenixcore.repositories.core.common.rendererOptions
import com.phenixrts.suite.phenixcore.repositories.models.*
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

class UserMediaRepository(
    private val pCastExpress: PCastExpress,
    private val configuration: PhenixConfiguration,
    private val onMicrophoneFailure: () -> Unit,
    private val onCameraFailure: () -> Unit,
) {

    private val videoRenderSurface by lazy { AndroidVideoRenderSurface() }
    private var currentFacingMode = FacingMode.USER
    private var selfVideoRenderer: Renderer? = null
    private var userMediaStream: UserMediaStream? = null
    private var selfPreviewImageView: ImageView? = null
    private var selfPreviewConfiguration: PhenixFrameReadyConfiguration? = null
    private var isFirstFrameDrawn = false

    private val frameCallback = Renderer.FrameReadyForProcessingCallback { frameNotification ->
        frameNotification?.read(object : AndroidReadVideoFrameCallback() {
            override fun onVideoFrameEvent(videoFrame: AndroidVideoFrame?) {
                videoFrame?.bitmap?.prepareBitmap(selfPreviewConfiguration)?.let { bitmap ->
                    selfPreviewImageView?.drawFrameBitmap(bitmap, isFirstFrameDrawn) {
                        isFirstFrameDrawn = true
                    }
                }
            }
        })
    }

    private val microphoneFailureHandler = Handler(Looper.getMainLooper())
    private val cameraFailureHandler = Handler(Looper.getMainLooper())
    private val microphoneFailureRunnable = Runnable {
        Timber.d("Audio recording has stopped")
        onMicrophoneFailure()
    }
    private val videoFailureRunnable = Runnable {
        Timber.d("Video recording is stopped")
        onCameraFailure()
    }

    private val _onError = ConsumableSharedFlow<PhenixError>()
    private val _onEvent = ConsumableSharedFlow<PhenixEvent>()

    val onError = _onError.asSharedFlow()
    val onEvent = _onEvent.asSharedFlow()

    init {
        pCastExpress.getUserMedia { userMedia ->
            setUserMedia(userMedia)
            observeMediaState()
        }
    }

    fun flipCamera() {
        val facingMode = if (currentFacingMode == FacingMode.USER) FacingMode.ENVIRONMENT else FacingMode.USER
        updateUserMedia(PhenixPublishConfiguration(cameraFacingMode = facingMode)) { status, _ ->
            if (status == RequestStatus.OK) {
                currentFacingMode = facingMode
                _onEvent.tryEmit(PhenixEvent.CAMERA_FLIPPED)
            } else {
                _onError.tryEmit(PhenixError.CAMERA_FLIP_FAILED)
            }
        }
    }

    fun renderOnSurface(surfaceView: SurfaceView?) {
        Timber.d("Rendering user media on SurfaceView")
        videoRenderSurface.setSurfaceHolder(surfaceView?.holder)
    }

    fun renderOnImage(imageView: ImageView?, configuration: PhenixFrameReadyConfiguration?) {
        Timber.d("Rendering user media on ImageView")
        selfPreviewImageView = imageView
        selfPreviewConfiguration = configuration
        userMediaStream?.mediaStream?.videoTracks?.lastOrNull()?.let { videoTrack ->
            val callback = if (selfPreviewImageView == null) null else frameCallback
            if (callback == null) isFirstFrameDrawn = false
            selfVideoRenderer?.setFrameReadyCallback(videoTrack, callback)
        }
    }

    fun setSelfVideoEnabled(enabled: Boolean) {
        if (enabled) {
            if (selfVideoRenderer != null) return
            selfVideoRenderer = userMediaStream?.mediaStream?.createRenderer(rendererOptions)
            val status = selfVideoRenderer?.start(videoRenderSurface)
            selfVideoRenderer?.start()
            Timber.d("Self video started: $status")
        } else {
            if (selfVideoRenderer == null) return
            selfVideoRenderer?.stop()
            selfVideoRenderer = null
            Timber.d("Self video ended")
        }
    }

    fun updateUserMedia(
        publishConfiguration: PhenixPublishConfiguration,
        onUpdated: (RequestStatus, UserMediaStream) -> Unit
    ) {
        userMediaStream?.run {
            val optionStatus = applyOptions(getUserMediaOptions(publishConfiguration))
            Timber.d("Updated user media stream configuration: $optionStatus, $configuration")
            if (optionStatus != RequestStatus.OK) {
                userMediaStream?.dispose()
                userMediaStream = null
                Timber.d("Failed to update user media stream settings, requesting new user media object")
                pCastExpress.getUserMedia(getUserMediaOptions(publishConfiguration)) { status, userMedia ->
                    Timber.d("Collected new media stream from pCast: $status")
                    setUserMedia(userMedia)
                    onUpdated(status, userMedia)
                }
            } else {
                onUpdated(optionStatus, this)
            }
        }
    }

    fun release() {
        userMediaStream?.dispose()
        userMediaStream = null
        selfVideoRenderer = null
        selfPreviewImageView = null
        selfPreviewConfiguration = null
    }

    private fun setUserMedia(userMedia: UserMediaStream) {
        userMediaStream = userMedia
        val wasEnabled = selfVideoRenderer != null
        selfVideoRenderer?.dispose()
        selfVideoRenderer = null
        renderOnImage(selfPreviewImageView, selfPreviewConfiguration)
        setSelfVideoEnabled(wasEnabled)
        observeMediaState()
    }

    private fun observeMediaState() {
        userMediaStream?.run {
            mediaStream.videoTracks.firstOrNull()?.let { videoTrack ->
                setFrameReadyCallback(videoTrack) {
                    cameraFailureHandler.removeCallbacks(videoFailureRunnable)
                    cameraFailureHandler.postDelayed(videoFailureRunnable, FAILURE_TIMEOUT)
                }
            }
            mediaStream.audioTracks.firstOrNull()?.let { audioTrack ->
                setFrameReadyCallback(audioTrack) {
                    microphoneFailureHandler.removeCallbacks(microphoneFailureRunnable)
                    microphoneFailureHandler.postDelayed(microphoneFailureRunnable, FAILURE_TIMEOUT)
                }
            }
        }
    }
}

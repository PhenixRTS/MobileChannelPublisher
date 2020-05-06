/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.repositories

import android.app.Application
import android.view.SurfaceHolder
import androidx.lifecycle.MutableLiveData
import com.phenixrts.common.RequestStatus
import com.phenixrts.environment.android.AndroidContext
import com.phenixrts.express.*
import com.phenixrts.pcast.Renderer
import com.phenixrts.pcast.UserMediaStream
import com.phenixrts.pcast.android.AndroidVideoRenderSurface
import com.phenixrts.room.RoomService
import com.phenixrts.suite.channelpublisher.common.*
import com.phenixrts.suite.channelpublisher.common.enums.ExpressError
import com.phenixrts.suite.channelpublisher.common.enums.StreamStatus
import kotlinx.coroutines.delay
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

private const val REINITIALIZATION_DELAY = 1000L

class ChannelExpressRepository(private val context: Application) {

    private var channelExpress: ChannelExpress? = null
    private var userMediaStream: UserMediaStream? = null
    private var userMediaRenderer: Renderer? = null
    private var expressPublisher: ExpressPublisher? = null
    private var roomService: RoomService? = null
    private var currentConfiguration = ChannelConfiguration()
    private val androidVideoSurface by lazy { AndroidVideoRenderSurface() }

    val onChannelExpressError = MutableLiveData<ExpressError>()
    val onChannelState = MutableLiveData<StreamStatus>()

    private fun hasConfigurationChanged(configuration: ChannelConfiguration): Boolean = currentConfiguration != configuration

    private suspend fun initializeChannelExpress() {
        Timber.d("Creating Channel Express: $currentConfiguration")
        AndroidContext.setContext(context)
        val pcastExpressOptions = PCastExpressFactory.createPCastExpressOptionsBuilder()
            .withBackendUri(currentConfiguration.backend)
            .withPCastUri(currentConfiguration.uri)
            .withUnrecoverableErrorCallback { status: RequestStatus?, description: String ->
                Timber.e("Unrecoverable error in PhenixSDK. Error status: [$status]. Description: [$description]")
                onChannelExpressError.value = ExpressError.UNRECOVERABLE_ERROR
            }
            .withMinimumConsoleLogLevel("info")
            .buildPCastExpressOptions()

        val roomExpressOptions = RoomExpressFactory.createRoomExpressOptionsBuilder()
            .withPCastExpressOptions(pcastExpressOptions)
            .buildRoomExpressOptions()

        val channelExpressOptions = ChannelExpressFactory.createChannelExpressOptionsBuilder()
            .withRoomExpressOptions(roomExpressOptions)
            .buildChannelExpressOptions()

        ChannelExpressFactory.createChannelExpress(channelExpressOptions)?.let { express ->
            channelExpress = express
            val userMedia = express.pCastExpress.getUserMedia(getDefaultUserMediaOptions())
            Timber.d("Media stream collected from pCast")
            userMediaStream = userMedia
        }

        if (userMediaStream == null) {
            onChannelExpressError.value = ExpressError.UNRECOVERABLE_ERROR
        }
    }

    private suspend fun reinitializeUserMediaStream(configuration: PublishConfiguration): RequestStatus {
        Timber.d("Reinitializing User Media stream")
        // TODO: Disposing the old stream doesn't change a thing = the new required stream will not work for publisher later
        userMediaStream?.dispose()
        userMediaStream = null
        userMediaRenderer = null
        // TODO: The new user media stream is not working when used for publishing although the local preview works
        userMediaStream = channelExpress?.pCastExpress?.getUserMedia(getUserMediaOptions(configuration))
        Timber.d("Collected media stream from pCast: $userMediaStream")
        if (userMediaStream != null) {
            showPublisherPreview()
            return RequestStatus.OK
        }
        return RequestStatus.FAILED
    }

    private suspend fun updateUserMediaStream(configuration: PublishConfiguration): RequestStatus {
        var requestStatus: RequestStatus = RequestStatus.FAILED
        // TODO: Applying new options can return BAD_REQUEST and after that user media stream is re-required;
        //  The re-required user media stream is showing the preview, but it's not being streamed;
        //  Channel viewers see only black-screen until the channel times out.
        userMediaStream?.applyOptions(getUserMediaOptions(configuration))?.let { status ->
            Timber.d("Updated user media stream configuration: $status : $configuration")
            requestStatus = status
            if (status != RequestStatus.OK) {
                requestStatus = reinitializeUserMediaStream(configuration)
            }
        }
        return requestStatus
    }

    suspend fun setupChannelExpress(configuration: ChannelConfiguration) {
        if (hasConfigurationChanged(configuration)) {
            Timber.d("Channel Express configuration has changed: $configuration")
            currentConfiguration = configuration
            channelExpress?.dispose()
            channelExpress = null
            Timber.d("Channel Express disposed")
            delay(REINITIALIZATION_DELAY)
            initializeChannelExpress()
        }
    }

    suspend fun waitForPCast(): Unit = suspendCoroutine {
        launchMain {
            Timber.d("Waiting for pCast")
            if (channelExpress == null) {
                initializeChannelExpress()
            }
            channelExpress?.pCastExpress?.waitForOnline()
            it.resume(Unit)
        }
    }

    suspend fun publishToChannel(configuration: PublishConfiguration) = launchIO {
        if (userMediaStream == null || channelExpress == null) {
            launchMain {
                Timber.d("Repository not initialized properly")
                onChannelExpressError.value = ExpressError.UNRECOVERABLE_ERROR
            }
        }
        val requestStatus = updateUserMediaStream(configuration)
        if (requestStatus == RequestStatus.OK) {
            Timber.d("Publishing stream")
            channelExpress?.publishToChannel(getPublishToChannelOptions(configuration, userMediaStream!!))?.let { status ->
                    launchMain {
                        Timber.d("Stream is published: $status")
                        expressPublisher = status.publisher
                        roomService = status.roomService
                        onChannelState.value = status.streamStatus
                    }
                }
        } else launchMain {
            Timber.d("Publishing failed")
            onChannelState.value = StreamStatus.FAILED
        }
    }

    fun updateSurfaceHolder(surfaceHolder: SurfaceHolder) {
        Timber.d("Updating surface holder")
        androidVideoSurface.setSurfaceHolder(surfaceHolder)
    }

    fun isChannelExpressInitialized(): Boolean = channelExpress != null

    fun showPublisherPreview() {
        if (userMediaRenderer == null) {
            Timber.d("Creating media renderer")
            userMediaRenderer = userMediaStream?.mediaStream?.createRenderer()
            userMediaRenderer?.start(androidVideoSurface)
        }
        if (userMediaRenderer == null) {
            onChannelExpressError.value = ExpressError.UNRECOVERABLE_ERROR
        }
    }

    fun stopPublishing() {
        expressPublisher?.stop()
        expressPublisher = null
        roomService?.leaveRoom { _, status ->
            launchMain {
                Timber.d("Room left: $status")
                onChannelState.value = StreamStatus.DISCONNECTED
            }
        }
    }
}

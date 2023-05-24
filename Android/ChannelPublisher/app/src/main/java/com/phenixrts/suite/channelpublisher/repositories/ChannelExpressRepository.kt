/*
 * Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.repositories

import android.app.Application
import android.view.SurfaceHolder
import com.phenixrts.common.RequestStatus
import com.phenixrts.environment.android.AndroidContext
import com.phenixrts.express.*
import com.phenixrts.pcast.Renderer
import com.phenixrts.pcast.UserMediaStream
import com.phenixrts.pcast.android.AndroidVideoRenderSurface
import com.phenixrts.room.RoomService
import com.phenixrts.suite.channelpublisher.ChannelPublisherApplication
import com.phenixrts.suite.channelpublisher.common.*
import com.phenixrts.suite.channelpublisher.common.enums.ExpressError
import com.phenixrts.suite.channelpublisher.common.enums.StreamStatus
import com.phenixrts.suite.phenixcommon.common.FileWriterDebugTree
import com.phenixrts.suite.phenixcommon.common.launchIO
import com.phenixrts.suite.phenixdeeplinks.models.PhenixDeepLinkConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber
import javax.inject.Inject

private const val REINITIALIZATION_DELAY = 1000L

class ChannelExpressRepository(private val context: Application) {

    @Inject
    lateinit var fileWriterTree: FileWriterDebugTree

    private var channelExpress: ChannelExpress? = null
    private var userMediaStream: UserMediaStream? = null
    private var userMediaRenderer: Renderer? = null
    private var expressPublisher: ExpressPublisher? = null
    private var roomService: RoomService? = null
    private var expressConfiguration : PhenixDeepLinkConfiguration? = null
    private val androidVideoSurface by lazy { AndroidVideoRenderSurface() }

    private val _onError = MutableSharedFlow<ExpressError>(replay = 1)
    private val _onChannelState = MutableSharedFlow<StreamStatus>(replay = 1)

    val onError: SharedFlow<ExpressError> = _onError
    val onChannelState: SharedFlow<StreamStatus> = _onChannelState
    var roomExpress: RoomExpress? = null

    init {
        ChannelPublisherApplication.component.inject(this)
    }

    suspend fun setupChannelExpress(configuration: PhenixDeepLinkConfiguration) {
        if (hasConfigurationChanged(configuration)) {
            Timber.d("Channel Express configuration has changed: $configuration")
            expressConfiguration = configuration
            channelExpress?.run {
                dispose()
                Timber.d("Channel Express disposed")
            }
            channelExpress = null
            delay(REINITIALIZATION_DELAY)
            initializeChannelExpress()
        }
    }

    suspend fun waitForPCast() {
        Timber.d("Waiting for pCast")
        if (channelExpress == null) {
            initializeChannelExpress()
        }
        channelExpress?.pCastExpress?.waitForOnline()
    }

    suspend fun updatePublisherPreview(configuration: PublishConfiguration) {
        if (userMediaStream == null || channelExpress == null) {
            Timber.d("Repository not initialized properly")
            _onError.tryEmit(ExpressError.UNRECOVERABLE_ERROR)
        }

        if (tryCreateUserMediaStream(configuration) != RequestStatus.OK) {
            Timber.d("Failed to show preview")
            _onChannelState.tryEmit(StreamStatus.FAILED)
        }
    }

    suspend fun publishToChannel(configuration: PublishConfiguration) = launchIO {
        if (userMediaStream == null || channelExpress == null || expressConfiguration == null) {
            Timber.d("Repository not initialized properly")
            _onError.tryEmit(ExpressError.UNRECOVERABLE_ERROR)
            return@launchIO
        }

        Timber.d("Publishing stream")

        // Note: not all user media stream options can be changed.
        // For example, disabling or enabling audio or video cannot is not possible.
        // If applying options fails, create a new user media stream.
        // This avoids creating a new media stream when changing basic options like echo cancellation mode.
        Timber.d("Applying user media stream options")
        if (userMediaStream?.applyOptions(getUserMediaOptions(configuration)) != RequestStatus.OK) {
            Timber.d("Cannot user media stream options, creating new user media stream")
            if (tryCreateUserMediaStream(configuration) != RequestStatus.OK) {
                Timber.d("User media stream creation failed")
                _onChannelState.tryEmit(StreamStatus.FAILED)
                return@launchIO
            }
        }

        channelExpress?.publishToChannel(
            getPublishToChannelOptions(expressConfiguration!!, userMediaStream!!))?.let { status ->
            Timber.d("Stream is published with status [$status]")
            expressPublisher = status.publisher
            roomService = status.roomService
            _onChannelState.tryEmit(status.streamStatus)
        }
    }

    fun updateSurfaceHolder(surfaceHolder: SurfaceHolder) {
        Timber.d("Updating surface holder")
        androidVideoSurface.setSurfaceHolder(surfaceHolder)
    }

    fun isChannelExpressInitialized(): Boolean = channelExpress != null

    fun setSelfVideoEnabled(enabled: Boolean) {
        userMediaStream?.mediaStream?.videoTracks?.firstOrNull()?.isEnabled = enabled
    }

    fun showPublisherPreview() {
        if (userMediaRenderer == null) {
            Timber.d("Creating media renderer")
            userMediaRenderer = userMediaStream?.mediaStream?.createRenderer()
            userMediaRenderer?.start(androidVideoSurface)
        }
        if (userMediaRenderer == null) {
            _onError.tryEmit(ExpressError.UNRECOVERABLE_ERROR)
        }
    }

    fun stopPublishing() {
        expressPublisher?.stop()
        expressPublisher = null
        roomService?.leaveRoom { _, status ->
            Timber.d("Room left: $status")
            _onChannelState.tryEmit(StreamStatus.DISCONNECTED)
        }
    }

    private fun hasConfigurationChanged(configuration: PhenixDeepLinkConfiguration): Boolean = expressConfiguration != configuration

    private suspend fun initializeChannelExpress() {
        if (expressConfiguration == null) {
            Timber.d("Repository not initialized properly")
            _onError.tryEmit(ExpressError.UNRECOVERABLE_ERROR)
            return
        }

        Timber.d("Creating Channel Express: $expressConfiguration")
        AndroidContext.setContext(context)
        val pCastExpress = PCastExpressFactory.createPCastExpressOptionsBuilder { status: RequestStatus?, description: String ->
            Timber.e("Unrecoverable error in PhenixSDK. Error status [$status]. Error description [$description]")
            _onError.tryEmit(ExpressError.UNRECOVERABLE_ERROR)
        }
            .withMinimumConsoleLogLevel("debug")
            .withAuthenticationToken(expressConfiguration!!.authToken)
            .buildPCastExpressOptions()
        val roomExpressOptions = RoomExpressFactory.createRoomExpressOptionsBuilder()
            .withPCastExpressOptions(pCastExpress)
            .buildRoomExpressOptions()

        val channelExpressOptions = ChannelExpressFactory.createChannelExpressOptionsBuilder()
            .withRoomExpressOptions(roomExpressOptions)
            .buildChannelExpressOptions()

        ChannelExpressFactory.createChannelExpress(channelExpressOptions)?.let { express ->
            channelExpress = express
            roomExpress = express.roomExpress
            roomExpress?.pCastExpress?.pCast?.run {
                fileWriterTree.setLogCollectionMethod(this::collectLogs)
            }
            userMediaStream = express.pCastExpress.getUserMedia(getDefaultUserMediaOptions())
            Timber.d("Media stream collected from pCast")
        } ?: run {
            Timber.e("Unrecoverable error in PhenixSDK")
            _onError.tryEmit(ExpressError.UNRECOVERABLE_ERROR)
        }

        if (userMediaStream == null) {
            _onError.tryEmit(ExpressError.UNRECOVERABLE_ERROR)
        }
    }

    private suspend fun tryCreateUserMediaStream(publishConfiguration: PublishConfiguration): RequestStatus {
        var requestStatus: RequestStatus = RequestStatus.FAILED

        Timber.d("Creating user media stream: $publishConfiguration")

        if (userMediaStream != null) {
            userMediaRenderer?.dispose()
            userMediaRenderer = null

            userMediaStream?.dispose()
            userMediaStream = null
        }

        channelExpress?.pCastExpress?.getUserMedia(getUserMediaOptions(publishConfiguration))?.run {
            Timber.d("Created new UserMediaStream: $this")

            userMediaStream = this

            userMediaRenderer = userMediaStream?.mediaStream?.createRenderer()
            userMediaRenderer?.start(androidVideoSurface)

            requestStatus = RequestStatus.OK
        }

        return requestStatus
    }
}

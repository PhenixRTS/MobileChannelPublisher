/*
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
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
import com.phenixrts.suite.channelpublisher.common.*
import com.phenixrts.suite.channelpublisher.common.enums.ExpressError
import com.phenixrts.suite.channelpublisher.common.enums.StreamStatus
import com.phenixrts.suite.phenixcommon.common.launchIO
import com.phenixrts.suite.phenixdeeplink.models.PhenixDeepLinkConfiguration
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import timber.log.Timber

private const val REINITIALIZATION_DELAY = 1000L

class ChannelExpressRepository(private val context: Application) {

    private var channelExpress: ChannelExpress? = null
    private var userMediaStream: UserMediaStream? = null
    private var userMediaRenderer: Renderer? = null
    private var expressPublisher: ExpressPublisher? = null
    private var roomService: RoomService? = null
    private var expressConfiguration = PhenixDeepLinkConfiguration()
    private val androidVideoSurface by lazy { AndroidVideoRenderSurface() }

    private val _onError = MutableSharedFlow<ExpressError>(replay = 1)
    private val _onChannelState = MutableSharedFlow<StreamStatus>(replay = 1)

    val onError: SharedFlow<ExpressError> = _onError
    val onChannelState: SharedFlow<StreamStatus> = _onChannelState
    var roomExpress: RoomExpress? = null

    private fun hasConfigurationChanged(configuration: PhenixDeepLinkConfiguration): Boolean = expressConfiguration != configuration

    private suspend fun initializeChannelExpress() {
        Timber.d("Creating Channel Express: $expressConfiguration")
        AndroidContext.setContext(context)
        val pCastExpress = PCastExpressFactory.createPCastExpressOptionsBuilder()
            .withMinimumConsoleLogLevel("info")
            .withUnrecoverableErrorCallback { status: RequestStatus?, description: String ->
                Timber.e("Unrecoverable error in PhenixSDK. Error status: [$status]. Description: [$description]")
                _onError.tryEmit(ExpressError.UNRECOVERABLE_ERROR)
            }
            .withAuthenticationToken(expressConfiguration.edgeToken)
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

    suspend fun publishToChannel(configuration: PublishConfiguration) = launchIO {
        if (userMediaStream == null || channelExpress == null) {
            Timber.d("Repository not initialized properly")
            _onError.tryEmit(ExpressError.UNRECOVERABLE_ERROR)
        }
        val requestStatus = updateUserMediaStream(configuration)
        if (requestStatus == RequestStatus.OK) {
            Timber.d("Publishing stream")
            channelExpress?.publishToChannel(
                getPublishToChannelOptions(configuration, expressConfiguration, userMediaStream!!))?.let { status ->
                Timber.d("Stream is published: $status")
                expressPublisher = status.publisher
                roomService = status.roomService
                _onChannelState.tryEmit(status.streamStatus)
            }
        } else {
            Timber.d("Publishing failed")
            _onChannelState.tryEmit(StreamStatus.FAILED)
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
}

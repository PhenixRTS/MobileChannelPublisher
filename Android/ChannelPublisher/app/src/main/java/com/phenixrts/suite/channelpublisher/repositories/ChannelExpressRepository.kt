/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.repositories

import android.app.Application
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

    val onChannelExpressError = MutableLiveData<ExpressError>()
    val onChannelState = MutableLiveData<StreamStatus>()

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
            .buildPCastExpressOptions()

        val roomExpressOptions = RoomExpressFactory.createRoomExpressOptionsBuilder()
            .withPCastExpressOptions(pcastExpressOptions)
            .buildRoomExpressOptions()

        val channelExpressOptions = ChannelExpressFactory.createChannelExpressOptionsBuilder()
            .withRoomExpressOptions(roomExpressOptions)
            .buildChannelExpressOptions()

        ChannelExpressFactory.createChannelExpress(channelExpressOptions)?.let { express ->
            channelExpress = express
            val userMedia = express.pCastExpress.getUserMedia(getUserMediaOptions())
            Timber.d("Media stream collected from pCast")
            userMediaStream = userMedia
        }

        if (userMediaStream == null) {
            onChannelExpressError.value = ExpressError.UNRECOVERABLE_ERROR
        }
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

    suspend fun publishToChannel(configuration: PublishConfiguration) {
        if (userMediaStream == null || channelExpress == null) {
            Timber.d("Repository not initialized properly")
            onChannelExpressError.value = ExpressError.UNRECOVERABLE_ERROR
        }
        channelExpress?.publishToChannel(getPublishToChannelOptions(configuration, userMediaStream!!))?.let { status ->
            launchMain {
                Timber.d("Stream is published: $status")
                expressPublisher = status.publisher
                roomService = status.roomService
                onChannelState.value = status.streamStatus
            }
        }
    }

    private fun hasConfigurationChanged(configuration: ChannelConfiguration): Boolean = currentConfiguration != configuration

    fun isChannelExpressInitialized(): Boolean = channelExpress != null

    fun showPublisherPreview(surface: AndroidVideoRenderSurface) {
        if (userMediaRenderer == null) {
            userMediaRenderer = userMediaStream?.mediaStream?.createRenderer()
            userMediaRenderer?.start(surface)
        }
        if (userMediaRenderer == null) {
            onChannelExpressError.value = ExpressError.UNRECOVERABLE_ERROR
        }
    }

    fun stopPublishing() {
        // TODO: If called - stops User media preview until re-published again
        expressPublisher?.stop()
        expressPublisher = null
        roomService?.leaveRoom { _, status ->
            Timber.d("Room left: $status")
        }
    }
}

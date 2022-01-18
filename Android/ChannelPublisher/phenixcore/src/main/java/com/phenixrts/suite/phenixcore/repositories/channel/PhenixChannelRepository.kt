/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.phenixcore.repositories.channel

import android.view.SurfaceView
import android.widget.ImageView
import com.phenixrts.common.RequestStatus
import com.phenixrts.express.ChannelExpress
import com.phenixrts.express.ExpressPublisher
import com.phenixrts.pcast.UserMediaStream
import com.phenixrts.room.RoomService
import com.phenixrts.suite.phenixcore.common.ConsumableSharedFlow
import com.phenixrts.suite.phenixcore.common.asPhenixChannels
import com.phenixrts.suite.phenixcore.common.launchIO
import com.phenixrts.suite.phenixcore.repositories.channel.models.PhenixCoreChannel
import com.phenixrts.suite.phenixcore.repositories.core.common.getPublishToChannelOptions
import com.phenixrts.suite.phenixcore.repositories.models.*
import kotlinx.coroutines.flow.*
import timber.log.Timber

internal class PhenixChannelRepository(
    private val channelExpress: ChannelExpress,
    private val configuration: PhenixConfiguration
) {
    private val pCastExpress = channelExpress.roomExpress!!.pCastExpress
    private val rawChannels = mutableListOf<PhenixCoreChannel>()
    private var publisher: ExpressPublisher? = null
    private var roomService: RoomService? = null
    private var channelConfiguration: PhenixChannelConfiguration? = null

    private val _onError = ConsumableSharedFlow<PhenixError>()
    private val _onEvent = ConsumableSharedFlow<PhenixEvent>()
    private val _channels = ConsumableSharedFlow<List<PhenixChannel>>(canReplay = true)

    val channels = _channels.asSharedFlow()
    val onError = _onError.asSharedFlow()
    val onEvent = _onEvent.asSharedFlow()

    fun joinAllChannels(channelAliases: List<String>, streamIDs: List<String>) {
        if (configuration.channelTokens.isNotEmpty() && configuration.channelTokens.size != channelAliases.size) {
            _onError.tryEmit(PhenixError.JOIN_ROOM_FAILED)
            return
        }
        channelAliases.forEachIndexed { index, channelAlias ->
            if (rawChannels.any { it.channelAlias == channelAlias }) return
            val channel = PhenixCoreChannel(pCastExpress, channelExpress, configuration, channelAlias = channelAlias)
            Timber.d("Joining channel: $channelAlias")
            channel.join(
                PhenixChannelConfiguration(
                    channelAlias = channelAlias,
                    streamToken = configuration.channelTokens.getOrNull(index) ?: configuration.edgeToken,
                    publishToken = configuration.publishToken ?: configuration.edgeToken
                )
            )
            launchIO { channel.onUpdated.collect { _channels.tryEmit(rawChannels.asPhenixChannels()) } }
            launchIO { channel.onError.collect { _onError.tryEmit(it) } }
            rawChannels.add(channel)
        }
        streamIDs.forEachIndexed { index, streamID ->
            if (rawChannels.any { it.streamID == streamID }) return
            val channel = PhenixCoreChannel(pCastExpress, channelExpress, configuration, streamID = streamID)
            Timber.d("Joining channel: $streamID")
            channel.join(
                PhenixChannelConfiguration(
                    channelID = streamID,
                    streamToken = configuration.channelTokens.getOrNull(index) ?: configuration.edgeToken,
                    publishToken = configuration.publishToken ?: configuration.edgeToken
                )
            )
            launchIO { channel.onUpdated.collect { _channels.tryEmit(rawChannels.asPhenixChannels()) } }
            launchIO { channel.onError.collect { _onError.tryEmit(it) } }
            rawChannels.add(channel)
        }
        _channels.tryEmit(rawChannels.asPhenixChannels())
    }

    fun joinChannel(phenixChannelConfiguration: PhenixChannelConfiguration) {
        channelConfiguration = phenixChannelConfiguration
        val channelAlias = phenixChannelConfiguration.channelAlias
        if (rawChannels.any { it.channelAlias == channelAlias }) return
        val channel = PhenixCoreChannel(pCastExpress, channelExpress, configuration, channelAlias)
        channel.join(phenixChannelConfiguration)
        launchIO { channel.onUpdated.collect { _channels.tryEmit(rawChannels.asPhenixChannels()) } }
        launchIO { channel.onError.collect { _onError.tryEmit(it) } }
        rawChannels.add(channel)
        _channels.tryEmit(rawChannels.asPhenixChannels())
    }

    fun publishToChannel(phenixChannelConfiguration: PhenixChannelConfiguration, userMediaStream: UserMediaStream) {
        channelConfiguration = phenixChannelConfiguration
        Timber.d("Publishing to channel: $phenixChannelConfiguration")
        _onEvent.tryEmit(PhenixEvent.PHENIX_CHANNEL_PUBLISHING)
        channelExpress.publishToChannel(
            getPublishToChannelOptions(configuration, phenixChannelConfiguration, userMediaStream)
        ) { status: RequestStatus?, service: RoomService?, expressPublisher: ExpressPublisher? ->
            Timber.d("Stream is published: $status")
            publisher = expressPublisher
            roomService = service
            if (status == RequestStatus.OK && roomService != null && publisher != null) {
                _onEvent.tryEmit(PhenixEvent.PHENIX_CHANNEL_PUBLISHED.apply { data = phenixChannelConfiguration })
            } else {
                _onError.tryEmit(PhenixError.PUBLISH_CHANNEL_FAILED.apply { data = phenixChannelConfiguration })
            }
        }
    }

    fun stopPublishingToChannel() {
        Timber.d("Stopping media publishing")
        publisher?.stop()
        publisher = null
        _onEvent.tryEmit(PhenixEvent.PHENIX_CHANNEL_PUBLISH_ENDED.apply { data = channelConfiguration })
    }

    fun selectChannel(channelAlias: String, isSelected: Boolean) {
        rawChannels.find { it.channelAlias == channelAlias }?.selectChannel(isSelected)
    }

    fun renderOnSurface(channelAlias: String, surfaceView: SurfaceView?) {
        rawChannels.find { it.channelAlias == channelAlias }?.renderOnSurface(surfaceView)
    }

    fun renderOnImage(channelAlias: String, imageView: ImageView?, configuration: PhenixFrameReadyConfiguration?) {
        rawChannels.find { it.channelAlias == channelAlias }?.renderOnImage(imageView, configuration)
    }

    fun setAudioEnabled(channelAlias: String, enabled: Boolean) {
        rawChannels.find { it.channelAlias == channelAlias }?.setAudioEnabled(enabled)
    }

    fun createTimeShift(channelAlias: String, timestamp: Long) {
        rawChannels.find { it.channelAlias == channelAlias }?.createTimeShift(timestamp)
    }

    fun startTimeShift(channelAlias: String, duration: Long) {
        rawChannels.find { it.channelAlias == channelAlias }?.startTimeShift(duration)
    }

    fun seekTimeShift(channelAlias: String, offset: Long) {
        rawChannels.find { it.channelAlias == channelAlias }?.seekTimeShift(offset)
    }

    fun playTimeShift(channelAlias: String) {
        rawChannels.find { it.channelAlias == channelAlias }?.playTimeShift()
    }

    fun pauseTimeShift(channelAlias: String) {
        rawChannels.find { it.channelAlias == channelAlias }?.pauseTimeShift()
    }

    fun stopTimeShift(channelAlias: String) {
        rawChannels.find { it.channelAlias == channelAlias }?.stopTimeShift()
    }

    fun limitBandwidth(channelAlias: String, bandwidth: Long) {
        rawChannels.find { it.channelAlias == channelAlias }?.limitBandwidth(bandwidth)
    }

    fun releaseBandwidthLimiter(channelAlias: String) {
        rawChannels.find { it.channelAlias == channelAlias }?.releaseBandwidthLimiter()
    }

    fun subscribeForMessages(channelAlias: String) {
        rawChannels.find { it.channelAlias == channelAlias }?.subscribeForMessages()
    }

    fun release() {
        rawChannels.forEach { it.release() }
        rawChannels.clear()
    }

}

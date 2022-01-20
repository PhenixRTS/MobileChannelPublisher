/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.phenixcore.repositories.core

import android.content.Context
import android.view.SurfaceView
import android.widget.ImageView
import com.phenixrts.common.RequestStatus
import com.phenixrts.environment.android.AndroidContext
import com.phenixrts.express.*
import com.phenixrts.room.MemberRole
import com.phenixrts.room.MemberState
import com.phenixrts.suite.phenixcore.BuildConfig
import com.phenixrts.suite.phenixcore.common.ConsumableSharedFlow
import com.phenixrts.suite.phenixcore.common.FileWriterDebugTree
import com.phenixrts.suite.phenixcore.common.launchIO
import com.phenixrts.suite.phenixcore.repositories.channel.PhenixChannelRepository
import com.phenixrts.suite.phenixcore.repositories.core.common.PHENIX_LOG_LEVEL
import com.phenixrts.suite.phenixcore.repositories.core.models.PhenixCoreState
import com.phenixrts.suite.phenixcore.repositories.models.*
import com.phenixrts.suite.phenixcore.repositories.room.PhenixRoomRepository
import com.phenixrts.suite.phenixcore.repositories.usermedia.UserMediaRepository
import kotlinx.coroutines.flow.*
import timber.log.Timber

internal class PhenixCoreRepository(
    private val context: Context,
    private val debugTree: FileWriterDebugTree
) {

    private var configuration = PhenixConfiguration()
    private var roomExpress: RoomExpress? = null
    private var channelRepository: PhenixChannelRepository? = null
    private var roomRepository: PhenixRoomRepository? = null
    private var userMediaRepository: UserMediaRepository? = null

    private var _phenixState = PhenixCoreState.NOT_INITIALIZED
    private val _onError = ConsumableSharedFlow<PhenixError>()
    private val _onEvent = ConsumableSharedFlow<PhenixEvent>()
    private val _channels = ConsumableSharedFlow<List<PhenixChannel>>(canReplay = true)
    private val _messages = ConsumableSharedFlow<List<PhenixMessage>>(canReplay = true)
    private val _members = ConsumableSharedFlow<List<PhenixMember>>(canReplay = true)
    private val _room = ConsumableSharedFlow<PhenixRoom?>(canReplay = true)
    private var _memberCount = ConsumableSharedFlow<Long>(canReplay = true)

    val onError = _onError.asSharedFlow()
    val onEvent = _onEvent.asSharedFlow()
    val channels = _channels.asSharedFlow()
    val members = _members.asSharedFlow()
    val messages = _messages.asSharedFlow()
    val room = _room.asSharedFlow()
    val memberCount = _memberCount.asSharedFlow()

    val isPhenixInitializing get() = _phenixState == PhenixCoreState.INITIALIZING
    val isPhenixInitialized get() = _phenixState == PhenixCoreState.INITIALIZED

    fun init(phenixConfiguration: PhenixConfiguration?) {
        _phenixState = PhenixCoreState.INITIALIZING
        AndroidContext.setContext(context)
        if (BuildConfig.DEBUG) {
            Timber.plant(debugTree)
        }
        if (phenixConfiguration != null) {
            configuration = phenixConfiguration
        }
        Timber.d("Initializing Phenix Core with configuration: $configuration")
        AndroidContext.setContext(context)
        var pcastBuilder = PCastExpressFactory.createPCastExpressOptionsBuilder()
            .withMinimumConsoleLogLevel(PHENIX_LOG_LEVEL)
            .withPCastUri(configuration.uri)
            .withUnrecoverableErrorCallback { status: RequestStatus, description: String ->
                Timber.e("Failed to initialize Phenix Core: $status, $description with configuration: $configuration")
                _onError.tryEmit(PhenixError.FAILED_TO_INITIALIZE)
            }
        pcastBuilder = if (!configuration.authToken.isNullOrBlank()) {
            pcastBuilder.withAuthenticationToken(configuration.authToken)
        } else if (!configuration.edgeToken.isNullOrBlank()) {
            pcastBuilder.withAuthenticationToken(configuration.edgeToken)
        } else {
            pcastBuilder.withBackendUri(configuration.backend)
        }
        val roomExpressOptions = RoomExpressFactory.createRoomExpressOptionsBuilder()
            .withPCastExpressOptions(pcastBuilder.buildPCastExpressOptions())
            .buildRoomExpressOptions()

        val channelExpressOptions = ChannelExpressFactory.createChannelExpressOptionsBuilder()
            .withRoomExpressOptions(roomExpressOptions)
            .buildChannelExpressOptions()

        ChannelExpressFactory.createChannelExpress(channelExpressOptions)?.let { express ->
            express.roomExpress?.pCastExpress?.waitForOnline {
                Timber.d("Phenix Core initialized")
                roomExpress = express.roomExpress
                channelRepository = PhenixChannelRepository(express, configuration)
                roomRepository = PhenixRoomRepository(express.roomExpress, configuration)
                userMediaRepository = UserMediaRepository(
                    express.pCastExpress,
                    configuration,
                    onMicrophoneFailure = {
                        roomRepository?.setSelfAudioEnabled(false)
                    },
                    onCameraFailure = {
                        roomRepository?.setSelfVideoEnabled(false)
                    }
                )
                _phenixState = PhenixCoreState.INITIALIZED
                _onEvent.tryEmit(PhenixEvent.PHENIX_CORE_INITIALIZED)

                launchIO { userMediaRepository?.onError?.collect { _onError.tryEmit(it) } }
                launchIO { userMediaRepository?.onEvent?.collect { _onEvent.tryEmit(it) } }

                launchIO { channelRepository?.onError?.collect { _onError.tryEmit(it) } }
                launchIO { channelRepository?.onEvent?.collect { _onEvent.tryEmit(it) } }
                launchIO { channelRepository?.channels?.collect { _channels.tryEmit(it) } }

                launchIO { roomRepository?.onError?.collect { _onError.tryEmit(it) } }
                launchIO { roomRepository?.onEvent?.collect { _onEvent.tryEmit(it) } }
                launchIO { roomRepository?.members?.collect { _members.tryEmit(it) } }
                launchIO { roomRepository?.messages?.collect { _messages.tryEmit(it) } }
                launchIO { roomRepository?.room?.collect { _room.tryEmit(it) } }
                launchIO { roomRepository?.memberCount?.collect { _memberCount.tryEmit(it) } }
            }
        } ?: run {
            Timber.e("Failed to initialize Phenix Core")
            _phenixState = PhenixCoreState.NOT_INITIALIZED
            _onError.tryEmit(PhenixError.FAILED_TO_INITIALIZE)
        }
    }

    fun joinAllChannels(channelAliases: List<String>, streamIDs: List<String>) =
        channelRepository?.joinAllChannels(channelAliases, streamIDs)

    fun joinChannel(configuration: PhenixChannelConfiguration) =
        channelRepository?.joinChannel(configuration)

    fun publishToChannel(configuration: PhenixChannelConfiguration, publishConfiguration: PhenixPublishConfiguration) {
        userMediaRepository?.updateUserMedia(publishConfiguration) { status, userMedia ->
            if (status == RequestStatus.OK) {
                channelRepository?.publishToChannel(configuration, userMedia)
            } else {
                _onError.tryEmit(PhenixError.PUBLISH_CHANNEL_FAILED.apply { data = configuration })
            }
        }
    }

    fun stopPublishingToChannel() = channelRepository?.stopPublishingToChannel()

    fun selectChannel(alias: String, isSelected: Boolean) =
        channelRepository?.selectChannel(alias, isSelected)

    fun flipCamera() = userMediaRepository?.flipCamera()

    fun renderOnSurface(alias: String, surfaceView: SurfaceView?) {
        userMediaRepository?.renderOnSurface(surfaceView)
        channelRepository?.renderOnSurface(alias, surfaceView)
        roomRepository?.renderOnSurface(alias, surfaceView)
    }

    fun renderOnImage(alias: String, imageView: ImageView?, configuration: PhenixFrameReadyConfiguration?) {
        channelRepository?.renderOnImage(alias, imageView, configuration)
        roomRepository?.renderOnImage(alias, imageView, configuration)
    }

    fun previewOnSurface(surfaceView: SurfaceView?) =
        userMediaRepository?.renderOnSurface(surfaceView)

    fun previewOnImage(imageView: ImageView?, configuration: PhenixFrameReadyConfiguration?) =
        userMediaRepository?.renderOnImage(imageView, configuration)

    fun createTimeShift(alias: String, timestamp: Long) =
        channelRepository?.createTimeShift(alias, timestamp)

    fun seekTimeShift(alias: String, offset: Long) =
        channelRepository?.seekTimeShift(alias, offset)

    fun playTimeShift(alias: String) =
        channelRepository?.playTimeShift(alias)

    fun startTimeShift(alias: String, duration: Long) =
        channelRepository?.startTimeShift(alias, duration)

    fun pauseTimeShift(alias: String) =
        channelRepository?.pauseTimeShift(alias)

    fun stopTimeShift(alias: String) =
        channelRepository?.stopTimeShift(alias)

    fun limitBandwidth(alias: String, bandwidth: Long) =
        channelRepository?.limitBandwidth(alias, bandwidth)

    fun releaseBandwidthLimiter(alias: String) =
        channelRepository?.releaseBandwidthLimiter(alias)

    fun subscribeForMessages(alias: String) =
        channelRepository?.subscribeForMessages(alias)

    fun setAudioEnabled(alias: String, enabled: Boolean) {
        channelRepository?.setAudioEnabled(alias, enabled)
        roomRepository?.setAudioEnabled(alias, enabled)
    }

    fun setSelfAudioEnabled(enabled: Boolean) =
        roomRepository?.setSelfAudioEnabled(enabled)

    fun setVideoEnabled(alias: String, enabled: Boolean) =
        roomRepository?.setVideoEnabled(alias, enabled)

    fun setSelfVideoEnabled(enabled: Boolean) {
        roomRepository?.setSelfVideoEnabled(enabled)
        userMediaRepository?.setSelfVideoEnabled(enabled)
    }

    fun joinRoom(configuration: PhenixRoomConfiguration) =
        roomRepository?.joinRoom(configuration)

    fun createRoom(configuration: PhenixRoomConfiguration) =
        roomRepository?.createRoom(configuration)

    fun stopPublishingToRoom() = roomRepository?.stopPublishingToRoom()

    fun leaveRoom() = roomRepository?.leaveRoom()

    fun updateMember(memberId: String, role: MemberRole?, active: MemberState?, name: String?) =
        roomRepository?.updateMember(memberId, role, active, name)

    fun setAudioLevel(memberId: String, level: Float) =
        roomRepository?.setAudioLevel(memberId, level)

    fun sendMessage(message: String, mimeType: String) =
        roomRepository?.sendMessage(message, mimeType)

    fun selectMember(memberId: String, isSelected: Boolean) =
        roomRepository?.selectMember(memberId, isSelected)

    fun publishToRoom(configuration: PhenixRoomConfiguration, publishConfiguration: PhenixPublishConfiguration) {
        userMediaRepository?.updateUserMedia(publishConfiguration) { status, userMedia ->
            if (status == RequestStatus.OK) {
                roomRepository?.publishToRoom(configuration, userMedia)
            } else {
                _onError.tryEmit(PhenixError.PUBLISH_ROOM_FAILED.apply { data = configuration })
            }
        }
    }

    fun subscribeToRoom() = roomRepository?.subscribeRoomMembers()

    fun collectLogs(onCollected: (String) -> Unit) {
        roomExpress?.pCastExpress?.pCast?.collectLogMessages { _, _, messages ->
            Timber.d("SDK logs collected")
            onCollected(messages)
        }
    }

    fun release() {
        roomExpress?.dispose()
        channelRepository?.release()
        roomRepository?.release()
        userMediaRepository?.release()

        configuration = PhenixConfiguration()
        roomExpress = null
        channelRepository = null
        roomRepository = null
        userMediaRepository = null
        _phenixState = PhenixCoreState.NOT_INITIALIZED
    }

}

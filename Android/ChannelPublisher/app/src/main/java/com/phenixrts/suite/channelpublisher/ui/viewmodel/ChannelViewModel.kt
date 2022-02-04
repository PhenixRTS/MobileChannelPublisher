/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui.viewmodel

import android.view.SurfaceView
import androidx.lifecycle.ViewModel
import com.phenixrts.suite.channelpublisher.BuildConfig
import com.phenixrts.suite.phenixcore.PhenixCore
import com.phenixrts.suite.phenixcore.common.launch
import com.phenixrts.suite.phenixcore.repositories.models.PhenixChannelConfiguration
import com.phenixrts.suite.phenixcore.repositories.models.PhenixEvent
import com.phenixrts.suite.phenixcore.repositories.models.PhenixMediaState
import com.phenixrts.suite.phenixcore.repositories.models.PhenixPublishConfiguration
import com.phenixrts.suite.phenixdebugmenu.DebugMenu
import com.phenixrts.suite.phenixdebugmenu.models.DebugEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import timber.log.Timber

class ChannelViewModel(private val phenixCore: PhenixCore) : ViewModel() {

    private val _onEvent = MutableSharedFlow<PhenixEvent>(replay = 1)
    private val _mediaState = MutableSharedFlow<PhenixMediaState>(replay = 1)

    val onError = phenixCore.onError
    val onEvent = _onEvent.asSharedFlow()
    val mediaState = _mediaState.asSharedFlow()

    init {
        launch {
            phenixCore.onEvent.collect { event ->
                _onEvent.tryEmit(event)
            }
        }

        launch {
            phenixCore.mediaState.collect { state ->
                Timber.d("Media state updated: $state")
                _mediaState.tryEmit(state)
            }
        }
    }

    fun showPublisherPreview(surfaceView: SurfaceView) {
        Timber.d("Showing preview")
        phenixCore.setSelfVideoEnabled(true)
        phenixCore.previewOnSurface(surfaceView)
    }

    fun publishToChannel(configuration: PhenixPublishConfiguration) {
        Timber.d("Publishing to channel: $configuration")
        val channelAlias = phenixCore.configuration!!.selectedAlias?.takeIf { it.isNotBlank() }
            ?: phenixCore.configuration!!.channelAliases.first()
        val streamToken = phenixCore.configuration!!.channelStreamTokens.firstOrNull()
        val publishToken = phenixCore.configuration!!.publishToken
        phenixCore.publishToChannel(
            configuration = PhenixChannelConfiguration(
                channelAlias = channelAlias,
                streamToken = streamToken,
                publishToken = publishToken
            ),
            publishConfiguration = configuration.copy(
                isAudioEnabled = _mediaState.replayCache.lastOrNull()?.isAudioEnabled ?: configuration.isAudioEnabled,
                isVideoEnabled = _mediaState.replayCache.lastOrNull()?.isVideoEnabled ?: configuration.isVideoEnabled
            )
        )
    }

    fun stopPublishing() = phenixCore.stopPublishingToChannel()

    fun observeDebugMenu(debugMenu: DebugMenu, onError: (String) -> Unit, onEvent: (DebugEvent) -> Unit) {
        debugMenu.observeDebugMenu(
            phenixCore,
            "${BuildConfig.APPLICATION_ID}.provider",
            onError = onError,
            onEvent = onEvent
        )
    }
}

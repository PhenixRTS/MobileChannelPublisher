/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui.viewmodel

import android.view.SurfaceView
import androidx.lifecycle.ViewModel
import com.phenixrts.suite.channelpublisher.BuildConfig
import com.phenixrts.suite.phenixcore.PhenixCore
import com.phenixrts.suite.phenixcore.repositories.models.PhenixChannelConfiguration
import com.phenixrts.suite.phenixcore.repositories.models.PhenixPublishConfiguration
import com.phenixrts.suite.phenixdebugmenu.DebugMenu
import timber.log.Timber

class ChannelViewModel(private val phenixCore: PhenixCore) : ViewModel() {

    val onError = phenixCore.onError
    val onEvent = phenixCore.onEvent

    fun showPublisherPreview(surfaceView: SurfaceView) {
        Timber.d("Showing preview")
        phenixCore.setSelfVideoEnabled(true)
        phenixCore.previewOnSurface(surfaceView)
    }

    fun publishToChannel(capabilities: List<String>, configuration: PhenixPublishConfiguration) {
        Timber.d("Publishing to channel: $configuration")
        val channelAlias = phenixCore.configuration!!.selectedAlias?.takeIf { it.isNotBlank() }
            ?: phenixCore.configuration!!.channelAliases.first()
        val streamToken = phenixCore.configuration!!.channelTokens.firstOrNull()
        val publishToken = phenixCore.configuration!!.publishToken
        phenixCore.publishToChannel(
            configuration = PhenixChannelConfiguration(
                channelAlias = channelAlias,
                streamToken = streamToken,
                publishToken = publishToken,
                channelCapabilities = capabilities
            ),
            publishConfiguration = PhenixPublishConfiguration(
                cameraFacingMode = configuration.cameraFacingMode,
                cameraFps = configuration.cameraFps,
                echoCancellationMode = configuration.echoCancellationMode,
                microphoneEnabled = configuration.microphoneEnabled
            )
        )
    }

    fun stopPublishing() = phenixCore.stopPublishingToChannel()

    fun observeDebugMenu(debugMenu: DebugMenu, onError: () -> Unit, onShow: () -> Unit) {
        debugMenu.observeDebugMenu(
            phenixCore,
            "${BuildConfig.APPLICATION_ID}.provider",
            onError = onError,
            onShow = onShow
        )
    }
}

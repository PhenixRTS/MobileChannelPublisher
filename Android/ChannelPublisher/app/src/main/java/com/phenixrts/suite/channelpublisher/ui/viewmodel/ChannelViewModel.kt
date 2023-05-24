/*
 * Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui.viewmodel

import android.view.SurfaceHolder
import androidx.lifecycle.ViewModel
import com.phenixrts.pcast.FacingMode
import com.phenixrts.suite.channelpublisher.common.PublishConfiguration
import com.phenixrts.suite.channelpublisher.repositories.ChannelExpressRepository
import com.phenixrts.suite.phenixcommon.common.launchMain
import timber.log.Timber

class ChannelViewModel(private val channelExpressRepository: ChannelExpressRepository) : ViewModel() {

    val onChannelExpressError = channelExpressRepository.onError
    val onChannelState = channelExpressRepository.onChannelState

    fun showPublisherPreview(surfaceHolder: SurfaceHolder) {
        Timber.d("Showing preview")
        channelExpressRepository.showPublisherPreview()
        setSelfVideoEnabled(true)
        channelExpressRepository.updateSurfaceHolder(surfaceHolder)
    }

    fun setSelfVideoEnabled(enabled: Boolean) {
        channelExpressRepository.setSelfVideoEnabled(enabled)
    }

    fun updatePublisherPreview(publishConfiguration: PublishConfiguration) = launchMain {
        channelExpressRepository.updatePublisherPreview(publishConfiguration)
    }

    fun publishToChannel(configuration: PublishConfiguration) = launchMain {
        Timber.d("Publishing to channel: $configuration")
        channelExpressRepository.publishToChannel(configuration)
    }

    fun stopPublishing() = channelExpressRepository.stopPublishing()
}

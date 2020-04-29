/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.Observer
import com.phenixrts.suite.channelpublisher.ChannelPublisherApplication
import com.phenixrts.suite.channelpublisher.R
import com.phenixrts.suite.channelpublisher.common.*
import com.phenixrts.suite.channelpublisher.common.enums.ExpressError
import com.phenixrts.suite.channelpublisher.common.enums.StreamStatus
import com.phenixrts.suite.channelpublisher.repositories.ChannelExpressRepository
import com.phenixrts.suite.channelpublisher.ui.viewmodel.ChannelViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.view_configuration.*
import timber.log.Timber
import javax.inject.Inject

const val EXTRA_DEEP_LINK_MODEL = "ExtraDeepLinkModel"

class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var channelExpressRepository: ChannelExpressRepository
    private val viewModel by lazyViewModel { ChannelViewModel(channelExpressRepository) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChannelPublisherApplication.component.inject(this)
        setContentView(R.layout.activity_main)

        viewModel.onChannelExpressError.observe(this, Observer { error ->
            Timber.d("Channel Express failed: $error")
            showErrorDialog(error)
        })
        viewModel.onChannelState.observe(this, Observer { status ->
            Timber.d("Stream state changed: $status")
            hideLoading()
            if (status == StreamStatus.CONNECTED) {
                hideConfigurationOverlay()
            } else {
                showConfigurationOverlay()
            }
            if (status == StreamStatus.FAILED) {
                Timber.d("Stream failed")
                showErrorDialog(ExpressError.STREAM_ERROR)
            }
        })

        publish_button.setOnClickListener {
            Timber.d("Publish button clicked")
            showLoading()
            viewModel.publishToChannel(getPublishConfiguration(), channel_surface.holder)
        }

        end_stream_button.setOnClickListener {
            Timber.d("End publish button clicked")
            showConfigurationOverlay()
            viewModel.stopPublishing()
            viewModel.showPublisherPreview(channel_surface.holder)
        }

        checkDeepLink(intent)
        viewModel.showPublisherPreview(channel_surface.holder)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("On new intent $intent")
        checkDeepLink(intent)
    }

    private fun showLoading() {
        loading_progress_bar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        loading_progress_bar.visibility = View.GONE
    }

    private fun showConfigurationOverlay() {
        configuration.visibility = View.VISIBLE
        end_stream_button.visibility = View.GONE
    }

    private fun hideConfigurationOverlay() {
        configuration.visibility = View.GONE
        end_stream_button.visibility = View.VISIBLE
    }

    private fun checkDeepLink(intent: Intent?) = launchMain {
        intent?.let { intent ->
            if (intent.hasExtra(EXTRA_DEEP_LINK_MODEL)) {
                (intent.getStringExtra(EXTRA_DEEP_LINK_MODEL))?.let { channelAlias ->
                    Timber.d("Received channel code: $channelAlias")
                    viewModel.channelAlias = channelAlias
                }
                intent.removeExtra(EXTRA_DEEP_LINK_MODEL)
                return@launchMain
            }
        }
    }

    private fun getPublishConfiguration(): PublishConfiguration {
        val cameraFacing = spinner_camera_facing.getCameraFacing()
        val cameraFps = spinner_camera_fps.getCameraFps()
        val streamQuality = spinner_camera_resolution.getStreamQuality()
        val capabilities = spinner_camera_mbr.getCapabilities().plus(streamQuality)
        val echoCancellationMode = spinner_echo_cancellation.getEchoCancellation()
        val microphoneEnabled = spinner_microphone.getMicrophoneEnabled()
        return PublishConfiguration(viewModel.channelAlias, cameraFacing, cameraFps, microphoneEnabled,
            echoCancellationMode, capabilities)
    }

}

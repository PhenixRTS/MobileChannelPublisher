/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.phenixrts.suite.channelpublisher.BuildConfig
import com.phenixrts.suite.channelpublisher.ChannelPublisherApplication
import com.phenixrts.suite.channelpublisher.R
import com.phenixrts.suite.channelpublisher.common.*
import com.phenixrts.suite.channelpublisher.databinding.ActivityMainBinding
import com.phenixrts.suite.channelpublisher.ui.viewmodel.ChannelViewModel
import com.phenixrts.suite.phenixcore.common.launchUI
import com.phenixrts.suite.phenixcore.PhenixCore
import com.phenixrts.suite.phenixcore.repositories.models.PhenixEvent
import com.phenixrts.suite.phenixcore.repositories.models.PhenixPublishConfiguration
import com.phenixrts.suite.phenixdebugmenu.models.DebugEvent
import timber.log.Timber
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject lateinit var phenixCore: PhenixCore

    private lateinit var binding: ActivityMainBinding

    private val viewModel: ChannelViewModel by lazyViewModel(
        { application as ChannelPublisherApplication },
        { ChannelViewModel(phenixCore) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChannelPublisherApplication.component.inject(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeDropDowns()

        launchUI {
            viewModel.onError.collect { error ->
                Timber.d("Channel Publisher failed: $error")
                hideLoading()
                binding.root.showSnackBar(error.message, Snackbar.LENGTH_LONG)
            }
        }
        launchUI {
            viewModel.onEvent.collect { event ->
                updateState(event)
            }
        }

        binding.configuration.publishButton.setOnClickListener {
            Timber.d("Publish button clicked")
            showLoading()
            viewModel.publishToChannel(
                capabilities = getCapabilities().plus(getStreamQuality()),
                configuration = getPublishConfiguration()
            )
        }

        binding.endStreamButton.setOnClickListener {
            Timber.d("End publish button clicked")
            viewModel.stopPublishing()
        }

        viewModel.observeDebugMenu(
            binding.debugMenu,
            onError = { error ->
                binding.root.showSnackBar(error, Snackbar.LENGTH_LONG)
            },
            onEvent = { event ->
                when (event) {
                    DebugEvent.SHOW_MENU -> binding.debugMenu.showAppChooser(this@MainActivity)
                    DebugEvent.FILES_DELETED -> binding.root.showSnackBar(getString(R.string.files_deleted), Snackbar.LENGTH_LONG)
                }
            }
        )
        binding.debugMenu.onStart(getString(R.string.debug_app_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        ), getString(R.string.debug_sdk_version,
            com.phenixrts.sdk.BuildConfig.VERSION_NAME,
            com.phenixrts.sdk.BuildConfig.VERSION_CODE
        ))

        viewModel.showPublisherPreview(binding.channelSurface)
        viewModel.onEvent.replayCache.lastOrNull()?.let { event ->
            updateState(event)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.debugMenu.onStop()
    }

    override fun onBackPressed() {
        if (binding.debugMenu.isOpened()) {
            binding.debugMenu.hide()
        } else {
            super.onBackPressed()
        }
    }

    private fun initializeDropDowns() {
        Timber.d("Initialize drop downs")
        binding.configuration.spinnerCameraFacing.setSelection(selectedCameraFacing)
        binding.configuration.spinnerCameraFps.setSelection(selectedFpsOption)
        binding.configuration.spinnerCameraMbr.setSelection(selectedMbrOption)
        binding.configuration.spinnerCameraQuality.setSelection(selectedQualityOption)
        binding.configuration.spinnerEchoCancellation.setSelection(selectedAecOption)
        binding.configuration.spinnerMicrophone.setSelection(selectedMicrophoneOption)

        binding.configuration.spinnerCameraFacing.onSelectionChanged { index ->
            Timber.d("Camera facing selected: $index")
            selectedCameraFacing = index
        }

        binding.configuration.spinnerCameraFps.onSelectionChanged { index ->
            Timber.d("Camera FPS selected: $index")
            selectedFpsOption = index
        }

        binding.configuration.spinnerCameraMbr.onSelectionChanged { index ->
            Timber.d("Camera MBR selected: $index")
            selectedMbrOption = index
        }

        binding.configuration.spinnerCameraQuality.onSelectionChanged { index ->
            Timber.d("Camera quality selected: $index")
            selectedQualityOption = index
        }

        binding.configuration.spinnerEchoCancellation.onSelectionChanged { index ->
            Timber.d("Camera echo cancellation selected: $index")
            selectedAecOption = index
        }

        binding.configuration.spinnerMicrophone.onSelectionChanged { index ->
            Timber.d("Camera microphone state selected: $index")
            selectedMicrophoneOption = index
        }
    }

    private fun updateState(event: PhenixEvent) {
        Timber.d("Channel Publisher event: $event")
        when (event) {
            PhenixEvent.PHENIX_CHANNEL_PUBLISHING -> showLoading()
            PhenixEvent.PHENIX_CHANNEL_PUBLISHED -> {
                hideLoading()
                hideConfigurationOverlay()
            }
            PhenixEvent.PHENIX_CHANNEL_PUBLISH_ENDED -> showConfigurationOverlay()
            else -> { /* Ignored */ }
        }
    }

    private fun showLoading() {
        binding.loadingProgressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        binding.loadingProgressBar.visibility = View.GONE
    }

    private fun showConfigurationOverlay() {
        binding.configuration.root.visibility = View.VISIBLE
        binding.endStreamButton.visibility = View.GONE
    }

    private fun hideConfigurationOverlay() {
        binding.configuration.root.visibility = View.GONE
        binding.endStreamButton.visibility = View.VISIBLE
    }

    private fun getPublishConfiguration() = PhenixPublishConfiguration(
        cameraFacingMode = getCameraFacing(),
        cameraFps = getCameraFps().toDouble(),
        microphoneEnabled = getMicrophoneEnabled(),
        echoCancellationMode = getEchoCancellation()
    )

}

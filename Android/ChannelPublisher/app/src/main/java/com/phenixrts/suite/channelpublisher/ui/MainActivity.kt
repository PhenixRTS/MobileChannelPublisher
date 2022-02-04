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
        launchUI {
            viewModel.mediaState.collect { state ->
                switchPreview(state.isVideoEnabled)
            }
        }

        binding.configuration.publishButton.setOnClickListener {
            Timber.d("Publish button clicked")
            showLoading()
            viewModel.publishToChannel(configuration = getPublishConfiguration())
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
        binding.debugMenu.onStart(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString())

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
        binding.configuration.spinnerEchoCancellation.setSelection(selectedAecOption)
        binding.configuration.spinnerMicrophone.setSelection(selectedMicrophoneOption)

        binding.configuration.spinnerCameraFacing.onSelectionChanged { index ->
            Timber.d("Camera facing selected: $index")
            if (selectedCameraFacing != index) {
                selectedCameraFacing = index
                if (index == CAMERA_OFF_INDEX) {
                    Timber.d("Disabling camera: $index")
                    phenixCore.setSelfVideoEnabled(false)
                } else {
                    Timber.d("Flipping camera: $index")
                    phenixCore.setSelfVideoEnabled(true)
                    phenixCore.setCameraFacing(getCameraFacing())
                }
            }
        }

        binding.configuration.spinnerCameraFps.onSelectionChanged { index ->
            Timber.d("Camera FPS selected: $index")
            selectedFpsOption = index
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

    private fun switchPreview(enabled: Boolean) {
        binding.channelSurface.visibility = if (enabled) View.VISIBLE else View.GONE
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
        isAudioEnabled = getMicrophoneEnabled(),
        isVideoEnabled = selectedCameraFacing != CAMERA_OFF_INDEX,
        echoCancellationMode = getEchoCancellation()
    )

}

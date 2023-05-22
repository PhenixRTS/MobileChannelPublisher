/*
 * Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.phenixrts.pcast.FacingMode
import com.phenixrts.suite.channelpublisher.BuildConfig
import com.phenixrts.suite.channelpublisher.ChannelPublisherApplication
import com.phenixrts.suite.channelpublisher.R
import com.phenixrts.suite.channelpublisher.common.*
import com.phenixrts.suite.channelpublisher.common.enums.ExpressError
import com.phenixrts.suite.channelpublisher.common.enums.StreamStatus
import com.phenixrts.suite.channelpublisher.databinding.ActivityMainBinding
import com.phenixrts.suite.channelpublisher.repositories.ChannelExpressRepository
import com.phenixrts.suite.channelpublisher.ui.viewmodel.ChannelViewModel
import com.phenixrts.suite.phenixcommon.common.FileWriterDebugTree
import com.phenixrts.suite.phenixcommon.common.launchMain
import com.phenixrts.suite.phenixdebugmenu.models.DebugEvent
import timber.log.Timber
import javax.inject.Inject

class MainActivity : AppCompatActivity() {

    @Inject lateinit var channelExpressRepository: ChannelExpressRepository
    @Inject lateinit var fileWriterTree: FileWriterDebugTree
    private lateinit var binding: ActivityMainBinding

    private val viewModel: ChannelViewModel by lazyViewModel(
        { application as ChannelPublisherApplication },
        { ChannelViewModel(channelExpressRepository) }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChannelPublisherApplication.component.inject(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeDropDowns()

        launchMain {
            viewModel.onChannelExpressError.collect { error ->
                Timber.d("Channel Express failed: $error")
                showErrorDialog(error)
            }
        }

        launchMain {
            viewModel.onChannelState.collect { status ->
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
            }
        }

        binding.configuration.publishButton.setOnClickListener {
            Timber.d("Publish button clicked")
            showLoading()
            viewModel.publishToChannel(getPublishConfiguration())
        }

        binding.endStreamButton.setOnClickListener {
            Timber.d("End publish button clicked")
            viewModel.stopPublishing()
        }

        viewModel.showPublisherPreview(binding.channelSurface.holder)

        binding.debugMenu.onStart(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE.toString())
        binding.debugMenu.observeDebugMenu(
            fileWriterTree,
            "${BuildConfig.APPLICATION_ID}.provider",
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

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.debugMenu.isOpened()) {
                    binding.debugMenu.hide()
                } else {
                    finish()
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.debugMenu.onStop()
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

                val publishConfiguration = getPublishConfiguration()
                viewModel.updatePublisherPreview(publishConfiguration)

                if (publishConfiguration.cameraFacingMode == FacingMode.UNDEFINED) {
                    binding.channelSurface.visibility = View.GONE
                    viewModel.setSelfVideoEnabled(false)
                } else {
                    viewModel.setSelfVideoEnabled(true)
                    binding.channelSurface.visibility = View.VISIBLE
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

    private fun getPublishConfiguration(): PublishConfiguration {
        val cameraFacing = getCameraFacing()
        val cameraFps = getCameraFps()
        val echoCancellationMode = getEchoCancellation()
        val microphoneEnabled = getMicrophoneEnabled()
        return PublishConfiguration(viewModel.channelAlias, cameraFacing, cameraFps, microphoneEnabled, echoCancellationMode)
    }

}

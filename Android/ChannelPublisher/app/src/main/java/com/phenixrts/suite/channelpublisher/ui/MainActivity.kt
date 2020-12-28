/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.phenixrts.suite.channelpublisher.BuildConfig
import com.phenixrts.suite.channelpublisher.ChannelPublisherApplication
import com.phenixrts.suite.channelpublisher.R
import com.phenixrts.suite.channelpublisher.common.*
import com.phenixrts.suite.channelpublisher.common.enums.ExpressError
import com.phenixrts.suite.channelpublisher.common.enums.StreamStatus
import com.phenixrts.suite.channelpublisher.databinding.ActivityMainBinding
import com.phenixrts.suite.channelpublisher.repositories.ChannelExpressRepository
import com.phenixrts.suite.channelpublisher.ui.viewmodel.ChannelViewModel
import com.phenixrts.suite.phenixcommon.DebugMenu
import com.phenixrts.suite.phenixcommon.common.FileWriterDebugTree
import com.phenixrts.suite.phenixcommon.common.showToast
import timber.log.Timber
import javax.inject.Inject

const val EXTRA_DEEP_LINK_MODEL = "ExtraDeepLinkModel"

class MainActivity : AppCompatActivity() {

    @Inject lateinit var channelExpressRepository: ChannelExpressRepository
    @Inject lateinit var fileWriterTree: FileWriterDebugTree
    private lateinit var binding: ActivityMainBinding

    private val viewModel: ChannelViewModel by lazyViewModel({ application as ChannelPublisherApplication }, {
        ChannelViewModel(channelExpressRepository)
    })
    private val debugMenu: DebugMenu by lazy {
        DebugMenu(fileWriterTree, channelExpressRepository.roomExpress, binding.root, { files ->
            debugMenu.showAppChooser(this, files)
        }, { error ->
            showToast(getString(error))
        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ChannelPublisherApplication.component.inject(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initializeDropDowns()

        viewModel.onChannelExpressError.observe(this, { error ->
            Timber.d("Channel Express failed: $error")
            showErrorDialog(error)
        })
        viewModel.onChannelState.observe(this, { status ->
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

        binding.configuration.publishButton.setOnClickListener {
            Timber.d("Publish button clicked")
            showLoading()
            viewModel.publishToChannel(getPublishConfiguration(), binding.channelSurface.holder)
        }

        binding.endStreamButton.setOnClickListener {
            Timber.d("End publish button clicked")
            viewModel.stopPublishing()
        }

        binding.menuOverlay.setOnClickListener {
            debugMenu.onScreenTapped()
        }

        checkDeepLink(intent)
        viewModel.showPublisherPreview(binding.channelSurface.holder)
        debugMenu.onStart(getString(R.string.debug_app_version,
            BuildConfig.VERSION_NAME,
            BuildConfig.VERSION_CODE
        ), getString(R.string.debug_sdk_version,
            com.phenixrts.sdk.BuildConfig.VERSION_NAME,
            com.phenixrts.sdk.BuildConfig.VERSION_CODE
        ))
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Timber.d("On new intent $intent")
        checkDeepLink(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        debugMenu.onStop()
    }

    override fun onBackPressed() {
        if (debugMenu.isClosed()){
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
        val cameraFacing = getCameraFacing()
        val cameraFps = getCameraFps()
        val streamQuality = getStreamQuality()
        val capabilities = getCapabilities().plus(streamQuality)
        val echoCancellationMode = getEchoCancellation()
        val microphoneEnabled = getMicrophoneEnabled()
        return PublishConfiguration(viewModel.channelAlias, cameraFacing, cameraFps, microphoneEnabled,
            echoCancellationMode, capabilities)
    }

}

/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.phenixrts.suite.channelpublisher.ChannelPublisherApplication
import com.phenixrts.suite.channelpublisher.R
import com.phenixrts.suite.channelpublisher.common.*
import com.phenixrts.suite.channelpublisher.databinding.ActivitySplashBinding
import com.phenixrts.suite.phenixcore.common.launchUI
import com.phenixrts.suite.phenixcore.repositories.models.PhenixError
import com.phenixrts.suite.phenixcore.repositories.models.PhenixEvent
import com.phenixrts.suite.phenixdeeplinks.common.init
import com.phenixrts.suite.phenixdeeplinks.models.DeepLinkStatus
import com.phenixrts.suite.phenixdeeplinks.models.PhenixDeepLinkConfiguration
import timber.log.Timber

private const val TIMEOUT_DELAY = 10000L

@SuppressLint("CustomSplashScreen")
class SplashActivity : EasyPermissionActivity() {

    private lateinit var binding: ActivitySplashBinding

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        launchUI {
            binding.root.showSnackBar(getString(R.string.err_network_problems))
        }
    }

    override fun isAlreadyInitialized() = phenixCore.isInitialized

    override fun onCreate(savedInstanceState: Bundle?) {
        Timber.d("Splash activity created")
        super.onCreate(savedInstanceState)
        ChannelPublisherApplication.component.inject(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        launchUI {
            phenixCore.onError.collect { error ->
                if (error == PhenixError.FAILED_TO_INITIALIZE) {
                    Timber.d("Splash: Failed to initialize Phenix Core: $error")
                    showErrorDialog(error.message)
                }
            }
        }
        launchUI {
            phenixCore.onEvent.collect { event ->
                Timber.d("Splash: Phenix core event: $event")
                if (event == PhenixEvent.PHENIX_CORE_INITIALIZED) {
                    showLandingScreen()
                }
            }
        }
    }

    override fun onDeepLinkQueried(
        status: DeepLinkStatus,
        configuration: PhenixDeepLinkConfiguration,
        rawConfiguration: Map<String, String>,
        deepLink: String
    ) {
        launchUI {
            when (status) {
                DeepLinkStatus.RELOAD -> showErrorDialog(getString(R.string.err_configuration_changed))
                DeepLinkStatus.READY -> if (arePermissionsGranted()) {
                    initializePhenixCore(configuration)
                } else {
                    askForPermissions { granted ->
                        if (granted) {
                            initializePhenixCore(configuration)
                        } else {
                            onDeepLinkQueried(status, configuration, rawConfiguration, deepLink)
                        }
                    }
                }
            }
        }
    }

    private fun initializePhenixCore(configuration: PhenixDeepLinkConfiguration) {
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_DELAY)
        Timber.d("Initializing phenix core: $configuration")
        phenixCore.init(configuration)
    }

    private fun showLandingScreen() = launchUI {
        val channelAlias = phenixCore.configuration?.selectedAlias?.takeIf { it.isNotBlank() }
            ?: phenixCore.configuration?.channelAliases?.firstOrNull()
        if (channelAlias == null) {
            showErrorDialog(getString(R.string.err_invalid_deep_link))
            return@launchUI
        }
        timeoutHandler.removeCallbacks(timeoutRunnable)
        Timber.d("Navigating to Landing Screen: $channelAlias")
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }
}

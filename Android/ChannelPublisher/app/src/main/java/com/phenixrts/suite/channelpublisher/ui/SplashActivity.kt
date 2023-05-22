/*
 * Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
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
import com.phenixrts.suite.channelpublisher.common.enums.ExpressError
import com.phenixrts.suite.channelpublisher.databinding.ActivitySplashBinding
import com.phenixrts.suite.channelpublisher.repositories.ChannelExpressRepository
import com.phenixrts.suite.channelpublisher.ui.viewmodel.ChannelViewModel
import com.phenixrts.suite.phenixcommon.common.launchMain
import com.phenixrts.suite.phenixdeeplinks.models.DeepLinkStatus
import com.phenixrts.suite.phenixdeeplinks.models.PhenixDeepLinkConfiguration
import kotlinx.coroutines.flow.collect
import timber.log.Timber
import javax.inject.Inject

private const val TIMEOUT_DELAY = 10000L

@SuppressLint("CustomSplashScreen")
class SplashActivity : EasyPermissionActivity() {

    @Inject lateinit var channelExpress: ChannelExpressRepository
    private lateinit var binding: ActivitySplashBinding

    private val viewModel: ChannelViewModel by lazyViewModel(
        { application as ChannelPublisherApplication },
        { ChannelViewModel(channelExpress) }
    )

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        launchMain {
            binding.root.showSnackBar(getString(R.string.err_network_problems))
        }
    }

    override fun isAlreadyInitialized(): Boolean = channelExpress.isChannelExpressInitialized()

    override val additionalConfiguration: HashMap<String, String>
        get() = hashMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        ChannelPublisherApplication.component.inject(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        launchMain {
            channelExpress.onError.collect { error ->
                Timber.d("Channel express failed")
                showErrorDialog(error)
            }
        }
        Timber.d("Splash activity created")
        super.onCreate(savedInstanceState)
    }

    override fun onDeepLinkQueried(
        status: DeepLinkStatus,
        configuration: PhenixDeepLinkConfiguration,
        rawConfiguration: Map<String, String>,
        deepLink: String
    ) {
        launchMain {
            when (status) {
                DeepLinkStatus.RELOAD -> showErrorDialog(ExpressError.CONFIGURATION_CHANGED_ERROR)
                DeepLinkStatus.READY -> if (arePermissionsGranted()) {
                    showLandingScreen(configuration)
                } else {
                    askForPermissions { granted ->
                        if (granted) {
                            showLandingScreen(configuration)
                        } else {
                            onDeepLinkQueried(status, configuration, rawConfiguration, deepLink)
                        }
                    }
                }
            }
        }
    }

    private fun showLandingScreen(configuration: PhenixDeepLinkConfiguration) = launchMain {
        if (configuration.selectedAlias.isEmpty()) {
            showErrorDialog(ExpressError.DEEP_LINK_ERROR)
            return@launchMain
        }
        Timber.d("Waiting for PCast")
        viewModel.channelAlias = configuration.selectedAlias
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_DELAY)
        channelExpress.setupChannelExpress(configuration)
        channelExpress.waitForPCast()
        timeoutHandler.removeCallbacks(timeoutRunnable)
        Timber.d("Navigating to Landing Screen")
        startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        finish()
    }
}

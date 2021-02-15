/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.google.android.material.snackbar.Snackbar
import com.phenixrts.suite.channelpublisher.ChannelPublisherApplication
import com.phenixrts.suite.channelpublisher.R
import com.phenixrts.suite.channelpublisher.common.*
import com.phenixrts.suite.channelpublisher.common.enums.ExpressError
import com.phenixrts.suite.channelpublisher.databinding.ActivitySplashBinding
import com.phenixrts.suite.channelpublisher.repositories.ChannelExpressRepository
import com.phenixrts.suite.phenixdeeplink.DeepLinkStatus
import com.phenixrts.suite.phenixdeeplink.common.asConfigurationModel
import timber.log.Timber
import javax.inject.Inject

private const val TIMEOUT_DELAY = 10000L

class SplashActivity : EasyPermissionActivity() {

    @Inject lateinit var channelExpressRepository: ChannelExpressRepository
    private lateinit var binding: ActivitySplashBinding

    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val timeoutRunnable = Runnable {
        launchMain {
            showSnackBar(getString(R.string.err_network_problems))
        }
    }

    override val additionalConfiguration: HashMap<String, String>
        get() = hashMapOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        ChannelPublisherApplication.component.inject(this)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        channelExpressRepository.onChannelExpressError.observe(this, { error ->
            Timber.d("Channel express failed")
            showErrorDialog(error)
        })
        Timber.d("Splash activity created")
        super.onCreate(savedInstanceState)
    }

    override fun onDeepLinkQueried(status: DeepLinkStatus) {
        com.phenixrts.suite.phenixcommon.common.launchMain {
            when (status) {
                DeepLinkStatus.RELOAD -> showErrorDialog(ExpressError.CONFIGURATION_CHANGED_ERROR)
                DeepLinkStatus.READY -> if (arePermissionsGranted()) {
                    showLandingScreen()
                } else {
                    askForPermissions { granted ->
                        if (granted) {
                            showLandingScreen()
                        } else {
                            onDeepLinkQueried(status)
                        }
                    }
                }
            }
        }
    }

    override fun isAlreadyInitialized(): Boolean = channelExpressRepository.isChannelExpressInitialized()

    private fun showSnackBar(message: String) = launchMain {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_INDEFINITE).show()
    }

    private fun showLandingScreen() = launchMain {
        val config = configuration.asConfigurationModel()
        if (config == null || config.channelAlias.isNullOrBlank()) {
            showErrorDialog(ExpressError.DEEP_LINK_ERROR)
            return@launchMain
        }
        Timber.d("Waiting for PCast")
        timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_DELAY)
        channelExpressRepository.setupChannelExpress(config)
        channelExpressRepository.waitForPCast()
        timeoutHandler.removeCallbacks(timeoutRunnable)
        Timber.d("Navigating to Landing Screen")
        val intent = Intent(this@SplashActivity, MainActivity::class.java)
        intent.putExtra(EXTRA_DEEP_LINK_MODEL, config.channelAlias)
        startActivity(intent)
        finish()
    }
}

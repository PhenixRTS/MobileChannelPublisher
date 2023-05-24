/*
 * Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.phenixdeeplinks

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.phenixrts.suite.phenixdeeplinks.cache.ConfigurationProvider
import com.phenixrts.suite.phenixdeeplinks.models.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import timber.log.Timber

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}

abstract class DeepLinkActivity : AppCompatActivity() {

    private val configurationProvider by lazy { ConfigurationProvider(this) }
    private val configuration: HashMap<String, String> = hashMapOf(
        QUERY_AUTH_TOKEN to "",
        QUERY_PUBLISH_TOKEN to "",
    )

    abstract val additionalConfiguration: HashMap<String, String>

    abstract fun isAlreadyInitialized(): Boolean

    abstract fun onDeepLinkQueried(
        status: DeepLinkStatus,
        configuration: PhenixDeepLinkConfiguration,
        rawConfiguration: Map<String, String>,
        deepLink: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent?.run {
            Timber.d("Deep Link activity created")
            updateConfiguration(this)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent?.run {
            Timber.d("Deep Link activity received new intent")
            updateConfiguration(this)
        }
    }

    private fun updateConfiguration(intent: Intent) {
        var path = ""
        var status = DeepLinkStatus.READY
        configuration.putAll(additionalConfiguration)
        Timber.d("Checking deep link: ${intent.data}, $configuration")
        if (configurationProvider.hasConfiguration()) {
            JSONObject(configurationProvider.getConfiguration()).run {
                Timber.d("Loading saved configuration: $this")
                keys().forEach { key ->
                    configuration[key] = getString(key)
                }
            }
        } else {
            intent.data?.let { deepLink ->
                Timber.d("Loading configuration from deep link: $deepLink")

                configuration.keys.forEach { key ->
                    deepLink.getQueryParameter(key)?.let { value -> configuration[key] = value }
                }

                if (isAlreadyInitialized()) {
                    Timber.d("Configuration already loaded")
                    configurationProvider.saveConfiguration(json.encodeToString(configuration))
                    status = DeepLinkStatus.RELOAD
                    onDeepLinkQueried(status, configuration.asConfigurationModel(), configuration, path)
                    return
                }
            }
        }
        Timber.d("Configuration updated: $configuration")
        configurationProvider.saveConfiguration(null)
        onDeepLinkQueried(status, configuration.asConfigurationModel(), configuration, path)
    }
}

private fun HashMap<String, String>.asConfigurationModel() =
    json.decodeFromString<PhenixDeepLinkConfiguration>(JSONObject(this as Map<*, *>).toString())

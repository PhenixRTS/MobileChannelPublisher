/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.common

import android.content.Context
import com.google.gson.Gson
import com.phenixrts.suite.channelpublisher.ChannelPublisherApplication

class PreferenceProvider(private val context: ChannelPublisherApplication) {

    fun saveConfiguration(configuration: ChannelConfiguration?) {
        context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString(CONFIGURATION, Gson().toJson(configuration))
            .apply()
    }

    fun getConfiguration(): ChannelConfiguration? {
        var configuration: ChannelConfiguration? = null
        context.getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE).getString(CONFIGURATION, null)?.let { cache ->
            configuration = Gson().fromJson(cache, ChannelConfiguration::class.java)
        }
        return configuration
    }

    companion object {
        private const val APP_PREFERENCES = "publisher_preferences"
        private const val CONFIGURATION = "configuration"
    }
}

/*
 * Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.phenixdeeplinks.models

import com.phenixrts.suite.phenixdeeplinks.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

internal const val QUERY_AUTH_TOKEN = "authToken"
internal const val QUERY_PUBLISH_TOKEN = "publishToken"

@Suppress("unused")
@Serializable
data class PhenixDeepLinkConfiguration(
    @SerialName(QUERY_AUTH_TOKEN) val authToken: String,
    @SerialName(QUERY_PUBLISH_TOKEN) val publishToken: String,
)

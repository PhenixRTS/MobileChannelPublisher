/*
 * Copyright 2021 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.phenixdeeplink.models

import com.phenixrts.suite.phenixdeeplink.BuildConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal const val STAGING_URI = "stg.phenixrts.com"
internal const val QUERY_ACTS = "acts"
internal const val QUERY_AUTH_TOKEN = "authToken"
internal const val QUERY_BACKEND = "backend"
internal const val QUERY_CHANNEL = "#"
internal const val QUERY_CHANNEL_ALIASES = "channelAliases"
internal const val QUERY_EDGE_TOKEN = "edgeToken"
internal const val QUERY_MIME_TYPES = "mimetypes"
internal const val QUERY_PUBLISH_TOKEN = "publishToken"
internal const val QUERY_STREAM_IDS = "streamIDs"
internal const val QUERY_URI = "uri"
internal const val QUERY_VIDEO_COUNT = "maxVideoMembers"

@Suppress("unused")
@Serializable
data class PhenixDeepLinkConfiguration(
    @SerialName(QUERY_AUTH_TOKEN) val authToken: String = "",
    @SerialName(QUERY_ACTS) val rawActs: String = "",
    @SerialName(QUERY_BACKEND) val backend: String = "",
    @SerialName(QUERY_CHANNEL_ALIASES) val rawChannelAliases: String = "",
    @SerialName(QUERY_EDGE_TOKEN) val edgeToken: String = "",
    @SerialName(QUERY_MIME_TYPES) val rawMimeTypes: String = "",
    @SerialName(QUERY_PUBLISH_TOKEN) val publishToken: String = "",
    @SerialName(QUERY_STREAM_IDS) val rawStreamIDs: String = "",
    @SerialName(QUERY_URI) val uri: String = "",
    @SerialName(QUERY_VIDEO_COUNT) private val maxVideoMembers: String = ""
) {
    val actTimes = rawActs.takeIf { it.isNotBlank() }?.split(",") ?: emptyList()
    val channels = rawChannelAliases.takeIf { it.isNotBlank() }?.split(",") ?: emptyList()
    val mimeTypes = rawMimeTypes.takeIf { it.isNotBlank() }?.split(",") ?: emptyList()
    val streams = rawStreamIDs.takeIf { it.isNotBlank() }?.split(",") ?: emptyList()
    val videoMemberCount = maxVideoMembers.takeIf { it.isNotBlank() }?.toIntOrNull() ?: BuildConfig.MAX_VIDEO_MEMBERS.toInt()
}
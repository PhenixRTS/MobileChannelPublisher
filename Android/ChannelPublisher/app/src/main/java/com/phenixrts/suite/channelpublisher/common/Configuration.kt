/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.common

import android.widget.Spinner
import com.phenixrts.express.ChannelExpressFactory
import com.phenixrts.express.PCastExpressFactory
import com.phenixrts.express.PublishToChannelOptions
import com.phenixrts.pcast.*
import com.phenixrts.room.RoomServiceFactory
import com.phenixrts.suite.channelpublisher.BuildConfig
import java.io.Serializable

const val QUERY_URI = "uri"
const val QUERY_BACKEND = "backend"
const val QUERY_CHANNEL = "#"
const val QUERY_STAGING = "https://stg.phenixrts.com"

private val CAMERA_OPTIONS = listOf(FacingMode.USER, FacingMode.ENVIRONMENT, FacingMode.UNDEFINED)
private val AUDIO_OPTIONS = listOf(true, false)
private val QUALITY_OPTIONS = listOf("vvld", "vld", "ld", "sd", "hd", "fhd")
private val FPS_OPTIONS = listOf(15, 30)
private val AEC_OPTIONS = listOf(AudioEchoCancelationMode.AUTOMATIC, AudioEchoCancelationMode.ON, AudioEchoCancelationMode.OFF)
private val MBR_OPTIONS = listOf(listOf(), listOf("multi-bitrate"), listOf("multi-bitrate", "multi-bitrate-codec=vp8"),
    listOf("multi-bitrate", "multi-bitrate-codec=h264"))

data class ChannelConfiguration(
    val uri: String = BuildConfig.PCAST_URL,
    val backend: String = BuildConfig.BACKEND_URL
) : Serializable

data class PublishConfiguration(
    val channelAlias: String,
    val cameraFacingMode: FacingMode,
    val cameraFps: Int,
    val microphoneEnabled: Boolean,
    val echoCancellationMode: AudioEchoCancelationMode,
    val capabilities: List<String>
)

fun getPublishToChannelOptions(configuration: PublishConfiguration, userMediaStream: UserMediaStream): PublishToChannelOptions {
    val channelOptions = RoomServiceFactory.createChannelOptionsBuilder()
        .withName(configuration.channelAlias)
        .buildChannelOptions()
    val mediaConstraints = UserMediaOptions().apply {
        videoOptions.capabilityConstraints[DeviceCapability.FACING_MODE] =
            listOf(DeviceConstraint(configuration.cameraFacingMode))
        videoOptions.capabilityConstraints[DeviceCapability.FRAME_RATE] =
            listOf(DeviceConstraint(configuration.cameraFps.toDouble()))
        audioOptions.capabilityConstraints[DeviceCapability.AUDIO_ECHO_CANCELATION_MODE] =
            listOf(DeviceConstraint(configuration.echoCancellationMode))
        audioOptions.enabled = configuration.microphoneEnabled
    }

    val publishOptions  = PCastExpressFactory.createPublishOptionsBuilder()
        .withCapabilities(configuration.capabilities.toTypedArray())
        // TODO: Setting this times out the publishing process without any errors or exceptions
        //.withMediaConstraints(mediaConstraints)
        .withUserMedia(userMediaStream)
        .buildPublishOptions()
    return ChannelExpressFactory.createPublishToChannelOptionsBuilder()
        .withChannelOptions(channelOptions)
        .withPublishOptions(publishOptions)
        .buildPublishToChannelOptions()
}

fun getUserMediaOptions(facingMode: FacingMode = FacingMode.USER): UserMediaOptions = UserMediaOptions().apply {
    videoOptions.capabilityConstraints[DeviceCapability.FACING_MODE] = listOf(DeviceConstraint(facingMode))
    videoOptions.capabilityConstraints[DeviceCapability.HEIGHT] = listOf(DeviceConstraint(360.0))
    videoOptions.capabilityConstraints[DeviceCapability.FRAME_RATE] = listOf(DeviceConstraint(15.0))
    audioOptions.capabilityConstraints[DeviceCapability.AUDIO_ECHO_CANCELATION_MODE] =
        listOf(DeviceConstraint(AudioEchoCancelationMode.ON))
}

fun Spinner.getCameraFacing(): FacingMode = CAMERA_OPTIONS[selectedItemPosition]

fun Spinner.getMicrophoneEnabled(): Boolean = AUDIO_OPTIONS[selectedItemPosition]

fun Spinner.getCameraFps(): Int = FPS_OPTIONS[selectedItemPosition]

fun Spinner.getStreamQuality(): String = QUALITY_OPTIONS[selectedItemPosition]

fun Spinner.getEchoCancellation(): AudioEchoCancelationMode = AEC_OPTIONS[selectedItemPosition]

fun Spinner.getCapabilities(): List<String> = MBR_OPTIONS[selectedItemPosition]

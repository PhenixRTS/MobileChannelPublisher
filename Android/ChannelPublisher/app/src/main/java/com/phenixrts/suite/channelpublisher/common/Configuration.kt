/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.common

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
private val MICROPHONE_OPTIONS = listOf(true, false)
private val QUALITY_OPTIONS = listOf("vvld", "vld", "ld", "sd", "hd", "fhd")
private val FPS_OPTIONS = listOf(15, 30)
private val AEC_OPTIONS = listOf(AudioEchoCancelationMode.AUTOMATIC, AudioEchoCancelationMode.ON, AudioEchoCancelationMode.OFF)
private val MBR_OPTIONS = listOf(listOf(), listOf("multi-bitrate"), listOf("multi-bitrate", "multi-bitrate-codec=vp8"),
    listOf("multi-bitrate", "multi-bitrate-codec=h264"))

var selectedCameraFacing = 0
var selectedMicrophoneOption = 0
var selectedQualityOption = 4
var selectedFpsOption = 0
var selectedAecOption = 1
var selectedMbrOption = 1

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
         // TODO: If name is not set - publish callback returns FAILED without any extra explanation why
        .withName(configuration.channelAlias)
        .withAlias(configuration.channelAlias)
        .buildChannelOptions()
    val publishOptions  = PCastExpressFactory.createPublishOptionsBuilder()
        .withCapabilities(configuration.capabilities.toTypedArray())
        .withUserMedia(userMediaStream)
        .buildPublishOptions()
    return ChannelExpressFactory.createPublishToChannelOptionsBuilder()
        .withChannelOptions(channelOptions)
        .withPublishOptions(publishOptions)
        .buildPublishToChannelOptions()
}

fun getUserMediaOptions(configuration: PublishConfiguration): UserMediaOptions = UserMediaOptions().apply {
    // TODO: Changing facing mode some time crashes the app with:
    //  JNI DETECTED ERROR IN APPLICATION: JNI GetObjectRefType called with pending exception java.lang.RuntimeException: Fail to connect to camera service
    if (configuration.cameraFacingMode != FacingMode.UNDEFINED) {
        videoOptions.capabilityConstraints[DeviceCapability.FACING_MODE] = listOf(DeviceConstraint(configuration.cameraFacingMode))
    }
    // TODO: If Height is not set to the same value as the default one (Set on app start) - then BAD_REQUEST is returned when applying options
    videoOptions.capabilityConstraints[DeviceCapability.HEIGHT] = listOf(DeviceConstraint(360.0))
    // TODO: Changing FPS - causes BAD_REQUEST which then causes the stream to be re-created and unusable for publishing;
    videoOptions.capabilityConstraints[DeviceCapability.FRAME_RATE] = listOf(DeviceConstraint(configuration.cameraFps.toDouble()))
    audioOptions.capabilityConstraints[DeviceCapability.AUDIO_ECHO_CANCELATION_MODE] = listOf(DeviceConstraint(configuration.echoCancellationMode))
    audioOptions.enabled = configuration.microphoneEnabled
}

fun getDefaultUserMediaOptions(): UserMediaOptions = UserMediaOptions().apply {
    videoOptions.capabilityConstraints[DeviceCapability.FACING_MODE] = listOf(DeviceConstraint(FacingMode.USER))
    videoOptions.capabilityConstraints[DeviceCapability.HEIGHT] = listOf(DeviceConstraint(360.0))
    videoOptions.capabilityConstraints[DeviceCapability.FRAME_RATE] = listOf(DeviceConstraint(15.0))
    audioOptions.capabilityConstraints[DeviceCapability.AUDIO_ECHO_CANCELATION_MODE] =
        listOf(DeviceConstraint(AudioEchoCancelationMode.ON))
}

fun getCameraFacing(): FacingMode = CAMERA_OPTIONS[selectedCameraFacing]

fun getMicrophoneEnabled(): Boolean = MICROPHONE_OPTIONS[selectedMicrophoneOption]

fun getCameraFps(): Int = FPS_OPTIONS[selectedFpsOption]

fun getStreamQuality(): String = QUALITY_OPTIONS[selectedQualityOption]

fun getEchoCancellation(): AudioEchoCancelationMode = AEC_OPTIONS[selectedAecOption]

fun getCapabilities(): List<String> = MBR_OPTIONS[selectedMbrOption]

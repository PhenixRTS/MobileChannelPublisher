/*
 * Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.common

import com.phenixrts.express.ChannelExpressFactory
import com.phenixrts.express.PCastExpressFactory
import com.phenixrts.express.PublishToChannelOptions
import com.phenixrts.pcast.*
import com.phenixrts.room.RoomServiceFactory
import com.phenixrts.suite.phenixdeeplinks.models.PhenixDeepLinkConfiguration
import timber.log.Timber

private val CAMERA_OPTIONS = listOf(FacingMode.USER, FacingMode.ENVIRONMENT, FacingMode.UNDEFINED)
private val MICROPHONE_OPTIONS = listOf(true, false)
private val FPS_OPTIONS = listOf(15, 30)
private val AEC_OPTIONS = listOf(AudioEchoCancelationMode.AUTOMATIC, AudioEchoCancelationMode.ON, AudioEchoCancelationMode.OFF)

var selectedCameraFacing = 0
var selectedMicrophoneOption = 0
var selectedFpsOption = 0
var selectedAecOption = 1

data class PublishConfiguration(
    val channelAlias: String,
    val cameraFacingMode: FacingMode,
    val cameraFps: Int,
    val microphoneEnabled: Boolean,
    val echoCancellationMode: AudioEchoCancelationMode
)

fun getPublishToChannelOptions(publishConfig: PublishConfiguration, configuration: PhenixDeepLinkConfiguration,
                               userMediaStream: UserMediaStream): PublishToChannelOptions {
    val channelOptions = RoomServiceFactory.createChannelOptionsBuilder()
        .withName(publishConfig.channelAlias)
        .withAlias(publishConfig.channelAlias)
        .buildChannelOptions()

    var publishOptionsBuilder = PCastExpressFactory.createPublishOptionsBuilder()
        .withUserMedia(userMediaStream)

    if (!configuration.publishToken.isNullOrBlank()) {
        Timber.d("Publishing with publish token: ${configuration.publishToken}")
        publishOptionsBuilder.withStreamToken(configuration.publishToken).withSkipRetryOnUnauthorized()
    }

    return ChannelExpressFactory.createPublishToChannelOptionsBuilder()
        .withChannelOptions(channelOptions)
        .withPublishOptions(publishOptionsBuilder.buildPublishOptions())
        .buildPublishToChannelOptions()
}

fun getUserMediaOptions(configuration: PublishConfiguration): UserMediaOptions = UserMediaOptions().apply {
    if (configuration.cameraFacingMode != FacingMode.UNDEFINED) {
        videoOptions.enabled = true

        videoOptions.capabilityConstraints[DeviceCapability.FACING_MODE] = listOf(DeviceConstraint(configuration.cameraFacingMode))
        videoOptions.capabilityConstraints[DeviceCapability.HEIGHT] = listOf(DeviceConstraint(360.0))
        videoOptions.capabilityConstraints[DeviceCapability.FRAME_RATE] = listOf(DeviceConstraint(configuration.cameraFps.toDouble()))
    } else {
        videoOptions.enabled = false
    }

    if (configuration.microphoneEnabled) {
        audioOptions.enabled = true
        audioOptions.capabilityConstraints[DeviceCapability.AUDIO_ECHO_CANCELATION_MODE] = listOf(DeviceConstraint(configuration.echoCancellationMode))
    } else {
        audioOptions.enabled = false
    }
}

fun getDefaultUserMediaOptions(): UserMediaOptions = UserMediaOptions().apply {
    videoOptions.enabled = true
    videoOptions.capabilityConstraints[DeviceCapability.FACING_MODE] = listOf(DeviceConstraint(FacingMode.USER))
    videoOptions.capabilityConstraints[DeviceCapability.HEIGHT] = listOf(DeviceConstraint(360.0))
    videoOptions.capabilityConstraints[DeviceCapability.FRAME_RATE] = listOf(DeviceConstraint(15.0))
    audioOptions.enabled = true
    audioOptions.capabilityConstraints[DeviceCapability.AUDIO_ECHO_CANCELATION_MODE] =
        listOf(DeviceConstraint(AudioEchoCancelationMode.ON))
}

fun getCameraFacing(): FacingMode = CAMERA_OPTIONS[selectedCameraFacing]

fun getMicrophoneEnabled(): Boolean = MICROPHONE_OPTIONS[selectedMicrophoneOption]

fun getCameraFps(): Int = FPS_OPTIONS[selectedFpsOption]

fun getEchoCancellation(): AudioEchoCancelationMode = AEC_OPTIONS[selectedAecOption]

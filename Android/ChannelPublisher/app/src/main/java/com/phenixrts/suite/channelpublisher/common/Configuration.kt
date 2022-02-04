/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.common

import com.phenixrts.suite.phenixcore.repositories.models.PhenixAudioEchoCancelationMode
import com.phenixrts.suite.phenixcore.repositories.models.PhenixFacingMode

private val CAMERA_OPTIONS = listOf(
    PhenixFacingMode.USER,
    PhenixFacingMode.ENVIRONMENT,
    PhenixFacingMode.UNDEFINED
)
private val MICROPHONE_OPTIONS = listOf(true, false)
private val FPS_OPTIONS = listOf(15, 30)
private val AEC_OPTIONS = listOf(
    PhenixAudioEchoCancelationMode.AUTOMATIC,
    PhenixAudioEchoCancelationMode.ON,
    PhenixAudioEchoCancelationMode.OFF
)

var selectedCameraFacing = 0
var selectedMicrophoneOption = 0
var selectedFpsOption = 0
var selectedAecOption = 1
const val CAMERA_OFF_INDEX = 2

fun getCameraFacing(): PhenixFacingMode = CAMERA_OPTIONS[selectedCameraFacing]

fun getMicrophoneEnabled(): Boolean = MICROPHONE_OPTIONS[selectedMicrophoneOption]

fun getCameraFps(): Int = FPS_OPTIONS[selectedFpsOption]

fun getEchoCancellation(): PhenixAudioEchoCancelationMode = AEC_OPTIONS[selectedAecOption]

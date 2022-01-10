/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.common

import com.phenixrts.pcast.*

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

fun getCameraFacing(): FacingMode = CAMERA_OPTIONS[selectedCameraFacing]

fun getMicrophoneEnabled(): Boolean = MICROPHONE_OPTIONS[selectedMicrophoneOption]

fun getCameraFps(): Int = FPS_OPTIONS[selectedFpsOption]

fun getStreamQuality(): String = QUALITY_OPTIONS[selectedQualityOption]

fun getEchoCancellation(): AudioEchoCancelationMode = AEC_OPTIONS[selectedAecOption]

fun getCapabilities(): List<String> = MBR_OPTIONS[selectedMbrOption]

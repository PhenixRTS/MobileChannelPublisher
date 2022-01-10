/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.phenixcore.repositories.models

import com.phenixrts.pcast.AudioEchoCancelationMode
import com.phenixrts.pcast.FacingMode

data class PhenixPublishConfiguration(
    val cameraFacingMode: FacingMode = FacingMode.UNDEFINED,
    val cameraFps: Double = 15.0,
    val microphoneEnabled: Boolean = false,
    val echoCancellationMode: AudioEchoCancelationMode = AudioEchoCancelationMode.OFF,
)

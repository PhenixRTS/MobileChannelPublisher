/*
 * Copyright 2024 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.common

import com.phenixrts.common.RequestStatus
import com.phenixrts.express.ChannelExpress
import com.phenixrts.express.ExpressPublisher
import com.phenixrts.express.PCastExpress
import com.phenixrts.express.PublishToChannelOptions
import com.phenixrts.pcast.PCast
import com.phenixrts.pcast.UserMediaOptions
import com.phenixrts.pcast.UserMediaStream
import com.phenixrts.room.RoomService
import com.phenixrts.suite.channelpublisher.common.enums.StreamStatus
import com.phenixrts.suite.phenixcommon.common.launchMain
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

suspend fun PCastExpress.waitForOnline() = suspendCoroutine<Unit> { continuation ->
    waitForOnline {
        continuation.resume(Unit)
    }
}

fun PCast.collectLogs(onCollected: (String) -> Unit) {
    collectLogMessages { _, _, messages ->
        onCollected(messages)
    }
}

suspend fun PCastExpress.getUserMedia(options: UserMediaOptions): UserMediaStream? = suspendCoroutine { continuation ->
    getUserMedia(options) { status, userMediaStream ->
        Timber.d("Collecting media stream from pCast: $status")
        if (status == RequestStatus.OK) {
            continuation.resume(userMediaStream)
        } else {
            continuation.resume(null)
        }
    }
}

suspend fun ChannelExpress.publishToChannel(options: PublishToChannelOptions): PublishState = suspendCoroutine { continuation ->
    publishToChannel(options) { requestStatus: RequestStatus?, roomService: RoomService?, publisher: ExpressPublisher? ->
        launchMain {
            Timber.d("Stream published: $requestStatus")
            if (requestStatus == RequestStatus.OK && roomService != null && publisher != null) {
                continuation.resume(PublishState(StreamStatus.CONNECTED, roomService, publisher))
            } else {
                continuation.resume(PublishState(StreamStatus.FAILED))
            }
        }
    }
}

data class PublishState(
    val streamStatus: StreamStatus,
    val roomService: RoomService? = null,
    val publisher: ExpressPublisher? = null
)

fun UserMediaOptions.copy() : UserMediaOptions {
    var newUserMediaOptions = UserMediaOptions()

     newUserMediaOptions.videoOptions.enabled = videoOptions.enabled
     videoOptions.capabilityConstraints.forEach { constraint ->
         newUserMediaOptions.videoOptions.capabilityConstraints[constraint.key] = constraint.value
     }

     newUserMediaOptions.audioOptions.enabled = audioOptions.enabled
     audioOptions.capabilityConstraints.forEach { constraint ->
         newUserMediaOptions.audioOptions.capabilityConstraints[constraint.key] = constraint.value
     }

     return newUserMediaOptions
}

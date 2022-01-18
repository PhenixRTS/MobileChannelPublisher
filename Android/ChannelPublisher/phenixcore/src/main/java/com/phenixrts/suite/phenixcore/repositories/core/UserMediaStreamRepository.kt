package com.phenixrts.suite.phenixcore.repositories.core

import com.phenixrts.common.RequestStatus
import com.phenixrts.pcast.UserMediaOptions
import com.phenixrts.pcast.UserMediaStream

class UserMediaStreamRepository: UserMediaStreamProvider {
    private var userMediaStream: UserMediaStream? = null

    override fun getUserMediaStream(): UserMediaStream? {
        return userMediaStream
    }

    fun setUserMediaStream(userMediaStream: UserMediaStream) {
        this.userMediaStream = userMediaStream
    }

    fun applyOptions(options: UserMediaOptions): RequestStatus? {
        return userMediaStream?.applyOptions(options)
    }

    fun dispose() {
        userMediaStream?.dispose()
        userMediaStream = null
    }
}
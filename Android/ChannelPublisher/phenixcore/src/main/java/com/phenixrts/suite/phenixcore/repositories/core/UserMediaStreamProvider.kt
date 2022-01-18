package com.phenixrts.suite.phenixcore.repositories.core

import com.phenixrts.pcast.UserMediaStream

interface UserMediaStreamProvider {
    fun getUserMediaStream(): UserMediaStream?
}
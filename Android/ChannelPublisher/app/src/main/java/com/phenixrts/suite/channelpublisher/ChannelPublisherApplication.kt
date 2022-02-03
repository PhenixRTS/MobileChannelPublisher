/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.phenixrts.suite.channelpublisher.common.LineNumberDebugTree
import com.phenixrts.suite.channelpublisher.injection.DaggerInjectionComponent
import com.phenixrts.suite.channelpublisher.injection.InjectionComponent
import com.phenixrts.suite.channelpublisher.injection.InjectionModule
import timber.log.Timber

class ChannelPublisherApplication : Application(), ViewModelStoreOwner {

    private val appViewModelStore: ViewModelStore by lazy {
        ViewModelStore()
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(LineNumberDebugTree("ChannelPublisher"))
        }
        component = DaggerInjectionComponent.builder().injectionModule(InjectionModule(this)).build()
    }

    override fun getViewModelStore() = appViewModelStore

    companion object {
        lateinit var component: InjectionComponent
            private set
    }
}

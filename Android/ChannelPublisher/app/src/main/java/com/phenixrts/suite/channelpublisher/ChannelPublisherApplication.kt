/*
 * Copyright 2023 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher

import android.app.Application
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import com.phenixrts.suite.channelpublisher.injection.DaggerInjectionComponent
import com.phenixrts.suite.channelpublisher.injection.InjectionComponent
import com.phenixrts.suite.channelpublisher.injection.InjectionModule
import com.phenixrts.suite.phenixcommon.common.FileWriterDebugTree
import timber.log.Timber
import javax.inject.Inject

class ChannelPublisherApplication : Application(), ViewModelStoreOwner {

    private val appViewModelStore: ViewModelStore by lazy {
        ViewModelStore()
    }

    @Inject
    lateinit var fileWriterTree: FileWriterDebugTree

    override fun onCreate() {
        super.onCreate()

        component = DaggerInjectionComponent.builder().injectionModule(InjectionModule(this)).build()
        component.inject(this)
        if (BuildConfig.DEBUG) {
            Timber.plant(fileWriterTree)
        }
    }

    override fun getViewModelStore() = appViewModelStore

    companion object {
        lateinit var component: InjectionComponent
            private set
    }
}

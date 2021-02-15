/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.injection

import com.phenixrts.suite.channelpublisher.BuildConfig
import com.phenixrts.suite.channelpublisher.ChannelPublisherApplication
import com.phenixrts.suite.channelpublisher.repositories.ChannelExpressRepository
import com.phenixrts.suite.phenixcommon.common.FileWriterDebugTree
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

private const val TIMBER_TAG = "ChannelPublisher:"

@Module
class InjectionModule(private val context: ChannelPublisherApplication) {

    @Singleton
    @Provides
    fun provideChannelExpressRepository(): ChannelExpressRepository = ChannelExpressRepository(context)

    @Provides
    @Singleton
    fun provideFileWriterDebugTree(): FileWriterDebugTree =
        FileWriterDebugTree(context, TIMBER_TAG, "${BuildConfig.APPLICATION_ID}.provider")

}

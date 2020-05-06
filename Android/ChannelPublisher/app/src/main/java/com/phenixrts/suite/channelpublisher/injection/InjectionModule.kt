/*
 * Copyright 2020 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.injection

import com.phenixrts.suite.channelpublisher.ChannelPublisherApplication
import com.phenixrts.suite.channelpublisher.common.PreferenceProvider
import com.phenixrts.suite.channelpublisher.repositories.ChannelExpressRepository
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class InjectionModule(private val context: ChannelPublisherApplication) {

    @Singleton
    @Provides
    fun provideChannelExpressRepository(): ChannelExpressRepository = ChannelExpressRepository(context)

    @Singleton
    @Provides
    fun providePreferenceProvider(): PreferenceProvider = PreferenceProvider(context)

}

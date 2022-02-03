/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.injection

import com.phenixrts.suite.channelpublisher.ChannelPublisherApplication
import com.phenixrts.suite.phenixcore.PhenixCore
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
class InjectionModule(private val context: ChannelPublisherApplication) {

    @Singleton
    @Provides
    fun providePhenixCore() = PhenixCore(context)

}

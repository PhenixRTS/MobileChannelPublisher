/*
 * Copyright 2022 Phenix Real Time Solutions, Inc. Confidential and Proprietary. All rights reserved.
 */

package com.phenixrts.suite.channelpublisher.injection

import com.phenixrts.suite.channelpublisher.ui.EasyPermissionActivity
import com.phenixrts.suite.channelpublisher.ui.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [InjectionModule::class])
interface InjectionComponent {
    fun inject(target: EasyPermissionActivity)
    fun inject(target: MainActivity)
}

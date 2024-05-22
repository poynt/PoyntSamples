package com.godaddy.commerce.services.sample

import android.app.Application
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import timber.log.Timber


class CommerceServicesApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CommerceDependencyProvider.init(this)
        Timber.plant(Timber.DebugTree())
    }
}
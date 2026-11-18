package com.numberone.app

import android.app.Application
import com.numberone.app.data.remote.SupabaseClientProvider

class NumberOneApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseClientProvider.init()
    }
}
package com.openmojang.launcher

import android.app.Application

class OpenMojangApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Init download cache, MSAL, version catalog here later
    }
}

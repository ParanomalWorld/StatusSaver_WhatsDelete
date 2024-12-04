/*
 * Copyright (C) 2023 Christians Martínez Alvarado
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 * the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package com.divineengine.statussaver

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.video.VideoFrameDecoder
import com.divineengine.statussaver.extensions.getDefaultDayNightMode
import com.divineengine.statussaver.extensions.isAnalyticsEnabled
import com.divineengine.statussaver.extensions.migratePreferences
import com.divineengine.statussaver.extensions.packageInfo
import com.divineengine.statussaver.extensions.preferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

fun getApp(): App = App.instance

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class App : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()
        instance = this

        preferences().migratePreferences()

        // Disable Analytics/Crashlytics for debug builds
        setAnalyticsEnabled(preferences().isAnalyticsEnabled())

        startKoin {
            androidContext(this@App)
            modules(appModules)
        }

        AppCompatDelegate.setDefaultNightMode(preferences().getDefaultDayNightMode())
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .crossfade(true)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .build()
    }

    val versionName: String
        get() = packageManager.packageInfo().versionName ?: "0"

    companion object {
        internal lateinit var instance: App
            private set

        fun isFDroidBuild() = BuildConfig.FLAVOR.equals("fdroid", ignoreCase = true)

        fun getFileProviderAuthority(): String = instance.packageName + ".file_provider"
    }
}
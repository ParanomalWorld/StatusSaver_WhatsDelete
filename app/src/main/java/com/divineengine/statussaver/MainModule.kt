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

import androidx.room.Room
import com.divineengine.statussaver.database.MIGRATION_1_2
import com.divineengine.statussaver.database.StatusDatabase
import com.divineengine.statussaver.repository.CountryRepository
import com.divineengine.statussaver.repository.CountryRepositoryImpl
import com.divineengine.statussaver.repository.MessageRepository
import com.divineengine.statussaver.repository.MessageRepositoryImpl
import com.divineengine.statussaver.repository.Repository
import com.divineengine.statussaver.repository.RepositoryImpl
import com.divineengine.statussaver.repository.StatusesRepository
import com.divineengine.statussaver.repository.StatusesRepositoryImpl
import com.divineengine.statussaver.storage.Storage
import com.divineengine.statussaver.update.provideOkHttp
import com.divineengine.statussaver.update.provideUpdateService
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.bind
import org.koin.dsl.module

private val networkModule = module {
    factory {
        provideOkHttp(get())
    }
    single {
        provideUpdateService(get())
    }
}

private val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(), StatusDatabase::class.java, "statuses.db")
            .addMigrations(MIGRATION_1_2)
            .build()
    }

    factory {
        get<StatusDatabase>().statusDao()
    }

    factory {
        get<StatusDatabase>().messageDao()
    }
}

private val managerModule = module {
    single {
        PhoneNumberUtil.createInstance(androidContext())
    }
    single {
        Storage(androidContext())
    }
}

private val statusesModule = module {
    single {
        CountryRepositoryImpl(androidContext())
    } bind CountryRepository::class

    single {
        StatusesRepositoryImpl(androidContext(), get(), get())
    } bind StatusesRepository::class

    single {
        MessageRepositoryImpl(get())
    } bind MessageRepository::class

    single {
        RepositoryImpl(get(), get(), get())
    } bind Repository::class
}

private val viewModelModule = module {
    viewModel {
        WhatSaveViewModel(get(), get(), get())
    }
}

val appModules = listOf(networkModule, dataModule, managerModule, statusesModule, viewModelModule)
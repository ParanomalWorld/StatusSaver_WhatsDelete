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

import android.app.DownloadManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.getSystemService
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.divineengine.statussaver.database.Conversation
import com.divineengine.statussaver.database.MessageEntity
import com.divineengine.statussaver.extensions.blacklistMessageSender
import com.divineengine.statussaver.extensions.getAllInstalledClients
import com.divineengine.statussaver.extensions.lastUpdateId
import com.divineengine.statussaver.extensions.preferences
import com.divineengine.statussaver.model.Country
import com.divineengine.statussaver.model.Status
import com.divineengine.statussaver.model.StatusQueryResult
import com.divineengine.statussaver.model.StatusQueryResult.ResultCode
import com.divineengine.statussaver.model.StatusType
import com.divineengine.statussaver.model.WaClient
import com.divineengine.statussaver.mvvm.DeletionResult
import com.divineengine.statussaver.mvvm.SaveResult
import com.divineengine.statussaver.mvvm.ShareResult
import com.divineengine.statussaver.repository.Repository
import com.divineengine.statussaver.storage.Storage
import com.divineengine.statussaver.storage.StorageDevice
import com.divineengine.statussaver.update.DEFAULT_REPO
import com.divineengine.statussaver.update.DEFAULT_USER
import com.divineengine.statussaver.update.GitHubRelease
import com.divineengine.statussaver.update.UpdateService
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import java.util.EnumMap

class WhatSaveViewModel(
    private val repository: Repository,
    private val updateService: UpdateService,
    private val storage: Storage
) : ViewModel() {

    private val liveDataMap = newStatusesLiveDataMap()
    private val savedLiveDataMap = newStatusesLiveDataMap()

    private val installedClients = MutableLiveData<List<WaClient>>()
    private val storageDevices = MutableLiveData<List<StorageDevice>>()
    private val countries = MutableLiveData<List<Country>>()
    private val selectedCountry = MutableLiveData<Country>()

    private val unlockMessageView = MutableLiveData(false)

    override fun onCleared() {
        super.onCleared()
        liveDataMap.clear()
        savedLiveDataMap.clear()
    }

    fun unlockMessageView() {
        unlockMessageView.value = true
    }

    fun getMessageViewLockObservable(): LiveData<Boolean> = unlockMessageView

    fun getInstalledClients(): LiveData<List<WaClient>> = installedClients

    fun getStorageDevices(): LiveData<List<StorageDevice>> = storageDevices

    fun getCountriesObservable(): LiveData<List<Country>> = countries

    fun getSelectedCountryObservable(): LiveData<Country> = selectedCountry

    fun getCountries() = countries.value ?: arrayListOf()

    fun getSelectedCountry() = selectedCountry.value

    fun getStatuses(type: StatusType): LiveData<StatusQueryResult> {
        return liveDataMap.getOrCreateLiveData(type)
    }

    fun getSavedStatuses(type: StatusType): LiveData<StatusQueryResult> {
        return savedLiveDataMap.getOrCreateLiveData(type)
    }

    fun loadClients() = viewModelScope.launch(IO) {
        val result = getApp().getAllInstalledClients()
        installedClients.postValue(result)
    }

    fun loadStorageDevices() = viewModelScope.launch(IO) {
        storageDevices.postValue(storage.storageVolumes)
    }

    fun loadCountries() = viewModelScope.launch(IO) {
        val result = repository.allCountries()
        countries.postValue(result)
    }

    fun loadSelectedCountry() = viewModelScope.launch(IO) {
        selectedCountry.postValue(repository.defaultCountry())
    }

    fun setSelectedCountry(country: Country) = viewModelScope.launch(IO) {
        repository.defaultCountry(country)
        selectedCountry.postValue(country)
    }

    fun loadStatuses(type: StatusType) = viewModelScope.launch(IO) {
        val liveData = liveDataMap[type]
        if (liveData != null) {
            liveData.postValue(liveData.value?.copy(code = ResultCode.Loading) ?: StatusQueryResult(ResultCode.Loading))
            liveData.postValue(repository.statuses(type))
        }
    }

    fun loadSavedStatuses(type: StatusType) = viewModelScope.launch(IO) {
        val liveData = savedLiveDataMap[type]
        if (liveData != null) {
            liveData.postValue(liveData.value?.copy(code = ResultCode.Loading) ?: StatusQueryResult(ResultCode.Loading))
            liveData.postValue(repository.savedStatuses(type))
        }
    }

    fun reloadAll() {
        StatusType.entries.forEach {
            loadStatuses(it)
            loadSavedStatuses(it)
        }
    }

    fun statusIsSaved(status: Status): LiveData<Boolean> = repository.statusIsSaved(status)

    fun shareStatus(status: Status): LiveData<ShareResult> = liveData(IO) {
        emit(ShareResult(isLoading = true))
        val data = repository.shareStatus(status)
        emit(ShareResult(data = data))
    }

    fun shareStatuses(statuses: List<Status>): LiveData<ShareResult> = liveData(IO) {
        emit(ShareResult(isLoading = true))
        val data = repository.shareStatuses(statuses)
        emit(ShareResult(data = data))
    }

    fun saveStatus(status: Status, saveName: String? = null): LiveData<SaveResult> = liveData(IO) {
        emit(SaveResult(isSaving = true))
        val result = repository.saveStatus(status, saveName)
        emit(SaveResult.single(status, result))
    }

    fun saveStatuses(statuses: List<Status>): LiveData<SaveResult> = liveData(IO) {
        emit(SaveResult(isSaving = true))
        val result = repository.saveStatuses(statuses)
        val savedStatuses = result.keys.toList()
        val savedUris = result.values.toList()
        emit(SaveResult(statuses = savedStatuses, uris = savedUris, saved = result.size))
    }

    fun deleteStatus(status: Status): LiveData<DeletionResult> = liveData(IO) {
        emit(DeletionResult(isDeleting = true))
        val result = repository.deleteStatus(status)
        emit(DeletionResult.single(status, result))
    }

    fun deleteStatuses(statuses: List<Status>): LiveData<DeletionResult> = liveData(IO) {
        emit(DeletionResult(isDeleting = true))
        val result = repository.deleteStatuses(statuses)
        emit(DeletionResult(statuses = statuses, deleted = result))
    }

    fun messageSenders(): LiveData<List<Conversation>> =
        repository.listConversations()

    fun receivedMessages(sender: Conversation): LiveData<List<MessageEntity>> =
        repository.receivedMessages(sender)

    fun deleteMessage(message: MessageEntity) = viewModelScope.launch(IO) {
        repository.removeMessage(message)
    }

    fun deleteConversations(conversations: List<Conversation>, addToBlacklist: Boolean = false) = viewModelScope.launch(IO) {
        repository.deleteConversations(conversations)
        if (addToBlacklist) conversations.forEach {
            getApp().preferences().blacklistMessageSender(it.name)
        }
    }

    fun deleteMessages(messages: List<MessageEntity>) = viewModelScope.launch(IO) {
        repository.removeMessages(messages)
    }

    fun deleteAllMessages() = viewModelScope.launch(IO) {
        repository.clearMessages()
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun createDeleteRequest(context: Context, statuses: List<Status>): LiveData<PendingIntent> = liveData(IO) {
        val uris = statuses.map { it.fileUri }
        if (uris.isNotEmpty()) {
            emit(MediaStore.createDeleteRequest(context.contentResolver, uris))
        }
    }

    fun getLatestUpdate(): LiveData<GitHubRelease> = liveData(IO + SilentHandler) {
        emit(updateService.latestRelease(DEFAULT_USER, DEFAULT_REPO))
    }

    fun downloadUpdate(context: Context, release: GitHubRelease) = viewModelScope.launch(IO + SilentHandler) {
        val downloadRequest = release.getDownloadRequest(context)
        if (downloadRequest != null) {
            val downloadManager = context.getSystemService<DownloadManager>()
            if (downloadManager != null) {
                val lastUpdateId = context.preferences().lastUpdateId
                if (lastUpdateId != -1L) {
                    downloadManager.remove(lastUpdateId)
                }
                context.preferences().lastUpdateId = downloadManager.enqueue(downloadRequest)
            }
        }
    }

    private val SilentHandler = CoroutineExceptionHandler { _, _ -> }
}

internal typealias StatusesLiveData = MutableLiveData<StatusQueryResult>
internal typealias StatusesLiveDataMap = EnumMap<StatusType, StatusesLiveData>

internal fun newStatusesLiveDataMap() = StatusesLiveDataMap(StatusType::class.java)

internal fun StatusesLiveDataMap.getOrCreateLiveData(type: StatusType): StatusesLiveData =
    getOrPut(type) { StatusesLiveData(StatusQueryResult.Idle) }
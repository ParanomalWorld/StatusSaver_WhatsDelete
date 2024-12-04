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
package com.divineengine.statussaver.repository

import android.net.Uri
import androidx.lifecycle.LiveData
import com.divineengine.statussaver.database.Conversation
import com.divineengine.statussaver.database.MessageEntity
import com.divineengine.statussaver.model.Country
import com.divineengine.statussaver.model.ShareData
import com.divineengine.statussaver.model.Status
import com.divineengine.statussaver.model.StatusQueryResult
import com.divineengine.statussaver.model.StatusType

interface Repository {
    fun statusIsSaved(status: Status): LiveData<Boolean>
    suspend fun statuses(type: StatusType): StatusQueryResult
    suspend fun savedStatuses(type: StatusType): StatusQueryResult
    suspend fun shareStatus(status: Status): ShareData
    suspend fun shareStatuses(statuses: List<Status>): ShareData
    suspend fun saveStatus(status: Status, saveName: String?): Uri?
    suspend fun saveStatuses(statuses: List<Status>): Map<Status, Uri>
    suspend fun deleteStatus(status: Status): Boolean
    suspend fun deleteStatuses(statuses: List<Status>): Int
    suspend fun allCountries(): List<Country>
    suspend fun defaultCountry(): Country
    fun defaultCountry(country: Country)
    fun listConversations(): LiveData<List<Conversation>>
    fun receivedMessages(sender: Conversation): LiveData<List<MessageEntity>>
    suspend fun insertMessage(message: MessageEntity): Long
    suspend fun removeMessage(message: MessageEntity)
    suspend fun removeMessages(messages: List<MessageEntity>)
    suspend fun deleteConversations(conversations: List<Conversation>)
    suspend fun clearMessages()
}

class RepositoryImpl(
    private val statusesRepository: StatusesRepository,
    private val countryRepository: CountryRepository,
    private val messageRepository: MessageRepository
) : Repository {

    override fun statusIsSaved(status: Status): LiveData<Boolean> = statusesRepository.statusIsSaved(status)

    override suspend fun statuses(type: StatusType): StatusQueryResult = statusesRepository.statuses(type)

    override suspend fun savedStatuses(type: StatusType): StatusQueryResult = statusesRepository.savedStatuses(type)

    override suspend fun shareStatus(status: Status): ShareData = statusesRepository.share(status)

    override suspend fun shareStatuses(statuses: List<Status>): ShareData = statusesRepository.share(statuses)

    override suspend fun saveStatus(status: Status, saveName: String?): Uri? = statusesRepository.save(status, saveName)

    override suspend fun saveStatuses(statuses: List<Status>): Map<Status, Uri> =
        statusesRepository.save(statuses)

    override suspend fun deleteStatus(status: Status): Boolean = statusesRepository.delete(status)

    override suspend fun deleteStatuses(statuses: List<Status>): Int = statusesRepository.delete(statuses)

    override suspend fun allCountries(): List<Country> = countryRepository.allCountries()

    override suspend fun defaultCountry(): Country = countryRepository.defaultCountry()

    override fun defaultCountry(country: Country) = countryRepository.defaultCountry(country)

    override fun listConversations(): LiveData<List<Conversation>> = messageRepository.listConversations()

    override fun receivedMessages(sender: Conversation): LiveData<List<MessageEntity>> {
        return messageRepository.listMessages(sender.name)
    }

    override suspend fun insertMessage(message: MessageEntity) = messageRepository.insertMessage(message)

    override suspend fun removeMessage(message: MessageEntity) = messageRepository.removeMessage(message)

    override suspend fun removeMessages(messages: List<MessageEntity>) = messageRepository.removeMessages(messages)

    override suspend fun deleteConversations(senders: List<Conversation>) =
        messageRepository.deleteConversations(senders.map { it.name })

    override suspend fun clearMessages() = messageRepository.clearMessages()
}
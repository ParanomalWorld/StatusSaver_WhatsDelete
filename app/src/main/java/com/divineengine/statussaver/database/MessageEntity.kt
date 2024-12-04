/*
 * Copyright (C) 2023 Christians Martínez Alvarado
 * Modified by Jay Kumar, 2024
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
package com.divineengine.statussaver.database

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "received_messages",
    indices = [Index("received_time", "received_from", "message_content", unique = true, name = "messages_index")]
)
data class MessageEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "message_id")
    val id: Int = 0,
    @ColumnInfo(name = "client_package")
    val clientPackage: String?,
    @ColumnInfo(name = "received_time")
    val time: Long,
    @ColumnInfo(name = "received_from")
    val senderName: String,
    @ColumnInfo(name = "message_content")
    val content: String
) : Parcelable
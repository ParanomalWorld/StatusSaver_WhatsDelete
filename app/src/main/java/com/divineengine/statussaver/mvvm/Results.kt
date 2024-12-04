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
package com.divineengine.statussaver.mvvm

import android.net.Uri
import com.divineengine.statussaver.model.ShareData
import com.divineengine.statussaver.model.Status

data class DeletionResult(
    val isDeleting: Boolean = false,
    val statuses: List<Status> = arrayListOf(),
    val deleted: Int = 0
) {
    val isSuccess: Boolean
        get() = statuses.size == deleted

    companion object {
        fun single(status: Status, success: Boolean) =
            DeletionResult(false, listOf(status), if (success) 1 else 0)
    }
}

data class SaveResult(
    val isSaving: Boolean = false,
    val statuses: List<Status> = arrayListOf(),
    val uris: List<Uri> = arrayListOf(),
    val saved: Int = 0
) {
    val isSuccess: Boolean
        get() = statuses.isNotEmpty() && uris.isNotEmpty() && statuses.size == uris.size

    companion object {
        fun single(status: Status, uri: Uri?): SaveResult {
            val statuses = if (uri != null) listOf(status) else arrayListOf()
            val uris = if (uri != null) listOf(uri) else arrayListOf()
            return SaveResult(false, statuses, uris, uris.size)
        }
    }
}

data class ShareResult(
    val isLoading: Boolean = false,
    val data: ShareData = ShareData()
) {
    val isSuccess: Boolean
        get() = data.hasData
}
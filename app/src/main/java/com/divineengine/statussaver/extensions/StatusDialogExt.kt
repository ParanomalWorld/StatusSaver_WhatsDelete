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
package com.divineengine.statussaver.extensions

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.divineengine.statussaver.databinding.DialogProgressBinding

fun Context.createProgressDialog(): Dialog {
    val builder = MaterialAlertDialogBuilder(this)
    val binding = DialogProgressBinding.inflate(LayoutInflater.from(builder.context))
    return builder.setView(binding.root).setCancelable(false).create()
}
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
package com.divineengine.statussaver.dialogs

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.divineengine.statussaver.R
import com.divineengine.statussaver.extensions.blacklistedSenders
import com.divineengine.statussaver.extensions.formattedAsHtml
import com.divineengine.statussaver.extensions.preferences
import com.divineengine.statussaver.extensions.whitelistMessageSender
import com.divineengine.statussaver.getApp

class BlacklistedSenderDialog : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val blacklisted = preferences().blacklistedSenders()?.toTypedArray()
        if (blacklisted.isNullOrEmpty()) {
            return MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.blacklisted_senders)
                .setMessage(R.string.no_blacklisted_senders)
                .setPositiveButton(android.R.string.ok, null)
                .create()
        }
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.blacklisted_senders)
            .setItems(blacklisted) { _: DialogInterface, which: Int ->
                removeItem(blacklisted[which])
            }
            .setPositiveButton(R.string.close_action, null)
            .create()
    }

    private fun removeItem(name: String) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(getString(R.string.remove_x_from_the_blacklist, name).formattedAsHtml())
            .setPositiveButton(R.string.yes_action) { _: DialogInterface, _: Int ->
                getApp().preferences().whitelistMessageSender(name)
            }
            .setNegativeButton(R.string.no_action, null)
            .show()
    }
}
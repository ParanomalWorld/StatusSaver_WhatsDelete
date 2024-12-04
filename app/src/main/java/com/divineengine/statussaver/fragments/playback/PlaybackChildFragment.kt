/*
 * Copyright (C) 2024 Christians Martínez Alvarado
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
package com.divineengine.statussaver.fragments.playback

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.divineengine.statussaver.R
import com.divineengine.statussaver.WhatSaveViewModel
import com.divineengine.statussaver.extensions.createProgressDialog
import com.divineengine.statussaver.extensions.hasR
import com.divineengine.statussaver.extensions.launchSafe
import com.divineengine.statussaver.extensions.showToast
import com.divineengine.statussaver.extensions.startActivitySafe
import com.divineengine.statussaver.fragments.playback.PlaybackFragment.Companion.EXTRA_STATUS
import com.divineengine.statussaver.model.SavedStatus
import com.divineengine.statussaver.model.Status
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import kotlin.properties.Delegates

/**
 * @author Christians M. A. (mardous)
 */
abstract class PlaybackChildFragment(layoutRes: Int) : Fragment(layoutRes), View.OnClickListener {

    protected val viewModel: WhatSaveViewModel by activityViewModel()

    protected abstract val saveButton: MaterialButton
    protected abstract val shareButton: MaterialButton
    protected abstract val deleteButton: MaterialButton

    protected var status: Status by Delegates.notNull()
    protected var isSaved: Boolean = false

    private lateinit var deletionRequestLauncher: ActivityResultLauncher<IntentSenderRequest>
    private val progressDialog by lazy { requireContext().createProgressDialog() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = BundleCompat.getParcelable(requireArguments(), EXTRA_STATUS, Status::class.java)!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        deletionRequestLauncher =
            registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
                if (it.resultCode == Activity.RESULT_OK) {
                    viewModel.reloadAll()
                    showToast(R.string.deletion_success)
                }
            }
    }

    override fun onStart() {
        super.onStart()
        viewModel.statusIsSaved(status).observe(viewLifecycleOwner) { isSaved ->
            this.isSaved = (status is SavedStatus) || isSaved
            if (this.isSaved) {
                saveButton.setText(R.string.saved_label)
                saveButton.setIconResource(R.drawable.ic_round_check_24dp)
            } else {
                saveButton.setText(R.string.save_action)
                saveButton.setIconResource(R.drawable.ic_save_alt_24dp)
            }
        }
        saveButton.setOnClickListener(this)
        shareButton.setOnClickListener(this)
        deleteButton.setOnClickListener(this)
        deleteButton.isEnabled = (status is SavedStatus)
    }

    override fun onClick(v: View) {
        when (v) {
            saveButton -> {
                if (!isSaved) {
                    viewModel.saveStatus(status).observe(viewLifecycleOwner) { result ->
                        if (result.isSuccess) {
                            showToast(R.string.saved_successfully)
                        } else if (!result.isSaving) {
                            showToast(R.string.failed_to_save)
                        }
                    }
                }
            }

            shareButton -> {
                viewModel.shareStatus(status).observe(viewLifecycleOwner) { result ->
                    if (result.isLoading) {
                        progressDialog.show()
                    } else {
                        progressDialog.dismiss()
                        if (result.isSuccess) {
                            startActivitySafe(result.data.createIntent(requireContext()))
                        }
                    }
                }
            }

            deleteButton -> {
                if (hasR()) {
                    viewModel.createDeleteRequest(requireContext(), listOf(status))
                        .observe(viewLifecycleOwner) {
                            deletionRequestLauncher.launchSafe(
                                IntentSenderRequest.Builder(it).build()
                            )
                        }
                } else {
                    MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.delete_saved_status_title)
                        .setMessage(R.string.this_saved_status_will_be_permanently_deleted)
                        .setPositiveButton(R.string.delete_action) { _: DialogInterface, _: Int ->
                            viewModel.deleteStatus(status).observe(viewLifecycleOwner) { result ->
                                if (result.isSuccess) {
                                    showToast(R.string.deletion_success)
                                    viewModel.reloadAll()
                                } else {
                                    showToast(R.string.deletion_failed)
                                }
                            }
                        }
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
                }
            }
        }
    }
}
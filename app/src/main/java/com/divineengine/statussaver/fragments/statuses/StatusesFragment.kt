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
package com.divineengine.statussaver.fragments.statuses

import android.app.Activity
import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isVisible
import androidx.core.view.setPadding
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.divineengine.statussaver.R
import com.divineengine.statussaver.WhatSaveViewModel
import com.divineengine.statussaver.adapter.StatusAdapter
import com.divineengine.statussaver.databinding.FragmentStatusesPageBinding
import com.divineengine.statussaver.extensions.createProgressDialog
import com.divineengine.statussaver.extensions.dip
import com.divineengine.statussaver.extensions.doOnPageSelected
import com.divineengine.statussaver.extensions.getPreferredClient
import com.divineengine.statussaver.extensions.hasR
import com.divineengine.statussaver.extensions.isNullOrEmpty
import com.divineengine.statussaver.extensions.isQuickDeletion
import com.divineengine.statussaver.extensions.launchSafe
import com.divineengine.statussaver.extensions.preferences
import com.divineengine.statussaver.extensions.primaryColor
import com.divineengine.statussaver.extensions.requestPermissions
import com.divineengine.statussaver.extensions.requestView
import com.divineengine.statussaver.extensions.serializable
import com.divineengine.statussaver.extensions.showToast
import com.divineengine.statussaver.extensions.startActivitySafe
import com.divineengine.statussaver.fragments.SectionFragment
import com.divineengine.statussaver.fragments.base.BaseFragment
import com.divineengine.statussaver.fragments.binding.StatusesPageBinding
import com.divineengine.statussaver.fragments.playback.PlaybackFragmentArgs
import com.divineengine.statussaver.interfaces.IPermissionChangeListener
import com.divineengine.statussaver.interfaces.IScrollable
import com.divineengine.statussaver.interfaces.IStatusCallback
import com.divineengine.statussaver.model.Status
import com.divineengine.statussaver.model.StatusQueryResult
import com.divineengine.statussaver.model.StatusType
import com.divineengine.statussaver.mvvm.DeletionResult
import com.divineengine.statussaver.mvvm.SaveResult
import org.koin.androidx.viewmodel.ext.android.activityViewModel

/**
 * @author Christians Martínez Alvarado (mardous)
 */
abstract class StatusesFragment : BaseFragment(R.layout.fragment_statuses_page),
    View.OnClickListener,
    OnRefreshListener,
    IScrollable,
    IPermissionChangeListener,
    IStatusCallback {

    private var _binding: StatusesPageBinding? = null
    protected val binding get() = _binding!!

    protected val viewModel by activityViewModel<WhatSaveViewModel>()
    protected lateinit var deletionRequestLauncher: ActivityResultLauncher<IntentSenderRequest>
    protected lateinit var statusType: StatusType
    protected var statusAdapter: StatusAdapter? = null

    private val progressDialog by lazy { requireContext().createProgressDialog() }
    private val sectionFragment: SectionFragment
        get() = parentFragment as SectionFragment

    private val lastResult: StatusQueryResult?
        get() = viewModel.getStatuses(statusType).value

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arguments = arguments
        if (arguments != null) {
            statusType = arguments.serializable(EXTRA_TYPE, StatusType::class)!!
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = StatusesPageBinding(FragmentStatusesPageBinding.bind(view)).apply {
            swipeRefreshLayout.setOnRefreshListener(this@StatusesFragment)
            swipeRefreshLayout.setColorSchemeColors(view.context.primaryColor())

            recyclerView.setPadding(dip(R.dimen.status_item_margin))
            recyclerView.layoutManager =
                GridLayoutManager(requireActivity(), resources.getInteger(R.integer.statuses_grid_span_count))
            recyclerView.adapter = onCreateAdapter().apply {
                registerAdapterDataObserver(adapterDataObserver)
            }.also { newStatusAdapter ->
                statusAdapter = newStatusAdapter
            }
        }

        deletionRequestLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.reloadAll()
                showToast(R.string.deletion_success)
            }
        }

        binding.emptyButton.setOnClickListener(this)
        sectionFragment.getViewPager().doOnPageSelected(viewLifecycleOwner) {
            statusAdapter?.finishActionMode()
        }
    }

    protected fun data(result: StatusQueryResult) {
        statusAdapter?.statuses = result.statuses
        binding.swipeRefreshLayout.isRefreshing = result.isLoading
        if (result.code.titleRes != 0) {
            binding.emptyTitle.text = getString(result.code.titleRes)
            binding.emptyTitle.isVisible = true
        } else {
            binding.emptyTitle.isVisible = false
        }
        if (result.code.descriptionRes != 0) {
            binding.emptyText.text = getString(result.code.descriptionRes)
            binding.emptyText.isVisible = true
        } else {
            binding.emptyText.isVisible = false
        }
        if (result.code.buttonTextRes != 0) {
            binding.emptyButton.text = getString(result.code.buttonTextRes)
            binding.emptyButton.isVisible = true
        } else {
            binding.emptyButton.isVisible = false
        }
    }

    protected abstract fun onCreateAdapter(): StatusAdapter

    override fun scrollToTop() {
        binding.recyclerView.scrollToPosition(0)
    }

    override fun onRefresh() {
        onLoadStatuses(statusType)
    }

    override fun onStart() {
        super.onStart()
        statusesActivity.addPermissionsChangeListener(this)
    }

    override fun onStop() {
        super.onStop()
        statusesActivity.removePermissionsChangeListener(this)
    }

    override fun onClick(view: View) {
        if (view == binding.emptyButton) {
            val resultCode = lastResult?.code
            if (resultCode != StatusQueryResult.ResultCode.Loading) {
                when (resultCode) {
                    StatusQueryResult.ResultCode.PermissionError -> requestPermissions()
                    StatusQueryResult.ResultCode.NotInstalled -> requireActivity().finish()
                    StatusQueryResult.ResultCode.NoStatuses -> requireContext().getPreferredClient()?.let {
                        startActivitySafe(it.getLaunchIntent(requireContext().packageManager))
                    }

                    else -> onLoadStatuses(statusType)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        statusAdapter?.unregisterAdapterDataObserver(adapterDataObserver)
        statusAdapter = null
    }

    private val adapterDataObserver = object : AdapterDataObserver() {
        override fun onChanged() {
            binding.emptyView.isVisible = statusAdapter.isNullOrEmpty()
        }
    }

    override fun permissionsStateChanged(hasPermissions: Boolean) {
        viewModel.reloadAll()
    }

    override fun multiSelectionItemClick(item: MenuItem, selection: List<Status>) = requestView {
        when (item.itemId) {
            R.id.action_share -> {
                viewModel.shareStatuses(selection).observe(viewLifecycleOwner) {
                    if (it.isLoading) {
                        progressDialog.show()
                    } else {
                        progressDialog.dismiss()
                        if (it.isSuccess) {
                            startActivitySafe(it.data.createIntent(requireContext()))
                        }
                    }
                }
            }

            R.id.action_save -> {
                viewModel.saveStatuses(selection).observe(viewLifecycleOwner) {
                    processSaveResult(it)
                }
            }

            R.id.action_delete -> {
                if (hasR()) {
                    viewModel.createDeleteRequest(requireContext(), selection).observe(viewLifecycleOwner) {
                        deletionRequestLauncher.launchSafe(IntentSenderRequest.Builder(it).build())
                    }
                } else {
                    if (!preferences().isQuickDeletion()) {
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.delete_saved_statuses_title)
                            .setMessage(
                                getString(R.string.x_saved_statuses_will_be_permanently_deleted, selection.size)
                            )
                            .setPositiveButton(R.string.delete_action) { _: DialogInterface, _: Int ->
                                viewModel.deleteStatuses(selection).observe(viewLifecycleOwner) {
                                    processDeletionResult(it)
                                }
                            }
                            .setNegativeButton(android.R.string.cancel, null)
                            .show()
                    } else {
                        viewModel.deleteStatuses(selection).observe(viewLifecycleOwner) {
                            processDeletionResult(it)
                        }
                    }
                }
            }
        }
    }

    override fun previewStatusesClick(statuses: List<Status>, startPosition: Int) {
        findNavController().navigate(
            R.id.playbackFragment,
            PlaybackFragmentArgs.Builder(statuses.toTypedArray(), startPosition).build()
                .toBundle()
        )
    }

    protected abstract fun onLoadStatuses(type: StatusType)

    private fun processSaveResult(result: SaveResult) = requestView { view ->
        if (result.isSaving) {
            Snackbar.make(view, R.string.saving_status, Snackbar.LENGTH_SHORT).show()
        } else {
            if (result.isSuccess) {
                if (result.saved == 1) {
                    Snackbar.make(view, R.string.saved_successfully, Snackbar.LENGTH_SHORT).show()
                } else {
                    Snackbar.make(view, getString(R.string.saved_x_statuses, result.saved), Snackbar.LENGTH_SHORT)
                        .show()
                }
                viewModel.reloadAll()
            } else {
                Snackbar.make(view, R.string.failed_to_save, Snackbar.LENGTH_SHORT).show()
            }
        }
        statusAdapter?.isSavingContent = result.isSaving
    }

    private fun processDeletionResult(result: DeletionResult) = requestView { view ->
        if (result.isDeleting) {
            Snackbar.make(view, R.string.deleting_please_wait, Snackbar.LENGTH_SHORT).show()
        } else if (result.isSuccess) {
            Snackbar.make(view, R.string.deletion_success, Snackbar.LENGTH_SHORT).show()
            viewModel.reloadAll()
        } else {
            Snackbar.make(view, R.string.deletion_failed, Snackbar.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_TYPE = "extra_type"
    }
}
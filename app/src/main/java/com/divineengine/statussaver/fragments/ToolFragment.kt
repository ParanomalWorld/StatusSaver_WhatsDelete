/*
 * Copyright (C) 2023 Christians Martínez Alvarado
 *
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
package com.divineengine.statussaver.fragments

import android.app.Activity
import android.app.KeyguardManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.core.view.doOnPreDraw
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.google.android.material.transition.MaterialFadeThrough
import com.divineengine.statussaver.R
import com.divineengine.statussaver.WhatSaveViewModel
import com.divineengine.statussaver.databinding.FragmentToolBinding
import com.divineengine.statussaver.extensions.isMessageViewEnabled
import com.divineengine.statussaver.extensions.isNotificationListener
import com.divineengine.statussaver.extensions.launchSafe
import com.divineengine.statussaver.extensions.preferences
import com.divineengine.statussaver.fragments.base.BaseFragment
import com.divineengine.statussaver.logToolView
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ToolFragment : BaseFragment(R.layout.fragment_tool) {

    private val viewModel: WhatSaveViewModel by activityViewModel()
    private val keyguardManager: KeyguardManager by lazy { requireContext().getSystemService()!! }
    private lateinit var credentialsRequestLauncher: ActivityResultLauncher<Intent>

    private var _binding: FragmentToolBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentToolBinding.bind(view)
        binding.msgANumber.setOnClickListener {
            logToolView("MessageFragment", "Message a number")
            findNavController().navigate(R.id.messageFragment)
        }
        binding.messageView.setOnClickListener {
            logToolView("ConversationListFragment", "Message view")
            if (requireContext().isNotificationListener()) {
                openMessageView()
            } else {
                findNavController().navigate(R.id.savedFragment)
            }
        }

        statusesActivity.setSupportActionBar(binding.toolbar)
        credentialsRequestLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                viewModel.unlockMessageView()
            } else {
                viewModel.getMessageViewLockObservable().removeObserver(credentialObserver)
            }
        }

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        enterTransition = MaterialFadeThrough().addTarget(view)
        reenterTransition = MaterialFadeThrough().addTarget(view)
    }

    private fun openMessageView() {
        viewModel.getMessageViewLockObservable().observe(viewLifecycleOwner, credentialObserver)
    }

    @Suppress("DEPRECATION")
    private val credentialObserver = Observer<Boolean> { isUnlocked ->
        if (isUnlocked || !preferences().isMessageViewEnabled) {
            findNavController().navigate(R.id.conversationsFragment)
        } else {
            val credentialsRequestIntel = keyguardManager.createConfirmDeviceCredentialIntent(
                getString(R.string.message_view),
                getString(R.string.confirm_device_credentials)
            )
            if (credentialsRequestIntel != null) {
                credentialsRequestLauncher.launchSafe(credentialsRequestIntel)
            } else {
                viewModel.unlockMessageView()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
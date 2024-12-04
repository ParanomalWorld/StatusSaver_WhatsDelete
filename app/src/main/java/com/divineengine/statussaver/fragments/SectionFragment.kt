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
package com.divineengine.statussaver.fragments

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.core.view.doOnPreDraw
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.android.material.transition.MaterialFadeThrough
import com.divineengine.statussaver.R
import com.divineengine.statussaver.activities.setupWhatsAppMenuItem
import com.divineengine.statussaver.adapter.PagerAdapter
import com.divineengine.statussaver.databinding.FragmentStatusesBinding
import com.divineengine.statussaver.extensions.PREFERENCE_DEFAULT_CLIENT
import com.divineengine.statussaver.extensions.doOnPageSelected
import com.divineengine.statussaver.extensions.findCurrentFragment
import com.divineengine.statussaver.extensions.preferences
import com.divineengine.statussaver.fragments.statuses.StatusesFragment
import com.divineengine.statussaver.fragments.base.BaseFragment
import com.divineengine.statussaver.fragments.statuses.HomeStatusesFragment
import com.divineengine.statussaver.fragments.statuses.SavedStatusesFragment
import com.divineengine.statussaver.interfaces.IScrollable
import com.divineengine.statussaver.model.StatusType

/**
 * @author Christians Martínez Alvarado (mardous)
 */
abstract class SectionFragment : BaseFragment(R.layout.fragment_statuses),
    SharedPreferences.OnSharedPreferenceChangeListener, IScrollable {

    private var _binding: FragmentStatusesBinding? = null
    private val binding get() = _binding!!

    private lateinit var tabLayoutMediator: TabLayoutMediator
    protected var pagerAdapter: PagerAdapter? = null
    protected var currentType: StatusType
        get() = StatusType.entries.first { type -> type.ordinal == binding.viewPager.currentItem }
        set(type) {
            binding.viewPager.currentItem = type.ordinal
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatusesBinding.bind(view).apply {
            appBar.statusBarForeground = MaterialShapeDrawable.createWithElevationOverlay(requireContext())
            viewPager.doOnPageSelected(viewLifecycleOwner) {
                onBackPressedCallback.isEnabled = currentType != StatusType.IMAGE
            }
            viewPager.adapter = onCreatePagerAdapter().also { newPagerAdapter ->
                pagerAdapter = newPagerAdapter
            }
            viewPager.offscreenPageLimit = pagerAdapter!!.itemCount - 1
        }.also { viewBinding ->
            tabLayoutMediator =
                TabLayoutMediator(viewBinding.tabLayout, viewBinding.viewPager) { tab: TabLayout.Tab, position: Int ->
                    tab.text = pagerAdapter?.getPageTitle(position)
                }.also { mediator ->
                    mediator.attach()
                }
        }

        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }
        enterTransition = MaterialFadeThrough().addTarget(view)
        reenterTransition = MaterialFadeThrough().addTarget(view)

        statusesActivity.setSupportActionBar(binding.toolbar)
        statusesActivity.onBackPressedDispatcher.addCallback(viewLifecycleOwner, onBackPressedCallback)

        preferences().registerOnSharedPreferenceChangeListener(this)
    }

    protected abstract fun onCreatePagerAdapter(): PagerAdapter

    override fun onSharedPreferenceChanged(preferences: SharedPreferences?, key: String?) {
        if (PREFERENCE_DEFAULT_CLIENT == key) {
            _binding?.apply { toolbar.menu?.setupWhatsAppMenuItem(requireActivity()) }
        }
    }

    override fun onDestroyView() {
        preferences().unregisterOnSharedPreferenceChangeListener(this)
        super.onDestroyView()
        tabLayoutMediator.detach()
        _binding = null
    }

    override fun scrollToTop() {
        val currentFragment = binding.viewPager.findCurrentFragment(childFragmentManager)
        if (currentFragment is StatusesFragment) {
            currentFragment.scrollToTop()
        }
    }

    internal fun getViewPager() = binding.viewPager

    private val onBackPressedCallback = object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
            currentType = StatusType.IMAGE
        }
    }
}

class HomeSectionFragment : SectionFragment() {
    override fun onCreatePagerAdapter(): PagerAdapter {
        return PagerAdapter(this, HomeStatusesFragment::class.java.name)
    }
}

class SavedSectionFragment : SectionFragment() {
    override fun onCreatePagerAdapter(): PagerAdapter {
        return PagerAdapter(this, SavedStatusesFragment::class.java.name)
    }
}
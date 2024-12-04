/*
 * Copyright (C) 2024 Christians Martínez Alvarado
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

package com.divineengine.statussaver.adapter

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.divineengine.statussaver.fragments.playback.PlaybackFragment
import com.divineengine.statussaver.fragments.playback.image.ImageFragment
import com.divineengine.statussaver.fragments.playback.video.VideoFragment
import com.divineengine.statussaver.model.Status
import com.divineengine.statussaver.model.StatusType

/**
 * @author Christians M. A. (mardous)
 */
class PlaybackAdapter(
    private val fragment: Fragment,
    private val statuses: List<Status>
) : FragmentStateAdapter(fragment) {

    private val fragmentFactory: FragmentFactory = fragment
        .childFragmentManager
        .fragmentFactory

    override fun createFragment(position: Int): Fragment {
        val status = statuses[position]
        val playbackType = PlaybackFragmentType.entries.first {
            it.type == status.type
        }
        return fragmentFactory.instantiate(
            fragment.requireContext().classLoader,
            playbackType.className
        ).apply { arguments = bundleOf(PlaybackFragment.EXTRA_STATUS to status) }
    }

    override fun getItemCount(): Int = statuses.size

    enum class PlaybackFragmentType(val className: String, val type: StatusType) {
        IMAGE_VIEWER(ImageFragment::class.java.name, StatusType.IMAGE),
        VIDEO_PLAYER(VideoFragment::class.java.name, StatusType.VIDEO)
    }
}
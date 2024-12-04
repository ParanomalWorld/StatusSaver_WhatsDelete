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

import android.os.Bundle
import android.view.View
import com.divineengine.statussaver.adapter.StatusAdapter
import com.divineengine.statussaver.model.StatusQueryResult
import com.divineengine.statussaver.model.StatusType

/**
 * @author Christians Martínez Alvarado (mardous)
 */
class SavedStatusesFragment : StatusesFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getSavedStatuses(statusType).apply {
            observe(viewLifecycleOwner) { result ->
                data(result)
            }
        }.also { liveData ->
            if (liveData.value == StatusQueryResult.Idle) {
                onLoadStatuses(statusType)
            }
        }
    }

    override fun onCreateAdapter(): StatusAdapter =
        StatusAdapter(
            requireActivity(),
            this,
            isSaveEnabled = false,
            isDeleteEnabled = true,
            isWhatsAppIconEnabled = false
        )

    override fun onLoadStatuses(type: StatusType) {
        viewModel.loadSavedStatuses(type)
    }
}
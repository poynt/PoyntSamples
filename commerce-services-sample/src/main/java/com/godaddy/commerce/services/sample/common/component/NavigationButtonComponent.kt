package com.godaddy.commerce.services.sample.common.component

import androidx.annotation.IdRes
import com.godaddy.commerce.services.sample.common.binding.RecyclerAdapterItem
import com.godaddy.commerce.services.sample.databinding.ComponentNavigationItemBinding

data class NavigationButtonComponent(
    val title: String,
    @IdRes val navigateToId: Int
) : RecyclerAdapterItem<String, ComponentNavigationItemBinding>(title)
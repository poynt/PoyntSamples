@file:Suppress("NOTHING_TO_INLINE")

package com.godaddy.commerce.services.sample.inventory

import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.godaddy.commercecore.models.InventoryInfo
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.binding.RecyclerAdapterItem
import com.godaddy.commerce.services.sample.databinding.InventoryItemBinding

data class InventoryRecyclerItem(
    override val item: Any,
    override val onBinding: (binding: InventoryItemBinding, position: Int, getItem: () -> Any) -> Unit = { _, _, _ -> },
) : RecyclerAdapterItem<Any, InventoryItemBinding>(item, onBinding)


inline fun Any.mapToUiItems(): InventoryRecyclerItem {
    return InventoryRecyclerItem(this) { binding, _, getItem ->
        binding.updateBt.setOnClickListener {
            it.findNavController().navigate(
                resId = R.id.inventoryUpdateFragment,
//                args = bundleOf("id" to getItem().level?.inventoryLevelId)
            )
        }
    }
}
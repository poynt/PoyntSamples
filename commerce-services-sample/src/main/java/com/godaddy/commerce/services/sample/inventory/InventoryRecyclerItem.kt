@file:Suppress("NOTHING_TO_INLINE")

package com.godaddy.commerce.services.sample.inventory

import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.godaddy.commerce.inventory.models.InventoryBundled
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.binding.RecyclerAdapterItem
import com.godaddy.commerce.services.sample.databinding.InventoryItemBinding

data class InventoryRecyclerItem(
    override val item: InventoryBundled,
    override val onBinding: (binding: InventoryItemBinding, position: Int, getItem: () -> InventoryBundled) -> Unit = { _, _, _ -> },
) : RecyclerAdapterItem<InventoryBundled, InventoryItemBinding>(item, onBinding)


inline fun InventoryBundled.mapToUiItems(): InventoryRecyclerItem {
    return InventoryRecyclerItem(this) { binding, _, getItem ->
        binding.updateBt.setOnClickListener {
            it.findNavController().navigate(
                resId = R.id.inventoryUpdateFragment,
                args = bundleOf("id" to getItem().level?.inventoryLevelId)
            )
        }
    }
}
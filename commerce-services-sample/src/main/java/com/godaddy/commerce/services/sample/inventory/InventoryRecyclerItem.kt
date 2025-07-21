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
) : RecyclerAdapterItem<Any, InventoryItemBinding>(item, BINDING_HANDLER) {

    companion object {
        private val BINDING_HANDLER: (InventoryItemBinding, Int, () -> Any) -> Unit = { binding, _, getItem ->
            binding.updateBt.setOnClickListener {
                it.findNavController().navigate(
                    resId = R.id.inventoryUpdateFragment,
                    // args = bundleOf("id" to getItem().level?.inventoryLevelId)
                )
            }
        }
    }
}
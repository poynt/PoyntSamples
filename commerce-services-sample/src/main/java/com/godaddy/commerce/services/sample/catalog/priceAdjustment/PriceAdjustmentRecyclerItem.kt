@file:Suppress("NOTHING_TO_INLINE")

package com.godaddy.commerce.services.sample.catalog.priceAdjustment

import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.godaddy.commerce.priceadjustments.models.PriceAdjustment
import com.godaddy.commerce.priceadjustments.models.PriceAdjustmentAssociationItemsInner
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.binding.RecyclerAdapterItem
import com.godaddy.commerce.services.sample.databinding.PriceAdjustmentAssociationItemBinding
import com.godaddy.commerce.services.sample.databinding.PriceAdjustmentItemBinding
import java.util.*

data class PriceAdjustmentRecyclerItem(
    override val item: PriceAdjustment,
    override val onBinding: (binding: PriceAdjustmentItemBinding, position: Int, getItem: () -> PriceAdjustment) -> Unit = { _, _, _ -> },
) : RecyclerAdapterItem<PriceAdjustment, PriceAdjustmentItemBinding>(item, onBinding)

inline fun PriceAdjustment.mapToUiItems(): PriceAdjustmentRecyclerItem {
    return PriceAdjustmentRecyclerItem(this) { binding, _, getItem ->
        binding.updateBt.setOnClickListener {
            it.findNavController().navigate(
                R.id.priceAdjustmentUpdateFragment, bundleOf(
                    "id" to getItem().id?.toString(),
                    "type" to getItem().type
                )
            )
        }
    }
}

data class PriceAdjustmentAssociationRecyclerItem(
    override val item: PriceAdjustmentAssociationItemsInner,
    override val onBinding: (binding: PriceAdjustmentAssociationItemBinding, position: Int, getItem: () -> PriceAdjustmentAssociationItemsInner) -> Unit = { _, _, _ -> }
) : RecyclerAdapterItem<PriceAdjustmentAssociationItemsInner, PriceAdjustmentAssociationItemBinding>(
    item,
    onBinding
)


inline fun PriceAdjustmentAssociationItemsInner.mapToUiItems(crossinline deleteItemId: (String) -> Unit): PriceAdjustmentAssociationRecyclerItem {
    return PriceAdjustmentAssociationRecyclerItem(this) { binding, _, getItem ->
        binding.deleteBtn.setOnClickListener {
            getItem().id?.let(deleteItemId)
        }
    }
}
@file:Suppress("NOTHING_TO_INLINE")

package com.godaddy.commerce.services.sample.catalog.tax

import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.binding.RecyclerAdapterItem
import com.godaddy.commerce.services.sample.databinding.TaxAssociationItemBinding
import com.godaddy.commerce.services.sample.databinding.TaxItemBinding
import com.godaddy.commerce.taxes.models.AssociationItems
import com.godaddy.commerce.taxes.models.Tax
import java.util.*

data class TaxRecyclerItem(
    override val item: Tax,
    override val onBinding: (binding: TaxItemBinding, position: Int, getItem: () -> Tax) -> Unit = { _, _, _ -> },
) : RecyclerAdapterItem<Tax, TaxItemBinding>(item, onBinding)

inline fun Tax.mapToUiItems(): TaxRecyclerItem {
    return TaxRecyclerItem(this) { binding, _, getItem ->
        binding.updateBt.setOnClickListener {
            it.findNavController().navigate(
                resId = R.id.taxUpdateFragment,
                args = bundleOf("id" to getItem().id?.toString())
            )
        }
    }
}

data class TaxAssociationRecyclerItem(
    override val item: AssociationItems,
    override val onBinding: (binding: TaxAssociationItemBinding, position: Int, getItem: () -> AssociationItems) -> Unit = { _, _, _ -> }
) : RecyclerAdapterItem<AssociationItems, TaxAssociationItemBinding>(item, onBinding){
    override fun getChangePayload(other: RecyclerAdapterItem<AssociationItems, TaxAssociationItemBinding>): Any = Unit
}


inline fun AssociationItems.mapToUiItems(crossinline deleteItemId: (UUID) -> Unit): TaxAssociationRecyclerItem {
    return TaxAssociationRecyclerItem(this) { binding, _, getItem ->
        binding.deleteBtn.setOnClickListener {
            getItem().id?.let(deleteItemId)
        }
    }
}
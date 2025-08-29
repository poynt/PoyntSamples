@file:Suppress("NOTHING_TO_INLINE")

package com.godaddy.commerce.services.sample.catalog.tax

import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.godaddy.commerce.catalog.model.CatalogTax
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.binding.RecyclerAdapterItem
import com.godaddy.commerce.services.sample.databinding.TaxItemBinding

data class TaxRecyclerItem(
    override val item: CatalogTax,
    val amountString: String = if (item.tax.amount != null) (
            item.tax.amount?.amount?.value.toString() + ' ' + item.tax.amount?.amount?.currencyCode
    ) else "N/A",
    val percentageString: String = if (item.tax.amount == null) (
            item.tax.percentage?.percentage.toString() + '%')
    else "N/A",

    val classificationCount: String = item.tax.classifications.orEmpty().size.toString(),
    val overrideCount: String = item.tax.overrides.orEmpty().size.toString(),

    override val onBinding: (binding: TaxItemBinding, position: Int, getItem: () -> CatalogTax) -> Unit = { _, _, _ -> }

    ) : RecyclerAdapterItem<CatalogTax, TaxItemBinding>(item, onBinding)

inline fun CatalogTax.mapToUiItems(): TaxRecyclerItem {
    return TaxRecyclerItem(this) { binding, _, getItem ->
        binding.updateBt.setOnClickListener {
            it.findNavController().navigate(
                resId = R.id.taxUpdateFragment,
                args = bundleOf("id" to getItem().tax.id)
            )
        }
    }
}

@file:Suppress("NOTHING_TO_INLINE")

package com.godaddy.commerce.services.sample.catalog.product

import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.godaddy.commerce.catalog.models.Product
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.binding.RecyclerAdapterItem
import com.godaddy.commerce.services.sample.databinding.ProductItemBinding

data class ProductRecyclerItem(
    override val item: Product,
    override val onBinding: (binding: ProductItemBinding, position: Int, getItem: () -> Product) -> Unit = { _, _, _ -> },
) : RecyclerAdapterItem<Product, ProductItemBinding>(item, onBinding)


inline fun Product.mapToUiItems(): ProductRecyclerItem {
    return ProductRecyclerItem(this) { binding, _, getItem ->
        binding.updateBt.setOnClickListener {
            it.findNavController().navigate(
                resId = R.id.productUpdateFragment,
                args = bundleOf("id" to getItem().productId?.toString())
            )
        }
    }
}
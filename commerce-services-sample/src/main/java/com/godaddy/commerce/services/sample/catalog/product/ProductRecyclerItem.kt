@file:Suppress("NOTHING_TO_INLINE")

package com.godaddy.commerce.services.sample.catalog.product

import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.binding.RecyclerAdapterItem
import com.godaddy.commerce.services.sample.databinding.ProductItemBinding
import com.godaddy.commercecore.models.InventoryInfo
import com.godaddy.commercecore.models.PricingInfo
import com.godaddy.commercecore.models.SellableProduct

data class ProductRecyclerItem(
    override val item: CatalogProduct,
    val priceInfo: PricingInfo? = item.product.pricingInfos?.firstOrNull(),
    val firstSellableProduct: SellableProduct? = item.product.sellableProducts?.firstOrNull(),
    val inventoryInfo: InventoryInfo? = firstSellableProduct?.inventoryInfos?.firstOrNull(),
    override val onBinding: (binding: ProductItemBinding, position: Int, getItem: () -> Any) -> Unit = { _, _, _ -> },
) : RecyclerAdapterItem<CatalogProduct, ProductItemBinding>(item, onBinding)


inline fun CatalogProduct.mapToUiItems(): ProductRecyclerItem {
    return ProductRecyclerItem(this) { binding, _, _ ->
        binding.updateBt.setOnClickListener {
            it.findNavController().navigate(
                resId = R.id.productUpdateFragment,
                args = bundleOf(
                    "id" to product.id.toString(),
                )
            )
        }
    }
}

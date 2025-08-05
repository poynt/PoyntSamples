@file:Suppress("NOTHING_TO_INLINE")

package com.godaddy.commerce.services.sample.catalog.product

import android.view.View
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
    val isSelected: Boolean,
    val priceInfo: PricingInfo? = item.product.pricingInfos?.firstOrNull(),
    val firstSellableProduct: SellableProduct? = item.product.sellableProducts?.firstOrNull(),
    val inventoryInfo: InventoryInfo? = firstSellableProduct?.inventoryInfos?.firstOrNull(),
    override val onBinding: (binding: ProductItemBinding, position: Int, getItem: () -> Any) -> Unit = { _, _, _ -> },
) : RecyclerAdapterItem<CatalogProduct, ProductItemBinding>(item, onBinding)

inline fun CatalogProduct.mapToProductUiItems(): ProductRecyclerItem {
    return ProductRecyclerItem(this, isSelected = false) { binding, _, _ ->
        binding.updateBt.setOnClickListener {
            it.findNavController().navigate(
                resId = R.id.productUpdateFragment,
                args = bundleOf("id" to product.id.toString())
            )
        }
        binding.selectBt.visibility = View.GONE
        binding.deleteButton.visibility = View.GONE
    }
}

inline fun CatalogProduct.mapToCategoryUiItems(
    isSelected: Boolean = false,
    noinline onDeleteClicked: (CatalogProduct) -> Unit,
    noinline onSelectClicked: (CatalogProduct, Boolean) -> Unit,
): ProductRecyclerItem {
    return ProductRecyclerItem(
        item = this,
        isSelected = isSelected
    ) { binding, _, _ ->
        binding.updateBt.visibility = View.GONE

        binding.selectBt.setOnClickListener {
            onSelectClicked.invoke(this@mapToCategoryUiItems, true)
        }
        binding.selectBt.visibility = if (!isSelected) View.VISIBLE else View.GONE

        binding.deleteButton.visibility = if (isSelected) View.VISIBLE else View.GONE

        binding.deleteButton.setOnClickListener {
            onDeleteClicked.invoke(this@mapToCategoryUiItems)
        }
    }
}
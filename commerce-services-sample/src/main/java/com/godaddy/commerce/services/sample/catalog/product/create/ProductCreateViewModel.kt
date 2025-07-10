@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.product.create

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.ProductParams
import com.godaddy.commerce.catalog.model.CatalogCategoryTreeNodes
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commercecore.models.InventoryInfo
import com.godaddy.commercecore.models.Money
import com.godaddy.commercecore.models.PricingInfo
import com.godaddy.commercecore.models.Product
import com.godaddy.commercecore.models.SellableProduct
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine

//The fields that are important to be exemplified on the sample are:
//•	Label
//•	Price
//•	SKU Code
//•	Inventory Info
//
//The Inventory API works in conjunction with the Product API to allow Inventory Management of a Merchant Product.
// Client applications are allowed to enable it for Product and update its quantity as needed.
//The Domain Model related to the Inventory API is the InventoryInfo. It has a direct relationship
// with the SellableProduct model.
//The fields that are important to be exemplified for Inventoryinfo are:

//•	Quantity
//•	Threshold

//The Create product Screen should exemplify how to create Products.
//1.	Allow user to input all important fields.
//2.	Display a Snackbar with any Error message given from CS when Creation Fails.
//3.	Short Label field should be filled with First 5 Chars from Label
//4.	Disable Price Override field should be filled based on whether user informed Price.
//5.	Enable Inventory Tracking field should be filled based on whether user informed Quantity.



class ProductCreateViewModel : CommonViewModel<ProductCreateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)

    fun onProductLabelChanged(value: String) {
        update { copy(label = value) }
    }
    fun onProductPriceChanged(value: Money?){
        update { copy(price = value)}
    }
    fun onProductSalePriceChanged(value: Money?){
        update { copy(salePrice = value)}
    }
    fun onProductQuantityChanged(value: Int?){
        update { copy(quantity = value)}
    }
    fun onProductThresholdChanged(value: Int?){
        update { copy(quantity = value)}
    }
    fun createProduct() {
        execute {
            val label = requireNotNull(state.label)
            val shortLabel = requireNotNull(label.take(5))
            val disablePriceOverride = (state.price == null)
            val pricingInfos = listOf(PricingInfo(
                price = state.price,
                salePrice = state.salePrice
            ))
            val enableInventoryTracking = (state.quantity == null)
            val inventoryInfos = listOf(InventoryInfo(
                quantity = state.quantity,
                threshold = state.threshold,
            ))

            val sellableProducts = listOf(SellableProduct(
                label = label,
                shortLabel = shortLabel,
                pricingInfos = pricingInfos,
                inventoryInfos = inventoryInfos,
                disablePriceOverride = disablePriceOverride,
                enableInventoryTracking = enableInventoryTracking
            ))
            val product = Product(
                label = label,
                shortLabel = shortLabel,
                pricingInfos = pricingInfos,
                sellableProducts = sellableProducts
            )

            val request = CatalogProduct(product)
            val catalogService = catalogServiceClient.getService().getOrThrow()

            // create product
            val response = suspendCancellableCoroutine<CatalogProduct?> {
                catalogService.createCatalogProduct(
                    request,
                    Bundle.EMPTY,
                    it.onSuccess(),
                    it.onError()
                )
            }
            update { copy(
                createdProductId = response?.product?.id,
                createdSellableProductId = response?.product?.sellableProducts?.first()?.id
            )}


        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Create Product"),

        val label: String? = null,
        val skuCode: String? = null,
        val price: Money? = null,
        val salePrice: Money? = null,
        val quantity: Int? = null,
        val threshold: Int? = null,

        val createdProductId: String? = null,
        val createdSellableProductId: String? = null
    ) : CommonViewModel.ViewModelState
}
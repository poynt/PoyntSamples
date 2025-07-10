@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.product.create

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.ProductConstants
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
import com.godaddy.commercecore.models.Category
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


    fun showProductDialog() {
        execute {
            if (state.products){
                update{
                    copy()
            }

            if (state.categories.isEmpty()) {
                val params = bundleOf(
                    // recommended data source is REMOTE_IF_EMPTY.
                    ProductParams.DATA_SOURCE to DataSource.REMOTE_IF_EMPTY,
                    // pagination is required otherwise exception can be thrown.
                    ProductParams.PAGE_SIZE to 100,
                    ProductParams.PAGE_OFFSET to 0,
                )
                val service = catalogServiceClient.getService().getOrThrow()
                val response = suspendCancellableCoroutine<CatalogCategoryTreeNodes?> {
                    service.getCatalogCategoryTreeNodes(params, it.onSuccess(), it.onError())
                }
                update {
                    copy(
                        categories = response?.values?.map { it.categoryTreeNode.category }.orEmpty(),
                        showCategoryDialog = true
                    )
                }
            } else {
                update { copy(showCategoryDialog = true) }
            }
        }
    }

    fun hideProductsDialog() {
        update { copy(showProductDialog = false) }
    }
    }

    fun create() {
        hideProductsDialog()
        execute {

            val inventoryInfo = InventoryInfo(
                quantity = state.quantity,
                threshold = state.threshold
            )

            val sellableProduct = SellableProduct(
                skuCode = state.skuCode,
                inventoryInfos = listOf(inventoryInfo)
            )

            val product = Product(
                label = requireNotNull(state.label),
                pricingInfos = requireNotNull(state.pricingInfos),
                sellableProducts = listOf(sellableProduct)
            )

            val request = CatalogProduct(
                product = product
            )

            val catalogService = catalogServiceClient.getService().getOrThrow()

            // create product
            val response = suspendCancellableCoroutine<CatalogProduct?> {
                catalogService.createCatalogProduct(request, Bundle.EMPTY, it.onSuccess(), it.onError())
            }

            // TODO: if quantity is not null then create inventory
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Create Product"),



        // for Product
        val label: String? = null,
        val pricingInfos: List<PricingInfo>? = null,
        val sellableProducts: List<SellableProduct>? = null,

        val skuCode: String? = null,

        val price: Money? = null,
        val salePrice: Money? = null,

        val quantity: Int? = null,
        val threshold: Int? = null,

    ) : CommonViewModel.ViewModelState
}
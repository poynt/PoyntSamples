@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.inventory.create

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.ProductParams
import com.godaddy.commerce.catalog.models.Product
import com.godaddy.commerce.catalog.models.Products
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.common.FilterBy
import com.godaddy.commerce.inventory.models.*
import com.godaddy.commerce.provider.catalog.CatalogContract
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getInventoryService
import com.godaddy.commerce.services.sample.inventory.onSuccess
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine

class InventoryCreateViewModel : CommonViewModel<InventoryCreateViewModel.State>(State()) {

    private val inventoryServiceClient = getInventoryService(viewModelScope)

    private val catalogServiceClient = getCatalogService(viewModelScope)

    fun onLowInventoryThresholdUpdated(value: String) {
        update { copy(updatedLowInventoryThreshold = value.toFloatOrNull()) }
    }

    fun onQuantityUpdated(value: String) {
        update { copy(updatedQuantity = value.toFloatOrNull()) }
    }

    fun showProductsDialog() {
        execute {
            if (state.products.isEmpty()) {
                val params = bundleOf(
                    // recommended data source is REMOTE_IF_EMPTY.
                    ProductParams.DATA_SOURCE to DataSource.REMOTE_IF_EMPTY,
                    // pagination is required otherwise exception can be thrown.
                    ProductParams.PAGE_SIZE to 100,
                    ProductParams.PAGE_OFFSET to 0,
                    // show products which are without inventory tracking
                    ProductParams.FILTER_BY_PRODUCT_COLUMNS to FilterBy(CatalogContract.Inventory.Columns.TRACKING, "'1'", FilterBy.ComparisonOperator.NOT)
                )
                val service = catalogServiceClient.getService().getOrThrow()
                val products = suspendCancellableCoroutine<Products?> {
                    service.getProducts(
                        params,
                        it.onSuccess(),
                        it.onError()
                    )
                }
                update { copy(products = products?.products.orEmpty(), showProductDialog = true) }
            } else {
                update { copy(showProductDialog = true) }
            }
        }
    }

    fun hideProductsDialog() {
        update { copy(showProductDialog = false) }
    }

    fun create(product: Product) {
        hideProductsDialog()
        execute {
            val service = inventoryServiceClient.getService().getOrThrow()

            val level = suspendCancellableCoroutine<Level?> {
                service.postLevel(
                    Level(
                        productId = product.productId?.toString(),
                        quantity = requireNotNull(state.updatedQuantity) { "Quantity is required" },
                        summaryData = SummaryData(
                            lowInventoryThreshold = state.updatedLowInventoryThreshold
                        )
                    ),
                    Bundle.EMPTY,
                    it.onSuccess(),
                    it.onError()
                )
            }
            update { copy(createdLevelId = level?.inventoryLevelId) }
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Inventory"),
        val updatedLowInventoryThreshold: Float? = null,
        val updatedQuantity: Float? = null,
        val products: List<Product> = emptyList(),
        val showProductDialog: Boolean = false,
        val createdLevelId: String? = null
    ) : ViewModelState
}
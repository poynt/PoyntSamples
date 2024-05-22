@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.category.create

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.ProductParams
import com.godaddy.commerce.catalog.models.*
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.inventory.models.*
import com.godaddy.commerce.sdk.util.nullIfEmpty
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getInventoryService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine

class CategoryCreateViewModel : CommonViewModel<CategoryCreateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)
    private val inventoryServiceClient = getInventoryService(viewModelScope)

    fun onNameChanged(value: String) {
        update { copy(name = value) }
    }

    fun onDescriptionChanged(value: String) {
        update { copy(name = value) }
    }

    fun showProductDialog() {
        execute {
            if (state.products.isEmpty()) {
                val params = bundleOf(
                    // recommended data source is REMOTE_IF_EMPTY.
                    ProductParams.DATA_SOURCE to DataSource.REMOTE_IF_EMPTY,
                    // pagination is required otherwise exception can be thrown.
                    ProductParams.PAGE_SIZE to 100,
                    ProductParams.PAGE_OFFSET to 0,
                )
                val service = catalogServiceClient.getService().getOrThrow()
                val response = suspendCancellableCoroutine<Products?> {
                    service.getProducts(params, it.onSuccess(), it.onError())
                }
                update {
                    copy(
                        products = response?.products.orEmpty(),
                        showProductDialog = true
                    )
                }
            } else {
                update { copy(showProductDialog = true) }
            }
        }
    }

    fun hideProductsDialog() {
        update { copy(showProductDialog = false) }
    }

    fun selectProduct(product: Product) {
        update { copy(selectedProduct = product) }
    }

    fun create() {
        hideProductsDialog()
        execute {
            val name = requireNotNull(state.name) { "Name is required" }

            val request = Category(
                name = name,
                description = state.description,
                productIds = listOfNotNull(state.selectedProduct?.productId).nullIfEmpty(),
            )
            val catalogService = catalogServiceClient.getService().getOrThrow()

            // create category
            val response = suspendCancellableCoroutine<Category?> {
                catalogService.postCategory(request, Bundle.EMPTY, it.onSuccess(), it.onError())
            }

            update { copy(createdId = response?.categoryId?.toString()) }
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Create Category"),
        val name: String? = null,
        val description: String? = null,
        val products: List<Product> = emptyList(),
        val selectedProduct: Product? = null,
        val showProductDialog: Boolean = false,
        val createdId: String? = null
    ) : ViewModelState
}
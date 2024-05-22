@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.product.create

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.ProductConstants
import com.godaddy.commerce.catalog.ProductParams
import com.godaddy.commerce.catalog.models.*
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.inventory.models.*
import com.godaddy.commerce.sdk.util.nullIfEmpty
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.extensions.toSimpleMoney
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getInventoryService
import com.godaddy.commerce.services.sample.inventory.onSuccess
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine

class ProductCreateViewModel : CommonViewModel<ProductCreateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)
    private val inventoryServiceClient = getInventoryService(viewModelScope)

    fun onProductNameChanged(value: String) {
        update { copy(productName = value) }
    }

    fun onAmountChanged(value: String) {
        update { copy(amount = value.toLongOrNull()) }
    }

    fun onQuantityChanged(value: String) {
        update { copy(quantity = value.toFloatOrNull()) }
    }

    fun showCategoryDialog() {
        execute {
            if (state.categories.isEmpty()) {
                val params = bundleOf(
                    // recommended data source is REMOTE_IF_EMPTY.
                    ProductParams.DATA_SOURCE to DataSource.REMOTE_IF_EMPTY,
                    // pagination is required otherwise exception can be thrown.
                    ProductParams.PAGE_SIZE to 100,
                    ProductParams.PAGE_OFFSET to 0,
                )
                val service = catalogServiceClient.getService().getOrThrow()
                val response = suspendCancellableCoroutine<Categories?> {
                    service.getCategories(params, it.onSuccess(), it.onError())
                }
                update {
                    copy(
                        categories = response?.categories.orEmpty(),
                        showCategoryDialog = true
                    )
                }
            } else {
                update { copy(showCategoryDialog = true) }
            }
        }
    }

    fun hideProductsDialog() {
        update { copy(showCategoryDialog = false) }
    }

    fun selectCategory(category: Category) {
        update { copy(selectedCategory = category) }
    }

    fun onTypeSelected(position: Int) {
        update { copy(selectedProductType = productTypes.getOrNull(position)) }
    }

    fun create() {
        hideProductsDialog()
        execute {
            val amount = requireNotNull(state.amount) { "Amount is required" }
            val type = requireNotNull(state.selectedProductType) { "Product type is required" }
            val selectedCategory = state.selectedCategory
            val quantity = state.quantity


            val request = CreateProduct(
                name = state.productName,
                type = type,
                description = "Poynt Sample Test",
                price = amount.toSimpleMoney(),
                categoryIds = listOfNotNull(selectedCategory?.categoryId).nullIfEmpty(),
                inventory = quantity?.let {
                    ProductInventory(tracking = true, externalService = true)
                }
            )
            val catalogService = catalogServiceClient.getService().getOrThrow()

            // create product
            val response = suspendCancellableCoroutine<Product?> {
                catalogService.postProduct(request, Bundle.EMPTY, it.onSuccess(), it.onError())
            }

            // if quantity is not null then create inventory
            val inventoryService = inventoryServiceClient.getService().getOrThrow()
            suspendCancellableCoroutine<Level?> {
                inventoryService.postLevel(
                    Level(quantity = quantity, productId = response?.productId?.toString()),
                    Bundle.EMPTY,
                    it.onSuccess(),
                    it.onError()
                )
            }

            update { copy(createdId = response?.productId?.toString()) }
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Create Product"),
        val productTypes: List<String> = ProductConstants.Type.values.toList(),
        val selectedProductType: String? = null,
        val productName: String? = null,
        val amount: Long? = null,
        val quantity: Float? = null,
        val categories: List<Category> = emptyList(),
        val selectedCategory: Category? = null,
        val showCategoryDialog: Boolean = false,
        val createdId: String? = null
    ) : ViewModelState
}
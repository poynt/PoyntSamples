@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.category.create

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.model.CatalogCategoryTreeNode
import com.godaddy.commerce.sdk.util.nullIfEmpty
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commercecore.models.Category
import com.godaddy.commercecore.models.CategoryProduct
import com.godaddy.commercecore.models.CategoryTreeNode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine

class CategoryCreateViewModel : CommonViewModel<CategoryCreateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)

    fun onLabelChanged(value: String) {
        update { copy(label = value) }
    }

    fun onShortLabelChanged(value: String) {
        update { copy(shortLabel = value) }
    }

    fun showProductDialog() {
        execute {
            if (state.products.isEmpty()) {
                val service = catalogServiceClient.getService().getOrThrow()
                val response = suspendCancellableCoroutine<CatalogCategoryTreeNode?> {
                    service.getCatalogCategoryTreeNode(state.createdId, Bundle.EMPTY, it.onSuccess(), it.onError())
                }
                update {
                    copy(
                        products = response?.categoryTreeNode?.category?.products?.sortedBy { it.displayOrder }.orEmpty(),
                        showProductDialog = true,
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

    fun selectProduct(product: CategoryProduct) {
        update { copy(selectedProduct = product) }
    }

    fun create() {
        hideProductsDialog()
        execute {
            val label = requireNotNull(state.label) { "Label is required" }

            val request = CatalogCategoryTreeNode(
                categoryTreeNode = CategoryTreeNode(
                    category = Category(
                        label = label,
                        shortLabel = state.shortLabel,
                        products = state.products.nullIfEmpty(),
                    )
                ),
            )

            val catalogService = catalogServiceClient.getService().getOrThrow()
            // create category
            val response = suspendCancellableCoroutine<CatalogCategoryTreeNode?> {
                catalogService.createCatalogCategoryTreeNode(request, Bundle.EMPTY, it.onSuccess(), it.onError())
            }

            update { copy(createdId = response?.categoryTreeNode?.category?.id) }
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Create Category"),
        val label: String? = null,
        val shortLabel: String? = null,
        val products: List<CategoryProduct> = emptyList(),
        val selectedProduct: CategoryProduct? = null,
        val showProductDialog: Boolean = false,
        val createdId: String? = null
    ) : ViewModelState

}
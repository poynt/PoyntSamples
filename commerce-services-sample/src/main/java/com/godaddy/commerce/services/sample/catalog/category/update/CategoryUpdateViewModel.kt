@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.category.update

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.model.CatalogCategoryTreeNode
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.provider.catalog.CatalogContract
import com.godaddy.commerce.sdk.catalog.CategoryParamsExt
import com.godaddy.commerce.sdk.catalog.ProductParamsExt
import com.godaddy.commerce.sdk.catalog.getCatalogCategoryTreeNode
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.catalog.product.ProductRecyclerItem
import com.godaddy.commerce.services.sample.catalog.product.mapToCategoryUiItems
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commerce.sdk.catalog.getCatalogProducts
import com.godaddy.commerce.sdk.catalog.updateCatalogCategoryTreeNode
import com.godaddy.commercecore.models.Category
import com.godaddy.commercecore.models.CategoryProduct
import com.godaddy.commercecore.models.CategoryTreeNode
import kotlinx.coroutines.FlowPreview

class CategoryUpdateViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<CategoryUpdateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)
    private val id get() = savedStateHandle.get<String>("id")

    init {
        loadCategory()
        loadProducts()
    }

    private fun setCategoryState(response: CatalogCategoryTreeNode?) {
        update {
            val categoryTreeNode = response?.categoryTreeNode
            val category = categoryTreeNode?.category
            copy(
                updatedCatalogCategoryTreeNode = response,
                updatedCategoryTreeNode = categoryTreeNode,
                updatedCategory = category,
                updatedLabel = category?.label,
                updatedDisplayOrder = categoryTreeNode?.displayOrder,
                updatedCategoryProducts = response?.categoryTreeNode?.category?.products
            )
        }
    }

    private fun loadCategory() {
        execute {
            val service = catalogServiceClient.getService().getOrThrow()
            val bundle = CategoryParamsExt.toBundle(
                includeProductIds = true
            )

            val response =  service.getCatalogCategoryTreeNode(id, bundle)
            setCategoryState(response)
        }
    }

    private fun loadProducts(query: String? = null) {
        execute {
            val service = catalogServiceClient.getService().getOrThrow()
            val bundle = ProductParamsExt.toBundle(
                dataSource = DataSource.REMOTE_IF_EMPTY,
                pageOffset = DEFAULT_CATEGORY_PRODUCTS_PAGE_OFFSET,
                pageSize = DEFAULT_CATEGORY_PRODUCTS_PAGE_SIZE,
                sortBy = CatalogContract.Product.Columns.UPDATED_AT,
                searchTerm = query,
            )
            val response = service.getCatalogProducts(bundle)
            val products = response?.products.orEmpty()

            update { copy(products = products) }
            separateProductLists(products)
        }
    }

    private fun separateProductLists(allProducts: List<CatalogProduct>) {
        val existingProductIds = state.updatedCategoryProducts?.map { it.id }?.toSet().orEmpty()
        val displayOrderMap = state.updatedCategoryProducts?.associate {
            it.id to it.displayOrder
        }.orEmpty()

        val addedProducts = allProducts.filter {
            existingProductIds.contains(it.product.id)
        }.sortedBy {
            displayOrderMap[it.product.id]
        }

        val availableProducts = allProducts.filter {
            existingProductIds.contains(it.product.id)
        }

        update {
            copy(
                addedProducts = addedProducts,
                items = availableProducts.map {
                    it.mapToCategoryUiItems(
                        isSelected = false,
                        onDeleteClicked = { catalogProduct -> removeProduct(catalogProduct) },
                        onSelectClicked = { catalogProduct, _ -> selectProduct(catalogProduct) }
                    )
                },
                addedItems = addedProducts.map {
                    it.mapToCategoryUiItems(
                        isSelected = true,
                        onDeleteClicked = { catalogProduct -> removeProduct(catalogProduct) },
                        onSelectClicked = { _, _ -> }
                    )
                }
            )
        }
    }

    private fun selectProduct(catalogProduct: CatalogProduct) {
        val productId = requireNotNull(catalogProduct.product.id)

        if (state.updatedCategoryProducts?.any { it.id == productId } == true) {
            return
        }

        update {
            copy(
                addedProducts = addedProducts + catalogProduct,
                selectedProduct = catalogProduct,
                items = items.filterNot { it.item == catalogProduct },
                addedItems = addedItems + catalogProduct.mapToCategoryUiItems(
                    isSelected = true,
                    onDeleteClicked = { removeProduct(it) },
                    onSelectClicked = { _, _ -> }
                ),
                updatedCategoryProducts = updatedCategoryProducts.orEmpty() + CategoryProduct(
                    id = productId,
                    displayOrder = updatedCategoryProducts.orEmpty().size + 1
                )
            )
        }
    }

    private fun removeProduct(catalogProduct: CatalogProduct) {
        val productId = requireNotNull(catalogProduct.product.id)

        update {
            copy(
                addedProducts = addedProducts - catalogProduct,
                updatedCategoryProducts = updatedCategoryProducts?.filterNot { it.id == productId },
                addedItems = addedItems.filterNot { it.item == catalogProduct },
                items = listOf(catalogProduct.mapToCategoryUiItems(
                    isSelected = false,
                    onDeleteClicked = { removeProduct(it) },
                    onSelectClicked = { _, _ -> selectProduct(catalogProduct) }
                )) + items
            )
        }
    }

    fun onLabelUpdated(value: String) {
        update {
            copy(
                updatedLabel = value,
                updatedShortLabel = value.take(DEFAULT_CATEGORY_SHORT_LABEL_SIZE)
            )
        }
    }

    fun onDisplayOrderUpdated(value: String) {
        value.toIntOrNull()?.let {
            update { copy(updatedDisplayOrder = it) }
        }
    }

    fun updateCategory() {
        execute {
            val service = catalogServiceClient.getService().getOrThrow()
            val category = Category(
                id = state.updatedCategory?.id,
                label = state.updatedLabel,
                shortLabel = state.updatedShortLabel,
                displayOrder = state.updatedDisplayOrder,
                products = state.updatedCategoryProducts,
            )
            val categoryTreeNode = CategoryTreeNode(
                id = state.updatedCategoryTreeNode?.id,
                category = category,
                displayOrder = state.updatedDisplayOrder,
                categoryTreeId = state.updatedCategoryTreeNode?.categoryTreeId,
                parentNodeId = state.updatedCategoryTreeNode?.parentNodeId,
                childNodeIds = state.updatedCategoryTreeNode?.childNodeIds
            )
            val request = CatalogCategoryTreeNode(
                categoryTreeNode = categoryTreeNode
            )

            val response = service.updateCatalogCategoryTreeNode(id.orEmpty(), request)
            setCategoryState(response)
            update { copy(updatedCategoryId = updatedCategory?.id) }
        }
    }
    /**
     * @property addedProducts holds all current CatalogProducts to be added to the new category
     * @property addedItems holds all the recyclable items of catalog products from addedProducts
     * @property products holds all un-added CatalogProducts
     * @property items holds recyclable items from products
     */
    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Category Update"),

        val updatedCategoryId: String? = null,
        val updatedCatalogCategoryTreeNode: CatalogCategoryTreeNode? = null,
        val updatedCategoryTreeNode: CategoryTreeNode? = null,
        val updatedCategory: Category? = null,
        val updatedLabel: String? = null,
        val updatedShortLabel: String? = null,
        val updatedDisplayOrder: Int? = null,
        val updatedCategoryProducts: List<CategoryProduct>? = emptyList(),

        val addedProducts: List<CatalogProduct> = emptyList(),
        val products: List<CatalogProduct> = emptyList(),
        val items: List<ProductRecyclerItem> = emptyList(),
        val addedItems: List<ProductRecyclerItem> = emptyList(),

        val selectedProduct: CatalogProduct? = null,
    ) : ViewModelState

    companion object{
        private const val DEFAULT_CATEGORY_SHORT_LABEL_SIZE = 5
        private const val DEFAULT_CATEGORY_PRODUCTS_PAGE_SIZE = 100
        private const val DEFAULT_CATEGORY_PRODUCTS_PAGE_OFFSET = 0
    }
}
@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.category.create

import android.view.View
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.model.CatalogCategoryTreeNode
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.provider.catalog.CatalogContract
import com.godaddy.commerce.sdk.catalog.ProductParamsExt
import com.godaddy.commerce.sdk.catalog.createCatalogCategoryTreeNode
import com.godaddy.commerce.sdk.catalog.getCatalogProducts
import com.godaddy.commerce.services.sample.catalog.product.ProductRecyclerItem
import com.godaddy.commerce.services.sample.catalog.product.mapToCategoryUiItems
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commercecore.models.Category
import com.godaddy.commercecore.models.CategoryProduct
import com.godaddy.commercecore.models.CategoryTreeNode
import kotlinx.coroutines.FlowPreview

class CategoryCreateViewModel : CommonViewModel<CategoryCreateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)

    init {
        loadProducts()
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
            update { copy (products = products) }
            loadItems(products)
        }
    }
    private fun loadItems(products: List<CatalogProduct>){
        update { copy(
            items = products.map {
                it.mapToCategoryUiItems(
                    onDeleteClicked = { catalogProduct -> removeProduct(catalogProduct) },
                    onSelectClicked = { catalogProduct, _ -> selectProduct(catalogProduct) },
                )
            }
        ) }
    }

    fun onLabelChanged(value: String) {
        update { copy(label = value) }
    }
    fun onDisplayOrderChanged(value: String) {
        value.toIntOrNull()?.let {
            update { copy(displayOrder = it) }
        }
    }
    private fun selectProduct(catalogProduct: CatalogProduct) {
        if (state.addedProducts.contains(catalogProduct)){ return }
        update { copy(
            addedProducts = addedProducts + catalogProduct,
            selectedProduct = catalogProduct,
            items = items.filterNot { it.item == catalogProduct },
            addedItems = addedItems + catalogProduct.mapToCategoryUiItems(
                isSelected = true,
                onDeleteClicked = {removeProduct(it)},
                onSelectClicked = { _, _ -> }
            ),
            categoryProducts = categoryProducts +
                CategoryProduct(
                    id = requireNotNull(catalogProduct.product.id),
                    displayOrder = state.categoryProducts.size + 1
                )
        ) }
    }
    private fun removeProduct(catalogProduct: CatalogProduct) {
        update { copy(
            addedProducts = addedProducts - catalogProduct,
            categoryProducts = categoryProducts.filterNot { it.id == catalogProduct.product.id },
            addedItems =  addedItems.filterNot {it.item == catalogProduct },
            items = listOf(catalogProduct.mapToCategoryUiItems(
                isSelected = false,
                onDeleteClicked = { removeProduct(it) },
                onSelectClicked = { _, _ -> selectProduct(catalogProduct) }
            )) + items
        ) }
    }

    fun create() {
        execute {
            try {
                val label = requireNotNull(state.label) { "Label is required" }
                val shortLabel = requireNotNull(label.take(DEFAULT_CATEGORY_SHORT_LABEL_SIZE))
                val category = Category(
                    label = label,
                    shortLabel = shortLabel,
                    displayOrder = state.displayOrder,
                    products = state.categoryProducts,
                )
                val categoryTreeNode = CategoryTreeNode(
                    category = category,
                    displayOrder = state.displayOrder
                )
                val request = CatalogCategoryTreeNode(categoryTreeNode = categoryTreeNode)
                val catalogService = catalogServiceClient.getService().getOrThrow()

                val response = catalogService.createCatalogCategoryTreeNode(request)
                update { copy (createdId = response?.categoryTreeNode?.category?.id) }
            } catch (e: Exception) {
                this.sendEffect(Effect.ShowSnackbar(e.message.toString()))
            }
        }
    }
    /**
     * @property addedProducts holds all current CatalogProducts to be added to the new category
     * @property addedItems holds all the recyclable items of catalog products from addedProducts
     * @property products holds all un-added CatalogProducts, starts off initially as list of all products
     * @property items holds recyclable items from products
     */
    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Create Category"),
        val label: String? = null,
        val displayOrder: Int? = null,

        val addedProducts: List<CatalogProduct> = emptyList(),
        val categoryProducts: List<CategoryProduct> = emptyList(),

        val products: List<CatalogProduct> = emptyList(),
        val items: List<ProductRecyclerItem> = emptyList(),
        val addedItems: List<ProductRecyclerItem> = emptyList(),

        val createdId: String? = null,

        val selectedProduct: CatalogProduct? = null,
    ) : ViewModelState

    companion object{
        private const val DEFAULT_CATEGORY_SHORT_LABEL_SIZE = 5
        private const val DEFAULT_CATEGORY_PRODUCTS_PAGE_SIZE = 100
        private const val DEFAULT_CATEGORY_PRODUCTS_PAGE_OFFSET = 0
    }
}
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
import com.godaddy.commercecore.models.Money
import com.godaddy.commercecore.models.PricingInfo
import com.godaddy.commercecore.models.Product
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine

class ProductCreateViewModel : CommonViewModel<ProductCreateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)

    fun onProductNameChanged(value: String) {
        update { copy(label = value) }
    }

//    fun onAmountChanged(value: String) {
//        update { copy( = value.toLongOrNull()) }
//    }

//    fun onQuantityChanged(value: String) {
//        update { copy(quantity = value.toFloatOrNull()) }
//    }

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

            val categoryIds = listOfNotNull( state.selectedCategory?.id )
            val label = requireNotNull( state.label)
            val shortLabel = requireNotNull( state.shortLabel )
            val pricingInfos = listOf( PricingInfo(price = state.price, salePrice = state.salePrice))

            val request = CatalogProduct(
                product = Product(
                    label = label,
                    shortLabel = shortLabel,
                    categoryIds = categoryIds,
                    pricingInfos = pricingInfos
                ),
                taxes = emptyList(),
                discounts = emptyList(),
                fees = emptyList(),
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
        val productTypes: List<String> = ProductConstants.Type.values.toList(),
        val selectedProductType: String? = null,

        val label: String? = null,
        val shortLabel: String? = null,
        val price: Money? = null,
        val salePrice: Money? = null,
        val type: String? = ProductConstants.Type.PHYSICAL,


        val categories: List<Category?> = emptyList(),
        val selectedCategory: Category? = null,
        val showCategoryDialog: Boolean = false,
        val createdId: String? = null
    ) : CommonViewModel.ViewModelState
}
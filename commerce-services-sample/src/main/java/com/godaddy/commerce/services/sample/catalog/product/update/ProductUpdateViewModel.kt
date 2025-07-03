@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.product.update

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.ProductParams
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.sdk.catalog.updateCatalogProduct
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.extensions.toSimpleMoney
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commercecore.models.Money
import com.godaddy.commercecore.models.Product
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine

class ProductUpdateViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<ProductUpdateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)

    private val id get() = savedStateHandle.get<String>("id")

    init {
        loadProduct()
    }

    private fun loadProduct() {
        execute {
            val service = catalogServiceClient.getService().getOrThrow()
            val bundle = Bundle().apply {
                // data source defines data provider: local db, remote or remote only if there are no data in local db.
                // It is better to use REMOTE_IF_EMPTY in most cases to improve UX and performance.
                putParcelable(ProductParams.DATA_SOURCE, DataSource.REMOTE_IF_EMPTY)
            }
            val response = suspendCancellableCoroutine<CatalogProduct?> {
                service.getCatalogProduct(id, bundle, it.onSuccess(), it.onError())
            }
            update {
                copy(
                    product = response?.product,
                    toolbarState = toolbarState.copy(title = "Product Update: ${response?.product?.id}")
                )
            }
        }
    }

    fun onProductLabelUpdated(value: String) {
        update { copy(updatedLabel = value) }
    }

    fun onProductPriceUpdated(value: String) {
        update { copy(updatedPrice = Money(value)) }
    }

    fun updateProduct() {
        execute {
            val service = catalogServiceClient.getService().getOrThrow()
            val request = CatalogProduct(
                product = Product(
                    label = state.updatedLabel,
                    shortLabel = state.updatedShortLabel,
//                pricingInfos = state.updatedPrice,
                )
            )

            val response = suspendCancellableCoroutine<CatalogProduct?> {
                service.updateCatalogProduct(id, request, Bundle.EMPTY, it.onSuccess(), it.onError())
            }

            update { copy(updatedId = response?.product?.id?.toString()) }
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Product Update"),
        val product: Product? = null,
        val updatedLabel: String? = null,
        val updatedShortLabel: String? = null,
        val updatedPrice: Money? = null,
        val updatedSalePrice: Money? = null,
        val updatedId: String? = null
    ) : ViewModelState
}
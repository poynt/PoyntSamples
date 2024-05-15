@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.product

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.ProductParams
import com.godaddy.commerce.catalog.models.Products
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine

class ProductViewModel : CommonViewModel<ProductViewModel.State>(State()) {

    private val serviceClient = CommerceDependencyProvider.getCatalogService(viewModelScope)

    init {
        loadProducts()
    }

    fun loadProducts() {
        execute {
            val service = serviceClient.getService().getOrThrow()
            val bundle = Bundle().apply {
                // data source defines data provider: local db, remote or remote only if there are no date in local db.
                // It is better to use REMOTE_IF_EMPTY in most cases to improve UX and performance.
                putParcelable(ProductParams.DATA_SOURCE, DataSource.REMOTE_IF_EMPTY)

                // Add pagination to improve UX and avoid TooLargeTransactionException
                putInt(ProductParams.PAGE_OFFSET, 0)
                putInt(ProductParams.PAGE_SIZE, 100)
            }
            val response = suspendCancellableCoroutine<Products?> {
                service.getProducts(bundle, it.onSuccess(), it.onError())
            }
            update { copy(items = response?.products.orEmpty().map { it.mapToUiItems() }) }
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Products"),
        val items: List<ProductRecyclerItem> = emptyList()
    ) : ViewModelState
}
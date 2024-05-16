@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.product

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.CatalogIntents
import com.godaddy.commerce.catalog.ProductParams
import com.godaddy.commerce.catalog.models.Products
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.provider.catalog.CatalogContract
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.extensions.subscribeOnUpdates
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class ProductViewModel : CommonViewModel<ProductViewModel.State>(State()) {

    private val serviceClient = CommerceDependencyProvider.getCatalogService(viewModelScope)
    private val searchQueryFlow = MutableSharedFlow<String>()

    init {
        loadProducts()
        subscribeOnSearch()

        // CS sends an event when data was changed. Subscribe on it and refresh the list.
        CommerceDependencyProvider.getContext().subscribeOnUpdates(
            CatalogIntents.ACTION_PRODUCTS_CHANGED
        ).onEach {
            loadProducts()
        }.launchIn(viewModelScope)
    }

    private fun subscribeOnSearch() {
        searchQueryFlow.debounce(300)
            .onEach { loadProducts(query = it) }
            .launchIn(viewModelScope)
    }

    @JvmOverloads
    fun loadProducts(query: String? = null) {
        execute {
            val service = serviceClient.getService().getOrThrow()
            val bundle = Bundle().apply {
                // data source defines data provider: local db, remote or remote only if there are no data in local db.
                // It is better to use REMOTE_IF_EMPTY in most cases to improve UX and performance.
                putParcelable(ProductParams.DATA_SOURCE, DataSource.REMOTE_IF_EMPTY)

                // Add pagination to improve UX and avoid TooLargeTransactionException
                putInt(ProductParams.PAGE_OFFSET, 0)
                putInt(ProductParams.PAGE_SIZE, 100)

                // sorting is optional. List can be sorted by any column in database.
                putString(ProductParams.SORT_BY, CatalogContract.Product.Columns.UPDATED_AT)

                // add search query if not null
                putString(ProductParams.SEARCH_TERM, query)
            }
            val response = suspendCancellableCoroutine<Products?> {
                service.getProducts(bundle, it.onSuccess(), it.onError())
            }
            update { copy(items = response?.products.orEmpty().map { it.mapToUiItems() }) }
        }
    }

    fun searchProduct(query: String) {
        viewModelScope.launch { searchQueryFlow.emit(query) }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Products", showSearchButton = true),
        val items: List<ProductRecyclerItem> = emptyList()
    ) : ViewModelState
}
@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.category

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.CatalogIntents
import com.godaddy.commerce.catalog.CategoryParams
import com.godaddy.commerce.catalog.models.Categories
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

class CategoryViewModel : CommonViewModel<CategoryViewModel.State>(State()) {

    private val serviceClient = CommerceDependencyProvider.getCatalogService(viewModelScope)
    private val searchQueryFlow = MutableSharedFlow<String>()

    init {
        loadCategories()
        subscribeOnSearch()

        // CS sends an event when data was changed. Subscribe on it and refresh the list.
        CommerceDependencyProvider.getContext().subscribeOnUpdates(
            CatalogIntents.ACTION_CATEGORIES_CHANGED
        ).onEach {
            loadCategories()
        }.launchIn(viewModelScope)
    }


    private fun subscribeOnSearch() {
        searchQueryFlow.debounce(SEARCH_DEBOUNCE_DELAY)
            .onEach { loadCategories(query = it) }
            .launchIn(viewModelScope)
    }

    @JvmOverloads
    fun loadCategories(query: String? = null) {
        execute {
            val service = serviceClient.getService().getOrThrow()
            val bundle = Bundle().apply {
                // data source defines data provider: local db, remote or remote only if there are no data in local db.
                // It is better to use REMOTE_IF_EMPTY in most cases to improve UX and performance.
                putParcelable(CategoryParams.DATA_SOURCE, DataSource.REMOTE_IF_EMPTY)

                // Add pagination to improve UX and avoid TooLargeTransactionException
                putInt(CategoryParams.PAGE_OFFSET, 0)
                putInt(CategoryParams.PAGE_SIZE, CATEGORY_DEFAULT_PAGE_SIZE)

                // sorting is optional. List can be sorted by any column in database.
                putString(CategoryParams.SORT_BY, CatalogContract.Category.Columns.UPDATED_AT)

                putString(CategoryParams.SEARCH_QUERY, query)

                // optional. Use only if need to get products list in category model.
                putBoolean(CategoryParams.INCLUDE_PRODUCT_IDS, true)
            }
            val response = suspendCancellableCoroutine<Categories?> {
                service.getCategories(bundle, it.onSuccess(), it.onError())
            }
            update { copy(items = response?.categories.orEmpty().map { it.mapToUiItems() }) }
        }
    }

    fun searchCategory(query: String) {
        viewModelScope.launch { searchQueryFlow.emit(query) }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(
            title = "Categories",
            showSearchButton = true
        ),
        val items: List<CategoryRecyclerItem> = emptyList()
    ) : ViewModelState

    private companion object {
        private const val SEARCH_DEBOUNCE_DELAY = 300L
        private const val CATEGORY_DEFAULT_PAGE_SIZE = 100
    }
}
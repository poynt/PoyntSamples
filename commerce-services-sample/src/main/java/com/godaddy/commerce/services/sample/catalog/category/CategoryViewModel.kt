@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.category

import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.CatalogIntents
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.provider.catalog.CatalogContract
import com.godaddy.commerce.sdk.catalog.CategoryParamsExt
import com.godaddy.commerce.sdk.catalog.getCatalogCategoryTreeNodes
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
            val bundle = CategoryParamsExt.toBundle(
                // data source defines data provider: local db, remote or remote only if there are no data in local db.
                // It is better to use REMOTE_IF_EMPTY in most cases to improve UX and performance.
                dataSource = DataSource.REMOTE_IF_EMPTY,
                // Add pagination to improve UX and avoid TooLargeTransactionException
                pageOffset = CATEGORY_DEFAULT_PAGE_OFFSET,
                pageSize = CATEGORY_DEFAULT_PAGE_SIZE,
                // sorting is optional. List can be sorted by any column in database.
                sortBy = CatalogContract.Category.Columns.DISPLAY_ORDER,
                // add search query if not null
                searchQuery = query,
                // optional. Use only if need to get product list in category model.
                // includeProductIds = false,
            )
            val response = service.getCatalogCategoryTreeNodes(bundle)
            update {
                copy(
                    items = response?.values.orEmpty()
                        .sortedBy { it.categoryTreeNode.displayOrder }
                        .map { it.mapToUiItems() }
                )
            }
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
        private const val CATEGORY_DEFAULT_PAGE_OFFSET = 0
    }
}
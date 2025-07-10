@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.category.update

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.CategoryParams
import com.godaddy.commerce.catalog.model.CatalogCategory
import com.godaddy.commerce.catalog.model.CatalogCategoryTreeNode
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.sdk.catalog.updateCatalogCategoryTreeNode
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commercecore.models.Category
import com.godaddy.commercecore.models.CategoryTreeNode
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine

class CategoryUpdateViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<CategoryUpdateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)

    private val id get() = savedStateHandle.get<String>("id")

    init {
        loadCategory()
    }

    private fun loadCategory() {
        execute {
            val service = catalogServiceClient.getService().getOrThrow()
            val bundle = Bundle().apply {
                // data source defines data provider: local db, remote or remote only if there are no data in local db.
                // It is better to use REMOTE_IF_EMPTY in most cases to improve UX and performance.
                putParcelable(CategoryParams.DATA_SOURCE, DataSource.REMOTE_IF_EMPTY)
            }
            val response = suspendCancellableCoroutine<CatalogCategoryTreeNode?> {
                service.getCatalogCategoryTreeNode(id, bundle, it.onSuccess(), it.onError())
            }
            update {
                copy(
                    item = response?.categoryTreeNode?.category,
                    toolbarState = toolbarState.copy(title = "Category Update: ${response?.categoryTreeNode?.category?.id}")
                )
            }
        }
    }

    fun onNameUpdated(value: String) {
        update { copy(updatedName = value) }
    }

    fun updateCategory() {
        execute {
            val service = catalogServiceClient.getService().getOrThrow()
            val request = CatalogCategoryTreeNode(
                categoryTreeNode = CategoryTreeNode(category = Category(label = state.updatedName))
            )

            val response = suspendCancellableCoroutine<CatalogCategoryTreeNode?> {
                service.updateCatalogCategoryTreeNode(id, request, Bundle.EMPTY, it.onSuccess(), it.onError())
            }
            update { copy(updatedId = response?.categoryTreeNode?.category?.id.toString()) }
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Category Update"),
        val item: Category? = null,
        val updatedName: String? = null,
        val updatedId: String? = null
    ) : ViewModelState
}
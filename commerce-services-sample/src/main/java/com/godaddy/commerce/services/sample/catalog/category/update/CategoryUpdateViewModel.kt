@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.category.update

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.CategoryParams
import com.godaddy.commerce.catalog.models.Category
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine

class CategoryUpdateViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<CategoryUpdateViewModel.State>(State()) {

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
                putParcelable(CategoryParams.DATA_SOURCE, DataSource.REMOTE_IF_EMPTY)
            }
            val response = suspendCancellableCoroutine<Category?> {
                service.getCategory(id, bundle, it.onSuccess(), it.onError())
            }
            update {
                copy(
                    item = response,
                    toolbarState = toolbarState.copy(title = "Category Update: ${response?.categoryId}")
                )
            }
        }
    }

    fun onNameUpdated(value: String) {
        update { copy(updatedName = value) }
    }

    fun updateProduct() {
        execute {
            val service = catalogServiceClient.getService().getOrThrow()
            val request = Category(
                name = state.updatedName,
            )

            val response = suspendCancellableCoroutine<Category?> {
                service.patchCategory(id, request, Bundle.EMPTY, it.onSuccess(), it.onError())
            }

            update { copy(updatedId = response?.categoryId?.toString()) }
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
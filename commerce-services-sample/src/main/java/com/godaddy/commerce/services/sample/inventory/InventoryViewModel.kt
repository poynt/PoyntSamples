@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.inventory

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.inventory.InventoryParams
import com.godaddy.commerce.inventory.models.InventoryBundledList
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import com.godaddy.commerce.services.sample.inventory.InventoryViewModel.State
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine

class InventoryViewModel : CommonViewModel<State>(State()) {

    private val inventoryServiceClient =
        CommerceDependencyProvider.getInventoryService(viewModelScope)

    init {
        loadInventory()
    }

    fun loadInventory() {
        execute {
            val service = inventoryServiceClient.getService().getOrThrow()
            val bundle = Bundle().apply {
                // data source defines data provider: local db, remote or remote only if there are no date in local db.
                // It is better to use REMOTE_IF_EMPTY in most cases to improve UX and performance.
                putParcelable(InventoryParams.DATA_SOURCE, DataSource.REMOTE_IF_EMPTY)
                // Optional: include summary if need
                putBoolean(InventoryParams.INCLUDE_SUMMARY, true)
                // Optional: include location if need
                // putBoolean(InventoryParams.INCLUDE_LOCATION, true)
                // Optional: include reservation if need
                // putBoolean(InventoryParams.INCLUDE_RESERVATION, true)
            }
            val response = suspendCancellableCoroutine<InventoryBundledList?> {
                service.getInventoryBundled(bundle, it.onSuccess(), it.onError())
            }
            update { copy(items = response?.inventoryBundles.orEmpty().map { it.mapToUiItems() }) }
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Inventory"),
        val items: List<InventoryRecyclerItem> = emptyList()
    ) : ViewModelState
}
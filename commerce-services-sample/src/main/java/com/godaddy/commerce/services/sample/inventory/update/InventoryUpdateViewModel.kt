@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.inventory.update

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.inventory.IInventoryService
import com.godaddy.commerce.inventory.InventoryParams
import com.godaddy.commerce.inventory.models.InventoryBundled
import com.godaddy.commerce.inventory.models.InventoryBundledList
import com.godaddy.commerce.inventory.models.Level
import com.godaddy.commerce.inventory.models.Summary
import com.godaddy.commerce.sdk.util.bundleOf
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getInventoryService
import com.godaddy.commerce.services.sample.inventory.onSuccess
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine

class InventoryUpdateViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<InventoryUpdateViewModel.State>(State()) {

    private val inventoryServiceClient = getInventoryService(viewModelScope)

    private val levelId get() = savedStateHandle.get<String>("id")

    init {
        loadInventory()
    }

    private fun loadInventory() {
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

                // Get level by id
                putString(InventoryParams.LEVEL_ID, levelId)
            }
            val response = suspendCancellableCoroutine<InventoryBundledList?> {
                service.getInventoryBundled(bundle, it.onSuccess(), it.onError())
            }
            update {
                copy(
                    item = response?.inventoryBundles?.firstOrNull(),
                    toolbarState = toolbarState.copy(title = "Inventory Update: $levelId")
                )
            }
        }
    }

    fun onLowInventoryThresholdUpdated(value: String) {
        update { copy(updatedLowInventoryThreshold = value.toFloatOrNull()) }
    }

    fun onQuantityUpdated(value: String) {
        update { copy(updatedQuantity = value.toFloatOrNull()) }
    }

    fun updateInventory() {
        execute {
            val service = inventoryServiceClient.getService().getOrThrow()

            val updatedLevel = service.updateLevel()
            service.updateSummary()

            update { copy(updatedLevelId = updatedLevel?.inventoryLevelId) }

            // refresh inventory
            loadInventory()
        }
    }

    private suspend fun IInventoryService.updateLevel(): Level? {
        val request = Level(
            quantity = state.updatedQuantity
        )
        val inventoryLevelId = requireNotNull(state.item?.level?.inventoryLevelId) {
            "Inventory Level id is null"
        }
        val params = bundleOf(InventoryParams.LEVEL_ID to inventoryLevelId)

        return suspendCancellableCoroutine {
            patchLevel(
                request,
                params,
                it.onSuccess(),
                it.onError()
            )
        }
    }

    private suspend fun IInventoryService.updateSummary(): Summary? {
        val request = Summary(
            lowInventoryThreshold = state.updatedLowInventoryThreshold,
        )
        val inventorySummaryId = requireNotNull(state.item?.summary?.inventorySummaryId) {
            "Inventory Summary Id is null"
        }
        val params = bundleOf(InventoryParams.SUMMARY_ID to inventorySummaryId)

        return suspendCancellableCoroutine {
            patchSummary(
                request,
                params,
                it.onSuccess(),
                it.onError()
            )
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Inventory Update"),
        val item: InventoryBundled? = null,
        val updatedLowInventoryThreshold: Float? = null,
        val updatedQuantity: Float? = null,
        val updatedLevelId: String? = null
    ) : ViewModelState
}
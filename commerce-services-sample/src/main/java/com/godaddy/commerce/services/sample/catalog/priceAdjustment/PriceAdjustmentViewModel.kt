package com.godaddy.commerce.services.sample.catalog.priceAdjustment

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.CatalogIntents
import com.godaddy.commerce.catalog.DiscountParams
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.priceadjustments.models.PriceAdjustment
import com.godaddy.commerce.priceadjustments.models.PriceAdjustments
import com.godaddy.commerce.provider.catalog.CatalogContract
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.extensions.subscribeOnUpdates
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.suspendCancellableCoroutine

class PriceAdjustmentViewModel : CommonViewModel<PriceAdjustmentViewModel.State>(State()) {

    private val serviceClient = CommerceDependencyProvider.getCatalogService(viewModelScope)

    init {
        loadPriceAdjustments()

        // CS sends an event when data was changed. Subscribe on it and refresh the list.
        CommerceDependencyProvider.getContext().run {
            merge(
                subscribeOnUpdates(CatalogIntents.ACTION_DISCOUNTS_CHANGED),
                subscribeOnUpdates(CatalogIntents.ACTION_FEES_CHANGED),
            ).onEach {
                loadPriceAdjustments()
            }.launchIn(viewModelScope)
        }
    }

    fun loadPriceAdjustments() {
        execute {
            val service = serviceClient.getService().getOrThrow()
            val bundle = Bundle().apply {
                // data source defines data provider: local db, remote or remote only if there are no data in local db.
                // It is better to use REMOTE_IF_EMPTY in most cases to improve UX and performance.
                putParcelable(DiscountParams.DATA_SOURCE, DataSource.REMOTE_IF_EMPTY)

                // Add pagination to improve UX and avoid TooLargeTransactionException
                putInt(DiscountParams.PAGE_OFFSET, 0)
                putInt(DiscountParams.PAGE_SIZE, 100)

                // sorting is optional. List can be sorted by any column in database.
                putString(
                    DiscountParams.SORT_BY,
                    CatalogContract.PriceAdjustment.Columns.UPDATED_AT
                )
            }
            val response = suspendCancellableCoroutine<PriceAdjustments?> {
                service.getPriceAdjustments(bundle, it.onSuccess(), it.onError())
            }
            update { copy(items = response?.adjustments.orEmpty()) }
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Price Adjustments"),
        val items: List<PriceAdjustment> = emptyList()
    ) : ViewModelState
}
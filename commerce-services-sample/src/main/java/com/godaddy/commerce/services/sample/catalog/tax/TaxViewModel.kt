@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.tax

import android.os.Bundle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.CatalogIntents
import com.godaddy.commerce.catalog.TaxParams
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.common.FilterBy
import com.godaddy.commerce.provider.catalog.CatalogContract
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.extensions.subscribeOnUpdates
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import com.godaddy.commerce.taxes.models.Taxes
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.suspendCancellableCoroutine

class TaxViewModel : CommonViewModel<TaxViewModel.State>(State()) {

    private val serviceClient = CommerceDependencyProvider.getCatalogService(viewModelScope)

    init {
        loadTaxes()

        // CS sends an event when data was changed. Subscribe on it and refresh the list.
        CommerceDependencyProvider.getContext().subscribeOnUpdates(
            CatalogIntents.ACTION_TAXES_CHANGED
        ).onEach {
            loadTaxes()
        }.launchIn(viewModelScope)
    }

    fun loadTaxes() {
        execute {
            val service = serviceClient.getService().getOrThrow()
            val bundle = Bundle().apply {
                // data source defines data provider: local db, remote or remote only if there are no data in local db.
                // It is better to use REMOTE_IF_EMPTY in most cases to improve UX and performance.
                putParcelable(TaxParams.DATA_SOURCE, DataSource.REMOTE_IF_EMPTY)

                // Add pagination to improve UX and avoid TooLargeTransactionException
                putInt(TaxParams.PAGE_OFFSET, 0)
                putInt(TaxParams.PAGE_SIZE, 100)

                // sorting is optional. List can be sorted by any column in database.
                putString(TaxParams.SORT_BY, CatalogContract.Tax.Columns.UPDATED_AT)

                // filter is optional. In this example we do showing taxes which are enabled.
                putParcelable(TaxParams.FILTER_BY, FilterBy(CatalogContract.Tax.Columns.ENABLED, "1", FilterBy.ComparisonOperator.EQUAL))
            }
            val response = suspendCancellableCoroutine<Taxes?> {
                service.getTaxes(bundle, it.onSuccess(), it.onError())
            }
            update { copy(items = response?.taxes.orEmpty().map { it.mapToUiItems() }) }
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Taxes"),
        val items: List<TaxRecyclerItem> = emptyList()
    ) : ViewModelState
}
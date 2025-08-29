@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.tax

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.CatalogIntents
import com.godaddy.commerce.catalog.model.CatalogTax
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.common.FilterBy
import com.godaddy.commerce.provider.catalog.CatalogContract
import com.godaddy.commerce.sdk.catalog.TaxParamsExt
import com.godaddy.commerce.sdk.catalog.getCatalogTaxes
import com.godaddy.commerce.services.sample.common.extensions.subscribeOnUpdates
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

class TaxViewModel : CommonViewModel<TaxViewModel.State>(State()) {

    private val serviceClient = CommerceDependencyProvider.getCatalogService(viewModelScope)

    init {
        loadTaxes()
        CommerceDependencyProvider.getContext().subscribeOnUpdates(
            CatalogIntents.ACTION_TAXES_CHANGED
        ).onEach {
            loadTaxes()
        }.launchIn(viewModelScope)
    }

    fun loadTaxes() {
        execute {
            val service = serviceClient.getService().getOrThrow()
            val bundle = TaxParamsExt.toBundle(
                dataSource = DataSource.REMOTE_IF_EMPTY,
                pageOffset = 0,
                pageSize = 100,
                sortBy = CatalogContract.Tax.Columns.UPDATED_AT,
                includeClassification = true,
                includeOverrides = true,

            )
            val response = service.getCatalogTaxes(bundle)
            update { copy(items = response?.taxes.orEmpty().map { it.mapToUiItems() }) }
        }

    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Taxes"),
        val items: List<TaxRecyclerItem> = emptyList()
    ) : ViewModelState
}
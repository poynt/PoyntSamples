@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.tax.update

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.CatalogIntents
import com.godaddy.commerce.catalog.TaxConstants
import com.godaddy.commerce.catalog.TaxParams
import com.godaddy.commerce.catalog.models.*
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.inventory.models.*
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onComplete
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.extensions.subscribeOnUpdates
import com.godaddy.commerce.services.sample.common.extensions.toSimpleMoney
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commerce.taxes.models.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.*

class TaxUpdateViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<TaxUpdateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)
    private val id get() = savedStateHandle.get<String>("id")

    init {
        fetchTax()
        CommerceDependencyProvider.getContext()
            .subscribeOnUpdates(CatalogIntents.ACTION_TAXES_CHANGED)
            // refresh only when current tax is updated
            .filter { it.getString(TaxParams.TAX_ID) == id }
            .onEach { fetchTax() }
            .launchIn(viewModelScope)
    }

    private fun fetchTax() {
        viewModelScope.launch {
            val service = catalogServiceClient.getService().getOrThrow()

            val params = bundleOf(TaxParams.DATA_SOURCE to DataSource.REMOTE_IF_EMPTY)
            val response = suspendCancellableCoroutine<Tax?> {
                service.getTax(id, params, it.onSuccess(), it.onError())
            } ?: return@launch
            update {
                copy(
                    tax = Tax(name = response.name, taxRates = response.taxRates?.map {
                        TaxRate(
                            name = it.name,
                            amountType = it.amountType,
                            amount = it.amount,
                            ratePercentage = it.ratePercentage
                        )
                    }),
                    toolbarState = toolbarState.copy("Update Tax: ${response.name}")
                )
            }
        }
    }

    fun onNameChanged(value: String) {
        updateTax { copy(name = value) }
    }

    fun onRateNameChanged(value: String) {
        updateTaxRate { copy(name = value) }
    }

    fun onAmountTypeChanged(position: Int) {
        updateTaxRate { copy(amountType = state.amountTypes.getOrNull(position)) }
    }

    fun onRatePercentageChanged(value: String) {
        updateTaxRate { copy(ratePercentage = value) }
    }

    fun onAmountChanged(value: String) {
        updateTaxRate { copy(amount = value.toLongOrNull().toSimpleMoney()) }
    }

    fun update() {
        execute {
            val tax = requireNotNull(state.tax) { "Tax must be loaded" }

            val catalogService = catalogServiceClient.getService().getOrThrow()

            val response = suspendCancellableCoroutine<Tax?> {
                catalogService.patchTax(id, tax, Bundle.EMPTY, it.onSuccess(), it.onError())
            }

            sendEffect(Effect.ShowToast("Tax was updated: ${response?.id}"))
        }
    }

    fun delete() {
        execute {
            val catalogService = catalogServiceClient.getService().getOrThrow()

            // to remove tax need to remove tax association only

            // get tax association
            val items = suspendCancellableCoroutine<TaxAssociations?> {
                val associationParams = bundleOf(
                    TaxParams.TAX_ID to id,
                    TaxParams.DATA_SOURCE to DataSource.REMOTE_IF_EMPTY
                )
                catalogService.getTaxAssociations(associationParams, it.onSuccess(), it.onError())
            }

            Timber.d("Association items: ${items?.associations}")
            val association =
                requireNotNull(items?.associations?.firstOrNull()) { "There are no associations for current tax" }

            // remove tax association by id
            suspendCancellableCoroutine {
                catalogService.deleteTaxAssociation(
                    association.id?.toString(),
                    Bundle.EMPTY,
                    it.onComplete(),
                    it.onError()
                )
            }

            sendEffect(Effect.ShowToast("Tax $id was removed"))
            sendEffect(Effect.PopScreen)
        }
    }

    private fun updateTax(block: Tax.() -> Tax) {
        state.tax ?: return
        update { copy(tax = block(tax!!)) }
    }

    private fun updateTaxRate(block: TaxRate.() -> TaxRate) {
        updateTax { copy(taxRates = listOf(block(taxRates!!.first()))) }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Update Tax"),
        val tax: Tax? = null,
        val overrideAssociation: TaxOverrideAssociation? = null,
        val amountTypes: List<String> = TaxConstants.AmountType.values.toList(),
        val products: List<Product> = emptyList(),
        val showTaxAssociationProductDialog: Boolean = false,
        val showTaxOverrideAssociationProductDialog: Boolean = false,
    ) : ViewModelState
}

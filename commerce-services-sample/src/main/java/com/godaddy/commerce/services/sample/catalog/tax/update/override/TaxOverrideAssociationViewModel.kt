package com.godaddy.commerce.services.sample.catalog.tax.update.override

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import co.poynt.api.model.Business
import com.godaddy.commerce.catalog.ICatalogService
import com.godaddy.commerce.catalog.ProductParams
import com.godaddy.commerce.catalog.TaxConstants
import com.godaddy.commerce.catalog.TaxParams
import com.godaddy.commerce.catalog.models.Product
import com.godaddy.commerce.catalog.models.Products
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.sdk.business.BusinessParams
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onComplete
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.extensions.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.toSimpleMoney
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commerce.taxes.models.AssociationItems
import com.godaddy.commerce.taxes.models.TaxOverrideAssociation
import com.godaddy.commerce.taxes.models.TaxOverrideAssociationOverrideValue
import com.godaddy.commerce.taxes.models.TaxOverrideAssociations
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*

class TaxOverrideAssociationViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<TaxOverrideAssociationViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)
    private val id get() = savedStateHandle.get<String>("id")

    init {
        viewModelScope.launch {
            val service = catalogServiceClient.getService().getOrThrow()
            fetchTaxAssociations(service)
        }
    }

    private fun fetchTaxAssociations(service: ICatalogService) {
        viewModelScope.launch {
            val items = suspendCancellableCoroutine<TaxOverrideAssociations?> {
                val associationParams = bundleOf(
                    TaxParams.TAX_ID to id,
                    TaxParams.DATA_SOURCE to DataSource.REMOTE_IF_EMPTY
                )
                service.getTaxOverrideAssociations(associationParams, it.onSuccess(), it.onError())
            }
            update {
                val association = items?.associations?.firstOrNull() ?: throw IllegalStateException(
                    "There are no override associations for current tax"
                )
                copy(
                    associationId = requireNotNull(association.id?.toString()),
                    overrideAssociation = association.let {
                        TaxOverrideAssociation(
                            name = it.name,
                            type = it.type,
                            items = it.items,
                            overrideValue = it.overrideValue
                        )
                    }
                )
            }
        }
    }

    fun showProductDialog() {
        execute {
            if (state.products.isEmpty()) {
                val params = bundleOf(
                    // recommended data source is REMOTE_IF_EMPTY.
                    ProductParams.DATA_SOURCE to DataSource.REMOTE_IF_EMPTY,
                    // pagination is required otherwise exception can be thrown.
                    ProductParams.PAGE_SIZE to 100,
                    ProductParams.PAGE_OFFSET to 0,
                )
                val service = catalogServiceClient.getService().getOrThrow()
                val response = suspendCancellableCoroutine<Products?> {
                    service.getProducts(params, it.onSuccess(), it.onError())
                }
                update { copy(products = response?.products.orEmpty(), showProductDialog = true) }
            } else {
                update { copy(showProductDialog = true) }
            }
        }
    }

    fun hideProductsDialog() {
        update { copy(showProductDialog = false) }
    }


    fun onTaxOverrideRateNameChanged(value: String) {
        updateTaxOverrideRate { copy(name = value) }
    }

    fun onTaxOverrideRateAmountChanged(value: String) {
        updateTaxOverrideValue { copy(amount = value.toLongOrNull().toSimpleMoney()) }
    }

    fun onTaxOverrideRatePercentageChanged(value: String) {
        updateTaxOverrideValue { copy(ratePercentage = value) }
    }

    fun onTaxOverrideRateAmountTypeChanged(value: Int) {
        updateTaxOverrideValue { copy(amountType = state.amountTypes.getOrNull(value)) }
    }

    fun addOverrideForProduct(product: Product) {
        hideProductsDialog()
        execute {
            val association = state.overrideAssociation
            requireNotNull(association) { "Override Association is required to add product." }

            val items = association.items.orEmpty().toMutableList()

            // add new product item to association
            items.add(
                AssociationItems(TaxConstants.Association.TYPE_PRODUCT, id = product.productId)
            )
            update { copy(overrideAssociation = association.copy(items = items)) }
        }
    }

    fun removeOverrideForProduct(itemId: UUID) {
        execute {
            val association = state.overrideAssociation
            requireNotNull(association) { "Override Association is required." }

            val items = association.items.orEmpty().toMutableList()
            items.removeAll { it.id == itemId }
            // if there are no items in association then keep it empty and then delete on update request.
            update { copy(overrideAssociation = association.copy(items = items)) }
        }
    }

    fun update() {
        execute {
            val catalogService = catalogServiceClient.getService().getOrThrow()
            val association =
                requireNotNull(state.overrideAssociation) { "Association is not loaded" }

            // Delete association if there are no items
            if (association.items.isNullOrEmpty()) {
                suspendCancellableCoroutine {
                    catalogService.deleteTaxOverrideAssociation(
                        state.associationId,
                        Bundle.EMPTY,
                        it.onComplete(),
                        it.onError()
                    )
                }
                sendEffect(Effect.ShowToast("Item was deleted"))
                sendEffect(Effect.PopScreen)
            } else {
                suspendCancellableCoroutine<TaxOverrideAssociation?> {
                    catalogService.patchTaxOverrideAssociation(
                        state.associationId,
                        association,
                        Bundle.EMPTY,
                        it.onSuccess(),
                        it.onError()
                    )
                }?.id
                sendEffect(Effect.ShowToast("Item was updated"))
            }

        }
    }


    private suspend fun getStoreId(): UUID? {
        val service =
            CommerceDependencyProvider.getBusinessService(viewModelScope).getService().getOrThrow()
        val result = suspendCancellableCoroutine<Business> {
            service.getBusiness(
                bundleOf(BusinessParams.FROM_CLOUD to false),
                it.onSuccess(),
                it.onError()
            )
        }
        return result.stores.firstOrNull()?.id
    }

    private fun updateTaxOverrideRate(block: TaxOverrideAssociation.() -> TaxOverrideAssociation) {
        state.overrideAssociation ?: return
        update { copy(overrideAssociation = block(overrideAssociation!!)) }
    }

    private fun updateTaxOverrideValue(block: TaxOverrideAssociationOverrideValue.() -> TaxOverrideAssociationOverrideValue) {
        updateTaxOverrideRate { copy(overrideValue = block(overrideValue!!)) }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Update Tax Override Association"),
        val products: List<Product> = emptyList(),
        val overrideAssociation: TaxOverrideAssociation? = null,
        val associationId: String? = null,
        val amountTypes: List<String> = TaxConstants.AmountType.values.toList(),
        val showProductDialog: Boolean = false,
    ) : ViewModelState
}

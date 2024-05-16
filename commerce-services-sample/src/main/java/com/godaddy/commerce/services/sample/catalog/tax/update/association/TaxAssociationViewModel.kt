package com.godaddy.commerce.services.sample.catalog.tax.update.association

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
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.extensions.onSuccess
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commerce.taxes.models.AssociationItems
import com.godaddy.commerce.taxes.models.TaxAssociation
import com.godaddy.commerce.taxes.models.TaxAssociations
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*

class TaxAssociationViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<TaxAssociationViewModel.State>(State()) {

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
            val items = suspendCancellableCoroutine<TaxAssociations?> {
                val associationParams = bundleOf(
                    TaxParams.TAX_ID to id,
                    TaxParams.DATA_SOURCE to DataSource.REMOTE_IF_EMPTY
                )
                service.getTaxAssociations(associationParams, it.onSuccess(), it.onError())
            }
            update {
                val association = items?.associations?.firstOrNull() ?: throw IllegalStateException(
                    "There are no associations for current tax"
                )
                copy(
                    associationId = requireNotNull(association.id?.toString()),
                    association = association.let {
                        TaxAssociation(name = it.name, type = it.type, items = it.items)
                    })
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

    fun addProductToTax(product: Product) {
        hideProductsDialog()
        execute {
            val association = state.association
            requireNotNull(association) { "Association is required to add product." }

            val items = association.items.orEmpty().toMutableList()
            // make sure that items list has only store item or (product and categories)  items
            items.removeAll { it.type == TaxConstants.Association.TYPE_STORE }
            // add new product item to association
            items.add(
                AssociationItems(TaxConstants.Association.TYPE_PRODUCT, id = product.productId)
            )
            update { copy(association = association.copy(items = items)) }
        }
    }

    fun removeProductFromTax(itemId: UUID) {
        execute {
            val association = state.association
            requireNotNull(association) { "Association is required." }

            val items = association.items.orEmpty().toMutableList()
            items.removeAll { it.id == itemId }
            // if there are no items in association then create store item association.
            if (items.isEmpty()) {
                items.add(
                    AssociationItems(TaxConstants.Association.TYPE_STORE, id = getStoreId())
                )
            }
            update { copy(association = association.copy(items = items)) }
        }
    }

    fun update() {
        execute {
            val catalogService = catalogServiceClient.getService().getOrThrow()
            val association = requireNotNull(state.association) { "Association is not loaded" }
            val response = suspendCancellableCoroutine<TaxAssociation?> {
                catalogService.patchTaxAssociation(
                    state.associationId,
                    association,
                    Bundle.EMPTY,
                    it.onSuccess(),
                    it.onError()
                )
            }
            sendEffect(Effect.ShowToast("Item was updated"))
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

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Update Tax Association"),
        val products: List<Product> = emptyList(),
        val showProductDialog: Boolean = false,
        val associationId: String? = null,
        val association: TaxAssociation? = null,
    ) : ViewModelState
}

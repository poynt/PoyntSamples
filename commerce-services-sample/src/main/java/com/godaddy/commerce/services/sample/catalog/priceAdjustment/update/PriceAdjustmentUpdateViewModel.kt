package com.godaddy.commerce.services.sample.catalog.priceAdjustment.update

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import co.poynt.api.model.Business
import com.godaddy.commerce.catalog.*
import com.godaddy.commerce.catalog.models.Product
import com.godaddy.commerce.catalog.models.Products
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.priceadjustments.models.PriceAdjustment
import com.godaddy.commerce.priceadjustments.models.PriceAdjustmentAssociation
import com.godaddy.commerce.priceadjustments.models.PriceAdjustmentAssociationItemsInner
import com.godaddy.commerce.priceadjustments.models.PriceAdjustmentAssociations
import com.godaddy.commerce.sdk.business.BusinessParams
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.extensions.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.subscribeOnUpdates
import com.godaddy.commerce.services.sample.common.extensions.toSimpleMoney
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

class PriceAdjustmentUpdateViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<PriceAdjustmentUpdateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)
    private val id get() = savedStateHandle.get<String>("id")
    private val type get() = savedStateHandle.get<String>("type")

    init {
        fetchItem()
        CommerceDependencyProvider.getContext().run {
            // refresh only when current item is updated
            merge(
                subscribeOnUpdates(CatalogIntents.ACTION_DISCOUNTS_CHANGED)
                    .filter { it.getString(DiscountParams.DISCOUNT_ID) == id },
                subscribeOnUpdates(CatalogIntents.ACTION_FEES_CHANGED)
                    .filter { it.getString(FeeParams.FEE_ID) == id }
            )
                .onEach { fetchItem() }
                .launchIn(viewModelScope)
        }
    }

    private fun fetchItem() {
        viewModelScope.launch {
            val service = catalogServiceClient.getService().getOrThrow()

            val params = bundleOf(DiscountParams.DATA_SOURCE to DataSource.REMOTE_IF_EMPTY)
            val response = suspendCancellableCoroutine<PriceAdjustment?> {
                service.getPriceAdjustment(id, params, it.onSuccess(), it.onError())
            } ?: return@launch

            val association = suspendCancellableCoroutine<PriceAdjustmentAssociations?> {
                val idParam = when (type) {
                    PriceAdjustmentsConstants.Type.FEE -> FeeParams.FEE_ID
                    PriceAdjustmentsConstants.Type.DISCOUNT -> DiscountParams.DISCOUNT_ID
                    else -> throw IllegalStateException("Type is unknown: $type")
                }
                service.getPriceAdjustmentAssociations(
                    bundleOf(idParam to response.id?.toString()),
                    it.onSuccess(),
                    it.onError()
                )
            }?.associations?.firstOrNull()

            if (association == null) {
                sendEffect(Effect.ShowToast("Item is broken. There is no association related to price adjustment"))
            }

            update {
                copy(
                    priceAdjustment = PriceAdjustment(
                        name = response.name,
                        type = response.type,
                        amountType = response.amountType,
                        amount = response.amount,
                        ratePercentage = response.ratePercentage
                    ),
                    priceAdjustmentId = response.id?.toString(),
                    association = PriceAdjustmentAssociation(
                        priceAdjustmentId = association?.priceAdjustmentId,
                        items = association?.items
                    ),
                    associationId = association?.id?.toString(),
                    toolbarState = toolbarState.copy(title = "Update PriceAdjustment: ${response.name}")
                )
            }
        }
    }

    fun onNameChanged(value: String) {
        updatePriceAdjustment { copy(name = value) }
    }

    fun onRatePercentageChanged(value: String) {
        updatePriceAdjustment { copy(ratePercentage = value) }
    }

    fun onAmountChanged(value: String) {
        updatePriceAdjustment { copy(amount = value.toLongOrNull().toSimpleMoney()) }
    }

    fun onAmountTypeSelected(pos: Int) {
        updatePriceAdjustment { copy(amountType = state.amountTypes.getOrNull(pos)) }
    }

    fun onTypeSelected(pos: Int) {
        updatePriceAdjustment { copy(type = state.types.getOrNull(pos)) }
    }

    private fun updatePriceAdjustment(block: PriceAdjustment.() -> PriceAdjustment) {
        state.priceAdjustment ?: return
        update { copy(priceAdjustment = block(priceAdjustment!!)) }
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
                update {
                    copy(
                        products = response?.products.orEmpty(),
                        showProductDialog = true
                    )
                }
            } else {
                update { copy(showProductDialog = true) }
            }
        }
    }

    fun hideProductsDialog() {
        update { copy(showProductDialog = false) }
    }


    fun update() {
        execute {
            val catalogService = catalogServiceClient.getService().getOrThrow()

            // Step 1: update price adjustment
            val response = suspendCancellableCoroutine<PriceAdjustment?> {
                catalogService.postPriceAdjustment(
                    state.priceAdjustment,
                    Bundle.EMPTY,
                    it.onSuccess(),
                    it.onError()
                )
            }

            // Step 2: update price adjustment association.
            suspendCancellableCoroutine<PriceAdjustmentAssociation?> {
                catalogService.patchPriceAdjustmentAssociation(
                    state.associationId,
                    state.association,
                    Bundle.EMPTY,
                    it.onSuccess(),
                    it.onError()
                )
            }
            sendEffect(Effect.ShowToast("Price Adjustment was update with id: ${response?.id}"))
        }
    }


    fun addProduct(product: Product) {
        val association = state.association ?: return
        val items = association.items.orEmpty().toMutableList()

        // make sure that there is no type STORE and PRODUCT in association items list.
        items.removeAll { it.type == PriceAdjustmentsConstants.Association.TYPE_STORE }

        // add new product item
        items.add(
            PriceAdjustmentAssociationItemsInner(
                type = PriceAdjustmentsConstants.Association.TYPE_PRODUCT,
                id = product.productId?.toString()
            )
        )
        update { copy(association = association.copy(items = items)) }
    }

    fun removeItemFromAssociation(itemId: String) {
        execute {
            val association = state.association ?: return@execute
            val items = association.items.orEmpty().toMutableList()

            items.removeAll { it.id == itemId }

            // if list item is empty then bind it to STORE
            if (items.isEmpty()) {
                items.add(
                    PriceAdjustmentAssociationItemsInner(
                        type = PriceAdjustmentsConstants.Association.TYPE_STORE,
                        id = getStoreId()
                    )
                )
            }
            update { copy(association = association.copy(items = items)) }
        }
    }

    private suspend fun getStoreId(): String? {
        val service =
            CommerceDependencyProvider.getBusinessService(viewModelScope).getService().getOrThrow()
        val result = suspendCancellableCoroutine<Business> {
            service.getBusiness(
                bundleOf(BusinessParams.FROM_CLOUD to false),
                it.onSuccess(),
                it.onError()
            )
        }
        return result.stores.firstOrNull()?.id?.toString()
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Update PriceAdjustments"),
        val amountTypes: List<String> = PriceAdjustmentsConstants.AmountType.values.toList(),
        val types: List<String> = PriceAdjustmentsConstants.Type.values.toList(),
        val priceAdjustment: PriceAdjustment? = null,
        val priceAdjustmentId: String? = null,
        val association: PriceAdjustmentAssociation? = null,
        val associationId: String? = null,
        val products: List<Product> = emptyList(),
        val showProductDialog: Boolean = false,
    ) : ViewModelState
}
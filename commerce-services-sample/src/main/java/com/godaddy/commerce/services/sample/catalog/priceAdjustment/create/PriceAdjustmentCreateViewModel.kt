package com.godaddy.commerce.services.sample.catalog.priceAdjustment.create

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import co.poynt.api.model.Business
import com.godaddy.commerce.catalog.PriceAdjustmentsConstants
import com.godaddy.commerce.catalog.ProductParams
import com.godaddy.commerce.catalog.models.Product
import com.godaddy.commerce.catalog.models.Products
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.priceadjustments.models.PriceAdjustment
import com.godaddy.commerce.priceadjustments.models.PriceAdjustmentAssociation
import com.godaddy.commerce.priceadjustments.models.PriceAdjustmentAssociationItemsInner
import com.godaddy.commerce.sdk.business.BusinessParams
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.extensions.onSuccess
import com.godaddy.commerce.services.sample.common.extensions.toSimpleMoney
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import kotlinx.coroutines.suspendCancellableCoroutine

class PriceAdjustmentCreateViewModel :
    CommonViewModel<PriceAdjustmentCreateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)

    fun onNameChanged(value: String) {
        update { copy(name = value) }
    }

    fun onRatePercentageChanged(value: String) {
        update { copy(ratePercentage = value) }
    }

    fun onAmountChanged(value: String) {
        update { copy(amount = value.toLongOrNull()) }
    }

    fun onAmountTypeSelected(pos: Int) {
        update { copy(amountType = amountTypes.getOrNull(pos)) }
    }

    fun onTypeSelected(pos: Int) {
        update { copy(type = types.getOrNull(pos)) }
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

    fun selectProduct(product: Product) {
        update { copy(selectedProduct = product) }
    }

    fun create() {
        execute {
            val name = requireNotNull(state.name) { "Name is required" }

            val request = PriceAdjustment(
                name = name,
                amountType = state.amountType,
                amount = state.amount.toSimpleMoney(),
                ratePercentage = state.ratePercentage,
                type = state.type,
                enabled = true
            )
            val catalogService = catalogServiceClient.getService().getOrThrow()

            // Step 1: create price adjustment
            val response = suspendCancellableCoroutine<PriceAdjustment?> {
                catalogService.postPriceAdjustment(
                    request,
                    Bundle.EMPTY,
                    it.onSuccess(),
                    it.onError()
                )
            }


            // Step 2: create price adjustment association. Price Adjustment Association creates one-to-many relationship, one price adjustment to many products/categories or store.
            // !!! Does not allow to create store and products/categories in one association type. Only store or only products/categories.
            val items = associationItems()

            suspendCancellableCoroutine<PriceAdjustmentAssociation?> {
                val association = PriceAdjustmentAssociation(
                    priceAdjustmentId = response?.id,
                    name = "price-adjustment-association",
                    items = items,
                    type = PriceAdjustmentsConstants.Association.ASSOCIATION_TYPE,
                )
                catalogService.postPriceAdjustmentAssociation(
                    association,
                    Bundle.EMPTY,
                    it.onSuccess(),
                    it.onError()
                )
            }
            sendEffect(Effect.ShowToast("Price Adjustment was created with id: ${response?.id}"))
        }
    }

    private suspend fun associationItems() = if (state.selectedProduct == null) {
        listOf(
            PriceAdjustmentAssociationItemsInner(
                type = PriceAdjustmentsConstants.Association.TYPE_STORE,
                id = getStoreId()
            )
        )
    } else {
        listOf(
            PriceAdjustmentAssociationItemsInner(
                type = PriceAdjustmentsConstants.Association.TYPE_PRODUCT,
                id = state.selectedProduct?.productId?.toString()
            )
        )
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
        override val toolbarState: ToolbarState = ToolbarState(title = "Create PriceAdjustment"),
        val types: List<String> = PriceAdjustmentsConstants.Type.values.toList(),
        val type: String? = types.first(),
        val amountTypes: List<String> = PriceAdjustmentsConstants.AmountType.values.toList(),
        val name: String? = null,
        val amountType: String? = amountTypes.first(),
        val amount: Long? = null,
        val ratePercentage: String? = null,
        val selectedProduct: Product? = null,
        val products: List<Product> = emptyList(),
        val showProductDialog: Boolean = false,

        ) : ViewModelState
}
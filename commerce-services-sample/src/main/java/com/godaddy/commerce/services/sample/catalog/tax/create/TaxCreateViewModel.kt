@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.tax.create

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import co.poynt.api.model.Business
import com.godaddy.commerce.catalog.ProductParams
import com.godaddy.commerce.catalog.TaxConstants
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.catalog.model.CatalogProducts
import com.godaddy.commerce.catalog.model.CatalogTax
import com.godaddy.commerce.catalog.models.*
import com.godaddy.commerce.common.DataSource
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
import com.godaddy.commercecore.models.Amount
import com.godaddy.commercecore.models.Classification
import com.godaddy.commercecore.models.Money
import com.godaddy.commercecore.models.Override
import com.godaddy.commercecore.models.OverrideRate
import com.godaddy.commercecore.models.Percentage
import com.godaddy.commercecore.models.Tax
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*

class TaxCreateViewModel : CommonViewModel<TaxCreateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)

    fun onLabelChanged(value: String) {
        update { copy(label = value) }
    }

    fun onRatePercentageChanged(value: String) {
        updatePercentage( Percentage(value))
    }

    fun onAmountChanged(value: String) {
        updateAmount( Amount(Money(value)) )
    }
//
//    fun onTaxOverrideRateAmountChanged(value: String) {
//        updateTaxOverrideRate { copy(amount = value.toLongOrNull()) }
//    }

    fun onTaxOverrideRatePercentageChanged(value: String) {
        updateTaxOverrideRate { copy(ratePercentage = Percentage(value)) }
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
                val response = suspendCancellableCoroutine<CatalogProducts?> {
                    service.getCatalogProducts(params, it.onSuccess(), it.onError())
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


    fun switchTaxOverride(isShow: Boolean) {
        update { copy(createTaxOverride = isShow) }
    }

    fun selectProduct(product: CatalogProduct) {
        update { copy(selectedProduct = product) }
    }

    fun create() {
        execute {
            val label = requireNotNull(state.label) { "Name is required" }

            val request = CatalogTax(
                tax = Tax(
                    label = state.label,

                    percentage = state.percentage,
                    amount = state.amount,


                )
            )
            val catalogService = catalogServiceClient.getService().getOrThrow()

            // Step 1: create tax
            val response = suspendCancellableCoroutine<CatalogTax?> {
                catalogService.postCatalogTax(request, Bundle.EMPTY, it.onSuccess(), it.onError())
            }


            // Step 2: create tax association. Tax Association creates one-to-many relationship, one tax to many products/categories.
            // !!! Does not allow to create store and products/categories association type. Only store or only products/categories.
//            val items = associationItems()
//
//            suspendCancellableCoroutine<Tax?> {
//                val association = TaxAssociation(
//                    taxId = response?.id,
//                    name = "tax-association",
//                    items = items,
//                    type = TaxConstants.Association.TYPE_ASSOCIATION,
//                )
//                catalogService.postPriceAdjustmentAssociation(
//                    association,
//                    Bundle.EMPTY,
//                    it.onSuccess(),
//                    it.onError()
//                )
//            }

            // Step 3: We can create override rate for specific product/category. To do that need to create Tax Override Association.
//            if (state.createTaxOverride) {
//                requireNotNull(state.selectedProduct) { "Product is required to create override rate for the item" }
//                suspendCancellableCoroutine<TaxOverrideAssociation?> {
//                    val overrideAssociation = TaxOverrideAssociation(
//                        taxId = response?.id,
//                        name = "tax-override",
//                        items = items,
//                        type = TaxConstants.Association.TYPE_ASSOCIATION_OVERRIDE,
//                        overrideValue = state.taxOverriderRate.toTaxOverriderRate()
//                    )
//                    catalogService.postTaxOverrideAssociation(
//                        overrideAssociation,
//                        Bundle.EMPTY,
//                        it.onSuccess(),
//                        it.onError()
//                    )
//                }
//            }

            update { copy(createdId = response?.tax?.id?.toString()) }
        }
    }

//    private suspend fun associationItems() = if (state.selectedProduct == null) {
//        listOf(
//            AssociationItems(type = TaxConstants.Association.TYPE_STORE, id = getStoreId())
//        )
//    } else {
//        listOf(
//            AssociationItems(
//                type = TaxConstants.Association.TYPE_PRODUCT,
//                id = state.selectedProduct?.productId
//            )
//        )
//    }

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

    private fun updatePercentage(percentage: Percentage?) {
        update { copy(percentage = percentage) }
    }

    private fun updateAmount(amount: Amount?) {
        update { copy(amount = amount) }
    }


    private fun updateTaxOverrideRate(block: OverrideRate.() -> OverrideRate) {
        update { copy(taxOverrideRate = taxOverrideRate?.let { block(it) }) }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Create Tax"),
        val label: String? = null,
        val amount: Amount? = null,
        val percentage: Percentage? = null,
        val overrides: List<Override>? = null,
        val selectedOverride: Override? = null,
        val selectedProduct: CatalogProduct? = null,
        val products: List<CatalogProduct> = emptyList(),
        val showProductDialog: Boolean = false,
        val createdId: String? = null,
        val taxOverrideRate: OverrideRate? = null,
        val createTaxOverride: Boolean = false,
    ) : ViewModelState
}
@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.tax.create

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import co.poynt.api.model.Business
import com.godaddy.commerce.catalog.ProductParams
import com.godaddy.commerce.catalog.TaxConstants
import com.godaddy.commerce.catalog.models.*
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.inventory.models.*
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
import com.godaddy.commerce.taxes.models.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.*

class TaxCreateViewModel : CommonViewModel<TaxCreateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)

    fun onNameChanged(value: String) {
        update { copy(name = value) }
    }

    fun onDescriptionChanged(value: String) {
        update { copy(name = value) }
    }

    fun onAmountTypeChanged(position: Int) {
        updateTaxRate { copy(amountTypeSelected = state.amountTypes.getOrNull(position)) }
    }

    fun onRatePercentageChanged(value: String) {
        updateTaxRate { copy(ratePercentage = value.toFloatOrNull()) }
    }

    fun onAmountChanged(value: String) {
        updateTaxRate { copy(amount = value.toLongOrNull()) }
    }

    fun onTaxOverrideRateAmountChanged(value: String) {
        updateTaxOverrideRate { copy(amount = value.toLongOrNull()) }
    }

    fun onTaxOverrideRatePercentageChanged(value: String) {
        updateTaxOverrideRate { copy(ratePercentage = value.toFloatOrNull()) }
    }

    fun onTaxOverrideRateAmountTypeChanged(value: Int) {
        updateTaxOverrideRate { copy(amountTypeSelected = state.amountTypes.getOrNull(value)) }
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


    fun switchTaxOverride(isShow: Boolean) {
        update { copy(createTaxOverride = isShow) }
    }

    fun selectProduct(product: Product) {
        update { copy(selectedProduct = product) }
    }

    fun create() {
        execute {
            val name = requireNotNull(state.name) { "Name is required" }

            val request = Tax(
                name = name,
                description = state.description,
                taxRates = listOf(state.taxRate.toModel()),
                enabled = true
            )
            val catalogService = catalogServiceClient.getService().getOrThrow()

            // Step 1: create tax
            val response = suspendCancellableCoroutine<Tax?> {
                catalogService.postTax(request, Bundle.EMPTY, it.onSuccess(), it.onError())
            }


            // Step 2: create tax association. Tax Association creates one-to-many relationship, one tax to many products/categories.
            // !!! Does not allow to create store and products/categories association type. Only store or only products/categories.
            val items = associationItems()

            suspendCancellableCoroutine<TaxAssociation?> {
                val association = TaxAssociation(
                    taxId = response?.id,
                    name = "tax-association",
                    items = items,
                    type = TaxConstants.Association.TYPE_ASSOCIATION,
                )
                catalogService.postTaxAssociation(
                    association,
                    Bundle.EMPTY,
                    it.onSuccess(),
                    it.onError()
                )
            }

            // Step 3: We can create override rate for specific product/category. To do that need to create Tax Override Association.
            if (state.createTaxOverride) {
                requireNotNull(state.selectedProduct) { "Product is required to create override rate for the item" }
                suspendCancellableCoroutine<TaxOverrideAssociation?> {
                    val overrideAssociation = TaxOverrideAssociation(
                        taxId = response?.id,
                        name = "tax-override",
                        items = items,
                        type = TaxConstants.Association.TYPE_ASSOCIATION_OVERRIDE,
                        overrideValue = state.taxOverriderRate.toTaxOverriderRate()
                    )
                    catalogService.postTaxOverrideAssociation(
                        overrideAssociation,
                        Bundle.EMPTY,
                        it.onSuccess(),
                        it.onError()
                    )
                }
            }

            update { copy(createdId = response?.id?.toString()) }
        }
    }

    private suspend fun associationItems() = if (state.selectedProduct == null) {
        listOf(
            AssociationItems(type = TaxConstants.Association.TYPE_STORE, id = getStoreId())
        )
    } else {
        listOf(
            AssociationItems(
                type = TaxConstants.Association.TYPE_PRODUCT,
                id = state.selectedProduct?.productId
            )
        )
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

    private fun State.Rate.toModel(): TaxRate {
        validateRate()
        return TaxRate(
            amount = amount.toSimpleMoney(),
            ratePercentage = ratePercentage.toString(),
            amountType = amountTypeSelected
        )
    }

    private fun State.Rate.toTaxOverriderRate(): TaxOverrideAssociationOverrideValue {
        validateRate()
        return TaxOverrideAssociationOverrideValue(
            amountType = amountTypeSelected,
            ratePercentage = ratePercentage?.toString(),
            amount = amount.toSimpleMoney()
        )
    }

    private fun State.Rate.validateRate() {
        val amountType = requireNotNull(amountTypeSelected) {
            "Amount type is required"
        }
        if (TaxConstants.AmountType.FIXED == amountType && amount == null) {
            throw IllegalStateException("Amount can't be null for type FIXED")
        }
        if (TaxConstants.AmountType.PERCENTAGE == amountType && ratePercentage == null) {
            throw IllegalStateException("Rate percentage can't be null for type PERCENTAGE")
        }
    }


    private fun updateTaxRate(block: State.Rate.() -> State.Rate) {
        update { copy(taxRate = block(taxRate)) }
    }

    private fun updateTaxOverrideRate(block: State.Rate.() -> State.Rate) {
        update { copy(taxOverriderRate = block(taxOverriderRate)) }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Create Tax"),
        val amountTypes: List<String> = TaxConstants.AmountType.values.toList(),
        val name: String? = null,
        val description: String? = null,
        val taxRate: Rate = Rate(),
        val taxOverriderRate: Rate = Rate(),
        val createTaxOverride: Boolean = false,
        val selectedProduct: Product? = null,
        val products: List<Product> = emptyList(),
        val showProductDialog: Boolean = false,
        val createdId: String? = null,
    ) : ViewModelState {

        data class Rate(
            val amountTypeSelected: String? = null,
            val amount: Long? = null,
            val ratePercentage: Float? = null,
        )
    }
}
@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.tax.update

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.CatalogIntents
import com.godaddy.commerce.catalog.TaxParams
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.catalog.model.CatalogTax
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.provider.catalog.CatalogContract
import com.godaddy.commerce.sdk.catalog.ProductParamsExt
import com.godaddy.commerce.sdk.catalog.TaxParamsExt
import com.godaddy.commerce.sdk.catalog.deleteCatalogTax
import com.godaddy.commerce.sdk.catalog.getCatalogProducts
import com.godaddy.commerce.sdk.catalog.getCatalogTax
import com.godaddy.commerce.sdk.catalog.patchCatalogTax
import com.godaddy.commerce.sdk.util.isNotNullOrBlank
import com.godaddy.commerce.services.sample.common.extensions.subscribeOnUpdates
import com.godaddy.commerce.services.sample.common.util.DEFAULT_CURRENCY_CODE
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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class TaxUpdateViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<TaxUpdateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)
    private val id get() = savedStateHandle.get<String>("id")

    init {
        fetchTax()
        loadProducts()
        CommerceDependencyProvider.getContext()
            .subscribeOnUpdates(CatalogIntents.ACTION_TAXES_CHANGED)
            // refresh only when current tax is updated
            .filter { it.getString(TaxParams.TAX_ID) == id }
            .onEach { fetchTax() }
            .launchIn(viewModelScope)
    }

    private fun fetchTax() {
        execute {
            val service = catalogServiceClient.getService().getOrThrow()
            val request = TaxParamsExt.toBundle(
                taxId = id,
                dataSource = DataSource.REMOTE_IF_EMPTY,
                includeClassification = true,
                includeOverrides = true,
            )
            val response = service.getCatalogTax(id.orEmpty(), request)
            update {
                val tax = requireNotNull(response?.tax)

                val classifications = tax.classifications.orEmpty()
                val selectedClassification = classifications.firstOrNull()
                val classificationLabel = selectedClassification?.label.orEmpty()
                val classificationProductIds = selectedClassification?.productIds.orEmpty()

                val overrides = tax.overrides.orEmpty()
                val selectedOverride = overrides.firstOrNull()
                val overrideLabel = selectedOverride?.label.orEmpty()
                val customRate = selectedOverride?.customRate
                val overrideProductIds = selectedOverride?.productIds.orEmpty()
                copy(
                    label = tax.label,
                    amount = tax.amount,
                    percentage = tax.percentage,
                    status = tax.status,
                    selectedClassification = selectedClassification,
                    availableClassifications = classifications,
                    classificationLabel = classificationLabel,
                    classificationProductIds = classificationProductIds,
                    selectedOverride = selectedOverride,
                    availableOverrides = overrides,
                    overrideLabel = overrideLabel,
                    overrideProductIds = overrideProductIds,
                    customRate = customRate,
                    taxType = if (tax.amount != null) "Amount" else "Percentage",
                    toolbarState = toolbarState.copy(title = "Update Tax: ${tax.label}")
                )
            }
        }
    }

    fun onLabelChanged(value: String) {
        update { copy(label = value) }
    }
    fun onRatePercentageChanged(value: String) {
        update { copy(percentage = Percentage(value)) }
    }
    fun onAmountChanged(value: String) {
        value.toLongOrNull()?.let {
            update {
                copy(amount = Amount(Money(currencyCode = DEFAULT_CURRENCY_CODE, it)))
            }
        }
    }
    fun onTaxOverrideLabelChanged(value: String) {
        update { copy(overrideLabel = value) }
    }
    fun onTaxOverrideCustomRateChanged(value: String) {
        update { copy(customRate = OverrideRate(ratePercentage = Percentage(value)))}
    }
    fun onTaxClassificationLabelChanged(value: String) {
        update { copy(classificationLabel = value) }
    }
    fun onTaxTypeChange(position: Int) {
        update { copy(
            taxType = state.types[position]
        ) }
    }
    private fun loadProducts(query: String? = null) {
        execute {
            val service = catalogServiceClient.getService().getOrThrow()
            val bundle = ProductParamsExt.toBundle(
                dataSource = DataSource.REMOTE_IF_EMPTY,
                pageOffset = DEFAULT_TAX_PRODUCTS_PAGE_OFFSET,
                pageSize = DEFAULT_TAX_PRODUCTS_PAGE_SIZE,
                sortBy = CatalogContract.Product.Columns.UPDATED_AT,
                searchTerm = query,
            )
            val response = service.getCatalogProducts(bundle)
            val products = response?.products.orEmpty().toSet()
            update { copy (allProducts = products) }
        }
    }
    fun switchTaxOverride(isShow: Boolean) {
        update { copy(updateTaxOverride = isShow) }
    }
    fun switchTaxClassification(isShow: Boolean) {
        update { copy(updateTaxClassification = isShow) }
    }
    fun hideDialog(){
        update{ copy(dialogType = DialogType.NO_SHOW) }
    }
    fun addClassificationProduct(){
        update {
            val classificationProducts = allProducts.filter {
                !classificationProductIds.contains(it.product.id)
            }
            copy(
                dialogType = DialogType.ADD_CLASSIFICATION,
                dialogList = classificationProducts
            )
        }
    }
    fun removeClassificationProduct(){
        update {
            val classificationProducts = allProducts.filter {
                classificationProductIds.contains(it.product.id)
            }
            copy(
                dialogType = DialogType.REMOVE_CLASSIFICATION,
                dialogList = classificationProducts
            )
        }
    }
    fun addOverrideProduct(){
        update {
            val overrideProducts = allProducts.filter {
                !overrideProductIds.contains(it.product.id)
            }
            copy(
                dialogType = DialogType.ADD_OVERRIDE,
                dialogList = overrideProducts
            )
        }
    }
    fun removeOverrideProduct(){
        update {
            val overrideProducts = allProducts.filter {
                overrideProductIds.contains(it.product.id)
            }
            copy(
                dialogType = DialogType.REMOVE_OVERRIDE,
                dialogList = overrideProducts
            )
        }
    }
    fun handleProduct(catalogProduct: CatalogProduct, dialogType: DialogType) {
        val id = catalogProduct.product.id.toString()
        if (dialogType == DialogType.ADD_CLASSIFICATION){
            update { copy (classificationProductIds = state.classificationProductIds.plus(id)) }
        }
        else if (state.dialogType == DialogType.ADD_OVERRIDE){
            update { copy (overrideProductIds = state.overrideProductIds.plus(id)) }
        }
        else if (dialogType == DialogType.REMOVE_CLASSIFICATION){
            update { copy (classificationProductIds = state.classificationProductIds.minus(id)) }
        }
        else if (dialogType == DialogType.REMOVE_OVERRIDE){
            update { copy (overrideProductIds = state.overrideProductIds.minus(id)) }
        }
    }

    fun update() {
        execute {
            val classifications = if (state.availableClassifications.isNotEmpty()) listOf(
                Classification(
                    id = state.selectedClassification?.id,
                    label = state.classificationLabel,
                    productIds = state.classificationProductIds
                )
            ) else emptyList()

            val overrides = if (state.availableOverrides.isNotEmpty()) listOf(
                Override(
                    id = state.selectedOverride?.id,
                    label = state.overrideLabel,
                    customRate = state.customRate,
                    productIds = state.overrideProductIds
                )
            ) else emptyList()

            val percentage = if (state.taxType == "Percentage") state.percentage else null
            val amount = if (state.taxType == "Amount") state.amount else null

            val tax = Tax(
                id = id,
                status = requireNotNull(state.status),
                label = requireNotNull(state.label),
                percentage = percentage,
                amount = amount,
                classifications = classifications,
                overrides = overrides,
            )

            val request = CatalogTax(tax)
            val catalogService = catalogServiceClient.getService().getOrThrow()
            val response = catalogService.patchCatalogTax(requireNotNull(id), request)
            update { copy(updatedTaxId = response?.tax?.id) }
            sendEffect(Effect.ShowToast("Tax was updated: ${response?.tax?.id}"))
        }
    }
    fun delete() {
        execute {
            val catalogService = catalogServiceClient.getService().getOrThrow()
            val request = TaxParamsExt.toBundle(
                taxId = TaxParams.TAX_ID,
                dataSource = DataSource.REMOTE_IF_EMPTY,
                includeOverrides = true,
                includeClassification = true,
            )
            val response = catalogService.deleteCatalogTax(id.orEmpty(), request)
        }
            sendEffect(Effect.ShowToast("Tax $id was removed"))
            sendEffect(Effect.PopScreen)
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Update Tax"),
        // Tax Fields
        val updatedTaxId: String? = null,
        val status: String? = null,
        val label: String? = null,
        val amount: Amount? = null,
        val percentage: Percentage? = null,
        val createdId: String? = null,
        val types: List<String> = listOf("Amount", "Percentage"),
        val taxType: String = "Amount",

        val availableOverrides: List<Override> = emptyList(),
        val availableClassifications: List<Classification> = emptyList(),
        val selectedOverride: Override? = null,
        val selectedClassification: Classification? = null,

        // Override Fields
        val overrideLabel: String? = null,
        val customRate: OverrideRate? = null,
        val updateTaxOverride: Boolean = false,

        // Classification Fields
        val classificationLabel: String? = null,
        val updateTaxClassification: Boolean = false,

        // Fields for handling product mapping
        val classificationProductIds: Set<String> = emptySet(),
        val overrideProductIds: Set<String> = emptySet(),

        val allProducts: Set<CatalogProduct> = emptySet(),
        val dialogList: List<CatalogProduct> = emptyList(),
        val dialogType: DialogType? = null,
    ) : ViewModelState

    companion object{
        private const val DEFAULT_TAX_PRODUCTS_PAGE_SIZE = 100
        private const val DEFAULT_TAX_PRODUCTS_PAGE_OFFSET = 0
    }
    enum class DialogType{
        ADD_CLASSIFICATION,
        ADD_OVERRIDE,
        REMOVE_CLASSIFICATION,
        REMOVE_OVERRIDE,
        NO_SHOW,
    }
}

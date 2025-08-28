@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.tax.create

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.catalog.model.CatalogTax
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.provider.catalog.CatalogContract
import com.godaddy.commerce.sdk.catalog.ProductParamsExt
import com.godaddy.commerce.sdk.catalog.getCatalogProducts
import com.godaddy.commerce.sdk.catalog.postCatalogTax
import com.godaddy.commerce.services.sample.common.util.DEFAULT_CURRENCY_CODE
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commercecore.models.Amount
import com.godaddy.commercecore.models.Classification
import com.godaddy.commercecore.models.Money
import com.godaddy.commercecore.models.Override
import com.godaddy.commercecore.models.OverrideRate
import com.godaddy.commercecore.models.Percentage
import com.godaddy.commercecore.models.Tax
import kotlinx.coroutines.FlowPreview
import timber.log.Timber

class TaxCreateViewModel : CommonViewModel<TaxCreateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)

    init {
        loadProducts()
    }
    fun onLabelChanged(value: String) {
        update { copy(label = value) }
    }
    fun onRatePercentageChanged(value: String) {
        update { copy(percentage = Percentage(value, value)) }
    }
    fun onAmountChanged(value: String) {
        value.toLongOrNull()?.let {
            val amount = Money(DEFAULT_CURRENCY_CODE, it)
            update {
                copy(amount = Amount(amount, amount))
            }
        }
    }
    fun onTaxOverrideLabelChanged(value: String) {
        update { copy(overrideLabel = value) }
    }
    fun onTaxOverrideCustomRateChanged(value: String) {
        update {
            copy(customRate = OverrideRate(ratePercentage = Percentage(value)))
        }
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
            val products = response?.products.orEmpty()
            update { copy (allProducts = products) }
        }
    }
    fun switchTaxOverride(isShow: Boolean) {
        update { copy(createTaxOverride = isShow) }
    }
    fun switchTaxClassification(isShow: Boolean) {
        update { copy(createTaxClassification = isShow) }
    }
    fun hideDialog(){
        update{ copy(dialogType = DialogType.NO_SHOW) }
    }
    fun addClassificationProduct(){
        update { copy(dialogType = DialogType.ADD_CLASSIFICATION) }
    }
    fun removeClassificationProduct(){
        update { copy(dialogType = DialogType.REMOVE_CLASSIFICATION) }
    }
    fun addOverrideProduct(){
        update { copy(dialogType = DialogType.ADD_OVERRIDE) }
    }
    fun removeOverrideProduct(){
        update { copy(dialogType = DialogType.REMOVE_OVERRIDE) }
    }
    fun handleProduct(catalogProduct: CatalogProduct, dialogType: DialogType) {
        if (dialogType == DialogType.ADD_CLASSIFICATION){
            update{ copy (classificationProducts = state.classificationProducts.plus(catalogProduct)) }
        }
        else if (state.dialogType == DialogType.ADD_OVERRIDE){
            update{ copy (overrideProducts = state.overrideProducts.plus(catalogProduct)) }
        }
        else if (dialogType == DialogType.REMOVE_CLASSIFICATION){
            update{ copy (classificationProducts = state.classificationProducts.minus(catalogProduct)) }
        }
        else if (dialogType == DialogType.REMOVE_OVERRIDE){
            update{ copy (overrideProducts = state.overrideProducts.minus(catalogProduct)) }
        }
    }
    fun create() {
        execute {
            val classifications = if (state.createTaxClassification) listOf(
                Classification(
                    label = state.classificationLabel,
                    productIds = state.classificationProducts.map {it.product.id.toString()}.toSet()
                )
            ) else emptyList()

            val overrides = if (state.createTaxOverride) listOf(
                Override(
                    label = state.overrideLabel,
                    customRate = state.customRate,
                    productIds = state.overrideProducts.map {it.product.id.toString()}.toSet()
                )
            ) else emptyList()

            val percentage = if (state.taxType == "Percentage") state.percentage else null
            val amount = if (state.taxType == "Amount") state.amount else null

            val tax = Tax(
                label = requireNotNull(state.label),
                percentage = percentage,
                amount = amount,
                classifications = classifications,
                overrides = overrides,
            )
            val request = CatalogTax(tax = tax)
            val catalogService = catalogServiceClient.getService().getOrThrow()
            val response = catalogService.postCatalogTax(request)
            update { copy(createdId = response?.tax?.id) }
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Create Tax"),
        // Tax Fields
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

        // New Override Fields
        val overrideLabel: String? = null,
        val customRate: OverrideRate? = null,
        val createTaxOverride: Boolean = false,

        // New Classification Fields
        val classificationLabel: String? = null,
        val createTaxClassification: Boolean = false,

        // Fields for handling product mapping
        val classificationProducts: Set<CatalogProduct> = emptySet(),
        val overrideProducts: Set<CatalogProduct> = emptySet(),
        val allProducts: List<CatalogProduct> = emptyList(),
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
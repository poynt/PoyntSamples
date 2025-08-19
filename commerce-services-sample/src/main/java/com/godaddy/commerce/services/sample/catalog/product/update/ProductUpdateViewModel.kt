@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.product.update

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.sdk.catalog.ProductParamsExt
import com.godaddy.commerce.sdk.catalog.getCatalogProduct
import com.godaddy.commerce.sdk.catalog.updateCatalogProduct
import com.godaddy.commerce.services.sample.common.util.DEFAULT_CURRENCY_CODE
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getCatalogService
import com.godaddy.commercecore.models.InventoryInfo
import com.godaddy.commercecore.models.Money
import com.godaddy.commercecore.models.PricingInfo
import com.godaddy.commercecore.models.Product
import com.godaddy.commercecore.models.SellableProduct
import kotlinx.coroutines.FlowPreview
class ProductUpdateViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<ProductUpdateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)

    private val id get() = requireNotNull(savedStateHandle.get<String>("id"))

    init {
        loadProduct()
    }
    
    private fun setProductState(response: CatalogProduct?){
        update {
            val product = response?.product
            val pricingInfo = product?.pricingInfos?.firstOrNull()
            val sellableProduct = product?.sellableProducts?.firstOrNull()
            val inventoryInfo = sellableProduct?.inventoryInfos?.firstOrNull()
            copy(
                updatedLabel = product?.label,
                updatedPrice = pricingInfo?.price,
                updatedSalePrice = pricingInfo?.salePrice,
                updatedQuantity = inventoryInfo?.quantity,
                updatedThreshold = inventoryInfo?.threshold,
                updatedProduct = product,
                updatedSellableProduct = sellableProduct,
                updatedPricingInfoId = pricingInfo?.id,
            )
        }
    }
    
    private fun loadProduct() {
        execute {
            val service = catalogServiceClient.getService().getOrThrow()
            val bundle = ProductParamsExt.toBundle(dataSource = DataSource.REMOTE_IF_EMPTY)
            val response = service.getCatalogProduct(id, bundle)
            setProductState(response)
        }
    }
    fun onProductLabelUpdated(value: String) {
        update { copy(
            updatedLabel = value,
            updatedShortLabel = value.take(DEFAULT_PRODUCT_SHORT_LABEL_SIZE)
        ) }
    }
    fun onProductPriceUpdated(value: String) {
        value.toLongOrNull()?.let{
            update { copy(
                updatedPrice = Money(
                    value = it,
                    currencyCode = DEFAULT_CURRENCY_CODE
                )
            ) }
        }
    }
    fun onProductSalePriceUpdated(value: String) {
        value.toLongOrNull()?.let{
            update { copy(
                updatedSalePrice = Money(
                    value = it,
                    currencyCode = DEFAULT_CURRENCY_CODE
                )
            ) }
        }
    }
    fun onProductQuantityUpdated(value: String) {
        value.toIntOrNull()?.let{
            update { copy(updatedQuantity = it) }
        }
    }
    fun onProductThresholdUpdated(value: String) {
        value.toIntOrNull()?.let{
            update { copy(updatedThreshold = it) }
        }
    }

    fun updateProduct() {
        execute {
            val label = state.updatedLabel
            val shortLabel = state.updatedShortLabel
            val pricingInfos = listOf(PricingInfo(
                id = state.updatedPricingInfoId,
                price = state.updatedPrice,
                salePrice = state.updatedSalePrice
            ))
            val disablePriceOverride = (state.updatedPrice != null)
            val inventoryInfos =
                if (state.updatedEnableInventoryTracking)
                    listOf(InventoryInfo(
                        quantity = state.updatedQuantity,
                        threshold = state.updatedThreshold
                    ))
                else emptyList()
            val enableInventoryTracking = (state.updatedQuantity != null)
            val updatedSellableProducts = listOf(SellableProduct(
                id = state.updatedSellableProduct?.id,
                label = label,
                shortLabel = shortLabel,
                skuCode = state.updatedSellableProduct?.skuCode,
                pricingInfos = pricingInfos,
                inventoryInfos = inventoryInfos,
                disablePriceOverride = disablePriceOverride,
                enableInventoryTracking = enableInventoryTracking,
                attributeValues = state.updatedSellableProduct?.attributeValues,
                status = state.updatedSellableProduct?.status
            ))
            val updatedProduct = Product(
                id = state.updatedProduct?.id,
                label = label,
                shortLabel = shortLabel,
                pricingInfos = pricingInfos,
                sellableProducts = updatedSellableProducts,
                type = state.updatedProduct?.type,
                status = state.updatedProduct?.status,
                sellInPerson = state.updatedProduct?.sellInPerson,
            )
            val request = CatalogProduct(updatedProduct)
            val catalogService = catalogServiceClient.getService().getOrThrow()

            val response = catalogService.updateCatalogProduct(id, request)
            setProductState(response)
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Update Product"),

        val updatedProduct: Product? = null,
        val updatedSellableProduct: SellableProduct? = null,
        val updatedLabel: String? = null,
        val updatedShortLabel: String? = null,
        val updatedPrice: Money? = null,
        val updatedSalePrice: Money? = null,
        val updatedQuantity: Int? = null,
        val updatedThreshold: Int? = null,
        val updatedProductId: String? = null,
        val updatedPricingInfoId: String? = null,
        val updatedEnableInventoryTracking: Boolean = true,
    ) : ViewModelState

    companion object{
        private const val DEFAULT_PRODUCT_SHORT_LABEL_SIZE = 5
    }
}
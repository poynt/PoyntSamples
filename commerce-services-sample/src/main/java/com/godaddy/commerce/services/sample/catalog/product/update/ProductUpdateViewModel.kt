@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.catalog.product.update

import android.os.Bundle
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.catalog.ProductParams
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.sdk.util.toOrNull
import com.godaddy.commerce.services.sample.catalog.onSuccess
import com.godaddy.commerce.services.sample.catalog.product.SkuFormatter
import com.godaddy.commerce.services.sample.common.extensions.onError
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
import kotlinx.coroutines.suspendCancellableCoroutine
class ProductUpdateViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<ProductUpdateViewModel.State>(State()) {

    private val catalogServiceClient = getCatalogService(viewModelScope)

    private val id get() = savedStateHandle.get<String>("id")
    private val skuFormatter = SkuFormatter()

    init {
        loadProduct()
    }

    private fun loadProduct() {
        execute {
            val service = catalogServiceClient.getService().getOrThrow()
            val bundle = Bundle().apply {
                putParcelable(ProductParams.DATA_SOURCE, DataSource.REMOTE_IF_EMPTY)
            }

            val response = suspendCancellableCoroutine<CatalogProduct?> {
                service.getCatalogProduct(id, bundle, it.onSuccess(), it.onError())
            }
            update { copy (
                updatedLabel = response?.product?.label,
                updatedPrice = response?.product?.pricingInfos?.firstOrNull()?.price,
                updatedSalePrice = response?.product?.pricingInfos?.firstOrNull()?.salePrice,
                updatedQuantity = response?.product?.sellableProducts?.firstOrNull()?.inventoryInfos?.firstOrNull()?.quantity,
                updatedThreshold = response?.product?.sellableProducts?.firstOrNull()?.inventoryInfos?.firstOrNull()?.threshold,
                catalogProduct = response
            )}
        }
    }

    fun onProductLabelUpdated(value: String) {
        //TODO: Optimal is read from string one by one and replace all difference in short label
        // fairly trivial since its only a 5 char operation
        update { copy(
            updatedLabel = value,
            updatedShortLabel = value.take(5)
        ) }
    }
    fun onProductPriceUpdated(value: String) {
        value.toLongOrNull()?.let{
            update { copy(updatedPrice = Money(value = it, currencyCode = "USD")) }
        }
    }
    fun onProductSalePriceUpdated(value: String) {
        value.toLongOrNull()?.let{
            update { copy(updatedSalePrice = Money(value = it, currencyCode = "USD")) }
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
            val product = requireNotNull(state.catalogProduct?.product)
            val sellableProduct = requireNotNull(product.sellableProducts?.first())

            val label = state.updatedLabel ?: product.label
            val shortLabel = state.updatedShortLabel ?: product.shortLabel

            val pricingInfo = requireNotNull(product.pricingInfos?.first())
            val pricingInfos = listOf(PricingInfo(
                id = pricingInfo.id,
                price = state.updatedPrice ?: pricingInfo.price,
                salePrice = state.updatedSalePrice ?: pricingInfo.salePrice
            ))
            val inventoryInfos =
                if (state.updatedEnableInventoryTracking) listOf(InventoryInfo(quantity = state.updatedQuantity, threshold = state.updatedThreshold))
                else if (requireNotNull(sellableProduct.enableInventoryTracking)) sellableProduct.inventoryInfos
                else emptyList<InventoryInfo>()

            val updatedSellableProducts = listOf(
                SellableProduct(
                    id = sellableProduct.id,
                    label = label,
                    shortLabel = shortLabel,
                    skuCode = sellableProduct.skuCode,
                    pricingInfos = pricingInfos,
                    inventoryInfos = inventoryInfos,
                    disablePriceOverride = sellableProduct.disablePriceOverride,
                    enableInventoryTracking = sellableProduct.enableInventoryTracking,
                    attributeValues = sellableProduct.attributeValues,
                    status = sellableProduct.status,
                )
            )
            val updatedProduct = Product(
                id = product.id,
                label = label,
                shortLabel = shortLabel,
                pricingInfos = pricingInfos, // Pricing infos should be here if no attribtues
                sellableProducts = updatedSellableProducts,
                type = product.type,
                status = product.status,
                sellInPerson = product.sellInPerson,
            )

            val request = CatalogProduct(updatedProduct)

            val catalogService = catalogServiceClient.getService().getOrThrow()

            val response = suspendCancellableCoroutine<CatalogProduct?> {
                catalogService.updateCatalogProduct(
                    id,
                    request,
                    Bundle.EMPTY,
                    it.onSuccess(),
                    it.onError()
                )
            }
            update {
                copy(
                    updatedProductId = response?.product?.id,
                )
            }
        }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Update Product"),

        val catalogProduct: CatalogProduct? = null,
        val updatedLabel: String? = null,
        val updatedShortLabel: String? = null,
        val updatedPrice: Money? = null,
        val updatedSalePrice: Money? = null,
        val updatedQuantity: Int? = null,
        val updatedThreshold: Int? = null,
        val updatedProductId: String? = null,
        val updatedEnableInventoryTracking: Boolean = true,
    ) : ViewModelState
}
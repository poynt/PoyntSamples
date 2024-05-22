package com.godaddy.commerce.services.sample.orders.create

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import com.amirkhawaja.Ksuid
import com.godaddy.commerce.common.models.SimpleMoney
import com.godaddy.commerce.order.OrderConstants
import com.godaddy.commerce.orders.models.*
import com.godaddy.commerce.sdk.business.BusinessParams
import com.godaddy.commerce.sdk.business.getBusiness
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getBusinessService
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider.getOrderService
import com.godaddy.commerce.services.sample.orders.create.OrderCreateViewModel.State
import com.godaddy.commerce.services.sample.orders.onSuccess
import com.godaddy.commerce.util.mapper.DateTimeMapper
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import java.util.*

class OrderCreateViewModel : CommonViewModel<State>(State()) {

    private val ordersServiceClient = getOrderService(viewModelScope)
    private val businessServiceClient = getBusinessService(viewModelScope)


    fun create() {
        execute {
            val service = ordersServiceClient.getService().getOrThrow()
            val business = businessServiceClient.getService().getOrThrow().getBusiness(
                bundleOf(BusinessParams.FROM_CLOUD to false)
            )

            val processedAt = DateTimeMapper().toString(Calendar.getInstance())
            val name = state.name.split(" ")
            val lineItemName = requireNotNull(state.lineItemName) { "lineItemName is not selected" }
            val selectedFulfillmentStatus =
                requireNotNull(state.selectedFulfillmentStatus) { "FulfillmentStatus is not selected" }
            val selectedFulfillmentMode =
                requireNotNull(state.selectedFulfillmentMode) { "FulfillmentMode is not selected" }
            val selectedStatus = requireNotNull(state.selectedStatus) { "Status is not selected" }
            val selectedPaymentStatus =
                requireNotNull(state.selectedPaymentStatus) { "PaymentStatus is not selected" }

            val amount =
                requireNotNull(state.amount?.toLongOrNull()) { "Amount is null or not correct" }

            val lineItemType = when (selectedFulfillmentMode) {
                "GIFT_CARD" -> "DIGITAL"
                else -> "PHYSICAL"
            }

            val order = Order(
                id = "Order_${Ksuid().generate()}",
                billing = Billing(firstName = name.firstOrNull(), lastName = name.getOrNull(1)),
                processedAt = processedAt,
                context = OrderContext(
                    channelId = DEFAULT_CHANNEL_ID,
                    storeId = business.stores.first().id.toString()
                ),
                number = UUID.randomUUID().toString(),
                numberDisplay = (1..100).random().toString(),
                lineItems = listOf(
                    LineItem(
                        type = lineItemType,
                        name = lineItemName,
                        fulfillmentMode = selectedFulfillmentMode,
                        status = "UNFULFILLED",
                        totals = LineItemTotals(
                            discountTotal = SimpleMoney("USD", 0),
                            feeTotal = SimpleMoney("USD", 0),
                            taxTotal = SimpleMoney("USD", 0),
                            subTotal = SimpleMoney("USD", amount),
                        ),
                        unitAmount = SimpleMoney("USD", amount),
                        fees = emptyList(),
                        taxes = emptyList(),
                        discounts = emptyList(),
                        quantity = 1f,
                        details = LineItemDetails(
                            sku = "head-first-java-75",
                            unitOfMeasure = "EACH"
                        ),
                    )
                ),
                statuses = OrderStatuses(
                    status = selectedStatus,
                    paymentStatus = selectedPaymentStatus,
                    fulfillmentStatus = selectedFulfillmentStatus
                ),
                taxExempted = false,
                totals = Total(
                    discountTotal = SimpleMoney("USD", 0),
                    feeTotal = SimpleMoney("USD", 0),
                    subTotal = SimpleMoney("USD", amount),
                    taxTotal = SimpleMoney("USD", 0),
                    shippingTotal = SimpleMoney("USD", 0),
                    total = SimpleMoney("USD", amount),
                )
            )

            val response = suspendCancellableCoroutine<Order?> {
                service.createOrder(order, Bundle.EMPTY, it.onSuccess(), it.onError())
            }
            Timber.d("Order was created: $response")
            update { copy(createdOrder = response) }
        }
    }

    fun onFulfillmentStatusSelected(position: Int) {
        update { copy(selectedFulfillmentStatus = fulfillmentStatuses.getOrNull(position)) }
    }

    fun onStatusSelected(position: Int) {
        update { copy(selectedStatus = statuses.getOrNull(position)) }
    }

    fun onNameChanged(name: String) {
        update { copy(name = name) }
    }

    fun onAmountChanged(amount: String) {
        update { copy(amount = amount) }
    }

    fun onLineItemNameChanged(name: String) {
        update { copy(lineItemName = name) }
    }

    fun onPaymentStatusSelected(position: Int) {
        update { copy(selectedPaymentStatus = paymentStatuses.getOrNull(position)) }
    }

    fun onFulfillmentModeSelected(position: Int) {
        update { copy(selectedFulfillmentMode = fulfillmentModes.getOrNull(position)) }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(title = "Order Create"),
        val fulfillmentStatuses: List<String> = OrderConstants.FulfillmentStatus.values.toList(),
        val fulfillmentModes: List<String> = OrderConstants.FulfillmentMode.values.toList(),
        val statuses: List<String> = OrderConstants.OrderStatus.values.toList(),
        val paymentStatuses: List<String> = OrderConstants.PaymentStatus.values.toList(),
        val lineItemName: String = "",
        val name: String = "",
        val selectedFulfillmentStatus: String? = null,
        val selectedFulfillmentMode: String? = null,
        val selectedStatus: String? = null,
        val selectedPaymentStatus: String? = null,
        val amount: String? = null,
        val createdOrder: Order? = null
    ) : ViewModelState

    companion object {
        private const val DEFAULT_CHANNEL_ID = "6e2ca858-1826-44c7-8334-47e45c1c7e7b"
    }
}
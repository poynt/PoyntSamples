package com.godaddy.commerce.services.sample.orders.update

import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.order.OrderConstants
import com.godaddy.commerce.order.OrderParams
import com.godaddy.commerce.orders.models.Order
import com.godaddy.commerce.orders.models.OrderPatch
import com.godaddy.commerce.orders.models.OrderPatchContext
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import com.godaddy.commerce.services.sample.orders.onSuccess
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

class OrderUpdateViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<OrderUpdateViewModel.State>(State()) {

    private val ordersServiceClient = CommerceDependencyProvider.getOrderService(viewModelScope)
    private val orderId get() = requireNotNull(savedStateHandle["id"]) { "Order id is not passed" }

    init {
        loadOrder()
    }

    fun loadOrder() {
        execute {
            // get service first
            val service = ordersServiceClient.getService().getOrThrow()
            // wrap getOrder method to coroutine suspend function
            val response = suspendCancellableCoroutine<Order?> {
                service.getOrder(
                    bundleOf(OrderParams.ORDER_ID to orderId),
                    it.onSuccess(),
                    it.onError()
                )
            }
            Timber.d("Get order by id: $orderId, Order: $response")
            // update state
            update {
                copy(
                    order = response,
                    toolbarState = toolbarState.copy(title = "Order Patch  [${response?.number}]")
                )
            }
        }
    }

    fun orderNumberChanged(newNumber: String) {
        update { copy(newNumber = newNumber) }
    }

    fun updateOrder() {
        execute {
            val orderPatch = OrderPatch(
                number = state.newNumber,
                context = state.order?.context?.let {
                    OrderPatchContext(
                        channelId = it.channelId,
                        storeId = it.storeId,
                        businessId = it.storeId
                    )
                }
            )

            val service = ordersServiceClient.getService().getOrThrow()

            val order = suspendCancellableCoroutine<Order?> {
                service.patchOrder(
                    orderPatch,
                    bundleOf(OrderParams.ORDER_ID to orderId),
                    it.onSuccess(),
                    it.onError()
                )
            }

            Timber.d("Order was patched")

            update { copy(order = order) }
        }
    }

    fun onStatusSelected(position: Int) {
        update { copy(selectedStatus = statuses.getOrNull(position)) }
    }

    data class State(
        val order: Order? = null,
        val newNumber: String? = null,
        val statuses: List<String> = OrderConstants.OrderStatus.values.toList(),
        val selectedStatus: String? = null,
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState("Order Update"),
    ) : ViewModelState
}
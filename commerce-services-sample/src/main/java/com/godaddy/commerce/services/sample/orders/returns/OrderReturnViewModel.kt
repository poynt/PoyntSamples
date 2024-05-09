package com.godaddy.commerce.services.sample.orders.returns

import androidx.core.os.bundleOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.order.OrderParams
import com.godaddy.commerce.order.OrderReturns
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import com.godaddy.commerce.services.sample.orders.onSuccess
import kotlinx.coroutines.suspendCancellableCoroutine

class OrderReturnViewModel(
    private val savedStateHandle: SavedStateHandle
) : CommonViewModel<OrderReturnViewModel.State>(State()) {

    private val ordersServiceClient = CommerceDependencyProvider.getOrderService(viewModelScope)
    private val orderId get() = requireNotNull(savedStateHandle["id"]) { "Order id is not passed" }

    init {
        refresh()
    }

    fun refresh() {
        execute {
            val service = ordersServiceClient.getService()
                .getOrThrow()

            val response = suspendCancellableCoroutine<OrderReturns?> {
                service.getReturnsByOrderId(
                    bundleOf(OrderParams.ORDER_ID to orderId),
                    it.onSuccess(), it.onError()
                )
            }
            update { copy(items = response.toRecyclerItems()) }
        }
    }


    data class State(
        val items: List<OrderReturnRecyclerItem> = emptyList(),
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState("Order Returns")
    ) : ViewModelState
}
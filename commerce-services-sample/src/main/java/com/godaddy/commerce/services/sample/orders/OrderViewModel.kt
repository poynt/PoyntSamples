@file:OptIn(FlowPreview::class)

package com.godaddy.commerce.services.sample.orders

import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.common.DataSource
import com.godaddy.commerce.common.SortOrder
import com.godaddy.commerce.order.OrderParams
import com.godaddy.commerce.order.OrderSortBy
import com.godaddy.commerce.order.ReturnConstants
import com.godaddy.commerce.orders.models.*
import com.godaddy.commerce.services.sample.common.extensions.onError
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState
import com.godaddy.commerce.services.sample.di.CommerceDependencyProvider
import com.godaddy.commerce.services.sample.orders.OrderViewModel.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber

class OrderViewModel : CommonViewModel<State>(State()) {

    private val ordersServiceClient = CommerceDependencyProvider.getOrderService(viewModelScope)
    private val searchQueryFlow = MutableSharedFlow<String>()

    init {
        loadOrders()
        subscribeOnSearch()
    }

    private fun subscribeOnSearch() {
        searchQueryFlow.debounce(300)
            .onEach { loadOrders(query = it) }
            .launchIn(viewModelScope)
    }

    @JvmOverloads
    fun loadOrders(query: String? = null) {
        execute {
            val service = ordersServiceClient.getService().getOrThrow()
            val bundle = Bundle().apply {
                // data source defines data provider: local db, remote or remote only if there are no date in local db.
                // It is better to use REMOTE_IF_EMPTY in most cases to improve UX and performance.
                putParcelable(OrderParams.DATA_SOURCE, DataSource.REMOTE_IF_EMPTY)
                // Always need to add pagination otherwise response can throw TooLargeTransaction exception.
                putInt(OrderParams.PAGE_SIZE, 100)
                putInt(OrderParams.OFFSET, 0)
                // Optional: add line items if it is required
                putBoolean(OrderParams.INCLUDE_LINE_ITEMS, true)
                // Optional: add sorting
                putParcelable(OrderParams.SORT_BY, OrderSortBy.UPDATED_AT)
                putSerializable(OrderParams.SORT_ORDER, SortOrder.DESC)
                putString(OrderParams.SEARCH_QUERY, query)
            }
            val response = suspendCancellableCoroutine<Orders?> {
                service.getOrders(bundle, it.onSuccess(), it.onError())
            }
            update { copy(items = response.mapToUiItems(::returnOrder)) }
        }
    }

    private fun returnOrder(order: Order) {
        execute {
            val lineItemReturns = requireNotNull(order.lineItems) {
                "Nothing to return. Line items are empty"
            }

            val orderReturn = Return(
                orderId = order.id,
                status = ReturnConstants.Status.COMPLETED,
                adjustments = order.retrieveAdjustmentsList(),
                items = lineItemReturns.map {
                    ReturnItem(
                        lineItemId = it.id.orEmpty(),
                        adjustments = it.retrieveAdjustmentsList(),
                        quantity = it.quantity,
                        otherReason = "Just testing...",
                        reason = ReturnConstants.Reason.OTHER,
                    )
                },
            )

            val service = ordersServiceClient.getService().getOrThrow()
            val response = suspendCancellableCoroutine<Return?> {
                service.postReturn(
                    orderReturn, Bundle.EMPTY, it.onSuccess(), it.onError()
                )
            }
            Timber.d("Return was created: $response")
            order.id?.let { refreshOrderById(it) }
        }
    }

    private suspend fun refreshOrderById(orderId: String) {
        val service = ordersServiceClient.getService().getOrThrow()
        val response = suspendCancellableCoroutine<Order?> {
            service.getOrder(
                bundleOf(OrderParams.ORDER_ID to orderId),
                it.onSuccess(),
                it.onError()
            )
        }
        Timber.d("Get order by id: $orderId, Order: $response")
        val order = response ?: return

        // update list
        update {
            copy(
                items = items.map {
                    if (it.item.id == orderId) order.mapToUiItems(::returnOrder) else it
                }
            )
        }
    }


    fun searchOrder(query: String) {
        viewModelScope.launch { searchQueryFlow.emit(query) }
    }

    data class State(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(
            title = "Orders",
            showSearchButton = true
        ),
        val items: List<OrderRecyclerItem> = emptyList()
    ) : ViewModelState
}
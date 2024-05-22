package com.godaddy.commerce.services.sample.orders

import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.godaddy.commerce.orders.models.Order
import com.godaddy.commerce.orders.models.Orders
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.binding.RecyclerAdapterItem
import com.godaddy.commerce.services.sample.databinding.OrderItemBinding

data class OrderRecyclerItem(
    override val item: Order,
    override val onBinding: (binding: OrderItemBinding, position: Int, getItem: () -> Order) -> Unit = { _, _, _ -> },
) : RecyclerAdapterItem<Order, OrderItemBinding>(item, onBinding) {

    val hasReturns get() = item.returnTotals != null
    val showReturnOrder get() = item.returnTotals == null
}


fun Orders?.mapToUiItems(returnOrder: (Order) -> Unit): List<OrderRecyclerItem> {
    return this?.orders.orEmpty().map { it.mapToUiItems(returnOrder) }
}

inline fun Order.mapToUiItems(crossinline returnOrder: (Order) -> Unit): OrderRecyclerItem {
    return OrderRecyclerItem(this) { binding, _, getItem ->
        binding.returnsBt.setOnClickListener { view ->
            view.findNavController().navigate(
                resId = R.id.orderReturnsFragment,
                args = bundleOf("id" to getItem().id)
            )
        }
        binding.updateBt.setOnClickListener { view ->
            view.findNavController().navigate(
                resId = R.id.orderUpdateFragment,
                args = bundleOf("id" to getItem().id)
            )
        }
        binding.returnBt.setOnClickListener { returnOrder(getItem()) }
    }
}
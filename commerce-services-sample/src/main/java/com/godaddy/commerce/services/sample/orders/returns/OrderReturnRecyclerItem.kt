package com.godaddy.commerce.services.sample.orders.returns

import com.godaddy.commerce.order.OrderReturns
import com.godaddy.commerce.orders.models.Return
import com.godaddy.commerce.services.sample.common.binding.RecyclerAdapterItem
import com.godaddy.commerce.services.sample.databinding.OrderReturnItemBinding

data class OrderReturnRecyclerItem(
    override val item: Return,
    override val onBinding: (binding: OrderReturnItemBinding, position: Int, getItem: () -> Return) -> Unit = { _, _, _ -> }
) : RecyclerAdapterItem<Return, OrderReturnItemBinding>(item, onBinding)


fun OrderReturns?.toRecyclerItems(): List<OrderReturnRecyclerItem>{
    return this?.returns.orEmpty().map { OrderReturnRecyclerItem(it) }
}
package com.godaddy.commerce.services.sample.orders

import com.godaddy.commerce.order.*
import com.godaddy.commerce.orders.models.*
import com.godaddy.commerce.services.sample.common.extensions.resumeIfActive
import com.godaddy.commerce.services.sample.common.extensions.toSimpleMoney
import kotlinx.coroutines.CancellableContinuation
import kotlin.math.roundToLong


/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Order?>.onSuccess(): IOrderServiceOrderCallback {
    return object : IOrderServiceOrderCallback.Stub() {
        override fun onSuccess(order: Order?) {
            resumeIfActive { order }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Orders?>.onSuccess(): IOrderServiceOrdersCallback {
    return object : IOrderServiceOrdersCallback.Stub() {
        override fun onSuccess(orders: Orders?) {
            resumeIfActive { orders }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Return?>.onSuccess(): IOrderServiceReturnCallback {
    return object : IOrderServiceReturnCallback.Stub() {
        override fun onSuccess(orderReturn: Return?) {
            resumeIfActive { orderReturn }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<OrderReturns?>.onSuccess(): IOrderServiceReturnsCallback {
    return object : IOrderServiceReturnsCallback.Stub() {
        override fun onSuccess(orderReturns: OrderReturns?) {
            resumeIfActive { orderReturns }
        }
    }
}

/**
 * Takes order tax, fee, discount values and maps that to ReturnAdjustment object.
 *
 * Adjustments processed:
 * 1. fee adjustment - Applied Order-level and Product-level fee.
 * 2. tax adjustment - Applied Order-level and Product-level tax.
 * 3. discount adjustment - Applied Order-level and Product-level discount.
 *
 * Note: Certain fields are generated on the backend side, thus their default value is null:
 * id, createdAt, updatedAt, transactionId.
 */
fun Order.retrieveAdjustmentsList(): List<ReturnAdjustment> {
    val feeAdjustments = fees?.map {
        ReturnAdjustment(
            amount = it.amount,
            type = ReturnConstants.AdjustmentType.FEE,
            referenceId = it.id,
        )
    }.orEmpty()

    val taxAdjustments = taxes?.map {
        ReturnAdjustment(
            amount = it.amount,
            type = ReturnConstants.AdjustmentType.TAX,
            referenceId = it.id,
        )
    }.orEmpty()

    val discountAdjustments = discounts?.map {
        ReturnAdjustment(
            amount = it.amount,
            type = ReturnConstants.AdjustmentType.DISCOUNT,
            referenceId = it.id,
        )
    }.orEmpty()

    return (feeAdjustments + taxAdjustments + discountAdjustments).filter {
        (it.amount?.value ?: 0) != 0L
    }
}

fun LineItem.retrieveAdjustmentsList(): List<ReturnAdjustment> {
    val feeAdjustments = fees?.map {
        ReturnAdjustment(
            amount = it.amount,
            type = ReturnConstants.AdjustmentType.FEE,
            referenceId = it.id,
        )
    }.orEmpty()

    val taxAdjustments = taxes?.map {
        ReturnAdjustment(
            amount = it.amount,
            type = ReturnConstants.AdjustmentType.TAX,
            referenceId = it.id,
        )
    }.orEmpty()

    val discountAdjustments = discounts?.map {
        ReturnAdjustment(
            amount = it.amount,
            type = ReturnConstants.AdjustmentType.DISCOUNT,
            referenceId = it.id,
        )
    }.orEmpty()

    val purchaseAdjustment = run {
        ReturnAdjustment(
            amount = (unitAmount?.value?.times(quantity?.roundToLong() ?: 0) ?: 0).toSimpleMoney(),
            type = ReturnConstants.AdjustmentType.PURCHASE,
        )
    }.let { listOf(it) }

    return (purchaseAdjustment + feeAdjustments + taxAdjustments + discountAdjustments).filter {
        (it.amount?.value ?: 0) != 0L
    }
}


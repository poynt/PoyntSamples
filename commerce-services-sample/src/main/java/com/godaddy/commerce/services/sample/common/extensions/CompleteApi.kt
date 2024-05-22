@file:JvmName("CompleteApi")

package com.godaddy.commerce.services.sample.common.extensions

import co.poynt.api.model.Business
import com.godaddy.commerce.common.ICompleteServiceCallback
import com.godaddy.commerce.sdk.business.IBusinessServiceCallback
import com.godaddy.commerce.sdk.business.IPaymentSettingsServiceCallback
import com.godaddy.commerce.sdk.business.model.PaymentSettings
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume


/**
 * Wraps [ICompleteServiceCallback] class to corouintes extension.
 */
fun CancellableContinuation<Unit>.onComplete(): ICompleteServiceCallback {
    return object : ICompleteServiceCallback.Stub() {
        override fun onComplete() {
            if (isActive) {
                resume(Unit)
            }
        }
    }
}


fun CancellableContinuation<Business>.onSuccess(): IBusinessServiceCallback {
    return object : IBusinessServiceCallback.Stub() {
        override fun onSuccess(result: Business?) {
            if (isActive) {
                resume(requireNotNull(result))
            }
        }
    }
}

fun CancellableContinuation<PaymentSettings>.onSuccess(): IPaymentSettingsServiceCallback {
    return object : IPaymentSettingsServiceCallback.Stub() {
        override fun onSuccess(result: PaymentSettings?) {
            if (isActive) {
                resume(requireNotNull(result))
            }
        }
    }
}

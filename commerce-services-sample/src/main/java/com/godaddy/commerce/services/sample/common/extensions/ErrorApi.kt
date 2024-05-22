@file:JvmName("ErrorApi")

package com.godaddy.commerce.services.sample.common.extensions

import android.os.Bundle
import com.godaddy.commerce.common.IErrorServiceCallback
import com.godaddy.commerce.sdk.util.toApiException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.isActive
import kotlin.coroutines.resumeWithException

/**
 * Wraps [IErrorServiceCallback] class to coroutines extension.
 */
fun CancellableContinuation<*>.onError(): IErrorServiceCallback {
    return object : IErrorServiceCallback.Stub() {
        override fun onError(error: Bundle?) {
            if (isActive) {
                resumeWithException(error.toApiException())
            }
        }
    }
}

/**
 * Wraps [IErrorServiceCallback] class to flow extension.
 */
fun <T> ProducerScope<Result<T?>>.onError(): IErrorServiceCallback {
    return object : IErrorServiceCallback.Stub() {
        override fun onError(error: Bundle?) {
            if (isActive) {
                trySendBlocking(Result.failure(error.toApiException()))
            }
        }
    }
}

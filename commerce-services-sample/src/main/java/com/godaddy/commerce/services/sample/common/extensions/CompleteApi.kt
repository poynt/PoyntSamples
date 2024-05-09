@file:JvmName("CompleteApi")

package com.godaddy.commerce.services.sample.common.extensions

import com.godaddy.commerce.common.ICompleteServiceCallback
import kotlinx.coroutines.CancellableContinuation
import kotlin.coroutines.resume


/**
 * Wraps [ICompleteServiceCallback] class to corouintes extension.
 */
public fun CancellableContinuation<Unit>.onComplete(): ICompleteServiceCallback {
    return object : ICompleteServiceCallback.Stub() {
        override fun onComplete() {
            if (isActive) {
                resume(Unit)
            }
        }
    }
}

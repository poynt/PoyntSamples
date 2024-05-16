package com.godaddy.commerce.services.sample.common.extensions

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.core.content.ContextCompat
import com.godaddy.commerce.sdk.util.SdkLogger.logError
import com.godaddy.commerce.sdk.util.SdkLogger.logInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.onSuccess
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow


internal fun Context.subscribeOnUpdates(action: String): Flow<Bundle> {
    return channelFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null && intent.action == action) {
                    val result = intent.extras ?: Bundle.EMPTY
                    trySendBlocking(result)
                        .onFailure { logError(throwable = it) { "Failed to send event for action: $action" } }
                        .onSuccess { logInfo { "Event was sent for action: $action" } }
                }
            }
        }
        ContextCompat.registerReceiver(
            this@subscribeOnUpdates,
            receiver,
            IntentFilter(action),
            ContextCompat.RECEIVER_EXPORTED
        )
        awaitClose { unregisterReceiver(receiver) }
    }
}


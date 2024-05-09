package com.godaddy.commerce.services.sample.common.extensions

import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.godaddy.commerce.common.models.SimpleMoney
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.*
import kotlin.coroutines.resume


internal inline fun <T> CancellableContinuation<T>.resumeIfActive(block: () -> T) {
    if (isActive) resume(block())
}

internal fun Fragment.launch(run: suspend CoroutineScope.() -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED, run)
    }
}

fun SimpleMoney?.format(): String {
    val value = this?.value ?: 0L
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2

    return formatter.format(value / 100.0)
}
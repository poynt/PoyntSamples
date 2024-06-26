package com.godaddy.commerce.services.sample.common.extensions

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.databinding.ObservableField
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.godaddy.commerce.common.models.SimpleMoney
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.NumberFormat
import java.util.*
import kotlin.coroutines.resume


internal inline fun <T> CancellableContinuation<T>.resumeIfActive(block: () -> T) {
    if (isActive) resume(block())
}

internal fun Fragment.launch(run: suspend CoroutineScope.() -> Unit) {
    viewLifecycleOwner.lifecycleScope.launch(CoroutineExceptionHandler { _, throwable ->
        Timber.e(throwable)
    }) {
        repeatOnLifecycle(Lifecycle.State.STARTED, run)
    }
}

fun Long?.toSimpleMoney(): SimpleMoney? = this?.let {
    // Use currency code from business store.
    SimpleMoney(currencyCode = "USD", value = it)
}

fun SimpleMoney?.format(): String {
    val value = this?.value ?: 0L
    val formatter = NumberFormat.getCurrencyInstance(Locale.US)
    formatter.maximumFractionDigits = 2
    formatter.minimumFractionDigits = 2

    return formatter.format(value / 100.0)
}

fun <T> Context.dialogBuilder(
    title: String,
    items: List<T>,
    map: (T) -> String?,
    onSelected: (T) -> Unit
): AlertDialog.Builder {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(title)
    builder.setItems(items.map(map).toTypedArray()) { dialog, which ->
        onSelected(items[which])
        dialog.dismiss()
    }
    return builder
}

suspend fun <T : CommonViewModel.ViewModelState, K> StateFlow<T>.bindTo(
    map: T.() -> K,
    keySelector: ((T) -> Any?)? = null,
    update: (K) -> Unit,
) {
    if (keySelector == null) {
        map(map).distinctUntilChanged()
    } else {
        distinctUntilChangedBy(keySelector).map(map)
    }.collectLatest { update(it) }
}

/**
 * Converts State property from StateFlow and sends data to ObservableField.
 */
context(Fragment) internal fun <T : CommonViewModel.ViewModelState, K> observableField(
    stateFlow: () -> StateFlow<T>,
    map: T.() -> K,
    keySelector: ((T) -> Any?)? = null,
): Lazy<ObservableField<K>> {
    return lazy(LazyThreadSafetyMode.NONE) {
        val observableField = ObservableField<K>()
        launch {
            stateFlow().bindTo(map = map, keySelector = keySelector, update = {
                observableField.set(it)
            })
        }
        observableField
    }
}

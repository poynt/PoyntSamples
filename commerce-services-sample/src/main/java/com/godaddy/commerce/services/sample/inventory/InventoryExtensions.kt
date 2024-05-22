package com.godaddy.commerce.services.sample.inventory

import com.godaddy.commerce.inventory.callback.*
import com.godaddy.commerce.inventory.models.*
import com.godaddy.commerce.services.sample.common.extensions.resumeIfActive
import kotlinx.coroutines.CancellableContinuation


/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<InventoryBundledList?>.onSuccess(): IInventoryBundleCallback {
    return object : IInventoryBundleCallback.Stub() {
        override fun onSuccess(list: InventoryBundledList?) {
            resumeIfActive { list }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Level?>.onSuccess(): IInventoryLevelCallback {
    return object : IInventoryLevelCallback.Stub() {
        override fun onSuccess(level: Level?) {
            resumeIfActive { level }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<InventoryLevels?>.onSuccess(): IInventoryLevelsCallback {
    return object : IInventoryLevelsCallback.Stub() {
        override fun onSuccess(levels: InventoryLevels?) {
            resumeIfActive { levels }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Location?>.onSuccess(): IInventoryLocationCallback {
    return object : IInventoryLocationCallback.Stub() {
        override fun onSuccess(location: Location?) {
            resumeIfActive { location }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<InventoryLocations?>.onSuccess(): IInventoryLocationsCallback {
    return object : IInventoryLocationsCallback.Stub() {
        override fun onSuccess(locations: InventoryLocations?) {
            resumeIfActive { locations }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Reservation?>.onSuccess(): IInventoryReservationCallback {
    return object : IInventoryReservationCallback.Stub() {
        override fun onSuccess(reservation: Reservation?) {
            resumeIfActive { reservation }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<InventoryReservations?>.onSuccess(): IInventoryReservationsCallback {
    return object : IInventoryReservationsCallback.Stub() {
        override fun onSuccess(reservations: InventoryReservations?) {
            resumeIfActive { reservations }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Summary?>.onSuccess(): IInventorySummaryCallback {
    return object : IInventorySummaryCallback.Stub() {
        override fun onSuccess(summary: Summary?) {
            resumeIfActive { summary }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<InventorySummaries?>.onSuccess(): IInventorySummariesCallback {
    return object : IInventorySummariesCallback.Stub() {
        override fun onSuccess(summaries: InventorySummaries?) {
            resumeIfActive { summaries }
        }
    }
}

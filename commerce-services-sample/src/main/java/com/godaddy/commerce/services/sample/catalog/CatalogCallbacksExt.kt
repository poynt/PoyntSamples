package com.godaddy.commerce.services.sample.catalog

import com.godaddy.commerce.catalog.callback.ICategoriesServiceCallback
import com.godaddy.commerce.catalog.callback.ICategoryServiceCallback
import com.godaddy.commerce.catalog.callback.IProductServiceCallback
import com.godaddy.commerce.catalog.callback.IProductsServiceCallback
import com.godaddy.commerce.catalog.models.Categories
import com.godaddy.commerce.catalog.models.Category
import com.godaddy.commerce.catalog.models.Product
import com.godaddy.commerce.catalog.models.Products
import com.godaddy.commerce.priceadjustments.callback.IPriceAdjustmentAssociationServiceCallback
import com.godaddy.commerce.priceadjustments.callback.IPriceAdjustmentAssociationsServiceCallback
import com.godaddy.commerce.priceadjustments.callback.IPriceAdjustmentServiceCallback
import com.godaddy.commerce.priceadjustments.callback.IPriceAdjustmentsServiceCallback
import com.godaddy.commerce.priceadjustments.models.PriceAdjustment
import com.godaddy.commerce.priceadjustments.models.PriceAdjustmentAssociation
import com.godaddy.commerce.priceadjustments.models.PriceAdjustmentAssociations
import com.godaddy.commerce.priceadjustments.models.PriceAdjustments
import com.godaddy.commerce.services.sample.common.extensions.resumeIfActive
import com.godaddy.commerce.taxes.callback.*
import com.godaddy.commerce.taxes.models.*
import kotlinx.coroutines.CancellableContinuation


/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Categories?>.onSuccess(): ICategoriesServiceCallback {
    return object : ICategoriesServiceCallback.Stub() {
        override fun onSuccess(categories: Categories?) {
            resumeIfActive { categories }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Category?>.onSuccess(): ICategoryServiceCallback {
    return object : ICategoryServiceCallback.Stub() {
        override fun onSuccess(category: Category?) {
            resumeIfActive { category }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Product?>.onSuccess(): IProductServiceCallback {
    return object : IProductServiceCallback.Stub() {
        override fun onSuccess(product: Product?) {
            resumeIfActive { product }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Products?>.onSuccess(): IProductsServiceCallback {
    return object : IProductsServiceCallback.Stub() {
        override fun onSuccess(products: Products?) {
            resumeIfActive { products }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<TaxAssociation?>.onSuccess(): ITaxAssociationServiceCallback {
    return object : ITaxAssociationServiceCallback.Stub() {
        override fun onSuccess(association: TaxAssociation?) {
            resumeIfActive { association }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<TaxAssociations?>.onSuccess(): ITaxAssociationsServiceCallback {
    return object : ITaxAssociationsServiceCallback.Stub() {
        override fun onSuccess(associations: TaxAssociations?) {
            resumeIfActive { associations }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Taxes?>.onSuccess(): ITaxesServiceCallback {
    return object : ITaxesServiceCallback.Stub() {
        override fun onSuccess(taxes: Taxes?) {
            resumeIfActive { taxes }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<TaxOverrideAssociation?>.onSuccess(): ITaxOverrideAssociationServiceCallback {
    return object : ITaxOverrideAssociationServiceCallback.Stub() {
        override fun onSuccess(association: TaxOverrideAssociation?) {
            resumeIfActive { association }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<TaxOverrideAssociations?>.onSuccess(): ITaxOverrideAssociationsServiceCallback {
    return object : ITaxOverrideAssociationsServiceCallback.Stub() {
        override fun onSuccess(associations: TaxOverrideAssociations?) {
            resumeIfActive { associations }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<Tax?>.onSuccess(): ITaxServiceCallback {
    return object : ITaxServiceCallback.Stub() {
        override fun onSuccess(tax: Tax?) {
            resumeIfActive { tax }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<PriceAdjustmentAssociation?>.onSuccess(): IPriceAdjustmentAssociationServiceCallback {
    return object : IPriceAdjustmentAssociationServiceCallback.Stub() {
        override fun onSuccess(association: PriceAdjustmentAssociation?) {
            resumeIfActive { association }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<PriceAdjustmentAssociations?>.onSuccess(): IPriceAdjustmentAssociationsServiceCallback {
    return object : IPriceAdjustmentAssociationsServiceCallback.Stub() {
        override fun onSuccess(associations: PriceAdjustmentAssociations?) {
            resumeIfActive { associations }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<PriceAdjustment?>.onSuccess(): IPriceAdjustmentServiceCallback {
    return object : IPriceAdjustmentServiceCallback.Stub() {
        override fun onSuccess(adjustment: PriceAdjustment?) {
            resumeIfActive { adjustment }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<PriceAdjustments?>.onSuccess(): IPriceAdjustmentsServiceCallback {
    return object : IPriceAdjustmentsServiceCallback.Stub() {
        override fun onSuccess(adjustments: PriceAdjustments?) {
            resumeIfActive { adjustments }
        }
    }
}

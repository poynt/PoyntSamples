package com.godaddy.commerce.services.sample.catalog

import com.godaddy.commerce.catalog.callback.ICatalogCategoriesServiceCallback
import com.godaddy.commerce.catalog.callback.ICatalogCategoryServiceCallback
import com.godaddy.commerce.catalog.callback.ICatalogCategoryTreeNodeServiceCallback
import com.godaddy.commerce.catalog.callback.ICatalogCategoryTreeNodesServiceCallback
import com.godaddy.commerce.catalog.callback.ICatalogProductServiceCallback
import com.godaddy.commerce.catalog.callback.ICatalogProductsServiceCallback
import com.godaddy.commerce.catalog.callback.ICatalogTaxCallback
import com.godaddy.commerce.catalog.callback.ICatalogTaxesCallback
import com.godaddy.commerce.catalog.model.CatalogCategories
import com.godaddy.commerce.catalog.model.CatalogCategory
import com.godaddy.commerce.catalog.model.CatalogCategoryTreeNode
import com.godaddy.commerce.catalog.model.CatalogCategoryTreeNodes
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.catalog.model.CatalogProducts
import com.godaddy.commerce.catalog.model.CatalogTax
import com.godaddy.commerce.catalog.model.CatalogTaxes
import com.godaddy.commerce.priceadjustments.callback.IPriceAdjustmentAssociationServiceCallback
import com.godaddy.commerce.priceadjustments.callback.IPriceAdjustmentAssociationsServiceCallback
import com.godaddy.commerce.priceadjustments.callback.IPriceAdjustmentServiceCallback
import com.godaddy.commerce.priceadjustments.callback.IPriceAdjustmentsServiceCallback
import com.godaddy.commerce.priceadjustments.models.PriceAdjustment
import com.godaddy.commerce.priceadjustments.models.PriceAdjustmentAssociation
import com.godaddy.commerce.priceadjustments.models.PriceAdjustmentAssociations
import com.godaddy.commerce.priceadjustments.models.PriceAdjustments
import com.godaddy.commerce.services.sample.common.extensions.resumeIfActive
import kotlinx.coroutines.CancellableContinuation


/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<CatalogCategories>.onSuccess(): ICatalogCategoriesServiceCallback {
    return object : ICatalogCategoriesServiceCallback.Stub() {
        override fun onSuccess(categories: CatalogCategories) {
            resumeIfActive { categories }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<CatalogCategory?>.onSuccess(): ICatalogCategoryServiceCallback {
    return object : ICatalogCategoryServiceCallback.Stub() {
        override fun onSuccess(category: CatalogCategory?) {
            resumeIfActive { category }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<CatalogCategoryTreeNode?>.onSuccess(): ICatalogCategoryTreeNodeServiceCallback {
    return object : ICatalogCategoryTreeNodeServiceCallback.Stub() {
        override fun onSuccess(category: CatalogCategoryTreeNode?) {
            resumeIfActive { category }

        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<CatalogCategoryTreeNodes?>.onSuccess(): ICatalogCategoryTreeNodesServiceCallback {
    return object : ICatalogCategoryTreeNodesServiceCallback.Stub() {
        override fun onSuccess(category: CatalogCategoryTreeNodes?) {
            resumeIfActive { category }
        }
    }
}


/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<CatalogProduct?>.onSuccess(): ICatalogProductServiceCallback {
    return object : ICatalogProductServiceCallback.Stub() {
        override fun onSuccess(product: CatalogProduct?) {
            resumeIfActive { product }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<CatalogProducts?>.onSuccess(): ICatalogProductsServiceCallback {
    return object : ICatalogProductsServiceCallback.Stub() {
        override fun onSuccess(products: CatalogProducts?) {
            resumeIfActive { products }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
//fun CancellableContinuation<TaxAssociation?>.onSuccess(): ITaxAssociationServiceCallback {
//    return object : ITaxAssociationServiceCallback.Stub() {
//        override fun onSuccess(association: TaxAssociation?) {
//            resumeIfActive { association }
//        }
//    }
//}

/**
 * Wraps AIDL callback to coroutine continuation
 */
//fun CancellableContinuation<TaxAssociations?>.onSuccess(): ITaxAssociationsServiceCallback {
//    return object : ITaxAssociationsServiceCallback.Stub() {
//        override fun onSuccess(associations: TaxAssociations?) {
//            resumeIfActive { associations }
//        }
//    }
//}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<CatalogTaxes?>.onSuccess():ICatalogTaxesCallback {
    return object : ICatalogTaxesCallback.Stub() {
        override fun onSuccess(taxes: CatalogTaxes?) {
            resumeIfActive { taxes }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
//fun CancellableContinuation<TaxOverrideAssociation?>.onSuccess(): ITaxOverrideAssociationServiceCallback {
//    return object : ITaxOverrideAssociationServiceCallback.Stub() {
//        override fun onSuccess(association: TaxOverrideAssociation?) {
//            resumeIfActive { association }
//        }
//    }
//}

/**
 * Wraps AIDL callback to coroutine continuation
 */
//fun CancellableContinuation<TaxOverrideAssociations?>.onSuccess(): ITaxOverrideAssociationsServiceCallback {
//    return object : ITaxOverrideAssociationsServiceCallback.Stub() {
//        override fun onSuccess(associations: TaxOverrideAssociations?) {
//            resumeIfActive { associations }
//        }
//    }
//}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<CatalogTax?>.onSuccess(): ICatalogTaxCallback {
    return object : ICatalogTaxCallback.Stub() {
        override fun onSuccess(tax: CatalogTax?) {
            resumeIfActive { tax }
        }
    }
}

/**
 * Wraps AIDL callback to coroutine continuation
 */
fun CancellableContinuation<PriceAdjustmentAssociation?>.onSucces(): IPriceAdjustmentAssociationServiceCallback {
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

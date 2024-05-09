package com.godaddy.commerce.services.sample.di

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.IInterface
import com.godaddy.commerce.catalog.CatalogIntents
import com.godaddy.commerce.catalog.ICatalogService
import com.godaddy.commerce.inventory.IInventoryService
import com.godaddy.commerce.inventory.InventoryIntents
import com.godaddy.commerce.order.IOrderService
import com.godaddy.commerce.order.OrderIntents
import com.godaddy.commerce.sdk.business.BusinessParams
import com.godaddy.commerce.sdk.business.IBusinessService
import com.godaddy.commerce.sdk.business.SERVICE_BUSINESS
import com.godaddy.commerce.util.client.ServiceClient
import com.godaddy.commerce.util.client.ServiceClientFactory
import com.godaddy.commerce.util.client.ServiceParams
import com.godaddy.commerce.util.coroutines.CoroutineDispatcherProvider
import com.godaddy.commerce.util.coroutines.CoroutineDispatcherProviderImpl
import kotlinx.coroutines.CoroutineScope

object CommerceDependencyProvider {

    private lateinit var app: Application

    val coroutineDispatchers: CoroutineDispatcherProvider by lazy {
        CoroutineDispatcherProviderImpl()
    }

    val serviceClientFactory: ServiceClientFactory by lazy {
        ServiceClientFactory(app)
    }

    fun getContext(): Context = app.applicationContext

    inline fun <reified T : IInterface> getServiceClient(
        scope: CoroutineScope,
        intent: Intent,
        noinline binderMapper: (IBinder) -> T
    ) = serviceClientFactory.create(
        ServiceParams(scope, intent, binderMapper)
    )

    fun getBusinessService(scope: CoroutineScope): ServiceClient<IBusinessService> = getServiceClient(
        scope,
        SERVICE_BUSINESS.create(app)
    ) { IBusinessService.Stub.asInterface(it) }

    fun getOrderService(scope: CoroutineScope): ServiceClient<IOrderService> = getServiceClient(
        scope,
        OrderIntents.SERVICE_ORDER.create(app)
    ) { IOrderService.Stub.asInterface(it) }


    fun getCatalogService(scope: CoroutineScope): ServiceClient<ICatalogService> = getServiceClient(
        scope,
        CatalogIntents.SERVICE_CATALOG.create(app)
    ) { ICatalogService.Stub.asInterface(it) }

    fun getInventoryService(scope: CoroutineScope): ServiceClient<IInventoryService> =
        getServiceClient(
            scope,
            InventoryIntents.SERVICE_INVENTORY.create(app)
        ) { IInventoryService.Stub.asInterface(it) }


    fun init(context: Application) {
        this.app = context
    }

}
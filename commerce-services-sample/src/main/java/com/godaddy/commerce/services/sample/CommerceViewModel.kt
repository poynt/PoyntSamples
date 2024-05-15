package com.godaddy.commerce.services.sample

import com.godaddy.commerce.services.sample.CommerceViewModel.CommerceState
import com.godaddy.commerce.services.sample.common.component.NavigationButtonComponent
import com.godaddy.commerce.services.sample.common.viewmodel.CommonState
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState

class CommerceViewModel : CommonViewModel<CommerceState>(CommerceState()) {

    init {
        update {
            copy(
                items = listOf(
                    NavigationButtonComponent(title = "ORDERS", navigateToId = R.id.ordersFragment),
                    NavigationButtonComponent(
                        title = "INVENTORY",
                        navigateToId = R.id.inventoryFragment
                    ),
                    NavigationButtonComponent(
                        title = "CATALOG - Product",
                        navigateToId = R.id.productFragment
                    ),
                )
            )
        }
    }

    data class CommerceState(
        override val commonState: CommonState = CommonState(),
        override val toolbarState: ToolbarState = ToolbarState(
            title = "Commerce Sample",
            showBackIcon = false
        ),
        val items: List<NavigationButtonComponent> = emptyList()
    ) : ViewModelState

}
package com.godaddy.commerce.services.sample.orders.create

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.OrderCreateFragmentBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.mapNotNull

class OrderCreateFragment :
    CommonFragment<OrderCreateFragmentBinding>(R.layout.order_create_fragment) {

    private val viewModel: OrderCreateViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch {
            viewModel.stateFlow.distinctUntilChangedBy { it.fulfillmentModes }.collectLatest {
                dataBinding.fulfillmentModes = it.fulfillmentModes
            }
        }
        launch {
            viewModel.stateFlow.distinctUntilChangedBy { it.statuses }.collectLatest {
                dataBinding.statuses = it.statuses
            }
        }
        launch {
            viewModel.stateFlow.distinctUntilChangedBy { it.paymentStatuses }.collectLatest {
                dataBinding.paymentStatuses = it.paymentStatuses
            }
        }
        launch {
            viewModel.stateFlow.distinctUntilChangedBy { it.fulfillmentStatuses }.collectLatest {
                dataBinding.fulfillmentStatuses = it.fulfillmentStatuses
            }
        }
        launch {
            viewModel.stateFlow.mapNotNull { it.createdOrder?.id }.distinctUntilChanged()
                .collectLatest { orderId ->
                    Toast.makeText(
                        requireContext(),
                        "Order was created: $orderId",
                        Toast.LENGTH_SHORT
                    )
                        .show()
                }
        }
    }
}
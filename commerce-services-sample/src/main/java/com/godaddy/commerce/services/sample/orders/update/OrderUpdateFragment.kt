package com.godaddy.commerce.services.sample.orders.update

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.OrderUpdateFragmentBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy

class OrderUpdateFragment :
    CommonFragment<OrderUpdateFragmentBinding>(R.layout.order_update_fragment) {

    private val viewModel: OrderUpdateViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch {
            viewModel.stateFlow.distinctUntilChangedBy { it.statuses }.collectLatest {
                dataBinding.statuses = it.statuses
            }
        }
        launch {
            viewModel.stateFlow.distinctUntilChangedBy { it.order }.collectLatest {
                dataBinding.order = it.order
                Toast.makeText(requireContext(), "Order was updated", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
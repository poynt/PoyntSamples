package com.godaddy.commerce.services.sample.orders.returns

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.OrderReturnFragmentBinding
import kotlinx.coroutines.flow.collectLatest

class OrderReturnFragment :
    CommonFragment<OrderReturnFragmentBinding>(R.layout.order_return_fragment) {

    private val viewModel: OrderReturnViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch { viewModel.stateFlow.collectLatest { dataBinding.items = it.items } }
    }
}
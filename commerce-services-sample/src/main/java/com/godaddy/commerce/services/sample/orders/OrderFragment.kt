package com.godaddy.commerce.services.sample.orders

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.common.view.doOnToolbarSearchQueryChanged
import com.godaddy.commerce.services.sample.databinding.OrderFragmentBinding
import kotlinx.coroutines.flow.collectLatest

class OrderFragment : CommonFragment<OrderFragmentBinding>(R.layout.order_fragment) {

    private val viewModel: OrderViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        doOnToolbarSearchQueryChanged { viewModel.searchOrder(it) }
        launch { viewModel.stateFlow.collectLatest { dataBinding.items = it.items } }
    }
}
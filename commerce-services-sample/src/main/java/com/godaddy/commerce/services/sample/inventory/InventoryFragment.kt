package com.godaddy.commerce.services.sample.inventory

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.InventoryFragmentBinding
import timber.log.Timber

class InventoryFragment : CommonFragment<InventoryFragmentBinding>(R.layout.inventory_fragment) {


    private val viewModel: InventoryViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch { viewModel.stateFlow.bindTo({
            Timber.d("Items : $items")
            items
        }) { dataBinding.items = it } }
    }
}
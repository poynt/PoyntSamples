package com.godaddy.commerce.services.sample.inventory.update

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.InventoryUpdateFragmentBinding
import com.godaddy.commerce.services.sample.inventory.update.InventoryUpdateViewModel.State

class InventoryUpdateFragment :
    CommonFragment<InventoryUpdateFragmentBinding>(R.layout.inventory_update_fragment) {


    private val viewModel: InventoryUpdateViewModel by viewModels()

    val item by observableField({ viewModel.stateFlow }, State::item)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch {
            viewModel.stateFlow.bindTo(State::updatedLevelId) {
                it ?: return@bindTo
                Toast.makeText(
                    requireContext(),
                    "Inventory with level id [$it] was updated",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }
}
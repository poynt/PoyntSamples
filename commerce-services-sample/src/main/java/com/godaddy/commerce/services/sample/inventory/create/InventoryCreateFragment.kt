package com.godaddy.commerce.services.sample.inventory.create

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godaddy.commerce.catalog.models.Product
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.dialogBuilder
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.InventoryCreateFragmentBinding
import com.godaddy.commerce.services.sample.inventory.create.InventoryCreateViewModel.State

class InventoryCreateFragment :
    CommonFragment<InventoryCreateFragmentBinding>(R.layout.inventory_create_fragment) {


    private val viewModel: InventoryCreateViewModel by viewModels()


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch {
            viewModel.stateFlow.bindTo(
                map = { products to showProductDialog },
                update = ::showProductsDialog
            )
        }
        launch {
            viewModel.stateFlow.bindTo(State::createdLevelId) { levelId ->
                levelId ?: return@bindTo
                Toast.makeText(
                    requireContext(),
                    "Inventory with level id [$levelId] was created",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun showProductsDialog(pair: Pair<List<Product>, Boolean>) {
        if (pair.second.not()) return
        requireContext().dialogBuilder(
            "Select product",
            items = pair.first,
            map = { it.name },
            onSelected = viewModel::create
        ).setOnDismissListener { viewModel.hideProductsDialog() }.create().show()
    }
}
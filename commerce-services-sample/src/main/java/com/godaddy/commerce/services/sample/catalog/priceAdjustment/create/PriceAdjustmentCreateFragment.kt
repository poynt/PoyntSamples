package com.godaddy.commerce.services.sample.catalog.priceAdjustment.create

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.godaddy.commerce.catalog.models.Product
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.dialogBuilder
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.PriceAdjustmentCreateFragmentBinding

class PriceAdjustmentCreateFragment :
    CommonFragment<PriceAdjustmentCreateFragmentBinding>(R.layout.price_adjustment_create_fragment) {

    private val viewModel: PriceAdjustmentCreateViewModel by viewModels()

    val amountTypes by observableField(
        stateFlow = { viewModel.stateFlow },
        map = PriceAdjustmentCreateViewModel.State::amountTypes
    )

    val types by observableField(
        stateFlow = { viewModel.stateFlow },
        map = PriceAdjustmentCreateViewModel.State::types
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch {
            viewModel.stateFlow.bindTo(
                map = { products to showProductDialog },
                update = ::showProductsDialog
            )
        }
    }


    private fun showProductsDialog(pair: Pair<List<Product>, Boolean>) {
        if (pair.second.not()) return
        requireContext().dialogBuilder(
            "Select Product",
            items = pair.first,
            map = { it.name },
            onSelected = viewModel::selectProduct
        ).setOnDismissListener { viewModel.hideProductsDialog() }.create().show()
    }
}
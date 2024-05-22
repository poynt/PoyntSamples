package com.godaddy.commerce.services.sample.catalog.priceAdjustment.update

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.godaddy.commerce.catalog.models.Product
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.catalog.priceAdjustment.mapToUiItems
import com.godaddy.commerce.services.sample.catalog.priceAdjustment.update.PriceAdjustmentUpdateViewModel.State
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.dialogBuilder
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.PriceAdjustmentUpdateFragmentBinding

class PriceAdjustmentUpdateFragment :
    CommonFragment<PriceAdjustmentUpdateFragmentBinding>(R.layout.price_adjustment_update_fragment) {

    private val viewModel: PriceAdjustmentUpdateViewModel by viewModels()

    val name by observableField(
        stateFlow = { viewModel.stateFlow },
        map = { priceAdjustment?.name }
    )

    val amountTypes by observableField(
        stateFlow = { viewModel.stateFlow },
        map = State::amountTypes
    )

    val types by observableField(
        stateFlow = { viewModel.stateFlow },
        map = State::types
    )

    val amount by observableField(
        stateFlow = { viewModel.stateFlow },
        map = { priceAdjustment?.amount?.value?.toString() }
    )

    val ratePercentage by observableField(
        stateFlow = { viewModel.stateFlow },
        map = { priceAdjustment?.ratePercentage }
    )

    val selectedAmountTypePos by observableField(
        { viewModel.stateFlow },
        { priceAdjustment?.amountType?.let { amountTypes.indexOf(it) } })

    val selectedTypePos by observableField(
        { viewModel.stateFlow },
        { priceAdjustment?.type?.let { types.indexOf(it) } })

    val associationItems by observableField(
        stateFlow = { viewModel.stateFlow },
        map = {
            association?.items.orEmpty().map {
                it.mapToUiItems { viewModel.removeItemFromAssociation(it) }
            }
        },
        keySelector = { it.association?.items })


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
            "Add Product",
            items = pair.first,
            map = { it.name },
            onSelected = viewModel::addProduct
        ).setOnDismissListener { viewModel.hideProductsDialog() }.create().show()
    }
}
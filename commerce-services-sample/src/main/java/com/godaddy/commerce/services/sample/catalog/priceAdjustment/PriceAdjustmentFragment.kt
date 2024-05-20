package com.godaddy.commerce.services.sample.catalog.priceAdjustment

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.PriceAdjustmentFragmentBinding

class PriceAdjustmentFragment :
    CommonFragment<PriceAdjustmentFragmentBinding>(R.layout.price_adjustment_fragment) {


    private val viewModel: PriceAdjustmentViewModel by viewModels()

    val items by observableField(
        stateFlow = { viewModel.stateFlow },
        map = { items.map { it.mapToUiItems() } },
        keySelector = { it.items }
    )


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
    }
}
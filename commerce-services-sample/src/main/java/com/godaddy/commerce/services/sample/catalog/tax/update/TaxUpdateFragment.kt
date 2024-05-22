package com.godaddy.commerce.services.sample.catalog.tax.update

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.catalog.tax.update.TaxUpdateViewModel.State
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.TaxUpdateFragmentBinding

class TaxUpdateFragment :
    CommonFragment<TaxUpdateFragmentBinding>(R.layout.tax_update_fragment) {


    private val viewModel: TaxUpdateViewModel by viewModels()

    val name by observableField({ viewModel.stateFlow }, { tax?.name })
    val rateName by observableField({ viewModel.stateFlow }, { tax?.taxRates?.firstOrNull()?.name })
    val amount by observableField(
        { viewModel.stateFlow },
        { tax?.taxRates?.firstOrNull()?.amount?.value?.toString() })
    val rate by observableField(
        { viewModel.stateFlow },
        { tax?.taxRates?.firstOrNull()?.ratePercentage })
    val selectedTypePos by observableField(
        { viewModel.stateFlow },
        { tax?.taxRates?.firstOrNull()?.amountType?.let { amountTypes.indexOf(it) } })

    val types by observableField(
        stateFlow = { viewModel.stateFlow },
        map = State::amountTypes
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
    }
}
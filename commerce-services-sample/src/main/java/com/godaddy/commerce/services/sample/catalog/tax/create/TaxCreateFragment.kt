package com.godaddy.commerce.services.sample.catalog.tax.create

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godaddy.commerce.catalog.models.Product
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.dialogBuilder
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.TaxCreateFragmentBinding

class TaxCreateFragment :
    CommonFragment<TaxCreateFragmentBinding>(R.layout.tax_create_fragment) {

    private val viewModel: TaxCreateViewModel by viewModels()

    val showTaxOverride by observableField(
        stateFlow = { viewModel.stateFlow },
        map = TaxCreateViewModel.State::createTaxOverride
    )

    val types by observableField(
        stateFlow = { viewModel.stateFlow },
        map = TaxCreateViewModel.State::amountTypes
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
        launch {
            viewModel.stateFlow.bindTo(TaxCreateViewModel.State::createdId) { id ->
                id ?: return@bindTo
                Toast.makeText(
                    requireContext(),
                    "Tax with id [$id] was created",
                    Toast.LENGTH_SHORT
                ).show()
            }
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
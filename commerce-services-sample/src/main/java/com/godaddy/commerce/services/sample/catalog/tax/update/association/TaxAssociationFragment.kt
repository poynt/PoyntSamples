package com.godaddy.commerce.services.sample.catalog.tax.update.association

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.godaddy.commerce.catalog.models.Product
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.catalog.tax.mapToUiItems
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.dialogBuilder
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.TaxAssociationFragmentBinding

class TaxAssociationFragment :
    CommonFragment<TaxAssociationFragmentBinding>(R.layout.tax_association_fragment) {


    private val viewModel: TaxAssociationViewModel by viewModels()

    val associationItems by observableField(
        stateFlow = { viewModel.stateFlow },
        map = {
            association?.items.orEmpty()
                .map { it.mapToUiItems { viewModel.removeProductFromTax(it) } }
        },
        keySelector = { it.association?.items })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch {
            viewModel.stateFlow.bindTo(
                map = { products to showProductDialog },
                update = ::showTaxAssociationProductDialog
            )
        }
    }

    private fun showTaxAssociationProductDialog(pair: Pair<List<Product>, Boolean>) {
        if (pair.second.not()) return
        requireContext().dialogBuilder(
            "Add Product to Tax",
            items = pair.first,
            map = { it.name },
            onSelected = viewModel::addProductToTax
        ).setOnDismissListener { viewModel.hideProductsDialog() }.create().show()
    }
}
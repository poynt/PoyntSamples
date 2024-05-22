package com.godaddy.commerce.services.sample.catalog.tax.update.override

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.godaddy.commerce.catalog.models.Product
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.catalog.tax.mapToUiItems
import com.godaddy.commerce.services.sample.catalog.tax.update.override.TaxOverrideAssociationViewModel.State
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.dialogBuilder
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.TaxOverrideAssociationFragmentBinding

class TaxOverrideAssociationFragment :
    CommonFragment<TaxOverrideAssociationFragmentBinding>(R.layout.tax_override_association_fragment) {


    private val viewModel: TaxOverrideAssociationViewModel by viewModels()

    val overrideRateName by observableField({ viewModel.stateFlow }, { overrideAssociation?.name })
    val overrideAmount by observableField(
        { viewModel.stateFlow },
        { overrideAssociation?.overrideValue?.amount?.value?.toString() })
    val overrideRate by observableField(
        { viewModel.stateFlow },
        { overrideAssociation?.overrideValue?.ratePercentage })
    val overrideType by observableField(
        { viewModel.stateFlow },
        { overrideAssociation?.overrideValue?.amountType?.let { amountTypes.indexOf(it) } })

    val overrideItems by observableField(
        stateFlow = { viewModel.stateFlow },
        map = {
            overrideAssociation?.items.orEmpty().map {
                it.mapToUiItems { viewModel.removeOverrideForProduct(it) }
            }
        },
        keySelector = { it.overrideAssociation?.items })

    val types by observableField(
        stateFlow = { viewModel.stateFlow },
        map = State::amountTypes
    )


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
            "Add override tax for product",
            items = pair.first,
            map = { it.name },
            onSelected = viewModel::addOverrideForProduct
        ).setOnDismissListener { viewModel.hideProductsDialog() }.create().show()
    }
}
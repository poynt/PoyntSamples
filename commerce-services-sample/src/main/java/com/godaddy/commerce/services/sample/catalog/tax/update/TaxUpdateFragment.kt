package com.godaddy.commerce.services.sample.catalog.tax.update

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.sdk.util.isNotNullOrBlank
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.catalog.tax.update.TaxUpdateViewModel.DialogType
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.dialogBuilder
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.TaxUpdateFragmentBinding

class TaxUpdateFragment :
    CommonFragment<TaxUpdateFragmentBinding>(R.layout.tax_update_fragment) {
    private val viewModel: TaxUpdateViewModel by viewModels()

    val label by observableField(
        stateFlow = { viewModel.stateFlow },
        map = TaxUpdateViewModel.State::label
    )
    val amount by observableField(
        stateFlow = {viewModel.stateFlow},
        map = { this.amount?.amount?.value?.toString() ?: "N/A"}
    )
    val percentage by observableField(
        stateFlow = {viewModel.stateFlow},
        map = {this.percentage?.percentage ?: "N/A"}
    )
    val showTaxOverride by observableField(
        stateFlow = { viewModel.stateFlow },
        map = TaxUpdateViewModel.State::updateTaxOverride
    )
    val showTaxClassification by observableField(
        stateFlow = { viewModel.stateFlow },
        map = TaxUpdateViewModel.State::updateTaxClassification
    )
    val classificationLabel by observableField(
        stateFlow = {viewModel.stateFlow},
        map = TaxUpdateViewModel.State::classificationLabel
    )
    val overrideLabel by observableField(
        stateFlow = {viewModel.stateFlow},
        map = TaxUpdateViewModel.State::overrideLabel
    )
    val customRate by observableField(
        stateFlow = {viewModel.stateFlow},
        map = {this.customRate?.ratePercentage?.percentage ?: "N/A"}
    )
    val taxTypes by observableField({ viewModel.stateFlow }, TaxUpdateViewModel.State::types)
    val taxTypePos by observableField({ viewModel.stateFlow }, { taxType.let{ types.indexOf(it)} })

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataBinding.fragment = this
        bindOnCommonViewModelUpdates(viewModel)
        bindToTaxUpdatedEvents()
        bindToTaxDialogEvents()
    }
    private fun bindToTaxDialogEvents(){
        launch { viewModel.stateFlow.bindTo(
            keySelector = { it.dialogType },
            map = { this },
            update = {
                if (it.dialogType != null && it.dialogType != DialogType.NO_SHOW){
                    handleProductDialog(it.dialogList, it.dialogType)
                }

            }
        ) }
    }
    private fun handleProductDialog(products: List<CatalogProduct>, dialogType: DialogType) {
        requireContext().dialogBuilder(
            "Select Product",
            extras = dialogType,
            items = products,
            map = { it.product.label },
            onSelected = viewModel::handleProduct
        ).setOnDismissListener { viewModel.hideDialog() }.create().show()
    }

private fun bindToTaxUpdatedEvents(){
        launch {
            viewModel.stateFlow.bindTo(
                TaxUpdateViewModel.State::updatedTaxId
            ) { id ->
                if (id.isNotNullOrBlank()) {
                    return@bindTo Toast.makeText(
                        requireContext(),
                        "Category with id [${id}] was updated",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

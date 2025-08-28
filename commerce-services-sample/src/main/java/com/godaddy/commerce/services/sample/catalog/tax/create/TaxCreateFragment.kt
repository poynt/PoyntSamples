package com.godaddy.commerce.services.sample.catalog.tax.create

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godaddy.commerce.catalog.model.CatalogProduct
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.dialogBuilder
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.TaxCreateFragmentBinding
import com.godaddy.commerce.services.sample.catalog.tax.create.TaxCreateViewModel.DialogType
import timber.log.Timber

class TaxCreateFragment :
    CommonFragment<TaxCreateFragmentBinding>(R.layout.tax_create_fragment) {

    private val viewModel: TaxCreateViewModel by viewModels()

    val showTaxOverride by observableField(
        stateFlow = { viewModel.stateFlow },
        map = TaxCreateViewModel.State::createTaxOverride
    )
    val showTaxClassification by observableField(
        stateFlow = { viewModel.stateFlow },
        map = TaxCreateViewModel.State::createTaxClassification
    )
    val taxTypes by observableField(
        stateFlow = { viewModel.stateFlow },
        map = TaxCreateViewModel.State::types
    )
    val taxTypePos by observableField(
        stateFlow = { viewModel.stateFlow },
        map = { taxType.let{ types.indexOf(it)} }
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        bindToTaxDialogEvents()
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
    private fun bindToTaxDialogEvents(){
        launch { viewModel.stateFlow.bindTo(
            keySelector = { it.dialogType },
            map = { this },
            update = {
                when (it.dialogType) {
                    DialogType.ADD_CLASSIFICATION ->
                        handleProductDialog(it.allProducts, it.dialogType)
                    DialogType.ADD_OVERRIDE ->
                        handleProductDialog(it.allProducts, it.dialogType)
                    DialogType.REMOVE_CLASSIFICATION ->
                        handleProductDialog(it.classificationProducts.toList(), it.dialogType)
                    DialogType.REMOVE_OVERRIDE ->
                        handleProductDialog(it.overrideProducts.toList(), it.dialogType)
                    DialogType.NO_SHOW -> {}
                    null -> {}
                }
            }
        ) }

    }
    private fun handleProductDialog(productIds: List<CatalogProduct>, dialogType: DialogType) {
        requireContext().dialogBuilder(
            "Select Product",
            extras = dialogType,
            items = productIds,
            map = { it.product.label },
            onSelected = viewModel::handleProduct
        ).setOnDismissListener { viewModel.hideDialog() }.create().show()}
}

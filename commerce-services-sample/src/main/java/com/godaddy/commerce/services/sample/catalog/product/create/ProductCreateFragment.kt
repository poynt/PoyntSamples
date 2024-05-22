package com.godaddy.commerce.services.sample.catalog.product.create

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godaddy.commerce.catalog.models.Category
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.dialogBuilder
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.ProductCreateFragmentBinding

class ProductCreateFragment :
    CommonFragment<ProductCreateFragmentBinding>(R.layout.product_create_fragment) {


    private val viewModel: ProductCreateViewModel by viewModels()

    val selectedCategory by observableField(
        stateFlow = { viewModel.stateFlow },
        map = ProductCreateViewModel.State::selectedCategory
    )

    val types by observableField(
        stateFlow = {viewModel.stateFlow},
        map = ProductCreateViewModel.State::productTypes
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch {
            viewModel.stateFlow.bindTo(
                map = { categories to showCategoryDialog },
                update = ::showCategoriesDialog
            )
        }
        launch {
            viewModel.stateFlow.bindTo(ProductCreateViewModel.State::createdId) { id ->
                id ?: return@bindTo
                Toast.makeText(
                    requireContext(),
                    "Product with id [$id] was created",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showCategoriesDialog(pair: Pair<List<Category>, Boolean>) {
        if (pair.second.not()) return
        requireContext().dialogBuilder(
            "Select Category",
            items = pair.first,
            map = { it.name },
            onSelected = viewModel::selectCategory
        ).setOnDismissListener { viewModel.hideProductsDialog() }.create().show()
    }
}
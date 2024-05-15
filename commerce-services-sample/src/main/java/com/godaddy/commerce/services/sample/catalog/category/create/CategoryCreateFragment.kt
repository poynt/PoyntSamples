package com.godaddy.commerce.services.sample.catalog.category.create

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
import com.godaddy.commerce.services.sample.databinding.CategoryCreateFragmentBinding

class CategoryCreateFragment :
    CommonFragment<CategoryCreateFragmentBinding>(R.layout.category_create_fragment) {


    private val viewModel: CategoryCreateViewModel by viewModels()

    val selectedProduct by observableField(
        stateFlow = { viewModel.stateFlow },
        map = CategoryCreateViewModel.State::selectedProduct
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch {
            viewModel.stateFlow.bindTo(
                map = { products to showProductDialog },
                update = ::showCategoriesDialog
            )
        }
        launch {
            viewModel.stateFlow.bindTo(CategoryCreateViewModel.State::createdId) { id ->
                id ?: return@bindTo
                Toast.makeText(
                    requireContext(),
                    "Category with id [$id] was created",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showCategoriesDialog(pair: Pair<List<Product>, Boolean>) {
        if (pair.second.not()) return
        requireContext().dialogBuilder(
            "Select Product",
            items = pair.first,
            map = { it.name },
            onSelected = viewModel::selectProduct
        ).setOnDismissListener { viewModel.hideProductsDialog() }.create().show()
    }
}
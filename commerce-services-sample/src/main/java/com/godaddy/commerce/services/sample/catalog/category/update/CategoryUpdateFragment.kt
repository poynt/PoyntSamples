package com.godaddy.commerce.services.sample.catalog.category.update

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godaddy.commerce.sdk.util.isNotNullOrBlank
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.catalog.category.create.CategoryCreateViewModel
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.CategoryUpdateFragmentBinding
import timber.log.Timber

class CategoryUpdateFragment :
    CommonFragment<CategoryUpdateFragmentBinding>(R.layout.category_update_fragment) {


    private val viewModel: CategoryUpdateViewModel by viewModels()

    val label by observableField(
        stateFlow = { viewModel.stateFlow },
        map = CategoryUpdateViewModel.State::updatedLabel
    )
    val displayOrder by observableField(
        stateFlow = { viewModel.stateFlow },
        map = CategoryUpdateViewModel.State::updatedDisplayOrder
    )
    val selectedProduct by observableField(
        stateFlow = { viewModel.stateFlow },
        map = CategoryUpdateViewModel.State::selectedProduct
    )
    val items by observableField(
        stateFlow = { viewModel.stateFlow },
        map = CategoryUpdateViewModel.State::items
    )
    val addedItems by observableField(
        stateFlow = { viewModel.stateFlow },
        map = CategoryUpdateViewModel.State::addedItems
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataBinding.fragment = this
        bindOnCommonViewModelUpdates(viewModel)
        bindToCategoryUpdatedEvents()
    }
    private fun bindToCategoryUpdatedEvents(){
        launch {
            viewModel.stateFlow.bindTo(
                CategoryUpdateViewModel.State::updatedCategoryId
            ) { id ->
                if (id.isNotNullOrBlank()) {
                    Timber.tag("test10").d("no")
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
package com.godaddy.commerce.services.sample.catalog.category.update

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.CategoryUpdateFragmentBinding

class CategoryUpdateFragment :
    CommonFragment<CategoryUpdateFragmentBinding>(R.layout.category_update_fragment) {


    private val viewModel: CategoryUpdateViewModel by viewModels()

    val item by observableField(
        stateFlow = { viewModel.stateFlow },
        map = CategoryUpdateViewModel.State::item
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch {
            viewModel.stateFlow.bindTo(CategoryUpdateViewModel.State::updatedId) {
                it ?: return@bindTo
                Toast.makeText(
                    requireContext(),
                    "Category with id [$it] was updated",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
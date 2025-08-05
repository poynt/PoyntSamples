package com.godaddy.commerce.services.sample.catalog.category.create

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.sdk.util.isNotNullOrBlank
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.CategoryCreateFragmentBinding
import java.io.Console

class CategoryCreateFragment :
    CommonFragment<CategoryCreateFragmentBinding>(R.layout.category_create_fragment) {

    private val viewModel: CategoryCreateViewModel by viewModels()

    val items by observableField(
        stateFlow = { viewModel.stateFlow },
        map = CategoryCreateViewModel.State::items
    )

    val addedItems by observableField(
        stateFlow = { viewModel.stateFlow },
        map = CategoryCreateViewModel.State::addedItems
    )

    val selectedProduct by observableField(
        stateFlow = { viewModel.stateFlow },
        map = CategoryCreateViewModel.State::selectedProduct
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataBinding.fragment = this
        bindToCategoryCreatedEvents()
    }

    private fun bindToCategoryCreatedEvents() {
        launch {
            viewModel.stateFlow.bindTo(
                CategoryCreateViewModel.State::createdId) { id ->
                if (id.isNotNullOrBlank()) {
                    return@bindTo Toast.makeText(
                        requireContext(),
                        "Category with id [$id] was created",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}

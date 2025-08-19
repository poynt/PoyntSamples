package com.godaddy.commerce.services.sample.catalog.category.create

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godaddy.commerce.sdk.util.isNotNullOrBlank
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.util.clearKeyboard
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.CategoryCreateFragmentBinding


class CategoryCreateFragment : CommonFragment<CategoryCreateFragmentBinding>(
    R.layout.category_create_fragment
) {
    private val viewModel: CategoryCreateViewModel by viewModels()

    val allItems by observableField(
        stateFlow = { viewModel.stateFlow },
        map = { addedItems + items }
    )
    val selectedProduct by observableField(
        stateFlow = { viewModel.stateFlow },
        map = CategoryCreateViewModel.State::selectedProduct
    )
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataBinding.fragment = this
        bindOnCommonViewModelUpdates(viewModel)
        bindToCategoryCreatedEvents()
        bindToAddedItemsEvents()
    }

    private fun bindToAddedItemsEvents() {
        launch { viewModel.stateFlow.bindTo(
            CategoryCreateViewModel.State::addedItems) {
                dataBinding.ProductRecyclerView.scrollToPosition(0)
            }
        }
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

    fun clearKeyboard(){
        clearKeyboard(requireView())
        view?.findViewById<View>(R.id.display_order_field)?.clearFocus()
        view?.findViewById<View>(R.id.label_field)?.clearFocus()
    }

     fun onCreateClick() {
         viewModel.create()
         clearKeyboard(requireView())
         view?.findViewById<View>(R.id.root)?.requestFocus()
    }
}
package com.godaddy.commerce.services.sample.catalog.category

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.common.view.doOnToolbarSearchQueryChanged
import com.godaddy.commerce.services.sample.databinding.CategoryFragmentBinding
import timber.log.Timber

class CategoryFragment : CommonFragment<CategoryFragmentBinding>(R.layout.category_fragment) {


    private val viewModel: CategoryViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        doOnToolbarSearchQueryChanged { viewModel.searchCategory(it) }
        launch {
            viewModel.stateFlow.bindTo({
                Timber.d("Items : $items")
                items
            }) { dataBinding.items = it }
        }
    }
}
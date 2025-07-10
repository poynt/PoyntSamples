package com.godaddy.commerce.services.sample.catalog.product

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.common.view.doOnToolbarSearchQueryChanged
import com.godaddy.commerce.services.sample.databinding.ProductFragmentBinding
import timber.log.Timber

//The Create product Screen should exemplify how to create Products.
//1.	Allow user to input all important fields.
//2.	Display a Snackbar with any Error message given from CS when Creation Fails.
//3.	Short Label field should be filled with First 5 Chars from Label
//4.	Disable Price Override field should be filled based on whether user informed Price.
//5.	Enable Inventory Tracking field should be filled based on whether user informed Quantity.


class ProductFragment : CommonFragment<ProductFragmentBinding>(R.layout.product_fragment) {


    private val viewModel: ProductViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        doOnToolbarSearchQueryChanged { viewModel.searchProduct(it) }
        launch {
            viewModel.stateFlow.bindTo({
                Timber.d("Items : $items")
                items
            }) { dataBinding.items = it }
        }
    }
}
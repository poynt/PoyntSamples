package com.godaddy.commerce.services.sample.catalog.product.create

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.catalog.product.create.ProductCreateViewModel.*
import com.godaddy.commerce.services.sample.common.extensions.bindTo
import com.godaddy.commerce.services.sample.common.extensions.dialogBuilder
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.extensions.observableField
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindOnCommonViewModelUpdates
import com.godaddy.commerce.services.sample.databinding.ProductCreateFragmentBinding
import com.godaddy.commercecore.models.Category

class ProductCreateFragment :
    CommonFragment<ProductCreateFragmentBinding>(R.layout.product_create_fragment) {


    private val viewModel: ProductCreateViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch {
            viewModel.stateFlow.bindTo(State::createdProductId) { id ->
                id ?: return@bindTo
                Toast.makeText(
                    requireContext(),
                    "Product with id [$id] was created",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

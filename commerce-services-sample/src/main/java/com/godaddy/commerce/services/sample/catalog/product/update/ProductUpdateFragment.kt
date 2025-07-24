package com.godaddy.commerce.services.sample.catalog.product.update

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
import com.godaddy.commerce.services.sample.databinding.ProductUpdateFragmentBinding

class ProductUpdateFragment :
    CommonFragment<ProductUpdateFragmentBinding>(R.layout.product_update_fragment) {

    private val viewModel: ProductUpdateViewModel by viewModels()

    val updatedLabel by observableField(
        stateFlow = { viewModel.stateFlow },
        map = { this.updatedLabel.toString()}
    )

    val updatedPrice by observableField(
        stateFlow = { viewModel.stateFlow },
        map = { this.updatedPrice?.value?.toString() }
    )

    val updatedSalePrice by observableField(
        stateFlow = { viewModel.stateFlow },
        map = { this.updatedSalePrice?.value?.toString() }
    )

    val updatedThreshold by observableField(
        stateFlow = { viewModel.stateFlow },
        map = { this.updatedThreshold?.toString() }
    )

    val updatedQuantity by observableField(
        stateFlow = { viewModel.stateFlow },
        map = { this.updatedQuantity?.toString() }
    )


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindOnCommonViewModelUpdates(viewModel)
        launch {
            viewModel.stateFlow.bindTo(ProductUpdateViewModel.State::updatedProductId) {
                it ?: return@bindTo
                Toast.makeText(
                    requireContext(),
                    "Product with id [$it] was updated",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}
package com.godaddy.commerce.services.sample

import android.os.Bundle
import android.view.View
import androidx.fragment.app.viewModels
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.view.CommonFragment
import com.godaddy.commerce.services.sample.common.view.bindToToolbarStateUpdates
import com.godaddy.commerce.services.sample.databinding.CommerceFragmentBinding
import kotlinx.coroutines.flow.collectLatest

class CommerceFragment : CommonFragment<CommerceFragmentBinding>(R.layout.commerce_fragment) {

    private val viewModel: CommerceViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindToToolbarStateUpdates(viewModel)
        launch { viewModel.stateFlow.collectLatest { dataBinding.items = it.items } }
    }
}


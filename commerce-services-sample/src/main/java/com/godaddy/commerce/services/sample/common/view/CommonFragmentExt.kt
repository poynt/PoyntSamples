package com.godaddy.commerce.services.sample.common.view

import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil.findBinding
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.godaddy.commerce.services.sample.BR
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.extensions.launch
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel
import com.godaddy.commerce.services.sample.databinding.ErrorLayoutBinding
import com.godaddy.commerce.services.sample.databinding.LoadingLayoutBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private typealias CommonFragmentTyped = CommonFragment<*>

fun CommonFragmentTyped.bindOnCommonViewModelUpdates(viewModel: CommonViewModel<*>) {
    bindCommonViewModel(viewModel = viewModel)
    bindCommonViewModelEffects(viewModel = viewModel)
    bindToCommonStateUpdates(viewModel = viewModel)
    bindToToolbarStateUpdates(viewModel = viewModel)
}

fun CommonFragmentTyped.bindCommonViewModel(viewModel: CommonViewModel<*>) {
    val binding = findBinding<ViewDataBinding>(requireView())
    binding?.setVariable(BR.viewModel, viewModel)
}

fun CommonFragmentTyped.bindCommonViewModelEffects(viewModel: CommonViewModel<*>) {
    launch {
        viewModel.effectFlow.collectLatest {
            when (it) {
                CommonViewModel.Effect.PopScreen -> findNavController().popBackStack()
                is CommonViewModel.Effect.ShowToast -> Toast.makeText(
                    requireContext(), it.message, Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

fun CommonFragmentTyped.bindToCommonStateUpdates(viewModel: CommonViewModel<*>) {
    val loadingBinding =
        findBinding<LoadingLayoutBinding>(requireView().findViewById(R.id.loading_fl))
    val errorBinding = findBinding<ErrorLayoutBinding>(requireView().findViewById(R.id.error_tv))

    viewModel.run {
        launch {
            stateFlow.map { it.commonState }.distinctUntilChanged().collectLatest {
                loadingBinding?.setVariable(BR.commonState, it)
                errorBinding?.setVariable(BR.commonState, it)
            }
        }
    }
}

fun CommonFragmentTyped.bindToToolbarStateUpdates(viewModel: CommonViewModel<*>) {
    val binding = findBinding<ViewDataBinding>(requireView().findViewById(R.id.toolbar))
    viewModel.run {
        launch {
            stateFlow.map { it.toolbarState }.distinctUntilChanged().collectLatest {
                binding?.setVariable(BR.toolbarState, it)
            }
        }
    }
}

fun Fragment.doOnToolbarSearchQueryChanged(onNewSearchText: (String) -> Unit) {
    requireView()
        .findViewById<Toolbar>(R.id.toolbar)
        .menu
        .findItem(R.id.action_search)
        .run { actionView as SearchView }
        .setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                onNewSearchText(newText.orEmpty())
                return false
            }
        })
}
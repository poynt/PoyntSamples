package com.godaddy.commerce.services.sample.common.binding

import android.view.View
import androidx.annotation.IdRes
import androidx.databinding.BindingAdapter
import androidx.navigation.Navigation

@BindingAdapter("onClickNavigateTo")
fun onClickNavigateTo(view: View, @IdRes navigationId: Int) {
    view.setOnClickListener {
        Navigation.findNavController(view).navigate(navigationId)
    }
}

@BindingAdapter("onClickNavigatePop", "onClickNavigatePopInclusive")
fun onClickNavigatePop(view: View, @IdRes popId: Int?, inclusive: Boolean? = null) {
    view.setOnClickListener {
        Navigation.findNavController(view).run {
            if (popId != null) {
                popBackStack(destinationId = popId, inclusive = inclusive == true)
            } else {
                popBackStack()
            }
        }
    }
}


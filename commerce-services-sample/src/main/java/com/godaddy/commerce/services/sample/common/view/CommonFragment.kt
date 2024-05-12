package com.godaddy.commerce.services.sample.common.view

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import com.godaddy.commerce.services.sample.BR

abstract class CommonFragment<Binding : ViewDataBinding>(contentLayoutId: Int) :
    Fragment(contentLayoutId) {

    protected lateinit var dataBinding: Binding

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dataBinding = DataBindingUtil.bind<Binding>(view) as Binding
        dataBinding.lifecycleOwner = viewLifecycleOwner
        dataBinding.setVariable(BR.fragment, this)
    }
}
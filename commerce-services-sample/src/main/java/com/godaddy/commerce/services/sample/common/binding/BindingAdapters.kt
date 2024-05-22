package com.godaddy.commerce.services.sample.common.binding

import android.app.Activity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.widget.Toolbar
import androidx.databinding.BindingAdapter
import androidx.navigation.Navigation
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.viewmodel.ToolbarState

@BindingAdapter("state")
fun state(view: Toolbar, toolbarState: ToolbarState?) {
    view.title = toolbarState?.title
    view.menu.findItem(R.id.action_search).isVisible = toolbarState?.showSearchButton == true
    if (toolbarState?.showBackIcon == true) {
        view.setNavigationIcon(R.drawable.ic_arrow_back)
        view.setNavigationOnClickListener {
            Navigation.findNavController(view.context as Activity, R.id.nav_host_fragment)
                .popBackStack()
        }
    } else {
        view.navigationIcon = null
    }
}

@BindingAdapter("android:visibility")
fun setVisibility(view: View, value: Boolean?) {
    view.visibility = if (value == true) View.VISIBLE else View.GONE
}

@BindingAdapter(value = ["app:autoCompleteData", "app:selectedPosition"], requireAll = false)
fun setAutoCompleteData(spinner: Spinner, data: List<String>?, selectedPosition: Int? = null) {
    data?.let { items ->
        val adapter = ArrayAdapter(
            spinner.context,
            android.R.layout.simple_dropdown_item_1line,
            items
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
        selectedPosition?.let { spinner.setSelection(it) }
    }
}

@BindingAdapter("app:onItemSelected")
fun setOnItemSelectedListener(
    spinner: Spinner,
    listener: OnItemSelectedListener?
) {
    listener?.let {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                listener.onItemSelected(position)
                spinner.setSelection(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // no-op
            }
        }
    }
}


interface OnItemSelectedListener {
    fun onItemSelected(position: Int)
}

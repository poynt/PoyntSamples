package com.godaddy.commerce.services.sample.common.util

import android.app.Activity
import android.view.View
import android.view.inputmethod.InputMethodManager

fun clearKeyboard(view: View) {
    view.context.getSystemService(Activity.INPUT_METHOD_SERVICE)
        .let { it as? InputMethodManager }
        ?.also { it.hideSoftInputFromWindow(view.windowToken, 0) }
}
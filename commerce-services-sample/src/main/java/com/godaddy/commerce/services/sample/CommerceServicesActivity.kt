package com.godaddy.commerce.services.sample

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.FragmentActivity
import com.godaddy.commerce.services.sample.databinding.CommerceActivityBinding

class CommerceServicesActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DataBindingUtil.setContentView<CommerceActivityBinding>(this, R.layout.commerce_activity)
    }
}
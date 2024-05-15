@file:Suppress("NOTHING_TO_INLINE")

package com.godaddy.commerce.services.sample.catalog.category

import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import com.godaddy.commerce.catalog.models.Category
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.binding.RecyclerAdapterItem
import com.godaddy.commerce.services.sample.databinding.CategoryItemBinding

data class CategoryRecyclerItem(
    override val item: Category,
    override val onBinding: (binding: CategoryItemBinding, position: Int, getItem: () -> Category) -> Unit = { _, _, _ -> },
) : RecyclerAdapterItem<Category, CategoryItemBinding>(item, onBinding)


inline fun Category.mapToUiItems(): CategoryRecyclerItem {
    return CategoryRecyclerItem(this) { binding, _, getItem ->
        binding.updateBt.setOnClickListener {
            it.findNavController().navigate(
                resId = R.id.categoryUpdateFragment,
                args = bundleOf("id" to getItem().categoryId?.toString())
            )
        }
    }
}
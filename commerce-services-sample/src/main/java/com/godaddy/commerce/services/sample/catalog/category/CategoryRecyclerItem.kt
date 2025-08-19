@file:Suppress("NOTHING_TO_INLINE")

package com.godaddy.commerce.services.sample.catalog.category

import androidx.navigation.findNavController
import com.godaddy.commerce.catalog.model.CatalogCategoryTreeNode
import com.godaddy.commerce.sdk.util.bundleOf
import com.godaddy.commerce.services.sample.R
import com.godaddy.commerce.services.sample.common.binding.RecyclerAdapterItem
import com.godaddy.commerce.services.sample.databinding.CategoryItemBinding

data class CategoryRecyclerItem(
    override val item: CatalogCategoryTreeNode,
    override val onBinding: (binding: CategoryItemBinding, position: Int, getItem: () -> Any) -> Unit = { _, _, _ -> },
) : RecyclerAdapterItem<CatalogCategoryTreeNode, CategoryItemBinding>(item, onBinding)


inline fun CatalogCategoryTreeNode.mapToUiItems(): CategoryRecyclerItem {
    return CategoryRecyclerItem(this) { binding, _, _ ->
        binding.updateBt.setOnClickListener { clickedItem ->
            clickedItem.findNavController().navigate(
                resId = R.id.categoryUpdateFragment,
                args = bundleOf("id" to categoryTreeNode.id.toString())
            )
        }
    }
}
@file:Suppress("UNCHECKED_CAST")

package com.godaddy.commerce.services.sample.common.binding

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.godaddy.commerce.services.sample.BR
import com.godaddy.commerce.services.sample.common.binding.RecyclerBindingAdapter.RecyclerBindingViewHolder
import kotlin.math.roundToInt


@BindingAdapter("adapterLayout")
fun initializeAdapter(view: RecyclerView, layoutId: Int) {
    val adapter = object : RecyclerBindingAdapter() {
        override fun getLayoutIdForPosition(position: Int): Int = layoutId
    }
    view.adapter = adapter
}


@BindingAdapter("adapterSubmitList")
fun submitItems(view: RecyclerView, items: List<RecyclerAdapterItem<*, *>>?) {
    val adapter = (view.adapter as? RecyclerBindingAdapter)
    adapter?.submitList(items as List<RecyclerAdapterItem<Any, ViewDataBinding>>)
}


@BindingAdapter("spaceItemDecoration")
fun spaceItemDecoration(view: RecyclerView, spacing: Float) {
    view.addItemDecoration(
        object : ItemDecoration() {
            override fun getItemOffsets(
                outRect: Rect,
                view: View,
                parent: RecyclerView,
                state: RecyclerView.State
            ) {
                val spacingInt = spacing.roundToInt()
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.set(0, 0, 0, spacingInt)
                } else {
                    outRect.set(0, spacingInt, 0, spacingInt)
                }
            }
        }
    )
}

@BindingAdapter("dividerItemDecoration")
fun dividerItemDecoration(view: RecyclerView, orientation: Int?) {
    view.addItemDecoration(
        DividerItemDecoration(
            view.context,
            orientation ?: RecyclerView.VERTICAL
        )
    )
}

open class RecyclerAdapterItem<T, Binding : ViewDataBinding>(
    open val item: T,
    open val onBinding: (binding: Binding, position: Int, getItem: () -> T) -> Unit = { _, _, _ -> }
) : RecyclerAdapterItemDiff<RecyclerAdapterItem<T, Binding>>


interface RecyclerAdapterItemDiff<T : Any> {
    fun areItemsTheSame(other: T): Boolean = this == other
    fun areContentsTheSame(other: T): Boolean = true
    fun getChangePayload(other: T): Any? = null
}


abstract class RecyclerBindingAdapter :
    ListAdapter<RecyclerAdapterItem<Any, ViewDataBinding>, RecyclerBindingViewHolder>(diffUtils) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerBindingViewHolder {
        val layoutInflater: LayoutInflater = LayoutInflater.from(parent.context)
        val binding: ViewDataBinding = DataBindingUtil.inflate(
            layoutInflater, viewType, parent, false
        )
        return RecyclerBindingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecyclerBindingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemViewType(position: Int): Int {
        return getLayoutIdForPosition(position)
    }

    protected abstract fun getLayoutIdForPosition(position: Int): Int


    inner class RecyclerBindingViewHolder(private val binding: ViewDataBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: RecyclerAdapterItem<Any, ViewDataBinding>) {
            binding.setVariable(BR.item, item)
            binding.executePendingBindings()
            item.onBinding(binding, bindingAdapterPosition) { getItem(bindingAdapterPosition).item }
        }
    }
}

private val diffUtils =
    object : DiffUtil.ItemCallback<RecyclerAdapterItem<Any, ViewDataBinding>>() {
        override fun areItemsTheSame(
            oldItem: RecyclerAdapterItem<Any, ViewDataBinding>,
            newItem: RecyclerAdapterItem<Any, ViewDataBinding>
        ): Boolean {
            return oldItem.areItemsTheSame(newItem)
        }

        override fun areContentsTheSame(
            oldItem: RecyclerAdapterItem<Any, ViewDataBinding>,
            newItem: RecyclerAdapterItem<Any, ViewDataBinding>
        ): Boolean {
            return oldItem.areContentsTheSame(newItem)
        }

        override fun getChangePayload(
            oldItem: RecyclerAdapterItem<Any, ViewDataBinding>,
            newItem: RecyclerAdapterItem<Any, ViewDataBinding>
        ): Any? {
            return oldItem.getChangePayload(newItem)
        }
    }
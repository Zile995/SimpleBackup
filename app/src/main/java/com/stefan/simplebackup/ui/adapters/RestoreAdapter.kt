package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import com.stefan.simplebackup.R

typealias selectionModeCallBack = (Boolean) -> Unit

class RestoreAdapter(
    override val selectedItems: MutableList<Int>,
    private val clickListener: OnClickListener,
    onSelectionModeCallback: selectionModeCallBack
) :
    BaseAppAdapter<RestoreViewHolder>(
        selectedItems,
        onSelectionModeCallback
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestoreViewHolder {
        return BaseViewHolder.getViewHolder(
            parent,
            R.layout.restore_item,
            clickListener,
            ::RestoreViewHolder
        )
    }

    override fun onBindViewHolder(holder: RestoreViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        if (selectedItems.contains(item.uid)) {
            holder.setSelected()
        }
    }

    override fun onViewRecycled(holder: RestoreViewHolder) {
        super.onViewRecycled(holder)
        holder.unsetSelected()
    }
}
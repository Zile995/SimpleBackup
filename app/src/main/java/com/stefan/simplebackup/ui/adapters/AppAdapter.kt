package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import com.stefan.simplebackup.R

class AppAdapter(
    override val selectedItems: MutableList<Int>,
    private val clickListener: OnClickListener,
    onSelectionModeCallback: selectionModeCallBack
) :
    BaseAppAdapter<AppViewHolder>(selectedItems, onSelectionModeCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return BaseViewHolder.getViewHolder(
            parent,
            R.layout.list_item,
            clickListener,
            ::AppViewHolder
        )
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        if (selectedItems.contains(item.uid)) {
            holder.setSelected()
        }
    }

    override fun onViewRecycled(holder: AppViewHolder) {
        super.onViewRecycled(holder)
        holder.unsetSelected()
    }
}


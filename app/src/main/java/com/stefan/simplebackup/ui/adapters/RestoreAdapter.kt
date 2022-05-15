package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.adapters.AppAdapter.Companion.AppDiffCallBack

class RestoreAdapter(
    override val selectedItems: MutableList<Int>,
    private val clickListener: OnClickListener,
    private val onSelectionModeCallback: (Boolean) -> Unit
) :
    ListAdapter<AppData, RestoreViewHolder>(AppDiffCallBack),
    SelectionListener<RestoreViewHolder> by BaseSelectionListenerImpl(
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
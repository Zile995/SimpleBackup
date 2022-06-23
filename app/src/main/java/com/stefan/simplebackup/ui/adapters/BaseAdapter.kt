package com.stefan.simplebackup.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.HomeItemBinding

typealias SelectionModeCallBack = (Boolean) -> Unit

class BaseAdapter(
    override val selectedItems: MutableList<Int>,
    private val onSelectionModeCallback: SelectionModeCallBack,
    private val clickListener: () -> OnClickListener
) : ListAdapter<AppData, BaseViewHolder>(DiffCallback()),
    SelectionListener<BaseViewHolder> by BaseSelectionListenerImpl(
        selectedItems,
        onSelectionModeCallback
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        val layoutInflater = getLayoutInflater(parent)
        // TODO: Use different logic for separate fragment creation
        return HomeViewHolder(
            HomeItemBinding.inflate(layoutInflater, parent, false),
            clickListener
        )
    }

    override fun onBindViewHolder(holder: BaseViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is HomeViewHolder -> holder.bind(item)
            is LocalViewHolder -> holder.bind(item)
        }
        if (selectedItems.contains(item.uid))
            holder.setSelected()
    }

    override fun onViewRecycled(holder: BaseViewHolder) {
        super.onViewRecycled(holder)
        holder.unsetSelected()
    }

    private fun getLayoutInflater(parent: ViewGroup): LayoutInflater {
        return LayoutInflater.from(parent.context)
    }

    private class DiffCallback : DiffUtil.ItemCallback<AppData>() {
        override fun areItemsTheSame(oldItem: AppData, newItem: AppData): Boolean {
            return oldItem.packageName == newItem.packageName &&
                    oldItem.versionName == newItem.versionName &&
                    oldItem.name == newItem.name &&
                    oldItem.date == newItem.date
        }

        override fun areContentsTheSame(oldItem: AppData, newItem: AppData): Boolean {
            return oldItem == newItem
        }
    }
}
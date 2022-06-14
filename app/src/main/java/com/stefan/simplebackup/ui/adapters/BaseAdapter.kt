package com.stefan.simplebackup.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.HomeItemBinding
import com.stefan.simplebackup.databinding.LocalItemBinding

typealias SelectionModeCallBack = (Boolean) -> Unit

class BaseAdapter(
    private val holderType: HolderType,
    override val selectedItems: MutableList<Int>,
    private val onSelectionModeCallback: SelectionModeCallBack,
) : ListAdapter<AppData, BaseViewHolder>(DiffCallback()),
    SelectionListener<BaseViewHolder> by BaseSelectionListenerImpl(
        selectedItems,
        onSelectionModeCallback
    ) {

    var clickListener: OnClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return when (holderType) {
            HolderType.HOME -> {
                HomeViewHolder(
                    HomeItemBinding.inflate(getLayoutInflater(parent), parent, false),
                    clickListener
                )
            }
            HolderType.LOCAL -> {
                LocalViewHolder(
                    LocalItemBinding.inflate(
                        getLayoutInflater(parent),
                        parent,
                        false
                    ), clickListener
                )
            }
            else -> {
                throw IllegalArgumentException("Invalid holder type")
            }
        }
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

enum class HolderType {
    HOME,
    LOCAL,
    DRIVE
}
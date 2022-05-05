package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.stefan.simplebackup.data.model.AppData

class AppAdapter(
    override val selectedPackageNames: MutableList<String>,
    private val clickListener: OnClickListener,
    private val onSelectionModeCallback: (Boolean) -> Unit
) :
    ListAdapter<AppData, AppViewHolder>(AppDiffCallBack),
    SelectionListener<AppViewHolder> by SharedSelectionListenerImpl(
        selectedPackageNames,
        onSelectionModeCallback
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        return AppViewHolder.getViewHolder(parent, clickListener)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        if (selectedPackageNames.contains(item.packageName)) {
            holder.setSelected()
        }
    }

    override fun onViewRecycled(holder: AppViewHolder) {
        holder.unsetSelected()
        super.onViewRecycled(holder)
    }

    companion object {
        val AppDiffCallBack = object : DiffUtil.ItemCallback<AppData>() {
            override fun areItemsTheSame(oldItem: AppData, newItem: AppData): Boolean {
                return oldItem.packageName == newItem.packageName &&
                        oldItem.versionName == newItem.versionName &&
                        oldItem.name == newItem.name
            }

            override fun areContentsTheSame(oldItem: AppData, newItem: AppData): Boolean {
                return oldItem == newItem
            }
        }
    }
}


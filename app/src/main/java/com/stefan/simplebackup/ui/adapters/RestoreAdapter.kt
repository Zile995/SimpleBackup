package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.adapters.AppAdapter.Companion.AppDiffCallBack

class RestoreAdapter(
    override val selectedPackageNames: MutableList<String>,
    private val clickListener: OnClickListener,
    private val onSelectionModeCallback: (Boolean) -> Unit
) :
    ListAdapter<AppData, RestoreViewHolder>(AppDiffCallBack),
    SelectionListener<RestoreViewHolder> by SharedSelectionListenerImpl(
        selectedPackageNames,
        onSelectionModeCallback
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RestoreViewHolder {
        return RestoreViewHolder.getViewHolder(parent, clickListener)
    }

    override fun onBindViewHolder(holder: RestoreViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        if (selectedPackageNames.contains(item.packageName)) {
            holder.setSelected()
        }
    }

    override fun onViewRecycled(holder: RestoreViewHolder) {
        holder.unsetSelected()
        super.onViewRecycled(holder)
    }
}
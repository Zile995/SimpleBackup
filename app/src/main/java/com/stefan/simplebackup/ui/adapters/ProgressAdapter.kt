package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.stefan.simplebackup.data.model.ProgressData
import com.stefan.simplebackup.databinding.ProgressItemBinding
import com.stefan.simplebackup.ui.adapters.viewholders.ProgressViewHolder
import com.stefan.simplebackup.utils.extensions.viewBinding

class ProgressAdapter : ListAdapter<ProgressData, ProgressViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder =
        ProgressViewHolder(parent.viewBinding(ProgressItemBinding::inflate))

    override fun submitList(list: MutableList<ProgressData>?) {
        val containsNonNullWorkResults = list?.all { it.workResult != null }
        if (containsNonNullWorkResults == true)
            super.submitList(list)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        val progressItem = getItem(position)
        holder.bind(progressItem)
    }

    private class DiffCallback : DiffUtil.ItemCallback<ProgressData>() {
        override fun areItemsTheSame(
            oldItem: ProgressData,
            newItem: ProgressData
        ): Boolean = oldItem.image.contentEquals(newItem.image) && oldItem.name == newItem.name

        override fun areContentsTheSame(
            oldItem: ProgressData,
            newItem: ProgressData
        ): Boolean = oldItem == newItem

    }
}
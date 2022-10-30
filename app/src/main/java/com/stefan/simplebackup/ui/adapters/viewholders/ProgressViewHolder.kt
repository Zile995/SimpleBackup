package com.stefan.simplebackup.ui.adapters.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.stefan.simplebackup.data.model.ProgressData
import com.stefan.simplebackup.databinding.ProgressItemBinding
import com.stefan.simplebackup.utils.extensions.loadBitmap

class ProgressViewHolder(
    private val binding: ProgressItemBinding
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(progressData: ProgressData) {
        binding.apply {
            applicationImage.loadBitmap(progressData.image)
            applicationName.text = progressData.name
            packageName.text = progressData.message
        }
    }

}
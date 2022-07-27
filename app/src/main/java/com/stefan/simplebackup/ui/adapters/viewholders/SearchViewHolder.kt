package com.stefan.simplebackup.ui.adapters.viewholders

import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.databinding.SearchItemBinding
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener

class SearchViewHolder(
    binding: SearchItemBinding,
    clickListener: OnClickListener
) : BaseViewHolder(binding, clickListener) {

    override val cardView: MaterialCardView
        get() = TODO("Not yet implemented")

    override fun bind(item: AppData) {
        TODO("Not yet implemented")
    }
}
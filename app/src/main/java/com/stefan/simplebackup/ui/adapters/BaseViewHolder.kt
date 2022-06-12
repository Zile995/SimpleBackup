package com.stefan.simplebackup.ui.adapters

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData

sealed class BaseViewHolder(
    binding: ViewBinding,
    private val clickListener: OnClickListener?
) : RecyclerView.ViewHolder(binding.root) {

    abstract val cardView: MaterialCardView
    abstract fun bind(item: AppData)

    init {
        binding.root.setOnClickListener {
            clickListener?.onItemViewClick(this, adapterPosition)
        }

        binding.root.setOnLongClickListener {
            clickListener?.onLongItemViewClick(this, adapterPosition)
            true
        }
    }

    fun setSelected() {
        cardView.apply {
            setCardBackgroundColor(
                context.getColor(R.color.cardViewSelected)
            )
        }
    }

    fun unsetSelected() {
        cardView.apply {
            setCardBackgroundColor(
                context.getColor(R.color.cardView)
            )
        }
    }
}
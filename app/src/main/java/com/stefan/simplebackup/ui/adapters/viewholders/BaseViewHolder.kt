package com.stefan.simplebackup.ui.adapters.viewholders

import android.content.Context
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener

sealed class BaseViewHolder(
    binding: ViewBinding,
    private val clickListener: OnClickListener
) : RecyclerView.ViewHolder(binding.root) {

    abstract val cardView: MaterialCardView
    abstract fun bind(item: AppData)

    private val context: Context = binding.root.context

    init {
        binding.root.setOnClickListener {
            clickListener.onItemViewClick(this, adapterPosition)
        }

        binding.root.setOnLongClickListener {
            clickListener.onLongItemViewClick(this, adapterPosition)
        }
    }

    fun clearSelectionColor() =
        cardView.setCardBackgroundColor(context.getColor(R.color.card_view))

    fun setSelectionColor() =
        cardView.setCardBackgroundColor(context.getColor(R.color.card_view_selected))
}
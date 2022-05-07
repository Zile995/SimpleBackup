package com.stefan.simplebackup.ui.adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R

abstract class SharedViewHolder(
    view: View,
    private val clickListener: OnClickListener
) : RecyclerView.ViewHolder(view) {

    abstract val cardView: MaterialCardView

    init {
        view.setOnLongClickListener {
            clickListener.onLongItemViewClick(this, adapterPosition)
            true
        }

        view.setOnClickListener {
            clickListener.onItemViewClick(this, adapterPosition)
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
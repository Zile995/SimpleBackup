package com.stefan.simplebackup.ui.adapters

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

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
}
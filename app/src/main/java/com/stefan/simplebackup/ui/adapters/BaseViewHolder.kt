package com.stefan.simplebackup.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.stefan.simplebackup.R

abstract class BaseViewHolder(
    view: View,
    private val clickListener: OnClickListener
) : RecyclerView.ViewHolder(view) {

    abstract val cardView: MaterialCardView

    init {
        view.setOnClickListener {
            clickListener.onItemViewClick(this, adapterPosition)
        }

        view.setOnLongClickListener {
            clickListener.onLongItemViewClick(this, adapterPosition)
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

    companion object {
        inline fun <T : BaseViewHolder> getViewHolder(
            parent: ViewGroup,
            @LayoutRes layoutRes: Int,
            clickListener: OnClickListener,
            constructor: (View, OnClickListener) -> T
        ): T {
            val layoutView = LayoutInflater
                .from(parent.context)
                .inflate(layoutRes, parent, false)

            return constructor(layoutView, clickListener)
        }
    }
}
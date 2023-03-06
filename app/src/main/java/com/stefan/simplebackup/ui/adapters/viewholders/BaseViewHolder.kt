package com.stefan.simplebackup.ui.adapters.viewholders

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
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

    private var shouldIntercept = false
    private val context: Context = binding.root.context

    abstract val cardView: MaterialCardView
    abstract fun bind(item: AppData)

    init {
        binding.apply {
            setOnTouchListener()
            root.setOnClickListener {
                clickListener.onItemViewClick(this@BaseViewHolder, adapterPosition)
            }
            root.setOnLongClickListener {
                clickListener.onLongItemViewClick(this@BaseViewHolder, adapterPosition).also {
                    shouldIntercept = it
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun ViewBinding.setOnTouchListener() {
        root.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_UP
                || event.actionMasked == MotionEvent.ACTION_DOWN
            ) shouldIntercept = false
            clickListener.onInterceptScrolling(shouldIntercept)
            false
        }
    }

    fun clearSelectionColor() =
        cardView.setCardBackgroundColor(context.getColor(R.color.card_view))

    fun setSelectionColor() =
        cardView.setCardBackgroundColor(context.getColor(R.color.card_view_selected))
}
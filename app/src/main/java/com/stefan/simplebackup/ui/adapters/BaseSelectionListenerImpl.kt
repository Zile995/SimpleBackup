package com.stefan.simplebackup.ui.adapters

import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData

class BaseSelectionListenerImpl<VH : BaseViewHolder>(
    override val selectedItems: MutableList<Int>,
    private val onSelectionModeCallback: SelectionModeCallBack
) : SelectionListener<VH> {

    override fun hasSelectedItems(): Boolean {
        return selectedItems.isNotEmpty()
    }

    override fun selectMultipleItems(selectedItems: MutableList<Int>) {
        this.selectedItems.clear()
        this.selectedItems.addAll(selectedItems)
    }

    override fun getSelected(): MutableList<Int> = selectedItems

    override fun addSelected(item: Int) {
        selectedItems.add(item)
    }

    override fun removeSelected(item: Int) {
        selectedItems.remove(item)
    }

    override fun doSelection(holder: VH, item: AppData) {
        holder.apply {
            val context = cardView.context
            if (selectedItems.contains(item.uid)) {
                removeSelected(item.uid)
                cardView.setCardBackgroundColor(context.getColor(R.color.cardView))
            } else {
                addSelected(item.uid)
                cardView.setCardBackgroundColor(context.getColor(R.color.cardViewSelected))
            }
            if (selectedItems.isEmpty()) {
                onSelectionModeCallback(false)
            }
            println("Selection list: $selectedItems")
        }
    }
}
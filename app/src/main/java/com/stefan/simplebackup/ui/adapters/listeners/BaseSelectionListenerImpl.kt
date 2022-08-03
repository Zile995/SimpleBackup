package com.stefan.simplebackup.ui.adapters.listeners

import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder

class BaseSelectionListenerImpl<VH : BaseViewHolder>(
    override val selectedItems: MutableList<Int>,
    private val onSelectionModeCallback: SelectionModeCallBack
) : SelectionListener<VH> {

    override fun hasSelectedItems(): Boolean {
        return selectedItems.isNotEmpty()
    }

    override fun selectMultipleItems(selectedItems: List<Int>) {
        this.selectedItems.clear()
        this.selectedItems.addAll(selectedItems)
    }

    override fun getSelected(): List<Int> = selectedItems

    override fun addSelected(item: Int) {
        selectedItems.add(item)
    }

    override fun removeSelected(item: Int) {
        selectedItems.remove(item)
    }

    override fun removeAllSelectedItems() {
        selectedItems.clear()
    }

    override fun doSelection(holder: VH, item: AppData) {
        holder.apply {
            selectionFinished = false
            val context = cardView.context
            if (selectedItems.contains(item.uid)) {
                removeSelected(item.uid)
                item.isSelected = false
                cardView.setCardBackgroundColor(context.getColor(R.color.cardView))
            } else {
                addSelected(item.uid)
                item.isSelected = true
                cardView.setCardBackgroundColor(context.getColor(R.color.cardViewSelected))
            }
            if (selectedItems.isEmpty()) {
                onSelectionModeCallback(false)
                selectionFinished = true
            }
        }
        println("Selection list: $selectedItems")
    }

    companion object {
        var selectionFinished: Boolean = true
    }
}
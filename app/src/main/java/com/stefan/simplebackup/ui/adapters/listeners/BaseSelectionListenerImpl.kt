package com.stefan.simplebackup.ui.adapters.listeners

import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.views.MainActivityAnimator

class BaseSelectionListenerImpl<VH : BaseViewHolder>(
    override val selectedItems: MutableList<String>,
    private val onSelectionModeCallback: SelectionModeCallBack
) : SelectionListener<VH> {

    override fun hasSelectedItems(): Boolean {
        return selectedItems.isNotEmpty()
    }

    override fun selectMultipleItems(selectedItems: List<String>) {
        this.selectedItems.clear()
        this.selectedItems.addAll(selectedItems)
    }

    override fun getSelected(): List<String> = selectedItems

    override fun addSelected(item: String) {
        selectedItems.add(item)
    }

    override fun removeSelected(item: String) {
        selectedItems.remove(item)
    }

    override fun removeAllSelectedItems() {
        selectedItems.clear()
    }

    override fun doSelection(holder: VH, item: AppData) {
        holder.apply {
            selectionFinished = false
            if (item.isSelected && selectedItems.size == 1 && !MainActivityAnimator.animationFinished) return
            val context = cardView.context
            if (selectedItems.contains(item.packageName)) {
                removeSelected(item.packageName)
                item.isSelected = false
                cardView.setCardBackgroundColor(context.getColor(R.color.cardView))
            } else {
                addSelected(item.packageName)
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
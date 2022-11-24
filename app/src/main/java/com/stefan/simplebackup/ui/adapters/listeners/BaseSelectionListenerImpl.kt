package com.stefan.simplebackup.ui.adapters.listeners

import android.util.Log
import com.stefan.simplebackup.data.model.AppData
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.views.MainActivityAnimator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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
        mNumberOfSelected.value = selectedItems.size
    }

    override fun addSelected(item: String) {
        selectedItems.add(item)
        mNumberOfSelected.value = selectedItems.size
    }

    override fun removeSelected(item: String) {
        selectedItems.remove(item)
        mNumberOfSelected.value = selectedItems.size
        if (selectedItems.isEmpty()) {
            onSelectionModeCallback(false)
            selectionFinished = true
        }
    }

    override fun removeAllSelectedItems() {
        selectedItems.clear()
        mNumberOfSelected.value = 0
        selectionFinished = true
    }

    override fun doSelection(holder: VH, item: AppData) {
        holder.apply {
            selectionFinished = false
            if (item.isSelected && selectedItems.size == 1 && !MainActivityAnimator.animationFinished) return
            if (selectedItems.contains(item.packageName)) {
                item.isSelected = false
                holder.clearSelectionColor()
                removeSelected(item.packageName)
            } else {
                item.isSelected = true
                holder.setSelectionColor()
                addSelected(item.packageName)
            }
        }
        Log.d("SelectionListener", "Selection list: $selectedItems")
    }

    companion object {
        var selectionFinished: Boolean = true
            private set

        private val mNumberOfSelected = MutableStateFlow(0)
        val numberOfSelected = mNumberOfSelected.asStateFlow()
    }
}
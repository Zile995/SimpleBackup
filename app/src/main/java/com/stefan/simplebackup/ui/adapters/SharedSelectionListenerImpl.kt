package com.stefan.simplebackup.ui.adapters

import android.util.Log
import com.stefan.simplebackup.R
import com.stefan.simplebackup.data.model.AppData

class SharedSelectionListenerImpl<VH : SharedViewHolder>(
    override val selectedPackageNames: MutableList<String>,
    private val onSelectionModeCallback: (Boolean) -> Unit
) : SelectionListener<VH> {

    override fun hasSelectedItems(): Boolean {
        return selectedPackageNames.isNotEmpty()
    }

    override fun selectMultipleItems(selectedPackageNames: List<String>) {
        this.selectedPackageNames.clear()
        this.selectedPackageNames.addAll(selectedPackageNames)
    }

    override fun getSelectedItems(): List<String> {
        return selectedPackageNames
    }

    override fun addSelectedItem(packageName: String) {
        selectedPackageNames.add(packageName)
    }

    override fun removeSelectedItem(packageName: String) {
        selectedPackageNames.remove(packageName)
    }

    override fun doSelection(holder: VH, item: AppData) {
        val context = holder.cardView.context
        if (selectedPackageNames.contains(item.packageName)) {
            removeSelectedItem(item.packageName)
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.cardView))
        } else {
            addSelectedItem(item.packageName)
            holder.cardView.setCardBackgroundColor(context.getColor(R.color.cardViewSelected))
        }
        if (selectedPackageNames.isEmpty()) {
            onSelectionModeCallback(false)
        }
        Log.d("Listener list: ", "${getSelectedItems().size}: ${getSelectedItems()}")
    }
}
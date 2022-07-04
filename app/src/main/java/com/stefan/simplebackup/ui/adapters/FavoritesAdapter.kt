package com.stefan.simplebackup.ui.adapters

import android.view.ViewGroup
import com.stefan.simplebackup.databinding.FavoritesItemBinding
import com.stefan.simplebackup.ui.adapters.listeners.OnClickListener
import com.stefan.simplebackup.ui.adapters.viewholders.BaseViewHolder
import com.stefan.simplebackup.ui.adapters.viewholders.FavoritesViewHolder
import com.stefan.simplebackup.utils.extensions.viewBinding

class FavoritesAdapter(
    selectedItems: MutableList<Int>,
    onSelectionModeCallback: SelectionModeCallBack,
    clickListener: () -> OnClickListener
) : BaseAdapter(selectedItems, onSelectionModeCallback, clickListener) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseViewHolder {
        return FavoritesViewHolder(parent.viewBinding(FavoritesItemBinding::inflate), clickListener)
    }
}
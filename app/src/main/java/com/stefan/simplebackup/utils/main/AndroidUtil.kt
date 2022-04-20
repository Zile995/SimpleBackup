package com.stefan.simplebackup.utils.main

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.stefan.simplebackup.R

fun Context.showToast(message: String) {
    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
}

fun Context.loadBitmapToImageView(byteArray: ByteArray, image: ImageView) {
    Glide.with(this).apply {
        asBitmap()
            .load(byteArray)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(image)
    }
}

//holder.cardView.setOnClickListener {
//        TODO: To be fixed later
//    val builder = AlertDialog.Builder(context, R.style.DialogTheme)
//    builder.setTitle(context.getString(R.string.confirm_restore))
//    builder.setMessage(context.getString(R.string.restore_confirmation_message))
//    builder.setPositiveButton(context.getString(R.string.yes)) { dialog, _ ->
//        dialog.cancel()
//        CoroutineScope(Dispatchers.Main).launch {
//            launch { installApp(context, item) }.join()
//            launch {
//                Toast.makeText(context, "Successfully restored!", Toast.LENGTH_SHORT)
//                    .show()
//            }
//        }
//    }
//    builder.setNegativeButton(context.getString(R.string.no)) { dialog, _ -> dialog.cancel() }
//    val alert = builder.create()
//    alert.setOnShowListener {
//        alert.getButton(AlertDialog.BUTTON_NEGATIVE)
//            .setTextColor(context.getColor(R.color.negativeDialog))
//        alert.getButton(AlertDialog.BUTTON_POSITIVE)
//            .setTextColor(context.getColor(R.color.positiveDialog))
//    }
//    alert.show()
//}

//    private fun createToolBar() {
//        TODO: To be fixed later
//        binding.toolBar.setOnMenuItemClickListener { menuItem ->
//            Log.d("Search", "toolbar item clicked")
//            when (menuItem.itemId) {
//                R.id.search -> {
//                    val searchView = menuItem?.actionView as SearchView
//                    searchView.imeOptions = EditorInfo.IME_ACTION_DONE
//                    searchView.queryHint = "Search for apps"
//
//                    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
//                        override fun onQueryTextSubmit(query: String?): Boolean {
//                            return false
//                        }
//
//                        override fun onQueryTextChange(newText: String?): Boolean {
//                            newText?.let { text ->
//                                appAdapter.filter(text)
//                            }
//                            return true
//                        }
//                    })
//                }
//                R.id.select_all -> {
//                    appViewModel.setSelectedItems(applicationList)
//                    appAdapter.notifyDataSetChanged()
//                }
//            }
//            true
//        }
//    }
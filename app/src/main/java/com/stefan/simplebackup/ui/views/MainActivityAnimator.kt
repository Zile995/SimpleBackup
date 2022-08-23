package com.stefan.simplebackup.ui.views

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.util.Log
import androidx.annotation.ColorRes
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.utils.extensions.changeBackgroundColor
import com.stefan.simplebackup.utils.extensions.getColorFromResource
import java.lang.ref.WeakReference

class MainActivityAnimator(
    private val activityReference: WeakReference<MainActivity>,
    private val bindingReference: WeakReference<ActivityMainBinding>
) {

    val animationDuration = 250L
    private val activity get() = activityReference.get()
    private val binding get() = bindingReference.get()

    fun animateSearchBarOnClick() {
        binding?.apply {
            Log.d("MainAnimator", "Expanding SearchBar on click")
            materialSearchBar.animateToParentSize(
                duration = animationDuration,
                doOnStart = {
                    animateStatusBarColor(color = R.color.searchBar)
                },
                doOnEnd = {
                    appBarLayout.setExpanded(true)
                })
        }
    }

    fun animateSearchBarOnSelection() {
        binding?.apply {
            Log.d("MainAnimator", "Expanding SearchBar on selection")
            materialSearchBar.animateToParentSize(
                duration = animationDuration,
                doOnStart = {
                    animateStatusBarColor(color = R.color.searchBar)
                })
        }
    }

    fun shrinkSearchBarToInitialSize() {
        binding?.apply {
            Log.d("MainAnimator", "Shrinking SearchBar to initial size")
            materialSearchBar.animateToInitialSize(duration = animationDuration,
                doOnStart = {
                    animateStatusBarColor(color = R.color.bottomView)
                })
        }
    }

    private fun animateStatusBarColor(@ColorRes color: Int) {
        activity?.apply {
            ObjectAnimator.ofObject(
                window,
                "statusBarColor",
                ArgbEvaluator(),
                window.statusBarColor,
                getColorFromResource(color)
            ).apply {
                duration = animationDuration
                start()
            }
        }
    }

    companion object {
        var animationFinished: Boolean = true
    }
}
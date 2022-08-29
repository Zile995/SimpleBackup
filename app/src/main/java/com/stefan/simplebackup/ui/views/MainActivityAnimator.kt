package com.stefan.simplebackup.ui.views

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.util.Log
import androidx.annotation.ColorRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.utils.extensions.getColorFromResource
import com.stefan.simplebackup.utils.extensions.getVisibleFragment
import com.stefan.simplebackup.utils.extensions.launchPostDelayed
import java.lang.ref.WeakReference

class MainActivityAnimator(
    private val activityReference: WeakReference<MainActivity>,
    private val bindingReference: WeakReference<ActivityMainBinding>
) {

    private val animationDuration = 250L
    private val binding get() = bindingReference.get()
    private val activity get() = activityReference.get()

    fun animateOnSelection(
        isSelected: Boolean,
        selectionModeCallBack: SelectionModeCallBack
    ) {
        activity?.apply {
            binding?.apply {
                if (isSelected) {
                    navigationBar.moveDown()
                    animateSearchBarOnSelection()
                    launchPostDelayed(50L) {
                        setFragmentContainerMargin(appBarLayout.height)
                    }
                } else {
                    navigationBar.moveUp {
                        setFragmentContainerMargin(navigationBar.height)
                        getVisibleFragment()?.fixRecyclerViewScrollPosition()
                    }
                    shrinkSearchBarToInitialSize()
                }
                expandAppBarLayout(isSelected)
                floatingButton.changeOnSelection(isSelected)
                materialToolbar.changeOnSelection(isSelected, selectionModeCallBack)
            }
        }
    }

    fun animateOnSearch(isSearching: Boolean) {
        binding?.apply {
            if (isSearching) {
                navigationBar.moveDown()
                animateSearchBarOnClick()
                floatingButton.hidePermanently = true
                setFragmentContainerMargin(appBarLayout.height)
            } else {
                shrinkSearchBarToInitialSize()
                floatingButton.hidePermanently = false
                navigationBar.moveUp { setFragmentContainerMargin(navigationBar.height) }
            }
            materialToolbar.changeOnSearch(isSearching,
                setNavigationOnClickListener = {
                    activity?.onSupportNavigateUp()
                })
        }
    }

    private fun expandAppBarLayout(shouldExpand: Boolean) =
        binding?.apply {
            if (shouldExpand) {
                animationFinished = false
                appBarLayout.setExpanded(shouldExpand, true)
            } else {
                if (activity?.getVisibleFragment()?.shouldMoveFragmentUp() == true) {
                    Log.d("AppBarLayout", "Collapsing the AppBarLayout")
                    appBarLayout.setExpanded(false, true)
                }
            }
        }

    private fun setFragmentContainerMargin(margin: Int) {
        binding?.apply {
            val layoutParams =
                navHostContainer.layoutParams as CoordinatorLayout.LayoutParams
            layoutParams.bottomMargin = margin
            navHostContainer.layoutParams = layoutParams
            navHostContainer.requestLayout()
        }
    }

    private fun animateSearchBarOnClick() {
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

    private fun animateSearchBarOnSelection() {
        binding?.apply {
            Log.d("MainAnimator", "Expanding SearchBar on selection")
            materialSearchBar.animateToParentSize(
                duration = animationDuration,
                doOnStart = {
                    animateStatusBarColor(color = R.color.searchBar)
                })
        }
    }

    private fun shrinkSearchBarToInitialSize() {
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
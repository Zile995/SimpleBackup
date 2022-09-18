package com.stefan.simplebackup.ui.views

import android.animation.AnimatorSet
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.animation.doOnEnd
import androidx.core.animation.doOnStart
import androidx.core.view.doOnPreDraw
import androidx.core.view.marginBottom
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.ui.adapters.SelectionModeCallBack
import com.stefan.simplebackup.utils.extensions.getColorFromResource
import com.stefan.simplebackup.utils.extensions.getVisibleFragment
import java.lang.ref.WeakReference

class MainActivityAnimator(
    private val activityReference: WeakReference<MainActivity>,
    private val bindingReference: WeakReference<ActivityMainBinding>
) {

    private val animationDuration = 250L
    private val binding get() = bindingReference.get()
    private val activity get() = activityReference.get()
    private val visibleFragment get() = activity?.getVisibleFragment()

    fun animateOnSettings(isInSettings: Boolean) {
        binding?.apply {
            root.doOnPreDraw {
                val animatorSet = AnimatorSet().apply {
                    duration = 50L
                    interpolator = DecelerateInterpolator()
                }
                if (isInSettings) {
                    animatorSet.playTogether(materialSearchBar.animateToParentSize(doOnEnd = {
                        activity?.apply {
                            window.statusBarColor = getColorFromResource(R.color.searchBar)
                        }
                        appBarLayout.setExpanded(true)
                    }))
                    animatorSet.start()
                } else {
                    animatorSet.playTogether(searchBarShrinkAnimator() ?: return@doOnPreDraw)
                    animatorSet.duration = animationDuration
                    animatorSet.start()
                }
            }
            floatingButton.hidePermanently = isInSettings
            materialToolbar.changeOnSettings(isInSettings) {
                activity?.onSupportNavigateUp()
            }
        }
    }

    fun animateOnSelection(
        isSelected: Boolean,
        selectionModeCallBack: SelectionModeCallBack
    ) {
        binding?.apply {
            val animatorSet = AnimatorSet().apply {
                duration = animationDuration
                interpolator = DecelerateInterpolator()
            }
            if (isSelected) {
                startAnimations(
                    animatorSet = animatorSet,
                    fragmentBottomMargin = appBarLayout.height
                )
            } else {
                reverseAnimations(animatorSet).doOnEnd {
                    visibleFragment?.fixRecyclerViewScrollPosition()
                }
            }
            expandAppBarLayout(isSelected)
            floatingButton.changeOnSelection(isSelected)
            materialToolbar.changeOnSelection(isSelected, selectionModeCallBack)
        }
    }

    fun animateOnSearch(isSearching: Boolean) {
        binding?.apply {
            val animatorSet = AnimatorSet().apply {
                duration = animationDuration
                interpolator = DecelerateInterpolator()
            }
            if (isSearching) {
                startAnimations(
                    animatorSet = animatorSet,
                    fragmentBottomMargin = -materialToolbar.height - root.resources.getDimensionPixelSize(
                        R.dimen.chip_group_height
                    )
                )
            } else {
                reverseAnimations(animatorSet).doOnStart {
                    setFragmentBottomMargin(navigationBar.height)
                }
            }
            floatingButton.hidePermanently = isSearching
            materialToolbar.changeOnSearch(isSearching,
                setNavigationOnClickListener = {
                    activity?.onSupportNavigateUp()
                })
        }
    }

    private fun ActivityMainBinding.startAnimations(
        animatorSet: AnimatorSet,
        fragmentBottomMargin: Int
    ) {
        setFragmentBottomMargin(fragmentBottomMargin)
        root.doOnPreDraw {
            animatorSet.playTogether(
                navigationBar.moveDown(),
                searchBarExpandAnimator() ?: return@doOnPreDraw
            )
            animatorSet.start()
        }
    }

    private fun ActivityMainBinding.reverseAnimations(animatorSet: AnimatorSet) =
        animatorSet.apply {
            root.doOnPreDraw {
                playTogether(
                    navigationBar.moveUp {
                        setFragmentBottomMargin(navigationBar.height)
                    } ?: return@doOnPreDraw,
                    searchBarShrinkAnimator() ?: return@doOnPreDraw
                )
                start()
            }
        }

    private fun ActivityMainBinding.expandAppBarLayout(shouldExpand: Boolean) =
        root.doOnPreDraw {
            if (shouldExpand)
                appBarLayout.setExpanded(true)
            else {
                if (visibleFragment?.shouldMoveFragmentUp() == true) {
                    Log.d("AppBarLayout", "Collapsing the AppBarLayout")
                    appBarLayout.setExpanded(false)
                }
            }
        }

    private fun ActivityMainBinding.setFragmentBottomMargin(bottomMargin: Int) {
        if (navHostContainer.marginBottom == bottomMargin) return
        val layoutParams =
            navHostContainer.layoutParams as CoordinatorLayout.LayoutParams
        layoutParams.bottomMargin = bottomMargin
    }

    private fun ActivityMainBinding.searchBarExpandAnimator() =
        materialSearchBar.animateToParentSize(
            doOnStart = {
                Log.d("MainAnimator", "Expanding SearchBar on click")
                activity?.apply {
                    window.statusBarColor = getColorFromResource(R.color.searchBar)
                }
            },
            doOnEnd = {
                appBarLayout.setExpanded(true)
            })

    private fun ActivityMainBinding.searchBarShrinkAnimator() =
        materialSearchBar.animateToInitialSize(
            doOnStart = {
                Log.d("MainAnimator", "Shrinking SearchBar to initial size")
                activity?.apply {
                    window.statusBarColor = getColorFromResource(R.color.bottomView)
                }
            })

    companion object {
        @Volatile
        var animationFinished: Boolean = true
    }
}
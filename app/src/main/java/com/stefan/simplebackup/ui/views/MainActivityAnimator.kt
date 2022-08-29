package com.stefan.simplebackup.ui.views

import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorRes
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.doOnPreDraw
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
                val animatorSet = AnimatorSet().apply {
                    duration = animationDuration
                    interpolator = DecelerateInterpolator()
                }
                if (isSelected) {
                    root.doOnPreDraw {
                        animatorSet.playTogether(
                            navigationBar.moveDown(),
                            *animateSearchBarOnSelection()
                        )
                        animatorSet.start()
                    }
                    launchPostDelayed(50L) {
                        setFragmentContainerMargin(appBarLayout.height)
                    }
                } else {
                    root.doOnPreDraw {
                        animatorSet.playTogether(
                            navigationBar.moveUp {
                                setFragmentContainerMargin(navigationBar.height)
                                getVisibleFragment()?.fixRecyclerViewScrollPosition()
                            }, *shrinkSearchBarToInitialSize()
                        )
                        animatorSet.start()
                    }
                }
                expandAppBarLayout(isSelected)
                floatingButton.changeOnSelection(isSelected)
                materialToolbar.changeOnSelection(isSelected, selectionModeCallBack)
            }
        }
    }

    fun animateOnSearch(isSearching: Boolean) {
        binding?.apply {
            val animatorSet = AnimatorSet().apply {
                duration = animationDuration
                interpolator = DecelerateInterpolator()
            }
            if (isSearching) {
                floatingButton.hidePermanently = true
                setFragmentContainerMargin(appBarLayout.height)
                root.doOnPreDraw {
                    animatorSet.playTogether(
                        navigationBar.moveDown(),
                        *animateSearchBarOnClick()
                    )
                    animatorSet.start()
                }
            } else {
                floatingButton.hidePermanently = false
                root.doOnPreDraw {
                    animatorSet.playTogether(navigationBar.moveUp {
                        setFragmentContainerMargin(
                            navigationBar.height
                        )
                    }, *shrinkSearchBarToInitialSize())
                    animatorSet.start()
                }
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

    private fun animateSearchBarOnClick(): Array<ValueAnimator> =
        binding?.run {
            return@run materialSearchBar.animateToParentSize(
                doOnStart = {
                    Log.d("MainAnimator", "Expanding SearchBar on click")
                    animateStatusBarColor(color = R.color.searchBar)
                },
                doOnEnd = {
                    appBarLayout.setExpanded(true)
                })
        } ?: arrayOf()

    private fun animateSearchBarOnSelection(): Array<ValueAnimator> =
        binding?.run {
            return@run materialSearchBar.animateToParentSize(
                doOnStart = {
                    Log.d("MainAnimator", "Expanding SearchBar on selection")
                    animateStatusBarColor(color = R.color.searchBar)
                })
        } ?: arrayOf()

    private fun shrinkSearchBarToInitialSize(): Array<ValueAnimator> =
        binding?.run {
            return@run materialSearchBar.animateToInitialSize(
                doOnStart = {
                    Log.d("MainAnimator", "Shrinking SearchBar to initial size")
                    animateStatusBarColor(color = R.color.bottomView)
                })
        } ?: arrayOf()

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
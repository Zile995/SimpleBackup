package com.stefan.simplebackup.ui.views

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.view.animation.AccelerateInterpolator
import androidx.annotation.ColorRes
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.utils.extensions.changeBackgroundColor
import com.stefan.simplebackup.utils.extensions.getColorFromResource
import com.stefan.simplebackup.utils.extensions.hide
import java.lang.ref.WeakReference

class MainActivityAnimator(
    private val activityReference: WeakReference<MainActivity>,
    private val bindingReference: WeakReference<ActivityMainBinding>
) {

    val animationDuration = 250L
    private val activity get() = activityReference.get()
    private val binding get() = bindingReference.get()

    fun animateSearchBarOnClick() {
        activity?.apply {
            binding?.apply {
                materialSearchBar.isEnabled = false
                expandSearchBarToParentView(
                    doOnStart = {
                        animateStatusBarColor(color = R.color.searchBar)
                    },
                    doOnEnd = {
                        appBarLayout.setExpanded(true)
                    })
            }
        }
    }

    fun animateSearchBarOnSelection() {
        activity?.apply {
            binding?.apply {
                materialSearchBar.isEnabled = false
                expandSearchBarToParentView(
                    doOnStart = {
                        animateStatusBarColor(color = R.color.searchBar)
                    },
                    doOnEnd = {
                        materialSearchBar.isEnabled = true
                    })
            }
        }
    }

    fun shrinkSearchBarToInitialSize() {
        activity?.apply {
            binding?.apply {
//                searchView.clearFocus()
//                searchView.fadeOut(0L)
//                searchView.setQuery("", false)
                println("Calling animateToInitialSize")
                appBarLayout.changeBackgroundColor(applicationContext, R.color.bottomView)
                materialSearchBar.animateToInitialSize(duration = animationDuration,
                    doOnStart = {
                        animationFinished = false
                        animateStatusBarColor(color = R.color.bottomView)
                    },
                    doOnEnd = {
                        materialSearchBar.isEnabled = true
                        animationFinished = true
                    })
            }
        }
    }

    private inline fun expandSearchBarToParentView(
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        binding?.apply {
            materialSearchBar.animateToParentSize(duration = animationDuration,
                doOnStart = {
                    animationFinished = false
                    doOnStart.invoke()
                },
                doOnEnd = {
                    doOnEnd.invoke()
                    animationFinished = true
                })
        }
    }

    fun prepareWhenSearching(isSearching: Boolean) {
        activity?.apply {
            binding?.apply {
                if (isSearching) {
                    window.statusBarColor = getColorFromResource(R.color.searchBar)
                    appBarLayout.changeBackgroundColor(applicationContext, R.color.searchBar)
                    navigationBar.hide()
                }
            }
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
package com.stefan.simplebackup.ui.views

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import androidx.annotation.ColorRes
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.utils.extensions.*
import java.lang.ref.WeakReference

class SearchBarAnimator(
    private val activityReference: WeakReference<MainActivity>,
    private val bindingReference: WeakReference<ActivityMainBinding>
) {

    val expandDuration = 300L
    val shrinkDuration = expandDuration

    private val activity get() = activityReference.get()
    private val binding get() = bindingReference.get()

    fun animateOnClick() {
        activity?.apply {
            binding?.apply {
                materialSearchBar.isEnabled = false
                expandToParentView(
                    doOnStart = {
                        animateStatusBarColor(
                            color = R.color.searchBar,
                            animationDuration = expandDuration
                        )
                    },
                    doOnEnd = {
                        appBarLayout.setExpanded(true)
                    })
            }
        }
    }

    fun animateOnSelection() {
        activity?.apply {
            binding?.apply {
                materialSearchBar.isEnabled = false
                expandToParentView(
                    doOnStart = {
                        animateStatusBarColor(
                            color = R.color.searchBar,
                            animationDuration = expandDuration
                        )
                    })
            }
        }
    }

    fun animateToInitialSize() {
        activity?.apply {
            binding?.apply {
                println("Calling animateToInitialSize")
                appBarLayout.changeBackgroundColor(applicationContext, R.color.bottomView)
                materialSearchBar.animateToInitialSize(duration = shrinkDuration,
                    doOnStart = {
                        animateStatusBarColor(
                            color = R.color.bottomView,
                            animationDuration = shrinkDuration
                        )
                    },
                    doOnEnd = {
                        materialSearchBar.isEnabled = true
                        materialToolbar.setNavigationIcon(R.drawable.ic_search)
                        materialToolbar.setNavigationOnClickListener {
                            materialSearchBar.performClick()
                        }
                    })
            }
        }
    }

    private inline fun expandToParentView(
        crossinline doOnStart: () -> Unit = {},
        crossinline doOnEnd: () -> Unit = {}
    ) {
        binding?.apply {
            materialSearchBar.animateToParentSize(duration = expandDuration,
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

    private fun resetSearchView() {
        binding?.apply {
//            searchView.clearFocus()
//            searchView.fadeOut(0L)
//            searchView.setQuery("", false)
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

    private fun animateStatusBarColor(@ColorRes color: Int, animationDuration: Long = 300L) {
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
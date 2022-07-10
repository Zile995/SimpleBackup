package com.stefan.simplebackup.ui.views

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.res.ColorStateList
import android.view.View
import androidx.annotation.ColorRes
import com.stefan.simplebackup.R
import com.stefan.simplebackup.databinding.ActivityMainBinding
import com.stefan.simplebackup.ui.activities.MainActivity
import com.stefan.simplebackup.utils.extensions.*
import java.lang.ref.WeakReference

class SearchBarAnimator(
    val activityReference: WeakReference<MainActivity>,
    val bindingReference: WeakReference<ActivityMainBinding>
) {

    val expandDuration = 300L
    val shrinkDuration = 250L

    inline fun animateSearchBarOnClick(
        crossinline navigate: () -> Unit
    ) {
        activityReference.get()?.apply {
            bindingReference.get()?.apply {
                searchBar.animateTo(
                    (searchBar.parent as View).height,
                    (searchBar.parent as View).width,
                    animationDuration = expandDuration,
                    doOnAnimationStart = {
                        navigate.invoke()
                        floatingButton.hide()
                        floatingButton.setOnClickListener(null)
                        searchMagIcon.fadeOut(expandDuration)
                        searchText.fadeOut(expandDuration)
                        animateStatusBarColor(R.color.searchBar)
                    },
                    doOnAnimationEnd = {
                        searchView.fadeIn(animationDuration = expandDuration)
                        materialToolbar.fadeIn(animationDuration = expandDuration,
                            onAnimationCancel = {
                                searchBar.show()
                            }, onAnimationEnd = {
                                searchBar.hide()
                            })
                    })
            }
        }
    }

    fun revertSearchBarToInitialSize() {
        activityReference.get()?.apply {
            bindingReference.get()?.apply {
                searchBar.show()
                resetSearchView()
                appBarLayout.changeBackgroundColor(applicationContext, R.color.bottomView)
                materialToolbar.fadeOut(animationDuration = 0L) {
                    searchBar.animateToInitialSize(animationDuration = shrinkDuration,
                        doOnAnimationStart = {
                            animateStatusBarColor(R.color.bottomView)
                            searchMagIcon.fadeIn(shrinkDuration)
                            searchText.fadeIn(shrinkDuration)
                        },
                        doOnAnimationEnd = {
                            searchBar.isEnabled = true

                        })
                }
            }
        }
    }

    private fun resetSearchView() {
        bindingReference.get()?.apply {
            searchView.setQuery("", false)
            searchView.clearFocus()
            searchView.fadeOut(0L)
        }
    }

    fun animateStatusBarColor(@ColorRes color: Int) {
        activityReference.get()?.apply {
            window.apply {
                ObjectAnimator.ofObject(
                    this,
                    "statusBarColor",
                    ArgbEvaluator(),
                    statusBarColor,
                    context.getColorFromResource(color)
                ).apply {
                    duration = 100L
                    start()
                }
            }
        }
    }
}
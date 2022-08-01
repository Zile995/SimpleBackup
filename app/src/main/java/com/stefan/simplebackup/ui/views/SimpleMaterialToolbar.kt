package com.stefan.simplebackup.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.google.android.material.appbar.MaterialToolbar
import com.stefan.simplebackup.R


class SimpleMaterialToolbar(
    context: Context, attrs: AttributeSet?,
    defStyleAttr: Int
) : MaterialToolbar(context, attrs, defStyleAttr) {

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, R.attr.toolbarStyle)
}
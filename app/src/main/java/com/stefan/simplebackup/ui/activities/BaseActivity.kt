package com.stefan.simplebackup.ui.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.stefan.simplebackup.R

abstract class BaseActivity : AppCompatActivity() {
    // TODO: Handle all shared permissions requests here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(R.color.background)
    }
}
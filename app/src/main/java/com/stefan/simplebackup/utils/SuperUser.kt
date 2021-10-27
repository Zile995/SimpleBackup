package com.stefan.simplebackup.utils

import android.util.Log
import java.io.DataOutputStream
import java.io.IOException

class SuperUser private constructor() {
    companion object Do {
        fun sudo(vararg strings: String) {
            try {
                val su = Runtime.getRuntime().exec("su")
                val outputStream = DataOutputStream(su.outputStream)
                for (s in strings) {
                    outputStream.writeBytes(s + "\n")
                    Log.d("command", "$s\n")
                    outputStream.flush()
                }

                outputStream.writeBytes("exit\n")
                outputStream.flush()
                try {
                    su.waitFor()
                } catch (e: InterruptedException) {
                    e.printStackTrace()
                }
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
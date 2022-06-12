package com.stefan.simplebackup.utils.extensions

import kotlin.math.pow

fun Number.bytesToString(): String {
    return String.format("%3.1f %s", this.toFloat() / 1_000.0.pow(2), "MB")
}

fun String.checkedString() = run {
    if (length > 36) substring(0, 36).plus("...") else this
}
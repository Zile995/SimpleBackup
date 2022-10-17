package com.stefan.simplebackup.utils.extensions

import kotlin.math.pow

fun Number.bytesToMegaBytesString(): String {
    return String.format("%3.1f %s", convertBytesToMegaBytes(), "MB")
}

fun Number.convertBytesToMegaBytes() = toFloat() / 1_000.0.pow(2)
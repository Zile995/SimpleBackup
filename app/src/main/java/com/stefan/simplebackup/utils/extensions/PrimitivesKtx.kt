package com.stefan.simplebackup.utils.extensions

import kotlin.math.pow

fun Number.bytesToMegaBytesString(): String =
    String.format("%3.2f %s", convertBytesToMegaBytes(), "MB")

fun Number.convertBytesToMegaBytes() = toDouble() / 1_000.0.pow(2)
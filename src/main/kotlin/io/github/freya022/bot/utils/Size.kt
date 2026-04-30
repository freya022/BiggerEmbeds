package io.github.freya022.bot.utils

import kotlin.math.roundToLong

@JvmInline
value class Size(val bits: Long) : Comparable<Size> {
    val bytes: Long get() = bits / 8

    operator fun plus(other: Size) = Size(bits + other.bits)
    operator fun minus(other: Size) = Size(bits - other.bits)
    operator fun times(other: Size) = Size(bits * other.bits)
    operator fun div(other: Size) = Size(bits / other.bits)
    operator fun rem(other: Size) = Size(bits % other.bits)

    override fun compareTo(other: Size): Int = bits.compareTo(other.bits)

    override fun toString(): String {
        if (this > 2.gigabytes) return "%.2f GB".format(this.bytes / 1.gigabytes.bytes.toDouble())
        if (this > 2.megabytes) return "%.2f MB".format(this.bytes / 1.megabytes.bytes.toDouble())
        if (this > 2.kilobytes) return "%.2f KB".format(this.bytes / 1.kilobytes.bytes.toDouble())
        return "$bytes B"
    }

    companion object {
        val Number.bytes get() = Size((this.toDouble() * 8).roundToLong())
        val Number.kilobytes get() = Size((this.toDouble() * 1024 * 8).roundToLong())
        val Number.megabytes get() = Size((this.toDouble() * 1024 * 1024 * 8).roundToLong())
        val Number.gigabytes get() = Size((this.toDouble() * 1024 * 1024 * 1024 * 8).roundToLong())

        val Number.bits get() = Size(this.toLong())
        val Number.kilobits get() = Size((this.toDouble() * 1024).roundToLong())
        val Number.megabits get() = Size((this.toDouble() * 1024 * 1024).roundToLong())
        val Number.gigabits get() = Size((this.toDouble() * 1024 * 1024 * 1024).roundToLong())
    }
}

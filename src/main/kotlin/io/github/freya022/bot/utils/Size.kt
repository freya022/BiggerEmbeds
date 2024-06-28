package io.github.freya022.bot.utils

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
        if (this > 2.gigabytes) return "%.2f GB".format(this.bytes / 2.gigabits.bytes.toDouble())
        if (this > 2.megabytes) return "%.2f MB".format(this.bytes / 2.megabytes.bytes.toDouble())
        if (this > 2.kilobytes) return "%.2f KB".format(this.bytes / 2.kilobytes.bytes.toDouble())
        return "$bytes B"
    }

    companion object {
        val Number.bytes get() = Size(this.toLong() * 8)
        val Number.kilobytes get() = Size(this.toLong() * 1024 * 8)
        val Number.megabytes get() = Size(this.toLong() * 1024 * 1024 * 8)
        val Number.gigabytes get() = Size(this.toLong() * 1024 * 1024 * 1024 * 8)

        val Number.bits get() = Size(this.toLong())
        val Number.kilobits get() = Size(this.toLong() * 1024)
        val Number.megabits get() = Size(this.toLong() * 1024 * 1024)
        val Number.gigabits get() = Size(this.toLong() * 1024 * 1024 * 1024)
    }
}
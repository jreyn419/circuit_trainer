package io.github.jreyn419.circuittrainer.util

/** Formats a duration in seconds as "m:ss" (or "h:mm:ss" for an hour or more). */
fun formatDuration(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

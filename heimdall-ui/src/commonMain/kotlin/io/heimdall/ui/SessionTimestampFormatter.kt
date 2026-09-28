package io.heimdall.ui

/**
 * UTC, not the viewer's local time zone — this is a debug panel read by the developer, and a
 * date without correct timezone handling would be worse than a plain, unambiguous epoch. Pure
 * arithmetic (Howard Hinnant's `civil_from_days`) so no platform date API or extra dependency is
 * needed for a KMP-common composable.
 */
internal fun formatSessionTimestamp(epochMillis: Long): String {
    val totalSeconds = epochMillis.floorDiv(1_000L)
    val days = totalSeconds.floorDiv(86_400L)
    val secondsOfDay = totalSeconds.mod(86_400L)
    val hour = secondsOfDay / 3_600L
    val minute = (secondsOfDay % 3_600L) / 60L

    val z = days + 719_468L
    val era = (if (z >= 0) z else z - 146_096L).floorDiv(146_097L)
    val doe = z - era * 146_097L
    val yoe = (doe - doe / 1_460L + doe / 36_524L - doe / 146_096L) / 365L
    val year = yoe + era * 400L
    val doy = doe - (365L * yoe + yoe / 4L - yoe / 100L)
    val mp = (5L * doy + 2L) / 153L
    val day = doy - (153L * mp + 2L) / 5L + 1L
    val month = mp + (if (mp < 10L) 3L else -9L)

    return "${(year + if (month <= 2L) 1L else 0L).pad(4)}-${month.pad(2)}-${day.pad(2)} " +
        "${hour.pad(2)}:${minute.pad(2)} UTC"
}

private fun Long.pad(width: Int): String = toString().padStart(width, '0')

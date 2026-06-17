package com.livana.app.core.common

import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shared timestamp formatting helpers. Both functions are parse-safe: if the
 * input string cannot be parsed as an ISO-8601 timestamp, they return the
 * original string unchanged (never crash).
 *
 * The backend returns timestamps like "2026-06-07T12:00:00.000+00:00".
 * These are parsed with [OffsetDateTime.parse], which handles ISO-8601 with
 * offset natively (no custom formatter needed).
 */

private val ShortDateThisYear: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d", Locale.US)

private val ShortDateOtherYear: DateTimeFormatter =
    DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US)

/**
 * Formats an ISO-8601 timestamp string as a relative time label:
 * - "just now" (< 1 minute ago)
 * - "N minutes ago" / "1 minute ago"
 * - "N hours ago" / "1 hour ago"
 * - "Yesterday"
 * - Short date like "Jun 7" (same year) or "Jun 7, 2026" (different year)
 *
 * Used for donation rows on the pool detail screen.
 */
fun String.toRelativeTime(now: Instant = Instant.now()): String {
    val instant = parseToInstant() ?: return this
    val elapsed = Duration.between(instant, now)

    if (elapsed.isNegative) {
        // Future timestamp — fall back to short date.
        return formatShortDate(instant, now)
    }

    val minutes = elapsed.toMinutes()
    val hours = elapsed.toHours()

    return when {
        minutes < 1 -> "just now"
        minutes == 1L -> "1 minute ago"
        minutes < 60 -> "$minutes minutes ago"
        hours == 1L -> "1 hour ago"
        hours < 24 -> "$hours hours ago"
        hours < 48 -> "Yesterday"
        else -> formatShortDate(instant, now)
    }
}

/**
 * Formats an ISO-8601 timestamp string as a short date: "Jun 7".
 * Used for proof rows on the pool detail screen (e.g. "Submitted Jun 7").
 *
 * Always uses the same-year format (no year suffix) because proof dates
 * are typically recent. If you need a year-aware variant, use [toRelativeTime].
 */
fun String.toShortDate(): String {
    val instant = parseToInstant() ?: return this
    val zoned = instant.atZone(ZoneId.systemDefault())
    return ShortDateThisYear.format(zoned)
}

// ---------------------------------------------------------------------------
// Internal helpers
// ---------------------------------------------------------------------------

private fun String.parseToInstant(): Instant? =
    try {
        OffsetDateTime.parse(this).toInstant()
    } catch (_: Exception) {
        null
    }

private fun formatShortDate(instant: Instant, now: Instant): String {
    val zoned = instant.atZone(ZoneId.systemDefault())
    val nowZoned = now.atZone(ZoneId.systemDefault())
    val formatter = if (zoned.year == nowZoned.year) ShortDateThisYear else ShortDateOtherYear
    return formatter.format(zoned)
}

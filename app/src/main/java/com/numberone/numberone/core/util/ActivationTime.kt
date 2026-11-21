package com.numberone.numberone.core.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Parses an ISO-8601 timestamp returned by the backend or 5SIM.
 *
 * Accepts offsets such as "Z" and "+00:00" (which java.time.Instant.parse rejects on
 * some Android runtimes) and falls back to UTC when no offset is present.
 */
fun parseInstantOrNull(raw: String?): Instant? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    return runCatching { OffsetDateTime.parse(value).toInstant() }
        .recoverCatching { LocalDateTime.parse(value).toInstant(ZoneOffset.UTC) }
        .getOrNull()
}

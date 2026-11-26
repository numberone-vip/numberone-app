package com.numberone.numberone.core.error

/**
 * Decides how much of a failure is shown on screen.
 *
 * Users always get the friendly message. Technical detail (exception type and raw message)
 * is shown only in debuggable builds. Release builds show a short category code instead,
 * which is enough for support to locate the problem without exposing internals.
 */
object ErrorPresentation {

    /** Set once at startup from the application's debuggable flag. */
    @Volatile
    var showTechnicalDetails: Boolean = false

    fun describe(e: Throwable): String {
        val friendly = ErrorMapper.toUserMessage(e)
        if (showTechnicalDetails) {
            val cause = (e as? AppException)?.cause ?: e
            // The friendly text already carries the raw message: nothing technical to add.
            if (cause.message?.let { friendly.contains(it) } == true) return friendly
            val technical = listOfNotNull(cause::class.simpleName, cause.message?.takeIf { it.isNotBlank() })
                .joinToString(": ")
            if (technical.isNotBlank() && technical !in friendly) return "$friendly\n\n[$technical]"
            return friendly
        }
        return "$friendly\n\n[${ErrorMapper.classify(e).category.name}]"
    }
}

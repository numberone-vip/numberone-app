package com.numberone.numberone.core.log

/**
 * Minimal logging facade. Errors that are intentionally swallowed must go through here
 * so they are visible in logcat instead of disappearing silently.
 *
 * Never pass tokens, passwords or request bodies as [message].
 */
object AppLog {
    enum class Level { WARN, ERROR }

    @Volatile
    var sink: (Level, String, String, Throwable?) -> Unit = { level, tag, message, t ->
        System.err.println("[$level][$tag] $message" + (t?.let { " :: ${it::class.simpleName}: ${it.message}" } ?: ""))
    }

    fun warn(tag: String, message: String, t: Throwable? = null) = sink(Level.WARN, tag, message, t)
    fun error(tag: String, message: String, t: Throwable? = null) = sink(Level.ERROR, tag, message, t)
}

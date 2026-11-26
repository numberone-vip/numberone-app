package com.numberone.numberone.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * App-wide signal that the signed-in session is no longer valid.
 * Any ViewModel may raise it; the navigation layer reacts once and acknowledges it.
 */
object SessionEvents {
    private val _expired = MutableStateFlow(false)
    val expired: StateFlow<Boolean> = _expired.asStateFlow()

    fun markExpired() { _expired.value = true }
    fun acknowledge() { _expired.value = false }
}

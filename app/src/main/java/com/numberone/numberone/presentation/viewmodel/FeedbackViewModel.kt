package com.numberone.numberone.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numberone.numberone.core.error.ErrorMapper
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/**
 * Shared loading / error / message state and the single task runner used by ViewModels
 * that show a feedback banner. Subclasses may change how errors are described.
 */
open class FeedbackViewModel : ViewModel() {
    var loading by mutableStateOf(false); protected set
    var error by mutableStateOf<String?>(null); protected set
    var message by mutableStateOf<String?>(null); protected set

    protected open fun describeError(e: Throwable): String = ErrorMapper.toUserMessage(e)

    /** Called after a task fails, so a subclass can react (for example, to an expired session). */
    protected open fun onTaskFailed(e: Throwable) {}

    /** Runs [task] unless another task is in flight. Cancellation is rethrown, never shown. */
    protected fun run(task: suspend () -> Unit) {
        if (loading) return
        viewModelScope.launch {
            loading = true; error = null; message = null
            try {
                task()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = describeError(e)
                onTaskFailed(e)
            } finally {
                loading = false
            }
        }
    }
}

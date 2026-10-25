package dev.neverpaid.health.core.mvi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.error.AppError
import dev.neverpaid.health.core.error.AppErrorType
import dev.neverpaid.health.core.error.toAppError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseViewModel<Action : UiAction, State : UiState, Effect : UiEffect>(
    initialState: State
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<State> = _state.asStateFlow()

    protected val currentState: State get() = _state.value

    private val _effect = Channel<Effect>(Channel.BUFFERED)
    val effect: Flow<Effect> = _effect.receiveAsFlow()

    private val _errors = Channel<AppError>(Channel.BUFFERED)
    val errors: Flow<AppError> = _errors.receiveAsFlow()

    abstract fun onAction(action: Action)

    protected fun updateState(reduce: State.() -> State) {
        _state.update(reduce)
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch {
            _effect.send(effect)
        }
    }

    protected fun sendError(error: AppError) {
        viewModelScope.launch {
            _errors.send(error)
        }
    }

    protected fun launchGuarded(
        onError: ((AppError) -> Unit)? = null,
        block: suspend CoroutineScope.() -> Unit
    ): Job {
        return viewModelScope.launch {
            try {
                block()
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                val appError = t.toAppError()
                if (onError != null) {
                    onError(appError)
                } else {
                    _errors.send(appError)
                }
            }
        }
    }
}

@file:Suppress("UNCHECKED_CAST")

package com.godaddy.commerce.services.sample.common.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.godaddy.commerce.services.sample.common.viewmodel.CommonViewModel.ViewModelState
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.reflect.KClass
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions

abstract class CommonViewModel<T : ViewModelState>(state: T) : ViewModel() {


    private val _mutableStateFlow = MutableStateFlow(state)
    val stateFlow = _mutableStateFlow.asStateFlow()
    protected val state get() = _mutableStateFlow.value

    private val _effectFlow = MutableSharedFlow<Effect>()
    val effectFlow = _effectFlow.asSharedFlow()

    protected fun update(run: T.() -> T) {
        _mutableStateFlow.update(run)
    }

    protected fun sendEffect(effect: Effect) {
        viewModelScope.launch { _effectFlow.emit(effect) }
    }

    protected fun execute(execute: suspend () -> Unit) {
        update { copyCommonState(this::class, commonState.copy(loading = true, error = "")) }
        viewModelScope.launch(CoroutineExceptionHandler { _, throwable ->
            update {
                copyCommonState(
                    this::class,
                    commonState.copy(loading = false, error = throwable.message)
                )
            }
        }) {
            execute()
            update { copyCommonState(this::class, commonState.copy(loading = false, error = "")) }
        }
    }

    // Do not use this method on real project.
    private fun T.copyCommonState(klass: KClass<out T>, commonState: CommonState): T {
        val copy = klass.memberFunctions.first { it.name == "copy" }
        val instanceParam = copy.instanceParameter!!
        val commonStateParam = copy.parameters.first { it.name == "commonState" }
        return copy.callBy(mapOf(instanceParam to this, commonStateParam to commonState)) as T
    }


    sealed interface Effect {
        data class ShowToast(val message: String) : Effect
        object PopScreen : Effect
    }

    interface ViewModelState {
        val commonState: CommonState
        val toolbarState: ToolbarState
    }
}
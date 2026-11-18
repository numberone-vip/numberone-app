package com.numberone.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numberone.app.data.repository.ServicesRepository
import com.numberone.app.domain.model.VirtualService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StoreUiState {
    data object Loading : StoreUiState
    data class Success(val services: List<VirtualService>) : StoreUiState
    data class Error(val message: String) : StoreUiState
}

class StoreViewModel(
    private val repository: ServicesRepository = ServicesRepository()
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<StoreUiState>(StoreUiState.Loading)

    val uiState: StateFlow<StoreUiState> = _uiState.asStateFlow()

    init {
        loadServices()
    }

    fun loadServices() {
        viewModelScope.launch {
            _uiState.value = StoreUiState.Loading

            repository.getAvailableServices()
                .onSuccess { services ->
                    _uiState.value = StoreUiState.Success(services)
                }
                .onFailure { error ->
                    _uiState.value = StoreUiState.Error(
                        error.localizedMessage
                            ?: "Failed to load services"
                    )
                }
        }
    }
}

package com.numberone.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.numberone.app.data.repository.OrdersRepository
import com.numberone.app.domain.model.PurchaseRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface OrdersUiState {
    data object Loading : OrdersUiState
    data class Success(val orders: List<PurchaseRequest>) : OrdersUiState
    data class Error(val message: String) : OrdersUiState
}

class OrdersViewModel(
    private val repository: OrdersRepository = OrdersRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<OrdersUiState>(OrdersUiState.Loading)
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.value = OrdersUiState.Loading
            repository.getMyOrders()
                .onSuccess { _uiState.value = OrdersUiState.Success(it) }
                .onFailure {
                    _uiState.value = OrdersUiState.Error(
                        it.localizedMessage ?: "تعذر تحميل طلباتك."
                    )
                }
        }
    }
}

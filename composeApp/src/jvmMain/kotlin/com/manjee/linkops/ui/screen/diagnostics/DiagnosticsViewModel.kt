package com.manjee.linkops.ui.screen.diagnostics

import com.manjee.linkops.di.AppContainer
import com.manjee.linkops.domain.model.AssetLinksValidation
import com.manjee.linkops.domain.model.ValidationStatus
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * UI State for Diagnostics Screen
 */
data class DiagnosticsUiState(
    val domain: String = "",
    val isLoading: Boolean = false,
    val validation: AssetLinksValidation? = null,
    val error: String? = null,
    val history: List<ValidationHistoryItem> = emptyList()
)

/**
 * History item for past validations
 */
data class ValidationHistoryItem(
    val domain: String,
    val status: ValidationStatus,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * ViewModel for Diagnostics Screen
 *
 * Handles assetlinks.json validation and result display
 */
class DiagnosticsViewModel {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    /**
     * Update domain input
     */
    fun updateDomain(domain: String) {
        _uiState.update { it.copy(domain = domain, error = null) }
    }

    /**
     * Validate assetlinks.json for the entered domain
     */
    fun validateDomain() {
        val domain = _uiState.value.domain.trim()
        if (domain.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a domain") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            AppContainer.validateAssetLinksUseCase(domain)
                .onSuccess { validation ->
                    // Add to history
                    val historyItem = ValidationHistoryItem(
                        domain = validation.domain,
                        status = validation.status
                    )
                    val newHistory = listOf(historyItem) + _uiState.value.history.take(9)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            validation = validation,
                            history = newHistory
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Validation failed"
                        )
                    }
                }
        }
    }

    /**
     * Validate a domain from history
     */
    fun validateFromHistory(domain: String) {
        _uiState.update { it.copy(domain = domain) }
        validateDomain()
    }

    /**
     * Clear current validation result
     */
    fun clearResult() {
        _uiState.update { it.copy(validation = null, error = null) }
    }

    /**
     * Clear validation history
     */
    fun clearHistory() {
        _uiState.update { it.copy(history = emptyList()) }
    }

    /**
     * Cleanup resources
     */
    fun onCleared() {
        viewModelScope.cancel()
    }
}

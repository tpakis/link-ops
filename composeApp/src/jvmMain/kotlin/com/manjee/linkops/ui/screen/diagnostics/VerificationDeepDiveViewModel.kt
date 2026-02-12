package com.manjee.linkops.ui.screen.diagnostics

import com.manjee.linkops.di.AppContainer
import com.manjee.linkops.domain.model.Device
import com.manjee.linkops.domain.model.VerificationDiagnostics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * UI State for Verification Deep Dive
 */
data class VerificationDeepDiveUiState(
    val packageName: String = "",
    val selectedDevice: Device? = null,
    val isLoading: Boolean = false,
    val diagnostics: VerificationDiagnostics? = null,
    val error: String? = null
)

/**
 * ViewModel for Verification Deep Dive feature
 *
 * Handles deep verification analysis for a specific package on a selected device,
 * including domain status tracking, fingerprint comparison, and failure root cause analysis.
 */
class VerificationDeepDiveViewModel {
    private val viewModelScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(VerificationDeepDiveUiState())
    val uiState: StateFlow<VerificationDeepDiveUiState> = _uiState.asStateFlow()

    /**
     * Update package name input
     */
    fun updatePackageName(packageName: String) {
        _uiState.update { it.copy(packageName = packageName, error = null) }
    }

    /**
     * Set the selected device for analysis
     */
    fun selectDevice(device: Device?) {
        _uiState.update { it.copy(selectedDevice = device, error = null) }
    }

    /**
     * Run deep verification analysis
     */
    fun analyzeVerification() {
        val device = _uiState.value.selectedDevice
        val packageName = _uiState.value.packageName.trim()

        if (device == null) {
            _uiState.update { it.copy(error = "Please select a device first") }
            return
        }

        if (packageName.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a package name") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, diagnostics = null) }

            AppContainer.analyzeVerificationUseCase(device.serialNumber, packageName)
                .onSuccess { diagnostics ->
                    _uiState.update {
                        it.copy(isLoading = false, diagnostics = diagnostics)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "Analysis failed"
                        )
                    }
                }
        }
    }

    /**
     * Clear current diagnostics result
     */
    fun clearResult() {
        _uiState.update { it.copy(diagnostics = null, error = null) }
    }

    /**
     * Cleanup resources
     */
    fun onCleared() {
        viewModelScope.cancel()
    }
}

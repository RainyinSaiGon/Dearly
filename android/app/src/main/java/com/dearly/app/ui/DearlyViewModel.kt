package com.dearly.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dearly.app.data.repository.AuthRepository
import com.dearly.app.data.repository.ContactRepository
import com.dearly.app.data.repository.MedicationRepository
import com.dearly.app.domain.model.Contact
import com.dearly.app.domain.model.MedicationLog
import com.dearly.app.domain.model.NewContact
import com.dearly.app.domain.model.NewMedication
import com.dearly.app.domain.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject

data class AppUiState(
    val busy: Boolean = false,
    val error: String? = null,
    val elderId: String? = null
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DearlyViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val contactRepository: ContactRepository,
    private val medicationRepository: MedicationRepository
) : ViewModel() {
    private val selectedElderId = MutableStateFlow(authRepository.userId())
    private val _uiState = MutableStateFlow(AppUiState(elderId = selectedElderId.value))
    val uiState: StateFlow<AppUiState> = _uiState

    val contacts: StateFlow<List<Contact>> = selectedElderId
        .flatMapLatest { elderId -> elderId?.let(contactRepository::observe) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val medicationLogs: StateFlow<List<MedicationLog>> = selectedElderId
        .flatMapLatest { elderId -> elderId?.let(medicationRepository::observeTodayLogs) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun createSession(role: UserRole, onSuccess: (UserRole) -> Unit) {
        launch {
            val user = authRepository.createBackendSession(role)
            val actualRole = UserRole.valueOf(user.role)
            val elderId = if (actualRole == UserRole.ELDER) user.id else authRepository.elders().firstOrNull()?.id
            selectedElderId.value = elderId
            _uiState.value = AppUiState(elderId = elderId)
            onSuccess(actualRole)
        }
    }

    fun refreshContacts() = launch { contactRepository.refresh(selectedElderId.value) }
    fun refreshMedications() = launch { medicationRepository.refresh(selectedElderId.value) }

    fun addContact(contact: NewContact, onSuccess: () -> Unit) = launch {
        requireElder()
        contactRepository.create(selectedElderId.value, contact)
        onSuccess()
    }

    fun addMedication(medication: NewMedication, onSuccess: () -> Unit) = launch {
        requireElder()
        medicationRepository.create(selectedElderId.value, medication)
        onSuccess()
    }

    fun markTaken(log: MedicationLog) = launch {
        medicationRepository.markTaken(selectedElderId.value, log)
    }

    fun signOut(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            authRepository.signOut()
            selectedElderId.value = null
            _uiState.value = AppUiState()
            onSuccess()
        }
    }

    private fun requireElder() {
        checkNotNull(selectedElderId.value) { "No elder is linked to this caregiver account" }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null)
            runCatching { block() }
                .onSuccess { _uiState.value = _uiState.value.copy(busy = false) }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        error = error.message ?: "Request failed"
                    )
                }
        }
    }
}

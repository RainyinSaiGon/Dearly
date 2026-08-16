package com.dearly.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dearly.app.data.repository.AuthRepository
import com.dearly.app.data.repository.ContactRepository
import com.dearly.app.data.repository.MedicationRepository
import com.dearly.app.data.repository.VoiceRepository
import com.dearly.app.domain.model.Contact
import com.dearly.app.domain.model.Medication
import com.dearly.app.domain.model.MedicationLog
import com.dearly.app.domain.model.NewContact
import com.dearly.app.domain.model.NewMedication
import com.dearly.app.domain.model.UserRole
import java.io.File
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
    val elderId: String? = null,
    val linkCode: String? = null,
    val linkCodeExpiresAt: String? = null,
    val linkMessage: String? = null,
    val sessionRole: UserRole? = null,
    val voiceTranscript: String? = null,
    val voiceMessage: String? = null,
    val voiceRequiresVerification: Boolean = false,
    val voiceEnrollmentCount: Int = 0
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DearlyViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val contactRepository: ContactRepository,
    private val medicationRepository: MedicationRepository,
    private val voiceRepository: VoiceRepository
) : ViewModel() {
    private val selectedElderId = MutableStateFlow(
        authRepository.userId().takeIf { authRepository.role() == UserRole.ELDER.name }
    )
    private val restoredRole = authRepository.role()?.let { runCatching { UserRole.valueOf(it) }.getOrNull() }
    private val _uiState = MutableStateFlow(
        AppUiState(elderId = selectedElderId.value, sessionRole = restoredRole)
    )
    val uiState: StateFlow<AppUiState> = _uiState

    val contacts: StateFlow<List<Contact>> = selectedElderId
        .flatMapLatest { elderId -> elderId?.let(contactRepository::observe) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val medicationLogs: StateFlow<List<MedicationLog>> = selectedElderId
        .flatMapLatest { elderId -> elderId?.let(medicationRepository::observeTodayLogs) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val medications: StateFlow<List<Medication>> = selectedElderId
        .flatMapLatest { elderId -> elderId?.let(medicationRepository::observeMedications) ?: flowOf(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (authRepository.role() == UserRole.CAREGIVER.name) {
            launch {
                val elderId = authRepository.elders().firstOrNull()?.id
                selectedElderId.value = elderId
                _uiState.value = _uiState.value.copy(elderId = elderId)
                if (elderId != null) {
                    contactRepository.refresh(elderId)
                    medicationRepository.refresh(elderId)
                }
            }
        }
    }

    fun createSession(role: UserRole, onSuccess: (UserRole) -> Unit) {
        launch {
            val user = authRepository.createBackendSession(role)
            val actualRole = UserRole.valueOf(user.role)
            val elderId = if (actualRole == UserRole.ELDER) user.id else authRepository.elders().firstOrNull()?.id
            selectedElderId.value = elderId
            _uiState.value = AppUiState(elderId = elderId, sessionRole = actualRole)
            onSuccess(actualRole)
        }
    }

    fun generateElderLinkCode() = launch {
        val result = authRepository.createElderLinkCode()
        _uiState.value = _uiState.value.copy(
            linkCode = result.code,
            linkCodeExpiresAt = result.expiresAt,
            linkMessage = null
        )
    }

    fun linkElder(code: String) = launch {
        require(code.isNotBlank()) { "Enter the elder's link code" }
        val elder = authRepository.linkElder(code)
        selectedElderId.value = elder.id
        _uiState.value = _uiState.value.copy(
            elderId = elder.id,
            linkMessage = "Elder linked successfully"
        )
        contactRepository.refresh(elder.id)
        medicationRepository.refresh(elder.id)
    }

    fun refreshContacts() = launch { contactRepository.refresh(selectedElderId.value) }
    fun refreshMedications() = launch { medicationRepository.refresh(selectedElderId.value) }

    fun addContact(contact: NewContact, onSuccess: () -> Unit) = launch {
        requireElder()
        contactRepository.create(selectedElderId.value, contact)
        onSuccess()
    }

    fun updateContact(contact: Contact, onSuccess: () -> Unit) = launch {
        requireElder()
        contactRepository.update(selectedElderId.value, contact)
        onSuccess()
    }

    fun deleteContact(contactId: String, onSuccess: () -> Unit) = launch {
        requireElder()
        contactRepository.delete(selectedElderId.value, contactId)
        onSuccess()
    }

    fun addMedication(medication: NewMedication, onSuccess: () -> Unit) = launch {
        requireElder()
        medicationRepository.create(selectedElderId.value, medication)
        onSuccess()
    }


    fun updateMedication(medicationId: String, medication: NewMedication, onSuccess: () -> Unit) = launch {
        requireElder()
        medicationRepository.update(selectedElderId.value, medicationId, medication)
        onSuccess()
    }

    fun deleteMedication(medicationId: String, onSuccess: () -> Unit) = launch {
        requireElder()
        medicationRepository.delete(selectedElderId.value, medicationId)
        onSuccess()
    }

    fun queryVoice(audio: File) = launch {
        try {
            val result = voiceRepository.query(audio)
            _uiState.value = _uiState.value.copy(
                voiceTranscript = result.transcript,
                voiceMessage = result.responseText,
                voiceRequiresVerification = result.svRequired
            )
        } finally {
            audio.delete()
        }
    }

    fun verifyAndMarkTaken(log: MedicationLog, audio: File, onSuccess: () -> Unit) = launch {
        val verification = try {
            voiceRepository.verify(audio, "MARK_TAKEN")
        } finally {
            audio.delete()
        }
        check(verification.passed) { "Speaker verification failed" }
        val grant = checkNotNull(verification.verificationGrant) {
            "Verification did not return an authorization grant"
        }
        medicationRepository.markTaken(selectedElderId.value, log, grant)
        _uiState.value = _uiState.value.copy(
            voiceMessage = "Đã xác nhận liều thuốc.",
            voiceRequiresVerification = false
        )
        onSuccess()
    }

    fun enrollVoice(audio: File) = launch {
        val phraseIndex = _uiState.value.voiceEnrollmentCount.coerceIn(0, 4)
        val result = try {
            voiceRepository.enroll(audio, phraseIndex)
        } finally {
            audio.delete()
        }
        _uiState.value = _uiState.value.copy(
            voiceEnrollmentCount = result.enrolledCount,
            voiceMessage = if (result.enrolledCount == result.totalRequired) {
                "Đăng ký giọng nói hoàn tất."
            } else {
                "Đã lưu câu ${result.enrolledCount}/${result.totalRequired}."
            }
        )
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

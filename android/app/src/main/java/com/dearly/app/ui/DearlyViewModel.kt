package com.dearly.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dearly.app.data.repository.AuthRepository
import com.dearly.app.data.repository.ContactRepository
import com.dearly.app.data.repository.MedicationRepository
import com.dearly.app.data.repository.VoiceRepository
import com.dearly.app.data.remote.VoicePersonalizationDto
import com.dearly.app.data.remote.UserDto
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
import retrofit2.HttpException
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
    val voicePersonalization: VoicePersonalizationDto? = null,
    val voiceRequiresVerification: Boolean = false,
    val pendingMedicationLog: MedicationLog? = null,
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
    private val _linkedElders = MutableStateFlow<List<UserDto>>(emptyList())
    val linkedElders: StateFlow<List<UserDto>> = _linkedElders

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
                val elders = authRepository.elders()
                _linkedElders.value = elders
                val elderId = elders.firstOrNull()?.id
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
            onSuccess(activateSession(user))
        }
    }

    fun resumeBackendSession(
        onExistingAccount: (UserRole) -> Unit,
        onRoleSelectionRequired: () -> Unit,
        onFailure: () -> Unit
    ) {
        viewModelScope.launch {
            try {
                onExistingAccount(activateSession(authRepository.resumeBackendSession()))
            } catch (error: HttpException) {
                if (error.code() == 409) onRoleSelectionRequired() else onFailure()
            } catch (_: Exception) {
                onFailure()
            }
        }
    }

    private suspend fun activateSession(user: UserDto): UserRole {
        val actualRole = UserRole.valueOf(user.role)
        val elders = if (actualRole == UserRole.CAREGIVER) authRepository.elders() else emptyList()
        _linkedElders.value = elders
        val elderId = if (actualRole == UserRole.ELDER) user.id else elders.firstOrNull()?.id
        selectedElderId.value = elderId
        _uiState.value = AppUiState(elderId = elderId, sessionRole = actualRole)
        return actualRole
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
        _linkedElders.value = authRepository.elders()
        selectedElderId.value = elder.id
        _uiState.value = _uiState.value.copy(
            elderId = elder.id,
            linkMessage = "Đã kết nối với ${elder.name}."
        )
        contactRepository.refresh(elder.id)
        medicationRepository.refresh(elder.id)
    }

    fun unlinkElder(elderId: String) = launch {
        val elderName = _linkedElders.value.firstOrNull { it.id == elderId }?.name ?: "người thân"
        authRepository.unlinkElder(elderId)
        val remainingElders = authRepository.elders()
        _linkedElders.value = remainingElders
        val selectedId = remainingElders.firstOrNull()?.id
        selectedElderId.value = selectedId
        _uiState.value = _uiState.value.copy(
            elderId = selectedId,
            linkMessage = "Đã ngắt kết nối với $elderName."
        )
        if (selectedId != null) {
            contactRepository.refresh(selectedId)
            medicationRepository.refresh(selectedId)
        }
    }

    fun refreshContacts() {
        val elderId = selectedElderId.value ?: run {
            clearMissingElderError()
            return
        }
        launch { contactRepository.refresh(elderId) }
    }

    fun refreshMedications() {
        val elderId = selectedElderId.value ?: run {
            clearMissingElderError()
            return
        }
        launch { medicationRepository.refresh(elderId) }
    }

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
            val query = voiceRepository.queryRaw(audio)
            if (query.intent == "MARK_TAKEN") {
                markTakenFromSpeech(query)
            } else {
                _uiState.value = _uiState.value.copy(
                    voiceTranscript = query.transcript,
                    voiceMessage = query.responseText,
                    voicePersonalization = query.personalization,
                    voiceRequiresVerification = false,
                    pendingMedicationLog = null,
                    error = voiceRepository.speak(query.responseText, query.personalization?.speechRate)
                )
            }
        } catch (error: HttpException) {
            if (error.code() == 422) {
                _uiState.value = _uiState.value.copy(
                    error = "Dearly chưa nghe rõ giọng nói. Hãy kiểm tra micro rồi nói gần hơn."
                )
            } else {
                throw error
            }
        } finally {
            audio.delete()
        }
    }

    private suspend fun markTakenFromSpeech(query: com.dearly.app.data.remote.VoiceQueryDto) {
        val requestedName = query.entities["medication_name"]?.toString()?.trim().orEmpty()
        val matchingLog = medicationLogs.value.firstOrNull { log ->
            log.status != com.dearly.app.domain.model.DoseStatus.TAKEN &&
                medicationNamesMatch(requestedName, log.medicationName)
        }
        if (requestedName.isBlank() || matchingLog == null) {
            val message = if (requestedName.isBlank()) {
                "Bác hãy nói rõ tên thuốc, ví dụ: Tôi đã uống thuốc Amlodipine rồi."
            } else {
                "Dearly chưa tìm thấy liều thuốc $requestedName chưa uống hôm nay."
            }
            _uiState.value = _uiState.value.copy(
                voiceTranscript = query.transcript,
                voiceMessage = message,
                voiceRequiresVerification = false,
                pendingMedicationLog = null,
                error = voiceRepository.speak(message)
            )
            return
        }
        val message = medicationConfirmationPrompt(matchingLog)
        _uiState.value = _uiState.value.copy(
            voiceTranscript = query.transcript,
            voiceMessage = message,
            voiceRequiresVerification = true,
            pendingMedicationLog = matchingLog,
            error = voiceRepository.speak(message)
        )
    }

    fun verifySpokenMedication(audio: File) = launch {
        val log = checkNotNull(_uiState.value.pendingMedicationLog) { "Không có liều thuốc chờ xác minh." }
        val verification = try {
            val confirmation = voiceRepository.queryRaw(audio)
            if (!isMedicationConfirmation(confirmation.transcript)) {
                val message = "Bác hãy nói ‘Đúng rồi’ nếu bác muốn xác nhận đã uống ${log.medicationName} lúc ${log.scheduledTime}."
                _uiState.value = _uiState.value.copy(
                    voiceTranscript = confirmation.transcript,
                    voiceMessage = message,
                    voiceRequiresVerification = true,
                    error = voiceRepository.speak(message)
                )
                return@launch
            }
            voiceRepository.verify(audio, "MARK_TAKEN")
        } catch (error: HttpException) {
            val message = when (error.code()) {
                409 -> "Bản ghi này đã được dùng để xác minh. Bác hãy nói lại ‘Đúng rồi’ nhé."
                429 -> "Dearly tạm dừng xác minh để bảo vệ tài khoản. Bác hãy thử lại sau 15 phút nhé."
                422 -> "Dearly chưa nghe rõ. Bác hãy nói ‘Đúng rồi’ gần micro hơn nhé."
                else -> throw error
            }
            _uiState.value = _uiState.value.copy(
                voiceMessage = message,
                voiceRequiresVerification = true,
                error = voiceRepository.speak(message)
            )
            return@launch
        } finally {
            audio.delete()
        }
        if (!verification.passed || verification.verificationGrant.isNullOrBlank()) {
            _uiState.value = _uiState.value.copy(
                voiceMessage = "Dearly chưa xác minh được giọng nói. Bác hãy nói ‘Đúng rồi’ thêm một lần nhé.",
                voiceRequiresVerification = true,
                error = null
            )
            return@launch
        }
        medicationRepository.markTaken(selectedElderId.value, log, verification.verificationGrant)
        val message = "Đã xác nhận ${log.medicationName} đã uống."
        _uiState.value = _uiState.value.copy(
            voiceMessage = message,
            voiceRequiresVerification = false,
            pendingMedicationLog = null,
            error = voiceRepository.speak(message)
        )
    }

    private fun isMedicationConfirmation(transcript: String): Boolean {
        val normalized = transcript.lowercase()
            .replace('đ', 'd')
            .filter(Char::isLetterOrDigit)
        return listOf("dungroi", "xacnhan", "dongy").any(normalized::contains)
    }

    private fun medicationConfirmationPrompt(log: MedicationLog): String =
        "Dearly đã nghe bác nói đã uống ${log.medicationName}. " +
            "Bác xác nhận đã uống ${log.medicationName} lúc ${log.scheduledTime} phải không? " +
            "Bác hãy nói ‘Đúng rồi’ để xác nhận nhé."

    private fun medicationNamesMatch(requested: String, actual: String): Boolean {
        val normalizedRequested = requested.lowercase().filter(Char::isLetterOrDigit)
        val normalizedActual = actual.lowercase().filter(Char::isLetterOrDigit)
        return normalizedRequested.isNotBlank() &&
            (normalizedActual.contains(normalizedRequested) || normalizedRequested.contains(normalizedActual))
    }

    fun queryPublicVoice(audio: File) = launch {
        try {
            val result = voiceRepository.queryPublic(audio)
            _uiState.value = _uiState.value.copy(
                voiceTranscript = result.query.transcript,
                voiceMessage = result.query.responseText,
                voiceRequiresVerification = false,
                error = result.speechWarning
            )
        } catch (error: HttpException) {
            if (error.code() == 422) {
                _uiState.value = _uiState.value.copy(
                    error = "Dearly chưa nghe rõ giọng nói. Bác hãy kiểm tra micro rồi nói gần hơn nhé."
                )
            } else {
                throw error
            }
        } finally {
            audio.delete()
        }
    }

    fun beginVoiceCapture() {
        _uiState.value = _uiState.value.copy(
            error = null,
            voiceTranscript = null,
            voiceMessage = null,
            voicePersonalization = null,
            voiceRequiresVerification = false,
            pendingMedicationLog = null
        )
    }

    fun verifyAndMarkTaken(log: MedicationLog, audio: File, onSuccess: () -> Unit) = launch {
        val verification = try {
            val confirmation = voiceRepository.queryRaw(audio)
            if (!isMedicationConfirmation(confirmation.transcript)) {
                _uiState.value = _uiState.value.copy(
                    voiceMessage = "Bác hãy nói ‘Đúng rồi’ để xác nhận đã uống ${log.medicationName} lúc ${log.scheduledTime}.",
                    error = voiceRepository.speak("Bác hãy nói Đúng rồi để xác nhận đã uống thuốc lúc ${log.scheduledTime}.")
                )
                return@launch
            }
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
        } catch (error: HttpException) {
            val message = when (error.code()) {
                422 -> "Bản ghi chưa đủ rõ. Bác hãy ghi lại câu ${phraseIndex + 1}/5 ở nơi yên tĩnh và nói gần micro hơn nhé."
                502, 503 -> "Dearly chưa thể xử lý bản ghi lúc này. Bác hãy thử lại sau ít phút nhé."
                else -> throw error
            }
            _uiState.value = _uiState.value.copy(voiceMessage = message)
            return@launch
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
        checkNotNull(selectedElderId.value) {
            "Hãy kết nối người được chăm sóc trong Cài đặt trước."
        }
    }

    private fun clearMissingElderError() {
        _uiState.value = _uiState.value.copy(error = null)
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

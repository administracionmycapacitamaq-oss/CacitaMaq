package com.example.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.CertificateRepository
import com.example.data.model.Certificate
import com.example.data.model.StudentProfile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class StudentPortalViewModel(
    private val repository: CertificateRepository,
    private val currentUserId: String,
    private val studentEmail: String
) : ViewModel() {

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    val verifierBaseUrl: StateFlow<String> = repository.observeVerifierConfig()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = ""
        )

    val profileState: StateFlow<UiState<StudentProfile?>> =
        repository.observeMyStudentProfile(currentUserId)
            .map<StudentProfile?, UiState<StudentProfile?>> { UiState.Success(it) }
            .catch { err ->
                emit(UiState.Error(err.localizedMessage ?: "No se pudo cargar tu perfil de alumno."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    val myCertificatesState: StateFlow<UiState<List<Certificate>>> =
        profileState
            .flatMapLatest { pState ->
                when (pState) {
                    is UiState.Loading -> flowOf(UiState.Loading)
                    is UiState.Error -> flowOf(UiState.Error(pState.message))
                    is UiState.Success -> {
                        val profile = pState.data
                        if (profile?.verificationStatus == "approved") {
                            repository.observeStudentApprovedCertificates(studentEmail)
                                .map<List<Certificate>, UiState<List<Certificate>>> { UiState.Success(it) }
                                .catch { err ->
                                    emit(
                                        UiState.Error(
                                            err.localizedMessage ?: "No se pudieron cargar tus certificaciones."
                                        )
                                    )
                                }
                        } else {
                            flowOf(UiState.Success(emptyList()))
                        }
                    }
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000L),
                initialValue = UiState.Loading
            )

    fun submitVerificationRequest(
        fullName: String,
        rut: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            _isSubmitting.value = true
            repository.submitStudentProfile(fullName, rut).fold(
                onSuccess = {
                    _isSubmitting.value = false
                    onSuccess()
                },
                onFailure = { err ->
                    _isSubmitting.value = false
                    onError(err.localizedMessage ?: "No se pudo enviar tu solicitud.")
                }
            )
        }
    }
}

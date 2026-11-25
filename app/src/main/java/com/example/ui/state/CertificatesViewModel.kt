package com.example.ui.state

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.CertificateRepository
import com.example.data.firebase.VerifierHostStatus
import com.example.data.model.Certificate
import com.example.data.model.CertificateDraft
import com.example.data.model.ComputedStatus
import com.example.data.model.RutUtils
import com.example.data.model.SortOrder
import com.example.data.model.StatusFilter
import com.example.data.model.StudentProfile
import com.example.util.AdminPresetStore
import com.example.util.DateFormatter
import com.example.util.QrCodecService
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class CertificateStats(
    val total: Int = 0,
    val valid: Int = 0,
    val pending: Int = 0,
    val expired: Int = 0,
    val invalid: Int = 0,
    val totalScans: Long = 0L
)

data class DashboardState(
    val allRecords: List<Certificate> = emptyList(),
    val filteredRecords: List<Certificate> = emptyList(),
    val totalMatching: Int = 0,
    val stats: CertificateStats = CertificateStats(),
    val searchQuery: String = "",
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val sortOrder: SortOrder = SortOrder.NEWEST,
    val pageLimit: Int = 10
)

sealed interface VerifyOutcome {
    data object Idle : VerifyOutcome
    data class Found(
        val certificate: Certificate,
        val isLiveCloudVerified: Boolean = true
    ) : VerifyOutcome
    data class NotFound(val searchedCode: String) : VerifyOutcome
}

class CertificatesViewModel(
    private val appContext: Context,
    private val repository: CertificateRepository,
    private val currentUserId: String
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _statusFilter = MutableStateFlow(StatusFilter.ALL)
    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    private val _pageLimit = MutableStateFlow(10)

    private val _editorDraft = MutableStateFlow(AdminPresetStore.buildNewDraft(appContext))
    val editorDraft: StateFlow<CertificateDraft> = _editorDraft.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _verifyOutcome = MutableStateFlow<VerifyOutcome>(VerifyOutcome.Idle)
    val verifyOutcome: StateFlow<VerifyOutcome> = _verifyOutcome.asStateFlow()

    private val _verifyInput = MutableStateFlow("")
    val verifyInput: StateFlow<String> = _verifyInput.asStateFlow()

    private val _verifierRefreshTick = MutableStateFlow(0)
    private var didSyncPublicIndex = false
    private var verifyJob: Job? = null
    private var lastCountedVerifyCode: String = ""

    init {
        viewModelScope.launch {
            repository.migrateLegacyCertificatesIfNeeded(currentUserId)
        }
    }

    val hasSavedPreset: Boolean
        get() = AdminPresetStore.hasSavedPreset(appContext)

    val verifierHostStatus: StateFlow<VerifierHostStatus> =
        repository.observeVerifierHostStatus(_verifierRefreshTick)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = VerifierHostStatus()
            )

    val verifierBaseUrl: StateFlow<String> =
        verifierHostStatus
            .map { it.effectiveUrl }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = ""
            )

    val studentProfilesState: StateFlow<UiState<List<StudentProfile>>> =
        repository.observeAllStudentProfiles()
            .map<List<StudentProfile>, UiState<List<StudentProfile>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit(UiState.Error(error.localizedMessage ?: "No se pudieron cargar las solicitudes de alumnos."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    val pendingStudentsCount: StateFlow<Int> = studentProfilesState
        .map { state ->
            (state as? UiState.Success)?.data?.count { it.verificationStatus == "pending" } ?: 0
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0
        )

    val allRecordsState: StateFlow<UiState<List<Certificate>>> =
        repository.observeAllCertificates()
            .onEach { list ->
                if (!didSyncPublicIndex && list.isNotEmpty()) {
                    didSyncPublicIndex = true
                    viewModelScope.launch {
                        repository.syncAllPublicCertificates(list)
                    }
                }
            }
            .map<List<Certificate>, UiState<List<Certificate>>> { UiState.Success(it) }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val rootError = firestoreError ?: error
                Log.w(TAG, "Error observing certificates", rootError)
                emit(UiState.Error(rootError.localizedMessage ?: "No se pudieron cargar las certificaciones."))
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = UiState.Loading
            )

    private data class FilterParams(
        val query: String,
        val status: StatusFilter,
        val sort: SortOrder,
        val limit: Int
    )

    private val filterParamsFlow = combine(
        _searchQuery,
        _statusFilter,
        _sortOrder,
        _pageLimit
    ) { q, st, so, lim ->
        FilterParams(q, st, so, lim)
    }

    val dashboardState: StateFlow<UiState<DashboardState>> = combine(
        allRecordsState,
        filterParamsFlow
    ) { recordsState, params ->
        when (recordsState) {
            is UiState.Loading -> UiState.Loading
            is UiState.Error -> UiState.Error(recordsState.message)
            is UiState.Success -> {
                val all = recordsState.data
                val today = Certificate.todayIsoDate()
                var validCount = 0
                var pendingCount = 0
                var expiredCount = 0
                var invalidCount = 0
                var scanSum = 0L

                all.forEach { cert ->
                    scanSum += cert.scanCount
                    when (cert.computedStatus(today)) {
                        ComputedStatus.VALID -> validCount++
                        ComputedStatus.PENDING -> pendingCount++
                        ComputedStatus.EXPIRED -> expiredCount++
                        ComputedStatus.INVALID -> invalidCount++
                    }
                }

                val q = params.query.trim().lowercase()
                val filtered = all.filter { cert ->
                    val matchesStatus = when (params.status) {
                        StatusFilter.ALL -> true
                        StatusFilter.VALID -> cert.computedStatus(today) == ComputedStatus.VALID
                        StatusFilter.PENDING -> cert.computedStatus(today) == ComputedStatus.PENDING
                        StatusFilter.EXPIRED -> cert.computedStatus(today) == ComputedStatus.EXPIRED
                        StatusFilter.INVALID -> cert.computedStatus(today) == ComputedStatus.INVALID
                    }
                    val matchesQuery = q.isEmpty() || listOf(
                        cert.studentName,
                        cert.studentRut,
                        cert.studentEmail,
                        cert.operatorName,
                        cert.courseName,
                        cert.certificateCode,
                        cert.equipment
                    ).any { it.lowercase().contains(q) }
                    matchesStatus && matchesQuery
                }.let { list ->
                    when (params.sort) {
                        SortOrder.NEWEST -> list.sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
                        SortOrder.OLDEST -> list.sortedBy { it.createdAt ?: Timestamp(0, 0) }
                        SortOrder.NAME_AZ -> list.sortedBy { it.studentName.lowercase() }
                    }
                }

                UiState.Success(
                    DashboardState(
                        allRecords = all,
                        filteredRecords = filtered.take(params.limit),
                        totalMatching = filtered.size,
                        stats = CertificateStats(
                            total = all.size,
                            valid = validCount,
                            pending = pendingCount,
                            expired = expiredCount,
                            invalid = invalidCount,
                            totalScans = scanSum
                        ),
                        searchQuery = params.query,
                        statusFilter = params.status,
                        sortOrder = params.sort,
                        pageLimit = params.limit
                    )
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = UiState.Loading
    )

    fun setSearchQuery(value: String) {
        _searchQuery.value = value
    }

    fun setStatusFilter(filter: StatusFilter) {
        _statusFilter.value = filter
    }

    fun setSortOrder(sort: SortOrder) {
        _sortOrder.value = sort
    }

    fun setPageLimit(limit: Int) {
        _pageLimit.value = limit
    }

    fun startNewCertificate() {
        val latestCert = (allRecordsState.value as? UiState.Success)
            ?.data
            ?.maxByOrNull { it.createdAt?.seconds ?: 0L }
        _editorDraft.value = AdminPresetStore.buildNewDraft(appContext, latestCert)
    }

    fun startNewCertificateForStudent(profile: StudentProfile) {
        val latestCert = (allRecordsState.value as? UiState.Success)
            ?.data
            ?.maxByOrNull { it.createdAt?.seconds ?: 0L }
        _editorDraft.value = AdminPresetStore.buildNewDraft(appContext, latestCert).copy(
            studentName = profile.fullName.trim(),
            studentRut = RutUtils.format(profile.rut),
            studentEmail = profile.email.trim().lowercase()
        )
    }

    fun duplicateCertificateForNewStudent(source: Certificate) {
        val today = Certificate.todayIsoDate()
        _editorDraft.value = source.toDraft().copy(
            editingId = null,
            existingCertificateCode = "",
            studentName = "",
            studentRut = "",
            studentEmail = "",
            issuedAt = today,
            expiresAt = DateFormatter.addOneYearIso(today),
            certificateCode = DateFormatter.generateCertificateCode(),
            status = "active"
        )
    }

    fun startEditing(certificate: Certificate) {
        _editorDraft.value = certificate.toDraft()
    }

    fun updateDraft(transform: (CertificateDraft) -> CertificateDraft) {
        _editorDraft.update(transform)
    }

    fun findByCode(code: String): Certificate? {
        val state = allRecordsState.value as? UiState.Success ?: return null
        return state.data.firstOrNull {
            it.certificateCode.equals(code.trim(), ignoreCase = true)
        }
    }

    fun setVerifyInput(value: String) {
        _verifyInput.value = value
    }

    fun verifyCodeOrUrl(rawInput: String, incrementScan: Boolean = true) {
        val code = QrCodecService.extractCertificateCode(rawInput)
        _verifyInput.value = code
        verifyJob?.cancel()
        if (code.isBlank()) {
            lastCountedVerifyCode = ""
            _verifyOutcome.value = VerifyOutcome.Idle
            return
        }

        verifyJob = viewModelScope.launch {
            combine(
                allRecordsState,
                repository.observePublicCertificateByCode(code)
            ) { recordsState, publicMatch ->
                recordsState to publicMatch
            }.collect { (state, publicMatch) ->
                val localMatch = (state as? UiState.Success)?.data?.firstOrNull {
                    it.certificateCode.equals(code, ignoreCase = true)
                }

                if (localMatch != null) {
                    _verifyOutcome.value = VerifyOutcome.Found(
                        certificate = localMatch,
                        isLiveCloudVerified = localMatch.status == "active"
                    )
                    if (incrementScan && localMatch.id.isNotBlank() && !lastCountedVerifyCode.equals(code, ignoreCase = true)) {
                        lastCountedVerifyCode = code
                        repository.incrementScanCount(localMatch)
                    }
                } else if (publicMatch != null) {
                    _verifyOutcome.value = VerifyOutcome.Found(
                        certificate = publicMatch,
                        isLiveCloudVerified = true
                    )
                } else if (state !is UiState.Success) {
                    val decoded = QrCodecService.decodeVerificationPayload(rawInput)?.takeIf {
                        it.certificateCode.equals(code, ignoreCase = true)
                    }
                    _verifyOutcome.value = if (decoded != null) {
                        VerifyOutcome.Found(decoded, isLiveCloudVerified = false)
                    } else {
                        VerifyOutcome.NotFound(code)
                    }
                } else {
                    _verifyOutcome.value = VerifyOutcome.NotFound(code)
                }
            }
        }
    }

    fun saveDraft(
        onSuccess: (Certificate, Boolean) -> Unit,
        onError: (String) -> Unit
    ) {
        if (_isSaving.value) return
        val draft = _editorDraft.value
        val existingList = (allRecordsState.value as? UiState.Success)?.data.orEmpty()

        if (draft.courseName.trim().isEmpty()) {
            onError("Ingresa el título de la certificación.")
            return
        }
        if (draft.studentName.trim().isEmpty()) {
            onError("Ingresa el nombre completo del alumno.")
            return
        }
        if (draft.studentRut.isNotBlank() && !RutUtils.isPlausibleRut(draft.studentRut)) {
            onError("El RUT ingresado no es válido (ej. 12.345.678-9).")
            return
        }
        if (draft.issuer.trim().isEmpty()) {
            onError("Ingresa la entidad certificadora.")
            return
        }
        if (draft.issuedAt.trim().isEmpty()) {
            onError("Selecciona la fecha de emisión.")
            return
        }
        if (draft.expiresAt.isNotBlank() && draft.expiresAt.trim() < draft.issuedAt.trim()) {
            onError("La fecha de vencimiento no puede ser anterior a la emisión.")
            return
        }

        val existingRecord = draft.editingId?.let { id ->
            existingList.firstOrNull { it.id == id }
        }
        if (draft.editingId != null && existingRecord == null) {
            onError("Este certificado ya no está disponible.")
            return
        }
        if (existingRecord == null && existingList.size >= 999) {
            onError("Se alcanzó el límite de 999 certificaciones.")
            return
        }

        val rawCode = draft.certificateCode.trim()
            .ifBlank { existingRecord?.certificateCode.orEmpty() }
            .ifBlank { DateFormatter.generateCertificateCode() }
        val resolvedCode = DateFormatter.sanitizeCertificateCode(rawCode)

        val duplicate = existingList.any {
            it.certificateCode.equals(resolvedCode, ignoreCase = true) && it.id != draft.editingId
        }
        if (duplicate) {
            onError("Ese identificador público ya pertenece a otra certificación.")
            return
        }

        val draftToSave = if (draft.studentEmail.isBlank() && draft.studentRut.isNotBlank()) {
            val cleanRut = RutUtils.clean(draft.studentRut)
            val matchedProfile = (studentProfilesState.value as? UiState.Success)
                ?.data
                ?.firstOrNull { RutUtils.clean(it.rut) == cleanRut && it.email.isNotBlank() }
            if (matchedProfile != null) {
                draft.copy(studentEmail = matchedProfile.email.trim().lowercase())
            } else {
                draft
            }
        } else {
            draft
        }

        viewModelScope.launch {
            _isSaving.value = true
            val result = if (existingRecord != null) {
                repository.updateCertificate(existingRecord, draftToSave, resolvedCode)
            } else {
                repository.createCertificate(draftToSave, resolvedCode)
            }
            _isSaving.value = false
            result.fold(
                onSuccess = { saved ->
                    AdminPresetStore.saveFromDraft(appContext, draftToSave)
                    onSuccess(saved, existingRecord != null)
                },
                onFailure = { err ->
                    onError(err.localizedMessage ?: "No se pudo confirmar el guardado en la nube.")
                }
            )
        }
    }

    fun togglePublishStatus(
        certificate: Certificate,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.togglePublishStatus(certificate)
            result.fold(
                onSuccess = { nextStatus ->
                    val msg = if (nextStatus == "active") {
                        "Certificación publicada en vivo por administrador."
                    } else {
                        "Certificación cambiada a pendiente (retirada del verificador público)."
                    }
                    onResult(msg)
                },
                onFailure = { err ->
                    onResult(err.localizedMessage ?: "No se pudo actualizar la publicación.")
                }
            )
        }
    }

    fun deleteCertificate(
        certificate: Certificate,
        onDeleted: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.deleteCertificate(certificate)
            result.fold(
                onSuccess = { onDeleted() },
                onFailure = { err ->
                    onError(err.localizedMessage ?: "No se pudo eliminar la certificación.")
                }
            )
        }
    }

    fun approveStudentProfile(
        profile: StudentProfile,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            val certs = (allRecordsState.value as? UiState.Success)?.data.orEmpty()
            repository.approveStudentProfile(profile, certs).fold(
                onSuccess = { linked ->
                    val suffix = if (linked > 0) " ($linked certificado(s) vinculados)" else ""
                    onResult("Alumno ${profile.fullName} comprobado y aprobado$suffix.")
                },
                onFailure = { err ->
                    onResult(err.localizedMessage ?: "No se pudo aprobar al alumno.")
                }
            )
        }
    }

    fun rejectStudentProfile(
        profile: StudentProfile,
        reason: String,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.rejectStudentProfile(profile, reason).fold(
                onSuccess = { onResult("Solicitud de ${profile.fullName} marcada como rechazada.") },
                onFailure = { err -> onResult(err.localizedMessage ?: "No se pudo rechazar la solicitud.") }
            )
        }
    }

    fun recheckVerifierHost() {
        _verifierRefreshTick.value += 1
    }

    fun saveVerifierBaseUrl(
        url: String,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch {
            repository.saveVerifierBaseUrl(url).fold(
                onSuccess = { saved ->
                    _verifierRefreshTick.value += 1
                    if (saved.isBlank()) {
                        onResult("URL web limpiada. Todos los enlaces funcionan ahora con la ficha verificable en vivo.")
                    } else {
                        onResult("Firebase Hosting verificado en vivo y activado para todos los alumnos y QR.")
                    }
                },
                onFailure = { err ->
                    onResult(err.localizedMessage ?: "No se pudo validar o guardar la URL del verificador.")
                }
            )
        }
    }

    private companion object {
        const val TAG = "CertificatesVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}

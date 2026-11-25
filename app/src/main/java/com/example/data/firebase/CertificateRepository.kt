package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.AdminConfig
import com.example.data.model.Certificate
import com.example.data.model.CertificateDraft
import com.example.data.model.RutUtils
import com.example.data.model.StudentProfile
import com.example.data.model.VerifierConfig
import com.example.util.DateFormatter
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class VerifierHostStatus(
    val rawUrl: String = "",
    val effectiveUrl: String = "",
    val isLive: Boolean = false
)

class CertificateRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireOfficialAdmin(): String {
        val user = auth.currentUser
            ?: throw IllegalStateException("Debes iniciar sesión con Google.")
        val email = user.email?.trim()?.lowercase(Locale.US)
        check(AdminConfig.isOfficialAdmin(email)) {
            "Acceso denegado: solo ${AdminConfig.OFFICIAL_ADMIN_EMAIL} puede crear, editar o aprobar certificaciones."
        }
        return user.uid
    }

    private fun requireSignedInUser(): Pair<String, String> {
        val user = auth.currentUser
            ?: throw IllegalStateException("Debes iniciar sesión con Google.")
        val email = user.email?.trim()?.lowercase(Locale.US).orEmpty()
        require(email.isNotBlank()) { "La cuenta Google no tiene un correo electrónico válido." }
        return user.uid to email
    }

    fun observeAllCertificates(): Flow<List<Certificate>> = flow {
        val path = "certificates"
        emitAll(
            db.collection("certificates")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Certificate::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && AdminConfig.isOfficialAdmin(auth.currentUser?.email)) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeStudentApprovedCertificates(studentEmail: String): Flow<List<Certificate>> = flow {
        val normalized = studentEmail.trim().lowercase(Locale.US)
        require(normalized.isNotBlank()) { "El correo del alumno es obligatorio." }
        val path = "certificates"
        emitAll(
            db.collection("certificates")
                .whereEqualTo("status", "active")
                .whereEqualTo("studentEmail", normalized)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        Certificate::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser != null) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun migrateLegacyCertificatesIfNeeded(adminUid: String) {
        if (!AdminConfig.isOfficialAdmin(auth.currentUser?.email)) return
        try {
            val legacySnap = db.collection("users")
                .document(adminUid)
                .collection("certificates")
                .get()
                .await()
            if (legacySnap.isEmpty) return

            for (doc in legacySnap.documents) {
                val legacyCert = doc.toObject(Certificate::class.java) ?: continue
                val existingTarget = db.collection("certificates").document(doc.id).get().await()
                if (!existingTarget.exists()) {
                    val draft = legacyCert.toDraft()
                    val code = legacyCert.certificateCode.ifBlank { doc.id }
                    db.collection("certificates")
                        .document(doc.id)
                        .set(draft.toCreateMap(adminUid, code))
                        .await()
                }
                doc.reference.delete().await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Legacy migration skipped or partial", e)
        }
    }

    private fun isValidPublicDocId(code: String): Boolean {
        return code.length in 1..128 && Regex("^[a-zA-Z0-9_\\-]+$").matches(code)
    }

    private fun documentSnapshotToPublicCertificate(snap: DocumentSnapshot, fallbackCode: String): Certificate? {
        if (!snap.exists()) return null
        val d = snap.data ?: return null
        return Certificate(
            id = "",
            certificateCode = (d["certificateCode"] as? String).orEmpty().ifBlank { fallbackCode },
            courseName = (d["courseName"] as? String).orEmpty(),
            description = (d["description"] as? String).orEmpty(),
            studentName = (d["studentName"] as? String).orEmpty(),
            studentRut = (d["studentRut"] as? String).orEmpty(),
            certificateType = (d["certificateType"] as? String).orEmpty(),
            operatorName = (d["operatorName"] as? String).orEmpty(),
            operatorRole = (d["operatorRole"] as? String).orEmpty(),
            equipment = (d["equipment"] as? String).orEmpty(),
            issuer = (d["issuer"] as? String).orEmpty(),
            issuedAt = (d["issuedAt"] as? String).orEmpty(),
            expiresAt = (d["expiresAt"] as? String).orEmpty(),
            status = (d["status"] as? String).orEmpty().ifBlank { "active" },
            whatsapp = (d["whatsapp"] as? String).orEmpty(),
            email = (d["email"] as? String).orEmpty(),
            verifiedByAdminEmail = (d["verifiedByAdminEmail"] as? String).orEmpty()
                .ifBlank { AdminConfig.OFFICIAL_ADMIN_EMAIL },
            themeColor = Certificate.safeHex(d["themeColor"] as? String, "#f3c970"),
            backgroundColor = Certificate.safeHex(d["backgroundColor"] as? String, "#ffffff"),
            cardColor = Certificate.safeHex(d["cardColor"] as? String, "#fafafa")
        )
    }

    suspend fun fetchPublicCertificateByCode(rawCode: String): Certificate? {
        val code = DateFormatter.sanitizeCertificateCode(rawCode)
        if (!isValidPublicDocId(code)) return null
        return try {
            val snap = db.collection("public_certificates")
                .document(code)
                .get()
                .await()
            documentSnapshotToPublicCertificate(snap, code)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
    }

    fun observePublicCertificateByCode(rawCode: String): Flow<Certificate?> = flow {
        val code = DateFormatter.sanitizeCertificateCode(rawCode)
        if (!isValidPublicDocId(code)) {
            emit(null)
            return@flow
        }
        emitAll(
            db.collection("public_certificates")
                .document(code)
                .snapshots()
                .map { snap -> documentSnapshotToPublicCertificate(snap, code) }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    emit(null)
                }
        )
    }

    suspend fun syncAllPublicCertificates(certificates: List<Certificate>) {
        if (!AdminConfig.isOfficialAdmin(auth.currentUser?.email)) return
        try {
            for (cert in certificates) {
                val safeCode = DateFormatter.sanitizeCertificateCode(cert.certificateCode)
                if (!isValidPublicDocId(safeCode)) continue
                val pubRef = db.collection("public_certificates").document(safeCode)
                if (cert.status == "active") {
                    pubRef.set(cert.copy(certificateCode = safeCode).toPublicVerificationMap()).await()
                } else {
                    pubRef.delete().await()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Public verification index sync skipped or partial", e)
        }
    }

    suspend fun createCertificate(
        draft: CertificateDraft,
        resolvedCode: String
    ): Result<Certificate> {
        val path = "certificates"
        return try {
            val uid = requireOfficialAdmin()
            val safeCode = DateFormatter.sanitizeCertificateCode(resolvedCode)
            validateDraft(draft, safeCode)
            val docRef = db.collection("certificates").document()
            val payload = draft.toCreateMap(uid, safeCode)
            val batch = db.batch()
            batch.set(docRef, payload)
            if (draft.status == "active" && isValidPublicDocId(safeCode)) {
                val pubRef = db.collection("public_certificates").document(safeCode)
                batch.set(pubRef, draft.toPublicVerificationMap(safeCode))
            }
            batch.commit().await()
            val created = draft.toPreviewCertificate(uid).copy(
                id = docRef.id,
                certificateCode = safeCode
            )
            Result.success(created)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateCertificate(
        existing: Certificate,
        draft: CertificateDraft,
        resolvedCode: String
    ): Result<Certificate> {
        val docPath = "certificates/${existing.id}"
        return try {
            val uid = requireOfficialAdmin()
            require(existing.id.isNotBlank()) { "El identificador del certificado no puede estar vacío." }
            val safeCode = DateFormatter.sanitizeCertificateCode(resolvedCode)
            validateDraft(draft, safeCode)
            val docRef = db.collection("certificates").document(existing.id)
            val batch = db.batch()
            batch.update(docRef, draft.toUpdateMap(safeCode))

            val oldCode = existing.certificateCode.trim()
            if (oldCode.isNotBlank() && oldCode != safeCode && isValidPublicDocId(oldCode)) {
                batch.delete(db.collection("public_certificates").document(oldCode))
            }
            if (isValidPublicDocId(safeCode)) {
                val pubRef = db.collection("public_certificates").document(safeCode)
                if (draft.status == "active") {
                    batch.set(pubRef, draft.toPublicVerificationMap(safeCode))
                } else {
                    batch.delete(pubRef)
                }
            }
            batch.commit().await()
            val updated = draft.toPreviewCertificate(uid).copy(
                id = existing.id,
                certificateCode = safeCode,
                scanCount = existing.scanCount,
                createdAt = existing.createdAt
            )
            Result.success(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun togglePublishStatus(certificate: Certificate): Result<String> {
        val docPath = "certificates/${certificate.id}"
        return try {
            requireOfficialAdmin()
            require(certificate.id.isNotBlank()) { "ID de certificado inválido." }
            val nextStatus = if (certificate.status == "pending") "active" else "pending"
            val batch = db.batch()
            batch.update(
                db.collection("certificates").document(certificate.id),
                mapOf(
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            val safeCode = DateFormatter.sanitizeCertificateCode(certificate.certificateCode)
            if (isValidPublicDocId(safeCode)) {
                val pubRef = db.collection("public_certificates").document(safeCode)
                if (nextStatus == "active") {
                    batch.set(pubRef, certificate.copy(certificateCode = safeCode, status = "active").toPublicVerificationMap())
                } else {
                    batch.delete(pubRef)
                }
            }
            batch.commit().await()
            Result.success(nextStatus)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun incrementScanCount(certificate: Certificate): Result<Unit> {
        val docPath = "certificates/${certificate.id}"
        return try {
            if (!AdminConfig.isOfficialAdmin(auth.currentUser?.email) || certificate.id.isBlank()) {
                return Result.success(Unit)
            }
            db.collection("certificates")
                .document(certificate.id)
                .update(
                    mapOf(
                        "scanCount" to FieldValue.increment(1L),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteCertificate(certificate: Certificate): Result<Unit> {
        val docPath = "certificates/${certificate.id}"
        return try {
            requireOfficialAdmin()
            require(certificate.id.isNotBlank()) { "ID de certificado inválido." }
            val batch = db.batch()
            batch.delete(db.collection("certificates").document(certificate.id))
            val oldCode = certificate.certificateCode.trim()
            if (isValidPublicDocId(oldCode)) {
                batch.delete(db.collection("public_certificates").document(oldCode))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeMyStudentProfile(uid: String): Flow<StudentProfile?> = flow {
        require(uid.isNotBlank())
        val path = "student_profiles/$uid"
        emitAll(
            db.collection("student_profiles")
                .document(uid)
                .snapshots()
                .map { doc ->
                    if (doc.exists()) {
                        doc.toObject(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else {
                        null
                    }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun submitStudentProfile(
        fullName: String,
        rawRut: String
    ): Result<Unit> {
        val (uid, email) = try {
            requireSignedInUser()
        } catch (e: Exception) {
            return Result.failure(e)
        }
        val docPath = "student_profiles/$uid"
        return try {
            val cleanName = fullName.trim()
            require(cleanName.length in 2..100) {
                "Ingresa tu nombre completo (entre 2 y 100 caracteres)."
            }
            require(RutUtils.isPlausibleRut(rawRut)) {
                "Ingresa un RUT válido (ej. 12.345.678-9)."
            }
            val formattedRut = RutUtils.format(rawRut)
            val docRef = db.collection("student_profiles").document(uid)
            val existing = docRef.get().await()

            if (existing.exists()) {
                docRef.update(
                    mapOf(
                        "fullName" to cleanName,
                        "rut" to formattedRut,
                        "verificationStatus" to "pending",
                        "approvedByEmail" to "",
                        "adminNote" to "",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "uid" to uid,
                        "email" to email,
                        "fullName" to cleanName,
                        "rut" to formattedRut,
                        "verificationStatus" to "pending",
                        "approvedByEmail" to "",
                        "adminNote" to "",
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeAllStudentProfiles(): Flow<List<StudentProfile>> = flow {
        val path = "student_profiles"
        emitAll(
            db.collection("student_profiles")
                .snapshots()
                .map { snap ->
                    snap.toObjects(StudentProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedWith(
                            compareBy<StudentProfile> {
                                when (it.verificationStatus) {
                                    "pending" -> 0
                                    "approved" -> 1
                                    else -> 2
                                }
                            }.thenByDescending { it.updatedAt?.seconds ?: 0L }
                        )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && AdminConfig.isOfficialAdmin(auth.currentUser?.email)) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun approveStudentProfile(
        profile: StudentProfile,
        matchingCertificates: List<Certificate>
    ): Result<Int> {
        val docPath = "student_profiles/${profile.uid}"
        return try {
            requireOfficialAdmin()
            db.collection("student_profiles")
                .document(profile.uid)
                .update(
                    mapOf(
                        "verificationStatus" to "approved",
                        "approvedByEmail" to AdminConfig.OFFICIAL_ADMIN_EMAIL,
                        "adminNote" to "Comprobado por administrador oficial",
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()

            val normalizedEmail = profile.email.trim().lowercase(Locale.US)
            val normalizedProfileRut = RutUtils.clean(profile.rut)
            var linkedCount = 0

            for (cert in matchingCertificates) {
                val certRutClean = RutUtils.clean(cert.studentRut)
                val matchesByRut = normalizedProfileRut.isNotBlank() && certRutClean == normalizedProfileRut
                val matchesByEmail = cert.studentEmail.trim().lowercase(Locale.US) == normalizedEmail
                if (matchesByRut || matchesByEmail) {
                    if (cert.studentEmail.trim().lowercase(Locale.US) != normalizedEmail ||
                        cert.studentRut.isBlank()
                    ) {
                        db.collection("certificates")
                            .document(cert.id)
                            .update(
                                mapOf(
                                    "studentEmail" to normalizedEmail,
                                    "studentRut" to cert.studentRut.ifBlank { profile.rut },
                                    "verifiedByAdminEmail" to AdminConfig.OFFICIAL_ADMIN_EMAIL,
                                    "updatedAt" to FieldValue.serverTimestamp()
                                )
                            )
                            .await()
                    }
                    linkedCount++
                }
            }
            Result.success(linkedCount)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun rejectStudentProfile(
        profile: StudentProfile,
        reason: String
    ): Result<Unit> {
        val docPath = "student_profiles/${profile.uid}"
        return try {
            requireOfficialAdmin()
            db.collection("student_profiles")
                .document(profile.uid)
                .update(
                    mapOf(
                        "verificationStatus" to "rejected",
                        "approvedByEmail" to AdminConfig.OFFICIAL_ADMIN_EMAIL,
                        "adminNote" to reason.trim().take(280).ifBlank {
                            "Los datos ingresados no coinciden con los registros oficiales."
                        },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun isVerifierHostLive(baseUrl: String): Boolean = withContext(Dispatchers.IO) {
        val cleanBase = baseUrl.trim().trimEnd('/')
        if (!cleanBase.startsWith("https://") && !cleanBase.startsWith("http://")) {
            return@withContext false
        }
        if (cleanBase.contains("netlify.app", ignoreCase = true)) {
            return@withContext false
        }
        val probeUrl = if (
            cleanBase.endsWith(".html", ignoreCase = true) ||
            cleanBase.endsWith(".htm", ignoreCase = true)
        ) {
            cleanBase
        } else {
            "$cleanBase/"
        }
        var connection: HttpURLConnection? = null
        try {
            val url = URL(probeUrl)
            connection = (url.openConnection() as? HttpURLConnection)?.apply {
                requestMethod = "GET"
                connectTimeout = 4000
                readTimeout = 4000
                instanceFollowRedirects = true
                useCaches = false
                setRequestProperty("Cache-Control", "no-cache")
            } ?: return@withContext false

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return@withContext false
            }

            val bodySnippet = connection.inputStream.bufferedReader().use { reader ->
                val buffer = CharArray(12288)
                val read = reader.read(buffer)
                if (read > 0) String(buffer, 0, read) else ""
            }
            val isNotFoundPage = bodySnippet.contains("Sitio no encontrado", ignoreCase = true) ||
                bodySnippet.contains("Site Not Found", ignoreCase = true) ||
                bodySnippet.contains("Page Not Found", ignoreCase = true)
            val looksLikeCapacitamaq = bodySnippet.contains("CapacitaMaq", ignoreCase = true) ||
                bodySnippet.contains("Verificador", ignoreCase = true)

            !isNotFoundPage && looksLikeCapacitamaq
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        } finally {
            connection?.disconnect()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeVerifierHostStatus(refreshTrigger: Flow<Int> = flowOf(0)): Flow<VerifierHostStatus> = flow {
        val rawFlow = db.collection("app_config")
            .document("verifier")
            .snapshots()
            .map { snap ->
                val raw = snap.toObject(VerifierConfig::class.java)?.verifierBaseUrl?.trim().orEmpty()
                if (raw.contains("netlify.app", ignoreCase = true)) "" else raw
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                emit("")
            }

        emitAll(
            combine(rawFlow, refreshTrigger) { raw, _ -> raw }
                .transformLatest { raw ->
                    val candidate = raw.ifBlank { AdminConfig.FIREBASE_HOSTING_DEFAULT_URL }
                    emit(VerifierHostStatus(rawUrl = raw, effectiveUrl = "", isLive = false))
                    val live = isVerifierHostLive(candidate)
                    if (live) {
                        emit(VerifierHostStatus(rawUrl = candidate, effectiveUrl = candidate, isLive = true))
                    }
                }
        )
    }

    fun observeVerifierConfig(): Flow<String> = observeVerifierHostStatus().map { it.effectiveUrl }

    suspend fun saveVerifierBaseUrl(rawUrl: String): Result<String> {
        val docPath = "app_config/verifier"
        return try {
            requireOfficialAdmin()
            val trimmed = rawUrl.trim().trimEnd('/')
            require(trimmed.isEmpty() || trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
                "La URL del verificador web debe comenzar con https://"
            }
            require(!trimmed.contains("netlify.app", ignoreCase = true)) {
                "Las URLs de Netlify están bloqueadas. Usa Firebase Hosting o deja el verificador automático de la app."
            }
            if (trimmed.isNotEmpty()) {
                val isLive = isVerifierHostLive(trimmed)
                check(isLive) {
                    "El sitio $trimmed aún no está publicado (devuelve 404). Todos tus códigos QR y botones siguen funcionando sin error dentro de la app. Para activar enlaces web externos, ejecuta primero «firebase deploy --only hosting» desde tu PC."
                }
            }
            db.collection("app_config")
                .document("verifier")
                .set(
                    mapOf(
                        "verifierBaseUrl" to trimmed.take(300),
                        "updatedByEmail" to AdminConfig.OFFICIAL_ADMIN_EMAIL,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(trimmed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (e is FirebaseFirestoreException) {
                handleFirestoreError(e, OperationType.WRITE, docPath)
            }
            Result.failure(e)
        }
    }

    private fun validateDraft(draft: CertificateDraft, resolvedCode: String) {
        require(draft.courseName.trim().isNotEmpty() && draft.courseName.trim().length <= 120) {
            "El título de la certificación es obligatorio (máximo 120 caracteres)."
        }
        require(draft.studentName.trim().isNotEmpty() && draft.studentName.trim().length <= 100) {
            "El nombre completo del alumno es obligatorio (máximo 100 caracteres)."
        }
        if (draft.studentRut.isNotBlank()) {
            require(RutUtils.isPlausibleRut(draft.studentRut)) {
                "El RUT del alumno no tiene un formato válido (ej. 12.345.678-9)."
            }
        }
        require(draft.issuer.trim().isNotEmpty() && draft.issuer.trim().length <= 100) {
            "La entidad certificadora es obligatoria."
        }
        require(draft.issuedAt.trim().length in 8..20) {
            "La fecha de emisión es obligatoria."
        }
        require(resolvedCode.isNotEmpty() && resolvedCode.length <= 100) {
            "El identificador público debe tener entre 1 y 100 caracteres."
        }
        if (draft.expiresAt.isNotBlank() && draft.expiresAt.trim() < draft.issuedAt.trim()) {
            throw IllegalArgumentException("La fecha de vencimiento no puede ser anterior a la emisión.")
        }
    }

    private companion object {
        const val TAG = "CertificateRepo"
    }
}

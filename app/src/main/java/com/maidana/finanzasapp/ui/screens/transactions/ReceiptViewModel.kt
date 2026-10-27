package com.maidana.finanzasapp.ui.screens.transactions

import android.app.Application
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.maidana.finanzasapp.data.receipt.MlKitReceiptRecognizer
import com.maidana.finanzasapp.domain.ReceiptDraft
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

sealed interface ReceiptState {
    data object Idle : ReceiptState
    data object Reading : ReceiptState
    data class Review(val draft: ReceiptDraft, val ownerUid: String) : ReceiptState
    data class Error(val message: String) : ReceiptState
}

class ReceiptViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    private val auth = FirebaseAuth.getInstance()
    private val recognizer = MlKitReceiptRecognizer(application)
    private val _state = MutableStateFlow<ReceiptState>(ReceiptState.Idle)
    val state = _state.asStateFlow()
    private var job: Job? = null
    private var owner = auth.currentUser?.uid
    private val authListener = FirebaseAuth.AuthStateListener { updated ->
        if (updated.currentUser?.uid != owner) {
            owner = updated.currentUser?.uid
            job?.cancel(); _state.value = ReceiptState.Idle; deletePending()
        }
    }

    init { auth.addAuthStateListener(authListener) }

    fun prepareCamera(): Uri {
        check(auth.currentUser != null) { "Iniciá sesión antes de escanear un ticket." }
        deletePending()
        val directory = File(getApplication<Application>().cacheDir, "receipt_captures").apply { mkdirs() }
        // Reclaim only this feature's old captures, including those left by a killed process.
        directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86_400_000 }?.forEach(File::delete)
        val file = File.createTempFile("ticket_", ".jpg", directory)
        savedState["capture"] = file.name
        savedState["captureOwner"] = auth.currentUser?.uid
        return FileProvider.getUriForFile(getApplication(), "${getApplication<Application>().packageName}.receipts", file)
    }

    fun cameraResult(success: Boolean) {
        val file = pendingFile()
        val expectedOwner: String? = savedState["captureOwner"]
        savedState.remove<String>("capture"); savedState.remove<String>("captureOwner")
        if (success && file != null && expectedOwner != null && auth.currentUser?.uid == expectedOwner) {
            read(Uri.fromFile(file), file)
        } else file?.delete()
    }

    fun read(uri: Uri, temporaryFile: File? = null) {
        val uid = auth.currentUser?.uid
        if (uid == null) { temporaryFile?.delete(); showError("Iniciá sesión para escanear un ticket."); return }
        if (job?.isActive == true) { temporaryFile?.delete(); return }
        _state.value = ReceiptState.Reading
        job = viewModelScope.launch {
            try {
                val draft = recognizer.recognize(uri)
                if (auth.currentUser?.uid == uid) _state.value = ReceiptState.Review(draft, uid)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _state.value = ReceiptState.Error("No se pudo leer el ticket. Probá una imagen más nítida o completá el gasto manualmente.") }
            finally { temporaryFile?.delete() }
        }
    }

    fun reset() { _state.value = ReceiptState.Idle }
    fun showError(message: String) { _state.value = ReceiptState.Error(message); deletePending() }
    private fun pendingFile(): File? {
        val name: String = savedState["capture"] ?: return null
        if (!name.matches(Regex("ticket_[a-zA-Z0-9_\\-]+\\.jpg"))) return null
        return File(File(getApplication<Application>().cacheDir, "receipt_captures"), name)
    }
    private fun deletePending() {
        pendingFile()?.delete(); savedState.remove<String>("capture"); savedState.remove<String>("captureOwner")
    }
    override fun onCleared() { auth.removeAuthStateListener(authListener); deletePending() }
}

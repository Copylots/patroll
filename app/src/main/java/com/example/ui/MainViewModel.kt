package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.GrupPatroli
import com.example.data.Patroli
import com.example.data.Repository
import com.example.data.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel(private val repository: Repository) : ViewModel() {

    // --- Authentication State ---
    var usernameInput = MutableStateFlow("")
    var passwordInput = MutableStateFlow("")
    private val _loginError = MutableStateFlow("")
    val loginError = _loginError.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    fun login(onSuccess: () -> Unit) {
        val user = usernameInput.value.trim()
        val pass = passwordInput.value.trim()

        if (user.isEmpty() || pass.isEmpty()) {
            _loginError.value = "Username dan Password wajib diisi!"
            return
        }

        viewModelScope.launch {
            val dbUser = repository.getUser(user)
            if (dbUser != null && dbUser.password == pass) {
                _loginError.value = ""
                _isLoggedIn.value = true
                onSuccess()
            } else {
                _loginError.value = "Username atau Password salah!"
            }
        }
    }

    fun logout() {
        usernameInput.value = ""
        passwordInput.value = ""
        _isLoggedIn.value = false
        _loginError.value = ""
    }

    // --- Groups State ---
    val allGroups = repository.allGrup.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun createGroup(name: String, description: String = "", onResult: (Boolean, String) -> Unit) {
        val cleanedName = name.trim()
        if (cleanedName.isEmpty()) {
            onResult(false, "Nama grup tidak boleh kosong!")
            return
        }

        viewModelScope.launch {
            val existing = repository.getGrupByName(cleanedName)
            if (existing != null) {
                onResult(false, "Nama judul sudah digunakan!")
            } else {
                try {
                    val id = repository.insertGrup(GrupPatroli(namaGrup = cleanedName, keterangan = description))
                    onResult(true, id.toString())
                } catch (e: Exception) {
                    onResult(false, "Gagal membuat grup: ${e.message}")
                }
            }
        }
    }

    // --- Patrol Form State ---
    var selectedGroup = MutableStateFlow<GrupPatroli?>(null)
    var hariTanggalInput = MutableStateFlow("")
    var jamInput = MutableStateFlow("")
    var shiftInput = MutableStateFlow("")
    var petugasInput = MutableStateFlow("")
    var uraianInput = MutableStateFlow("")
    var keteranganInput = MutableStateFlow("")

    var ttdPjtPath = MutableStateFlow<String?>(null)
    var ttdPetugasPath = MutableStateFlow<String?>(null)

    init {
        // Initialize with default date & time formatted like in Python script
        val today = Date()
        val dateLocale = Locale("id", "ID")
        val dateFmt = SimpleDateFormat("EEEE, dd-MM-yyyy", dateLocale)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())

        hariTanggalInput.value = dateFmt.format(today)
        jamInput.value = timeFmt.format(today)
    }

    fun clearForm() {
        selectedGroup.value = null
        val today = Date()
        val dateLocale = Locale("id", "ID")
        val dateFmt = SimpleDateFormat("EEEE, dd-MM-yyyy", dateLocale)
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        hariTanggalInput.value = dateFmt.format(today)
        jamInput.value = timeFmt.format(today)
        shiftInput.value = ""
        petugasInput.value = ""
        uraianInput.value = ""
        keteranganInput.value = ""
        ttdPjtPath.value = null
        ttdPetugasPath.value = null
        selectedPatrolId.value = null
    }

    fun saveSignature(context: Context, bitmap: Bitmap, prefix: String, onSaved: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val dir = File(context.filesDir, "Signature_Files")
                if (!dir.exists()) {
                    dir.mkdirs()
                }
                val timeStamp = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())
                val file = File(dir, "sign_${prefix}_$timeStamp.png")
                val fos = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
                fos.flush()
                fos.close()
                withContext(Dispatchers.Main) {
                    onSaved(file.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Historical Patrol State ---
    val allPatrolLogs = repository.allPatroli.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    var searchQuery = MutableStateFlow("")
    val searchedLogs = searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.trim().isEmpty()) {
                allPatrolLogs
            } else {
                flow {
                    emit(repository.searchPatroli("%${query.trim()}%"))
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    var selectedPatrolId = MutableStateFlow<Int?>(null)

    fun selectPatrolForEdit(patrol: Patroli) {
        viewModelScope.launch {
            selectedPatrolId.value = patrol.id
            jamInput.value = patrol.jam
            hariTanggalInput.value = patrol.hariTanggal
            shiftInput.value = patrol.shift
            petugasInput.value = patrol.petugas
            uraianInput.value = patrol.uraian
            keteranganInput.value = patrol.keterangan
            ttdPjtPath.value = patrol.ttdPjt
            ttdPetugasPath.value = patrol.ttdPetugas

            // Retrieve group
            val groupIdStr = patrol.grupId
            if (!groupIdStr.isNullOrEmpty()) {
                val gId = groupIdStr.toIntOrNull()
                if (gId != null) {
                    selectedGroup.value = repository.getGrupById(gId)
                }
            } else {
                selectedGroup.value = null
            }
        }
    }

    fun saveOrUpdatePatrol(onResult: (Boolean, String) -> Unit) {
        val group = selectedGroup.value
        val tgl = hariTanggalInput.value.trim()
        val jam = jamInput.value.trim()
        val shift = shiftInput.value.trim()
        val petugas = petugasInput.value.trim()
        val uraian = uraianInput.value.trim()
        val keterangan = keteranganInput.value.trim()

        if (tgl.isEmpty() || jam.isEmpty() || shift.isEmpty() || petugas.isEmpty()) {
            onResult(false, "Semua kolom (kecuali uraian & keterangan) wajib diisi!")
            return
        }

        if (!tgl.contains(",")) {
            onResult(false, "Format Tanggal salah! Gunakan koma (Contoh: Senin, 22-07-2026)")
            return
        }

        if (!jam.contains(":")) {
            onResult(false, "Format Jam salah! Gunakan titik dua (Contoh: 21:00)")
            return
        }

        viewModelScope.launch {
            val patrol = Patroli(
                id = selectedPatrolId.value ?: 0,
                grupId = group?.id?.toString(),
                jam = jam,
                uraian = uraian,
                hariTanggal = tgl,
                shift = shift,
                petugas = petugas,
                ttdPjt = ttdPjtPath.value,
                ttdPetugas = ttdPetugasPath.value,
                keterangan = keterangan
            )

            try {
                if (patrol.id == 0) {
                    repository.insertPatroli(patrol)
                    onResult(true, "Data Berhasil Disimpan!")
                } else {
                    repository.updatePatroli(patrol)
                    onResult(true, "Data berhasil diperbarui!")
                }
                clearForm()
            } catch (e: Exception) {
                onResult(false, "Gagal menyimpan data: ${e.message}")
            }
        }
    }

    fun deletePatrolLogs(patrolIds: List<Int>, onResult: (Boolean, String) -> Unit) {
        if (patrolIds.isEmpty()) {
            onResult(false, "Pilih data yang ingin dihapus!")
            return
        }

        viewModelScope.launch {
            try {
                for (id in patrolIds) {
                    val p = repository.getPatroliById(id)
                    if (p != null) {
                        repository.deletePatroli(p)
                    }
                }
                onResult(true, "${patrolIds.size} Data berhasil dihapus dari sistem!")
            } catch (e: Exception) {
                onResult(false, "Gagal menghapus data: ${e.message}")
            }
        }
    }

    // --- PDF Generator State & Triggers ---
    var pimp1Name = MutableStateFlow("")
    var pimp1Title = MutableStateFlow("Komandan Regu / Danru")
    var pimp1SignPath = MutableStateFlow<String?>(null)

    var pimp2Name = MutableStateFlow("")
    var pimp2Title = MutableStateFlow("Supervisor")
    var pimp2SignPath = MutableStateFlow<String?>(null)

    var pimp3Name = MutableStateFlow("")
    var pimp3Title = MutableStateFlow("Pelaksana Tugas")
    var pimp3SignPath = MutableStateFlow<String?>(null)

    fun clearPimpinanSignatures() {
        pimp1Name.value = ""
        pimp1Title.value = "Komandan Regu / Danru"
        pimp1SignPath.value = null

        pimp2Name.value = ""
        pimp2Title.value = "Supervisor"
        pimp2SignPath.value = null

        pimp3Name.value = ""
        pimp3Title.value = "Pelaksana Tugas"
        pimp3SignPath.value = null
    }

    fun printPdfForGroupAndDate(
        context: Context,
        titleHeader: String,
        targetPatrol: Patroli?,
        onResult: (Boolean, File?) -> Unit
    ) {
        viewModelScope.launch {
            // Determine active date and group
            val activeDate: String
            val activeGroupId: String?
            val activeGroupName: String

            if (targetPatrol != null) {
                activeDate = targetPatrol.hariTanggal
                activeGroupId = targetPatrol.grupId
                val gId = activeGroupId?.toIntOrNull()
                activeGroupName = if (gId != null) {
                    repository.getGrupById(gId)?.namaGrup ?: "Laporan Patroli"
                } else {
                    "Laporan Patroli"
                }
            } else {
                activeDate = hariTanggalInput.value.trim()
                activeGroupId = selectedGroup.value?.id?.toString()
                activeGroupName = selectedGroup.value?.namaGrup ?: "Laporan Patroli"
            }

            if (activeDate.isEmpty() || activeGroupId == null) {
                onResult(false, null)
                return@launch
            }

            // Retrieve all records for this group and date on Dispatchers.IO
            val records = withContext(Dispatchers.IO) {
                repository.getPatroliByDateAndGroup(activeDate, activeGroupId)
            }

            if (records.isEmpty()) {
                onResult(false, null)
                return@launch
            }

            // Create target PDF file
            val pdfDir = File(context.filesDir, "PDF_Patroli")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }

            val sanitizedGroupName = activeGroupName.replace("\\s+".toRegex(), "_").lowercase()
            val sanitizedShift = records.firstOrNull()?.shift?.replace("\\s+".toRegex(), "_")?.lowercase() ?: "no_shift"
            val sanitizedDate = activeDate.replace(", ", "_").replace("-", "").replace("\\s+".toRegex(), "_")
            val timeStamp = SimpleDateFormat("HHmmss", Locale.getDefault()).format(Date())

            val fileName = "Laporan_${sanitizedGroupName}_${sanitizedShift}_${sanitizedDate}_$timeStamp.pdf"
            val pdfFile = File(pdfDir, fileName)

            val success = withContext(Dispatchers.IO) {
                PdfGenerator.generatePatroliPdf(
                    context = context,
                    pdfFile = pdfFile,
                    headerTitle = titleHeader,
                    grupName = activeGroupName,
                    hariTanggal = activeDate,
                    shift = records.firstOrNull()?.shift ?: "-",
                    petugas = records.firstOrNull()?.petugas ?: "-",
                    records = records,
                    pimp1Name = pimp1Name.value.trim(),
                    pimp1Title = pimp1Title.value.trim(),
                    pimp1SignPath = pimp1SignPath.value,
                    pimp2Name = pimp2Name.value.trim(),
                    pimp2Title = pimp2Title.value.trim(),
                    pimp2SignPath = pimp2SignPath.value,
                    pimp3Name = pimp3Name.value.trim(),
                    pimp3Title = pimp3Title.value.trim(),
                    pimp3SignPath = pimp3SignPath.value
                )
            }

            onResult(success, if (success) pdfFile else null)
        }
    }
}

class MainViewModelFactory(private val repository: Repository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

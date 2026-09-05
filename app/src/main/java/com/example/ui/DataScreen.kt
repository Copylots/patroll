package com.example.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Patroli
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataScreen(
    viewModel: MainViewModel,
    onNavigateToInput: () -> Unit
) {
    val context = LocalContext.current

    // States from ViewModel
    val logs by viewModel.searchedLogs.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedPatrolId by viewModel.selectedPatrolId.collectAsStateWithLifecycle()
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()

    // Screen-local states for multi selection & dialogs
    val checkedLogs = remember { mutableStateListOf<Int>() }
    var isPdfDialogOpen by remember { mutableStateOf(false) }
    var isPimpSignatureDialogOpen by remember { mutableStateOf(false) }
    var pimpSignatureTargetIndex by remember { mutableStateOf(1) } // 1, 2, or 3

    // Success dialog after PDF generated
    var generatedPdfFile by remember { mutableStateOf<File?>(null) }
    var isSuccessDialogOpen by remember { mutableStateOf(false) }

    // Clear checks when logs reload
    LaunchedEffect(logs) {
        checkedLogs.clear()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Data Riwayat Patroli",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                label = { Text("Cari Nama Petugas / Uraian Kegiatan...") },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Table / List View
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Empty Status",
                            tint = Color.Gray,
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "Belum ada data patroli terekam.",
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        val groupName = groups.find { it.id.toString() == log.grupId }?.namaGrup ?: "Laporan"
                        val isSelected = selectedPatrolId == log.id

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.selectPatrolForEdit(log)
                                    onNavigateToInput()
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                MaterialTheme.colorScheme.primary
                            ) else null
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = checkedLogs.contains(log.id),
                                    onCheckedChange = { checked ->
                                        if (checked) {
                                            checkedLogs.add(log.id)
                                        } else {
                                            checkedLogs.remove(log.id)
                                        }
                                    }
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "[${log.jam}]",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = log.hariTanggal,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Text(
                                        text = "Kategori: $groupName",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Gray
                                    )

                                    Text(
                                        text = "Petugas: ${log.petugas} (${log.shift})",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )

                                    Text(
                                        text = "Uraian: ${log.uraian}",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        val hasPjt = !log.ttdPjt.isNullOrEmpty() && File(log.ttdPjt).exists()
                                        val hasPetugas = !log.ttdPetugas.isNullOrEmpty() && File(log.ttdPetugas).exists()

                                        Text(
                                            text = "TTD PJT: " + if (hasPjt) "Ada ✓" else "-",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (hasPjt) Color(0xFF2E7D32) else Color.Red
                                        )
                                        Text(
                                            text = "TTD Petugas: " + if (hasPetugas) "Ada ✓" else "-",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (hasPetugas) Color(0xFF2E7D32) else Color.Red
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Navigation & Actions Row
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = onNavigateToInput,
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("< Kembali Input", fontSize = 12.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            if (selectedPatrolId == null) {
                                Toast.makeText(context, "Klik salah satu baris data pada tabel terlebih dahulu!", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.saveOrUpdatePatrol { success, message ->
                                    Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE65100)
                        )
                    ) {
                        Text("Update", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.clearPimpinanSignatures()
                            isPdfDialogOpen = true
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1B5E20)
                        )
                    ) {
                        Text("Cetak PDF", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.deletePatrolLogs(checkedLogs) { success, message ->
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(45.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. PDF Signatures and Configurations Wizard Dialog
    if (isPdfDialogOpen) {
        val pimp1Name by viewModel.pimp1Name.collectAsStateWithLifecycle()
        val pimp1Title by viewModel.pimp1Title.collectAsStateWithLifecycle()
        val pimp1SignPath by viewModel.pimp1SignPath.collectAsStateWithLifecycle()

        val pimp2Name by viewModel.pimp2Name.collectAsStateWithLifecycle()
        val pimp2Title by viewModel.pimp2Title.collectAsStateWithLifecycle()
        val pimp2SignPath by viewModel.pimp2SignPath.collectAsStateWithLifecycle()

        val pimp3Name by viewModel.pimp3Name.collectAsStateWithLifecycle()
        val pimp3Title by viewModel.pimp3Title.collectAsStateWithLifecycle()
        val pimp3SignPath by viewModel.pimp3SignPath.collectAsStateWithLifecycle()

        val activeGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
        val activeGroupText = activeGroup?.namaGrup?.uppercase() ?: ""

        var titleHeaderInput by remember {
            mutableStateOf(
                if (activeGroupText.isNotEmpty()) "LAPORAN PATROLI - $activeGroupText" else "BUKU LAPORAN PATROLI SECURITY"
            )
        }

        AlertDialog(
            onDismissRequest = { isPdfDialogOpen = false },
            title = { Text("Konfigurasi 3 TTD Pimpinan") },
            text = {
                val scrollState = rememberScrollState()
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .verticalScroll(scrollState)
                ) {
                    OutlinedTextField(
                        value = titleHeaderInput,
                        onValueChange = { titleHeaderInput = it },
                        label = { Text("Judul Header Laporan PDF") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "--- Data Verifikasi Pimpinan ---",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

                    // Pimpinan 1: Danru
                    OutlinedTextField(
                        value = pimp1Name,
                        onValueChange = { viewModel.pimp1Name.value = it },
                        label = { Text("Nama Danru") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pimp1Title,
                        onValueChange = { viewModel.pimp1Title.value = it },
                        label = { Text("Jabatan Pimpinan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val isPimp1Signed = !pimp1SignPath.isNullOrEmpty() && File(pimp1SignPath!!).exists()
                    Button(
                        onClick = {
                            pimpSignatureTargetIndex = 1
                            isPimpSignatureDialogOpen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPimp1Signed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = if (isPimp1Signed) Icons.Default.CheckCircle else Icons.Default.Edit, contentDescription = "Draw")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPimp1Signed) "TTD Danru: Terekam ✓" else "TTD Danru")
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pimpinan 2: SPV
                    OutlinedTextField(
                        value = pimp2Name,
                        onValueChange = { viewModel.pimp2Name.value = it },
                        label = { Text("Nama SPV") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pimp2Title,
                        onValueChange = { viewModel.pimp2Title.value = it },
                        label = { Text("Jabatan Pimpinan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val isPimp2Signed = !pimp2SignPath.isNullOrEmpty() && File(pimp2SignPath!!).exists()
                    Button(
                        onClick = {
                            pimpSignatureTargetIndex = 2
                            isPimpSignatureDialogOpen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPimp2Signed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = if (isPimp2Signed) Icons.Default.CheckCircle else Icons.Default.Edit, contentDescription = "Draw")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPimp2Signed) "TTD SPV: Terekam ✓" else "TTD SPV")
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Pimpinan 3: PJT
                    OutlinedTextField(
                        value = pimp3Name,
                        onValueChange = { viewModel.pimp3Name.value = it },
                        label = { Text("Nama PJT") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = pimp3Title,
                        onValueChange = { viewModel.pimp3Title.value = it },
                        label = { Text("Jabatan Pimpinan") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    val isPimp3Signed = !pimp3SignPath.isNullOrEmpty() && File(pimp3SignPath!!).exists()
                    Button(
                        onClick = {
                            pimpSignatureTargetIndex = 3
                            isPimpSignatureDialogOpen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isPimp3Signed) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(imageVector = if (isPimp3Signed) Icons.Default.CheckCircle else Icons.Default.Edit, contentDescription = "Draw")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isPimp3Signed) "TTD PJT: Terekam ✓" else "TTD PJT")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activeLogTarget = if (selectedPatrolId != null) {
                            logs.find { it.id == selectedPatrolId }
                        } else {
                            logs.firstOrNull()
                        }

                        viewModel.printPdfForGroupAndDate(context, titleHeaderInput, activeLogTarget) { success, file ->
                            if (success && file != null) {
                                generatedPdfFile = file
                                isPdfDialogOpen = false
                                isSuccessDialogOpen = true
                            } else {
                                Toast.makeText(
                                    context,
                                    "Gagal Cetak: Sistem tidak mendeteksi tanggal laporan atau tidak ada data cocok!\n\nCara Cetak:\n1. Klik salah satu baris data di tabel terlebih dahulu.\n2. Klik tombol Cetak PDF.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                ) {
                    Text("Cetak Laporan PDF")
                }
            },
            dismissButton = {
                TextButton(onClick = { isPdfDialogOpen = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // 2. Interactive Pimpinan Signature dialog
    if (isPimpSignatureDialogOpen) {
        var clearCanvasTrigger by remember { mutableStateOf(0) }
        var currentDrawingBitmap by remember { mutableStateOf<Bitmap?>(null) }

        AlertDialog(
            onDismissRequest = { isPimpSignatureDialogOpen = false },
            title = { Text("Tanda Tangan Pimpinan $pimpSignatureTargetIndex (Gunakan Jari)") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SignatureCanvas(
                        modifier = Modifier.fillMaxWidth(),
                        clearTrigger = clearCanvasTrigger,
                        onDrawStateChanged = { bitmap ->
                            currentDrawingBitmap = bitmap
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val drawing = currentDrawingBitmap
                        if (drawing != null) {
                            viewModel.saveSignature(context, drawing, "pimpinan_$pimpSignatureTargetIndex") { path ->
                                when (pimpSignatureTargetIndex) {
                                    1 -> viewModel.pimp1SignPath.value = path
                                    2 -> viewModel.pimp2SignPath.value = path
                                    3 -> viewModel.pimp3SignPath.value = path
                                }
                                isPimpSignatureDialogOpen = false
                            }
                        } else {
                            Toast.makeText(context, "Silakan bubuhkan tanda tangan terlebih dahulu!", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Kunci & Simpan")
                }
            },
            dismissButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = { clearCanvasTrigger++ }
                    ) {
                        Text("Bersihkan")
                    }
                    TextButton(onClick = { isPimpSignatureDialogOpen = false }) {
                        Text("Batal")
                    }
                }
            }
        )
    }

    // 3. Success dialog after generating the PDF, containing the SHARE / OPEN PDF trigger
    if (isSuccessDialogOpen) {
        AlertDialog(
            onDismissRequest = { isSuccessDialogOpen = false },
            title = { Text("Cetak Berhasil") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("PDF berhasil disimpan!\n\nNama file:\n${generatedPdfFile?.name ?: ""}")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = generatedPdfFile
                        if (target != null) {
                            try {
                                val authority = "${context.packageName}.fileprovider"
                                val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, target)
                                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                    type = "application/pdf"
                                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(android.content.Intent.createChooser(intent, "Bagikan Laporan PDF"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal membagikan file: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                        isSuccessDialogOpen = false
                    }
                ) {
                    Text("BAGIKAN / BUKA")
                }
            },
            dismissButton = {
                TextButton(onClick = { isSuccessDialogOpen = false }) {
                    Text("TUTUP")
                }
            }
        )
    }
}

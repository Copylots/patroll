package com.example.ui

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.GrupPatroli
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToData: () -> Unit
) {
    val context = LocalContext.current

    // Observe StateFlow inputs
    val groups by viewModel.allGroups.collectAsStateWithLifecycle()
    val selectedGroup by viewModel.selectedGroup.collectAsStateWithLifecycle()
    val hariTanggal by viewModel.hariTanggalInput.collectAsStateWithLifecycle()
    val jam by viewModel.jamInput.collectAsStateWithLifecycle()
    val shift by viewModel.shiftInput.collectAsStateWithLifecycle()
    val petugas by viewModel.petugasInput.collectAsStateWithLifecycle()
    val uraian by viewModel.uraianInput.collectAsStateWithLifecycle()
    val keterangan by viewModel.keteranganInput.collectAsStateWithLifecycle()

    val ttdPjtPath by viewModel.ttdPjtPath.collectAsStateWithLifecycle()
    val ttdPetugasPath by viewModel.ttdPetugasPath.collectAsStateWithLifecycle()
    val isEditMode by viewModel.selectedPatrolId.collectAsStateWithLifecycle()

    // Dialog & Dropdown triggers
    var isGroupDropdownExpanded by remember { mutableStateOf(false) }
    var isNewGroupDialogOpen by remember { mutableStateOf(false) }
    var isSignatureDialogOpen by remember { mutableStateOf(false) }
    var signatureTypeTarget by remember { mutableStateOf("PJT") } // "PJT" or "Petugas"

    // Form scroll state
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Buku Patroli Security",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = {
                        viewModel.logout()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = if (isEditMode != null) "Edit Catatan Patroli" else "Input Data Patroli Baru",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            // Category/Group Selector Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Custom Dropdown Box for selecting Category
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedGroup?.namaGrup ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Pilih Judul / Kategori Grup") },
                        trailingIcon = {
                            Icon(
                                imageVector = if (isGroupDropdownExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown Trigger"
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isGroupDropdownExpanded = true },
                        enabled = true
                    )
                    // Click helper overlay
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { isGroupDropdownExpanded = !isGroupDropdownExpanded }
                    )

                    DropdownMenu(
                        expanded = isGroupDropdownExpanded,
                        onDismissRequest = { isGroupDropdownExpanded = false },
                        modifier = Modifier.fillMaxWidth(0.7f)
                    ) {
                        if (groups.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Belum ada judul. Buat baru!") },
                                onClick = {
                                    isGroupDropdownExpanded = false
                                    isNewGroupDialogOpen = true
                                }
                            )
                        } else {
                            groups.forEach { group ->
                                DropdownMenuItem(
                                    text = { Text(group.namaGrup) },
                                    onClick = {
                                        viewModel.selectedGroup.value = group
                                        isGroupDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Button(
                    onClick = { isNewGroupDialogOpen = true },
                    modifier = Modifier.height(54.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Group")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Judul")
                }
            }

            // Inputs for Hari / Tanggal
            OutlinedTextField(
                value = hariTanggal,
                onValueChange = { viewModel.hariTanggalInput.value = it },
                label = { Text("Hari / Tanggal") },
                placeholder = { Text("Contoh: Senin, 22-07-2026") },
                supportingText = { Text("Contoh format: Senin, 22-07-2026") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Inputs for Jam
            OutlinedTextField(
                value = jam,
                onValueChange = { viewModel.jamInput.value = it },
                label = { Text("Jam") },
                placeholder = { Text("Contoh: 21:00") },
                supportingText = { Text("Contoh format: 21:00") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Shift Kerja
            OutlinedTextField(
                value = shift,
                onValueChange = { viewModel.shiftInput.value = it },
                label = { Text("Shift Kerja") },
                placeholder = { Text("Shift 1 / Shift Malam / dll") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Nama Petugas Jaga
            OutlinedTextField(
                value = petugas,
                onValueChange = { viewModel.petugasInput.value = it },
                label = { Text("Nama Petugas Jaga") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Uraian Kegiatan (multiline)
            OutlinedTextField(
                value = uraian,
                onValueChange = { viewModel.uraianInput.value = it },
                label = { Text("Uraian Kegiatan") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )

            // Keterangan Tambahan
            OutlinedTextField(
                value = keterangan,
                onValueChange = { viewModel.keteranganInput.value = it },
                label = { Text("Keterangan Tambahan") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Signature Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // TTD PJT Button
                val isPjtRecorded = !ttdPjtPath.isNullOrEmpty() && File(ttdPjtPath!!).exists()
                Button(
                    onClick = {
                        signatureTypeTarget = "PJT"
                        isSignatureDialogOpen = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPjtRecorded) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isPjtRecorded) Icons.Default.CheckCircle else Icons.Default.Edit,
                        contentDescription = "Signature Status"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPjtRecorded) "PJT: Terekam ✓" else "Tanda Tangan PJT",
                        maxLines = 1
                    )
                }

                // TTD Petugas Button
                val isPetugasRecorded = !ttdPetugasPath.isNullOrEmpty() && File(ttdPetugasPath!!).exists()
                Button(
                    onClick = {
                        signatureTypeTarget = "Petugas"
                        isSignatureDialogOpen = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPetugasRecorded) Color(0xFF2E7D32) else MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isPetugasRecorded) Icons.Default.CheckCircle else Icons.Default.Edit,
                        contentDescription = "Signature Status"
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isPetugasRecorded) "Petugas: Terekam ✓" else "Tanda Tangan Petugas",
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Lower Action Buttons (Save & Navigate to Data Table)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        viewModel.saveOrUpdatePatrol { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .weight(1.1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Data")
                }

                Button(
                    onClick = onNavigateToData,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00796B)
                    )
                ) {
                    Text("Lihat Tabel Data >")
                }
            }

            if (isEditMode != null) {
                OutlinedButton(
                    onClick = { viewModel.clearForm() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Batal Edit / Bersihkan")
                }
            }
        }
    }

    // --- DIALOGS ---

    // 1. New Group Creation Dialog
    if (isNewGroupDialogOpen) {
        var groupNameInput by remember { mutableStateOf("") }
        var groupDescInput by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { isNewGroupDialogOpen = false },
            title = { Text("Tambah Judul / Grup Baru") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = groupNameInput,
                        onValueChange = { groupNameInput = it },
                        label = { Text("Nama Judul") },
                        placeholder = { Text("Contoh: Patroli Gedung A") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = groupDescInput,
                        onValueChange = { groupDescInput = it },
                        label = { Text("Keterangan") },
                        placeholder = { Text("Opsional") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createGroup(groupNameInput, groupDescInput) { success, msg ->
                            if (success) {
                                // Find created group to select it
                                val createdId = msg.toIntOrNull()
                                val created = groups.find { it.id == createdId } ?: GrupPatroli(id = createdId ?: 0, namaGrup = groupNameInput, keterangan = groupDescInput)
                                viewModel.selectedGroup.value = created
                                isNewGroupDialogOpen = false
                                Toast.makeText(context, "Grup Berhasil Ditambahkan", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { isNewGroupDialogOpen = false }) {
                    Text("Batal")
                }
            }
        )
    }

    // 2. Interactive Signature Capture Dialog
    if (isSignatureDialogOpen) {
        var clearCanvasTrigger by remember { mutableStateOf(0) }
        var currentDrawingBitmap by remember { mutableStateOf<Bitmap?>(null) }

        AlertDialog(
            onDismissRequest = { isSignatureDialogOpen = false },
            title = { Text("Tanda Tangan $signatureTypeTarget (Gunakan Jari)") },
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
                            viewModel.saveSignature(context, drawing, signatureTypeTarget) { path ->
                                if (signatureTypeTarget == "PJT") {
                                    viewModel.ttdPjtPath.value = path
                                } else {
                                    viewModel.ttdPetugasPath.value = path
                                }
                                isSignatureDialogOpen = false
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
                    TextButton(onClick = { isSignatureDialogOpen = false }) {
                        Text("Batal")
                    }
                }
            }
        )
    }
}

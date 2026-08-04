package com.petmed.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import android.view.WindowManager
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import com.petmed.app.ui.components.WheelDatePickerDialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petmed.app.data.model.Pet
import com.petmed.app.data.repository.OwnerStore
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.CreamDark
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.White
import com.petmed.app.ui.viewmodel.PetViewModel

@Composable
fun ProfileScreen(
    petViewModel: PetViewModel = viewModel()
) {
    val firstPet by petViewModel.firstPet.collectAsState()
    var showDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Orange
            ) {
                Icon(
                    if (firstPet == null) Icons.Default.Add else Icons.Default.Edit,
                    contentDescription = if (firstPet == null) "新增寵物" else "編輯寵物",
                    tint = White
                )
            }
        },
        containerColor = Cream
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Orange)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text("我的寵物", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }

            if (firstPet == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("尚未建立寵物資料", color = BrownLight, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("點右下角 ＋ 新增", color = BrownLight, fontSize = 12.sp)
                    }
                }
            } else {
                PetInfoCard(pet = firstPet!!)
            }
        }
    }

    if (showDialog) {
        PetEditDialog(
            pet = firstPet,
            onConfirm = { pet ->
                petViewModel.insertOrUpdate(pet)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun PetInfoCard(pet: Pet) {
    val context = LocalContext.current
    val ownerName = OwnerStore.getName(context)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (ownerName.isNotBlank()) InfoRow(label = "主人名稱", value = ownerName)
        InfoRow(label = "名字", value = pet.name)
        InfoRow(label = "種類", value = pet.species)
        if (pet.breed.isNotBlank()) InfoRow(label = "品種", value = pet.breed)
        if (pet.birthDate.isNotBlank()) InfoRow(label = "生日", value = pet.birthDate)
        if (pet.weight.isNotBlank()) InfoRow(label = "體重", value = "${pet.weight} kg")
        if (pet.notes.isNotBlank()) InfoRow(label = "備註", value = pet.notes)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(White)
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, color = BrownLight, fontSize = 13.sp)
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = value, color = Brown, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PetEditDialog(
    pet: Pet?,
    onConfirm: (Pet) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var ownerName by remember { mutableStateOf(TextFieldValue(OwnerStore.getName(context))) }
    var name by remember { mutableStateOf(TextFieldValue(pet?.name ?: "")) }
    var species by remember { mutableStateOf(TextFieldValue(pet?.species ?: "")) }
    var breed by remember { mutableStateOf(TextFieldValue(pet?.breed ?: "")) }
    var birthDate by remember { mutableStateOf(pet?.birthDate ?: "") }
    var weight by remember { mutableStateOf(TextFieldValue(pet?.weight ?: "")) }
    var notes by remember { mutableStateOf(TextFieldValue(pet?.notes ?: "")) }
    var showBirthDatePicker by remember { mutableStateOf(false) }

    val (initYear, initMonth, initDay) = remember(birthDate) {
        val parts = birthDate.split("-")
        val cal = java.util.Calendar.getInstance()
        Triple(
            parts.getOrNull(0)?.toIntOrNull() ?: (cal.get(java.util.Calendar.YEAR) - 5),
            parts.getOrNull(1)?.toIntOrNull() ?: 1,
            parts.getOrNull(2)?.toIntOrNull() ?: 1
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (pet == null) "新增寵物" else "編輯寵物資料") },
        text = {
            val view = LocalView.current
            SideEffect {
                (view.parent as? DialogWindowProvider)?.window
                    ?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = ownerName,
                    onValueChange = { ownerName = it },
                    label = { Text("主人名稱") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名字 *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = species,
                    onValueChange = { species = it },
                    label = { Text("種類（狗/貓/兔/鳥/其他）*") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = breed,
                    onValueChange = { breed = it },
                    label = { Text("品種") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .border(1.dp, BrownLight.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .clickable { showBirthDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("生日", color = BrownLight, fontSize = 12.sp)
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = birthDate.ifBlank { "點擊選擇日期" },
                            color = if (birthDate.isBlank()) BrownLight.copy(alpha = 0.5f) else Brown,
                            fontSize = 14.sp
                        )
                    }
                    Text("▼", color = BrownLight, fontSize = 11.sp)
                }
                OutlinedTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = { Text("體重（kg）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("備註") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.text.isNotBlank() && species.text.isNotBlank()) {
                        OwnerStore.setName(context, ownerName.text.trim())
                        onConfirm(
                            Pet(
                                id = pet?.id ?: 0,
                                name = name.text.trim(),
                                species = species.text.trim(),
                                breed = breed.text.trim(),
                                birthDate = birthDate.trim(),
                                weight = weight.text.trim(),
                                notes = notes.text.trim()
                            )
                        )
                    }
                }
            ) { Text("儲存", color = Orange) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = BrownLight) }
        }
    )

    if (showBirthDatePicker) {
        WheelDatePickerDialog(
            initialYear = initYear,
            initialMonth = initMonth,
            initialDay = initDay,
            onConfirm = { y, m, d ->
                birthDate = "%04d-%02d-%02d".format(y, m, d)
                showBirthDatePicker = false
            },
            onDismiss = { showBirthDatePicker = false }
        )
    }
}

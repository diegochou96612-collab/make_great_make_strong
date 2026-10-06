package com.petmed.app.ui.screens

import android.view.WindowManager
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.petmed.app.data.model.Pet
import com.petmed.app.data.repository.OwnerStore
import com.petmed.app.ui.components.WheelDatePickerDialog
import com.petmed.app.ui.theme.Brown
import com.petmed.app.ui.theme.BrownLight
import com.petmed.app.ui.theme.Cream
import com.petmed.app.ui.theme.CreamDark
import com.petmed.app.ui.theme.Orange
import com.petmed.app.ui.theme.Red
import com.petmed.app.ui.theme.White
import com.petmed.app.ui.viewmodel.PetViewModel

@Composable
fun ProfileScreen(
    petViewModel: PetViewModel = viewModel()
) {
    val context = LocalContext.current
    val allPets by petViewModel.allPets.collectAsState()
    var ownerName by remember { mutableStateOf(OwnerStore.getName(context)) }

    var showOwnerEditDialog by remember { mutableStateOf(false) }
    var showAddPetDialog by remember { mutableStateOf(false) }
    var editingPet by remember { mutableStateOf<Pet?>(null) }
    var deletingPet by remember { mutableStateOf<Pet?>(null) }

    Scaffold(
        floatingActionButton = {
            if (ownerName.isNotBlank()) {
                FloatingActionButton(
                    onClick = { showAddPetDialog = true },
                    containerColor = Orange
                ) {
                    Icon(Icons.Default.Add, contentDescription = "新增寵物", tint = White)
                }
            }
        },
        containerColor = Cream
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            // 頂部標題
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Orange)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text("個人資料", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }

            // 主人資料卡片
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(White)
                        .padding(20.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 頭像圓圈
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Orange.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (ownerName.isNotBlank()) ownerName.take(1) else "?",
                                color = Orange,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("主人", color = BrownLight, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (ownerName.isNotBlank()) ownerName else "尚未設定",
                                color = if (ownerName.isNotBlank()) Brown else BrownLight,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        IconButton(onClick = { showOwnerEditDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "編輯主人名稱", tint = BrownLight)
                        }
                    }
                }
            }

            // 需要先輸入主人名稱的提示
            if (ownerName.isBlank()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("請先輸入主人名稱", color = BrownLight, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("才能開始新增寵物資料", color = BrownLight, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Orange)
                                    .clickable { showOwnerEditDialog = true }
                                    .padding(horizontal = 24.dp, vertical = 10.dp)
                            ) {
                                Text("設定名稱", color = White, fontSize = 14.sp)
                            }
                        }
                    }
                }
            } else {
                // 寵物區段標題
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("我的寵物", color = BrownLight, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        if (allPets.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Orange.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("${allPets.size}", color = Orange, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                if (allPets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("尚未新增寵物", color = BrownLight, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("點右下角 ＋ 新增", color = BrownLight, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(allPets) { pet ->
                        PetCard(
                            pet = pet,
                            onEdit = { editingPet = pet },
                            onDelete = { deletingPet = pet }
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    // 主人名稱編輯
    if (showOwnerEditDialog) {
        OwnerNameDialog(
            current = ownerName,
            onConfirm = { name ->
                OwnerStore.setName(context, name)
                ownerName = name
                showOwnerEditDialog = false
            },
            onDismiss = { showOwnerEditDialog = false }
        )
    }

    // 新增寵物
    if (showAddPetDialog) {
        PetEditDialog(
            pet = null,
            onConfirm = { pet ->
                petViewModel.insertOrUpdate(pet)
                showAddPetDialog = false
            },
            onDismiss = { showAddPetDialog = false }
        )
    }

    // 編輯寵物
    if (editingPet != null) {
        PetEditDialog(
            pet = editingPet,
            onConfirm = { pet ->
                petViewModel.insertOrUpdate(pet)
                editingPet = null
            },
            onDismiss = { editingPet = null }
        )
    }

    // 刪除確認
    if (deletingPet != null) {
        AlertDialog(
            onDismissRequest = { deletingPet = null },
            title = { Text("刪除 ${deletingPet!!.name}？") },
            text = { Text("確定要刪除這筆寵物資料嗎？此操作無法復原。", color = Brown, fontSize = 14.sp) },
            confirmButton = {
                TextButton(onClick = {
                    petViewModel.delete(deletingPet!!)
                    deletingPet = null
                }) { Text("刪除", color = Red) }
            },
            dismissButton = {
                TextButton(onClick = { deletingPet = null }) { Text("取消", color = BrownLight) }
            }
        )
    }
}

@Composable
private fun PetCard(
    pet: Pet,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "arrow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(White)
            .animateContentSize()
    ) {
        Column {
            // 標題列（永遠顯示，點擊展開/收合）
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Orange.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(pet.species, color = Orange, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = pet.name,
                    color = Brown,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "收合" else "展開",
                    tint = BrownLight,
                    modifier = Modifier
                        .size(22.dp)
                        .rotate(arrowRotation)
                )
            }

            // 展開內容
            if (expanded) {
                HorizontalDivider(color = CreamDark, modifier = Modifier.padding(horizontal = 20.dp))
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pet.breed.isNotBlank()) PetInfoRow("品種", pet.breed)
                    if (pet.birthDate.isNotBlank()) PetInfoRow("生日", pet.birthDate)
                    if (pet.weight.isNotBlank()) PetInfoRow("體重", "${pet.weight} kg")
                    if (pet.notes.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Cream)
                                .padding(10.dp)
                        ) {
                            Text(pet.notes, color = BrownLight, fontSize = 13.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Orange.copy(alpha = 0.1f))
                                .clickable(onClick = onEdit)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("編輯", color = Orange, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onDelete)
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text("刪除", color = Red.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PetInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = BrownLight, fontSize = 13.sp)
        Text(value, color = Brown, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun OwnerNameDialog(
    current: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(TextFieldValue(current)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("主人名稱") },
        text = {
            val view = LocalView.current
            SideEffect {
                (view.parent as? DialogWindowProvider)?.window
                    ?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名稱") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = {
                if (name.text.isNotBlank()) onConfirm(name.text.trim())
            }) { Text("儲存", color = Orange) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消", color = BrownLight) }
        }
    )
}

@Composable
private fun PetEditDialog(
    pet: Pet?,
    onConfirm: (Pet) -> Unit,
    onDismiss: () -> Unit
) {
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
        title = { Text(if (pet == null) "新增寵物" else "編輯 ${pet.name}") },
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
            ) { Text(if (pet == null) "新增" else "儲存", color = Orange) }
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

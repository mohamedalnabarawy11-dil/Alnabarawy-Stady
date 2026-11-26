package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.SoundEngine
import com.example.data.ai.GeminiStudyRepository
import com.example.data.local.BusySlotEntity
import com.example.data.local.ScheduleSessionEntity
import com.example.data.local.SubjectPrefEntity
import com.example.i18n.AppLanguage
import com.example.i18n.AppStrings
import com.example.ui.ScheduleUiState
import com.example.ui.components.LuxuryGoldButton
import com.example.ui.components.LuxuryPressableBox
import com.example.ui.components.LuxurySectionCard
import com.example.ui.components.TeacherLoadingCard
import com.example.ui.theme.LocalNabraawyColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartScheduleScreen(
    state: ScheduleUiState,
    busySlots: List<BusySlotEntity>,
    subjects: List<SubjectPrefEntity>,
    sessions: List<ScheduleSessionEntity>,
    language: AppLanguage,
    strings: AppStrings,
    soundEngine: SoundEngine,
    onAddBusySlot: (String, Int, Int) -> Unit,
    onDeleteBusySlot: (Long) -> Unit,
    onToggleSelectAll: (Boolean) -> Unit,
    onToggleSubjectSelected: (SubjectPrefEntity) -> Unit,
    onUpdateSubjectDifficulty: (SubjectPrefEntity, String) -> Unit,
    onAddCustomSubject: (String, String) -> Unit,
    onDeleteSubject: (Long) -> Unit,
    onUpdateStudyWindow: (Int, Int) -> Unit,
    onGenerateSchedule: () -> Unit,
    onToggleSessionCompleted: (ScheduleSessionEntity) -> Unit
) {
    val palette = LocalNabraawyColors.current
    var showBusyDialog by remember { mutableStateOf(false) }
    var newSubjectName by remember { mutableStateOf("") }
    var newSubjectDiff by remember { mutableStateOf("MEDIUM") }

    if (showBusyDialog) {
        AddBusySlotDialog(
            language = language,
            strings = strings,
            soundEngine = soundEngine,
            onDismiss = { showBusyDialog = false },
            onConfirm = { label, startMin, endMin ->
                onAddBusySlot(label, startMin, endMin)
                showBusyDialog = false
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.screenBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Busy Time Slots (Excluded Completely)
        item {
            Box(modifier = Modifier.widthIn(max = 760.dp)) {
                LuxurySectionCard(
                    title = strings.busySlotsTitle,
                    icon = Icons.Filled.EventBusy
                ) {
                    Text(
                        text = strings.busySlotsSubtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.secondaryText
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LuxuryGoldButton(
                            text = strings.addBusySlotBtn,
                            icon = Icons.Filled.Add,
                            onClick = { showBusyDialog = true },
                            soundEngine = soundEngine,
                            isPrimary = false
                        )
                        LuxuryGoldButton(
                            text = strings.presetSleep,
                            onClick = { onAddBusySlot(strings.presetSleep, 23 * 60, 7 * 60) },
                            soundEngine = soundEngine,
                            isPrimary = false
                        )
                        LuxuryGoldButton(
                            text = strings.presetSchool,
                            onClick = { onAddBusySlot(strings.presetSchool, 8 * 60, 14 * 60) },
                            soundEngine = soundEngine,
                            isPrimary = false
                        )
                    }

                    if (busySlots.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            busySlots.forEach { slot ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.elevatedCardBg,
                                    border = BorderStroke(1.dp, palette.cardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = slot.label,
                                                style = MaterialTheme.typography.titleMedium,
                                                color = palette.primaryText
                                            )
                                            Text(
                                                text = "${GeminiStudyRepository.formatMinutes(slot.startMinutes, language)} - ${GeminiStudyRepository.formatMinutes(slot.endMinutes, language)}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = palette.gold
                                            )
                                        }
                                        IconButton(onClick = {
                                            soundEngine.playClick()
                                            onDeleteBusySlot(slot.id)
                                        }) {
                                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = palette.secondaryText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Subjects & Difficulty Rating + Study Window
        item {
            Box(modifier = Modifier.widthIn(max = 760.dp)) {
                LuxurySectionCard(
                    title = strings.subjectsTitle,
                    icon = Icons.Filled.MenuBook
                ) {
                    // All Subjects Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = strings.allSubjectsLabel,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = palette.gold
                        )
                        Switch(
                            checked = state.selectAllSubjects,
                            onCheckedChange = {
                                soundEngine.playClick()
                                onToggleSelectAll(it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = palette.screenBg,
                                checkedTrackColor = palette.gold
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    subjects.forEach { subj ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = palette.elevatedCardBg,
                            border = BorderStroke(
                                1.dp,
                                if (subj.isSelected) palette.goldBorder else palette.cardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Checkbox(
                                            checked = subj.isSelected,
                                            onCheckedChange = {
                                                soundEngine.playClick()
                                                onToggleSubjectSelected(subj)
                                            },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = palette.gold,
                                                checkmarkColor = palette.screenBg
                                            )
                                        )
                                        Text(
                                            text = subj.name,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.primaryText
                                        )
                                    }
                                    IconButton(onClick = {
                                        soundEngine.playClick()
                                        onDeleteSubject(subj.id)
                                    }) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = palette.secondaryText)
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        "EASY" to strings.diffEasy,
                                        "MEDIUM" to strings.diffMedium,
                                        "HARD" to strings.diffHard
                                    ).forEach { (key, label) ->
                                        val active = subj.difficulty == key
                                        LuxuryPressableBox(
                                            onClick = { onUpdateSubjectDifficulty(subj, key) },
                                            soundEngine = soundEngine,
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .background(
                                                    if (active) palette.gold else palette.cardBg,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (active) palette.gold else palette.cardBorder,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .padding(vertical = 7.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = if (active) palette.screenBg else palette.primaryText,
                                                modifier = Modifier.align(Alignment.Center)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Add Custom Subject
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newSubjectName,
                            onValueChange = { newSubjectName = it },
                            placeholder = { Text(strings.addSubjectHint) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = palette.gold,
                                unfocusedBorderColor = palette.cardBorder,
                                focusedTextColor = palette.primaryText,
                                unfocusedTextColor = palette.primaryText
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        LuxuryGoldButton(
                            text = strings.addSubjectBtn,
                            icon = Icons.Filled.Add,
                            onClick = {
                                if (newSubjectName.isNotBlank()) {
                                    onAddCustomSubject(newSubjectName, newSubjectDiff)
                                    newSubjectName = ""
                                }
                            },
                            soundEngine = soundEngine,
                            isPrimary = false
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Study Window Selector
                    Text(
                        text = strings.studyWindowTitle,
                        style = MaterialTheme.typography.labelLarge,
                        color = palette.gold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TimeStepperBox(
                            valueText = GeminiStudyRepository.formatMinutes(state.studyWindowStartMinutes, language),
                            onDecrement = {
                                onUpdateStudyWindow((state.studyWindowStartMinutes - 60).coerceAtLeast(5 * 60), state.studyWindowEndMinutes)
                            },
                            onIncrement = {
                                onUpdateStudyWindow((state.studyWindowStartMinutes + 60).coerceAtMost(state.studyWindowEndMinutes - 60), state.studyWindowEndMinutes)
                            },
                            soundEngine = soundEngine,
                            modifier = Modifier.weight(1f)
                        )
                        TimeStepperBox(
                            valueText = GeminiStudyRepository.formatMinutes(state.studyWindowEndMinutes, language),
                            onDecrement = {
                                onUpdateStudyWindow(state.studyWindowStartMinutes, (state.studyWindowEndMinutes - 60).coerceAtLeast(state.studyWindowStartMinutes + 60))
                            },
                            onIncrement = {
                                onUpdateStudyWindow(state.studyWindowStartMinutes, (state.studyWindowEndMinutes + 60).coerceAtMost(23 * 60 + 30))
                            },
                            soundEngine = soundEngine,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (!state.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(state.errorMessage, color = palette.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    LuxuryGoldButton(
                        text = strings.generateScheduleBtn,
                        icon = Icons.Filled.AutoAwesome,
                        onClick = onGenerateSchedule,
                        soundEngine = soundEngine,
                        enabled = !state.isGenerating,
                        isPrimary = true,
                        testTagName = "generate_schedule_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. Generated Calm Schedule
        if (state.isGenerating) {
            item {
                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                    TeacherLoadingCard(
                        message = strings.teacherThinking,
                        retryingMessage = if (state.isRetrying) strings.retryingConnection else null
                    )
                }
            }
        } else if (sessions.isEmpty()) {
            item {
                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                    LuxurySectionCard(title = strings.tabSchedule, icon = Icons.Filled.CalendarMonth) {
                        Text(
                            text = strings.scheduleEmpty,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.secondaryText
                        )
                    }
                }
            }
        } else {
            items(sessions, key = { it.id }) { session ->
                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                    ScheduleSessionCard(
                        session = session,
                        strings = strings,
                        soundEngine = soundEngine,
                        onToggleCompleted = { onToggleSessionCompleted(session) }
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ScheduleSessionCard(
    session: ScheduleSessionEntity,
    strings: AppStrings,
    soundEngine: SoundEngine,
    onToggleCompleted: () -> Unit
) {
    val palette = LocalNabraawyColors.current
    val bgColor = when {
        session.isBreak -> palette.contrastWhiteCardBg
        session.isCompleted -> palette.successBg
        else -> palette.cardBg
    }
    val titleColor = if (session.isBreak) palette.contrastWhiteText else palette.primaryText
    val diffBadge = when (session.difficulty) {
        "HARD" -> strings.diffHard
        "MEDIUM" -> strings.diffMedium
        "EASY" -> strings.diffEasy
        else -> strings.breakBadge
    }

    LuxuryPressableBox(
        onClick = onToggleCompleted,
        soundEngine = soundEngine,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(18.dp))
            .border(
                1.dp,
                if (session.isCompleted) palette.success else palette.goldBorder,
                RoundedCornerShape(18.dp)
            )
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.gold.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (session.isBreak) Icons.Filled.Coffee else Icons.Filled.Schedule,
                        contentDescription = session.subjectName,
                        tint = palette.gold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "${session.startTime} - ${session.endTime}",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = palette.gold
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = palette.gold.copy(alpha = 0.18f)
                        ) {
                            Text(
                                text = "$diffBadge • ${session.durationMinutes}m",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.gold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = session.subjectName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = titleColor
                    )
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (session.isBreak) palette.contrastWhiteText.copy(alpha = 0.8f) else palette.secondaryText
                    )
                }
            }
            Icon(
                imageVector = if (session.isCompleted) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = "Complete",
                tint = if (session.isCompleted) palette.success else palette.gold,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
private fun TimeStepperBox(
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    soundEngine: SoundEngine,
    modifier: Modifier = Modifier
) {
    val palette = LocalNabraawyColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = palette.elevatedCardBg,
        border = BorderStroke(1.dp, palette.goldBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LuxuryPressableBox(
                onClick = onDecrement,
                soundEngine = soundEngine,
                shape = CircleShape,
                modifier = Modifier
                    .size(34.dp)
                    .background(palette.cardBg, CircleShape)
            ) {
                Text("-", style = MaterialTheme.typography.titleMedium, color = palette.gold, modifier = Modifier.align(Alignment.Center))
            }
            Text(
                text = valueText,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = palette.primaryText
            )
            LuxuryPressableBox(
                onClick = onIncrement,
                soundEngine = soundEngine,
                shape = CircleShape,
                modifier = Modifier
                    .size(34.dp)
                    .background(palette.cardBg, CircleShape)
            ) {
                Text("+", style = MaterialTheme.typography.titleMedium, color = palette.gold, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
private fun AddBusySlotDialog(
    language: AppLanguage,
    strings: AppStrings,
    soundEngine: SoundEngine,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, Int) -> Unit
) {
    val palette = LocalNabraawyColors.current
    var label by remember { mutableStateOf("") }
    var startMin by remember { mutableIntStateOf(16 * 60) }
    var endMin by remember { mutableIntStateOf(18 * 60) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = palette.cardBg,
        title = {
            Text(strings.addBusySlotBtn, color = palette.gold, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    placeholder = { Text(if (language == AppLanguage.AR) "مثال: درس خصوصي / راحة" else "e.g. Tutoring / Rest") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                TimeStepperBox(
                    valueText = GeminiStudyRepository.formatMinutes(startMin, language),
                    onDecrement = { startMin = (startMin - 30).coerceAtLeast(0) },
                    onIncrement = { startMin = (startMin + 30).coerceAtMost(23 * 60) },
                    soundEngine = soundEngine
                )
                TimeStepperBox(
                    valueText = GeminiStudyRepository.formatMinutes(endMin, language),
                    onDecrement = { endMin = (endMin - 30).coerceAtLeast(0) },
                    onIncrement = { endMin = (endMin + 30).coerceAtMost(23 * 60 + 30) },
                    soundEngine = soundEngine
                )
            }
        },
        confirmButton = {
            LuxuryGoldButton(
                text = strings.addSubjectBtn,
                onClick = {
                    val safeLabel = label.ifBlank {
                        if (language == AppLanguage.AR) "موعد مشغول" else "Busy Slot"
                    }
                    onConfirm(safeLabel, startMin, endMin)
                },
                soundEngine = soundEngine
            )
        },
        dismissButton = {
            LuxuryGoldButton(
                text = strings.cancelBtn,
                onClick = onDismiss,
                soundEngine = soundEngine,
                isPrimary = false
            )
        }
    )
}

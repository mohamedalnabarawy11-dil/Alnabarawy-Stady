package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.audio.SoundEngine
import com.example.data.ai.ExamDifficulty
import com.example.data.ai.ExamMode
import com.example.data.ai.ExamType
import com.example.data.local.FlashcardEntity
import com.example.data.local.MistakeEntity
import com.example.i18n.AppStrings
import com.example.ui.ExamSubTab
import com.example.ui.ExamUiState
import com.example.ui.components.CameraCaptureModal
import com.example.ui.components.LuxuryGoldButton
import com.example.ui.components.LuxuryPressableBox
import com.example.ui.components.LuxurySectionCard
import com.example.ui.components.TeacherLoadingCard
import com.example.ui.theme.LocalNabraawyColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExamRecitationScreen(
    state: ExamUiState,
    mistakes: List<MistakeEntity>,
    flashcards: List<FlashcardEntity>,
    strings: AppStrings,
    soundEngine: SoundEngine,
    onSubTabChange: (ExamSubTab) -> Unit,
    onSourceTextChange: (String) -> Unit,
    onBitmapSelected: (Bitmap?) -> Unit,
    onImportLastLesson: () -> Unit,
    onModeChange: (ExamMode) -> Unit,
    onTypeChange: (ExamType) -> Unit,
    onDifficultyChange: (ExamDifficulty) -> Unit,
    onCountChange: (Int) -> Unit,
    onTotalGradeChange: (Int) -> Unit,
    onTimerChange: (Int) -> Unit,
    onGenerateExam: () -> Unit,
    onGenerateFlashcards: () -> Unit,
    onAnswerChange: (Int, String) -> Unit,
    onSubmitFullExam: () -> Unit,
    onCheckRecitation: () -> Unit,
    onNextRecitation: () -> Unit,
    onResetExam: () -> Unit,
    onStartMistakeDrill: (MistakeEntity) -> Unit,
    onMistakeDrillAnswerChange: (String) -> Unit,
    onSubmitMistakeDrill: () -> Unit,
    onCloseMistakeDrill: () -> Unit,
    onDeleteMistake: (Long) -> Unit,
    onToggleFlashcardMastered: (FlashcardEntity) -> Unit,
    onDeleteFlashcard: (Long) -> Unit
) {
    val palette = LocalNabraawyColors.current
    val context = LocalContext.current
    var showCamera by remember { mutableStateOf(false) }

    if (state.subTab != ExamSubTab.CREATE_OR_TAKE) {
        BackHandler { onSubTabChange(ExamSubTab.CREATE_OR_TAKE) }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
                } else {
                    @Suppress("DEPRECATION")
                    MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                }
                onBitmapSelected(bmp)
            } catch (_: Exception) {
            }
        }
    }

    if (showCamera) {
        CameraCaptureModal(
            strings = strings,
            soundEngine = soundEngine,
            onCaptured = { bmp ->
                onBitmapSelected(bmp)
                showCamera = false
            },
            onDismiss = { showCamera = false }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.screenBg),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Sub-tabs: Exam & Quiz | Mistake Bank | Flashcards
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 760.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.cardBg)
                    .border(1.dp, palette.goldBorder, RoundedCornerShape(18.dp))
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SubTabPill(
                    title = strings.examSubTabCreate,
                    selected = state.subTab == ExamSubTab.CREATE_OR_TAKE,
                    soundEngine = soundEngine,
                    onClick = { onSubTabChange(ExamSubTab.CREATE_OR_TAKE) },
                    modifier = Modifier.weight(1f)
                )
                SubTabPill(
                    title = "${strings.examSubTabMistakes} (${mistakes.size})",
                    selected = state.subTab == ExamSubTab.MISTAKE_BANK,
                    soundEngine = soundEngine,
                    onClick = { onSubTabChange(ExamSubTab.MISTAKE_BANK) },
                    modifier = Modifier.weight(1f)
                )
                SubTabPill(
                    title = "${strings.examSubTabFlashcards} (${flashcards.size})",
                    selected = state.subTab == ExamSubTab.FLASHCARDS,
                    soundEngine = soundEngine,
                    onClick = { onSubTabChange(ExamSubTab.FLASHCARDS) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (state.subTab) {
            ExamSubTab.CREATE_OR_TAKE -> {
                if (state.isGenerating) {
                    item {
                        Box(modifier = Modifier.widthIn(max = 760.dp)) {
                            TeacherLoadingCard(
                                message = strings.teacherThinking,
                                retryingMessage = if (state.isRetrying) strings.retryingConnection else null
                            )
                        }
                    }
                } else if (state.questions.isEmpty()) {
                    // Exam Configurator
                    item {
                        Box(modifier = Modifier.widthIn(max = 760.dp)) {
                            LuxurySectionCard(
                                title = strings.examSubTabCreate,
                                icon = Icons.Filled.Quiz
                            ) {
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    LuxuryGoldButton(
                                        text = strings.cameraBtn,
                                        icon = Icons.Filled.CameraAlt,
                                        onClick = { showCamera = true },
                                        soundEngine = soundEngine,
                                        isPrimary = false
                                    )
                                    LuxuryGoldButton(
                                        text = strings.galleryBtn,
                                        icon = Icons.Filled.PhotoLibrary,
                                        onClick = {
                                            photoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        soundEngine = soundEngine,
                                        isPrimary = false
                                    )
                                    LuxuryGoldButton(
                                        text = strings.importLastLesson,
                                        icon = Icons.Filled.FileDownload,
                                        onClick = onImportLastLesson,
                                        soundEngine = soundEngine,
                                        isPrimary = false
                                    )
                                }

                                if (state.attachedBitmap != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(1.dp, palette.goldBorder, RoundedCornerShape(14.dp))
                                    ) {
                                        Image(
                                            bitmap = state.attachedBitmap.asImageBitmap(),
                                            contentDescription = strings.cameraBtn,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = {
                                                soundEngine.playClick()
                                                onBitmapSelected(null)
                                            },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(palette.screenBg.copy(alpha = 0.85f))
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = strings.removeImage, tint = palette.gold)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = state.sourceText,
                                    onValueChange = onSourceTextChange,
                                    placeholder = {
                                        Text(
                                            text = strings.examSourceHint,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.secondaryText
                                        )
                                    },
                                    minLines = 3,
                                    maxLines = 6,
                                    shape = RoundedCornerShape(14.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = palette.gold,
                                        unfocusedBorderColor = palette.cardBorder,
                                        focusedTextColor = palette.primaryText,
                                        unfocusedTextColor = palette.primaryText,
                                        cursorColor = palette.gold
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("exam_source_input")
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                // Mode: Full Exam vs Question by Question
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SelectableChip(
                                        label = strings.examModeFull,
                                        selected = state.examMode == ExamMode.FULL_EXAM,
                                        soundEngine = soundEngine,
                                        onClick = { onModeChange(ExamMode.FULL_EXAM) },
                                        modifier = Modifier.weight(1f)
                                    )
                                    SelectableChip(
                                        label = strings.examModeRecitation,
                                        selected = state.examMode == ExamMode.QUESTION_BY_QUESTION,
                                        soundEngine = soundEngine,
                                        onClick = { onModeChange(ExamMode.QUESTION_BY_QUESTION) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(strings.examTypeLabel, style = MaterialTheme.typography.labelLarge, color = palette.gold)
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        ExamType.MCQ to strings.typeMcq,
                                        ExamType.TRUE_FALSE to strings.typeTrueFalse,
                                        ExamType.SHORT_ESSAY to strings.typeEssay,
                                        ExamType.MIXED to strings.typeMixed
                                    ).forEach { (type, label) ->
                                        SelectableChip(
                                            label = label,
                                            selected = state.examType == type,
                                            soundEngine = soundEngine,
                                            onClick = { onTypeChange(type) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(strings.difficultyLabel, style = MaterialTheme.typography.labelLarge, color = palette.gold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(
                                        ExamDifficulty.EASY to strings.diffEasy,
                                        ExamDifficulty.MEDIUM to strings.diffMedium,
                                        ExamDifficulty.HARD to strings.diffHard
                                    ).forEach { (diff, label) ->
                                        SelectableChip(
                                            label = label,
                                            selected = state.difficulty == diff,
                                            soundEngine = soundEngine,
                                            onClick = { onDifficultyChange(diff) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(strings.questionCountLabel, style = MaterialTheme.typography.labelLarge, color = palette.gold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(5, 10, 15, 20).forEach { cnt ->
                                        SelectableChip(
                                            label = "$cnt",
                                            selected = state.questionCount == cnt,
                                            soundEngine = soundEngine,
                                            onClick = { onCountChange(cnt) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                Text(strings.finalGradeLabel, style = MaterialTheme.typography.labelLarge, color = palette.gold)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf(10, 20, 50, 100).forEach { grade ->
                                        SelectableChip(
                                            label = "$grade",
                                            selected = state.totalGrade == grade,
                                            soundEngine = soundEngine,
                                            onClick = { onTotalGradeChange(grade) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                if (state.examMode == ExamMode.FULL_EXAM) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(strings.timerLabel, style = MaterialTheme.typography.labelLarge, color = palette.gold)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf(0, 5, 10, 15, 30).forEach { mins ->
                                            val label = if (mins == 0) strings.timerOff else "$mins ${strings.minutesUnit}"
                                            SelectableChip(
                                                label = label,
                                                selected = state.timerMinutes == mins,
                                                soundEngine = soundEngine,
                                                onClick = { onTimerChange(mins) }
                                            )
                                        }
                                    }
                                }

                                if (!state.errorMessage.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(state.errorMessage, color = palette.error, style = MaterialTheme.typography.bodyMedium)
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                LuxuryGoldButton(
                                    text = strings.generateExamBtn,
                                    icon = Icons.Filled.AutoAwesome,
                                    onClick = onGenerateExam,
                                    soundEngine = soundEngine,
                                    isPrimary = true,
                                    testTagName = "generate_exam_button",
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                LuxuryGoldButton(
                                    text = strings.generateFlashcardsBtn,
                                    icon = Icons.Filled.Style,
                                    onClick = onGenerateFlashcards,
                                    soundEngine = soundEngine,
                                    isPrimary = false,
                                    testTagName = "generate_flashcards_button",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                } else {
                    // Active Exam or Recitation Session
                    item {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 760.dp)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.remainingSeconds > 0 && state.gradingReport == null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = palette.cardBg,
                                    border = BorderStroke(1.dp, palette.gold)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Filled.Timer, contentDescription = strings.timerLabel, tint = palette.gold, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        val mins = state.remainingSeconds / 60
                                        val secs = state.remainingSeconds % 60
                                        Text(
                                            text = String.format("%02d:%02d", mins, secs),
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.gold
                                        )
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.width(1.dp))
                            }
                            LuxuryGoldButton(
                                text = strings.newExamBtn,
                                icon = Icons.Filled.Refresh,
                                onClick = onResetExam,
                                soundEngine = soundEngine,
                                isPrimary = false
                            )
                        }
                    }

                    if (state.examMode == ExamMode.FULL_EXAM) {
                        val report = state.gradingReport
                        if (report != null) {
                            item {
                                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                                    LuxurySectionCard(
                                        title = strings.scoreBannerTitle,
                                        icon = Icons.Filled.AssignmentTurnedIn,
                                        useIvoryContrast = true
                                    ) {
                                        Text(
                                            text = "${report.totalEarned} / ${report.totalMax}",
                                            style = MaterialTheme.typography.displayLarge,
                                            color = palette.gold
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = report.overallTeacherNote,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = palette.contrastWhiteText
                                        )
                                        if (report.results.any { !it.isCorrect }) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text(
                                                text = strings.savedToMistakeBank,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = palette.goldDeep
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        items(state.questions, key = { it.index }) { q ->
                            val graded = state.gradingReport?.results?.firstOrNull { it.index == q.index }
                            Box(modifier = Modifier.widthIn(max = 760.dp)) {
                                LuxurySectionCard(
                                    title = "${q.index + 1} / ${state.questions.size}",
                                    accentBorder = graded == null || graded.isCorrect
                                ) {
                                    Text(
                                        text = q.questionText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.primaryText
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    val selectedAnswer = state.studentAnswers[q.index].orEmpty()
                                    if (q.options.isNotEmpty()) {
                                        q.options.forEach { opt ->
                                            val isSelected = selectedAnswer == opt
                                            LuxuryPressableBox(
                                                onClick = {
                                                    if (state.gradingReport == null) onAnswerChange(q.index, opt)
                                                },
                                                soundEngine = soundEngine,
                                                enabled = state.gradingReport == null,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .background(
                                                        if (isSelected) palette.gold.copy(alpha = 0.2f) else palette.elevatedCardBg,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (isSelected) palette.gold else palette.cardBorder,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = opt,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = if (isSelected) palette.gold else palette.primaryText
                                                )
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = selectedAnswer,
                                            onValueChange = { if (state.gradingReport == null) onAnswerChange(q.index, it) },
                                            enabled = state.gradingReport == null,
                                            placeholder = { Text(strings.answerHint) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }

                                    if (graded != null) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (graded.isCorrect) palette.successBg else palette.errorBg,
                                            border = BorderStroke(1.dp, if (graded.isCorrect) palette.success else palette.error)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = "${graded.earnedPoints} / ${graded.maxPoints}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = if (graded.isCorrect) palette.success else palette.error
                                                )
                                                if (!graded.isCorrect) {
                                                    Text(
                                                        text = "${strings.correctAnswerLabel}: ${graded.correctAnswer}",
                                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                        color = palette.primaryText
                                                    )
                                                }
                                                Text(
                                                    text = graded.teacherFeedback,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = palette.primaryText
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (state.gradingReport == null) {
                            item {
                                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                                    if (state.isGrading) {
                                        TeacherLoadingCard(
                                            message = strings.teacherThinking,
                                            retryingMessage = if (state.isRetrying) strings.retryingConnection else null
                                        )
                                    } else {
                                        LuxuryGoldButton(
                                            text = strings.submitExamBtn,
                                            icon = Icons.Filled.Check,
                                            onClick = onSubmitFullExam,
                                            soundEngine = soundEngine,
                                            isPrimary = true,
                                            testTagName = "submit_exam_button",
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Question-by-Question Recitation Mode ("سؤال بسؤال")
                        item {
                            Box(modifier = Modifier.widthIn(max = 760.dp)) {
                                if (state.recitationFinished) {
                                    val correctCount = state.recitationResults.count { it.isCorrect }
                                    LuxurySectionCard(
                                        title = strings.scoreBannerTitle,
                                        icon = Icons.Filled.AssignmentTurnedIn,
                                        useIvoryContrast = true
                                    ) {
                                        Text(
                                            text = "$correctCount / ${state.questions.size}",
                                            style = MaterialTheme.typography.displayLarge,
                                            color = palette.gold
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        LuxuryGoldButton(
                                            text = strings.newExamBtn,
                                            onClick = onResetExam,
                                            soundEngine = soundEngine,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else {
                                    val q = state.questions[state.currentRecitationIndex]
                                    val selectedAnswer = state.studentAnswers[q.index].orEmpty()
                                    LuxurySectionCard(
                                        title = "${state.currentRecitationIndex + 1} / ${state.questions.size}",
                                        icon = Icons.Filled.Quiz
                                    ) {
                                        Text(
                                            text = q.questionText,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.primaryText
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        if (q.options.isNotEmpty()) {
                                            q.options.forEach { opt ->
                                                val isSel = selectedAnswer == opt
                                                LuxuryPressableBox(
                                                    onClick = {
                                                        if (state.currentRecitationResult == null) onAnswerChange(q.index, opt)
                                                    },
                                                    soundEngine = soundEngine,
                                                    enabled = state.currentRecitationResult == null,
                                                    shape = RoundedCornerShape(12.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 4.dp)
                                                        .background(
                                                            if (isSel) palette.gold.copy(alpha = 0.2f) else palette.elevatedCardBg,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (isSel) palette.gold else palette.cardBorder,
                                                            RoundedCornerShape(12.dp)
                                                        )
                                                        .padding(12.dp)
                                                ) {
                                                    Text(
                                                        text = opt,
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = if (isSel) palette.gold else palette.primaryText
                                                    )
                                                }
                                            }
                                        } else {
                                            OutlinedTextField(
                                                value = selectedAnswer,
                                                onValueChange = { if (state.currentRecitationResult == null) onAnswerChange(q.index, it) },
                                                enabled = state.currentRecitationResult == null,
                                                placeholder = { Text(strings.answerHint) },
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))

                                        if (state.isGrading) {
                                            TeacherLoadingCard(message = strings.teacherThinking)
                                        } else if (state.currentRecitationResult == null) {
                                            LuxuryGoldButton(
                                                text = strings.checkAnswerBtn,
                                                onClick = onCheckRecitation,
                                                soundEngine = soundEngine,
                                                enabled = selectedAnswer.isNotBlank(),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        } else {
                                            val res = state.currentRecitationResult
                                            Surface(
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (res.isCorrect) palette.successBg else palette.errorBg,
                                                border = BorderStroke(1.dp, if (res.isCorrect) palette.success else palette.error)
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    if (!res.isCorrect) {
                                                        Text(
                                                            text = "${strings.correctAnswerLabel}: ${res.correctAnswer}",
                                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                            color = palette.primaryText
                                                        )
                                                    }
                                                    Text(
                                                        text = res.teacherFeedback,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = palette.primaryText
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            val isLast = state.currentRecitationIndex == state.questions.lastIndex
                                            LuxuryGoldButton(
                                                text = if (isLast) strings.finishRecitationBtn else strings.nextQuestionBtn,
                                                onClick = onNextRecitation,
                                                soundEngine = soundEngine,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            ExamSubTab.MISTAKE_BANK -> {
                val drill = state.drillMistake
                if (drill != null) {
                    item {
                        Box(modifier = Modifier.widthIn(max = 760.dp)) {
                            LuxurySectionCard(title = strings.retryMistakeBtn, icon = Icons.Filled.Quiz) {
                                Text(drill.question, style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = state.drillMistakeAnswer,
                                    onValueChange = onMistakeDrillAnswerChange,
                                    placeholder = { Text(strings.answerHint) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LuxuryGoldButton(
                                        text = strings.cancelBtn,
                                        onClick = onCloseMistakeDrill,
                                        soundEngine = soundEngine,
                                        isPrimary = false,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LuxuryGoldButton(
                                        text = strings.checkAnswerBtn,
                                        onClick = onSubmitMistakeDrill,
                                        soundEngine = soundEngine,
                                        enabled = state.drillMistakeAnswer.isNotBlank() && !state.isCheckingMistakeDrill,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                state.drillMistakeResult?.let { res ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = if (res.isCorrect) "✓ ${res.teacherFeedback}" else "✕ ${strings.correctAnswerLabel}: ${res.correctAnswer}\n${res.teacherFeedback}",
                                        color = if (res.isCorrect) palette.success else palette.error,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }

                if (mistakes.isEmpty()) {
                    item {
                        Box(modifier = Modifier.widthIn(max = 760.dp)) {
                            LuxurySectionCard(title = strings.examSubTabMistakes, icon = Icons.Filled.WarningAmber) {
                                Text(strings.mistakeBankEmpty, style = MaterialTheme.typography.bodyLarge, color = palette.secondaryText)
                            }
                        }
                    }
                } else {
                    items(mistakes, key = { it.id }) { m ->
                        Box(modifier = Modifier.widthIn(max = 760.dp)) {
                            LuxurySectionCard(title = m.subject, icon = Icons.Filled.WarningAmber) {
                                Text(m.question, style = MaterialTheme.typography.titleMedium, color = palette.primaryText)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("${strings.yourAnswerLabel}: ${m.studentAnswer}", style = MaterialTheme.typography.bodyMedium, color = palette.error)
                                Text("${strings.correctAnswerLabel}: ${m.correctAnswer}", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = palette.success)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(m.explanation, style = MaterialTheme.typography.bodyMedium, color = palette.secondaryText)
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    LuxuryGoldButton(
                                        text = strings.retryMistakeBtn,
                                        onClick = { onStartMistakeDrill(m) },
                                        soundEngine = soundEngine,
                                        isPrimary = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    LuxuryGoldButton(
                                        text = strings.masteredRemoveBtn,
                                        onClick = { onDeleteMistake(m.id) },
                                        soundEngine = soundEngine,
                                        isPrimary = false,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ExamSubTab.FLASHCARDS -> {
                if (flashcards.isEmpty()) {
                    item {
                        Box(modifier = Modifier.widthIn(max = 760.dp)) {
                            LuxurySectionCard(title = strings.examSubTabFlashcards, icon = Icons.Filled.Style) {
                                Text(strings.flashcardsEmpty, style = MaterialTheme.typography.bodyLarge, color = palette.secondaryText)
                            }
                        }
                    }
                } else {
                    items(flashcards, key = { it.id }) { card ->
                        Box(modifier = Modifier.widthIn(max = 760.dp)) {
                            FlipFlashcardItem(
                                card = card,
                                strings = strings,
                                soundEngine = soundEngine,
                                onToggleMastered = { onToggleFlashcardMastered(card) },
                                onDelete = { onDeleteFlashcard(card.id) }
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun SubTabPill(
    title: String,
    selected: Boolean,
    soundEngine: SoundEngine,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNabraawyColors.current
    LuxuryPressableBox(
        onClick = onClick,
        soundEngine = soundEngine,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .background(
                if (selected) palette.gold else palette.cardBg,
                RoundedCornerShape(14.dp)
            )
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = if (selected) palette.screenBg else palette.primaryText,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun SelectableChip(
    label: String,
    selected: Boolean,
    soundEngine: SoundEngine,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = LocalNabraawyColors.current
    LuxuryPressableBox(
        onClick = onClick,
        soundEngine = soundEngine,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .background(
                if (selected) palette.gold.copy(alpha = 0.22f) else palette.elevatedCardBg,
                RoundedCornerShape(12.dp)
            )
            .border(
                1.2.dp,
                if (selected) palette.gold else palette.cardBorder,
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = if (selected) palette.gold else palette.primaryText,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun FlipFlashcardItem(
    card: FlashcardEntity,
    strings: AppStrings,
    soundEngine: SoundEngine,
    onToggleMastered: () -> Unit,
    onDelete: () -> Unit
) {
    val palette = LocalNabraawyColors.current
    val density = LocalDensity.current.density
    var flipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (flipped) 180f else 0f,
        animationSpec = tween(420),
        label = "cardFlip"
    )
    val showingFront = rotation <= 90f

    LuxuryPressableBox(
        onClick = { flipped = !flipped },
        soundEngine = soundEngine,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                rotationY = rotation
                cameraDistance = 12f * density
            }
            .background(
                if (showingFront) palette.cardBg else palette.contrastWhiteCardBg,
                RoundedCornerShape(20.dp)
            )
            .border(1.2.dp, palette.goldBorder, RoundedCornerShape(20.dp))
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    if (!showingFront) rotationY = 180f
                }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showingFront) card.deckTitle else strings.correctAnswerLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.gold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LuxuryPressableBox(
                        onClick = onToggleMastered,
                        soundEngine = soundEngine,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .background(
                                if (card.isMastered) palette.successBg else palette.elevatedCardBg,
                                RoundedCornerShape(10.dp)
                            )
                            .border(
                                1.dp,
                                if (card.isMastered) palette.success else palette.goldBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (card.isMastered) Icons.Filled.Check else Icons.Filled.BookmarkBorder,
                                contentDescription = strings.cardMastered,
                                tint = if (card.isMastered) palette.success else palette.gold,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (card.isMastered) strings.cardMastered else strings.cardNeedsReview,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (card.isMastered) palette.success else palette.gold
                            )
                        }
                    }
                    IconButton(onClick = {
                        soundEngine.playClick()
                        onDelete()
                    }) {
                        Icon(Icons.Filled.DeleteOutline, contentDescription = "Delete", tint = palette.secondaryText)
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = if (showingFront) card.front else card.back,
                style = MaterialTheme.typography.titleLarge,
                color = if (showingFront) palette.primaryText else palette.contrastWhiteText
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.tapCardToFlip,
                style = MaterialTheme.typography.labelSmall,
                color = palette.secondaryText
            )
        }
    }
}

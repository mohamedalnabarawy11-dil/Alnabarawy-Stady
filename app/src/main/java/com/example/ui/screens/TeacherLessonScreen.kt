package com.example.ui.screens

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BookmarkAdded
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.School
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.audio.SoundEngine
import com.example.data.local.LessonEntity
import com.example.i18n.AppStrings
import com.example.ui.TeacherUiState
import com.example.ui.components.CameraCaptureModal
import com.example.ui.components.LuxuryGoldButton
import com.example.ui.components.LuxuryPressableBox
import com.example.ui.components.LuxurySectionCard
import com.example.ui.components.TeacherLoadingCard
import com.example.ui.theme.LocalNabraawyColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TeacherLessonScreen(
    state: TeacherUiState,
    savedLessons: List<LessonEntity>,
    strings: AppStrings,
    soundEngine: SoundEngine,
    onLessonTextChange: (String) -> Unit,
    onBitmapSelected: (Bitmap?) -> Unit,
    onCramModeChange: (Boolean) -> Unit,
    onExplainLesson: () -> Unit,
    onClarifyQuestionChange: (String) -> Unit,
    onAskClarify: (String?) -> Unit,
    onStudentEvalInputChange: (String) -> Unit,
    onEvaluateStudent: () -> Unit,
    onRequestMorePractice: () -> Unit,
    onDrillAnswerChange: (String) -> Unit,
    onCheckDrill: () -> Unit,
    onMarkAllGood: () -> Unit,
    onLoadSavedLesson: (LessonEntity) -> Unit,
    onDeleteSavedLesson: (Long) -> Unit
) {
    val palette = LocalNabraawyColors.current
    val context = LocalContext.current
    var showCamera by remember { mutableStateOf(false) }

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
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Input Card
        item {
            Box(modifier = Modifier.widthIn(max = 760.dp)) {
                LuxurySectionCard(
                    title = strings.tabTeacher,
                    icon = Icons.Filled.School
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LuxuryGoldButton(
                            text = strings.cameraBtn,
                            icon = Icons.Filled.CameraAlt,
                            onClick = { showCamera = true },
                            soundEngine = soundEngine,
                            isPrimary = false,
                            testTagName = "teacher_camera_button",
                            modifier = Modifier.weight(1f)
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
                            isPrimary = false,
                            testTagName = "teacher_gallery_button",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (state.attachedBitmap != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .border(1.dp, palette.goldBorder, RoundedCornerShape(16.dp))
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
                                    .padding(8.dp)
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(palette.screenBg.copy(alpha = 0.85f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = strings.removeImage,
                                    tint = palette.gold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = state.lessonText,
                        onValueChange = onLessonTextChange,
                        placeholder = {
                            Text(
                                text = strings.lessonInputHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondaryText
                            )
                        },
                        minLines = 4,
                        maxLines = 8,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = palette.gold,
                            unfocusedBorderColor = palette.cardBorder,
                            focusedTextColor = palette.primaryText,
                            unfocusedTextColor = palette.primaryText,
                            cursorColor = palette.gold
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("lesson_text_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Cram Mode Toggle ("وضع الزنقة")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = palette.elevatedCardBg,
                        border = BorderStroke(
                            1.dp,
                            if (state.cramMode) palette.gold else palette.cardBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Bolt,
                                    contentDescription = strings.cramModeTitle,
                                    tint = palette.gold,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = strings.cramModeTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.primaryText
                                    )
                                    Text(
                                        text = strings.cramModeSubtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.secondaryText
                                    )
                                }
                            }
                            Switch(
                                checked = state.cramMode,
                                onCheckedChange = {
                                    soundEngine.playClick()
                                    onCramModeChange(it)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = palette.screenBg,
                                    checkedTrackColor = palette.gold
                                ),
                                modifier = Modifier.testTag("cram_mode_switch")
                            )
                        }
                    }

                    if (!state.errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = state.errorMessage,
                            color = palette.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    LuxuryGoldButton(
                        text = strings.explainLessonBtn,
                        icon = Icons.Filled.AutoAwesome,
                        onClick = onExplainLesson,
                        soundEngine = soundEngine,
                        enabled = !state.isLoading,
                        isPrimary = true,
                        testTagName = "explain_lesson_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 2. Loading State
        if (state.isLoading) {
            item {
                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                    TeacherLoadingCard(
                        message = strings.teacherThinking,
                        retryingMessage = if (state.isRetrying) strings.retryingConnection else null
                    )
                }
            }
        }

        // 3. Structured Lesson Breakdown (4 Clearly Separated Cards)
        val breakdown = state.breakdown
        if (breakdown != null && !state.isLoading) {
            item {
                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                    Text(
                        text = breakdown.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = palette.gold,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }

            // 3A. Simplified Explanation (Gold in Ivory White contrast for reading comfort!)
            item {
                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                    LuxurySectionCard(
                        title = strings.simplifiedSectionTitle,
                        icon = Icons.Filled.MenuBook,
                        useIvoryContrast = true
                    ) {
                        Text(
                            text = breakdown.simplifiedExplanation,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.contrastWhiteText
                        )
                    }
                }
            }

            // 3B. Outside Connections (Gold in Black)
            item {
                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                    LuxurySectionCard(
                        title = strings.outsideConnectionTitle,
                        icon = Icons.Filled.Lightbulb
                    ) {
                        Text(
                            text = breakdown.outsideConnections,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.primaryText
                        )
                    }
                }
            }

            // 3C. Important Questions Radar
            item {
                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                    LuxurySectionCard(
                        title = strings.radarQuestionsTitle,
                        icon = Icons.Filled.Radar
                    ) {
                        Text(
                            text = breakdown.importantQuestionsRadar,
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.primaryText
                        )
                    }
                }
            }

            // 3D. Must Memorize (Clearly separated from understanding)
            item {
                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                    LuxurySectionCard(
                        title = strings.mustMemorizeTitle,
                        icon = Icons.Filled.BookmarkAdded,
                        useIvoryContrast = true
                    ) {
                        Text(
                            text = breakdown.mustMemorize,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = palette.contrastWhiteText
                        )
                    }
                }
            }

            // 4. Clarify Any Unclear Point (Streaming)
            item {
                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                    LuxurySectionCard(
                        title = strings.clarifyPointTitle,
                        icon = Icons.Filled.Psychology
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                strings.quickPromptSimplify,
                                strings.quickPromptExample,
                                strings.quickPromptOtherWay
                            ).forEach { prompt ->
                                LuxuryPressableBox(
                                    onClick = { onAskClarify(prompt) },
                                    soundEngine = soundEngine,
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .background(palette.elevatedCardBg, RoundedCornerShape(12.dp))
                                        .border(1.dp, palette.goldBorder, RoundedCornerShape(12.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = prompt,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = palette.gold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = state.clarifyQuestion,
                                onValueChange = onClarifyQuestionChange,
                                placeholder = {
                                    Text(
                                        text = strings.clarifyPointHint,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.secondaryText
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = palette.gold,
                                    unfocusedBorderColor = palette.cardBorder,
                                    focusedTextColor = palette.primaryText,
                                    unfocusedTextColor = palette.primaryText,
                                    cursorColor = palette.gold
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            LuxuryGoldButton(
                                text = strings.askTeacherBtn,
                                icon = Icons.AutoMirrored.Filled.Send,
                                onClick = { onAskClarify(null) },
                                soundEngine = soundEngine,
                                enabled = !state.isClarifying && state.clarifyQuestion.isNotBlank()
                            )
                        }

                        if (state.isClarifying && state.clarifyAnswer.isBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = strings.teacherThinking,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.gold
                            )
                        }
                        if (state.clarifyAnswer.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.elevatedCardBg,
                                border = BorderStroke(1.dp, palette.goldBorder)
                            ) {
                                Text(
                                    text = state.clarifyAnswer,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = palette.primaryText,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. "اشرحلي إنت فهمت إيه" (Student Teach-Back Evaluation)
            item {
                Box(modifier = Modifier.widthIn(max = 760.dp)) {
                    LuxurySectionCard(
                        title = strings.explainWhatYouUnderstoodTitle,
                        icon = Icons.Filled.CheckCircle
                    ) {
                        OutlinedTextField(
                            value = state.studentExplanationInput,
                            onValueChange = onStudentEvalInputChange,
                            placeholder = {
                                Text(
                                    text = strings.explainWhatYouUnderstoodHint,
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
                                .testTag("student_explain_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        LuxuryGoldButton(
                            text = strings.evaluateMyExplanationBtn,
                            icon = Icons.Filled.AutoAwesome,
                            onClick = onEvaluateStudent,
                            soundEngine = soundEngine,
                            enabled = !state.isEvaluating && state.studentExplanationInput.isNotBlank(),
                            testTagName = "evaluate_explanation_button",
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (state.isEvaluating) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = strings.teacherThinking,
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.gold
                            )
                        }

                        val eval = state.evaluation
                        if (eval != null && !state.isEvaluating) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.elevatedCardBg,
                                border = BorderStroke(1.2.dp, palette.gold)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = strings.teacherEvaluationTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = palette.gold
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = palette.gold
                                        ) {
                                            Text(
                                                text = "${eval.scoreOutOf10} / 10",
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = palette.screenBg,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = eval.praise,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = palette.primaryText
                                    )
                                    if (eval.corrections.isNotBlank()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = eval.corrections,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = palette.secondaryText
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = strings.wantMorePracticePrompt,
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        color = palette.gold
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        LuxuryGoldButton(
                                            text = strings.wantMorePracticeBtn,
                                            onClick = onRequestMorePractice,
                                            soundEngine = soundEngine,
                                            isPrimary = true,
                                            enabled = !state.isGeneratingDrill,
                                            modifier = Modifier.weight(1f)
                                        )
                                        LuxuryGoldButton(
                                            text = strings.allGoodBtn,
                                            onClick = onMarkAllGood,
                                            soundEngine = soundEngine,
                                            isPrimary = false,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        if (!state.understoodBanner.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = palette.successBg,
                                border = BorderStroke(1.dp, palette.success)
                            ) {
                                Text(
                                    text = state.understoodBanner,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = palette.success,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }

                        if (state.isGeneratingDrill) {
                            Spacer(modifier = Modifier.height(12.dp))
                            TeacherLoadingCard(message = strings.teacherThinking)
                        }

                        val drillQ = state.drillQuestion
                        if (drillQ != null && !state.isGeneratingDrill) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = palette.elevatedCardBg,
                                border = BorderStroke(1.dp, palette.goldBorder)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(
                                        text = drillQ.questionText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = palette.primaryText
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    if (drillQ.options.isNotEmpty()) {
                                        drillQ.options.forEach { opt ->
                                            val selected = state.drillAnswer == opt
                                            LuxuryPressableBox(
                                                onClick = { onDrillAnswerChange(opt) },
                                                soundEngine = soundEngine,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 4.dp)
                                                    .background(
                                                        if (selected) palette.gold.copy(alpha = 0.2f) else palette.cardBg,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (selected) palette.gold else palette.cardBorder,
                                                        RoundedCornerShape(12.dp)
                                                    )
                                                    .padding(12.dp)
                                            ) {
                                                Text(
                                                    text = opt,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = if (selected) palette.gold else palette.primaryText
                                                )
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = state.drillAnswer,
                                            onValueChange = onDrillAnswerChange,
                                            placeholder = { Text(strings.answerHint) },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    LuxuryGoldButton(
                                        text = strings.checkAnswerBtn,
                                        onClick = onCheckDrill,
                                        soundEngine = soundEngine,
                                        enabled = state.drillAnswer.isNotBlank() && !state.isCheckingDrill,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    state.drillResult?.let { res ->
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Text(
                                            text = if (res.isCorrect) "✓ ${res.teacherFeedback}" else "✕ ${strings.correctAnswerLabel}: ${res.correctAnswer}\n${res.teacherFeedback}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (res.isCorrect) palette.success else palette.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Local Saved Lessons History
        item {
            Box(modifier = Modifier.widthIn(max = 760.dp)) {
                LuxurySectionCard(
                    title = strings.savedLessonsTitle,
                    icon = Icons.Filled.History
                ) {
                    if (savedLessons.isEmpty()) {
                        Text(
                            text = strings.noSavedLessons,
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.secondaryText
                        )
                    }
                }
            }
        }

        items(savedLessons, key = { it.id }) { lesson ->
            Box(modifier = Modifier.widthIn(max = 760.dp)) {
                LuxuryPressableBox(
                    onClick = { onLoadSavedLesson(lesson) },
                    soundEngine = soundEngine,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.cardBg, RoundedCornerShape(16.dp))
                        .border(1.dp, palette.cardBorder, RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = lesson.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = palette.primaryText
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (lesson.cramMode) {
                                    Text(
                                        text = strings.cramModeTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.gold
                                    )
                                }
                                if (lesson.isUnderstood) {
                                    Text(
                                        text = "✓ ${strings.allGoodBtn}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = palette.success
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = {
                                soundEngine.playClick()
                                onDeleteSavedLesson(lesson.id)
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Delete",
                                tint = palette.secondaryText
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

package com.example.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.audio.SoundEngine
import com.example.i18n.AppStrings
import com.example.i18n.MainSection
import com.example.i18n.stringsFor
import com.example.ui.components.GoldHairlineDivider
import com.example.ui.components.LuxuryGoldButton
import com.example.ui.components.LuxuryPressableBox
import com.example.ui.screens.EntryGateScreen
import com.example.ui.screens.ExamRecitationScreen
import com.example.ui.screens.SmartScheduleScreen
import com.example.ui.screens.SplashScreenOverlay
import com.example.ui.screens.TeacherLessonScreen
import com.example.ui.theme.LocalNabraawyColors
import com.example.ui.theme.NabraawyStudyTheme
import kotlinx.coroutines.launch

@Composable
fun NabraawyAppRoot(
    viewModel: MainViewModel,
    onExitApp: () -> Unit
) {
    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val showSplash by viewModel.showSplash.collectAsStateWithLifecycle()
    val hasEntered by viewModel.hasEnteredApp.collectAsStateWithLifecycle()
    val strings = stringsFor(prefs.language)
    val layoutDirection = if (prefs.language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        NabraawyStudyTheme(darkTheme = prefs.isDarkTheme) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!hasEntered) {
                    EntryGateScreen(
                        strings = strings,
                        soundEngine = viewModel.soundEngine,
                        onInstallEnter = { viewModel.enterAppFromGate() },
                        onExitApp = onExitApp
                    )
                } else {
                    MainWorkspaceShell(
                        viewModel = viewModel,
                        strings = strings
                    )
                }

                if (showSplash) {
                    SplashScreenOverlay(onFinished = { viewModel.finishSplash() })
                }
            }
        }
    }
}

@Composable
private fun MainWorkspaceShell(
    viewModel: MainViewModel,
    strings: AppStrings
) {
    val palette = LocalNabraawyColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val prefs by viewModel.preferences.collectAsStateWithLifecycle()
    val currentSection by viewModel.currentSection.collectAsStateWithLifecycle()
    val teacherState by viewModel.teacherState.collectAsStateWithLifecycle()
    val examState by viewModel.examState.collectAsStateWithLifecycle()
    val scheduleState by viewModel.scheduleState.collectAsStateWithLifecycle()
    val savedLessons by viewModel.savedLessons.collectAsStateWithLifecycle()
    val mistakes by viewModel.mistakes.collectAsStateWithLifecycle()
    val flashcards by viewModel.flashcards.collectAsStateWithLifecycle()
    val busySlots by viewModel.busySlots.collectAsStateWithLifecycle()
    val subjects by viewModel.subjects.collectAsStateWithLifecycle()
    val scheduleSessions by viewModel.scheduleSessions.collectAsStateWithLifecycle()

    if (drawerState.isOpen) {
        BackHandler {
            scope.launch { drawerState.close() }
        }
    } else if (currentSection != MainSection.TEACHER_LESSON) {
        BackHandler {
            viewModel.selectSection(MainSection.TEACHER_LESSON)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            NabraawyDrawerContent(
                strings = strings,
                currentSection = currentSection,
                isMusicEnabled = prefs.isMusicEnabled,
                isDarkTheme = prefs.isDarkTheme,
                soundEngine = viewModel.soundEngine,
                onSelectSection = { section ->
                    viewModel.selectSection(section)
                    scope.launch { drawerState.close() }
                },
                onToggleMusic = { viewModel.toggleMusic() },
                onToggleLanguage = { viewModel.toggleLanguage() },
                onToggleTheme = { viewModel.toggleThemePolarity() },
                onOpenWhatsApp = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://wa.me/201034910183")
                    )
                    try {
                        context.startActivity(intent)
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, strings.whatsappNotFound, Toast.LENGTH_LONG).show()
                    }
                }
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.screenBg)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Luxury Top Header + Prominent Top Section Navigation (At TOP, NOT bottom)
            Surface(
                color = palette.cardBg,
                shadowElevation = 6.dp,
                border = BorderStroke(1.dp, palette.goldBorder),
                shape = RoundedCornerShape(bottomStart = 22.dp, bottomEnd = 22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(palette.cardBg, palette.elevatedCardBg)
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    // Row 1: Menu | Logo + "النبراوي للدراسة" | Music + Language
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            LuxuryPressableBox(
                                onClick = { scope.launch { drawerState.open() } },
                                soundEngine = viewModel.soundEngine,
                                shape = CircleShape,
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(palette.screenBg.copy(alpha = 0.6f), CircleShape)
                                    .border(1.dp, palette.goldBorder, CircleShape)
                                    .testTag("drawer_menu_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Menu,
                                    contentDescription = "Menu",
                                    tint = palette.gold,
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Image(
                                painter = painterResource(id = R.drawable.ic_nabraawy_logo),
                                contentDescription = strings.appTitle,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .border(1.dp, palette.gold, CircleShape),
                                contentScale = ContentScale.Crop
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = strings.appTitle,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = palette.gold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = strings.appSubtitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = palette.secondaryText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Clear Study Music Play / Mute Button
                            LuxuryPressableBox(
                                onClick = { viewModel.toggleMusic() },
                                soundEngine = viewModel.soundEngine,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .background(
                                        if (prefs.isMusicEnabled) palette.gold.copy(alpha = 0.18f) else palette.screenBg.copy(alpha = 0.6f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(1.dp, palette.goldBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("music_toggle_button")
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (prefs.isMusicEnabled) Icons.Filled.MusicNote else Icons.Filled.MusicOff,
                                        contentDescription = if (prefs.isMusicEnabled) strings.musicOn else strings.musicOff,
                                        tint = palette.gold,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Language Toggle AR / EN
                            LuxuryPressableBox(
                                onClick = { viewModel.toggleLanguage() },
                                soundEngine = viewModel.soundEngine,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .background(palette.screenBg.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                    .border(1.dp, palette.goldBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                    .testTag("language_toggle_button")
                            ) {
                                Text(
                                    text = strings.langToggleLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = palette.gold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    GoldHairlineDivider(color = palette.gold)
                    Spacer(modifier = Modifier.height(10.dp))

                    // Row 2: Prominent Top 3-Section Navigation Buttons (ONLY these 3 sections!)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TopSectionTabButton(
                            title = strings.tabTeacher,
                            icon = Icons.Filled.School,
                            selected = currentSection == MainSection.TEACHER_LESSON,
                            soundEngine = viewModel.soundEngine,
                            onClick = { viewModel.selectSection(MainSection.TEACHER_LESSON) },
                            testTagName = "nav_teacher_tab",
                            modifier = Modifier.weight(1f)
                        )
                        TopSectionTabButton(
                            title = strings.tabExam,
                            icon = Icons.Filled.Quiz,
                            selected = currentSection == MainSection.EXAM_RECITATION,
                            soundEngine = viewModel.soundEngine,
                            onClick = { viewModel.selectSection(MainSection.EXAM_RECITATION) },
                            testTagName = "nav_exam_tab",
                            modifier = Modifier.weight(1f)
                        )
                        TopSectionTabButton(
                            title = strings.tabSchedule,
                            icon = Icons.Filled.CalendarMonth,
                            selected = currentSection == MainSection.SMART_SCHEDULE,
                            soundEngine = viewModel.soundEngine,
                            onClick = { viewModel.selectSection(MainSection.SMART_SCHEDULE) },
                            testTagName = "nav_schedule_tab",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Active Section Content
            Crossfade(
                targetState = currentSection,
                animationSpec = tween(260),
                label = "sectionCrossfade",
                modifier = Modifier.weight(1f)
            ) { section ->
                when (section) {
                    MainSection.TEACHER_LESSON -> {
                        TeacherLessonScreen(
                            state = teacherState,
                            savedLessons = savedLessons,
                            strings = strings,
                            soundEngine = viewModel.soundEngine,
                            onLessonTextChange = viewModel::updateLessonText,
                            onBitmapSelected = viewModel::setTeacherBitmap,
                            onCramModeChange = viewModel::setCramMode,
                            onExplainLesson = viewModel::explainLesson,
                            onClarifyQuestionChange = viewModel::updateClarifyQuestion,
                            onAskClarify = viewModel::askTeacherToClarify,
                            onStudentEvalInputChange = viewModel::updateStudentExplanationInput,
                            onEvaluateStudent = viewModel::evaluateStudentExplanation,
                            onRequestMorePractice = viewModel::requestFollowUpPracticeDrill,
                            onDrillAnswerChange = viewModel::updateDrillAnswer,
                            onCheckDrill = viewModel::checkFollowUpDrill,
                            onMarkAllGood = viewModel::markLessonAllGood,
                            onLoadSavedLesson = viewModel::loadSavedLesson,
                            onDeleteSavedLesson = viewModel::deleteSavedLesson
                        )
                    }

                    MainSection.EXAM_RECITATION -> {
                        ExamRecitationScreen(
                            state = examState,
                            mistakes = mistakes,
                            flashcards = flashcards,
                            strings = strings,
                            soundEngine = viewModel.soundEngine,
                            onSubTabChange = viewModel::setExamSubTab,
                            onSourceTextChange = viewModel::updateExamSourceText,
                            onBitmapSelected = viewModel::setExamBitmap,
                            onImportLastLesson = viewModel::importLastLessonIntoExam,
                            onModeChange = viewModel::setExamMode,
                            onTypeChange = viewModel::setExamType,
                            onDifficultyChange = viewModel::setExamDifficulty,
                            onCountChange = viewModel::setExamCount,
                            onTotalGradeChange = viewModel::setExamTotalGrade,
                            onTimerChange = viewModel::setExamTimerMinutes,
                            onGenerateExam = viewModel::generateExam,
                            onGenerateFlashcards = viewModel::generateFlashcards,
                            onAnswerChange = viewModel::setExamAnswer,
                            onSubmitFullExam = viewModel::submitFullExam,
                            onCheckRecitation = viewModel::checkRecitationAnswer,
                            onNextRecitation = viewModel::nextRecitationQuestion,
                            onResetExam = viewModel::resetExam,
                            onStartMistakeDrill = viewModel::startMistakeDrill,
                            onMistakeDrillAnswerChange = viewModel::updateMistakeDrillAnswer,
                            onSubmitMistakeDrill = viewModel::submitMistakeDrill,
                            onCloseMistakeDrill = viewModel::closeMistakeDrill,
                            onDeleteMistake = viewModel::deleteMistake,
                            onToggleFlashcardMastered = viewModel::toggleFlashcardMastered,
                            onDeleteFlashcard = viewModel::deleteFlashcard
                        )
                    }

                    MainSection.SMART_SCHEDULE -> {
                        SmartScheduleScreen(
                            state = scheduleState,
                            busySlots = busySlots,
                            subjects = subjects,
                            sessions = scheduleSessions,
                            language = prefs.language,
                            strings = strings,
                            soundEngine = viewModel.soundEngine,
                            onAddBusySlot = viewModel::addBusySlot,
                            onDeleteBusySlot = viewModel::deleteBusySlot,
                            onToggleSelectAll = viewModel::toggleSelectAllSubjects,
                            onToggleSubjectSelected = viewModel::toggleSubjectSelected,
                            onUpdateSubjectDifficulty = viewModel::updateSubjectDifficulty,
                            onAddCustomSubject = viewModel::addCustomSubject,
                            onDeleteSubject = viewModel::deleteSubject,
                            onUpdateStudyWindow = viewModel::updateStudyWindow,
                            onGenerateSchedule = viewModel::generateSmartSchedule,
                            onToggleSessionCompleted = viewModel::toggleSessionCompleted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopSectionTabButton(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    soundEngine: SoundEngine,
    onClick: () -> Unit,
    testTagName: String,
    modifier: Modifier = Modifier
) {
    val palette = LocalNabraawyColors.current
    val bgColor = if (selected) palette.gold else palette.screenBg.copy(alpha = 0.55f)
    val fgColor = if (selected) palette.screenBg else palette.primaryText
    val borderColor = if (selected) palette.gold else palette.goldBorder.copy(alpha = 0.5f)

    LuxuryPressableBox(
        onClick = onClick,
        soundEngine = soundEngine,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .testTag(testTagName)
            .background(bgColor, RoundedCornerShape(14.dp))
            .border(1.dp, borderColor, RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 10.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = fgColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = fgColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun NabraawyDrawerContent(
    strings: AppStrings,
    currentSection: MainSection,
    isMusicEnabled: Boolean,
    isDarkTheme: Boolean,
    soundEngine: SoundEngine,
    onSelectSection: (MainSection) -> Unit,
    onToggleMusic: () -> Unit,
    onToggleLanguage: () -> Unit,
    onToggleTheme: () -> Unit,
    onOpenWhatsApp: () -> Unit
) {
    val palette = LocalNabraawyColors.current
    ModalDrawerSheet(
        drawerContainerColor = palette.cardBg,
        drawerContentColor = palette.primaryText,
        modifier = Modifier.width(300.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_nabraawy_logo),
                        contentDescription = strings.drawerTitle,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, palette.gold, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = strings.drawerTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.gold
                        )
                        Text(
                            text = strings.appSubtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = palette.secondaryText
                        )
                    }
                }

                GoldHairlineDivider(color = palette.gold)

                Text(
                    text = strings.drawerSectionsHeader,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.gold
                )

                DrawerActionItem(
                    label = strings.tabTeacher,
                    icon = Icons.Filled.School,
                    selected = currentSection == MainSection.TEACHER_LESSON,
                    soundEngine = soundEngine,
                    onClick = { onSelectSection(MainSection.TEACHER_LESSON) }
                )
                DrawerActionItem(
                    label = strings.tabExam,
                    icon = Icons.Filled.Quiz,
                    selected = currentSection == MainSection.EXAM_RECITATION,
                    soundEngine = soundEngine,
                    onClick = { onSelectSection(MainSection.EXAM_RECITATION) }
                )
                DrawerActionItem(
                    label = strings.tabSchedule,
                    icon = Icons.Filled.CalendarMonth,
                    selected = currentSection == MainSection.SMART_SCHEDULE,
                    soundEngine = soundEngine,
                    onClick = { onSelectSection(MainSection.SMART_SCHEDULE) }
                )

                Spacer(modifier = Modifier.height(6.dp))
                GoldHairlineDivider(color = palette.gold)

                Text(
                    text = strings.drawerAppearanceHeader,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.gold
                )

                DrawerActionItem(
                    label = if (isMusicEnabled) strings.musicOn else strings.musicOff,
                    icon = if (isMusicEnabled) Icons.Filled.MusicNote else Icons.Filled.MusicOff,
                    selected = isMusicEnabled,
                    soundEngine = soundEngine,
                    onClick = onToggleMusic
                )
                DrawerActionItem(
                    label = if (isDarkTheme) strings.themeObsidian else strings.themeIvory,
                    icon = if (isDarkTheme) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                    selected = false,
                    soundEngine = soundEngine,
                    onClick = onToggleTheme
                )
                DrawerActionItem(
                    label = strings.langToggleLabel,
                    icon = Icons.Filled.Language,
                    selected = false,
                    soundEngine = soundEngine,
                    onClick = onToggleLanguage
                )
            }

            // Prominent WhatsApp Developer Support Button
            Column {
                GoldHairlineDivider(color = palette.gold)
                Spacer(modifier = Modifier.height(14.dp))
                LuxuryGoldButton(
                    text = strings.whatsappSupportTitle,
                    icon = Icons.AutoMirrored.Filled.Chat,
                    onClick = onOpenWhatsApp,
                    soundEngine = soundEngine,
                    isPrimary = true,
                    testTagName = "whatsapp_support_button",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = strings.whatsappSupportSubtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondaryText
                )
            }
        }
    }
}

@Composable
private fun DrawerActionItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    soundEngine: SoundEngine,
    onClick: () -> Unit
) {
    val palette = LocalNabraawyColors.current
    LuxuryPressableBox(
        onClick = onClick,
        soundEngine = soundEngine,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) palette.gold.copy(alpha = 0.16f) else palette.elevatedCardBg,
                RoundedCornerShape(14.dp)
            )
            .border(
                1.dp,
                if (selected) palette.gold else palette.cardBorder,
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = palette.gold,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) palette.gold else palette.primaryText
            )
        }
    }
}

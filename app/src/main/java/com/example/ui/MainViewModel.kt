package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.data.ai.ExamDifficulty
import com.example.data.ai.ExamGradingReport
import com.example.data.ai.ExamMode
import com.example.data.ai.ExamQuestion
import com.example.data.ai.ExamType
import com.example.data.ai.GeminiStudyRepository
import com.example.data.ai.GradedQuestionResult
import com.example.data.ai.LessonBreakdown
import com.example.data.ai.StudentEvaluation
import com.example.data.local.AppDatabase
import com.example.data.local.BusySlotEntity
import com.example.data.local.FlashcardEntity
import com.example.data.local.LessonEntity
import com.example.data.local.MistakeEntity
import com.example.data.local.PreferencesManager
import com.example.data.local.ScheduleSessionEntity
import com.example.data.local.SubjectPrefEntity
import com.example.data.local.UserPreferences
import com.example.i18n.AppLanguage
import com.example.i18n.MainSection
import com.example.i18n.stringsFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "MainViewModel"

enum class ExamSubTab {
    CREATE_OR_TAKE,
    MISTAKE_BANK,
    FLASHCARDS
}

data class TeacherUiState(
    val lessonText: String = "",
    val attachedBitmap: Bitmap? = null,
    val cramMode: Boolean = false,
    val isLoading: Boolean = false,
    val isRetrying: Boolean = false,
    val errorMessage: String? = null,
    val activeLessonId: Long? = null,
    val breakdown: LessonBreakdown? = null,
    val clarifyQuestion: String = "",
    val clarifyAnswer: String = "",
    val isClarifying: Boolean = false,
    val studentExplanationInput: String = "",
    val isEvaluating: Boolean = false,
    val evaluation: StudentEvaluation? = null,
    val understoodBanner: String? = null,
    val drillQuestion: ExamQuestion? = null,
    val drillAnswer: String = "",
    val drillResult: GradedQuestionResult? = null,
    val isGeneratingDrill: Boolean = false,
    val isCheckingDrill: Boolean = false
)

data class ExamUiState(
    val subTab: ExamSubTab = ExamSubTab.CREATE_OR_TAKE,
    val sourceText: String = "",
    val attachedBitmap: Bitmap? = null,
    val examMode: ExamMode = ExamMode.FULL_EXAM,
    val examType: ExamType = ExamType.MIXED,
    val difficulty: ExamDifficulty = ExamDifficulty.MEDIUM,
    val questionCount: Int = 5,
    val totalGrade: Int = 20,
    val timerMinutes: Int = 0, // 0 = no timer
    val remainingSeconds: Int = 0,
    val isGenerating: Boolean = false,
    val isGrading: Boolean = false,
    val isRetrying: Boolean = false,
    val errorMessage: String? = null,
    val questions: List<ExamQuestion> = emptyList(),
    val studentAnswers: Map<Int, String> = emptyMap(),
    val gradingReport: ExamGradingReport? = null,
    val currentRecitationIndex: Int = 0,
    val currentRecitationResult: GradedQuestionResult? = null,
    val recitationResults: List<GradedQuestionResult> = emptyList(),
    val recitationFinished: Boolean = false,
    val drillMistake: MistakeEntity? = null,
    val drillMistakeAnswer: String = "",
    val drillMistakeResult: GradedQuestionResult? = null,
    val isCheckingMistakeDrill: Boolean = false
)

data class ScheduleUiState(
    val selectAllSubjects: Boolean = true,
    val studyWindowStartMinutes: Int = 8 * 60, // 8:00 AM
    val studyWindowEndMinutes: Int = 23 * 60,  // 11:00 PM
    val isGenerating: Boolean = false,
    val isRetrying: Boolean = false,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getInstance(application).appDao()
    private val prefsManager = PreferencesManager(application)
    private val aiRepo = GeminiStudyRepository()
    val soundEngine = SoundEngine()

    val preferences: StateFlow<UserPreferences> = prefsManager.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val _showSplash = MutableStateFlow(true)
    val showSplash: StateFlow<Boolean> = _showSplash.asStateFlow()

    private val _hasEnteredApp = MutableStateFlow(false)
    val hasEnteredApp: StateFlow<Boolean> = _hasEnteredApp.asStateFlow()

    private val _currentSection = MutableStateFlow(MainSection.TEACHER_LESSON)
    val currentSection: StateFlow<MainSection> = _currentSection.asStateFlow()

    private val _teacherState = MutableStateFlow(TeacherUiState())
    val teacherState: StateFlow<TeacherUiState> = _teacherState.asStateFlow()

    private val _examState = MutableStateFlow(ExamUiState())
    val examState: StateFlow<ExamUiState> = _examState.asStateFlow()

    private val _scheduleState = MutableStateFlow(ScheduleUiState())
    val scheduleState: StateFlow<ScheduleUiState> = _scheduleState.asStateFlow()

    val savedLessons = dao.observeLessons()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mistakes = dao.observeMistakes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val flashcards = dao.observeFlashcards()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val busySlots = dao.observeBusySlots()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subjects = dao.observeSubjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scheduleSessions = dao.observeScheduleSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var timerJob: Job? = null
    private var clarifyJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.collect { pref ->
                soundEngine.setMusicEnabled(pref.isMusicEnabled)
            }
        }
        viewModelScope.launch {
            seedDefaultSubjectsIfNeeded()
        }
    }

    private suspend fun seedDefaultSubjectsIfNeeded() {
        if (dao.countSubjects() == 0) {
            dao.insertSubjects(
                listOf(
                    SubjectPrefEntity(name = "الرياضيات", difficulty = "HARD", isSelected = true),
                    SubjectPrefEntity(name = "الفيزياء", difficulty = "HARD", isSelected = true),
                    SubjectPrefEntity(name = "الكيمياء", difficulty = "MEDIUM", isSelected = true),
                    SubjectPrefEntity(name = "اللغة العربية", difficulty = "MEDIUM", isSelected = true),
                    SubjectPrefEntity(name = "اللغة الإنجليزية", difficulty = "EASY", isSelected = true)
                )
            )
        }
    }

    fun finishSplash() {
        _showSplash.value = false
    }

    fun enterAppFromGate() {
        _hasEnteredApp.value = true
    }

    fun selectSection(section: MainSection) {
        _currentSection.value = section
    }

    fun toggleLanguage() {
        viewModelScope.launch {
            val next = if (preferences.value.language == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
            prefsManager.setLanguage(next)
        }
    }

    fun toggleMusic() {
        viewModelScope.launch {
            val next = !preferences.value.isMusicEnabled
            prefsManager.setMusicEnabled(next)
            soundEngine.setMusicEnabled(next)
        }
    }

    fun toggleThemePolarity() {
        viewModelScope.launch {
            prefsManager.setDarkTheme(!preferences.value.isDarkTheme)
        }
    }

    // ==================== SECTION 1: TEACHER & LESSON ====================

    fun updateLessonText(text: String) {
        _teacherState.update { it.copy(lessonText = text, errorMessage = null) }
    }

    fun setTeacherBitmap(bitmap: Bitmap?) {
        _teacherState.update { it.copy(attachedBitmap = bitmap, errorMessage = null) }
    }

    fun setCramMode(enabled: Boolean) {
        _teacherState.update { it.copy(cramMode = enabled) }
    }

    fun explainLesson() {
        val state = _teacherState.value
        val lang = preferences.value.language
        val strings = stringsFor(lang)
        if (state.lessonText.isBlank() && state.attachedBitmap == null) {
            _teacherState.update { it.copy(errorMessage = strings.lessonContentRequired) }
            return
        }

        viewModelScope.launch {
            _teacherState.update {
                it.copy(
                    isLoading = true,
                    isRetrying = false,
                    errorMessage = null,
                    clarifyAnswer = "",
                    evaluation = null,
                    understoodBanner = null,
                    drillQuestion = null,
                    drillAnswer = "",
                    drillResult = null
                )
            }
            try {
                val result = aiRepo.explainLesson(
                    lessonText = state.lessonText,
                    bitmap = state.attachedBitmap,
                    cramMode = state.cramMode,
                    lang = lang,
                    onRetrying = { _teacherState.update { it.copy(isRetrying = true) } }
                )
                val savedId = dao.insertLesson(
                    LessonEntity(
                        title = result.title,
                        sourceText = state.lessonText.ifBlank { "[Image Lesson]" },
                        cramMode = state.cramMode,
                        simplifiedExplanation = result.simplifiedExplanation,
                        outsideConnections = result.outsideConnections,
                        importantQuestionsRadar = result.importantQuestionsRadar,
                        mustMemorize = result.mustMemorize
                    )
                )
                _teacherState.update {
                    it.copy(
                        isLoading = false,
                        isRetrying = false,
                        activeLessonId = savedId,
                        breakdown = result
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "explainLesson failed", e)
                _teacherState.update {
                    it.copy(
                        isLoading = false,
                        isRetrying = false,
                        errorMessage = e.localizedMessage ?: "Failed to explain lesson."
                    )
                }
            }
        }
    }

    fun loadSavedLesson(lesson: LessonEntity) {
        _teacherState.update {
            it.copy(
                activeLessonId = lesson.id,
                lessonText = if (lesson.sourceText == "[Image Lesson]") "" else lesson.sourceText,
                cramMode = lesson.cramMode,
                breakdown = LessonBreakdown(
                    title = lesson.title,
                    simplifiedExplanation = lesson.simplifiedExplanation,
                    outsideConnections = lesson.outsideConnections,
                    importantQuestionsRadar = lesson.importantQuestionsRadar,
                    mustMemorize = lesson.mustMemorize
                ),
                clarifyAnswer = "",
                evaluation = null,
                understoodBanner = null,
                drillQuestion = null,
                errorMessage = null
            )
        }
    }

    fun deleteSavedLesson(id: Long) {
        viewModelScope.launch {
            dao.deleteLesson(id)
        }
    }

    fun updateClarifyQuestion(text: String) {
        _teacherState.update { it.copy(clarifyQuestion = text) }
    }

    fun askTeacherToClarify(customQuestion: String? = null) {
        val state = _teacherState.value
        val question = (customQuestion ?: state.clarifyQuestion).trim()
        val breakdown = state.breakdown ?: return
        if (question.isBlank()) return
        val lang = preferences.value.language

        clarifyJob?.cancel()
        clarifyJob = viewModelScope.launch {
            _teacherState.update {
                it.copy(
                    clarifyQuestion = question,
                    isClarifying = true,
                    clarifyAnswer = "",
                    errorMessage = null
                )
            }
            val contextText = buildString {
                appendLine(breakdown.title)
                appendLine(breakdown.simplifiedExplanation)
                appendLine(breakdown.mustMemorize)
            }
            try {
                aiRepo.clarifyLessonPointStream(contextText, question, lang).collect { partial ->
                    _teacherState.update { it.copy(clarifyAnswer = partial) }
                }
                _teacherState.update { it.copy(isClarifying = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "clarifyPoint failed", e)
                _teacherState.update {
                    it.copy(
                        isClarifying = false,
                        errorMessage = e.localizedMessage ?: "Could not clarify point."
                    )
                }
            }
        }
    }

    fun updateStudentExplanationInput(text: String) {
        _teacherState.update { it.copy(studentExplanationInput = text) }
    }

    fun evaluateStudentExplanation() {
        val state = _teacherState.value
        val breakdown = state.breakdown ?: return
        val input = state.studentExplanationInput.trim()
        if (input.isBlank()) return
        val lang = preferences.value.language

        viewModelScope.launch {
            _teacherState.update {
                it.copy(
                    isEvaluating = true,
                    isRetrying = false,
                    understoodBanner = null,
                    errorMessage = null
                )
            }
            val contextText = "${breakdown.title}\n${breakdown.simplifiedExplanation}\n${breakdown.mustMemorize}"
            try {
                val eval = aiRepo.evaluateStudentExplanation(
                    lessonContext = contextText,
                    studentExplanation = input,
                    lang = lang,
                    onRetrying = { _teacherState.update { it.copy(isRetrying = true) } }
                )
                _teacherState.update {
                    it.copy(
                        isEvaluating = false,
                        isRetrying = false,
                        evaluation = eval
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "evaluateStudentExplanation failed", e)
                _teacherState.update {
                    it.copy(
                        isEvaluating = false,
                        isRetrying = false,
                        errorMessage = e.localizedMessage ?: "Evaluation failed."
                    )
                }
            }
        }
    }

    fun requestFollowUpPracticeDrill() {
        val state = _teacherState.value
        val breakdown = state.breakdown ?: return
        val lang = preferences.value.language

        viewModelScope.launch {
            _teacherState.update {
                it.copy(
                    isGeneratingDrill = true,
                    drillQuestion = null,
                    drillAnswer = "",
                    drillResult = null,
                    understoodBanner = null
                )
            }
            val contextText = "${breakdown.title}\n${breakdown.simplifiedExplanation}\n${breakdown.importantQuestionsRadar}\n${breakdown.mustMemorize}"
            try {
                val q = aiRepo.generateQuickPracticeDrill(contextText, lang)
                _teacherState.update {
                    it.copy(isGeneratingDrill = false, drillQuestion = q)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "requestFollowUpPracticeDrill failed", e)
                _teacherState.update {
                    it.copy(
                        isGeneratingDrill = false,
                        errorMessage = e.localizedMessage ?: "Could not generate drill."
                    )
                }
            }
        }
    }

    fun updateDrillAnswer(ans: String) {
        _teacherState.update { it.copy(drillAnswer = ans) }
    }

    fun checkFollowUpDrill() {
        val state = _teacherState.value
        val q = state.drillQuestion ?: return
        val lang = preferences.value.language
        viewModelScope.launch {
            _teacherState.update { it.copy(isCheckingDrill = true) }
            try {
                val res = aiRepo.gradeSingleQuestion(q, state.drillAnswer, lang)
                if (!res.isCorrect) {
                    dao.insertMistakes(
                        listOf(
                            MistakeEntity(
                                subject = state.breakdown?.title ?: "Lesson",
                                question = q.questionText,
                                studentAnswer = state.drillAnswer,
                                correctAnswer = q.correctAnswer,
                                explanation = res.teacherFeedback
                            )
                        )
                    )
                }
                _teacherState.update { it.copy(isCheckingDrill = false, drillResult = res) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _teacherState.update {
                    it.copy(isCheckingDrill = false, errorMessage = e.localizedMessage)
                }
            }
        }
    }

    fun markLessonAllGood() {
        val lang = preferences.value.language
        val strings = stringsFor(lang)
        val id = _teacherState.value.activeLessonId
        viewModelScope.launch {
            if (id != null) {
                dao.updateLessonUnderstood(id, true)
            }
            _teacherState.update {
                it.copy(understoodBanner = strings.understoodCelebration)
            }
        }
    }

    // ==================== SECTION 2: EXAM & RECITATION ====================

    fun setExamSubTab(tab: ExamSubTab) {
        _examState.update { it.copy(subTab = tab, errorMessage = null) }
    }

    fun updateExamSourceText(text: String) {
        _examState.update { it.copy(sourceText = text, errorMessage = null) }
    }

    fun setExamBitmap(bitmap: Bitmap?) {
        _examState.update { it.copy(attachedBitmap = bitmap, errorMessage = null) }
    }

    fun importLastLessonIntoExam() {
        val tState = _teacherState.value
        val combined = buildString {
            tState.breakdown?.let { b ->
                appendLine(b.title)
                appendLine(b.simplifiedExplanation)
                appendLine(b.mustMemorize)
            } ?: append(tState.lessonText)
        }.trim()
        _examState.update {
            it.copy(
                sourceText = combined.ifBlank { tState.lessonText },
                attachedBitmap = it.attachedBitmap ?: tState.attachedBitmap,
                errorMessage = null
            )
        }
    }

    fun setExamMode(mode: ExamMode) = _examState.update { it.copy(examMode = mode) }
    fun setExamType(type: ExamType) = _examState.update { it.copy(examType = type) }
    fun setExamDifficulty(diff: ExamDifficulty) = _examState.update { it.copy(difficulty = diff) }
    fun setExamCount(count: Int) = _examState.update { it.copy(questionCount = count) }
    fun setExamTotalGrade(grade: Int) = _examState.update { it.copy(totalGrade = grade) }
    fun setExamTimerMinutes(mins: Int) = _examState.update { it.copy(timerMinutes = mins) }

    fun generateExam() {
        val state = _examState.value
        val lang = preferences.value.language
        val strings = stringsFor(lang)
        if (state.sourceText.isBlank() && state.attachedBitmap == null) {
            _examState.update { it.copy(errorMessage = strings.examContentRequired) }
            return
        }

        timerJob?.cancel()
        viewModelScope.launch {
            _examState.update {
                it.copy(
                    isGenerating = true,
                    isRetrying = false,
                    errorMessage = null,
                    questions = emptyList(),
                    studentAnswers = emptyMap(),
                    gradingReport = null,
                    currentRecitationIndex = 0,
                    currentRecitationResult = null,
                    recitationResults = emptyList(),
                    recitationFinished = false
                )
            }
            try {
                val questions = aiRepo.generateExam(
                    sourceText = state.sourceText,
                    bitmap = state.attachedBitmap,
                    examType = state.examType,
                    difficulty = state.difficulty,
                    count = state.questionCount,
                    totalGrade = state.totalGrade,
                    lang = lang,
                    onRetrying = { _examState.update { it.copy(isRetrying = true) } }
                )
                val timerSec = if (state.examMode == ExamMode.FULL_EXAM && state.timerMinutes > 0) {
                    state.timerMinutes * 60
                } else 0
                _examState.update {
                    it.copy(
                        isGenerating = false,
                        isRetrying = false,
                        questions = questions,
                        remainingSeconds = timerSec
                    )
                }
                if (timerSec > 0) {
                    startExamTimer()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateExam failed", e)
                _examState.update {
                    it.copy(
                        isGenerating = false,
                        isRetrying = false,
                        errorMessage = e.localizedMessage ?: "Failed to generate exam."
                    )
                }
            }
        }
    }

    private fun startExamTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_examState.value.remainingSeconds > 0 && _examState.value.gradingReport == null) {
                delay(1000L)
                _examState.update { it.copy(remainingSeconds = (it.remainingSeconds - 1).coerceAtLeast(0)) }
            }
            if (_examState.value.remainingSeconds == 0 &&
                _examState.value.questions.isNotEmpty() &&
                _examState.value.gradingReport == null
            ) {
                submitFullExam()
            }
        }
    }

    fun setExamAnswer(index: Int, answer: String) {
        _examState.update { state ->
            val updated = state.studentAnswers.toMutableMap().apply { put(index, answer) }
            state.copy(studentAnswers = updated)
        }
    }

    fun submitFullExam() {
        val state = _examState.value
        if (state.questions.isEmpty() || state.isGrading) return
        val lang = preferences.value.language
        timerJob?.cancel()

        viewModelScope.launch {
            _examState.update { it.copy(isGrading = true, isRetrying = false, errorMessage = null) }
            try {
                val report = aiRepo.gradeExam(
                    questions = state.questions,
                    studentAnswers = state.studentAnswers,
                    totalGrade = state.totalGrade,
                    lang = lang,
                    onRetrying = { _examState.update { it.copy(isRetrying = true) } }
                )
                saveMistakesFromResults(state.questions, report.results, state.sourceText)
                _examState.update {
                    it.copy(
                        isGrading = false,
                        isRetrying = false,
                        gradingReport = report
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "submitFullExam failed", e)
                _examState.update {
                    it.copy(
                        isGrading = false,
                        isRetrying = false,
                        errorMessage = e.localizedMessage ?: "Failed to grade exam."
                    )
                }
            }
        }
    }

    fun checkRecitationAnswer() {
        val state = _examState.value
        val question = state.questions.getOrNull(state.currentRecitationIndex) ?: return
        val ans = state.studentAnswers[question.index].orEmpty()
        val lang = preferences.value.language

        viewModelScope.launch {
            _examState.update { it.copy(isGrading = true, isRetrying = false, errorMessage = null) }
            try {
                val result = aiRepo.gradeSingleQuestion(
                    question = question,
                    studentAnswer = ans,
                    lang = lang,
                    onRetrying = { _examState.update { it.copy(isRetrying = true) } }
                ).copy(index = question.index)

                if (!result.isCorrect) {
                    saveMistakesFromResults(listOf(question), listOf(result), state.sourceText)
                }
                _examState.update {
                    it.copy(
                        isGrading = false,
                        isRetrying = false,
                        currentRecitationResult = result,
                        recitationResults = it.recitationResults + result
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "checkRecitationAnswer failed", e)
                _examState.update {
                    it.copy(
                        isGrading = false,
                        isRetrying = false,
                        errorMessage = e.localizedMessage ?: "Grading failed."
                    )
                }
            }
        }
    }

    fun nextRecitationQuestion() {
        _examState.update { state ->
            val nextIdx = state.currentRecitationIndex + 1
            if (nextIdx >= state.questions.size) {
                state.copy(recitationFinished = true, currentRecitationResult = null)
            } else {
                state.copy(currentRecitationIndex = nextIdx, currentRecitationResult = null)
            }
        }
    }

    fun resetExam() {
        timerJob?.cancel()
        _examState.update {
            it.copy(
                questions = emptyList(),
                studentAnswers = emptyMap(),
                gradingReport = null,
                currentRecitationIndex = 0,
                currentRecitationResult = null,
                recitationResults = emptyList(),
                recitationFinished = false,
                remainingSeconds = 0,
                errorMessage = null
            )
        }
    }

    private suspend fun saveMistakesFromResults(
        questions: List<ExamQuestion>,
        results: List<GradedQuestionResult>,
        sourceHint: String
    ) {
        val label = sourceHint.lineSequence().firstOrNull()?.take(36).orEmpty().ifBlank { "General" }
        val toInsert = results.filter { !it.isCorrect }.mapNotNull { r ->
            val q = questions.firstOrNull { it.index == r.index } ?: return@mapNotNull null
            MistakeEntity(
                subject = label,
                question = q.questionText,
                studentAnswer = r.studentAnswer.ifBlank { "—" },
                correctAnswer = q.correctAnswer,
                explanation = r.teacherFeedback
            )
        }
        if (toInsert.isNotEmpty()) {
            dao.insertMistakes(toInsert)
        }
    }

    fun startMistakeDrill(mistake: MistakeEntity) {
        _examState.update {
            it.copy(
                drillMistake = mistake,
                drillMistakeAnswer = "",
                drillMistakeResult = null
            )
        }
    }

    fun updateMistakeDrillAnswer(ans: String) {
        _examState.update { it.copy(drillMistakeAnswer = ans) }
    }

    fun submitMistakeDrill() {
        val state = _examState.value
        val m = state.drillMistake ?: return
        val lang = preferences.value.language
        viewModelScope.launch {
            _examState.update { it.copy(isCheckingMistakeDrill = true) }
            try {
                val q = ExamQuestion(
                    index = 0,
                    type = "SHORT_ESSAY",
                    questionText = m.question,
                    options = emptyList(),
                    correctAnswer = m.correctAnswer,
                    explanation = m.explanation
                )
                val res = aiRepo.gradeSingleQuestion(q, state.drillMistakeAnswer, lang)
                _examState.update { it.copy(isCheckingMistakeDrill = false, drillMistakeResult = res) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _examState.update {
                    it.copy(isCheckingMistakeDrill = false, errorMessage = e.localizedMessage)
                }
            }
        }
    }

    fun closeMistakeDrill() {
        _examState.update {
            it.copy(drillMistake = null, drillMistakeAnswer = "", drillMistakeResult = null)
        }
    }

    fun deleteMistake(id: Long) {
        viewModelScope.launch { dao.deleteMistake(id) }
    }

    fun generateFlashcards() {
        val state = _examState.value
        val lang = preferences.value.language
        val strings = stringsFor(lang)
        if (state.sourceText.isBlank() && state.attachedBitmap == null) {
            _examState.update { it.copy(errorMessage = strings.examContentRequired) }
            return
        }

        viewModelScope.launch {
            _examState.update { it.copy(isGenerating = true, isRetrying = false, errorMessage = null) }
            try {
                val cards = aiRepo.generateFlashcards(
                    sourceText = state.sourceText,
                    bitmap = state.attachedBitmap,
                    count = 8,
                    lang = lang,
                    onRetrying = { _examState.update { it.copy(isRetrying = true) } }
                )
                val title = state.sourceText.lineSequence().firstOrNull()?.take(32).orEmpty().ifBlank { "Flashcards" }
                dao.insertFlashcards(
                    cards.map { c ->
                        FlashcardEntity(deckTitle = title, front = c.front, back = c.back)
                    }
                )
                _examState.update {
                    it.copy(
                        isGenerating = false,
                        isRetrying = false,
                        subTab = ExamSubTab.FLASHCARDS
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateFlashcards failed", e)
                _examState.update {
                    it.copy(
                        isGenerating = false,
                        isRetrying = false,
                        errorMessage = e.localizedMessage ?: "Failed to generate flashcards."
                    )
                }
            }
        }
    }

    fun toggleFlashcardMastered(card: FlashcardEntity) {
        viewModelScope.launch { dao.updateFlashcardMastered(card.id, !card.isMastered) }
    }

    fun deleteFlashcard(id: Long) {
        viewModelScope.launch { dao.deleteFlashcard(id) }
    }

    // ==================== SECTION 3: SMART SCHEDULE ====================

    fun addBusySlot(label: String, startMinutes: Int, endMinutes: Int) {
        viewModelScope.launch {
            dao.insertBusySlot(
                BusySlotEntity(
                    label = label.trim(),
                    startMinutes = startMinutes,
                    endMinutes = endMinutes
                )
            )
        }
    }

    fun deleteBusySlot(id: Long) {
        viewModelScope.launch { dao.deleteBusySlot(id) }
    }

    fun toggleSelectAllSubjects(all: Boolean) {
        _scheduleState.update { it.copy(selectAllSubjects = all, errorMessage = null) }
        viewModelScope.launch {
            subjects.value.forEach { subj ->
                if (subj.isSelected != all) {
                    dao.updateSubject(subj.copy(isSelected = all))
                }
            }
        }
    }

    fun toggleSubjectSelected(subject: SubjectPrefEntity) {
        viewModelScope.launch {
            val updated = subject.copy(isSelected = !subject.isSelected)
            dao.updateSubject(updated)
            val allSelectedNow = subjects.value.all { if (it.id == subject.id) updated.isSelected else it.isSelected }
            _scheduleState.update { it.copy(selectAllSubjects = allSelectedNow, errorMessage = null) }
        }
    }

    fun updateSubjectDifficulty(subject: SubjectPrefEntity, difficulty: String) {
        viewModelScope.launch {
            dao.updateSubject(subject.copy(difficulty = difficulty))
        }
    }

    fun addCustomSubject(name: String, difficulty: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            dao.insertSubject(
                SubjectPrefEntity(
                    name = name.trim(),
                    difficulty = difficulty,
                    isSelected = true
                )
            )
        }
    }

    fun deleteSubject(id: Long) {
        viewModelScope.launch { dao.deleteSubject(id) }
    }

    fun updateStudyWindow(startMin: Int, endMin: Int) {
        if (endMin <= startMin + 30) return
        _scheduleState.update {
            it.copy(studyWindowStartMinutes = startMin, studyWindowEndMinutes = endMin)
        }
    }

    fun generateSmartSchedule() {
        val state = _scheduleState.value
        val lang = preferences.value.language
        val strings = stringsFor(lang)
        val activeSubjects = if (state.selectAllSubjects) {
            subjects.value
        } else {
            subjects.value.filter { it.isSelected }
        }

        if (activeSubjects.isEmpty()) {
            _scheduleState.update { it.copy(errorMessage = strings.selectAtLeastOneSubject) }
            return
        }

        viewModelScope.launch {
            _scheduleState.update { it.copy(isGenerating = true, isRetrying = false, errorMessage = null) }
            try {
                val generated = aiRepo.generateSmartSchedule(
                    busySlots = busySlots.value,
                    subjects = activeSubjects,
                    windowStartMinutes = state.studyWindowStartMinutes,
                    windowEndMinutes = state.studyWindowEndMinutes,
                    lang = lang,
                    onRetrying = { _scheduleState.update { it.copy(isRetrying = true) } }
                )
                dao.clearScheduleSessions()
                dao.insertScheduleSessions(generated)
                _scheduleState.update { it.copy(isGenerating = false, isRetrying = false) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "generateSmartSchedule failed", e)
                _scheduleState.update {
                    it.copy(
                        isGenerating = false,
                        isRetrying = false,
                        errorMessage = e.localizedMessage ?: "Failed to build schedule."
                    )
                }
            }
        }
    }

    fun toggleSessionCompleted(session: ScheduleSessionEntity) {
        viewModelScope.launch {
            dao.updateSessionCompleted(session.id, !session.isCompleted)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        clarifyJob?.cancel()
        soundEngine.release()
    }
}

package com.example.data.ai

import android.graphics.Bitmap
import android.util.Log
import com.example.data.local.BusySlotEntity
import com.example.data.local.ScheduleSessionEntity
import com.example.data.local.SubjectPrefEntity
import com.example.i18n.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.min
import kotlin.math.roundToInt

private const val TAG = "GeminiStudyRepository"
private const val MODEL_ID = "gemini-3.8-flash"

class GeminiStudyRepository {

    private val streamingTeacherModel by lazy {
        Firebase.ai.generativeModel(modelName = MODEL_ID)
    }

    private val lessonModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "title" to Schema.string(description = "Concise descriptive title of the lesson"),
                "simplifiedExplanation" to Schema.string(
                    description = "Clear, warm, encouraging explanation in the style of a brilliant teacher using analogies and structured bullet points"
                ),
                "outsideConnections" to Schema.string(
                    description = "Real-world examples and cross-topic connections outside the lesson textbook"
                ),
                "importantQuestionsRadar" to Schema.string(
                    description = "High-probability exam questions, common traps, and key focus alerts"
                ),
                "mustMemorize" to Schema.string(
                    description = "Exact definitions, formulas, dates, rules, or terms that MUST be memorized verbatim, cleanly separated from conceptual explanation"
                )
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_ID,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val evaluationModel by lazy {
        val schema = Schema.obj(
            properties = mapOf(
                "scoreOutOf10" to Schema.integer(description = "Score from 1 to 10"),
                "praise" to Schema.string(description = "Warm encouraging praise of what the student understood well"),
                "corrections" to Schema.string(description = "Gentle, clear correction of missing or inaccurate points"),
                "closingQuestion" to Schema.string(description = "Encouraging closing question asking if they want more practice or are all good")
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_ID,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val examModel by lazy {
        val questionSchema = Schema.obj(
            properties = mapOf(
                "type" to Schema.enumeration(
                    values = listOf("MCQ", "TRUE_FALSE", "SHORT_ESSAY"),
                    description = "Question format"
                ),
                "questionText" to Schema.string(description = "The question prompt strictly grounded in the provided content"),
                "options" to Schema.array(
                    items = Schema.string(),
                    description = "4 answer choices for MCQ, 2 choices for TRUE_FALSE, or empty list for SHORT_ESSAY"
                ),
                "correctAnswer" to Schema.string(description = "Exact correct option text or concise model answer"),
                "explanation" to Schema.string(description = "Brief encouraging teacher explanation referencing the uploaded content")
            )
        )
        val schema = Schema.obj(
            properties = mapOf(
                "questions" to Schema.array(items = questionSchema)
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_ID,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val gradingModel by lazy {
        val itemSchema = Schema.obj(
            properties = mapOf(
                "index" to Schema.integer(description = "0-based question index"),
                "isCorrect" to Schema.boolean(description = "Whether the student answer is fully or mostly correct"),
                "earnedRatio" to Schema.double(description = "Score ratio from 0.0 to 1.0"),
                "feedback" to Schema.string(description = "Warm, constructive teacher feedback explaining why")
            )
        )
        val schema = Schema.obj(
            properties = mapOf(
                "overallTeacherNote" to Schema.string(description = "Encouraging overall summary from the smart teacher"),
                "items" to Schema.array(items = itemSchema)
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_ID,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val flashcardModel by lazy {
        val cardSchema = Schema.obj(
            properties = mapOf(
                "front" to Schema.string(description = "Short front question or key term"),
                "back" to Schema.string(description = "Clear concise answer or definition")
            )
        )
        val schema = Schema.obj(
            properties = mapOf(
                "cards" to Schema.array(items = cardSchema)
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_ID,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private val scheduleAdviceModel by lazy {
        val subjectBlockSchema = Schema.obj(
            properties = mapOf(
                "subjectName" to Schema.string(),
                "focusTip" to Schema.string(description = "Calm 1-line study focus tip for this subject")
            )
        )
        val schema = Schema.obj(
            properties = mapOf(
                "orderedTips" to Schema.array(items = subjectBlockSchema)
            )
        )
        Firebase.ai.generativeModel(
            modelName = MODEL_ID,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = schema
            }
        )
    }

    private suspend fun <T> executeWithSmartRetry(
        maxAttempts: Int = 3,
        onRetrying: (() -> Unit)? = null,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null
        for (attempt in 1..maxAttempts) {
            try {
                return withTimeout(60_000L) {
                    block()
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastException = e
                Log.w(TAG, "AI call attempt $attempt failed: ${e.message}", e)
                if (attempt < maxAttempts) {
                    onRetrying?.invoke()
                    val backoffMs = attempt * 1300L
                    delay(backoffMs)
                }
            }
        }
        throw lastException ?: IllegalStateException("AI request failed after retries.")
    }

    private fun prepareBitmap(bitmap: Bitmap): Bitmap {
        val maxSide = 1280
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSide && height <= maxSide) return bitmap
        val scale = min(maxSide.toFloat() / width, maxSide.toFloat() / height)
        val newW = (width * scale).roundToInt().coerceAtLeast(1)
        val newH = (height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    private fun languageInstruction(lang: AppLanguage): String = when (lang) {
        AppLanguage.AR ->
            "Respond in clear, warm, encouraging Arabic using the 'الأستاذ الشاطر' (smart, supportive master tutor) persona. Use crisp bullet points and clean formatting without raw markdown clutter."
        AppLanguage.EN ->
            "Respond in clear, warm, encouraging English using a supportive master tutor persona. Use crisp bullet points and clean formatting without raw markdown clutter."
    }

    suspend fun explainLesson(
        lessonText: String?,
        bitmap: Bitmap?,
        cramMode: Boolean,
        lang: AppLanguage,
        onRetrying: () -> Unit = {}
    ): LessonBreakdown {
        val modePrompt = if (cramMode) {
            if (lang == AppLanguage.AR) {
                "المطلوب تفعيل 'وضع الزنقة': ركّز على الخلاصة السريعة، القوانين المباشرة، الأخطاء الشائعة التي يقع فيها الطلاب في الامتحان، ونقاط الحل السريع بأقصر وأوضح عبارة."
            } else {
                "CRAM MODE ACTIVE: Deliver ultra-focused high-yield takeaways, rapid shortcuts, exam traps, and must-know bullets in concise form."
            }
        } else {
            if (lang == AppLanguage.AR) {
                "قدّم شرحاً وافياً ومبسطاً خطوة بخطوة يبني الفهم العميق أولاً ثم يفصل الحفظ بوضوح."
            } else {
                "Provide a thorough, crystal-clear step-by-step explanation that builds deep understanding first, keeping rote memorization strictly separated."
            }
        }

        val prompt = buildString {
            appendLine(languageInstruction(lang))
            appendLine(modePrompt)
            appendLine("Read the lesson content (text and/or attached page image) carefully.")
            appendLine("Strictly separate:")
            appendLine("1) simplifiedExplanation: friendly, clear conceptual explanation.")
            appendLine("2) outsideConnections: links to real life, practical applications, or broader topics outside the lesson.")
            appendLine("3) importantQuestionsRadar: likely exam questions, tricky variations, and teacher alerts.")
            appendLine("4) mustMemorize: ONLY exact definitions, formulas, dates, or terms to memorize verbatim.")
            if (!lessonText.isNullOrBlank()) {
                appendLine("\nLesson Text:\n$lessonText")
            }
        }

        val scaledBitmap = bitmap?.let { prepareBitmap(it) }

        val response = executeWithSmartRetry(onRetrying = onRetrying) {
            lessonModel.generateContent(
                content {
                    if (scaledBitmap != null) image(scaledBitmap)
                    text(prompt)
                }
            )
        }

        val raw = response.text ?: throw IllegalStateException("Empty response from AI tutor.")
        val json = JSONObject(raw)
        return LessonBreakdown(
            title = json.optString("title").ifBlank { if (lang == AppLanguage.AR) "شرح الدرس" else "Lesson Breakdown" },
            simplifiedExplanation = json.optString("simplifiedExplanation"),
            outsideConnections = json.optString("outsideConnections"),
            importantQuestionsRadar = json.optString("importantQuestionsRadar"),
            mustMemorize = json.optString("mustMemorize")
        )
    }

    fun clarifyLessonPointStream(
        lessonContext: String,
        studentQuestion: String,
        lang: AppLanguage
    ): Flow<String> = flow {
        val prompt = buildString {
            appendLine(languageInstruction(lang))
            appendLine("The student is studying this lesson and didn't understand a specific point. Clarify it warmly like a patient, brilliant teacher with a fresh analogy or concrete example.")
            appendLine("\nLesson Context:\n${lessonContext.take(3500)}")
            appendLine("\nStudent's Question:\n$studentQuestion")
        }

        val accumulated = StringBuilder()
        var attempt = 0
        val maxAttempts = 2
        while (true) {
            attempt++
            try {
                withTimeout(60_000L) {
                    streamingTeacherModel.generateContentStream(prompt).collect { chunk ->
                        val part = chunk.text.orEmpty()
                        if (part.isNotEmpty()) {
                            accumulated.append(part)
                            emit(accumulated.toString())
                        }
                    }
                }
                break
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (attempt >= maxAttempts) throw e
                accumulated.clear()
                delay(1200L)
            }
        }
    }

    suspend fun evaluateStudentExplanation(
        lessonContext: String,
        studentExplanation: String,
        lang: AppLanguage,
        onRetrying: () -> Unit = {}
    ): StudentEvaluation {
        val closingRequired = if (lang == AppLanguage.AR) {
            "عايز تدريب كمان ولا تمام كده؟"
        } else {
            "Would you like more practice, or are you all good?"
        }

        val prompt = buildString {
            appendLine(languageInstruction(lang))
            appendLine("The student tried to explain the lesson back in their own words ('اشرحلي إنت فهمت إيه').")
            appendLine("Evaluate their understanding fairly out of 10, praise what they grasped well, gently clarify anything missing or inaccurate, and set closingQuestion to: \"$closingRequired\"")
            appendLine("\nLesson Reference:\n${lessonContext.take(3500)}")
            appendLine("\nStudent's Own Explanation:\n$studentExplanation")
        }

        val response = executeWithSmartRetry(onRetrying = onRetrying) {
            evaluationModel.generateContent(prompt)
        }
        val raw = response.text ?: throw IllegalStateException("Empty evaluation response.")
        val json = JSONObject(raw)
        return StudentEvaluation(
            scoreOutOf10 = json.optInt("scoreOutOf10", 8).coerceIn(1, 10),
            praise = json.optString("praise"),
            corrections = json.optString("corrections"),
            closingQuestion = closingRequired
        )
    }

    suspend fun generateQuickPracticeDrill(
        lessonContext: String,
        lang: AppLanguage,
        onRetrying: () -> Unit = {}
    ): ExamQuestion {
        val list = generateExam(
            sourceText = lessonContext,
            bitmap = null,
            examType = ExamType.MIXED,
            difficulty = ExamDifficulty.MEDIUM,
            count = 1,
            totalGrade = 10,
            lang = lang,
            onRetrying = onRetrying
        )
        return list.first()
    }

    suspend fun generateExam(
        sourceText: String?,
        bitmap: Bitmap?,
        examType: ExamType,
        difficulty: ExamDifficulty,
        count: Int,
        totalGrade: Int,
        lang: AppLanguage,
        onRetrying: () -> Unit = {}
    ): List<ExamQuestion> {
        val typeDirective = when (examType) {
            ExamType.MCQ -> "ALL questions must be MCQ with exactly 4 distinct options."
            ExamType.TRUE_FALSE -> if (lang == AppLanguage.AR) {
                "ALL questions must be TRUE_FALSE with options [\"صح\", \"غلط\"]."
            } else {
                "ALL questions must be TRUE_FALSE with options [\"True\", \"False\"]."
            }
            ExamType.SHORT_ESSAY -> "ALL questions must be SHORT_ESSAY with empty options []."
            ExamType.MIXED -> "Mix MCQ (4 options), TRUE_FALSE (2 options), and SHORT_ESSAY (empty options)."
        }

        val diffDirective = when (difficulty) {
            ExamDifficulty.EASY -> "Difficulty: EASY (direct recall and foundational understanding)."
            ExamDifficulty.MEDIUM -> "Difficulty: MEDIUM (conceptual understanding and direct application)."
            ExamDifficulty.HARD -> "Difficulty: HARD (analytical synthesis, subtle distinctions, and exam-level challenge)."
        }

        val prompt = buildString {
            appendLine(languageInstruction(lang))
            appendLine("CRITICAL RULE: Generate exactly $count exam questions STRICTLY AND EXCLUSIVELY from the uploaded image/text content below. Do NOT introduce facts, examples, or topics that are not present in the provided content.")
            appendLine(typeDirective)
            appendLine(diffDirective)
            appendLine("Total exam grade is $totalGrade points.")
            if (!sourceText.isNullOrBlank()) {
                appendLine("\nProvided Study Content:\n$sourceText")
            }
        }

        val scaledBitmap = bitmap?.let { prepareBitmap(it) }

        val response = executeWithSmartRetry(onRetrying = onRetrying) {
            examModel.generateContent(
                content {
                    if (scaledBitmap != null) image(scaledBitmap)
                    text(prompt)
                }
            )
        }

        val raw = response.text ?: throw IllegalStateException("Empty exam response.")
        val root = JSONObject(raw)
        val arr = root.optJSONArray("questions") ?: JSONArray()
        val questions = mutableListOf<ExamQuestion>()
        for (i in 0 until arr.length()) {
            val item = arr.getJSONObject(i)
            val optsArr = item.optJSONArray("options") ?: JSONArray()
            val opts = (0 until optsArr.length()).map { optsArr.getString(it) }
            questions.add(
                ExamQuestion(
                    index = i,
                    type = item.optString("type", "MCQ"),
                    questionText = item.optString("questionText"),
                    options = opts,
                    correctAnswer = item.optString("correctAnswer"),
                    explanation = item.optString("explanation")
                )
            )
        }
        if (questions.isEmpty()) throw IllegalStateException("No questions generated from content.")
        return questions
    }

    suspend fun gradeExam(
        questions: List<ExamQuestion>,
        studentAnswers: Map<Int, String>,
        totalGrade: Int,
        lang: AppLanguage,
        onRetrying: () -> Unit = {}
    ): ExamGradingReport {
        val perQuestionMax = totalGrade.toDouble() / questions.size.coerceAtLeast(1)

        val payload = JSONArray()
        questions.forEach { q ->
            payload.put(
                JSONObject().apply {
                    put("index", q.index)
                    put("type", q.type)
                    put("question", q.questionText)
                    put("modelAnswer", q.correctAnswer)
                    put("studentAnswer", studentAnswers[q.index].orEmpty().ifBlank { "(No answer)" })
                }
            )
        }

        val prompt = buildString {
            appendLine(languageInstruction(lang))
            appendLine("Grade each student answer fairly against the modelAnswer. For MCQ/TRUE_FALSE, exact/semantic match gets earnedRatio = 1.0, wrong gets 0.0. For SHORT_ESSAY, award partial credit (0.0 to 1.0) accurately and give encouraging teacher feedback.")
            appendLine("\nQuestions and Answers:\n${payload.toString()}")
        }

        val response = executeWithSmartRetry(onRetrying = onRetrying) {
            gradingModel.generateContent(prompt)
        }
        val raw = response.text ?: throw IllegalStateException("Empty grading response.")
        val root = JSONObject(raw)
        val itemsArr = root.optJSONArray("items") ?: JSONArray()
        val resultMap = mutableMapOf<Int, Pair<Boolean, Pair<Double, String>>>()
        for (i in 0 until itemsArr.length()) {
            val obj = itemsArr.getJSONObject(i)
            val idx = obj.optInt("index", i)
            val ratio = obj.optDouble("earnedRatio", if (obj.optBoolean("isCorrect")) 1.0 else 0.0)
                .coerceIn(0.0, 1.0)
            val isCorrect = obj.optBoolean("isCorrect", ratio >= 0.7)
            val fb = obj.optString("feedback")
            resultMap[idx] = isCorrect to (ratio to fb)
        }

        val gradedList = questions.map { q ->
            val studentAns = studentAnswers[q.index].orEmpty()
            val entry = resultMap[q.index]
            val isCorrect: Boolean
            val ratio: Double
            val fb: String
            if (entry != null) {
                isCorrect = entry.first
                ratio = entry.second.first
                fb = entry.second.second.ifBlank { q.explanation }
            } else {
                val match = studentAns.trim().equals(q.correctAnswer.trim(), ignoreCase = true)
                isCorrect = match
                ratio = if (match) 1.0 else 0.0
                fb = q.explanation
            }
            GradedQuestionResult(
                index = q.index,
                isCorrect = isCorrect,
                earnedPoints = ((ratio * perQuestionMax) * 10.0).roundToInt() / 10.0,
                maxPoints = (perQuestionMax * 10.0).roundToInt() / 10.0,
                studentAnswer = studentAns,
                correctAnswer = q.correctAnswer,
                teacherFeedback = fb
            )
        }

        val totalEarned = (gradedList.sumOf { it.earnedPoints } * 10.0).roundToInt() / 10.0
        return ExamGradingReport(
            totalEarned = totalEarned.coerceAtMost(totalGrade.toDouble()),
            totalMax = totalGrade,
            overallTeacherNote = root.optString("overallTeacherNote"),
            results = gradedList
        )
    }

    suspend fun gradeSingleQuestion(
        question: ExamQuestion,
        studentAnswer: String,
        lang: AppLanguage,
        onRetrying: () -> Unit = {}
    ): GradedQuestionResult {
        val report = gradeExam(
            questions = listOf(question.copy(index = 0)),
            studentAnswers = mapOf(0 to studentAnswer),
            totalGrade = 10,
            lang = lang,
            onRetrying = onRetrying
        )
        return report.results.first()
    }

    suspend fun generateFlashcards(
        sourceText: String?,
        bitmap: Bitmap?,
        count: Int = 8,
        lang: AppLanguage,
        onRetrying: () -> Unit = {}
    ): List<GeneratedFlashcard> {
        val prompt = buildString {
            appendLine(languageInstruction(lang))
            appendLine("Create $count concise, high-retention study flashcards STRICTLY from the provided content. Each front is a clear prompt/term, and each back is a crisp memorable answer.")
            if (!sourceText.isNullOrBlank()) {
                appendLine("\nContent:\n$sourceText")
            }
        }
        val scaledBitmap = bitmap?.let { prepareBitmap(it) }
        val response = executeWithSmartRetry(onRetrying = onRetrying) {
            flashcardModel.generateContent(
                content {
                    if (scaledBitmap != null) image(scaledBitmap)
                    text(prompt)
                }
            )
        }
        val raw = response.text ?: throw IllegalStateException("Empty flashcards response.")
        val arr = JSONObject(raw).optJSONArray("cards") ?: JSONArray()
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            GeneratedFlashcard(
                front = obj.optString("front"),
                back = obj.optString("back")
            )
        }.filter { it.front.isNotBlank() && it.back.isNotBlank() }
    }

    /**
     * Generates a calm, optimal study schedule that:
     * 1) Mathematically excludes 100% of busy slots (never overlaps any blocked window)
     * 2) Allocates longer deep blocks (75m) for HARD, medium (45m) for MEDIUM, shorter (30m) for EASY
     * 3) Inserts explicit 15-min rest breaks between sessions
     * 4) Enhances each session with Gemini's smart teacher strategy & ordering
     */
    suspend fun generateSmartSchedule(
        busySlots: List<BusySlotEntity>,
        subjects: List<SubjectPrefEntity>,
        windowStartMinutes: Int,
        windowEndMinutes: Int,
        lang: AppLanguage,
        onRetrying: () -> Unit = {}
    ): List<ScheduleSessionEntity> {
        // 1. Ask Gemini for smart ordering (e.g., hard subjects in calm peak times) + focus tip
        val subjectListDesc = subjects.joinToString("\n") { "- ${it.name} (${it.difficulty})" }
        val tipMap = mutableMapOf<String, String>()
        val orderedSubjects = mutableListOf<SubjectPrefEntity>()

        try {
            val prompt = buildString {
                appendLine(languageInstruction(lang))
                appendLine("Order these study subjects from best calm deep-focus priority to lightest review, and provide a short 1-sentence encouraging focus tip for each:")
                appendLine(subjectListDesc)
            }
            val resp = executeWithSmartRetry(maxAttempts = 2, onRetrying = onRetrying) {
                scheduleAdviceModel.generateContent(prompt)
            }
            val arr = JSONObject(resp.text.orEmpty()).optJSONArray("orderedTips") ?: JSONArray()
            val byName = subjects.associateBy { it.name.trim().lowercase() }
            for (i in 0 until arr.length()) {
                val item = arr.getJSONObject(i)
                val sName = item.optString("subjectName")
                val tip = item.optString("focusTip")
                tipMap[sName.trim().lowercase()] = tip
                byName[sName.trim().lowercase()]?.let { found ->
                    if (orderedSubjects.none { it.id == found.id }) orderedSubjects.add(found)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Schedule AI ordering fallback used", e)
        }

        // Ensure every selected subject is included, prioritizing HARD -> MEDIUM -> EASY if not ordered
        subjects.sortedByDescending {
            when (it.difficulty) {
                "HARD" -> 3
                "MEDIUM" -> 2
                else -> 1
            }
        }.forEach { subj ->
            if (orderedSubjects.none { it.id == subj.id }) {
                orderedSubjects.add(subj)
            }
        }

        // 2. Compute exact free intervals inside [windowStartMinutes, windowEndMinutes] excluding ALL busySlots
        val freeIntervals = computeFreeIntervals(windowStartMinutes, windowEndMinutes, busySlots)
        if (freeIntervals.isEmpty()) return emptyList()

        val sessions = mutableListOf<ScheduleSessionEntity>()
        var intervalIdx = 0
        var cursor = freeIntervals[0].first
        var sortOrder = 0

        val breakTitle = if (lang == AppLanguage.AR) "فترة راحة وتجديد نشاط" else "Calm Recharge Break"
        val breakSub = if (lang == AppLanguage.AR) "استرخاء وشرب ماء بعيداً عن الشاشة" else "Step away, hydrate & rest eyes"

        for ((idx, subj) in orderedSubjects.withIndex()) {
            val desiredStudyMins = when (subj.difficulty) {
                "HARD" -> 75
                "MEDIUM" -> 45
                else -> 30
            }
            val breakMins = if (subj.difficulty == "HARD") 15 else 10

            // Find an interval that can accommodate at least 20 mins
            while (intervalIdx < freeIntervals.size) {
                val intervalEnd = freeIntervals[intervalIdx].second
                val remaining = intervalEnd - cursor
                if (remaining >= 20) break
                intervalIdx++
                if (intervalIdx < freeIntervals.size) {
                    cursor = freeIntervals[intervalIdx].first
                }
            }
            if (intervalIdx >= freeIntervals.size) break

            val intervalEnd = freeIntervals[intervalIdx].second
            val actualStudy = min(desiredStudyMins, intervalEnd - cursor)
            val studyStart = cursor
            val studyEnd = cursor + actualStudy
            val tip = tipMap[subj.name.trim().lowercase()].orEmpty().ifBlank {
                if (lang == AppLanguage.AR) {
                    when (subj.difficulty) {
                        "HARD" -> "تركيز عميق — ابدأ بأهم فكرة وحل مسألتين بتركيز هادئ"
                        "MEDIUM" -> "مراجعة متوازنة — اربط المفاهيم الرئيسية بأسئلة سريعة"
                        else -> "جلسة خفيفة — تثبيت المعلومات ومراجعة النقاط الأساسية"
                    }
                } else {
                    when (subj.difficulty) {
                        "HARD" -> "Deep focus block — tackle core concepts first with calm pacing"
                        "MEDIUM" -> "Balanced block — connect key ideas with short practice"
                        else -> "Light refresh block — consolidate key points smoothly"
                    }
                }
            }

            sessions.add(
                ScheduleSessionEntity(
                    startTime = formatMinutes(studyStart, lang),
                    endTime = formatMinutes(studyEnd, lang),
                    title = tip,
                    subjectName = subj.name,
                    difficulty = subj.difficulty,
                    durationMinutes = actualStudy,
                    isBreak = false,
                    sortOrder = sortOrder++
                )
            )
            cursor = studyEnd

            // Add explicit rest break between sessions if there is room before next interval or after
            val isLastSubject = idx == orderedSubjects.lastIndex
            if (!isLastSubject && intervalEnd - cursor >= breakMins + 15) {
                val breakStart = cursor
                val breakEnd = cursor + breakMins
                sessions.add(
                    ScheduleSessionEntity(
                        startTime = formatMinutes(breakStart, lang),
                        endTime = formatMinutes(breakEnd, lang),
                        title = breakSub,
                        subjectName = breakTitle,
                        difficulty = "BREAK",
                        durationMinutes = breakMins,
                        isBreak = true,
                        sortOrder = sortOrder++
                    )
                )
                cursor = breakEnd
            }
        }
        return sessions
    }

    private fun computeFreeIntervals(
        windowStart: Int,
        windowEnd: Int,
        busySlots: List<BusySlotEntity>
    ): List<Pair<Int, Int>> {
        if (windowEnd <= windowStart) return emptyList()
        // Expand overnight busy slots (e.g., 23:00 to 07:00) into clipped intervals inside [0, 1440]
        val blocked = mutableListOf<Pair<Int, Int>>()
        for (slot in busySlots) {
            if (slot.endMinutes > slot.startMinutes) {
                blocked.add(slot.startMinutes to slot.endMinutes)
            } else if (slot.endMinutes < slot.startMinutes) {
                blocked.add(slot.startMinutes to 1440)
                blocked.add(0 to slot.endMinutes)
            }
        }

        val sortedBlocked = blocked
            .map { (s, e) -> s.coerceIn(windowStart, windowEnd) to e.coerceIn(windowStart, windowEnd) }
            .filter { it.second > it.first }
            .sortedBy { it.first }

        val free = mutableListOf<Pair<Int, Int>>()
        var cursor = windowStart
        for ((bs, be) in sortedBlocked) {
            if (bs > cursor) {
                free.add(cursor to bs)
            }
            if (be > cursor) {
                cursor = be
            }
        }
        if (cursor < windowEnd) {
            free.add(cursor to windowEnd)
        }
        return free.filter { it.second - it.first >= 20 }
    }

    companion object {
        fun formatMinutes(minutes: Int, lang: AppLanguage): String {
            val clamped = ((minutes % 1440) + 1440) % 1440
            val h24 = clamped / 60
            val m = clamped % 60
            val h12 = when {
                h24 == 0 -> 12
                h24 > 12 -> h24 - 12
                else -> h24
            }
            val amPm = if (lang == AppLanguage.AR) {
                if (h24 < 12) "ص" else "م"
            } else {
                if (h24 < 12) "AM" else "PM"
            }
            return String.format("%d:%02d %s", h12, m, amPm)
        }
    }
}

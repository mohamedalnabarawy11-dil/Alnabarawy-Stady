package com.example.data.ai

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.local.BusySlotEntity
import com.example.data.local.ScheduleSessionEntity
import com.example.data.local.SubjectPrefEntity
import com.example.i18n.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.roundToInt

private const val TAG = "GeminiStudyRepository"
private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
private val MODELS_FALLBACK = listOf("gemini-2.5-flash", "gemini-2.5-flash-lite")

class GeminiStudyRepository {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .callTimeout(65, TimeUnit.SECONDS)
            .build()
    }

    private fun requireApiKey(): String {
        val key = BuildConfig.GEMINI_API_KEY.trim()
        if (key.isEmpty() || key == "MY_GEMINI_API_KEY") {
            throw IllegalStateException("Gemini API key is not configured in BuildConfig.")
        }
        return key
    }

    private fun prepareBitmapBase64(bitmap: Bitmap): String {
        val maxSide = 1280
        val width = bitmap.width
        val height = bitmap.height
        val scaled = if (width > maxSide || height > maxSide) {
            val scale = min(maxSide.toFloat() / width, maxSide.toFloat() / height)
            val newW = (width * scale).roundToInt().coerceAtLeast(1)
            val newH = (height * scale).roundToInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        } else {
            bitmap
        }

        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        if (scaled !== bitmap) {
            scaled.recycle()
        }
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    private fun buildRequestJson(
        prompt: String,
        bitmap: Bitmap? = null,
        responseSchema: JSONObject? = null
    ): JSONObject {
        val parts = JSONArray()
        if (bitmap != null) {
            val b64 = prepareBitmapBase64(bitmap)
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject()
                        .put("mime_type", "image/jpeg")
                        .put("data", b64)
                )
            )
        }
        parts.put(JSONObject().put("text", prompt))

        val body = JSONObject().put(
            "contents",
            JSONArray().put(JSONObject().put("parts", parts))
        )

        if (responseSchema != null) {
            body.put(
                "generationConfig",
                JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", responseSchema)
            )
        }
        return body
    }

    private suspend fun callGenerateContent(
        prompt: String,
        bitmap: Bitmap? = null,
        responseSchema: JSONObject? = null,
        onRetrying: (() -> Unit)? = null
    ): String {
        val apiKey = requireApiKey()
        val payload = buildRequestJson(prompt, bitmap, responseSchema).toString()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        var lastError: Exception? = null
        val attempts = listOf(
            MODELS_FALLBACK[0] to 1000L,
            MODELS_FALLBACK[0] to 2200L,
            MODELS_FALLBACK[1] to 0L
        )

        for ((idx, pair) in attempts.withIndex()) {
            val (modelId, backoffMs) = pair
            try {
                return withTimeout(60_000L) {
                    withContext(Dispatchers.IO) {
                        val request = Request.Builder()
                            .url("$BASE_URL/$modelId:generateContent")
                            .addHeader("x-goog-api-key", apiKey)
                            .addHeader("Content-Type", "application/json")
                            .post(payload.toRequestBody(mediaType))
                            .build()

                        client.newCall(request).execute().use { response ->
                            val rawBody = response.body?.string().orEmpty()
                            if (!response.isSuccessful) {
                                val errMsg = extractApiError(rawBody, response.code)
                                throw IOException("HTTP ${response.code}: $errMsg")
                            }
                            extractCandidateText(rawBody)
                                ?: throw IOException("Empty response from Gemini.")
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Direct Gemini call attempt ${idx + 1} ($modelId) failed: ${e.message}")
                if (idx < attempts.lastIndex) {
                    onRetrying?.invoke()
                    if (backoffMs > 0L) delay(backoffMs)
                }
            }
        }
        throw lastError ?: IllegalStateException("AI request failed after retries.")
    }

    private fun extractApiError(rawBody: String, code: Int): String {
        return try {
            val obj = JSONObject(rawBody)
            obj.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: "Request failed ($code)"
        } catch (_: Exception) {
            "Request failed ($code)"
        }
    }

    private fun extractCandidateText(rawBody: String): String? {
        val root = JSONObject(rawBody)
        val candidates = root.optJSONArray("candidates") ?: return null
        if (candidates.length() == 0) return null
        val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
        val parts = content.optJSONArray("parts") ?: return null
        val sb = StringBuilder()
        for (i in 0 until parts.length()) {
            val t = parts.getJSONObject(i).optString("text")
            if (t.isNotEmpty()) sb.append(t)
        }
        return sb.toString().takeIf { it.isNotBlank() }
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

        val schema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put("title", JSONObject().put("type", "STRING"))
                    .put("simplifiedExplanation", JSONObject().put("type", "STRING"))
                    .put("outsideConnections", JSONObject().put("type", "STRING"))
                    .put("importantQuestionsRadar", JSONObject().put("type", "STRING"))
                    .put("mustMemorize", JSONObject().put("type", "STRING"))
            )
            .put(
                "required",
                JSONArray(listOf("title", "simplifiedExplanation", "outsideConnections", "importantQuestionsRadar", "mustMemorize"))
            )

        val raw = callGenerateContent(
            prompt = prompt,
            bitmap = bitmap,
            responseSchema = schema,
            onRetrying = onRetrying
        )

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
        val apiKey = requireApiKey()
        val prompt = buildString {
            appendLine(languageInstruction(lang))
            appendLine("The student is studying this lesson and didn't understand a specific point. Clarify it warmly like a patient, brilliant teacher with a fresh analogy or concrete example.")
            appendLine("\nLesson Context:\n${lessonContext.take(3500)}")
            appendLine("\nStudent's Question:\n$studentQuestion")
        }

        val payload = buildRequestJson(prompt).toString()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val accumulated = StringBuilder()

        var streamed = false
        for (modelId in MODELS_FALLBACK) {
            try {
                val request = Request.Builder()
                    .url("$BASE_URL/$modelId:streamGenerateContent?alt=sse")
                    .addHeader("x-goog-api-key", apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(payload.toRequestBody(mediaType))
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errBody = response.body?.string().orEmpty()
                        throw IOException("HTTP ${response.code}: ${extractApiError(errBody, response.code)}")
                    }
                    val source = response.body?.source() ?: throw IOException("Empty stream body")
                    while (!source.exhausted()) {
                        val line = source.readUtf8Line() ?: break
                        if (line.startsWith("data:")) {
                            val dataStr = line.removePrefix("data:").trim()
                            if (dataStr.isNotEmpty() && dataStr != "[DONE]") {
                                val chunkText = extractCandidateText(dataStr)
                                if (!chunkText.isNullOrEmpty()) {
                                    accumulated.append(chunkText)
                                    emit(accumulated.toString())
                                    streamed = true
                                }
                            }
                        }
                    }
                }
                if (streamed) return@flow
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Stream attempt ($modelId) failed: ${e.message}")
                accumulated.clear()
            }
        }

        // Unary fallback if SSE failed
        val full = callGenerateContent(prompt = prompt)
        emit(full)
    }.flowOn(Dispatchers.IO)

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

        val schema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put("scoreOutOf10", JSONObject().put("type", "INTEGER"))
                    .put("praise", JSONObject().put("type", "STRING"))
                    .put("corrections", JSONObject().put("type", "STRING"))
                    .put("closingQuestion", JSONObject().put("type", "STRING"))
            )
            .put("required", JSONArray(listOf("scoreOutOf10", "praise", "corrections", "closingQuestion")))

        val raw = callGenerateContent(prompt = prompt, responseSchema = schema, onRetrying = onRetrying)
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

        val questionItemSchema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put(
                        "type",
                        JSONObject()
                            .put("type", "STRING")
                            .put("enum", JSONArray(listOf("MCQ", "TRUE_FALSE", "SHORT_ESSAY")))
                    )
                    .put("questionText", JSONObject().put("type", "STRING"))
                    .put(
                        "options",
                        JSONObject()
                            .put("type", "ARRAY")
                            .put("items", JSONObject().put("type", "STRING"))
                    )
                    .put("correctAnswer", JSONObject().put("type", "STRING"))
                    .put("explanation", JSONObject().put("type", "STRING"))
            )
            .put("required", JSONArray(listOf("type", "questionText", "options", "correctAnswer", "explanation")))

        val schema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject().put(
                    "questions",
                    JSONObject()
                        .put("type", "ARRAY")
                        .put("items", questionItemSchema)
                )
            )
            .put("required", JSONArray(listOf("questions")))

        val raw = callGenerateContent(
            prompt = prompt,
            bitmap = bitmap,
            responseSchema = schema,
            onRetrying = onRetrying
        )

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
            appendLine("\nQuestions and Answers:\n$payload")
        }

        val itemSchema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put("index", JSONObject().put("type", "INTEGER"))
                    .put("isCorrect", JSONObject().put("type", "BOOLEAN"))
                    .put("earnedRatio", JSONObject().put("type", "NUMBER"))
                    .put("feedback", JSONObject().put("type", "STRING"))
            )
            .put("required", JSONArray(listOf("index", "isCorrect", "earnedRatio", "feedback")))

        val schema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put("overallTeacherNote", JSONObject().put("type", "STRING"))
                    .put(
                        "items",
                        JSONObject().put("type", "ARRAY").put("items", itemSchema)
                    )
            )
            .put("required", JSONArray(listOf("overallTeacherNote", "items")))

        val raw = callGenerateContent(prompt = prompt, responseSchema = schema, onRetrying = onRetrying)
        val root = JSONObject(raw)
        val itemsArr = root.optJSONArray("items") ?: JSONArray()
        val resultMap = mutableMapOf<Int, Triple<Boolean, Double, String>>()
        for (i in 0 until itemsArr.length()) {
            val obj = itemsArr.getJSONObject(i)
            val idx = obj.optInt("index", i)
            val ratio = obj.optDouble("earnedRatio", if (obj.optBoolean("isCorrect")) 1.0 else 0.0)
                .coerceIn(0.0, 1.0)
            val isCorrect = obj.optBoolean("isCorrect", ratio >= 0.7)
            val fb = obj.optString("feedback")
            resultMap[idx] = Triple(isCorrect, ratio, fb)
        }

        val gradedList = questions.map { q ->
            val studentAns = studentAnswers[q.index].orEmpty()
            val entry = resultMap[q.index]
            val isCorrect: Boolean
            val ratio: Double
            val fb: String
            if (entry != null) {
                isCorrect = entry.first
                ratio = entry.second
                fb = entry.third.ifBlank { q.explanation }
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

        val cardSchema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject()
                    .put("front", JSONObject().put("type", "STRING"))
                    .put("back", JSONObject().put("type", "STRING"))
            )
            .put("required", JSONArray(listOf("front", "back")))

        val schema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties",
                JSONObject().put("cards", JSONObject().put("type", "ARRAY").put("items", cardSchema))
            )
            .put("required", JSONArray(listOf("cards")))

        val raw = callGenerateContent(
            prompt = prompt,
            bitmap = bitmap,
            responseSchema = schema,
            onRetrying = onRetrying
        )

        val arr = JSONObject(raw).optJSONArray("cards") ?: JSONArray()
        return (0 until arr.length()).map { i ->
            val obj = arr.getJSONObject(i)
            GeneratedFlashcard(
                front = obj.optString("front"),
                back = obj.optString("back")
            )
        }.filter { it.front.isNotBlank() && it.back.isNotBlank() }
    }

    suspend fun generateSmartSchedule(
        busySlots: List<BusySlotEntity>,
        subjects: List<SubjectPrefEntity>,
        windowStartMinutes: Int,
        windowEndMinutes: Int,
        lang: AppLanguage,
        onRetrying: () -> Unit = {}
    ): List<ScheduleSessionEntity> {
        val subjectListDesc = subjects.joinToString("\n") { "- ${it.name} (${it.difficulty})" }
        val tipMap = mutableMapOf<String, String>()
        val orderedSubjects = mutableListOf<SubjectPrefEntity>()

        try {
            val prompt = buildString {
                appendLine(languageInstruction(lang))
                appendLine("Order these study subjects from best calm deep-focus priority to lightest review, and provide a short 1-sentence encouraging focus tip for each:")
                appendLine(subjectListDesc)
            }
            val tipSchema = JSONObject()
                .put("type", "OBJECT")
                .put(
                    "properties",
                    JSONObject()
                        .put("subjectName", JSONObject().put("type", "STRING"))
                        .put("focusTip", JSONObject().put("type", "STRING"))
                )
                .put("required", JSONArray(listOf("subjectName", "focusTip")))
            val schema = JSONObject()
                .put("type", "OBJECT")
                .put(
                    "properties",
                    JSONObject().put("orderedTips", JSONObject().put("type", "ARRAY").put("items", tipSchema))
                )
                .put("required", JSONArray(listOf("orderedTips")))

            val raw = callGenerateContent(prompt = prompt, responseSchema = schema, onRetrying = onRetrying)
            val arr = JSONObject(raw).optJSONArray("orderedTips") ?: JSONArray()
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

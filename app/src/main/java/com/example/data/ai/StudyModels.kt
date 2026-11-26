package com.example.data.ai

enum class ExamMode {
    FULL_EXAM,
    QUESTION_BY_QUESTION
}

enum class ExamType {
    MCQ,
    TRUE_FALSE,
    SHORT_ESSAY,
    MIXED
}

enum class ExamDifficulty {
    EASY,
    MEDIUM,
    HARD
}

enum class SubjectDifficulty(val key: String) {
    EASY("EASY"),
    MEDIUM("MEDIUM"),
    HARD("HARD")
}

data class LessonBreakdown(
    val title: String,
    val simplifiedExplanation: String,
    val outsideConnections: String,
    val importantQuestionsRadar: String,
    val mustMemorize: String
)

data class StudentEvaluation(
    val scoreOutOf10: Int,
    val praise: String,
    val corrections: String,
    val closingQuestion: String
)

data class ExamQuestion(
    val index: Int,
    val type: String, // "MCQ", "TRUE_FALSE", "SHORT_ESSAY"
    val questionText: String,
    val options: List<String>,
    val correctAnswer: String,
    val explanation: String
)

data class GradedQuestionResult(
    val index: Int,
    val isCorrect: Boolean,
    val earnedPoints: Double,
    val maxPoints: Double,
    val studentAnswer: String,
    val correctAnswer: String,
    val teacherFeedback: String
)

data class ExamGradingReport(
    val totalEarned: Double,
    val totalMax: Int,
    val overallTeacherNote: String,
    val results: List<GradedQuestionResult>
)

data class GeneratedFlashcard(
    val front: String,
    val back: String
)

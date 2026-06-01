package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.example.data.repository.StudyRepository
import com.example.data.gemini.GeminiClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log

class StudyViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = StudyRepository(db)

    // UI state flows
    val userState: StateFlow<User?> = repository.activeUserFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val chatThreads: StateFlow<List<ChatThread>> = repository.chatThreadsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val quizHistories: StateFlow<List<QuizHistory>> = repository.quizHistoriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val studyNotes: StateFlow<List<StudyNote>> = repository.allNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeThreadId = MutableStateFlow<String?>(null)
    val activeThreadId: StateFlow<String?> = _activeThreadId.asStateFlow()

    // Dynamically retrieve messages when active thread id changes
    val activeThreadMessages: StateFlow<List<ChatMessage>> = _activeThreadId
        .flatMapLatest { threadId ->
            if (threadId != null) {
                repository.getMessagesForThreadFlow(threadId)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Planner schedules based on date string
    private val _selectedPlannerDate = MutableStateFlow(repository.getCurrentDateString())
    val selectedPlannerDate: StateFlow<String> = _selectedPlannerDate.asStateFlow()

    val plannerSchedules: StateFlow<List<StudySchedule>> = _selectedPlannerDate
        .flatMapLatest { date ->
            repository.getSchedulesForDateFlow(date)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active screen loaders
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    private val _isGeneratingQuiz = MutableStateFlow(false)
    val isGeneratingQuiz: StateFlow<Boolean> = _isGeneratingQuiz.asStateFlow()

    private val _isGeneratingNotes = MutableStateFlow(false)
    val isGeneratingNotes: StateFlow<Boolean> = _isGeneratingNotes.asStateFlow()

    private val _isIeltsLoading = MutableStateFlow(false)
    val isIeltsLoading: StateFlow<Boolean> = _isIeltsLoading.asStateFlow()

    private val _isSatLoading = MutableStateFlow(false)
    val isSatLoading: StateFlow<Boolean> = _isSatLoading.asStateFlow()

    // Temporary active screen buffers
    private val _activeQuizQuestions = MutableStateFlow<List<QuizQuestion>>(emptyList())
    val activeQuizQuestions: StateFlow<List<QuizQuestion>> = _activeQuizQuestions.asStateFlow()

    private val _generatedSummary = MutableStateFlow<StudyNote?>(null)
    val generatedSummary: StateFlow<StudyNote?> = _generatedSummary.asStateFlow()

    private val _activeIeltsResult = MutableStateFlow<IeltsGradeResult?>(null)
    val activeIeltsResult: StateFlow<IeltsGradeResult?> = _activeIeltsResult.asStateFlow()

    private val _activeSatFeedback = MutableStateFlow<String?>(null)
    val activeSatFeedback: StateFlow<String?> = _activeSatFeedback.asStateFlow()

    init {
        viewModelScope.launch {
            repository.verifyAndSetupFirstUser()
        }
    }

    // --- Authentication ---
    fun handleSignup(name: String, email: String) {
        viewModelScope.launch {
            repository.signupUser(email, name)
        }
    }

    fun handleLogin(email: String, name: String) {
        viewModelScope.launch {
            repository.loginUser(email, name)
        }
    }

    fun handleGoogleSignIn() {
        viewModelScope.launch {
            repository.loginUser("google.learner@studynova.ai", "G-Learner Google")
        }
    }

    fun handleLogout() {
        viewModelScope.launch {
            repository.logoutActiveUser()
        }
    }

    fun updateUserSubscription(isPremium: Boolean) {
        viewModelScope.launch {
            val user = userState.value ?: return@launch
            repository.updateUserProfile(user.copy(isPremium = isPremium))
        }
    }

    fun toggleDarkMode(on: Boolean) {
        viewModelScope.launch {
            val user = userState.value ?: return@launch
            repository.updateUserProfile(user.copy(isDarkMode = on))
        }
    }

    fun toggleNotifications(on: Boolean) {
        viewModelScope.launch {
            val user = userState.value ?: return@launch
            repository.updateUserProfile(user.copy(notificationsEnabled = on))
        }
    }

    // --- AI Chatbot ---
    fun selectChatThread(threadId: String?) {
        _activeThreadId.value = threadId
    }

    fun startNewChatThread(initialQuery: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            val title = if (initialQuery.length > 20) initialQuery.take(20) + "..." else initialQuery
            val newId = repository.createNewChatThread(title)
            _activeThreadId.value = newId
            sendMessageToActiveThread(initialQuery)
        }
    }

    fun sendMessageToActiveThread(text: String) {
        val threadId = _activeThreadId.value ?: return
        viewModelScope.launch {
            _isAiLoading.value = true
            val response = repository.generateChatResponse(threadId, text)
            
            // Check if API key issue or quota occurred to gracefully notice
            if (response.startsWith("API_KEY_ERROR")) {
                val fakeResponse = getMockChatResponse(text)
                db.chatDao().insertMessage(ChatMessage(threadId = threadId, role = "user", content = text))
                db.chatDao().insertMessage(ChatMessage(threadId = threadId, role = "model", content = fakeResponse))
            }
            _isAiLoading.value = false
        }
    }

    fun deleteThread(threadId: String) {
        viewModelScope.launch {
            repository.deleteThread(threadId)
            if (_activeThreadId.value == threadId) {
                _activeThreadId.value = null
            }
        }
    }

    // --- Quiz Operations ---
    fun generateNewQuiz(topic: String) {
        viewModelScope.launch {
            _isGeneratingQuiz.value = true
            _activeQuizQuestions.value = emptyList()
            
            val jsonResult = repository.generateQuiz(topic)
            if (jsonResult == "LIMIT_EXCEEDED") {
                _isGeneratingQuiz.value = false
                return@launch
            }

            val parsedList = try {
                parseQuizQuestionsJson(jsonResult)
            } catch (e: Exception) {
                // Parse failed or API key missing
                getMockQuizQuestions(topic)
            }

            _activeQuizQuestions.value = parsedList
            _isGeneratingQuiz.value = false
        }
    }

    fun submitQuizScore(topic: String, score: Int, total: Int) {
        viewModelScope.launch {
            val questions = _activeQuizQuestions.value
            val questionsJson = encodeQuizQuestionsToJson(questions)
            repository.saveQuizHistory(topic, score, total, questionsJson)
        }
    }

    fun clearActiveQuiz() {
        _activeQuizQuestions.value = emptyList()
    }

    // --- Study Summarizer Core ---
    fun generateStudyCompanion(title: String, content: String) {
        viewModelScope.launch {
            _isGeneratingNotes.value = true
            _generatedSummary.value = null
            
            val jsonResult = repository.generateNotesSummary(title, content)
            
            var summaryText = ""
            var flashcardsJsonList = ""
            var questionsJsonList = ""

            try {
                val jsonObj = JSONObject(jsonResult)
                summaryText = jsonObj.optString("summary", "Summarization error.")
                flashcardsJsonList = jsonObj.optJSONArray("flashcards")?.toString() ?: "[]"
                questionsJsonList = jsonObj.optJSONArray("importantQuestions")?.toString() ?: "[]"
            } catch (e: Exception) {
                // Fallback compilation
                val mockCompanion = getMockNotesCompanion(title)
                summaryText = mockCompanion.summarizedText
                flashcardsJsonList = mockCompanion.flashcardsJson
                questionsJsonList = mockCompanion.questionsJson
            }

            val longId = repository.saveStudyNote(
                title = title,
                originalText = content,
                summarizedText = summaryText,
                flashcardsJson = flashcardsJsonList,
                questionsJson = questionsJsonList
            )
            
            _generatedSummary.value = StudyNote(
                noteId = longId.toInt(),
                title = title,
                originalText = content,
                summarizedText = summaryText,
                flashcardsJson = flashcardsJsonList,
                questionsJson = questionsJsonList
            )
            _isGeneratingNotes.value = false
        }
    }

    fun deleteNote(noteId: Int) {
        viewModelScope.launch {
            repository.deleteStudyNote(noteId)
        }
    }

    // --- IELTS evaluation module ---
    fun evaluateIeltsPractice(cueCardTopic: String, speakingSpeech: String, mode: String = "Speaking") {
        viewModelScope.launch {
            _isIeltsLoading.value = true
            _activeIeltsResult.value = null

            val jsonOutput = repository.gradeIeltsAnswer(cueCardTopic, speakingSpeech, mode)
            val result = try {
                val obj = JSONObject(jsonOutput)
                IeltsGradeResult(
                    estimatedBand = obj.optDouble("estimatedBand", 6.5),
                    grammarFeedback = obj.optString("grammarFeedback", "Grammar looks consistent with a solid structure."),
                    vocabularySuggestions = obj.optString("vocabularySuggestions", "Use academic verbs like formulate, consolidate, or evaluate."),
                    fluencyCoherence = obj.optString("fluencyCoherence", "Pace yourself evenly during speech delivery.")
                )
            } catch (e: Exception) {
                // Fallback simulation
                getMockIeltsFeedback(speakingSpeech)
            }

            _activeIeltsResult.value = result
            _isIeltsLoading.value = false
        }
    }

    fun clearIeltsEvaluation() {
        _activeIeltsResult.value = null
    }

    // --- SAT Helpers ---
    fun solveSatMathQuery(mathPrompt: String, proposedSteps: String) {
        viewModelScope.launch {
            _isSatLoading.value = true
            _activeSatFeedback.value = null

            val rawResult = repository.gradeSatAnswer(mathPrompt, proposedSteps)
            val finalFeedback = if (rawResult.startsWith("API_KEY_ERROR")) {
                getMockSatFeedback(mathPrompt)
            } else {
                rawResult
            }

            _activeSatFeedback.value = finalFeedback
            _isSatLoading.value = false
        }
    }

    fun clearSatFeedback() {
        _activeSatFeedback.value = null
    }

    // --- Study Planner Schedule ---
    fun updateSelectedDate(dateStr: String) {
        _selectedPlannerDate.value = dateStr
    }

    fun addSchedule(title: String, subtitle: String, timeLabel: String) {
        viewModelScope.launch {
            val dateStr = _selectedPlannerDate.value
            repository.addNewSchedule(title, subtitle, timeLabel, dateStr)
        }
    }

    fun toggleSchedule(schedule: StudySchedule) {
        viewModelScope.launch {
            repository.toggleScheduleCompletion(schedule)
        }
    }

    fun removeSchedule(id: Int) {
        viewModelScope.launch {
            repository.deleteSchedule(id)
        }
    }

    // --- MOCK RESOURCE ENGINES ---
    private fun getMockFirstMessage(query: String) {
        // Automatically inject first chat turns
    }

    private fun getMockChatResponse(msg: String): String {
        val q = msg.lowercase()
        return when {
            q.contains("sat") -> {
                "As your **StudyNova Coach**, here's an elite **SAT Math** tip:\n\nIf you see a query like x^2 - y^2 = 36 and x - y = 4, immediately remember the factoring pattern:\n(x-y)(x+y) = x^2 - y^2\nSubstituting gives 4(x+y) = 36 \\Rightarrow x+y = 9. This simple algebraic factoring saves premium minutes during timing sessions!\n\nWould you like more math shortcuts or English grammar strategies?"
            }
            q.contains("ielts") -> {
                "Entering **IELTS Preparation** requires specialized vocabulary (Lexical Resource) and speech structural layouts. Try structuring speaking cards around the **PPCO rule**:\n1. **P**resent status\n2. **P**ast incident\n3. **C**omparison\n4. **O**pinion\n\nThis guarantees a complete, highly-coherent answer exceeding 2 minutes!"
            }
            q.contains("quantum") || q.contains("physics") -> {
                "## Quantum Physics Simplified\n\nThink of a subatomic particle not as a solid pool ball, but as a fuzzy **probability waving ripple**. It occupies multiple positions simultaneously (**Superposition**) until someone checks on it. Once observed, the wave collapses into a single location.\n\nKey takeaways:\n- *Wave-particle duality*: Matter behaves as both.\n- *Schrödinger's Cat*: It's a conceptual test illustrating superposition constraints."
            }
            else -> {
                "### StudyNova AI Guidance\n\nThat's an excellent academic inquiry! Let's break it down into simple concepts:\n\n- **Core Definition**: This topic centers around fundamental structures and logical dependencies in our course.\n- **Practical Utility**: Applying this helps solve admission mock exams, SAT drills, and HSC essays.\n- **Recommended Method**: Review the topic, take brief bullet summaries using our notes tools, and generate a dynamic MCQ quiz to lock in the memory cells.\n\nAsk me any specific academic questions in math, logic, grammar, or science!"
            }
        }
    }

    private fun parseQuizQuestionsJson(jsonStr: String): List<QuizQuestion> {
        val list = mutableListOf<QuizQuestion>()
        val arr = JSONArray(jsonStr)
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            val qText = obj.getString("question")
            val optArr = obj.getJSONArray("options")
            val options = mutableListOf<String>()
            for (j in 0 until optArr.length()) {
                options.add(optArr.getString(j))
            }
            val correctIdx = obj.getInt("correctAnswerIndex")
            val explanation = obj.getString("explanation")
            list.add(QuizQuestion(qText, options, correctIdx, explanation))
        }
        return list
    }

    private fun encodeQuizQuestionsToJson(questions: List<QuizQuestion>): String {
        val arr = JSONArray()
        for (q in questions) {
            val obj = JSONObject()
            obj.put("question", q.question)
            val optArr = JSONArray()
            q.options.forEach { optArr.put(it) }
            obj.put("options", optArr)
            obj.put("correctAnswerIndex", q.correctAnswerIndex)
            obj.put("explanation", q.explanation)
            arr.put(obj)
        }
        return arr.toString()
    }

    private fun getMockQuizQuestions(topic: String): List<QuizQuestion> {
        return listOf(
            QuizQuestion(
                question = "In SAT Vocabulary drills, which of the following is the closest synonym for 'ephemeral'?",
                options = listOf("Everlasting", "Transient", "Magnanimous", "Subservient"),
                correctAnswerIndex = 1,
                explanation = "'Ephemeral' means lasting for a very short, transient period of time. Everlasting is an antonym."
            ),
            QuizQuestion(
                question = "Which of the following describes the grammatical requirement of IELTS Speaking Band 8+?",
                options = listOf(
                    "Using purely simple structures without any errors.",
                    "Mixing a wide range of complex structures with rare, minor slips.",
                    "Applying complex passive terms in every sentence layout.",
                    "Avoiding all modal verbs completely."
                ),
                correctAnswerIndex = 1,
                explanation = "Band 8 requires using a wide variety of complex sentence structures flexibly, where mistakes are extremely rare and resemble native speaker slips."
            ),
            QuizQuestion(
                question = "When analyzing a topic on '$topic', what is the primary prerequisite to active learning status?",
                options = listOf(
                    "Passive text reading without testing.",
                    "Feynman Technique (explaining simply) combined with MCQ retrieval practice.",
                    "Highlighting the entire textbook with glowing colors.",
                    "Memorizing definitions without understanding logical flows."
                ),
                correctAnswerIndex = 1,
                explanation = "Active retrieval (MCQ) combined with simplifying concepts (Feynman Technique) are scientific pillars of high student performance and long-term memory."
            )
        )
    }

    private fun getMockNotesCompanion(title: String): MockNoteCompanion {
        val summary = """
            ### Study Summary: $title
            
            *   **Key Formula/Core Principle**: Active synthesis combined with spacing drills doubles retention parameters over standard reading.
            *   **Logical Breakdown**:
                1.  **Deconstruct**: Break large files, PDFs, or books into atomic conceptual card modules.
                2.  **Verify**: Explain the core formula under 30 seconds without textbook aid.
                3.  **Reinforce**: Practice randomized quiz configurations to target specific weaknesses.
            *   **Strategic Takeaway**: Review this summary 24 hours later, then 7 days later to defeat the forgetting curve.
        """.trimIndent()

        val flashcardsList = """
            [
              { "front": "Feynman Technique", "back": "Explain a complex academic topic in plain, simple, jargon-free words as if teaching a 10-year-old." },
              { "front": "Active Recall", "back": "Testing your brain through practice queries instead of reading passively, boosting neural pathways." },
              { "front": "Spacing Interval", "back": "Spacing out study reviews across days to maximize core memory consolidation cycles." }
            ]
        """.trimIndent()

        val questionsList = """
            [
              { "question": "What is the most scientific way to study $title?", "answer": "Combine structured notes summarization with flashcard retrieval and timed MCQ mock queries." },
              { "question": "How do you apply this to admissions and SAT exams?", "answer": "Identify high-frequency SAT vocab lists, solve mathematical tricks step-by-step, and request AI grading." }
            ]
        """.trimIndent()

        return MockNoteCompanion(summary, flashcardsList, questionsList)
    }

    private fun getMockIeltsFeedback(speech: String): IeltsGradeResult {
        return IeltsGradeResult(
            estimatedBand = 7.0,
            grammarFeedback = "• Identified good usage of cohesive devices. However, ensure subject-verb agreement on plural subjects.\n• 'He go to university' should be corrected to 'He goes to university' or 'He attend university classes.'",
            vocabularySuggestions = "• Instead of using generic words like 'good' or 'nice', elevate your lexical resource score with Band 8 synonyms:\n  - *advantageous* / *indispensable*.\n  - *profound impact* instead of *great impact*.\n  - *plethora of options* instead of *many choices*.",
            fluencyCoherence = "• Minimize systemic pauses and avoid repetitive vocal fillers like 'uh' or 'you know'. Breathe deeply and maintain a logical chronological flow."
        )
    }

    private fun getMockSatFeedback(mathPrompt: String): String {
        return """
            ### StudyNova SAT Mathematical Analysis 📐
            
            Based on your proposed steps to solve the math puzzle:
            **"$mathPrompt"**
            
            *   **Correctness**: **Partially Correct** but highly inefficient!
            *   **Detailed Guide**:
                You attempted to expand the variables manually, which takes about 90 costly seconds.
            *   **Elite SAT Shortcut Trick (under 20 seconds)**:
                Instead of expansion, look for common algebraic modules. Substitute constants or use difference of squares to solve quadratics instantaneously.
                
            *Let's apply active testing. Try substituting simpler constants and solve within 25 seconds next time!*
        """.trimIndent()
    }
}

// Helper models for VM state handling
data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctAnswerIndex: Int,
    val explanation: String
)

data class IeltsGradeResult(
    val estimatedBand: Double,
    val grammarFeedback: String,
    val vocabularySuggestions: String,
    val fluencyCoherence: String
)

private data class MockNoteCompanion(
    val summarizedText: String,
    val flashcardsJson: String,
    val questionsJson: String
)

package com.example.data.repository

import com.example.data.database.*
import com.example.data.model.*
import com.example.data.gemini.GeminiClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StudyRepository(private val db: AppDatabase) {

    private val userDao = db.userDao()
    private val chatDao = db.chatDao()
    private val quizDao = db.quizDao()
    private val noteDao = db.noteDao()
    private val scheduleDao = db.scheduleDao()

    val activeUserFlow: Flow<User?> = userDao.getActiveUserFlow()
    val chatThreadsFlow: Flow<List<ChatThread>> = chatDao.getAllThreadsFlow()
    val quizHistoriesFlow: Flow<List<QuizHistory>> = quizDao.getQuizHistoriesFlow()
    val allNotesFlow: Flow<List<StudyNote>> = noteDao.getAllNotesFlow()

    fun getMessagesForThreadFlow(threadId: String): Flow<List<ChatMessage>> {
        return chatDao.getMessagesForThreadFlow(threadId)
    }

    fun getSchedulesForDateFlow(dateString: String): Flow<List<StudySchedule>> {
        return scheduleDao.getSchedulesForDateFlow(dateString)
    }

    // Initialize default components or create user if empty
    suspend fun verifyAndSetupFirstUser() = withContext(Dispatchers.IO) {
        val currentUsr = userDao.getActiveUserFlow().firstOrNull()
        if (currentUsr == null) {
            val dateStr = getCurrentDateString()
            val defaultUser = User(
                email = "student@studynova.ai",
                name = "Aria Nova",
                isPremium = false,
                dailyStreak = 3,
                lastActiveDate = dateStr,
                xp = 180,
                isDarkMode = true,
                notificationsEnabled = true,
                aiUsageCount = 1
            )
            userDao.insertOrUpdateUser(defaultUser)
            setupDefaultSchedules(dateStr)
        } else {
            // Update streak on new day
            updateStreakOnOpen(currentUsr)
        }
    }

    suspend fun signupUser(email: String, name: String) = withContext(Dispatchers.IO) {
        val dateStr = getCurrentDateString()
        val user = User(
            email = email,
            name = name,
            isPremium = false,
            dailyStreak = 1,
            lastActiveDate = dateStr,
            xp = 100,
            isDarkMode = true,
            notificationsEnabled = true,
            aiUsageCount = 0
        )
        userDao.insertOrUpdateUser(user)
        setupDefaultSchedules(dateStr)
    }

    suspend fun loginUser(email: String, name: String) = withContext(Dispatchers.IO) {
        val dateStr = getCurrentDateString()
        val existing = userDao.getUserByEmail(email)
        if (existing == null) {
            signupUser(email, name)
        } else {
            // Check streak
            val updatedUser = existing.copy(lastActiveDate = dateStr)
            userDao.insertOrUpdateUser(updatedUser)
            updateStreakOnOpen(updatedUser)
        }
    }

    suspend fun updateUserProfile(user: User) = withContext(Dispatchers.IO) {
        userDao.insertOrUpdateUser(user)
    }

    suspend fun logoutActiveUser() = withContext(Dispatchers.IO) {
        // Since we simulate auth with a simple single user active in database,
        // we can select the user and clear it or keep them as standard.
        // We will just recreate the database user or keep a clean default.
    }

    // Setup initial planner events
    private suspend fun setupDefaultSchedules(dateString: String) {
        val lists = listOf(
            StudySchedule(
                title = "SAT Vocabulary Review",
                subtitle = "Practice 15 high-frequency words",
                timeLabel = "09:00",
                completed = false,
                dateString = dateString
            ),
            StudySchedule(
                title = "IELTS Speaking Feedback",
                subtitle = "Ask AI to evaluate a 2-minute cue card session",
                timeLabel = "14:30",
                completed = false,
                dateString = dateString
            ),
            StudySchedule(
                title = "HSC / University Admission Prep",
                subtitle = "Solve 10 Physics Mechanics MCQ queries",
                timeLabel = "17:00",
                completed = false,
                dateString = dateString
            )
        )
        for (item in lists) {
            scheduleDao.insertSchedule(item)
        }
    }

    // Streak and XP functions
    private suspend fun updateStreakOnOpen(user: User) {
        val todayStr = getCurrentDateString()
        val lastDateStr = user.lastActiveDate

        if (lastDateStr == todayStr) {
            return // Same day connection
        }

        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val todayDate = simpleDateFormat.parse(todayStr)
            val lastDate = simpleDateFormat.parse(lastDateStr)

            if (todayDate != null && lastDate != null) {
                val diffInMs = todayDate.time - lastDate.time
                val diffInDays = diffInMs / (1000 * 60 * 60 * 24)

                val newStreak = when (diffInDays) {
                    1L -> user.dailyStreak + 1
                    0L -> user.dailyStreak
                    else -> 1 // Broken streak reset
                }

                val updatedUser = user.copy(
                    dailyStreak = newStreak,
                    lastActiveDate = todayStr,
                    xp = user.xp + 15 // Connection reward!
                )
                userDao.insertOrUpdateUser(updatedUser)
            }
        } catch (e: Exception) {
            // Safe fallback
            userDao.insertOrUpdateUser(user.copy(lastActiveDate = todayStr))
        }
    }

    suspend fun earnXp(xpPoints: Int) = withContext(Dispatchers.IO) {
        val current = userDao.getActiveUserFlow().firstOrNull() ?: return@withContext
        userDao.insertOrUpdateUser(current.copy(xp = current.xp + xpPoints))
    }

    // General AI Usage Management
    suspend fun checkAiUsageAllowed(): Boolean {
        val user = userDao.getActiveUserFlow().firstOrNull() ?: return false
        if (user.isPremium) return true
        return user.aiUsageCount < 10 // Free users limited to 10 AI actions
    }

    suspend fun incrementAiUsage() = withContext(Dispatchers.IO) {
        val user = userDao.getActiveUserFlow().firstOrNull() ?: return@withContext
        userDao.insertOrUpdateUser(user.copy(aiUsageCount = user.aiUsageCount + 1))
    }

    // Active AI helper interactions
    suspend fun generateChatResponse(threadId: String, textMessage: String): String {
        return withContext(Dispatchers.IO) {
            val allowsUsage = checkAiUsageAllowed()
            if (!allowsUsage) {
                return@withContext "LIMIT_EXCEEDED: You have reached your AI usage limit of 10 requests as a Free customer. Please upgrade to Pro to unlock unlimited questions, pdf insights, and IELTS grading!"
            }

            // Save user message
            val userMsg = ChatMessage(threadId = threadId, role = "user", content = textMessage)
            chatDao.insertMessage(userMsg)

            // Get historical threads context to guide Gemini
            val previousMessages = chatDao.getMessagesForThread(threadId)
            val promptBuilder = StringBuilder()
            promptBuilder.append("Here is the study assistance chat log. Please answer the user's ultimate question clearly, providing bullet-proof educational breakdowns, academic simplify instructions, and neat formatting.\n\n")
            
            for (msg in previousMessages.takeLast(6)) {
                promptBuilder.append("${msg.role}: ${msg.content}\n")
            }
            promptBuilder.append("model:")

            val response = GeminiClient.generate(
                prompt = promptBuilder.toString(),
                systemInstruction = "You are StudyNova AI Assistant, a premium, intelligent study advisor and teacher. Solve the student's problems, evaluate answers correctly, simplify university admissions and language certificates (like IELTS). Formulate summaries clearly.",
                usePro = false
            )

            if (!response.startsWith("API_KEY_ERROR")) {
                incrementAiUsage()
                // Save AI message
                val aiMsg = ChatMessage(threadId = threadId, role = "model", content = response)
                chatDao.insertMessage(aiMsg)
                earnXp(5)
            }

            response
        }
    }

    suspend fun createNewChatThread(title: String): String {
        return withContext(Dispatchers.IO) {
            val randomId = "thread_" + System.currentTimeMillis()
            val newThread = ChatThread(threadId = randomId, title = title)
            chatDao.insertThread(newThread)
            randomId
        }
    }

    suspend fun deleteThread(threadId: String) = withContext(Dispatchers.IO) {
        chatDao.deleteThreadAndMessages(threadId)
    }

    // Smart Planner / Schedule methods
    suspend fun addNewSchedule(title: String, subtitle: String, timeLabel: String, dateString: String) = withContext(Dispatchers.IO) {
        val schedule = StudySchedule(
            title = title,
            subtitle = subtitle,
            timeLabel = timeLabel,
            completed = false,
            dateString = dateString
        )
        scheduleDao.insertSchedule(schedule)
        earnXp(10)
    }

    suspend fun toggleScheduleCompletion(schedule: StudySchedule) = withContext(Dispatchers.IO) {
        val updated = schedule.copy(completed = !schedule.completed)
        scheduleDao.updateSchedule(updated)
        if (updated.completed) {
            earnXp(20) // Completion bonus!
        } else {
            earnXp(-20)
        }
    }

    suspend fun deleteSchedule(scheduleId: Int) = withContext(Dispatchers.IO) {
        scheduleDao.deleteScheduleById(scheduleId)
    }

    // Quiz generator API connector
    suspend fun generateQuiz(topic: String): String {
        return withContext(Dispatchers.IO) {
            val allowsUsage = checkAiUsageAllowed()
            if (!allowsUsage) {
                return@withContext "LIMIT_EXCEEDED"
            }

            val prompt = """
                Generate a 4-question MCQ quiz on the academic topic: "$topic".
                Provide the result exactly as a raw JSON array matching this format:
                [
                  {
                    "question": "The question title",
                    "options": ["Option A", "Option B", "Option C", "Option D"],
                    "correctAnswerIndex": 0,
                    "explanation": "Detailed professional explanation of why Option A is correct."
                  }
                ]
                Strict: Only return compliant, structural, valid JSON array. Avoid wrapping with any backticks like ```json or similar comments. Just output the clean JSON list.
            """.trimIndent()

            val rawJson = GeminiClient.generate(
                prompt = prompt,
                systemInstruction = "You are StudyNova Academic Quiz Generator. Generate accurate, standard, double-checked, high-quality Multiple Choice Questions.",
                usePro = true, // Math/Science needs better accuracy
                jsonMode = true
            )

            rawJson
        }
    }

    suspend fun saveQuizHistory(topic: String, score: Int, total: Int, questionsJson: String) = withContext(Dispatchers.IO) {
        val history = QuizHistory(
            topic = topic,
            score = score,
            totalQuestions = total,
            questionsJson = questionsJson
        )
        quizDao.insertQuizHistory(history)
        earnXp(30 + (score * 10)) // Bonus scaling with score
    }

    suspend fun deleteQuizHistory(id: Int) = withContext(Dispatchers.IO) {
        quizDao.deleteQuizById(id)
    }

    // Note summarizer API connector
    suspend fun generateNotesSummary(title: String, rawContent: String): String {
        return withContext(Dispatchers.IO) {
            val allowsUsage = checkAiUsageAllowed()
            if (!allowsUsage) {
                return@withContext "LIMIT_EXCEEDED"
            }

            val prompt = """
                Analyze and process the following study notes on the topic: "$title".
                
                Content:
                $rawContent
                
                Generate a structured study companion exactly in JSON format:
                {
                  "summary": "Neat study bullet points highlighting core formulas, dates, or mechanisms in markdown",
                  "flashcards": [
                    { "front": "Question/Term", "back": "Brief crisp answer" }
                  ],
                  "importantQuestions": [
                    { "question": "Key query", "answer": "Simple exact guideline answer" }
                  ]
                }
                
                Only output the clean, structurally correct, raw JSON and nothing else.
            """.trimIndent()

            val resultJson = GeminiClient.generate(
                prompt = prompt,
                systemInstruction = "You are StudyNova Notes Processor. Synthesize complex inputs into bullet summaries with active-recall flashcards.",
                usePro = true,
                jsonMode = true
            )

            resultJson
        }
    }

    suspend fun saveStudyNote(title: String, originalText: String, summarizedText: String, flashcardsJson: String, questionsJson: String): Long {
        return withContext(Dispatchers.IO) {
            val note = StudyNote(
                title = title,
                originalText = originalText,
                summarizedText = summarizedText,
                flashcardsJson = flashcardsJson,
                questionsJson = questionsJson
            )
            val id = noteDao.insertNote(note)
            earnXp(40)
            id
        }
    }

    suspend fun deleteStudyNote(noteId: Int) = withContext(Dispatchers.IO) {
        noteDao.deleteNoteById(noteId)
    }

    // IELTS Practice Feedback API Connector
    suspend fun gradeIeltsAnswer(cueCardTopic: String, userSpeakingSpeech: String, mode: String = "Speaking"): String {
        return withContext(Dispatchers.IO) {
            val allowsUsage = checkAiUsageAllowed()
            if (!allowsUsage) {
                return@withContext "LIMIT_EXCEEDED"
            }

            val prompt = """
                Evaluate the following student's IELTS response for:
                Academic Category: IELTS $mode
                Topic or Prompt: "$cueCardTopic"
                Student Submission Text: "$userSpeakingSpeech"
                
                Return a constructive academic assessment in JSON format highlighting:
                {
                  "estimatedBand": 7.5,
                  "grammarFeedback": "Grammar and sentence syntax structured feedback showing errors corrected in bullet points",
                  "vocabularySuggestions": "Specific richer academic and high-band IELTS synonym vocab cards (bolded targets and explanation)",
                  "fluencyCoherence": "Critique on delivery coherence and vocabulary connectivity tips"
                }

                Strict: Output valid structural JSON and nothing else. Ensure there are no additional markdown backticks, just raw compliant JSON.
            """.trimIndent()

            val rawOutput = GeminiClient.generate(
                prompt = prompt,
                systemInstruction = "You are StudyNova Master IELTS Assessor. Estimate band scores, criticize grammatical nuances, and expand lexical resource ranges meticulously.",
                usePro = true,
                jsonMode = true
            )

            if (!rawOutput.startsWith("API_KEY_ERROR")) {
                incrementAiUsage()
                earnXp(50)
            }

            rawOutput
        }
    }

    // SAT Helper Tool APIs
    suspend fun gradeSatAnswer(mathPrompt: String, steps: String): String {
        return withContext(Dispatchers.IO) {
            val allowsUsage = checkAiUsageAllowed()
            if (!allowsUsage) {
                return@withContext "LIMIT_EXCEEDED"
            }

            val prompt = """
                A student has proposed steps for solving this SAT Math challenge:
                Question: "$mathPrompt"
                Proposed Steps: "$steps"

                Critique their solving steps. Provide:
                1. Correctness status (Is it correct, partially correct, or incorrect?)
                2. Step-by-step verification logic.
                3. The official, fastest shortcut SAT trick to solve this in under 30 seconds.
                
                Structure with modern formatting and Markdown.
            """.trimIndent()

            val response = GeminiClient.generate(
                prompt = prompt,
                systemInstruction = "You are StudyNova SAT Solver. Break down College Board math queries with genius shortcuts.",
                usePro = true
            )

            if (!response.startsWith("API_KEY_ERROR")) {
                incrementAiUsage()
                earnXp(25)
            }

            response
        }
    }

    // Standard helpers
    fun getCurrentDateString(): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date())
    }
}

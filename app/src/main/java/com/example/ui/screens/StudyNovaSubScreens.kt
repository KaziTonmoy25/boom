package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

// -----------------------------------------------------------------------------------------
// AI CHAT SCREEN (CHATGPT COGNITIVE STYLE)
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: StudyViewModel,
    onNavigateBack: () -> Unit
) {
    val activeThreadMessages by viewModel.activeThreadMessages.collectAsState()
    val chatThreads by viewModel.chatThreads.collectAsState()
    val activeThreadId by viewModel.activeThreadId.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()
    val userState by viewModel.userState.collectAsState()

    var inputMessage by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    var showHistoryDialog by remember { mutableStateOf(false) }

    // Scroll to latest message on update
    LaunchedEffect(activeThreadMessages.size) {
        if (activeThreadMessages.isNotEmpty()) {
            listState.animateScrollToItem(activeThreadMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "StudyNova Cognitive AI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            if (activeThreadId == null) "Start fresh learning query" else "Active Dialogue Session",
                            fontSize = 11.sp,
                            color = PrimaryCyan
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                actions = {
                    // History icon
                    IconButton(onClick = { showHistoryDialog = true }) {
                        Icon(Icons.Default.Refresh, contentDescription = "All Chats", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDarkBg)
            )
        },
        containerColor = CosmicDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (activeThreadId == null) {
                // SUGGESTED PROMPTS ENTRY HUB
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = "AI Assistant",
                            tint = PrimaryPurple.copy(alpha = 0.5f),
                            modifier = Modifier.size(72.dp)
                        )
                        Text(
                            text = "Challenge your learning walls",
                            fontSize = 18.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select a suggested topic below or ask your custom textbook query.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        val suggestions = listOf(
                            "Explain Quantum Superposition simply 🌌",
                            "Give 5 high-band IELTS transition terms 📝",
                            "Show a premium SAT math factoring shortcut 📐",
                            "Summarize active study methods under 60 words ⚡"
                        )

                        suggestions.forEach { prompt ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CosmicSurface)
                                    .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        viewModel.startNewChatThread(prompt)
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = BrightGold,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        prompt,
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // CHAT CONVERSATION VIEW
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(activeThreadMessages) { msg ->
                        val isUser = msg.role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .background(
                                        if (isUser) PrimaryPurple else CosmicSurface,
                                        RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isUser) 16.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 16.dp
                                        )
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isUser) Color.Transparent else CosmicBorder,
                                        shape = RoundedCornerShape(
                                            topStart = 16.dp,
                                            topEnd = 16.dp,
                                            bottomStart = if (isUser) 16.dp else 4.dp,
                                            bottomEnd = if (isUser) 4.dp else 16.dp
                                        )
                                    )
                                    .padding(12.dp)
                            ) {
                                Column {
                                    Text(
                                        text = if (isUser) "You" else "StudyNova AI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = if (isUser) Color.White.copy(alpha = 0.7f) else PrimaryCyan
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = msg.content,
                                        color = TextPrimary,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }
                    }

                    if (isAiLoading) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .background(CosmicSurface, RoundedCornerShape(16.dp))
                                        .border(1.dp, CosmicBorder, RoundedCornerShape(16.dp))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            "Thinking...",
                                            fontSize = 12.sp,
                                            color = TextSecondary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(12.dp),
                                            color = PrimaryCyan,
                                            strokeWidth = 1.dp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // INPUT CONTROLS BAR
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CosmicSurface)
                    .border(1.dp, CosmicBorder)
                    .padding(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = inputMessage,
                        onValueChange = { inputMessage = it },
                        placeholder = { Text("Ask anything, Aria solves.", fontSize = 13.sp, color = TextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CosmicDarkBg,
                            unfocusedContainerColor = CosmicDarkBg,
                            focusedBorderColor = PrimaryPurple,
                            unfocusedBorderColor = CosmicBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                    )

                    IconButton(
                        onClick = {
                            if (inputMessage.isNotBlank()) {
                                val currentText = inputMessage
                                inputMessage = ""
                                if (activeThreadId == null) {
                                    viewModel.startNewChatThread(currentText)
                                } else {
                                    viewModel.sendMessageToActiveThread(currentText)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(46.dp)
                            .background(PrimaryPurple, CircleShape)
                    ) {
                        Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
                    }
                }
            }
        }
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = { Text("Conversational Logs", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                ) {
                    if (chatThreads.isEmpty()) {
                        Text("No active historical logs.", color = TextSecondary, fontSize = 14.sp)
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(chatThreads) { thread ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectChatThread(thread.threadId)
                                            showHistoryDialog = false
                                        }
                                        .background(CosmicSurfaceVariant, RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(0.8f)
                                    ) {
                                        Icon(Icons.Default.Chat, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            thread.title,
                                            color = TextPrimary,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteThread(thread.threadId) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.selectChatThread(null)
                            showHistoryDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Start Fresh Thread", color = TextPrimary)
                    }
                }
            },
            confirmButton = {},
            containerColor = CosmicSurface
        )
    }
}


// -----------------------------------------------------------------------------------------
// QUIZ GENERATOR SCREEN
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(
    viewModel: StudyViewModel,
    onNavigateBack: () -> Unit
) {
    val activeQuizQuestions by viewModel.activeQuizQuestions.collectAsState()
    val isGeneratingQuiz by viewModel.isGeneratingQuiz.collectAsState()
    val userState by viewModel.userState.collectAsState()

    var topicInput by remember { mutableStateOf("") }
    var currentQuestionIdx by remember { mutableStateOf(0) }
    var selectedOptionIdx by remember { mutableStateOf<Int?>(null) }
    var isSubmitted by remember { mutableStateOf(false) }
    var scoreCount by remember { mutableStateOf(0) }

    // MCQ timing simulation
    var timeLeftSeconds by remember { mutableStateOf(30) }
    LaunchedEffect(currentQuestionIdx, activeQuizQuestions.size) {
        if (activeQuizQuestions.isNotEmpty() && currentQuestionIdx < activeQuizQuestions.size) {
            timeLeftSeconds = 30
            while (timeLeftSeconds > 0 && !isSubmitted && activeQuizQuestions.isNotEmpty()) {
                delay(1000)
                timeLeftSeconds--
            }
            if (timeLeftSeconds == 0 && !isSubmitted && activeQuizQuestions.isNotEmpty()) {
                // Auto fail/skip
                isSubmitted = true
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StudyNova Quiz Lab", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDarkBg)
            )
        },
        containerColor = CosmicDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (activeQuizQuestions.isEmpty()) {
                // QUIZ TOPIC ENTRY HUB
                Spacer(modifier = Modifier.height(24.dp))
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Quiz Logo",
                    tint = PrimaryCyan,
                    modifier = Modifier.size(64.dp)
                )

                Text(
                    "AI Quiz Creator",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Text(
                    "Input your academic query topic (e.g. 'Photosynthesis mechanisms', 'Linear equations solver') and StudyNova creates standard MCQ modules.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = topicInput,
                    onValueChange = { topicInput = it },
                    label = { Text("Topic or chapter text label") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryCyan,
                        unfocusedBorderColor = CosmicBorder,
                        focusedLabelColor = PrimaryCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (isGeneratingQuiz) {
                    CircularProgressIndicator(color = PrimaryCyan)
                    Text("Studynova extracting study parameters...", color = TextSecondary, fontSize = 12.sp)
                } else {
                    Button(
                        onClick = {
                            if (topicInput.isNotBlank()) {
                                currentQuestionIdx = 0
                                selectedOptionIdx = null
                                isSubmitted = false
                                scoreCount = 0
                                viewModel.generateNewQuiz(topicInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Synthesize MCQ Module", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            } else if (currentQuestionIdx < activeQuizQuestions.size) {
                // ACTIVE MCQ QUIZ RUN
                val currentQ = activeQuizQuestions[currentQuestionIdx]

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Question ${currentQuestionIdx + 1} of ${activeQuizQuestions.size}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )

                    // Timer block representation
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = BrightGold, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${timeLeftSeconds}s Left",
                            fontSize = 12.sp,
                            color = if (timeLeftSeconds < 10) Color(0xFFFF5252) else PrimaryCyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                LinearProgressIndicator(
                    progress = { (currentQuestionIdx.toFloat() / activeQuizQuestions.size.toFloat()) },
                    color = PrimaryCyan,
                    trackColor = CosmicBorder,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp))
                )

                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = CosmicSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = currentQ.question,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(20.dp)
                    )
                }

                // OPTIONS LIST
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    currentQ.options.forEachIndexed { optIdx, op ->
                        val isSelected = selectedOptionIdx == optIdx
                        val cardBg = when {
                            isSubmitted && optIdx == currentQ.correctAnswerIndex -> Color(0xFF10B981).copy(alpha = 0.2f)
                            isSubmitted && isSelected && isSelected != (optIdx == currentQ.correctAnswerIndex) -> Color(0xFFFF5252).copy(alpha = 0.2f)
                            isSelected -> PrimaryCyan.copy(alpha = 0.15f)
                            else -> CosmicSurface
                        }
                        val cardBorder = when {
                            isSubmitted && optIdx == currentQ.correctAnswerIndex -> Color(0xFF10B981)
                            isSubmitted && isSelected && isSelected != (optIdx == currentQ.correctAnswerIndex) -> Color(0xFFFF5252)
                            isSelected -> PrimaryCyan
                            else -> CosmicBorder
                        }

                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    if (!isSubmitted) {
                                        selectedOptionIdx = optIdx
                                    }
                                }
                        ) {
                            Text(
                                text = op,
                                fontSize = 14.sp,
                                color = TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }

                if (isSubmitted) {
                    // SHOW EXPLANATION SLIDERS
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = CosmicSurfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.School, contentDescription = null, tint = BrightGold, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (selectedOptionIdx == currentQ.correctAnswerIndex) "Correct Solution! ⭐" else "Answer Analysis",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (selectedOptionIdx == currentQ.correctAnswerIndex) Color(0xFF10B981) else BrightGold
                                )
                            }
                            Text(
                                text = currentQ.explanation,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                lineHeight = 18.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            if (selectedOptionIdx == currentQ.correctAnswerIndex) {
                                scoreCount++
                            }
                            if (currentQuestionIdx + 1 == activeQuizQuestions.size) {
                                // COMPLETE, STORE HISTORY
                                viewModel.submitQuizScore(topicInput, scoreCount, activeQuizQuestions.size)
                                currentQuestionIdx++ // push beyond bounds to trigger finish screen
                            } else {
                                currentQuestionIdx++
                                selectedOptionIdx = null
                                isSubmitted = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text(
                            if (currentQuestionIdx + 1 == activeQuizQuestions.size) "Finalize Exam Module" else "Continue Exam Drill",
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Button(
                        onClick = {
                            if (selectedOptionIdx != null) {
                                isSubmitted = true
                            }
                        },
                        enabled = selectedOptionIdx != null,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryCyan,
                            disabledContainerColor = CosmicBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Confirm Answer", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // QUIZ SCORE WRAP SCREEN
                Spacer(modifier = Modifier.height(24.dp))
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = BrightGold,
                    modifier = Modifier.size(80.dp)
                )

                Text(
                    text = "Admission Drill Complete!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                val percentage = (scoreCount.toFloat() / activeQuizQuestions.size.toFloat()) * 100f
                Text(
                    text = "Score: $scoreCount / ${activeQuizQuestions.size} (${percentage.toInt()}%)",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (percentage >= 70f) Color(0xFF10B981) else TextAccent
                )

                Text(
                    text = "You earned +${30 + (scoreCount * 10)} StudyNova XP points! Excellent mental recovery.",
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        viewModel.clearActiveQuiz()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Return to Lab Lobby", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}


// -----------------------------------------------------------------------------------------
// NOTES SUMMARIZER SCREEN (active recall, flashcards, flips)
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesScreen(
    viewModel: StudyViewModel,
    onNavigateBack: () -> Unit
) {
    val studyNotes by viewModel.studyNotes.collectAsState()
    val isGeneratingNotes by viewModel.isGeneratingNotes.collectAsState()
    val generatedSummary by viewModel.generatedSummary.collectAsState()

    var docTitle by remember { mutableStateOf("") }
    var notesText by remember { mutableStateOf("") }
    var activeTabIdx by remember { mutableStateOf(0) } // 0: Summary, 1: Flashcards (#Recall), 2: VIP Queries

    var activeCardIdx by remember { mutableStateOf(0) }
    var isCardFlipped by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notes Summarizer & Flashcards", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDarkBg)
            )
        },
        containerColor = CosmicDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (generatedSummary == null && !isGeneratingNotes) {
                // ARCHIVE SELECT OR FRESH SUBMIT HUB
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        Text("Distill Chapter Texts", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("Paste your university admission texts, mechanical manuals, chemical equations, and let StudyNova generate bullet outlines and flashcard stacks.", fontSize = 12.sp, color = TextSecondary)
                    }

                    item {
                        OutlinedTextField(
                            value = docTitle,
                            onValueChange = { docTitle = it },
                            label = { Text("Outline Title (e.g. Mechanical Mechanics, Cell Cytosis)") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = CosmicBorder
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = notesText,
                            onValueChange = { notesText = it },
                            label = { Text("Paste chapter notes / transcription blocks") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = CosmicBorder
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                        )
                    }

                    item {
                        Button(
                            onClick = {
                                if (docTitle.isNotBlank() && notesText.isNotBlank()) {
                                    viewModel.generateStudyCompanion(docTitle, notesText)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text("Consolidate Knowledge", color = TextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Historical outlines list
                    if (studyNotes.isNotEmpty()) {
                        item {
                            Text("Your Saved Academic Portfolios", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
                        }

                        items(studyNotes) { note ->
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = CosmicSurface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp)
                                    .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                                    .clickable {
                                        // Trigger detail direct binding
                                        viewModel.addSchedule("Review Summary: ${note.title}", "Recall summarized bullet outlines", "StudyNova Portal")
                                        // Simulate loading generatedSummary bind
                                        viewModel.generateStudyCompanion(note.title, note.originalText)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(0.8f)) {
                                        Text(note.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Outlined on ${viewModel.repository.getCurrentDateString()}", color = TextSecondary, fontSize = 11.sp)
                                    }
                                    IconButton(onClick = { viewModel.deleteNote(note.noteId) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (isGeneratingNotes) {
                // PIPELINE LOADING BLOCK
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator(color = PrimaryPurple)
                        Text("StudyNova AI creating detailed semantic structures...", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            } else {
                // GENERATED VALUE TABS VIEW
                val outputCmd = generatedSummary!!

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val tabs = listOf("Outline Summary", "Recall Flashcards ⚡", "Key Q&A")
                    tabs.forEachIndexed { i, title ->
                        Button(
                            onClick = {
                                activeTabIdx = i
                                activeCardIdx = 0
                                isCardFlipped = false
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (activeTabIdx == i) PrimaryPurple else CosmicSurface
                            ),
                            border = BorderStroke(1.dp, if (activeTabIdx == i) PrimaryPurple else CosmicBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                title,
                                fontSize = 10.sp,
                                color = if (activeTabIdx == i) Color.White else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (activeTabIdx) {
                    0 -> {
                        // TAB 0: OUTLINE TEXT SUMMARY CARD
                        ElevatedCard(
                            colors = CardDefaults.elevatedCardColors(containerColor = CosmicSurface),
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(outputCmd.title, color = PrimaryCyan, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                HorizontalDivider(color = CosmicBorder)
                                Text(
                                    text = outputCmd.summarizedText,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                    1 -> {
                        // TAB 1: FLASHCARDS FLIPPING BOARD
                        val cards = remember(outputCmd.flashcardsJson) {
                            try {
                                val list = mutableListOf<JSONObject>()
                                val arr = JSONArray(outputCmd.flashcardsJson)
                                for (idx in 0 until arr.length()) {
                                    list.add(arr.getJSONObject(idx))
                                }
                                list
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }

                        if (cards.isEmpty()) {
                            Text("No cards parsed.", color = TextSecondary)
                        } else {
                            val activeCard = cards[activeCardIdx]
                            Column(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("Card ${activeCardIdx + 1} of ${cards.size}", fontSize = 12.sp, color = TextSecondary)

                                // Dynamic animated Card Flip box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(if (isCardFlipped) Color(0xFF1E152A) else CosmicSurface)
                                        .border(2.dp, if (isCardFlipped) PrimaryPurple else CosmicBorder, RoundedCornerShape(20.dp))
                                        .clickable { isCardFlipped = !isCardFlipped }
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = if (isCardFlipped) "ANSWER / RECALL" else "TERM / CHALLENGE",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isCardFlipped) TextAccent else PrimaryCyan,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = if (isCardFlipped) activeCard.optString("back") else activeCard.optString("front"),
                                            fontSize = 16.sp,
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            "Tap card to flip...",
                                            fontSize = 9.sp,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(top = 12.dp)
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Button(
                                        onClick = {
                                            if (activeCardIdx > 0) {
                                                activeCardIdx--
                                                isCardFlipped = false
                                            }
                                        },
                                        enabled = activeCardIdx > 0,
                                        colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Previous", color = TextPrimary)
                                    }

                                    Button(
                                        onClick = {
                                            if (activeCardIdx + 1 < cards.size) {
                                                activeCardIdx++
                                                isCardFlipped = false
                                            }
                                        },
                                        enabled = activeCardIdx + 1 < cards.size,
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Next Card", color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // TAB 2: VIP QA LIST
                        val questions = remember(outputCmd.questionsJson) {
                            try {
                                val list = mutableListOf<JSONObject>()
                                val arr = JSONArray(outputCmd.questionsJson)
                                for (idx in 0 until arr.length()) {
                                    list.add(arr.getJSONObject(idx))
                                }
                                list
                            } catch (e: Exception) {
                                emptyList()
                            }
                        }

                        if (questions.isEmpty()) {
                            Text("No questions parsed.", color = TextSecondary)
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(questions) { item ->
                                    val q = item.optString("question")
                                    val a = item.optString("answer")

                                    var showAnswer by remember { mutableStateOf(false) }

                                    ElevatedCard(
                                        colors = CardDefaults.elevatedCardColors(containerColor = CosmicSurface),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                                            .clickable { showAnswer = !showAnswer }
                                    ) {
                                        Column(modifier = Modifier.padding(14.dp)) {
                                            Text(
                                                text = "Q: $q",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = TextPrimary
                                            )
                                            if (showAnswer) {
                                                HorizontalDivider(color = CosmicBorder, modifier = Modifier.padding(vertical = 8.dp))
                                                Text(
                                                    text = "A: $a",
                                                    fontSize = 12.sp,
                                                    color = PrimaryCyan,
                                                    lineHeight = 16.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        viewModel.generateStudyCompanion("", "") // Clean buffer bypass
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                    border = BorderStroke(1.dp, CosmicBorder),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Exit Summary Lab", color = TextPrimary)
                }
            }
        }
    }
}


// -----------------------------------------------------------------------------------------
// IELTS LAB PRACTICE
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IeltsScreen(
    viewModel: StudyViewModel,
    onNavigateBack: () -> Unit
) {
    val activeIeltsResult by viewModel.activeIeltsResult.collectAsState()
    val isIeltsLoading by viewModel.isIeltsLoading.collectAsState()

    var activeCueCard by remember { mutableStateOf("Describe a crucial academic decision that significantly shaped your university admissions journey.") }
    var userSpeakingInput by remember { mutableStateOf("") }
    
    // Voice simulator pulse
    var isRecordingSimulated by remember { mutableStateOf(false) }
    var elapsedSeconds by remember { mutableStateOf(0) }

    LaunchedEffect(isRecordingSimulated) {
        if (isRecordingSimulated) {
            elapsedSeconds = 0
            while (isRecordingSimulated) {
                delay(1000)
                elapsedSeconds++
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IELTS Academic Lab", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDarkBg)
            )
        },
        containerColor = CosmicDarkBg
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text("Cue Card Speaking Simulator", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("Select your card topic below, simulate recording a 2 minute answer, or type your answer script. StudyNova provides analytical feedback.", fontSize = 12.sp, color = TextSecondary)
            }

            // Cue card topics carousel list
            item {
                val cueOptions = listOf(
                    "Describe a crucial academic decision that shaped your life admissions.",
                    "Describe a historical research book you found exceptionally profound.",
                    "Describe an intelligent mentor who simplified mathematics.",
                    "Describe a science experiment setup that went successfully."
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cueOptions) { topic ->
                        val isSelected = activeCueCard == topic
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryPurple else CosmicSurface)
                                .border(1.dp, if (isSelected) PrimaryPurple else CosmicBorder, RoundedCornerShape(12.dp))
                                .clickable {
                                    activeCueCard = topic
                                    elapsedSeconds = 0
                                    isRecordingSimulated = false
                                }
                                .padding(12.dp)
                        ) {
                            Text(
                                topic,
                                fontSize = 11.sp,
                                color = TextPrimary,
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Target cue card rendering
            item {
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = ComicSlateThemeCardBg),
                    modifier = Modifier.border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Active Cue Card Prompt:", color = BrightGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(activeCueCard, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        HorizontalDivider(color = CosmicBorder, modifier = Modifier.padding(vertical = 4.dp))
                        Text(
                            "Provide feedback criteria:\n• Fluency & Coherence\n• Lexical Resource Range\n• Grammatical Range Range\n• Pronunciation quality",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Active simulation pulsator
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            isRecordingSimulated = !isRecordingSimulated
                            if (!isRecordingSimulated) {
                                // Paste simulator trigger
                                userSpeakingInput = "Personally, regarding this academic decision, it was absolutely indispensable. I was considering attending university for computer engineering instead of biology. My mentor helped me consolidate my core goals, representing profound logic, and it had super benefits..."
                            }
                        },
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = if (isRecordingSimulated) Color(0xFFFF5252) else CosmicSurfaceVariant,
                                shape = CircleShape
                            )
                            .border(1.dp, CosmicBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isRecordingSimulated) Icons.Default.Add else Icons.Default.PlayArrow, // Standin for recording active
                            contentDescription = "Simulate Speak",
                            tint = if (isRecordingSimulated) Color.White else PrimaryCyan
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isRecordingSimulated) "Simulated Recording active" else "Practice voice delivery",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = if (isRecordingSimulated) "Secured: $elapsedSeconds seconds (IELTS recommends > 60s)" else "Click above to simulate active record",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // Script typed box
            item {
                OutlinedTextField(
                    value = userSpeakingInput,
                    onValueChange = { userSpeakingInput = it },
                    label = { Text("Answer Script (spoken text or typed response)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = BrightGold,
                        unfocusedBorderColor = CosmicBorder
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                )
            }

            // Evaluation actions button
            item {
                if (isIeltsLoading) {
                    Box(modifier = Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BrightGold)
                            Text("IELTS AI evaluating grammar structure & bands...", color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                } else {
                    Button(
                        onClick = {
                            if (userSpeakingInput.isNotBlank()) {
                                viewModel.evaluateIeltsPractice(activeCueCard, userSpeakingInput)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrightGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text("Extract Core IELTS Assessment", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Results render ledger
            activeIeltsResult?.let { res ->
                item {
                    ElevatedCard(
                        colors = CardDefaults.elevatedCardColors(containerColor = CosmicSurface),
                        modifier = Modifier.border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("IELTS EVALUATION INDEX", color = PrimaryCyan, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                Badge(
                                    containerColor = BrightGold,
                                    modifier = Modifier.padding(4.dp)
                                ) {
                                    Text("Estimated Band: ${res.estimatedBand}", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                                }
                            }

                            HorizontalDivider(color = CosmicBorder)

                            Text("Grammar Feedback Corrective Guides:", color = TextAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(res.grammarFeedback, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)

                            Text("Vocabulary & Lexical Upgrades suggestions:", color = TextAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(res.vocabularySuggestions, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)

                            Text("Fluency Coherence diagnostics:", color = TextAccent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text(res.fluencyCoherence, color = TextPrimary, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
            }
        }
    }
}


// -----------------------------------------------------------------------------------------
// SAT MODULE HELPERS
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SatScreen(
    viewModel: StudyViewModel,
    onNavigateBack: () -> Unit
) {
    val activeSatFeedback by viewModel.activeSatFeedback.collectAsState()
    val isSatLoading by viewModel.isSatLoading.collectAsState()

    var selectedSatTab by remember { mutableStateOf(0) } // 0: Math tricks, 1: Flash Vocab, 2: Comp Card

    // Typical SAT Vocab lists
    val vocabList = listOf(
        Pair("Ephemeral", "Lasting a very short, transient period of time."),
        Pair("Anachronistic", "Out of chronological order; belonging to another age."),
        Pair("Superfluous", "More than enough, surplus, unnecessary excess."),
        Pair("Capricious", "Subject to sudden changes of mood or behavior; fickle."),
        Pair("Pragmatic", "Dealing with matters logically and practically, not conceptually."),
        Pair("Assiduous", "Showing great care, attention, and persistent effort.")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SAT Admission Prep Office", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDarkBg)
            )
        },
        containerColor = CosmicDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tab control row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val tabs = listOf("Math Solver", "Vocab List", "Admissions Plan")
                tabs.forEachIndexed { idx, tabTitle ->
                    Button(
                        onClick = { selectedSatTab = idx },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedSatTab == idx) Color(0xFFFF5252) else CosmicSurface
                        ),
                        border = BorderStroke(1.dp, if (selectedSatTab == idx) Color(0xFFFF5252) else CosmicBorder),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(tabTitle, fontSize = 10.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            when (selectedSatTab) {
                0 -> {
                    // TAB 0: MATH CHALLENGES SOLVER
                    var mathPrompt by remember { mutableStateOf("If kx - 3y = 4 and 4x - y = 7 represent parallel equation models, check the target value of k.") }
                    var proposalTxt by remember { mutableStateOf("Parallel equations must share equivalent slopes, so k/3 must equal 4, giving k = 12.") }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Text("SAT AI Slope Solver", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("Formulate study steps below, and let Aria critique accuracy and reveal 15-second shortcut logic.", fontSize = 12.sp, color = TextSecondary)
                        }

                        item {
                            OutlinedTextField(
                                value = mathPrompt,
                                onValueChange = { mathPrompt = it },
                                label = { Text("College Board SAT Math concept") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = Color(0xFFFF5252)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = proposalTxt,
                                onValueChange = { proposalTxt = it },
                                label = { Text("Your proposed solving steps / solution status") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedBorderColor = Color(0xFFFF5252)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        item {
                            if (isSatLoading) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(color = Color(0xFFFF5252))
                                }
                            } else {
                                Button(
                                    onClick = {
                                        if (mathPrompt.isNotBlank() && proposalTxt.isNotBlank()) {
                                            viewModel.solveSatMathQuery(mathPrompt, proposalTxt)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Text("Verify solving process steps", color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        activeSatFeedback?.let { res ->
                            item {
                                ElevatedCard(
                                    colors = CardDefaults.elevatedCardColors(containerColor = CosmicSurface),
                                    modifier = Modifier.border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.School, contentDescription = null, tint = BrightGold, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("AI CRITIQUE RESULTS", fontWeight = FontWeight.Bold, color = PrimaryCyan, fontSize = 12.sp)
                                        }
                                        HorizontalDivider(color = CosmicBorder)
                                        Text(res, color = TextPrimary, fontSize = 13.sp, lineHeight = 19.sp)
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // TAB 1: SAT FLASH VOCAB CONTAINER
                    Text("High-Frequency SAT Vocabulary Grid", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(vocabList) { term ->
                            var showDef by remember { mutableStateOf(false) }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (showDef) Color(0xFF1E2638) else CosmicSurface)
                                    .border(1.dp, if (showDef) Color(0xFFFF5252) else CosmicBorder, RoundedCornerShape(14.dp))
                                    .clickable { showDef = !showDef }
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (showDef) "DEFINITION" else term.first.uppercase(),
                                        fontSize = if (showDef) 9.sp else 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (showDef) BrightGold else TextPrimary,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = if (showDef) term.second else "Tap card to flip definition",
                                        fontSize = if (showDef) 11.sp else 9.sp,
                                        color = if (showDef) TextPrimary else TextSecondary,
                                        lineHeight = 13.sp,
                                        textAlign = TextAlign.Center,
                                        maxLines = 4,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // TAB 2: ADMISSIONS COMPREHENSION PLANS
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            Text("Daily SAT Question of the Day", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }

                        item {
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(containerColor = CosmicSurface),
                                modifier = Modifier.border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("READING & REASONING INDEX", color = BrightGold, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        "The author emphasizes that 'active validation of hypothesis modules defeats empirical rote learning.' Based on current parameters, what is the best inference?",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "• Solution status: High performers apply critical reasoning checks instead of repeating textbook chapters.",
                                        fontSize = 11.sp,
                                        color = PrimaryCyan
                                    )
                                }
                            }
                        }

                        item {
                            Text("Admission badges and requirements", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                            Text("Review your checklists daily. Keep solving math puzzles to raise your StudyNova candidate levels.", fontSize = 11.sp, color = TextSecondary)
                        }
                    }
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}


// -----------------------------------------------------------------------------------------
// STUDY PLANNER SCREEN
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: StudyViewModel,
    onNavigateBack: () -> Unit
) {
    val selectedPlannerDate by viewModel.selectedPlannerDate.collectAsState()
    val plannerSchedules by viewModel.plannerSchedules.collectAsState()

    var showAddTaskDialog by remember { mutableStateOf(false) }

    var taskTitle by remember { mutableStateOf("") }
    var taskSubtitle by remember { mutableStateOf("") }
    var taskTime by remember { mutableStateOf("12:00") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Planner Hub", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDarkBg)
            )
        },
        containerColor = CosmicDarkBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddTaskDialog = true },
                containerColor = Color(0xFF10B981)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = Color.White)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Horizon date scroller representation
            Text("Select Study Target Date", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            
            val datesList = listOf("2026-06-01", "2026-06-02", "2026-06-03", "2026-06-04", "2026-06-05")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(datesList) { itemDate ->
                    val isSelected = selectedPlannerDate == itemDate
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFF10B981) else CosmicSurface)
                            .border(1.dp, if (isSelected) Color(0xFF10B981) else CosmicBorder, RoundedCornerShape(12.dp))
                            .clickable { viewModel.updateSelectedDate(itemDate) }
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Text(
                            itemDate,
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Text("Daily Goals Checklist", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

            if (plannerSchedules.isEmpty()) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Zero items", tint = Color.LightGray, modifier = Modifier.size(48.dp))
                        Text("All admission goals completed for this index!", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(plannerSchedules) { schedule ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CosmicSurface, RoundedCornerShape(12.dp))
                                .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(0.8f)
                            ) {
                                Checkbox(
                                    checked = schedule.completed,
                                    onCheckedChange = { viewModel.toggleSchedule(schedule) },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = Color(0xFF10B981),
                                        checkmarkColor = CosmicDarkBg
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = schedule.title,
                                        color = if (schedule.completed) TextSecondary else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${schedule.timeLabel} • ${schedule.subtitle}",
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            IconButton(onClick = { viewModel.removeSchedule(schedule.scheduleId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.LightGray)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddTaskDialog) {
        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("Assemble New Goal Block", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Goal Title (e.g. Physics Formulas, TOEFL Write)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = taskSubtitle,
                        onValueChange = { taskSubtitle = it },
                        label = { Text("Instructions Details") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = taskTime,
                        onValueChange = { taskTime = it },
                        label = { Text("Schedule Time (e.g. 15:30)") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            viewModel.addSchedule(taskTitle, taskSubtitle, taskTime)
                            taskTitle = ""
                            taskSubtitle = ""
                            showAddTaskDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                ) {
                    Text("Assemble Goal", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Dismiss", color = TextSecondary)
                }
            },
            containerColor = CosmicSurface
        )
    }
}


// -----------------------------------------------------------------------------------------
// PREMIUM SUBSCRIPTION PRICING SCREEN
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremiumScreen(
    viewModel: StudyViewModel,
    onNavigateBack: () -> Unit
) {
    val userState by viewModel.userState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("StudyNova Unlimited Pro", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = CosmicDarkBg)
            )
        },
        containerColor = CosmicDarkBg
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(
                        Brush.linearGradient(colors = listOf(PrimaryPurple, PrimaryCyan)),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
            }

            Text(
                "Supercharge study limits",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                "Unlock unlimited AI dialog sessions, multi-choice quiz solvers, detailed IELTS speech evaluations, and PDF analysis.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            // Current status banner
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (userState?.isPremium == true) Color(0xFF1E2F23) else CosmicSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = if (userState?.isPremium == true) Color(0xFF10B981) else CosmicBorder,
                        shape = RoundedCornerShape(12.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Personal Hub Status:", color = TextSecondary, fontSize = 11.sp)
                        Text(
                            text = if (userState?.isPremium == true) "PRO SUBSCRIPTION ACTIVE" else "FREE TIER PROFILE",
                            fontWeight = FontWeight.Bold,
                            color = if (userState?.isPremium == true) Color(0xFF10B981) else BrightGold,
                            fontSize = 14.sp
                        )
                    }
                    if (userState?.isPremium == false) {
                        Text("10 queries limit", color = TextSecondary, fontSize = 11.sp)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Active", tint = Color(0xFF10B981))
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Free card benefits list
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = CosmicSurface),
                    modifier = Modifier
                        .weight(1.5f)
                        .border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("FREE PACKAGE", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("FREE", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        HorizontalDivider(color = CosmicBorder)
                        Text("• Capped 10 AI queries\n• Standard SAT practice\n• Basic study summaries Outline", fontSize = 11.sp, color = TextSecondary, lineHeight = 16.sp)
                    }
                }

                // Premium subscription package
                ElevatedCard(
                    colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF17132B)),
                    modifier = Modifier
                        .weight(1.8f)
                        .border(2.dp, PrimaryPurple, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("NOVA UNLIMITED", color = PrimaryCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.background(PrimaryPurple, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Text("POPULAR", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Text("${"$"}4.99 / mo", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = TextPrimary)
                        HorizontalDivider(color = CosmicBorder)
                        Text("• Infinite StudyNova Dialogs\n• Live IELTS Speech evaluator\n• PDF/Outlines generation\n• Zero ads interruptions", fontSize = 11.sp, color = TextPrimary, lineHeight = 16.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (userState?.isPremium == false) {
                Button(
                    onClick = {
                        viewModel.updateUserSubscription(true)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Unlock Nova Unlimited Pro (Mock)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            } else {
                Button(
                    onClick = {
                        viewModel.updateUserSubscription(false)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CosmicSurfaceVariant),
                    border = BorderStroke(1.dp, CosmicBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Degrade to Free Tier (Simulated)", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
            }
        }
    }
}


// -----------------------------------------------------------------------------------------
// SETTINGS SCREEN
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: StudyViewModel,
    onLogoutPressed: () -> Unit
) {
    val userState by viewModel.userState.collectAsState()

    var editNameInput by remember { mutableStateOf(userState?.name ?: "Aria Nova") }
    var showEditProfileDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Settings & Core Parameters", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 16.dp))
        }

        // Profile brief
        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = CosmicSurface),
                modifier = Modifier.border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(userState?.name ?: "Aria Scholar", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(userState?.email ?: "aria@studynova.ai", color = TextSecondary, fontSize = 12.sp)
                    }
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = PrimaryCyan)
                    }
                }
            }
        }

        // TOGGLES BLOCK
        item {
            Text("Personal Theme & App Preferences", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.padding(top = 8.dp))
        }

        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = CosmicSurface),
                modifier = Modifier.border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
            ) {
                Column {
                    // Dark theme toggler
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Dark Theme UI Mode", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Maximize ocular stability under dark slates", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = userState?.isDarkMode == true,
                            onCheckedChange = { viewModel.toggleDarkMode(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple)
                        )
                    }

                    HorizontalDivider(color = CosmicBorder)

                    // Notification active
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Daily Practice Reminders", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Receive motivational triggers and active goals alerts", color = TextSecondary, fontSize = 10.sp)
                        }
                        Switch(
                            checked = userState?.notificationsEnabled == true,
                            onCheckedChange = { viewModel.toggleNotifications(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = PrimaryPurple)
                        )
                    }
                }
            }
        }

        // OTHER REQUISITE LINKS
        item {
            Text("Legal & Support Ledger", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }

        item {
            ElevatedCard(
                colors = CardDefaults.elevatedCardColors(containerColor = CosmicSurface),
                modifier = Modifier.border(1.dp, CosmicBorder, RoundedCornerShape(12.dp))
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {}.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Privacy Policy Agreement", fontSize = 13.sp, color = TextPrimary)
                        Icon(Icons.Default.Star, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                    HorizontalDivider(color = CosmicBorder)
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {}.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subscription Management Rules", fontSize = 13.sp, color = TextPrimary)
                        Icon(Icons.Default.Star, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.handleLogout()
                    onLogoutPressed()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5252)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Logout Scholar Session", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showEditProfileDialog) {
        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Student Credentials", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Provide your preferred full username designation:", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        label = { Text("Academic Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                val coroutineScope = rememberCoroutineScope()
                Button(
                    onClick = {
                        val currentUsr = userState
                        if (currentUsr != null && editNameInput.isNotBlank()) {
                            coroutineScope.launch {
                                viewModel.repository.updateUserProfile(currentUsr.copy(name = editNameInput))
                            }
                            showEditProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Modify Profile", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Dismiss", color = TextSecondary)
                }
            },
            containerColor = CosmicSurface
        )
    }
}

private val ComicSlateThemeCardBg = Color(0xFF1B1F2F)

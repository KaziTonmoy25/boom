package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// -----------------------------------------------------------------------------------------
// ONBOARDING & AUTHENTICATION SCREEN
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingAuthScreen(
    viewModel: StudyViewModel,
    onAuthSuccess: () -> Unit
) {
    val userState by viewModel.userState.collectAsState()
    var isSignUp by remember { mutableStateOf(false) }
    
    var nameInput by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var showForgotPassword by remember { mutableStateOf(false) }

    val gradientBg = Brush.verticalGradient(
        colors = listOf(
            CosmicDarkBg,
            Color(0xFF11072F),
            CosmicDarkBg
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBg)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            item {
                Spacer(modifier = Modifier.height(32.dp))
                // Logo & Header animation
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(PrimaryPurple, PrimaryCyan)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            item {
                Text(
                    text = "StudyNova AI",
                    style = TextStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Serif,
                        brush = Brush.horizontalGradient(
                            colors = listOf(PrimaryPurple, PrimaryCyan, BrightGold)
                        )
                    )
                )

                Text(
                    text = "Your elite Gen-Z academic advisor",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                ElevatedCard(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = CosmicSurface
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, CosmicBorder, RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = if (isSignUp) "Create Account" else "Welcome Back",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        if (isSignUp) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Your Academic Name") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryPurple,
                                    unfocusedBorderColor = CosmicBorder,
                                    focusedLabelColor = PrimaryPurple,
                                    unfocusedLabelColor = TextSecondary,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("University / Student Email") },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = CosmicBorder,
                                focusedLabelColor = PrimaryPurple,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Secure Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = PrimaryPurple,
                                unfocusedBorderColor = CosmicBorder,
                                focusedLabelColor = PrimaryPurple,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (!isSignUp) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                Text(
                                    text = "Forgot password?",
                                    fontSize = 12.sp,
                                    color = TextAccent,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        showForgotPassword = true
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (isSignUp) {
                                    if (nameInput.isNotBlank() && emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                                        viewModel.handleSignup(nameInput, emailInput)
                                        onAuthSuccess()
                                    }
                                } else {
                                    if (emailInput.isNotBlank() && passwordInput.isNotBlank()) {
                                        val display = emailInput.substringBefore("@").replaceFirstChar { it.uppercase() }
                                        viewModel.handleLogin(emailInput, display)
                                        onAuthSuccess()
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryPurple
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(
                                text = if (isSignUp) "Launch Account" else "Unlock Dashboard",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            HorizontalDivider(
                                color = CosmicBorder,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = " OR ",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            HorizontalDivider(
                                color = CosmicBorder,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        // Google Sign-In click
                        OutlinedButton(
                            onClick = {
                                viewModel.handleGoogleSignIn()
                                onAuthSuccess()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, CosmicBorder),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star, // Signifying glowing star
                                contentDescription = "Google Sign-In",
                                tint = BrightGold,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Sign in with Google",
                                color = TextPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isSignUp) "Already a Scholar? " else "New Academic? ",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isSignUp) "Login here" else "Register account",
                        color = PrimaryCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable {
                            isSignUp = !isSignUp
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                // Quick Bypass for evaluation
                TextButton(onClick = {
                    onAuthSuccess()
                }) {
                    Text(
                        text = "⚡ Bypass & Explore Immediately",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }

    if (showForgotPassword) {
        AlertDialog(
            onDismissRequest = { showForgotPassword = false },
            title = { Text("Recover Admission Keys", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter your student email and StudyNova AI will deliver recovery guides instantly.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Student Email") },
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
                    onClick = { showForgotPassword = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) {
                    Text("Send Recovery", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPassword = false }) {
                    Text("Dismiss", color = TextSecondary)
                }
            },
            containerColor = CosmicSurface
        )
    }
}


// -----------------------------------------------------------------------------------------
// DASHBOARD SCREEN
// -----------------------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: StudyViewModel,
    onNavigateToChat: () -> Unit,
    onNavigateToQuiz: () -> Unit,
    onNavigateToNotes: () -> Unit,
    onNavigateToIelts: () -> Unit,
    onNavigateToSat: () -> Unit,
    onNavigateToPlanner: () -> Unit,
    onNavigateToPremium: () -> Unit
) {
    val userState by viewModel.userState.collectAsState()
    val isDark = userState?.isDarkMode ?: true
    
    // Dynamic lists of quotes
    val quotes = listOf(
        "“The premium way of learning is active recall.”",
        "“No focus is lost when we construct schedules daily.”",
        "“IELTS success is built day by day with speaking mocks.”",
        "“HSC admissions aren't about luck, they're about systemized recall.”",
        "“Study smarter, get badges, beat your limits.”"
    )
    val randomQuote = remember { quotes.random() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome and Streak Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GOOD MORNING",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryPurple,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Text(
                        text = "Hi, ${userState?.name ?: "Scholar Alex"} 👋",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                // High-End Streak Fire Indicator Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (!isDark) Color(0xFFFFECE0) else Color(0x27FF5252),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .border(
                            1.dp,
                            if (!isDark) Color(0xFFFFC09F) else Color(0x60FF5252),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("🔥", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${userState?.dailyStreak ?: 12}",
                        color = if (!isDark) Color(0xFFC2410C) else Color(0xFFFF8F8F),
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // MOTIVATIONAL QUOTE CARD
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(Color(0xFF4F46E5), Color(0xFF7C3AED)) // Premium Indigo-violet gradient
                        )
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "DAILY INSIGHT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC7D2FE), // Light indigo tint
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = randomQuote,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Serif,
                        color = Color.White,
                        lineHeight = 21.sp
                    )
                }
            }
        }

        // AI ASSISTANT MINI PROMPT CARD (CTA Ask StudyNova AI)
        item {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = CosmicSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CosmicBorder, RoundedCornerShape(24.dp))
                    .clickable { onNavigateToChat() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (!isDark) Color(0xFFEEF2FF) else Color(0x1A818CF8),
                                    RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✨", fontSize = 22.sp)
                        }
                        Column {
                            Text(
                                text = "Ask StudyNova AI",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Explain any concept instantly",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (!isDark) Color(0xFFF1F5F9) else Color(0x19FFFFFF),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Ask AI",
                            tint = if (!isDark) Color(0xFF4F46E5) else Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // QUICK ACCESS NAVIGATION GRID
        item {
            Text(
                text = "Premium Tools Suite",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: Quiz Generator & Notes Summarizer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ToolCard(
                            title = "Quiz Generator",
                            subtitle = "Multi-choice AI tests with feedback loops",
                            icon = Icons.Default.School,
                            tint = Color(0xFF10B981),
                            isDark = isDark,
                            cardType = "quiz",
                            onClick = onNavigateToQuiz
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ToolCard(
                            title = "Summarizer",
                            subtitle = "Distill text or books into flashcards",
                            icon = Icons.Default.MenuBook,
                            tint = Color(0xFFF59E0B),
                            isDark = isDark,
                            cardType = "notes",
                            onClick = onNavigateToNotes
                        )
                    }
                }

                // Row 2: IELTS Practice & SAT Prep
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ToolCard(
                            title = "IELTS Lab",
                            subtitle = "Record voice responses, secure bands feedback",
                            icon = Icons.Default.VolumeUp,
                            tint = Color(0xFFF43F5E),
                            isDark = isDark,
                            cardType = "ielts",
                            onClick = onNavigateToIelts
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ToolCard(
                            title = "SAT Shortcuts",
                            subtitle = "Target Math tricks & Vocab flashcard grids",
                            icon = Icons.Default.Star,
                            tint = Color(0xFFFF5252),
                            isDark = isDark,
                            cardType = "sat",
                            onClick = onNavigateToSat
                        )
                    }
                }

                // Row 3: Study Planner & Premium Upgrades
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        ToolCard(
                            title = "Smart Planner",
                            subtitle = "Construct schedule blocks, win items XP",
                            icon = Icons.Default.DateRange,
                            tint = Color(0xFF3B82F6),
                            isDark = isDark,
                            cardType = "planner",
                            onClick = onNavigateToPlanner
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        ToolCard(
                            title = "Nova Unlimited",
                            subtitle = "Premium subscription, zero ad restrictions",
                            icon = Icons.Default.WorkspacePremium,
                            tint = Color(0xFF8B5CF6),
                            isDark = isDark,
                            cardType = "premium",
                            onClick = onNavigateToPremium
                        )
                    }
                }
            }
        }

        // PROGRESS WIDGET
        item {
            val user = userState
            val currentXp = user?.xp ?: 100
            val neededXp = (((currentXp / 100) + 1) * 100)
            val currentLevel = (currentXp / 100) + 1
            val currentLevelXp = currentXp % 100
            val progressFactor = currentLevelXp.toFloat() / 100f

            ElevatedCard(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = CosmicSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .border(1.dp, CosmicBorder, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Progress",
                                tint = PrimaryCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Academic Rank Progress",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            "$currentLevelXp / 100 XP",
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryPurple,
                            fontSize = 12.sp
                        )
                    }

                    LinearProgressIndicator(
                        progress = { progressFactor },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = PrimaryPurple,
                        trackColor = CosmicSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Level $currentLevel",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                        Text(
                            "Level ${currentLevel + 1} Badge",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ToolCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    isDark: Boolean,
    cardType: String,
    onClick: () -> Unit
) {
    val (backColor, borderColor, textColor) = remember(isDark, cardType) {
        if (!isDark) {
            when (cardType) {
                "quiz" -> Triple(Color(0x56E8F5E9), Color(0xFFD1FAE5), Color(0xFF065F46))
                "notes" -> Triple(Color(0x56FEF3C7), Color(0xFFFEF3C7), Color(0xFF92400E))
                "ielts" -> Triple(Color(0x56FFF1F2), Color(0xFFFFE4E6), Color(0xFF9F1239))
                "sat" -> Triple(Color(0x56FDF2E9), Color(0xFFFFE5D9), Color(0xFF9A3412))
                "planner" -> Triple(Color(0x56EFF6FF), Color(0xFFDBEAFE), Color(0xFF1E40AF))
                else -> Triple(Color(0x56FAF5FF), Color(0xFFF3E8FF), Color(0xFF6B21A8))
            }
        } else {
            when (cardType) {
                "quiz" -> Triple(Color(0x2410B981), Color(0x4210B981), Color(0xFFA7F3D0))
                "notes" -> Triple(Color(0x24F59E0B), Color(0x42F59E0B), Color(0xFFFDE68A))
                "ielts" -> Triple(Color(0x24F43F5E), Color(0x42F43F5E), Color(0xFFFECDD3))
                "sat" -> Triple(Color(0x24FF5252), Color(0x42FF5252), Color(0xFFFFCDCD))
                "planner" -> Triple(Color(0x243B82F6), Color(0x423B82F6), Color(0xFFBFDBFE))
                else -> Triple(Color(0x24C084FC), Color(0x42C084FC), Color(0xFFE9D5FF))
            }
        }
    }

    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = backColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(115.dp)
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (!isDark) textColor else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    fontSize = 10.sp,
                    color = if (!isDark) textColor.copy(alpha = 0.8f) else TextSecondary,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}


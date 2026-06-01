package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf

// StudyNova AI Curated Palette
val PrimaryPurple = Color(0xFF6366F1) // Beautiful Indigo
val PrimaryCyan = Color(0xFF0EA5E9)   // Sky Blue
val BrightGold = Color(0xFFF59E0B)    // Amber Gold

// Reactive Frosted Glass Slates
var CosmicDarkBg by mutableStateOf(Color(0xFF0F111A))
var CosmicSurface by mutableStateOf(Color(0xFF1E293B))
var CosmicSurfaceVariant by mutableStateOf(Color(0xFF334155))
var CosmicBorder by mutableStateOf(Color(0x33FFFFFF))

// Text slates
var TextPrimary by mutableStateOf(Color(0xFFF8FAFC))
var TextSecondary by mutableStateOf(Color(0xFF94A3B8))
var TextAccent by mutableStateOf(Color(0xFF818CF8))

// Soft gradients values
val GradientStart = Color(0xFF6366F1) // Indigo
val GradientMiddle = Color(0xFF8B5CF6) // Purple
val GradientEnd = Color(0xFFD946EF) // Magenta
val AccentGradientStart = Color(0xFF10B981) // Emerald
val AccentGradientEnd = Color(0xFF059669) // Green


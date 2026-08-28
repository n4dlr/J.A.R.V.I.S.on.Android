package com.example.jarvis.presentation.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.presentation.JarvisViewModel
import com.example.jarvis.presentation.components.ConfirmationDialog
import com.example.jarvis.presentation.components.ConversationTimeline
import com.example.jarvis.presentation.components.JarvisArcReactorOrb
import com.example.jarvis.presentation.components.QuickActionChips
import com.example.jarvis.presentation.components.TelemetryHudCard
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisDarkNavy
import com.example.ui.theme.JarvisDarkVoid
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisHeaderDark
import com.example.ui.theme.JarvisSurfaceGlass
import com.example.ui.theme.JarvisSurfaceGlassBorder
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisMainScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val listState = rememberLazyListState()

    // Permission Launcher for Microphone
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val recordAudioGranted = permissionsMap[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted) {
            viewModel.startVoiceListening()
        }
    }

    // Auto-scroll to latest conversation item
    LaunchedEffect(conversations.size) {
        if (conversations.isNotEmpty()) {
            listState.animateScrollToItem(conversations.size - 1)
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(JarvisDarkVoid)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        containerColor = JarvisDarkVoid
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Immersive Top Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                JarvisHeaderDark,
                                JarvisDarkVoid
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "NEURAL SYSTEM",
                            color = JarvisCyan.copy(alpha = 0.9f),
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 1.6.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "JARVIS",
                                color = JarvisTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Light,
                                letterSpacing = (-0.5).sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "CORE",
                                color = JarvisCyan,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (uiState.isSpeaking) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(JarvisCyanPrimary.copy(alpha = 0.15f))
                                    .border(1.dp, JarvisCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.GraphicEq,
                                        contentDescription = "TTS Danışır",
                                        tint = JarvisCyan,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "TTS",
                                        color = JarvisCyan,
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                        }

                        // Settings Icon Glass Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(JarvisSurfaceGlass)
                                .border(1.dp, JarvisSurfaceGlassBorder, RoundedCornerShape(10.dp))
                                .clickable { viewModel.toggleSettingsSheet(true) }
                                .testTag("settings_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Tənzimləmələr",
                                tint = JarvisTextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                // 2. HUD Telemetry Card
                TelemetryHudCard(
                    telemetry = uiState.telemetry,
                    providerType = uiState.activeProviderType,
                    providerHealth = uiState.providerHealth,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // 3. Central Arc Reactor Core Interactive Widget
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    JarvisArcReactorOrb(
                        isListening = uiState.isListening,
                        isSpeaking = uiState.isSpeaking,
                        isProcessing = uiState.isProcessing,
                        audioLevel = uiState.audioLevel,
                        size = 115.dp,
                        onClick = {
                            if (uiState.isListening) {
                                viewModel.stopVoiceListening()
                            } else {
                                if (viewModel.permissionManager.isPermissionGranted(com.example.jarvis.permissions.AppPermission.MICROPHONE)) {
                                    viewModel.startVoiceListening()
                                } else {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.RECORD_AUDIO,
                                            Manifest.permission.CAMERA
                                        )
                                    )
                                }
                            }
                        },
                        modifier = Modifier.testTag("arc_reactor_orb")
                    )
                }

                // Error Banner if present
                if (uiState.speechError != null) {
                    Text(
                        text = "⚠ ${uiState.speechError}",
                        color = JarvisCrimson,
                        fontSize = 10.5.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    )
                }

                // 4. Conversation Message Stream / Empty Greeting State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (conversations.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Azərbaycan dili aktivdir",
                                color = JarvisCyan.copy(alpha = 0.85f),
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Necə kömək edə bilərəm?",
                                color = JarvisTextPrimary,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Səsli əmr verin və ya aşağıdakı əmrlərə toxunun.",
                                color = JarvisTextMuted,
                                fontSize = 11.5.sp
                            )
                        }
                    } else {
                        ConversationTimeline(
                            messages = conversations,
                            listState = listState,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // 5. Quick Action Voice Shortcuts in Azerbaijani
                QuickActionChips(
                    onActionClick = { command ->
                        viewModel.processUserCommand(command)
                    },
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                // 6. Bottom Command Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Glass Input Pill Container
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(JarvisSurfaceGlass)
                            .border(
                                1.dp,
                                if (uiState.isListening) JarvisCyan else JarvisSurfaceGlassBorder,
                                RoundedCornerShape(24.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = uiState.currentInputText,
                            onValueChange = { viewModel.onInputTextChanged(it) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("command_input_field"),
                            singleLine = true,
                            textStyle = TextStyle(
                                color = JarvisTextPrimary,
                                fontSize = 13.5.sp,
                                fontFamily = FontFamily.Default
                            ),
                            cursorBrush = SolidColor(JarvisCyan),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                focusManager.clearFocus()
                                viewModel.submitTextCommand()
                            }),
                            decorationBox = { innerTextField ->
                                if (uiState.currentInputText.isEmpty()) {
                                    Text(
                                        text = if (uiState.isListening) "Sizi dinləyirəm..." else "Əmr daxil edin və ya mikrofona toxunun...",
                                        color = if (uiState.isListening) JarvisCyan else JarvisTextMuted,
                                        fontSize = 12.5.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Dynamic Mic / Send Glass Button
                    if (uiState.currentInputText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            JarvisCyanPrimary,
                                            JarvisCyanDark
                                        )
                                    )
                                )
                                .clickable {
                                    focusManager.clearFocus()
                                    viewModel.submitTextCommand()
                                }
                                .testTag("send_command_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Göndər",
                                tint = JarvisDarkVoid,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (uiState.isListening) JarvisCrimson.copy(alpha = 0.25f) else JarvisSurfaceGlass
                                )
                                .border(
                                    1.dp,
                                    if (uiState.isListening) JarvisCrimson else JarvisSurfaceGlassBorder,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    if (uiState.isListening) {
                                        viewModel.stopVoiceListening()
                                    } else {
                                        if (viewModel.permissionManager.isPermissionGranted(com.example.jarvis.permissions.AppPermission.MICROPHONE)) {
                                            viewModel.startVoiceListening()
                                        } else {
                                            permissionLauncher.launch(
                                                arrayOf(
                                                    Manifest.permission.RECORD_AUDIO,
                                                    Manifest.permission.CAMERA
                                                )
                                            )
                                        }
                                    }
                                }
                                .testTag("mic_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (uiState.isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                contentDescription = "Mikrofon",
                                tint = if (uiState.isListening) JarvisCrimson else JarvisCyan,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    // 7. Security Confirmation Dialog for HIGH/CRITICAL risk operations
    uiState.pendingConfirmation?.let { confirmation ->
        ConfirmationDialog(
            confirmation = confirmation,
            onConfirm = { viewModel.confirmPendingAction(confirmation) },
            onDismiss = { viewModel.dismissPendingConfirmation() }
        )
    }

    // 8. System & AI Settings Bottom Sheet
    if (uiState.showSettingsSheet) {
        JarvisSettingsSheet(
            activeProviderType = uiState.activeProviderType,
            providerHealth = uiState.providerHealth,
            permissions = uiState.permissionStatuses,
            isLowRamMode = uiState.isLowRamModeEnforced,
            onSelectProvider = { viewModel.setAIProvider(it) },
            onToggleLowRamMode = { viewModel.toggleLowRamMode(it) },
            onOpenSystemSettings = {
                context.startActivity(viewModel.permissionManager.getAppSettingsIntent())
            },
            onClearConversations = { viewModel.clearConversations() },
            isTtsEnabled = uiState.isTtsEnabled,
            hasGeminiApiKey = uiState.hasGeminiApiKey,
            onToggleTts = { viewModel.setTtsEnabled(it) },
            onGeminiApiKeyChanged = { viewModel.setGeminiApiKey(it) },
            onDismiss = { viewModel.toggleSettingsSheet(false) }
        )
    }
}


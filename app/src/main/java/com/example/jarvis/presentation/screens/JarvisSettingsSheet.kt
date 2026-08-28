package com.example.jarvis.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.domain.model.AIProviderType
import com.example.jarvis.domain.model.ProviderHealth
import com.example.jarvis.permissions.PermissionStatusInfo
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisBlue
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisDarkNavy
import com.example.ui.theme.JarvisDarkVoid
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSurfaceCard
import com.example.ui.theme.JarvisSurfaceCardBorder
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JarvisSettingsSheet(
    activeProviderType: AIProviderType,
    providerHealth: ProviderHealth?,
    permissions: List<PermissionStatusInfo>,
    isLowRamMode: Boolean,
    onSelectProvider: (AIProviderType) -> Unit,
    onToggleLowRamMode: (Boolean) -> Unit,
    onOpenSystemSettings: () -> Unit,
    onClearConversations: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = JarvisDarkNavy,
        contentColor = JarvisTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        tint = JarvisCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SİSTEM VƏ AI TƏNZİMLƏMƏLƏRİ",
                        color = JarvisCyan,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 1.sp
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Bağla",
                        tint = JarvisTextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. AI Provider Selection
            Text(
                text = "AI MODEL & PROVAYDER",
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurfaceCard)
                    .border(1.dp, JarvisSurfaceCardBorder, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                ProviderOptionItem(
                    title = "Lokal SLM (Offline)",
                    description = "100% Cihaz daxili, internet tələb etmir, batareya qənaətli.",
                    isSelected = activeProviderType == AIProviderType.LOCAL_SLM,
                    onClick = { onSelectProvider(AIProviderType.LOCAL_SLM) }
                )
                ProviderOptionItem(
                    title = "Gemini Cloud (Bulud)",
                    description = "Google Gemini 3.5 Flash ilə dərin NLU və geniş bilik.",
                    isSelected = activeProviderType == AIProviderType.GEMINI_CLOUD,
                    onClick = { onSelectProvider(AIProviderType.GEMINI_CLOUD) }
                )
                ProviderOptionItem(
                    title = "Hybrid Auto Fallback (Tövsiyə olunur)",
                    description = "Lokal SLM əsasdır, mürəkkəb suallarda avtomatik bulud dəstəyi.",
                    isSelected = activeProviderType == AIProviderType.FALLBACK_HYBRID,
                    onClick = { onSelectProvider(AIProviderType.FALLBACK_HYBRID) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. RAM & Performance Management
            Text(
                text = "RAM VƏ YADDAŞ OPTİMİZASİYASI",
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurfaceCard)
                    .border(1.dp, JarvisSurfaceCardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "4GB RAM Low-Memory Rejimi",
                            color = JarvisTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "RAM təzyiqini minimuma endirir, konteksti 4 mesajla məhdudlaşdırır.",
                            color = JarvisTextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isLowRamMode,
                        onCheckedChange = onToggleLowRamMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = JarvisCyan,
                            checkedTrackColor = JarvisDarkNavy,
                            uncheckedThumbColor = Color.Gray,
                            uncheckedTrackColor = JarvisDarkVoid
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Permission Manager Status
            Text(
                text = "TƏHLÜKƏSİZLİK VƏ İCAZƏLƏR",
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(JarvisSurfaceCard)
                    .border(1.dp, JarvisSurfaceCardBorder, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                permissions.forEach { perm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = perm.permission.titleAz,
                                color = JarvisTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = perm.permission.descriptionAz,
                                color = JarvisTextSecondary,
                                fontSize = 10.sp
                            )
                        }
                        Icon(
                            imageVector = if (perm.isGranted) Icons.Default.CheckCircle else Icons.Default.Lock,
                            contentDescription = null,
                            tint = if (perm.isGranted) JarvisGreen else JarvisAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onOpenSystemSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cihaz İcazələrini Aç", color = JarvisCyan, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4. Data & Cache
            Text(
                text = "YERLİ MƏLUMAT VƏ KEŞ",
                color = JarvisTextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onClearConversations,
                colors = ButtonDefaults.buttonColors(
                    containerColor = JarvisCrimson.copy(alpha = 0.2f),
                    contentColor = JarvisCrimson
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Danışıq Tarixçəsini Təmizlə", fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ProviderOptionItem(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) JarvisCyan.copy(alpha = 0.08f) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = JarvisCyan,
                unselectedColor = JarvisTextSecondary
            )
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (isSelected) JarvisCyan else JarvisTextPrimary,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = description,
                color = JarvisTextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

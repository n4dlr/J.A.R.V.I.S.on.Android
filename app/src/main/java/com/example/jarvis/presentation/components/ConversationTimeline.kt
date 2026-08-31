package com.example.jarvis.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.domain.model.ConversationMessage
import com.example.jarvis.domain.model.MessageSender
import com.example.jarvis.domain.model.ToolStatus
import com.example.ui.theme.JarvisAmber
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisCyanPrimary
import com.example.ui.theme.JarvisCrimson
import com.example.ui.theme.JarvisDarkNavy
import com.example.ui.theme.JarvisGreen
import com.example.ui.theme.JarvisSurfaceGlass
import com.example.ui.theme.JarvisSurfaceGlassBorder
import com.example.ui.theme.JarvisTextMuted
import com.example.ui.theme.JarvisTextPrimary
import com.example.ui.theme.JarvisTextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConversationTimeline(
    messages: List<ConversationMessage>,
    listState: LazyListState,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(messages, key = { it.id }) { msg ->
            ConversationMessageBubble(msg)
        }
    }
}

@Composable
fun ConversationMessageBubble(message: ConversationMessage) {
    val isUser = message.sender == MessageSender.USER
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(JarvisCyanPrimary.copy(alpha = 0.15f))
                    .border(1.dp, JarvisCyanPrimary.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = JarvisCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(if (isUser) JarvisCyanDark.copy(alpha = 0.22f) else JarvisSurfaceGlass)
                    .border(
                        1.dp,
                        if (isUser) JarvisCyan.copy(alpha = 0.35f) else JarvisSurfaceGlassBorder,
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column {
                    if (!isUser && message.intentId != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 6.dp)
                        ) {
                            Text(
                                text = "INTENT: ${message.intentId}",
                                color = JarvisCyan,
                                fontSize = 9.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            if (message.toolResultStatus != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                StatusBadge(status = message.toolResultStatus)
                            }
                        }
                    }

                    Text(
                        text = message.text,
                        color = JarvisTextPrimary,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Text(
                text = formattedTime,
                color = JarvisTextMuted,
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 3.dp, start = 4.dp, end = 4.dp)
            )
        }

        if (isUser) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = JarvisTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: ToolStatus) {
    val (color, icon, text) = when (status) {
        ToolStatus.SUCCESS -> Triple(JarvisGreen, Icons.Default.CheckCircle, "SUCCESS")
        ToolStatus.PARTIAL_SUCCESS -> Triple(JarvisCyan, Icons.Default.CheckCircle, "PARTIAL")
        ToolStatus.FAILED -> Triple(JarvisCrimson, Icons.Default.Error, "ERROR")
        ToolStatus.PERMISSION_REQUIRED -> Triple(JarvisAmber, Icons.Default.Lock, "AUTH")
        ToolStatus.ACCESSIBILITY_REQUIRED -> Triple(JarvisAmber, Icons.Default.Warning, "ACCESSIBILITY")
        ToolStatus.CONFIRMATION_REQUIRED -> Triple(JarvisAmber, Icons.Default.Warning, "CONFIRM")
        ToolStatus.VERIFICATION_FAILED -> Triple(JarvisAmber, Icons.Default.Warning, "UNVERIFIED")
        ToolStatus.TIMEOUT -> Triple(JarvisCrimson, Icons.Default.Error, "TIMEOUT")
        ToolStatus.CANCELLED -> Triple(Color.Gray, Icons.Default.Error, "CANCELLED")
        ToolStatus.DENIED -> Triple(JarvisCrimson, Icons.Default.Error, "DENIED")
        ToolStatus.UNSUPPORTED -> Triple(Color.Gray, Icons.Default.Error, "N/A")
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.15f))
            .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 5.dp, vertical = 1.5.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(9.dp)
        )
        Spacer(modifier = Modifier.width(3.dp))
        Text(
            text = text,
            color = color,
            fontSize = 8.5.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}


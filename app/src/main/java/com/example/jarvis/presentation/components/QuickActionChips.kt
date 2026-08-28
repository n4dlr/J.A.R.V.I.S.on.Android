package com.example.jarvis.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisSurfaceGlass
import com.example.ui.theme.JarvisSurfaceGlassBorder
import com.example.ui.theme.JarvisTextPrimary

data class QuickAction(
    val title: String,
    val command: String
)

val defaultQuickActions = listOf(
    QuickAction("🔋 Batareya", "Batareya nə qədərdir?"),
    QuickAction("⚡ Fənəri yandır", "Fənəri yandır"),
    QuickAction("💡 Fənəri söndür", "Fənəri söndür"),
    QuickAction("📊 RAM yoxla", "RAM nə qədərdir?"),
    QuickAction("💾 Yaddaş", "Nə qədər boş yer var?"),
    QuickAction("🔊 Səsi artır", "Səsi artır"),
    QuickAction("🔇 Səssiz et", "Səsi kəs"),
    QuickAction("📱 Ekranı kilidlə", "Ekranı kilidlə"),
    QuickAction("📷 Şəkil çək", "Şəkil çək"),
    QuickAction("⚙ Wi-Fi aç", "Wi-Fi tənzimləmələrini aç"),
    QuickAction("▶ YouTube aç", "YouTube aç")
)

@Composable
fun QuickActionChips(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    actions: List<QuickAction> = defaultQuickActions
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        actions.forEach { action ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(JarvisSurfaceGlass)
                    .border(1.dp, JarvisSurfaceGlassBorder, RoundedCornerShape(50))
                    .clickable { onActionClick(action.command) }
                    .padding(horizontal = 14.dp, vertical = 7.dp)
            ) {
                Text(
                    text = action.title,
                    color = JarvisTextPrimary.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}


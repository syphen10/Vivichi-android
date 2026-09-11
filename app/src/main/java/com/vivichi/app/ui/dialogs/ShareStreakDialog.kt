package com.vivichi.app.ui.dialogs

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vivichi.app.data.AppState
import com.vivichi.app.data.petEmoji
import com.vivichi.app.domain.GameLogic
import com.vivichi.app.ui.components.EmojiGlyph
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.STREAK_FRAMES
import com.vivichi.app.util.SoundFx
import com.vivichi.app.util.buildStreakBitmap
import com.vivichi.app.util.shareStreakBitmap

@Composable
fun ShareStreakDialog(state: AppState, onDismiss: () -> Unit) {
    var frame by remember { mutableStateOf(STREAK_FRAMES[0]) }
    val context = LocalContext.current
    val title = GameLogic.activeTitle(state)

    val bitmap: Bitmap = remember(frame, state.streak, state.pet.level, state.totalXP, state.totalDone, state.pet.outfit, title) {
        buildStreakBitmap(
            context = context,
            frame = frame,
            petName = state.pet.name,
            titleEmoji = title?.emoji,
            titleName = title?.name,
            streak = state.streak,
            level = state.pet.level,
            totalXP = state.totalXP,
            totalDone = state.totalDone,
            petEmoji = petEmoji(state.pet.species)
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White)
                .padding(20.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Share Streak", fontWeight = FontWeight.Black, fontSize = 16.sp)
                IconButton(onClick = { SoundFx.click(); onDismiss() }) { Icon(Icons.Filled.Close, null, tint = SoftText) }
            }
            Spacer(Modifier.height(10.dp))

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Streak card preview",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(800f / 500f)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                STREAK_FRAMES.forEach { f ->
                    val selected = f.id == frame.id
                    Column(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (selected) Color(0xFFFFF0F5) else BgPink)
                            .clickable { SoundFx.click(); frame = f }
                            .padding(vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        EmojiGlyph(raw = f.emoji, size = 18.dp)
                        Text(f.name, fontSize = 9.sp, fontWeight = FontWeight.Black, color = if (selected) PinkDark else SoftText, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            VivichiButton(
                text = "Share ✨",
                onClick = { shareStreakBitmap(context, bitmap, "${state.pet.name}_streak_${state.streak}.png") },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

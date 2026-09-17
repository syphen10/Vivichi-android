package com.vivichi.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vivichi.app.data.CemeteryEntry
import com.vivichi.app.data.petEmoji
import com.vivichi.app.data.petSpeciesName
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.components.*
import com.vivichi.app.util.SoundFx

// Moonlit palette: the one screen that deliberately leaves the pastel daylight of the rest of the app.
private val NightTop = Color(0xFF1B1836)
private val NightMid = Color(0xFF2B2452)
private val NightLow = Color(0xFF3A2F66)
private val HillBack = Color(0xFF30285A)
private val HillFront = Color(0xFF241E47)
private val StoneLight = Color(0xFFD9DCEA)
private val StoneDark = Color(0xFF9EA3BD)
private val Engrave = Color(0xFF4B4F68)
private val Moonlight = Color(0xFFFFF4D6)

/** Tombstone silhouette: straight sides with a round arched top. */
private val TombShape = RoundedCornerShape(topStartPercent = 50, topEndPercent = 50, bottomStartPercent = 6, bottomEndPercent = 6)

@Composable
fun CemeteryScreen(viewModel: VivichiViewModel) {
    val state by viewModel.state.collectAsState()
    var detail by remember { mutableStateOf<CemeteryEntry?>(null) }
    val entries = state.cemetery

    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(NightMid, NightLow, NightMid)))) {
        FloatingSparkles(Modifier.matchParentSize(), color = Moonlight.copy(alpha = 0.55f), count = 22, seed = 404)

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) { NightHeader(count = entries.size) }

            if (entries.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) { EmptyGraveyard() }
            } else {
                itemsIndexed(entries, key = { _, e -> e.id }) { index, entry ->
                    Tombstone(entry, Modifier.enterFromBelow(index + 1)) { SoundFx.click(); detail = entry }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "Every buddy here was loved. Keep your current one thriving 💗",
                        color = Color.White.copy(alpha = 0.55f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )
                }
            }
        }
    }

    detail?.let { entry -> MemorialDialog(entry) { detail = null } }
}

/** Night sky with a glowing moon, stars and two rolling hills. */
@Composable
private fun NightHeader(count: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp)
            .enterFromBelow(0)
            .shadow(14.dp, RoundedCornerShape(28.dp), clip = false, ambientColor = Color.Black, spotColor = Color.Black)
            .clip(RoundedCornerShape(28.dp))
            .background(Brush.verticalGradient(listOf(NightTop, NightMid, NightLow)))
            .height(190.dp)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val moon = Offset(size.width * 0.8f, size.height * 0.3f)
            val r = 26.dp.toPx()
            drawCircle(Brush.radialGradient(listOf(Moonlight.copy(alpha = 0.35f), Color.Transparent), center = moon, radius = r * 3.2f), r * 3.2f, moon)
            drawCircle(Moonlight, r, moon)
            drawCircle(Color(0xFFEFE0B8), r * 0.22f, Offset(moon.x - r * 0.3f, moon.y - r * 0.2f))
            drawCircle(Color(0xFFEFE0B8), r * 0.14f, Offset(moon.x + r * 0.35f, moon.y + r * 0.25f))
            drawCircle(Color(0xFFEFE0B8), r * 0.1f, Offset(moon.x + r * 0.05f, moon.y + r * 0.45f))
            hills(this)
            // two tiny silhouetted tombstones on the far hill
            listOf(0.18f to 0.66f, 0.27f to 0.69f).forEach { (fx, fy) ->
                val w = 12.dp.toPx(); val h = 16.dp.toPx()
                drawRoundRect(HillFront, Offset(size.width * fx, size.height * fy), Size(w, h), androidx.compose.ui.geometry.CornerRadius(w / 2, w / 2))
            }
        }
        FloatingSparkles(Modifier.matchParentSize(), color = Color.White, count = 14, seed = 7)
        Column(Modifier.align(Alignment.TopStart).padding(20.dp)) {
            Text("In Loving Memory", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Text(
                when (count) { 0 -> "No buddies have passed on"; 1 -> "1 buddy remembered"; else -> "$count buddies remembered" },
                color = Moonlight.copy(alpha = 0.8f), fontSize = 12.sp, fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun hills(scope: DrawScope) = with(scope) {
    val back = Path().apply {
        moveTo(0f, size.height * 0.72f)
        cubicTo(size.width * 0.25f, size.height * 0.55f, size.width * 0.5f, size.height * 0.8f, size.width, size.height * 0.62f)
        lineTo(size.width, size.height); lineTo(0f, size.height); close()
    }
    drawPath(back, HillBack)
    val front = Path().apply {
        moveTo(0f, size.height * 0.86f)
        cubicTo(size.width * 0.35f, size.height * 0.74f, size.width * 0.65f, size.height * 0.95f, size.width, size.height * 0.8f)
        lineTo(size.width, size.height); lineTo(0f, size.height); close()
    }
    drawPath(front, HillFront)
}

@Composable
private fun Tombstone(entry: CemeteryEntry, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth(0.92f)
                .shadow(10.dp, TombShape, clip = false, ambientColor = Color.Black, spotColor = Color.Black)
                .bounceClick(TombShape, pressedScale = 0.95f, onClick = onClick)
                .background(Brush.verticalGradient(listOf(StoneLight, StoneDark)))
        ) {
            // weathering speckles, stable per entry
            Canvas(Modifier.matchParentSize()) {
                val rnd = kotlin.random.Random(entry.id.hashCode())
                repeat(14) {
                    drawCircle(Color.White.copy(alpha = 0.18f), 1.6.dp.toPx(), Offset(size.width * rnd.nextFloat(), size.height * (0.25f + rnd.nextFloat() * 0.7f)))
                }
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .border(1.5.dp, Engrave.copy(alpha = 0.22f), TombShape)
                    .padding(top = 18.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("R.I.P", color = Engrave.copy(alpha = 0.55f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                Spacer(Modifier.height(6.dp))
                EmojiGlyph(raw = petEmoji(entry.species), size = 38.dp, modifier = Modifier.alpha(0.82f))
                Spacer(Modifier.height(6.dp))
                Text(entry.name, color = Engrave, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${petSpeciesName(entry.species)} · Lv ${entry.level}",
                    color = Engrave.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1
                )
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(36.dp).height(1.5.dp).background(Engrave.copy(alpha = 0.25f)))
                Spacer(Modifier.height(6.dp))
                Text(
                    "${entry.daysAlive} day${if (entry.daysAlive != 1) "s" else ""} together",
                    color = Engrave.copy(alpha = 0.8f), fontSize = 10.sp, fontWeight = FontWeight.Black
                )
            }
        }
        // grass mound with a little flower at the foot of the stone
        Box(Modifier.fillMaxWidth().height(18.dp)) {
            Canvas(Modifier.matchParentSize()) {
                drawOval(Color(0xFF3E6B4F), Offset(0f, size.height * 0.1f), Size(size.width, size.height * 1.6f))
                drawOval(Color(0xFF4F8A63), Offset(size.width * 0.08f, 0f), Size(size.width * 0.84f, size.height * 1.3f))
            }
            EmojiGlyph(raw = "🌸", size = 14.dp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 18.dp))
        }
    }
}

@Composable
private fun EmptyGraveyard() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .enterFromBelow(1)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.06f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
            .padding(vertical = 28.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.size(width = 150.dp, height = 70.dp), contentAlignment = Alignment.BottomCenter) {
            Canvas(Modifier.matchParentSize()) {
                drawOval(Color(0xFF3E6B4F), Offset(0f, size.height * 0.62f), Size(size.width, size.height * 0.8f))
            }
            EmojiGlyph(raw = "🌱", size = 40.dp, modifier = Modifier.padding(bottom = 16.dp).wiggling(6f, 1400).floating(3.dp, 2000))
        }
        Spacer(Modifier.height(12.dp))
        Text("Nobody rests here", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Text(
            "Keep completing habits and your buddy will never end up here.",
            color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun MemorialDialog(entry: CemeteryEntry, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(28.dp))
                .background(Brush.verticalGradient(listOf(NightTop, NightLow)))
                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(28.dp))
                .padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("IN LOVING MEMORY", color = Moonlight.copy(alpha = 0.7f), fontSize = 10.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(48.dp))
                    .background(Brush.radialGradient(listOf(Moonlight.copy(alpha = 0.35f), Color.Transparent))),
                contentAlignment = Alignment.Center
            ) { EmojiGlyph(raw = petEmoji(entry.species), size = 60.dp, modifier = Modifier.floating(4.dp, 2200)) }
            Text(entry.name, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 8.dp))
            Text(
                "${petSpeciesName(entry.species)} · ${entry.bornAt} – ${entry.diedAt}",
                color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MemorialStat("${entry.daysAlive}", "days", Modifier.weight(1f))
                MemorialStat("${entry.level}", "level", Modifier.weight(1f))
                MemorialStat("${entry.streak}", "best streak", Modifier.weight(1f))
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MemorialStat("${entry.totalDone}", "habits done", Modifier.weight(1f))
                MemorialStat("${entry.totalXP}", "total XP", Modifier.weight(1f))
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { SoundFx.click(); onDismiss() }) {
                Text("Rest well 🌸", color = Moonlight, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun MemorialStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        Text(label, color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

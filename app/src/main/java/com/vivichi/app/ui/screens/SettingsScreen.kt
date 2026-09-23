package com.vivichi.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.vivichi.app.notify.ReminderScheduler
import com.vivichi.app.ui.VivichiViewModel
import com.vivichi.app.ui.components.*
import com.vivichi.app.ui.dialogs.EditTimesDialog
import com.vivichi.app.ui.theme.*
import com.vivichi.app.util.SoundFx
import java.util.TimeZone

private const val NAYAPAY_ID = "mansoor.849@nayapay"
private const val NAYAPAY_IBAN = "PK36NAYA1234503160483899"

// 0 = collapsed (just the Donate button), 1 = asking which region, 2 = Pakistan (NayaPay ID), 3 = international (IBAN)
private const val DONATE_HIDDEN = 0
private const val DONATE_ASK_REGION = 1
private const val DONATE_LOCAL = 2
private const val DONATE_INTL = 3

@Composable
fun SettingsScreen(
    viewModel: VivichiViewModel,
    onOpenPremium: () -> Unit = {},
    onRestorePremium: () -> Unit = {},
    onAdPrivacy: (() -> Unit)? = null
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showEditTimes by remember { mutableStateOf(false) }
    var showResetConfirm by remember { mutableStateOf(false) }
    var adTapCount by remember { mutableIntStateOf(0) }
    var showAdStatus by remember { mutableStateOf(false) }
    var donateStep by remember { mutableIntStateOf(DONATE_HIDDEN) }

    val scheduler = remember { ReminderScheduler(context) }
    fun openSettings(intent: android.content.Intent) {
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(context, "Couldn't open that settings screen. Go to your phone's Settings > Apps > Vivichi manually.", Toast.LENGTH_LONG).show()
        }
    }
    var notifsOk by remember { mutableStateOf(scheduler.areNotificationsEnabled()) }
    var exactAlarmOk by remember { mutableStateOf(scheduler.canScheduleExact()) }
    var batteryOk by remember { mutableStateOf(scheduler.isIgnoringBatteryOptimizations()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notifsOk = scheduler.areNotificationsEnabled()
                exactAlarmOk = scheduler.canScheduleExact()
                batteryOk = scheduler.isIgnoringBatteryOptimizations()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 13.dp)) {
        item {
            PageHeader(
                title = "More",
                subtitle = "Settings, reminders & support",
                emoji = "⚙️",
                accent = listOf(Color(0xFFB8C4FF), PurpleDark),
                horizontalPadding = 3.dp
            )
        }

        item { SettingsLabel("Support Vivichi") }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(Brush.horizontalGradient(listOf(Pink, Purple)))
                    .then(if (donateStep == DONATE_HIDDEN) Modifier.clickable { SoundFx.click(); donateStep = DONATE_ASK_REGION } else Modifier)
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    EmojiGlyph(raw = "💗", size = 36.dp)
                    Text("Support Vivichi", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 6.dp))
                    if (donateStep == DONATE_HIDDEN) {
                        Text("Tap to get the donation details", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                        Box(
                            Modifier
                                .padding(top = 12.dp)
                                .clip(RoundedCornerShape(40.dp))
                                .background(Color.White.copy(alpha = 0.25f))
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                        ) { Text("Donate ✨", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black) }
                    }
                    AnimatedVisibility(visible = donateStep == DONATE_ASK_REGION) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Do you live in Pakistan?", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(40.dp))
                                        .background(Color.White)
                                        .clickable { SoundFx.click(); donateStep = DONATE_LOCAL }
                                        .padding(horizontal = 22.dp, vertical = 9.dp)
                                ) { Text("Yes, Pakistan", color = PinkDark, fontSize = 13.sp, fontWeight = FontWeight.Black) }
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(40.dp))
                                        .background(Color.White.copy(alpha = 0.25f))
                                        .clickable { SoundFx.click(); donateStep = DONATE_INTL }
                                        .padding(horizontal = 22.dp, vertical = 9.dp)
                                ) { Text("No, elsewhere", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black) }
                            }
                        }
                    }
                    AnimatedVisibility(visible = donateStep == DONATE_LOCAL) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Send via NayaPay to:", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
                            Box(
                                Modifier
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .clickable {
                                        SoundFx.click()
                                        clipboard.setText(AnnotatedString(NAYAPAY_ID))
                                        Toast.makeText(context, "NayaPay ID copied!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(NAYAPAY_ID, color = PinkDark, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                            }
                            Text("Tap to copy 💗", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    AnimatedVisibility(visible = donateStep == DONATE_INTL) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("International transfer — NayaPay IBAN:", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp), textAlign = TextAlign.Center)
                            Box(
                                Modifier
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .clickable {
                                        SoundFx.click()
                                        clipboard.setText(AnnotatedString(NAYAPAY_IBAN))
                                        Toast.makeText(context, "IBAN copied!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp)
                            ) {
                                Text(NAYAPAY_IBAN, color = PinkDark, fontSize = 13.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                            }
                            Text("Tap to copy 💗", color = Color.White.copy(alpha = 0.85f), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                }
            }
        }

        item { SettingsLabel("Schedule") }
        item {
            SettingsRow(title = "Edit habit times", subtitle = "Change when habits unlock and expire", onClick = { showEditTimes = true })
        }

        item { SettingsLabel("Habits") }
        items(state.habits, key = { it.id }) { h ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 7.dp)
                    .card(radius = 18.dp, elevation = 2.dp)
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                EmojiGlyph(raw = h.icon, size = 15.dp)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(h.name, fontSize = 13.sp, fontWeight = FontWeight.Black, maxLines = 1)
                    Text("+${h.xp} XP · ${com.vivichi.app.util.formatHabitTime(h.time, state.use24h)}${if (h.custom) " · Custom" else ""}", fontSize = 11.sp, color = SoftText, fontWeight = FontWeight.SemiBold)
                }
                if (h.custom) {
                    IconButton(onClick = { SoundFx.click(); viewModel.deleteHabit(h.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = SoftText)
                    }
                }
                Switch(
                    checked = h.enabled,
                    onCheckedChange = { SoundFx.click(); viewModel.toggleHabit(h.id, it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Pink)
                )
            }
        }

        item { SettingsLabel("Clock") }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .card(radius = 18.dp, elevation = 2.dp)
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("24-hour clock", fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text(
                        if (state.use24h) "Times show like 13:00" else "Times show like 1:00 PM",
                        fontSize = 10.sp, color = SoftText, fontWeight = FontWeight.SemiBold
                    )
                }
                Switch(
                    checked = state.use24h,
                    onCheckedChange = { SoundFx.click(); viewModel.setUse24h(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Pink)
                )
            }
        }

        item { SettingsLabel("Timezone") }
        item {
            SettingsRow(title = TimeZone.getDefault().id, subtitle = "All habit times use your local timezone automatically", trailing = "🌍")
        }

        item { SettingsLabel("Notifications") }
        if (!notifsOk) {
            item {
                ReliabilityWarning(
                    title = "Notifications are off for Vivichi",
                    body = "This is the phone-level notification permission, separate from the switch below — until it's on, no reminder can ever show, no matter what. Tap below, then tap \"Notifications\" on the app info screen that opens.",
                    actionLabel = "Open app settings",
                    onClick = {
                        SoundFx.click()
                        openSettings(scheduler.notificationSettingsIntent())
                    }
                )
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = if (!notifsOk) 8.dp else 0.dp)
                    .card(radius = 18.dp, elevation = 2.dp)
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Reminders", fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text("Habit alerts and streak warnings", fontSize = 10.sp, color = SoftText, fontWeight = FontWeight.SemiBold)
                }
                Switch(
                    checked = state.notif,
                    onCheckedChange = { on ->
                        SoundFx.click()
                        viewModel.setNotif(on)
                        if (on && !notifsOk) openSettings(scheduler.notificationSettingsIntent())
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = Pink)
                )
            }
        }
        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 7.dp)
                    .card(radius = 18.dp, elevation = 2.dp)
                    .padding(13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Pet status panel", fontSize = 13.sp, fontWeight = FontWeight.Black)
                    Text("Live panel in your notification shade with health and next habit", fontSize = 10.sp, color = SoftText, fontWeight = FontWeight.SemiBold)
                }
                Switch(
                    checked = state.statusPanel,
                    onCheckedChange = { SoundFx.click(); viewModel.setStatusPanel(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Pink)
                )
            }
        }
        item {
            SettingsRow(
                title = "Send a test notification",
                subtitle = "Fires immediately — the fastest way to check notifications actually work on this phone",
                onClick = {
                    scheduler.sendTestNotification()
                    Toast.makeText(context, "Sent — check your notification shade now", Toast.LENGTH_SHORT).show()
                }
            )
        }
        // Deliberately a separate section from "Notifications" above: these two are about alarm
        // *scheduling* reliability (timing/background execution), not the notification permission
        // itself — keeping them apart avoids the confusing impression that battery settings are
        // somehow part of turning notifications on.
        if (state.notif && (!exactAlarmOk || !batteryOk)) {
            item { SettingsLabel("Reminder Reliability") }
        }
        if (state.notif && !exactAlarmOk) {
            item {
                ReliabilityWarning(
                    title = "Reminders may arrive late",
                    body = "Android needs permission to schedule exact alarms, or your habit reminders can be delayed by the system.",
                    actionLabel = "Allow exact alarms",
                    onClick = {
                        SoundFx.click()
                        scheduler.exactAlarmSettingsIntent()?.let { openSettings(it) }
                    }
                )
            }
        }
        if (state.notif && !batteryOk) {
            item {
                ReliabilityWarning(
                    title = "Battery optimization may block reminders",
                    body = "This is unrelated to the notification permission above — it's a separate Android setting that can kill background apps entirely. Exempt Vivichi from it so reminders keep firing on time.",
                    actionLabel = "Fix battery settings",
                    onClick = {
                        SoundFx.click()
                        openSettings(scheduler.batteryOptimizationIntent())
                    }
                )
            }
        }

        item {
            // Seven taps on this heading opens the ad diagnostics sheet. Hidden on purpose:
            // it's for working out why ads aren't showing, not something users need to see.
            Box(Modifier.clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { if (++adTapCount >= 7) { adTapCount = 0; showAdStatus = true } }) {
                SettingsLabel("Premium & Ads")
            }
        }
        item {
            SettingsRow(
                title = if (state.premium) "Vivichi Premium" else "Go Premium",
                subtitle = if (state.premium) "Active — thank you for supporting Vivichi!" else "All buddies, exclusive themes, daily coins, no ads",
                trailing = if (state.premium) "💎" else null,
                onClick = onOpenPremium
            )
        }
        item {
            SettingsRow(title = "Restore purchase", subtitle = "Bought Premium before? Get it back on this device", onClick = onRestorePremium)
        }
        if (onAdPrivacy != null) {
            item {
                SettingsRow(title = "Ad privacy choices", subtitle = "Review or change your ad consent", onClick = onAdPrivacy)
            }
        }

        item { SettingsLabel("Danger Zone") }
        item {
            SettingsRow(title = "Reset all data", titleColor = PinkDark, subtitle = "Cannot be undone", onClick = { showResetConfirm = true })
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showEditTimes) {
        EditTimesDialog(
            habits = state.habits,
            onDismiss = { showEditTimes = false },
            onSave = { times -> viewModel.saveHabitTimes(times); showEditTimes = false }
        )
    }
    if (showAdStatus) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val report = com.vivichi.app.monetize.AdDiagnostics.summary()
        AlertDialog(
            onDismissRequest = { showAdStatus = false },
            title = { Text("Ad status", fontWeight = FontWeight.Black) },
            text = {
                Text(
                    report,
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    modifier = Modifier.verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                    cm?.setPrimaryClip(android.content.ClipData.newPlainText("Vivichi ad status", report))
                    android.widget.Toast.makeText(context, "Copied", android.widget.Toast.LENGTH_SHORT).show()
                }) { Text("Copy", color = PinkDark, fontWeight = FontWeight.Black) }
            },
            dismissButton = {
                TextButton(onClick = { showAdStatus = false }) { Text("Close", color = SoftText, fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (showResetConfirm) {
        AlertDialog(
            onDismissRequest = { showResetConfirm = false },
            title = { Text("Reset ALL Vivichi data?", fontWeight = FontWeight.Black) },
            text = { Text("This cannot be undone. Your pet, streak, habits, and cemetery will all be erased.", fontSize = 13.sp) },
            confirmButton = {
                TextButton(onClick = { SoundFx.click(); viewModel.resetAll(); showResetConfirm = false }) {
                    Text("Reset", color = PinkDark, fontWeight = FontWeight.Black)
                }
            },
            dismissButton = { TextButton(onClick = { SoundFx.click(); showResetConfirm = false }) { Text("Cancel", color = SoftText, fontWeight = FontWeight.Bold) } }
        )
    }
}

@Composable
private fun ReliabilityWarning(title: String, body: String, actionLabel: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFFFFF3CD))
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            EmojiGlyph(raw = "⚠️", size = 13.dp)
            Spacer(Modifier.width(5.dp))
            Text(title, fontSize = 12.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B6914))
        }
        Text(body, fontSize = 11.sp, color = Color(0xFF8B6914), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 3.dp, bottom = 10.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(40.dp))
                .background(Color(0xFF8B6914))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) { Text(actionLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black) }
    }
}

@Composable
private fun SettingsLabel(text: String) {
    SectionTitle(title = text, horizontalPadding = 3.dp)
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    titleColor: Color = TextDark,
    trailing: String? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(bottom = 9.dp)
            .cardShadow(18.dp, 2.dp)
            .clip(RoundedCornerShape(18.dp))
            .cardSurface(18.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = { SoundFx.click(); onClick() }) else Modifier)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(title, fontSize = 13.sp, fontWeight = FontWeight.Black, color = titleColor)
            Text(subtitle, fontSize = 10.sp, color = SoftText, fontWeight = FontWeight.SemiBold)
        }
        if (trailing != null) EmojiGlyph(raw = trailing, size = 18.dp) else Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = SoftText)
    }
}

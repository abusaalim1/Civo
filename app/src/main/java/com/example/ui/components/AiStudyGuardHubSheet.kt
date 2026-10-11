package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.R
import com.example.ui.theme.BalooBrandFontFamily
import com.example.ui.theme.NearBlack
import com.example.ui.theme.RegainLimeDeepText
import com.example.ui.theme.RegainLimePrimary
import com.example.ui.theme.SecondaryTextLight
import com.example.ui.theme.isAppInDarkTheme
import com.example.util.AiStudyGuardManager
import com.example.util.ShortsLockManager
import com.example.util.StrictModeManager
import com.example.util.YouTubeStudyGuardManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dedicated AI Study Guard section: mascot-guarded hub for everything the guard does —
 * master switch, accessibility permission status, YouTube AI filter, Shorts/Reels locks,
 * strict mode, punishment history and a "how it works" explainer. Reuses the existing
 * managers; no parallel state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiStudyGuardHubSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lifecycleOwner = LocalLifecycleOwner.current

    var isAiGuardEnabled by remember { mutableStateOf(AiStudyGuardManager.isAiGuardEnabled(context)) }
    var hasAccessibility by remember { mutableStateOf(AiStudyGuardManager.isAccessibilityPermissionGranted(context)) }
    var isStudyActiveNow by remember { mutableStateOf(AiStudyGuardManager.isStudyPeriodActive(context)) }
    var showOptInDialog by remember { mutableStateOf(false) }

    val isShortsLockEnabled by ShortsLockManager.isShortsLockState.collectAsState()
    val isReelsLockEnabled by ShortsLockManager.isReelsLockState.collectAsState()
    val isYouTubeFilterEnabled by YouTubeStudyGuardManager.isFilterEnabledState.collectAsState()
    val blockedVideoCount by YouTubeStudyGuardManager.blockedVideosCountState.collectAsState()
    val isStrictModeEnabled by StrictModeManager.isStrictModeState.collectAsState()
    val punishmentLogs by AiStudyGuardManager.punishmentLogs.collectAsState()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibility = AiStudyGuardManager.isAccessibilityPermissionGranted(context)
                isAiGuardEnabled = AiStudyGuardManager.isAiGuardEnabled(context)
                isStudyActiveNow = AiStudyGuardManager.isStudyPeriodActive(context)
                ShortsLockManager.init(context)
                YouTubeStudyGuardManager.init(context)
                AiStudyGuardManager.loadLocalLogs(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isDark = isAppInDarkTheme()
    val sheetBackground = if (isDark) Color(0xFF0F1410) else Color(0xFFFAFBF7)
    val textPrimary = if (isDark) Color(0xFFF0F4ED) else NearBlack
    val textSecondary = if (isDark) Color(0xFFA0A89E) else SecondaryTextLight
    val accentLime = if (isDark) RegainLimePrimary else RegainLimeDeepText

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = sheetBackground,
        dragHandle = null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "AI Study Guard",
                    fontFamily = BalooBrandFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = textPrimary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = textSecondary)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Guard mascot header in a glass card with shield badge
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = if (isDark) 0.10f else 0.55f),
                                        accentLime.copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                            .padding(18.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(contentAlignment = Alignment.BottomEnd) {
                                Image(
                                    painter = painterResource(id = R.drawable.mascot_guard),
                                    contentDescription = "AI Study Guard mascot",
                                    modifier = Modifier
                                        .size(88.dp)
                                        .clip(RoundedCornerShape(20.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(accentLime)
                                        .border(2.dp, Color.White, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Study Sentinel",
                                    fontFamily = BalooBrandFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 19.sp,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Watches your AI chats & YouTube during study sessions. Non-study content gets warned first — ignored warnings lock the app for 3 hours.",
                                    fontSize = 12.5.sp,
                                    lineHeight = 17.sp,
                                    color = textSecondary
                                )
                            }
                        }
                    }
                }

                // Live guard status — glanceable diagnosis of why the guard may not fire
                item {
                    GuardSectionTitle("Guard Status", textPrimary)
                    GuardCard {
                        GuardStatusRow(
                            label = "Accessibility service",
                            stateText = if (hasAccessibility) "Connected" else "OFF",
                            ok = hasAccessibility,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentLime = accentLime
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GuardStatusRow(
                            label = "Study period",
                            stateText = if (isStudyActiveNow) "Active now" else "Inactive",
                            ok = isStudyActiveNow,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentLime = accentLime
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GuardStatusRow(
                            label = "YouTube AI filter",
                            stateText = if (isYouTubeFilterEnabled) "ON" else "OFF",
                            ok = isYouTubeFilterEnabled,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentLime = accentLime
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GuardStatusRow(
                            label = "AI Guard master switch",
                            stateText = if (isAiGuardEnabled) "ON" else "OFF",
                            ok = isAiGuardEnabled,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentLime = accentLime
                        )
                        if (isAiGuardEnabled && !hasAccessibility) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Accessibility OFF hai — Guard kaam nahi karega",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE05252)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Open settings",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentLime,
                                modifier = Modifier.clickable {
                                    AiStudyGuardManager.openAccessibilitySettings(context)
                                }
                            )
                        }
                    }
                }

                // Master switch + accessibility permission
                item {
                    GuardCard {
                        GuardSwitchRow(
                            title = "AI Study Guard",
                            description = if (hasAccessibility) "Active during study sessions" else "Needs accessibility permission",
                            checked = isAiGuardEnabled,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentLime = accentLime,
                            onCheckedChange = { enabled ->
                                if (enabled && !hasAccessibility) {
                                    showOptInDialog = true
                                } else {
                                    AiStudyGuardManager.setAiGuardEnabled(context, enabled)
                                    isAiGuardEnabled = enabled
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(if (hasAccessibility) accentLime else Color(0xFFFF8A65))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (hasAccessibility) "Accessibility permission granted" else "Accessibility permission required",
                                fontSize = 12.sp,
                                color = textSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "Open settings",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = accentLime,
                                modifier = Modifier.clickable {
                                    AiStudyGuardManager.openAccessibilitySettings(context)
                                }
                            )
                        }
                    }
                }

                // Content filters
                item {
                    GuardSectionTitle("Content Filters", textPrimary)
                    GuardCard {
                        GuardSwitchRow(
                            title = "YouTube AI Filter",
                            description = "AI checks video titles during study ($blockedVideoCount blocked so far)",
                            checked = isYouTubeFilterEnabled,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentLime = accentLime,
                            onCheckedChange = { YouTubeStudyGuardManager.setFilterEnabled(context, it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GuardSwitchRow(
                            title = "YouTube Shorts Lock",
                            description = "Blocks the Shorts player during study",
                            checked = isShortsLockEnabled,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentLime = accentLime,
                            onCheckedChange = { ShortsLockManager.setShortsLockEnabled(context, it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        GuardSwitchRow(
                            title = "Instagram Reels Lock",
                            description = "Blocks the Reels viewer during study",
                            checked = isReelsLockEnabled,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentLime = accentLime,
                            onCheckedChange = { ShortsLockManager.setReelsLockEnabled(context, it) }
                        )
                    }
                }

                // Discipline
                item {
                    GuardSectionTitle("Discipline", textPrimary)
                    GuardCard {
                        GuardSwitchRow(
                            title = "Strict Mode",
                            description = "Protects the app from being uninstalled during study",
                            checked = isStrictModeEnabled,
                            textPrimary = textPrimary,
                            textSecondary = textSecondary,
                            accentLime = accentLime,
                            onCheckedChange = { StrictModeManager.setStrictModeEnabled(context, it) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Flow: 1st non-study detection → warning with the mascot → keep going → the app locks for 3 hours. Returning to study topics clears the warning.",
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            color = textSecondary
                        )
                    }
                }

                // Punishment history
                item {
                    GuardSectionTitle("Recent Warnings & Blocks", textPrimary)
                    GuardCard {
                        if (punishmentLogs.isEmpty()) {
                            Text(
                                text = "No warnings yet — keep it that way! 📚",
                                fontSize = 13.sp,
                                color = textSecondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        } else {
                            punishmentLogs.take(12).forEach { log ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (log.status == "blocked") Icons.Default.Lock else Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = if (log.status == "blocked") Color(0xFFE05252) else accentLime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = friendlyPackageName(log.app_package) + " · " + log.status.replace('_', ' ').replaceFirstChar { it.uppercase() },
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = textPrimary
                                        )
                                        Text(
                                            text = formatLogDate(log.warned_at) + (log.block_duration_minutes?.let { " · locked ${it}m" } ?: ""),
                                            fontSize = 11.5.sp,
                                            color = textSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Pipeline self-test — bypasses the classifier, runs the real
                // warning -> 3-hour lock flow with a fake message
                item {
                    GuardSectionTitle("Test", textPrimary)
                    GuardCard {
                        Text(
                            text = "Runs the REAL warning → 3-hour lock pipeline with a fake test message — classifier ki zaroorat nahi. AI Guard master switch ON hona chahiye.",
                            fontSize = 12.5.sp,
                            lineHeight = 17.sp,
                            color = textSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(accentLime)
                                .clickable {
                                    AiStudyGuardManager.onNonStudyDetected(
                                        context = context,
                                        packageName = "com.test.guard",
                                        messageText = "test: movie dekhega kya bhai?",
                                        reason = "Manual pipeline test (Test Guard button)"
                                    )
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "▶ Test Guard Pipeline",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Text(
                            text = "1st tap → mascot warning. Warning ke baad dobara tap → test app pe 3-hour lock lag jayega.",
                            fontSize = 11.5.sp,
                            color = textSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // How it works
                item {
                    GuardSectionTitle("How it works", textPrimary)
                    GuardCard {
                        val steps = listOf(
                            "1. Start a focus timer or study schedule — the guard arms automatically.",
                            "2. It reads what's on screen inside ChatGPT, Claude, Gemini, Copilot, Perplexity and YouTube (only during study).",
                            "3. On-device AI judges study vs non-study instantly; optional cloud check verifies deeper.",
                            "4. Study doubts stay allowed — YouTube & AI chats remain open for genuine questions.",
                            "5. Off-topic chat → mascot warning → continue anyway → 3-hour lock of that app."
                        )
                        steps.forEach { step ->
                            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                                Icon(
                                    Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = accentLime,
                                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = step, fontSize = 12.5.sp, lineHeight = 17.sp, color = textSecondary)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }

    if (showOptInDialog) {
        AiStudyAccessibilityOptInDialog(
            onConfirmOptIn = {
                showOptInDialog = false
                AiStudyGuardManager.openAccessibilitySettings(context)
            },
            onDismiss = { showOptInDialog = false }
        )
    }
}

@Composable
private fun GuardSectionTitle(text: String, color: Color) {
    Text(
        text = text,
        fontFamily = BalooBrandFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        color = color
    )
}

/**
 * Shared glass card — reuses the app's central [GlassCard] style
 * (translucent surface, 1dp light border, soft shadow, top highlight).
 */
@Composable
private fun GuardCard(
    content: @Composable () -> Unit
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

@Composable
private fun GuardStatusRow(
    label: String,
    stateText: String,
    ok: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accentLime: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (ok) accentLime else Color(0xFFFF8A65))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            color = textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = stateText,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (ok) textPrimary else Color(0xFFE05252)
        )
    }
}

@Composable
private fun GuardSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    textPrimary: Color,
    textSecondary: Color,
    accentLime: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
            Text(text = description, fontSize = 12.sp, color = textSecondary)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = accentLime,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFC9CFC4)
            )
        )
    }
}

private fun friendlyPackageName(pkg: String): String {
    return when {
        pkg.contains("chatgpt") || pkg.contains("openai") -> "ChatGPT"
        pkg.contains("claude") || pkg.contains("anthropic") -> "Claude"
        pkg.contains("bard") || pkg.contains("gemini") -> "Gemini"
        pkg.contains("copilot") -> "Copilot"
        pkg.contains("perplexity") -> "Perplexity"
        pkg.contains("youtube") -> "YouTube"
        pkg.contains("instagram") -> "Instagram"
        else -> pkg.substringAfterLast(".")
    }
}

private fun formatLogDate(iso: String?): String {
    if (iso.isNullOrBlank()) return "recently"
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        val date = parser.parse(iso) ?: return "recently"
        SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(date)
    } catch (_: Exception) {
        "recently"
    }
}

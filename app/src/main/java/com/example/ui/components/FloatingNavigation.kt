package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CenterFocusStrong
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleLinearFontFamily
import com.example.ui.theme.MutedBorderLight
import com.example.ui.theme.NearBlack
import com.example.ui.theme.PureWhite
import com.example.ui.theme.RegainLimeContainer
import com.example.ui.theme.RegainLimeDeepText
import com.example.ui.theme.RegainLimeLight
import com.example.ui.theme.RegainLimePrimary
import com.example.ui.theme.SecondaryTextLight
import com.example.ui.theme.isAppInDarkTheme

enum class NavTab(
    val title: String,
    val icon: ImageVector
) {
    HOME("Home", Icons.Rounded.Home),
    FOCUS("Study", Icons.Rounded.CenterFocusStrong),
    LEADERBOARD("Ranking", Icons.Rounded.EmojiEvents),
    INSIGHTS("Stats", Icons.Rounded.BarChart),
    PROFILE("Profile", Icons.Rounded.Person)
}

@Composable
fun FloatingNavigation(
    selectedTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
    hapticsEnabled: Boolean = true
) {
    val view = LocalView.current
    val isDark = isAppInDarkTheme()

    val barBg = if (isDark) Color(0xF0181D18) else Color(0xF6FFFFFF)
    val barBorder = if (isDark) Color(0x35FFFFFF) else Color(0x18000000)
    val activeBg = RegainLimePrimary
    val activeContent = Color(0xFF061505)
    val inactiveContent = if (isDark) Color(0xFF8E8E93) else Color(0xFF6C6C70)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        // Apple-inspired Floating Dock with Liquid Glass refraction
        Box(
            modifier = Modifier
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    ambientColor = if (isDark) Color(0x28000000) else Color(0x0E000000),
                    spotColor = if (isDark) Color(0x45000000) else Color(0x18000000)
                )
                .clip(CircleShape)
                .background(barBg)
                .drawBehind {
                    val highlight = if (isDark) Color(0x35FFFFFF) else Color(0xB5FFFFFF)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                highlight.copy(alpha = 0.4f),
                                highlight,
                                highlight.copy(alpha = 0.4f),
                                Color.Transparent
                            )
                        ),
                        start = Offset(0f, 1f),
                        end = Offset(size.width, 1f),
                        strokeWidth = 1.6f
                    )
                }
                .border(0.85.dp, barBorder, CircleShape)
                .padding(horizontal = 7.dp, vertical = 5.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()

                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.94f else 1f,
                        animationSpec = spring(dampingRatio = 0.72f, stiffness = 400f),
                        label = "tab_scale"
                    )

                    val activeBgColor by animateColorAsState(
                        targetValue = if (isSelected) activeBg else Color.Transparent,
                        animationSpec = tween(180),
                        label = "tab_bg"
                    )

                    val activeContentColor by animateColorAsState(
                        targetValue = if (isSelected) activeContent else inactiveContent,
                        animationSpec = tween(180),
                        label = "tab_content"
                    )

                    Row(
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = scale
                                scaleY = scale
                            }
                            .clip(CircleShape)
                            .background(activeBgColor)
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null
                            ) {
                                if (hapticsEnabled) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                }
                                onTabSelected(tab)
                            }
                            .padding(
                                horizontal = if (isSelected) 14.dp else 10.dp,
                                vertical = 9.dp
                            )
                            .testTag("nav_tab_${tab.name.lowercase()}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = activeContentColor,
                            modifier = Modifier.size(19.dp)
                        )

                        AnimatedVisibility(
                            visible = isSelected,
                            enter = fadeIn(animationSpec = tween(150)) + expandHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ),
                            exit = fadeOut(animationSpec = tween(100)) + shrinkHorizontally(
                                animationSpec = tween(120)
                            )
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = AppleLinearFontFamily,
                                        color = activeContentColor,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.1.sp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


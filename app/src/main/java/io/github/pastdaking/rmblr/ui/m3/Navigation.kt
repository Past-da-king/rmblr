package io.github.pastdaking.rmblr.ui.m3

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.pastdaking.rmblr.ui.components.Space
import io.github.pastdaking.rmblr.ui.components.pressable
import io.github.pastdaking.rmblr.ui.theme.AccentWash
import io.github.pastdaking.rmblr.ui.theme.OnAccentWash

/** One tab of the floating navigation. */
data class FloatingNavItem(val title: String, val icon: ImageVector, val testTag: String)

/**
 * The floating navigation: a 64dp pill in primary container, four tabs, the selected one
 * expanding into a filled pill with its label (spatial spring, damping .8, stiffness 380).
 */
@Composable
fun FloatingNav(
    items: List<FloatingNavItem>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.lg, vertical = Space.md)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier
                .shadow(6.dp, RoundedCornerShape(32.dp))
                .clip(RoundedCornerShape(32.dp))
                .background(AccentWash)
                .height(64.dp)
                .padding(horizontal = 8.dp)
                .testTag("main_bottom_nav")
        ) {
            items.forEachIndexed { index, item ->
                NavTab(item, selected == index) { onSelect(index) }
            }
        }
    }
}

@Composable
private fun NavTab(item: FloatingNavItem, selected: Boolean, onClick: () -> Unit) {
    val weight by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
        label = "nav_tab"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .height(48.dp)
            .pressable(onClick = onClick)
            .clip(RoundedCornerShape(24.dp))
            .background(if (selected) OnAccentWash else AccentWash)
            .padding(horizontal = (12 + 6 * weight).dp)
            .testTag(item.testTag)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.title,
            tint = if (selected) AccentWash else OnAccentWash,
            modifier = Modifier.size(24.dp)
        )
        if (weight > 0.01f) {
            Spacer(Modifier.width((8 * weight).dp))
            Text(
                text = item.title,
                color = AccentWash,
                maxLines = 1,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.graphicsLayer { alpha = weight }
            )
        }
    }
}

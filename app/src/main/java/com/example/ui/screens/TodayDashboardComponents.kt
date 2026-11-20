package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.DashboardDomain
import com.example.model.HousekeepingCategory
import com.example.model.HousekeepingItem
import com.example.model.PriorityLevel
import com.example.ui.components.rememberStarkPulse
import com.example.ui.components.starkPulseGlow
import com.example.ui.theme.AlertCrimson
import com.example.ui.theme.AlertCrimsonDim
import com.example.ui.theme.ArcCyan
import com.example.ui.theme.HudOutline
import com.example.ui.theme.SecureEmerald
import com.example.ui.theme.SlateSurface
import com.example.ui.theme.SlateSurfaceElevated
import com.example.ui.theme.StarkGold
import com.example.ui.theme.TextPrimary

enum class DashboardTypeFilter(val label: String) {
    ALL("All"),
    TASKS("Tasks"),
    BILLS("Bills")
}

fun DashboardDomain.accentColor(): Color = when (this) {
    DashboardDomain.HOUSEKEEPING -> ArcCyan
    DashboardDomain.STUDY -> StarkGold
    DashboardDomain.SECURITY -> SecureEmerald
}

fun DashboardDomain.iconVector(): ImageVector = when (this) {
    DashboardDomain.HOUSEKEEPING -> Icons.Default.CleaningServices
    DashboardDomain.STUDY -> Icons.Default.School
    DashboardDomain.SECURITY -> Icons.Default.Shield
}

fun PriorityLevel.accentColor(): Color = when (this) {
    PriorityLevel.HIGH -> AlertCrimson
    PriorityLevel.MEDIUM -> StarkGold
    PriorityLevel.LOW -> SecureEmerald
}

fun PriorityLevel.iconVector(): ImageVector = when (this) {
    PriorityLevel.HIGH -> Icons.Default.ArrowUpward
    PriorityLevel.MEDIUM -> Icons.Default.DragHandle
    PriorityLevel.LOW -> Icons.Default.ArrowDownward
}

@Composable
fun DashboardKpiCard(
    label: String,
    count: Int,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurfaceElevated),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DashboardFilterChip(
    label: String,
    selected: Boolean,
    accentColor: Color = ArcCyan,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glowPulse = rememberStarkPulse(durationMillis = 2300, minValue = 0.25f, maxValue = 0.70f)
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .starkPulseGlow(glowColor = accentColor, cornerRadius = 50.dp, enabled = selected)
            .clip(RoundedCornerShape(50))
            .background(if (selected) accentColor.copy(alpha = 0.20f) else SlateSurfaceElevated)
            .border(1.dp, if (selected) accentColor else HudOutline, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = if (selected) {
                MaterialTheme.typography.labelLarge.copy(
                    shadow = Shadow(
                        color = accentColor.copy(alpha = glowPulse),
                        offset = Offset.Zero,
                        blurRadius = 8f
                    )
                )
            } else {
                MaterialTheme.typography.labelLarge
            },
            color = if (selected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
fun DashboardSectionHeader(
    domain: DashboardDomain,
    pendingCount: Int,
    totalCount: Int,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = domain.accentColor()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accent.copy(alpha = 0.16f))
                    .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            ) {
                Icon(
                    imageVector = domain.iconVector(),
                    contentDescription = domain.labelEnglish,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = domain.labelEnglish,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${domain.labelMarathi} • $pendingCount बाकी ($totalCount एकूण)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        TextButton(
            onClick = onQuickAdd,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.testTag("add_domain_${domain.name.lowercase()}_button")
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add to ${domain.labelEnglish}",
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Add",
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
        }
    }
}

@Composable
fun DashboardSectionEmptyCard(
    domain: DashboardDomain,
    typeFilter: DashboardTypeFilter,
    onQuickAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = domain.accentColor()
    val emptyMessage = when (typeFilter) {
        DashboardTypeFilter.BILLS -> "${domain.labelEnglish} मध्ये आज कोणतेही बिल नाही."
        DashboardTypeFilter.TASKS -> "${domain.labelEnglish} मध्ये आज कोणतेही काम नाही."
        DashboardTypeFilter.ALL -> "${domain.labelEnglish} मध्ये आज कोणतेही काम किंवा बिल नाही."
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = SlateSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HudOutline.copy(alpha = 0.7f), RoundedCornerShape(14.dp))
            .clickable(onClick = onQuickAdd)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Text(
                text = emptyMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "+ जोडा",
                style = MaterialTheme.typography.labelLarge,
                color = accent
            )
        }
    }
}

@Composable
fun SwipeToDeleteDashboardItem(
    item: HousekeepingItem,
    showDomainBadge: Boolean = false,
    onToggle: () -> Unit,
    onPriorityChange: (PriorityLevel) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentOnDelete by rememberUpdatedState(onDelete)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { targetValue ->
            targetValue == SwipeToDismissBoxValue.EndToStart
        }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            currentOnDelete()
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        modifier = modifier
            .fillMaxWidth()
            .testTag("swipe_dismiss_item_${item.id}"),
        backgroundContent = {
            val isTriggered = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val bgColor by animateColorAsState(
                targetValue = if (isTriggered) AlertCrimson else AlertCrimsonDim.copy(alpha = 0.7f),
                label = "swipe_delete_bg"
            )
            val iconScale by animateFloatAsState(
                targetValue = if (isTriggered) 1.15f else 0.92f,
                label = "swipe_delete_scale"
            )

            Box(
                contentAlignment = Alignment.CenterEnd,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(bgColor)
                    .border(
                        width = 1.dp,
                        color = AlertCrimson.copy(alpha = if (isTriggered) 0.9f else 0.45f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.scale(iconScale)
                ) {
                    Text(
                        text = "हटवा",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Swipe to delete",
                        tint = TextPrimary
                    )
                }
            }
        }
    ) {
        DashboardItemCard(
            item = item,
            showDomainBadge = showDomainBadge,
            onToggle = onToggle,
            onPriorityChange = onPriorityChange,
            onDelete = onDelete
        )
    }
}

@Composable
fun DashboardItemCard(
    item: HousekeepingItem,
    showDomainBadge: Boolean = false,
    onToggle: () -> Unit,
    onPriorityChange: (PriorityLevel) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val domain = item.domainEnum()
    val category = item.categoryEnum()
    val priority = item.priorityEnum()
    val isBill = category == HousekeepingCategory.BILL
    val domainColor = domain.accentColor()
    val priorityColor = priority.accentColor()
    var priorityMenuExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (item.completed) SlateSurface else SlateSurfaceElevated
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = when {
                    item.completed -> HudOutline.copy(alpha = 0.55f)
                    priority == PriorityLevel.HIGH -> AlertCrimson.copy(alpha = 0.6f)
                    isBill -> StarkGold.copy(alpha = 0.45f)
                    else -> domainColor.copy(alpha = 0.35f)
                },
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(
                        if (item.completed) HudOutline.copy(alpha = 0.45f) else priorityColor
                    )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 2.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)
            ) {
                Checkbox(
                    checked = item.completed,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = SecureEmerald,
                        uncheckedColor = if (isBill) StarkGold else domainColor
                    ),
                    modifier = Modifier.testTag("dashboard_item_checkbox_${item.id}")
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val badgeColor = if (isBill) StarkGold else domainColor
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.16f))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = if (isBill) Icons.Default.ReceiptLong else Icons.Default.TaskAlt,
                                contentDescription = category.shortEnglish,
                                tint = badgeColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = category.shortEnglish.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = badgeColor
                            )
                        }

                        if (showDomainBadge) {
                            Text(
                                text = domain.labelEnglish,
                                style = MaterialTheme.typography.labelSmall,
                                color = domainColor,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(domainColor.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }

                        Box {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(priorityColor.copy(alpha = 0.16f))
                                    .border(
                                        width = 1.dp,
                                        color = priorityColor.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable { priorityMenuExpanded = true }
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                                    .testTag("dashboard_item_priority_${item.id}")
                            ) {
                                Icon(
                                    imageVector = priority.iconVector(),
                                    contentDescription = "Change priority",
                                    tint = priorityColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = "${priority.labelEnglish.uppercase()} • ${priority.labelMarathi}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = priorityColor
                                )
                            }

                            DropdownMenu(
                                expanded = priorityMenuExpanded,
                                onDismissRequest = { priorityMenuExpanded = false },
                                containerColor = SlateSurfaceElevated
                            ) {
                                listOf(PriorityLevel.HIGH, PriorityLevel.MEDIUM, PriorityLevel.LOW).forEach { level ->
                                    val levelColor = level.accentColor()
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = "${level.labelEnglish} (${level.labelMarathi})",
                                                color = levelColor,
                                                fontWeight = if (level == priority) FontWeight.Bold else FontWeight.Normal
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = level.iconVector(),
                                                contentDescription = level.labelEnglish,
                                                tint = levelColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        trailingIcon = if (level == priority) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Selected",
                                                    tint = levelColor,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        } else null,
                                        onClick = {
                                            priorityMenuExpanded = false
                                            onPriorityChange(level)
                                        },
                                        modifier = Modifier.testTag("priority_menu_${item.id}_${level.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (item.completed) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.completed) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (item.scheduleLabel.isNotBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = item.scheduleLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (item.notes.isNotBlank()) {
                            Text(
                                text = item.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("dashboard_item_delete_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete item",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

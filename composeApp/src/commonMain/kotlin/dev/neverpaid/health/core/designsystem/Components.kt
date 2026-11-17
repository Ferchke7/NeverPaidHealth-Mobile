package dev.neverpaid.health.core.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.math.StrengthCalculator

// ============================================================================
// Gym design system: shared primitives
// Flat tonal cards, big rounded corners, one lime accent, heavy numerals.
// ============================================================================

private val CardRadius = 20.dp
private val ControlRadius = 16.dp
private val TileRadius = 14.dp

private fun brandWordmark(): AnnotatedString = buildAnnotatedString {
    withStyle(SpanStyle(color = TextPrimary, fontWeight = FontWeight.Black)) { append("duda") }
    withStyle(SpanStyle(color = AccentLime, fontWeight = FontWeight.Black)) { append(".uz") }
}

@Composable
fun GymHeaderBar(
    menuLabel: String,
    hasActiveWorkout: Boolean = false,
    isWorkoutScreen: Boolean = false,
    hasFocusOpen: Boolean = false,
    onLogoClick: () -> Unit = {},
    onLiveClick: () -> Unit = {},
    onFocusClick: () -> Unit = {},
    onMenuClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = brandWordmark(),
            fontSize = 24.sp,
            letterSpacing = (-0.8).sp,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onLogoClick)
                .padding(vertical = 6.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (hasActiveWorkout && !isWorkoutScreen) {
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(CircleShape)
                        .background(AccentLime)
                        .clickable(onClick = onLiveClick)
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(GymInkOnLight)
                    )
                    Text(
                        text = "LIVE",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.6.sp,
                        color = GymInkOnLight
                    )
                }
            }

            if (hasFocusOpen) {
                RoundIconButton(
                    icon = Icons.Default.Timer,
                    contentDescription = "Focus Timer",
                    tint = AccentLime,
                    onClick = onFocusClick
                )
            }

            RoundIconButton(
                icon = Icons.Default.Apps,
                contentDescription = menuLabel,
                tint = TextPrimary,
                onClick = onMenuClick
            )
        }
    }
}

@Composable
private fun RoundIconButton(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(DarkSurfaceHigh)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

data class GymNavDestination(
    val route: String,
    val label: String,
    val icon: ImageVector? = null
)

@Composable
fun GymBottomNavBar(
    items: List<GymNavDestination>,
    selectedRoute: String,
    onSelectRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = DarkSurface,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = item.route == selectedRoute
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(TileRadius))
                        .clickable { onSelectRoute(item.route) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .width(52.dp)
                            .height(30.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) AccentLime else Color.Transparent),
                        contentAlignment = Alignment.Center
                    ) {
                        if (item.icon != null) {
                            Icon(
                                imageVector = item.icon,
                                contentDescription = item.label,
                                tint = if (isSelected) GymInkOnLight else TextMuted,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Text(
                        text = item.label,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) TextPrimary else TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymSectionMenuSheet(
    title: String,
    subtitle: String,
    destinations: List<GymNavDestination>,
    currentRoute: String,
    onSelectRoute: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimary
                )
                if (subtitle.isNotBlank()) {
                    Text(text = subtitle, fontSize = 14.sp, color = TextSecondary)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                destinations.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { dest ->
                            val isSelected = dest.route == currentRoute
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(CardRadius))
                                    .background(if (isSelected) AccentLime else DarkSurfaceHigh)
                                    .clickable {
                                        onSelectRoute(dest.route)
                                        onDismiss()
                                    }
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                if (dest.icon != null) {
                                    Icon(
                                        imageVector = dest.icon,
                                        contentDescription = null,
                                        tint = if (isSelected) GymInkOnLight else AccentLime,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Text(
                                    text = dest.label,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) GymInkOnLight else TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun GymPageHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 36.sp,
            letterSpacing = (-1).sp,
            color = TextPrimary
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextSecondary
            )
        }
    }
}

/** Section heading inside a screen, with an optional lime text action on the right. */
@Composable
fun GymSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.4).sp,
            color = TextPrimary,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (actionLabel != null && onAction != null) {
            Text(
                text = actionLabel,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AccentLime,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onAction)
                    .padding(horizontal = 6.dp, vertical = 4.dp)
            )
        }
    }
}

/** Small uppercase caption above a title. */
@Composable
fun GymKicker(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = AccentLime
) {
    Text(
        text = text.uppercase(),
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = color,
        modifier = modifier
    )
}

/** Rounded square icon holder used in list rows and card headers. */
@Composable
fun GymIconBadge(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    tint: Color = AccentLime,
    background: Color = AccentLimeSubtle,
    size: Dp = 44.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(TileRadius))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/** Compact metric tile: caption, big figure, optional unit. */
@Composable
fun GymStatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String = "",
    valueColor: Color = TextPrimary
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(CardRadius))
            .background(DarkSurface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = valueColor,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun GymCard(
    modifier: Modifier = Modifier,
    backgroundColor: Color = DarkSurface,
    borderColor: Color = Color.Transparent,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(CardRadius)),
        shape = RoundedCornerShape(CardRadius),
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

@Composable
fun GymDivider(
    modifier: Modifier = Modifier,
    color: Color = BorderDivider
) {
    HorizontalDivider(
        modifier = modifier.fillMaxWidth(),
        thickness = 1.dp,
        color = color
    )
}

@Composable
private fun GymButtonLabel(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

private val ButtonPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp)
private val ButtonMinHeight = 52.dp

@Composable
fun GymPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(ControlRadius),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentLime,
            contentColor = GymInkOnLight,
            disabledContainerColor = DarkSurfaceHigh,
            disabledContentColor = TextMuted
        ),
        contentPadding = ButtonPadding,
        modifier = (if (fillWidth) modifier.fillMaxWidth() else modifier)
            .defaultMinSize(minHeight = ButtonMinHeight)
    ) {
        GymButtonLabel(text)
    }
}

/** Secondary action: tonal fill, no outline. Name kept for call-site compatibility. */
@Composable
fun GymOutlineButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false,
    textColor: Color = TextPrimary,
    borderColor: Color = BorderSubtle
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(ControlRadius),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        border = if (borderColor == BorderSubtle) null else BorderStroke(1.dp, borderColor),
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkSurfaceHigh,
            contentColor = textColor,
            disabledContainerColor = DarkSurfaceHigh,
            disabledContentColor = TextMuted
        ),
        contentPadding = ButtonPadding,
        modifier = (if (fillWidth) modifier.fillMaxWidth() else modifier)
            .defaultMinSize(minHeight = ButtonMinHeight)
    ) {
        GymButtonLabel(text)
    }
}

@Composable
fun GymWhiteButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    fillWidth: Boolean = false
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(ControlRadius),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = TextPrimary,
            contentColor = GymInkOnLight,
            disabledContainerColor = DarkSurfaceHigh,
            disabledContentColor = TextMuted
        ),
        contentPadding = ButtonPadding,
        modifier = (if (fillWidth) modifier.fillMaxWidth() else modifier)
            .defaultMinSize(minHeight = ButtonMinHeight)
    ) {
        GymButtonLabel(text)
    }
}

/** Filter / tab chip: lime when selected, tonal otherwise. */
@Composable
fun GymPillTab(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .clip(CircleShape)
            .background(if (selected) AccentLime else DarkSurfaceHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) GymInkOnLight else TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun GymTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                fontSize = 15.sp,
                color = TextMuted
            )
        },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        textStyle = TextStyle(
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            fontFeatureSettings = "tnum"
        ),
        shape = RoundedCornerShape(ControlRadius),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = DarkSurfaceHigh,
            unfocusedContainerColor = DarkSurfaceHigh,
            focusedBorderColor = AccentLime,
            unfocusedBorderColor = Color.Transparent,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = AccentLime
        ),
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
    )
}

@Composable
fun GymLabeledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "",
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        GymTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = placeholder,
            keyboardOptions = keyboardOptions,
            singleLine = singleLine,
            minLines = minLines
        )
    }
}

@Composable
fun GymInnerWell(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(ControlRadius))
            .background(DarkSurfaceHigh)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun GymProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    heightDp: Int = 8,
    color: Color = AccentLime
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(CircleShape)
            .background(DarkSurfaceHigh)
    ) {
        if (clamped > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .height(heightDp.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
    }
}

@Composable
fun GymLoadingCard(
    title: String,
    hint: String,
    modifier: Modifier = Modifier
) {
    GymCard(modifier = modifier) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.55f)
                    .height(16.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceHigh)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(ControlRadius))
                    .background(DarkSurfaceHigh)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(ControlRadius))
                    .background(DarkSurfaceHigh)
            )
        }
        Text(
            text = hint,
            fontSize = 14.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun GymEmptyCard(
    title: String,
    description: String,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    GymCard(modifier = modifier) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            color = TextPrimary
        )
        Text(
            text = description,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            color = TextSecondary
        )
        if (primaryActionLabel != null && onPrimaryAction != null) {
            GymPrimaryButton(
                text = primaryActionLabel,
                onClick = onPrimaryAction,
                fillWidth = true
            )
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            GymOutlineButton(
                text = secondaryActionLabel,
                onClick = onSecondaryAction,
                fillWidth = true
            )
        }
    }
}

// ============================================================================
// Domain components (names kept for existing call sites)
// ============================================================================

@Composable
fun DudaBrandLogo(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    onClick: () -> Unit = {}
) {
    Text(
        text = brandWordmark(),
        fontSize = if (compact) 20.sp else 26.sp,
        letterSpacing = (-0.8).sp,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(4.dp)
    )
}

@Composable
fun DudaTopHeader(
    streakDays: Int = 4,
    onLogoClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    GymHeaderBar(
        menuLabel = "Меню",
        onLogoClick = onLogoClick,
        onMenuClick = onProfileClick,
        modifier = modifier
    )
}

data class QuickNavPillItem(
    val id: String,
    val title: String,
    val icon: ImageVector? = null
)

@Composable
fun DudaQuickNavPills(
    items: List<QuickNavPillItem>,
    selectedId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            GymPillTab(
                text = item.title,
                selected = item.id == selectedId,
                onClick = { onSelect(item.id) }
            )
        }
    }
}

@Composable
fun AthleticCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(CardRadius),
    backgroundColor: Color = DarkSurface,
    borderColor: Color = Color.Transparent,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(width = 1.dp, color = borderColor, shape = shape),
        shape = shape,
        color = backgroundColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        content()
    }
}

@Composable
fun SetTypeBadge(
    type: String, // "warmup", "normal", "drop", "failure"
    setIndex: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val (badgeText, badgeColor, textColor) = when (type.lowercase()) {
        "warmup", "w" -> Triple("W", AmberWarm, GymInkOnLight)
        "drop", "d" -> Triple("D", InfoAccent, GymInkOnLight)
        "failure", "f" -> Triple("F", CrimsonRed, TextPrimary)
        else -> Triple("$setIndex", DarkSurfaceElevated, TextPrimary)
    }

    Box(
        modifier = modifier
            .size(36.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(badgeColor)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = badgeText,
            fontWeight = FontWeight.Black,
            fontSize = 14.sp,
            color = textColor
        )
    }
}

@Composable
fun PrimaryAthleticButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ButtonMinHeight),
        enabled = enabled,
        shape = RoundedCornerShape(ControlRadius),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentLime,
            contentColor = GymInkOnLight,
            disabledContainerColor = DarkSurfaceHigh,
            disabledContentColor = TextMuted
        ),
        contentPadding = ButtonPadding
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        GymButtonLabel(text)
    }
}

@Composable
fun SecondaryAthleticButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = ButtonMinHeight),
        enabled = enabled,
        shape = RoundedCornerShape(ControlRadius),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp, 0.dp, 0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkSurfaceHigh,
            contentColor = TextPrimary,
            disabledContainerColor = DarkSurfaceHigh,
            disabledContentColor = TextMuted
        ),
        contentPadding = ButtonPadding
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        GymButtonLabel(text)
    }
}

@Composable
fun StatCounter(
    title: String,
    value: String,
    unit: String = "",
    modifier: Modifier = Modifier,
    accentColor: Color = TextPrimary
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = TextSecondary
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontWeight = FontWeight.Black,
                fontSize = 28.sp,
                letterSpacing = (-0.5).sp,
                color = accentColor,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            if (unit.isNotEmpty()) {
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = unit,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RestTimerFloatingBar(
    secondsRemaining: Int,
    onAddSeconds: (Int) -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = StrengthCalculator.formatTime(minutes, seconds)

    GymCard(
        modifier = modifier.padding(16.dp),
        backgroundColor = DarkSurfaceElevated,
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GymKicker(text = "Отдых")
            Text(
                text = timeFormatted,
                fontSize = 44.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1).sp,
                color = TextPrimary,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
        }
        GymProgressBar(progress = (secondsRemaining.toFloat() / 90f).coerceIn(0f, 1f))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GymOutlineButton(
                text = "−15",
                onClick = { onAddSeconds(-15) },
                modifier = Modifier.weight(1f)
            )
            GymOutlineButton(
                text = "+30",
                onClick = { onAddSeconds(30) },
                modifier = Modifier.weight(1f)
            )
            GymPrimaryButton(
                text = "Пропустить",
                onClick = onSkip,
                modifier = Modifier.weight(1.4f)
            )
        }
    }
}

package dev.neverpaid.health.core.designsystem

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.data.catalog.ExerciseVisualResolver
import kotlinx.coroutines.delay

enum class ExerciseThumbnailSize(val dpSize: Dp, val iconSize: Dp, val cornerRadius: Dp) {
    XS(32.dp, 16.dp, 10.dp),
    SM(44.dp, 20.dp, 12.dp),
    MD(56.dp, 24.dp, 14.dp),
    LG(72.dp, 28.dp, 16.dp)
}

private fun muscleTintColor(muscle: String?): Color = when (muscle?.lowercase()?.trim()) {
    "chest" -> CrimsonRed
    "back", "lats", "middle back", "lower back" -> InfoAccent
    "quads", "quadriceps", "hamstrings", "glutes", "legs", "calves" -> AccentLime
    "shoulders" -> AmberGold
    "biceps", "triceps", "arms", "forearms" -> AccentLime
    "core", "abs", "abdominals" -> AmberWarm
    else -> AccentLime
}

@Composable
fun ExerciseThumbnail(
    exerciseName: String,
    sourceId: String? = null,
    exerciseId: String? = null,
    fallbackMuscle: String? = null,
    size: ExerciseThumbnailSize = ExerciseThumbnailSize.MD,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val visualMeta = remember(exerciseName, sourceId, exerciseId) {
        ExerciseVisualResolver.resolve(
            nameOrId = exerciseName,
            sourceId = sourceId,
            exerciseId = exerciseId
        )
    }
    val imageState = rememberRemoteImageBitmap(visualMeta.primaryThumbnailUrl)
    val shape = RoundedCornerShape(size.cornerRadius)
    val tint = muscleTintColor(fallbackMuscle)

    Box(
        modifier = modifier
            .size(size.dpSize)
            .clip(shape)
            .background(RoyalWell)
            .let { base -> if (onClick != null) base.clickable(onClick = onClick) else base },
        contentAlignment = Alignment.Center
    ) {
        val bmp = imageState.bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = exerciseName,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.Default.FitnessCenter,
                contentDescription = exerciseName,
                tint = tint,
                modifier = Modifier.size(size.iconSize)
            )
        }
    }
}

/**
 * 2-frame alternating ("туда-сюда") exercise illustration card.
 * Preloads both frame 0.jpg (start) and 1.jpg (peak), cycling every 1200ms while auto-playing.
 */
@Composable
fun AnimatedExerciseFramesCard(
    exerciseName: String,
    sourceId: String? = null,
    exerciseId: String? = null,
    primaryMuscle: String = "",
    lang: String = "ru",
    modifier: Modifier = Modifier
) {
    val visualMeta = remember(exerciseName, sourceId, exerciseId) {
        ExerciseVisualResolver.resolve(
            nameOrId = exerciseName,
            sourceId = sourceId,
            exerciseId = exerciseId
        )
    }

    val frame0State = rememberRemoteImageBitmap(visualMeta.frameUrls.getOrNull(0))
    val frame1State = rememberRemoteImageBitmap(visualMeta.frameUrls.getOrNull(1))

    var activeFrame by remember(exerciseName, sourceId, exerciseId) { mutableIntStateOf(0) }
    var isAutoPlaying by remember(exerciseName, sourceId, exerciseId) { mutableStateOf(true) }

    val hasTwoLoadedFrames = frame0State.bitmap != null && frame1State.bitmap != null
    val hasAnyFrame = frame0State.bitmap != null || frame1State.bitmap != null

    LaunchedEffect(isAutoPlaying, hasTwoLoadedFrames, visualMeta.resolvedId) {
        if (!isAutoPlaying || !hasTwoLoadedFrames) return@LaunchedEffect
        while (true) {
            delay(1200L)
            activeFrame = if (activeFrame == 0) 1 else 0
        }
    }

    val phaseHeader = when (lang) {
        "uz" -> if (activeFrame == 0) "BOSHLANG'ICH HOLAT" else "ENG YUQORI NUQTA"
        "en" -> if (activeFrame == 0) "START POSITION" else "PEAK CONTRACTION"
        else -> if (activeFrame == 0) "ИСХОДНОЕ ПОЛОЖЕНИЕ" else "ПИКОВОЕ СОКРАЩЕНИЕ"
    }
    val startLabel = when (lang) { "uz" -> "Start"; "en" -> "Start"; else -> "Старт" }
    val peakLabel = when (lang) { "uz" -> "Pik"; "en" -> "Peak"; else -> "Пик" }
    val animLabel = when (lang) { "uz" -> "Avto"; "en" -> "Auto"; else -> "Авто" }
    val pauseLabel = when (lang) { "uz" -> "Pauza"; "en" -> "Pause"; else -> "Пауза" }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(SurfaceWhite)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = phaseHeader,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = AccentLime,
                modifier = Modifier.weight(1f)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (visualMeta.hasAnimatedFrames) {
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isAutoPlaying) AccentLimeSubtle else DarkSurfaceElevated)
                            .clickable { isAutoPlaying = !isAutoPlaying }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isAutoPlaying) pauseLabel else animLabel,
                            tint = if (isAutoPlaying) AccentLime else TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isAutoPlaying) pauseLabel else animLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isAutoPlaying) AccentLime else TextSecondary
                        )
                    }
                }

                listOf(0 to startLabel, 1 to peakLabel).forEach { (idx, label) ->
                    val selected = activeFrame == idx
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (selected) VioletPrimary else SurfaceLilac)
                            .clickable {
                                isAutoPlaying = false
                                activeFrame = idx
                            }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selected) InkWhite else TextSecondary
                        )
                    }
                }
            }
        }

        val viewportModifier = if (visualMeta.hasImages && (hasAnyFrame || frame0State.isLoading)) {
            Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .clip(RoundedCornerShape(16.dp))
                .background(GymWell)
        } else {
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GymWell)
                .padding(18.dp)
        }

        Box(
            modifier = viewportModifier,
            contentAlignment = Alignment.Center
        ) {
            when {
                hasAnyFrame -> {
                    val shownFrameIndex = if (activeFrame == 1 && frame1State.bitmap != null) 1 else 0
                    Crossfade(
                        targetState = shownFrameIndex,
                        animationSpec = tween(durationMillis = 250),
                        label = "ExerciseFrameCrossfade"
                    ) { frameIdx ->
                        val bmp = if (frameIdx == 1) {
                            frame1State.bitmap ?: frame0State.bitmap
                        } else {
                            frame0State.bitmap ?: frame1State.bitmap
                        }
                        if (bmp != null) {
                            Image(
                                bitmap = bmp,
                                contentDescription = exerciseName,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    }

                    if (hasTwoLoadedFrames) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(10.dp)
                                .clip(CircleShape)
                                .background(DarkSurface.copy(alpha = 0.9f))
                                .clickable { isAutoPlaying = !isAutoPlaying }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isAutoPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = AccentLime,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${shownFrameIndex + 1} / 2",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                        }
                    }
                }

                frame0State.isLoading -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = AccentLime,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (lang) {
                                "uz" -> "Rasm yuklanmoqda..."
                                "en" -> "Loading illustration..."
                                else -> "Загружаем иллюстрацию..."
                            },
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                else -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = if (activeFrame == 0) AccentLime else AmberWarm,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = phaseCoachingCue(activeFrame, primaryMuscle, lang),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (hasAnyFrame) {
            Text(
                text = phaseCoachingCue(activeFrame, primaryMuscle, lang),
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp
            )
        }
    }
}

private fun phaseCoachingCue(activeFrame: Int, primaryMuscle: String, lang: String): String {
    val muscleName = localizeMuscleName(primaryMuscle).lowercase()
    return when (lang) {
        "uz" -> if (activeFrame == 0) {
            "Nazorat ostida cho'zilish • Nafas olish • Kuraklar va kor barqarorligi"
        } else {
            "$muscleName mushagining kuchli qisqarishi • Nafas chiqarish • 0.5s pauza"
        }
        "en" -> if (activeFrame == 0) {
            "Controlled eccentric stretch • Inhale • Scapular & core stability"
        } else {
            "Peak $muscleName contraction • Exhale • 0.5s squeeze at top"
        }
        else -> if (activeFrame == 0) {
            "Растяжение под контролем • Вдох • Стабилизация лопаток и кора"
        } else {
            "Мощное сокращение $muscleName • Выдох • Пауза 0.5с в верхней точке"
        }
    }
}

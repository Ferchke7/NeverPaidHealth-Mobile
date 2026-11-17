package dev.neverpaid.health.features.nutrition

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.GymIconBadge
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymTextField
import dev.neverpaid.health.core.designsystem.InfoAccent
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.data.catalog.StandardFoodCatalog
import dev.neverpaid.health.domain.model.AthleticFoodItem
import dev.neverpaid.health.domain.model.FoodCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodDatabaseBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onFoodSelected: (name: String, calories: Double, protein: Double, carbs: Double, fats: Double) -> Unit
) {
    if (!isOpen) return

    val strings = LocalDudaStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<FoodCategory?>(null) }
    var activeFoodItem by remember { mutableStateOf<AthleticFoodItem?>(null) }
    var portionGramsText by remember { mutableStateOf("100") }

    val filteredFoods = remember(searchQuery, selectedCategory) {
        StandardFoodCatalog.search(searchQuery, selectedCategory)
    }

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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GymIconBadge(
                        icon = Icons.Default.Restaurant,
                        tint = AccentLime,
                        background = AccentLimeSubtle,
                        size = 40.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "База продуктов",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "${filteredFoods.size} продуктов · на 100 г",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceHigh)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.cancel,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Search TextField
            GymTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = "Поиск (курица, рис, творог, лосось...)"
            )

            // Category Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item(key = "cat_all") {
                    GymPillTab(
                        text = "Все",
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                }
                items(FoodCategory.values(), key = { it.name }) { category ->
                    val catLabel = when (category) {
                        FoodCategory.PROTEIN_SOURCE -> "Белки"
                        FoodCategory.COMPLEX_CARBS -> "Углеводы"
                        FoodCategory.HEALTHY_FATS -> "Жиры"
                        FoodCategory.DAIRY_EGGS -> "Молочные"
                        FoodCategory.FRUITS_BERRIES -> "Фрукты"
                        FoodCategory.VEGETABLES_GREENS -> "Овощи"
                        FoodCategory.SPORTS_SUPPLEMENTS -> "Спортпит"
                        FoodCategory.PRE_WORKOUT_SNACKS -> "Перекусы"
                    }
                    GymPillTab(
                        text = catLabel,
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                        }
                    )
                }
            }

            // Portion Adjuster if food is selected
            if (activeFoodItem != null) {
                val currentItem = activeFoodItem!!
                val grams = portionGramsText.toDoubleOrNull() ?: 100.0
                val calculated = currentItem.calculateNutrients(grams)
                val adjusterShape = RoundedCornerShape(20.dp)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(adjusterShape)
                        .background(DarkSurfaceHigh)
                        .border(1.dp, BorderHighlight, adjusterShape)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = currentItem.localizedName(strings.languageCode),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${calculated.calories.toInt()}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-0.8).sp,
                                    color = AccentLime,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                                Text(
                                    text = "ккал · ${grams.toInt()} г",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    style = TextStyle(fontFeatureSettings = "tnum"),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(DarkSurface)
                                .clickable { activeFoodItem = null },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Macros Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroPillBadge(
                            label = "Белки",
                            value = "${calculated.protein.toInt()} г",
                            color = AccentLime,
                            modifier = Modifier.weight(1f)
                        )
                        MacroPillBadge(
                            label = "Углеводы",
                            value = "${calculated.carbs.toInt()} г",
                            color = InfoAccent,
                            modifier = Modifier.weight(1f)
                        )
                        MacroPillBadge(
                            label = "Жиры",
                            value = "${calculated.fats.toInt()} г",
                            color = AmberWarm,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Standard portions quick picker
                    if (currentItem.servingPortions.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(currentItem.servingPortions, key = { it.label }) { portion ->
                                val portionStr = portion.grams.toInt().toString()
                                GymPillTab(
                                    text = portion.label,
                                    selected = portionGramsText == portionStr,
                                    onClick = { portionGramsText = portionStr }
                                )
                            }
                        }
                    }

                    // Grams input + Log button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GymTextField(
                            value = portionGramsText,
                            onValueChange = { input -> portionGramsText = input.filter { it.isDigit() } },
                            placeholder = "Граммы (г)",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )

                        GymPrimaryButton(
                            text = "Добавить",
                            onClick = {
                                onFoodSelected(
                                    currentItem.localizedName(strings.languageCode),
                                    calculated.calories,
                                    calculated.protein,
                                    calculated.carbs,
                                    calculated.fats
                                )
                                onDismiss()
                            }
                        )
                    }
                }
            }

            // Food Catalog List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredFoods,
                    key = { it.id }
                ) { food ->
                    val isSelected = activeFoodItem?.id == food.id
                    val rowShape = RoundedCornerShape(16.dp)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .background(if (isSelected) AccentLimeSubtle else DarkSurfaceHigh)
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.dp, BorderHighlight, rowShape)
                                } else {
                                    Modifier
                                }
                            )
                            .clickable {
                                activeFoodItem = food
                                portionGramsText = "100"
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = food.localizedName(strings.languageCode),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Б ${food.proteinPer100g} · У ${food.carbsPer100g} · Ж ${food.fatsPer100g} г",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${food.caloriesPer100g.toInt()}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AccentLime,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                                Text(
                                    text = "ккал",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) AccentLime else DarkSurfaceElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = if (isSelected) GymInkOnLight else TextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MacroPillBadge(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = color,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
    }
}

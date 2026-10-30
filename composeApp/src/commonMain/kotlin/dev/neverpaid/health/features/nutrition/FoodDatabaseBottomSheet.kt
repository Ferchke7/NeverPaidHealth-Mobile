package dev.neverpaid.health.features.nutrition

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CyanElectric
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.PurpleAccent
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
        containerColor = DarkBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "База продуктов атлета (150+)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = strings.cancel, tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search TextField
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Поиск (курица, рис, творог, лосось...)", style = MaterialTheme.typography.bodyMedium, color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceElevated,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = EmeraldNeon,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Category Chips Row
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null },
                        label = { Text("Все", style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldNeon.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldNeon,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == null,
                            borderColor = BorderSubtle,
                            selectedBorderColor = EmeraldNeon
                        )
                    )
                }
                items(FoodCategory.values()) { category ->
                    val catLabel = when (category) {
                        FoodCategory.PROTEIN_SOURCE -> "Белки"
                        FoodCategory.COMPLEX_CARBS -> "Углеводы"
                        FoodCategory.HEALTHY_FATS -> "Жиры"
                        FoodCategory.DAIRY_EGGS -> "Молочные и яйца"
                        FoodCategory.FRUITS_BERRIES -> "Фрукты/ягоды"
                        FoodCategory.VEGETABLES_GREENS -> "Овощи/зелень"
                        FoodCategory.SPORTS_SUPPLEMENTS -> "Спортпит"
                        FoodCategory.PRE_WORKOUT_SNACKS -> "Перекусы"
                    }
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = if (selectedCategory == category) null else category },
                        label = { Text(catLabel, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldNeon.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldNeon,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedCategory == category,
                            borderColor = BorderSubtle,
                            selectedBorderColor = EmeraldNeon
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Portion Adjuster if food is selected
            if (activeFoodItem != null) {
                val currentItem = activeFoodItem!!
                val grams = portionGramsText.toDoubleOrNull() ?: 100.0
                val calculated = currentItem.calculateNutrients(grams)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                    border = BorderStroke(1.dp, EmeraldNeon)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentItem.localizedName(strings.languageCode),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${calculated.calories.toInt()} ккал на ${grams.toInt()}г",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = EmeraldNeon,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            IconButton(onClick = { activeFoodItem = null }) {
                                Icon(Icons.Default.Close, contentDescription = null, tint = TextMuted)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Macros Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "Б: ${calculated.protein.toInt()}г",
                                style = MaterialTheme.typography.labelSmall,
                                color = IndigoElectric,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Ж: ${calculated.fats.toInt()}г",
                                style = MaterialTheme.typography.labelSmall,
                                color = PurpleAccent,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "У: ${calculated.carbs.toInt()}г",
                                style = MaterialTheme.typography.labelSmall,
                                color = CyanElectric,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Standard portions quick picker
                        if (currentItem.servingPortions.isNotEmpty()) {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(currentItem.servingPortions) { portion ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = DarkSurfaceElevated,
                                        border = BorderStroke(1.dp, BorderSubtle),
                                        modifier = Modifier.clickable {
                                            portionGramsText = portion.grams.toInt().toString()
                                        }
                                    ) {
                                        Text(
                                            text = portion.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Grams input + Log button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = portionGramsText,
                                onValueChange = { portionGramsText = it },
                                modifier = Modifier.weight(1f),
                                label = { Text("Граммы (г)", style = MaterialTheme.typography.labelSmall) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true
                            )

                            FilledTonalButton(
                                onClick = {
                                    onFoodSelected(
                                        currentItem.localizedName(strings.languageCode),
                                        calculated.calories,
                                        calculated.protein,
                                        calculated.carbs,
                                        calculated.fats
                                    )
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = EmeraldNeon,
                                    contentColor = DarkBackground
                                ),
                                modifier = Modifier.height(52.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
            }

            // Food Catalog List
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredFoods) { food ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeFoodItem = food
                                portionGramsText = "100"
                            },
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, if (activeFoodItem?.id == food.id) EmeraldNeon else BorderSubtle)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = food.localizedName(strings.languageCode),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    Text(
                                        text = "${food.caloriesPer100g.toInt()} ккал",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = EmeraldNeon
                                    )
                                    Text(
                                        text = "Б: ${food.proteinPer100g}г",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = IndigoElectric
                                    )
                                    Text(
                                        text = "У: ${food.carbsPer100g}г",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = CyanElectric
                                    )
                                    Text(
                                        text = "Ж: ${food.fatsPer100g}г",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PurpleAccent
                                    )
                                }
                            }

                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

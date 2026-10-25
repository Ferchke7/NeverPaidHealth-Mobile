package dev.neverpaid.health.features.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.DudaTopHeader
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.SecondaryAthleticButton
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary

data class MealItem(
    val id: String,
    val name: String,
    val description: String,
    val calories: Int,
    val protein: Int
)

@Composable
fun NutritionScreen(
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val meals = remember {
        mutableStateListOf(
            MealItem("1", "Завтрак", "Овсянка с сывороточным протеином и ягодами", 540, 42),
            MealItem("2", "Обед", "Куриное филе на гриле, рис басмати и брокколи", 680, 55),
            MealItem("3", "Перекус", "Греческий йогурт 2% и спелый банан", 260, 22),
            MealItem("4", "Ужин", "Филе лосося, печёный картофель и спаржа", 420, 35)
        )
    }

    val totalCalories = meals.sumOf { it.calories }
    val targetCalories = 2450
    val caloriesProgress = (totalCalories.toFloat() / targetCalories.toFloat()).coerceIn(0f, 1f)

    val totalProtein = meals.sumOf { it.protein }
    val targetProtein = 180

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            DudaTopHeader(
                streakDays = 4,
                onProfileClick = onNavigateToProfile,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "ПИТАНИЕ И МАКРОСЫ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldLight,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Дневной рацион",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        }

        // Calorie Hero Card
        item(key = "calories_card") {
            AthleticCard(
                backgroundColor = DarkSurfaceVariant,
                borderColor = BorderHighlight
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "ЭНЕРГЕТИЧЕСКИЙ БАЛАНС",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$totalCalories",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = " / $targetCalories ккал",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldSubtle)
                                .border(width = 1.dp, color = BorderHighlight, shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Осталось ${targetCalories - totalCalories} ккал",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    LinearProgressIndicator(
                        progress = { caloriesProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldNeon,
                        trackColor = DarkSurfaceHigh,
                    )
                }
            }
        }

        // Macros Row (Protein, Fats, Carbs)
        item(key = "macros_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MacroCard(
                    title = "Белки",
                    current = "$totalProtein г",
                    target = "$targetProtein г",
                    color = EmeraldNeon,
                    modifier = Modifier.weight(1f)
                )
                MacroCard(
                    title = "Жиры",
                    current = "52 г",
                    target = "70 г",
                    color = AmberWarm,
                    modifier = Modifier.weight(1f)
                )
                MacroCard(
                    title = "Углеводы",
                    current = "215 г",
                    target = "280 г",
                    color = IndigoElectric,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Meals Section Header
        item(key = "meals_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ПРИЁМЫ ПИЩИ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "${meals.size} записи",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }

        // Meal list items
        items(meals, key = { it.id }) { meal ->
            AthleticCard(
                backgroundColor = DarkSurface,
                borderColor = BorderSubtle
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Restaurant,
                                contentDescription = null,
                                tint = EmeraldLight,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = meal.name,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = meal.description,
                                fontSize = 12.sp,
                                color = TextMuted,
                                maxLines = 1
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${meal.calories} ккал",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${meal.protein}г белка",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = EmeraldLight
                        )
                    }
                }
            }
        }

        item(key = "add_meal_button") {
            SecondaryAthleticButton(
                text = "+ Добавить продукт в рацион",
                onClick = {
                    meals.add(
                        MealItem(
                            id = (meals.size + 1).toString(),
                            name = "Перекус #${meals.size + 1}",
                            description = "Протеиновый батончик и кофе",
                            calories = 210,
                            protein = 20
                        )
                    )
                },
                icon = Icons.Default.Add
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MacroCard(
    title: String,
    current: String,
    target: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    AthleticCard(
        backgroundColor = DarkSurface,
        borderColor = BorderSubtle,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = current,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = "из $target",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

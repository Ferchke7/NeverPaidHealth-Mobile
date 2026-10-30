package dev.neverpaid.health.data.catalog

import dev.neverpaid.health.domain.model.AthleteHabit
import dev.neverpaid.health.domain.model.HabitCategory
import dev.neverpaid.health.domain.model.HabitFrequency

/**
 * Standard athletic habits engineered for elite physical performance,
 * injury prevention, muscle protein synthesis, and circadian rhythm alignment.
 */
object StandardHabitsCatalog {

    val standardPresets: List<AthleteHabit> = listOf(
        AthleteHabit(
            id = "preset_sleep_8h",
            title = "Сон 8 часов (глубокое восстановление ЦНС)",
            description = "Оптимальная продолжительность сна для секреции гормона роста (HGH) и мышечной регенерации.",
            category = HabitCategory.RECOVERY,
            targetValue = "8 ч",
            frequency = HabitFrequency.DAILY,
            iconName = "Bedtime"
        ),
        AthleteHabit(
            id = "preset_morning_water",
            title = "Утренний приём воды 500 мл",
            description = "Запуск метаболизма, регидратация органов и стимуляция перистальтики ЖКТ после ночного сна.",
            category = HabitCategory.HYDRATION,
            targetValue = "500 мл",
            frequency = HabitFrequency.DAILY,
            iconName = "WaterDrop"
        ),
        AthleteHabit(
            id = "preset_daily_creatine",
            title = "Приём креатина 5г",
            description = "Поддержание максимального уровня фосфокреатина в мышечной ткани для взрывной силы и объёма клеток.",
            category = HabitCategory.NUTRITION,
            targetValue = "5 г",
            frequency = HabitFrequency.DAILY,
            iconName = "FitnessCenter"
        ),
        AthleteHabit(
            id = "preset_daily_mobility",
            title = "Мобильность и суставная разминка (10 мин)",
            description = "Раскрытие тазобедренных суставов, грудного отдела позвоночника и голеностопа для профилактики травм.",
            category = HabitCategory.TRAINING,
            targetValue = "10 мин",
            frequency = HabitFrequency.DAILY,
            iconName = "SelfImprovement"
        ),
        AthleteHabit(
            id = "preset_steps_10k",
            title = "10 000 шагов (активность NEAT)",
            description = "Базовая нетренировочная активность для улучшения чувствительности к инсулину и сжигания жира.",
            category = HabitCategory.TRAINING,
            targetValue = "10000 шагов",
            frequency = HabitFrequency.DAILY,
            iconName = "DirectionsWalk"
        ),
        AthleteHabit(
            id = "preset_target_protein",
            title = "Норма белка 2.0 г/кг веса",
            description = "Обеспечение аминокислотного пула лейцина для максимального запуска MPS (синтеза мышечного белка).",
            category = HabitCategory.NUTRITION,
            targetValue = "2.0 г/кг",
            frequency = HabitFrequency.DAILY,
            iconName = "Egg"
        ),
        AthleteHabit(
            id = "preset_cold_shower",
            title = "Контрастный или холодный душ",
            description = "Стимуляция вагусного тонуса, выработка норадреналина и укрепление сосудистого русла.",
            category = HabitCategory.RECOVERY,
            targetValue = "3 мин",
            frequency = HabitFrequency.DAILY,
            iconName = "AcUnit"
        ),
        AthleteHabit(
            id = "preset_evening_disconnect",
            title = "Отказ от ярких экранов за 1 час до сна",
            description = "Снижение воздействия синего спектра света для естественной выработки мелатонина эпифизом.",
            category = HabitCategory.MINDSET,
            targetValue = "60 мин",
            frequency = HabitFrequency.DAILY,
            iconName = "NightlightRound"
        )
    )

    fun findById(id: String): AthleteHabit? = standardPresets.find { it.id == id }
}

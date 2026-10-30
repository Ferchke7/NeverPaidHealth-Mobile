package dev.neverpaid.health.core.math

import kotlin.math.roundToInt

/**
 * Protocol Strategy for Generating Warmup Sets.
 */
enum class WarmupProtocol(
    val id: String,
    val titleRu: String,
    val titleUz: String,
    val titleEn: String,
    val descriptionRu: String,
    val descriptionUz: String,
    val descriptionEn: String
) {
    STANDARD_HYPERTROPHY(
        id = "hypertrophy",
        titleRu = "Стандартный объёмный (4 подхода)",
        titleUz = "Standart gipertrofiya (4 yondashuv)",
        titleEn = "Standard Hypertrophy (4 sets)",
        descriptionRu = "Постепенная пирамида для рабочих подходов на 8-12 повторений.",
        descriptionUz = "8-12 takrorlik ishchi yondashuvlar uchun bosqichma-bosqich piramida.",
        descriptionEn = "Progressive pyramid for 8-12 rep working sets without accumulating fatigue."
    ),
    POWERLIFTING_HEAVY(
        id = "powerlifting",
        titleRu = "Силовой / Тяжёлый сингл (5-6 подходов)",
        titleUz = "Kuch / Og'ir singl (5-6 yondashuv)",
        titleEn = "Powerlifting Heavy Single (5-6 sets)",
        descriptionRu = "Тщательная подготовка ЦНС и суставов к весам 85%+ от 1ПМ.",
        descriptionUz = "85%+ og'irliklar uchun asab tizimi va bo'g'imlarni chuqur tayyorlash.",
        descriptionEn = "Deep CNS and joint potentiation for 85%+ 1RM heavy working weights."
    ),
    WENDLER_531(
        id = "wendler_531",
        titleRu = "Wendler 5/3/1 (3 подхода)",
        titleUz = "Wendler 5/3/1 (3 yondashuv)",
        titleEn = "Wendler 5/3/1 (3 sets)",
        descriptionRu = "Классическая разминка Джима Вендлера: 40%×5, 50%×5, 60%×3.",
        descriptionUz = "Jim Vendlerning klassik isinishi: 40%×5, 50%×5, 60%×3.",
        descriptionEn = "Jim Wendler's classic warmup: 40%×5, 50%×5, 60%×3."
    ),
    QUICK_POTENTIATION(
        id = "quick",
        titleRu = "Быстрая активация (2 подхода)",
        titleUz = "Tez faollashtirish (2 yondashuv)",
        titleEn = "Quick Potentiation (2 sets)",
        descriptionRu = "Экспресс-разминка при дефиците времени: 50%×8, 75%×3.",
        descriptionUz = "Vaqt kam bo'lganda tezkor isinish: 50%×8, 75%×3.",
        descriptionEn = "Express warmup when short on time: 50%×8, 75%×3."
    )
}

/**
 * A single calculated warmup set.
 */
data class WarmupSetStep(
    val stepIndex: Int,
    val percentageOfTarget: Int,
    val weightKg: Double,
    val reps: Int,
    val targetRpe: Double = 5.0,
    val suggestedRestSeconds: Int = 60,
    val note: String = "",
    val plateBreakdown: String = ""
)

/**
 * Scientific warmup set progression generator.
 */
object WarmupProtocolEngine {

    /**
     * Generates a sequence of warmup sets based on chosen protocol and target working weight.
     */
    fun generateWarmupSets(
        targetWorkingWeightKg: Double,
        protocol: WarmupProtocol = WarmupProtocol.STANDARD_HYPERTROPHY,
        barWeightKg: Double = 20.0
    ): List<WarmupSetStep> {
        if (targetWorkingWeightKg <= barWeightKg) {
            return listOf(
                WarmupSetStep(
                    stepIndex = 1,
                    percentageOfTarget = 100,
                    weightKg = barWeightKg,
                    reps = 10,
                    suggestedRestSeconds = 60,
                    note = "Разминка с пустым грифом",
                    plateBreakdown = "Пустой гриф"
                )
            )
        }

        return when (protocol) {
            WarmupProtocol.STANDARD_HYPERTROPHY -> generateHypertrophy(targetWorkingWeightKg, barWeightKg)
            WarmupProtocol.POWERLIFTING_HEAVY -> generatePowerlifting(targetWorkingWeightKg, barWeightKg)
            WarmupProtocol.WENDLER_531 -> generateWendler(targetWorkingWeightKg, barWeightKg)
            WarmupProtocol.QUICK_POTENTIATION -> generateQuick(targetWorkingWeightKg, barWeightKg)
        }
    }

    private fun generateHypertrophy(targetKg: Double, barKg: Double): List<WarmupSetStep> {
        val steps = mutableListOf<WarmupSetStep>()
        // Step 1: Empty bar
        steps.add(
            createStep(
                index = 1,
                pct = ((barKg / targetKg) * 100).toInt(),
                weightKg = barKg,
                reps = 10,
                restSec = 45,
                note = "Суставная разминка, темп 2-0-2"
            )
        )

        // Step 2: ~50%
        val w50 = roundToStep((targetKg * 0.50).coerceAtLeast(barKg), 2.5)
        if (w50 > barKg && w50 < targetKg) {
            steps.add(
                createStep(
                    index = steps.size + 1,
                    pct = 50,
                    weightKg = w50,
                    reps = 6,
                    restSec = 60,
                    note = "Включение целевых волокон"
                )
            )
        }

        // Step 3: ~70%
        val w70 = roundToStep((targetKg * 0.70).coerceAtLeast(w50 + 2.5), 2.5)
        if (w70 > w50 && w70 < targetKg) {
            steps.add(
                createStep(
                    index = steps.size + 1,
                    pct = 70,
                    weightKg = w70,
                    reps = 4,
                    restSec = 75,
                    note = "Контроль траектории и дыхания"
                )
            )
        }

        // Step 4: ~85%
        val w85 = roundToStep((targetKg * 0.85).coerceAtLeast(w70 + 2.5), 2.5)
        if (w85 > w70 && w85 < targetKg - 2.5) {
            steps.add(
                createStep(
                    index = steps.size + 1,
                    pct = 85,
                    weightKg = w85,
                    reps = 2,
                    restSec = 90,
                    note = "Финальный акклиматизационный сингл"
                )
            )
        }

        return steps
    }

    private fun generatePowerlifting(targetKg: Double, barKg: Double): List<WarmupSetStep> {
        val steps = mutableListOf<WarmupSetStep>()
        // 1. Bar x 10
        steps.add(createStep(1, ((barKg / targetKg) * 100).toInt(), barKg, 8, 60, "Активация суставов и связок"))

        // 2. 40% x 5
        val w40 = roundToStep((targetKg * 0.40).coerceAtLeast(barKg), 2.5)
        if (w40 > barKg) steps.add(createStep(steps.size + 1, 40, w40, 5, 60, "Наработка скорости съёма"))

        // 3. 60% x 3
        val w60 = roundToStep((targetKg * 0.60).coerceAtLeast(w40 + 2.5), 2.5)
        if (w60 > w40) steps.add(createStep(steps.size + 1, 60, w60, 3, 90, "Взрывное ускорение грифа"))

        // 4. 75% x 2
        val w75 = roundToStep((targetKg * 0.75).coerceAtLeast(w60 + 2.5), 2.5)
        if (w75 > w60) steps.add(createStep(steps.size + 1, 75, w75, 2, 90, "Жёсткий упор и натяг"))

        // 5. 85% x 1
        val w85 = roundToStep((targetKg * 0.85).coerceAtLeast(w75 + 2.5), 2.5)
        if (w85 > w75 && w85 < targetKg) steps.add(createStep(steps.size + 1, 85, w85, 1, 120, "Одиночный потентяционный повтор"))

        // 6. 92.5% x 1 (if target is heavy enough)
        if (targetKg >= 100.0) {
            val w92 = roundToStep((targetKg * 0.925).coerceAtLeast(w85 + 2.5), 2.5)
            if (w92 > w85 && w92 < targetKg - 2.5) {
                steps.add(createStep(steps.size + 1, 92, w92, 1, 120, "Психологическая готовность к рабочему весу"))
            }
        }

        return steps
    }

    private fun generateWendler(targetKg: Double, barKg: Double): List<WarmupSetStep> {
        val steps = mutableListOf<WarmupSetStep>()
        val w40 = roundToStep((targetKg * 0.40).coerceAtLeast(barKg), 2.5)
        val w50 = roundToStep((targetKg * 0.50).coerceAtLeast(w40 + 2.5), 2.5)
        val w60 = roundToStep((targetKg * 0.60).coerceAtLeast(w50 + 2.5), 2.5)

        steps.add(createStep(1, 40, w40, 5, 60, "Сет 1: 40% от тренировочного максимума"))
        steps.add(createStep(2, 50, w50, 5, 60, "Сет 2: 50% от тренировочного максимума"))
        steps.add(createStep(3, 60, w60, 3, 90, "Сет 3: 60% от тренировочного максимума"))
        return steps
    }

    private fun generateQuick(targetKg: Double, barKg: Double): List<WarmupSetStep> {
        val steps = mutableListOf<WarmupSetStep>()
        val w50 = roundToStep((targetKg * 0.50).coerceAtLeast(barKg), 2.5)
        val w75 = roundToStep((targetKg * 0.75).coerceAtLeast(w50 + 2.5), 2.5)

        steps.add(createStep(1, 50, w50, 8, 60, "Активация (50%)"))
        if (w75 < targetKg) {
            steps.add(createStep(2, 75, w75, 3, 75, "Калибровка (75%)"))
        }
        return steps
    }

    private fun createStep(
        index: Int,
        pct: Int,
        weightKg: Double,
        reps: Int,
        restSec: Int,
        note: String
    ): WarmupSetStep {
        val plateRes = PlateCalculatorEngine.calculatePlates(weightKg)
        val breakdown = PlateCalculatorEngine.formatPlateBreakdown(plateRes.platesPerSide)
        return WarmupSetStep(
            stepIndex = index,
            percentageOfTarget = pct,
            weightKg = weightKg,
            reps = reps,
            suggestedRestSeconds = restSec,
            note = note,
            plateBreakdown = breakdown
        )
    }

    private fun roundToStep(value: Double, step: Double): Double {
        return (value / step).roundToInt() * step
    }
}

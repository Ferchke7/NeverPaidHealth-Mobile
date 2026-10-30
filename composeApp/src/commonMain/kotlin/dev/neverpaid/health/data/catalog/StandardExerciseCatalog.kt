package dev.neverpaid.health.data.catalog

import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.RoutineExercise

object StandardExerciseCatalog {

    val standardExercises: List<Exercise> = listOf(
        Exercise(
            id = "10000000-0000-0000-0000-000000000001",
            name = "Barbell Bench Press",
            primaryMuscleGroup = "chest",
            secondaryMuscleGroups = listOf("triceps", "shoulders"),
            equipment = "barbell",
            instructionsList = listOf(
                "Лягте на горизонтальную скамью, возьмите гриф чуть шире плеч.",
                "Опустите штангу к середине груди под контролем, затем мощно выжмите вверх."
            )
        ),
        Exercise(
            id = "10000000-0000-0000-0000-000000000002",
            name = "Incline Barbell Bench Press",
            primaryMuscleGroup = "chest",
            secondaryMuscleGroups = listOf("shoulders", "triceps"),
            equipment = "barbell",
            instructionsList = listOf(
                "Установите наклон скамьи 30 градусов.",
                "Опустите гриф к верхней части груди и выжмите вверх."
            )
        ),
        Exercise(
            id = "20000000-0000-0000-0000-000000000001",
            name = "Deadlift",
            primaryMuscleGroup = "back",
            secondaryMuscleGroups = listOf("hamstrings", "core", "quads"),
            equipment = "barbell",
            instructionsList = listOf(
                "Поставьте стопы на ширине таза, гриф над серединой стопы.",
                "Напрягите кор и широчайшие, вытяните штангу вдоль голеней до полного выпрямления."
            )
        ),
        Exercise(
            id = "20000000-0000-0000-0000-000000000002",
            name = "Barbell Bent Over Row",
            primaryMuscleGroup = "back",
            secondaryMuscleGroups = listOf("biceps"),
            equipment = "barbell",
            instructionsList = listOf(
                "Наклоните корпус под углом 45 градусов с прямой спиной.",
                "Тяните штангу к низу живота, сводя лопатки в верхней точке."
            )
        ),
        Exercise(
            id = "20000000-0000-0000-0000-000000000003",
            name = "Pull Up",
            primaryMuscleGroup = "back",
            secondaryMuscleGroups = listOf("biceps"),
            equipment = "bodyweight",
            instructionsList = listOf(
                "Возьмитесь за перекладину хватом чуть шире плеч.",
                "Подтянитесь грудью к перекладине, опуская лопатки вниз."
            )
        ),
        Exercise(
            id = "30000000-0000-0000-0000-000000000001",
            name = "Barbell Squat",
            primaryMuscleGroup = "quads",
            secondaryMuscleGroups = listOf("hamstrings", "core", "glutes"),
            equipment = "barbell",
            instructionsList = listOf(
                "Расположите штангу на трапециях, напрягите пресс.",
                "Присядьте до параллели бедра с полом или ниже и мощно встаньте."
            )
        ),
        Exercise(
            id = "30000000-0000-0000-0000-000000000003",
            name = "Leg Press",
            primaryMuscleGroup = "quads",
            secondaryMuscleGroups = listOf("glutes"),
            equipment = "machine",
            instructionsList = listOf(
                "Поставьте стопы на ширине плеч на платформу.",
                "Опустите платформу до угла 90 градусов в коленях, не отрывая таз."
            )
        ),
        Exercise(
            id = "40000000-0000-0000-0000-000000000001",
            name = "Romanian Deadlift",
            primaryMuscleGroup = "hamstrings",
            secondaryMuscleGroups = listOf("back", "glutes"),
            equipment = "barbell",
            instructionsList = listOf(
                "Слегка согните колени и отводите таз назад, ведя гриф вдоль бёдер.",
                "Почувствуйте растяжение бицепса бедра и вернитесь в исходное положение."
            )
        ),
        Exercise(
            id = "50000000-0000-0000-0000-000000000001",
            name = "Overhead Press",
            primaryMuscleGroup = "shoulders",
            secondaryMuscleGroups = listOf("triceps", "core"),
            equipment = "barbell",
            instructionsList = listOf(
                "Стоя ровно, выжмите штангу с груди строго вертикально над головой.",
                "Полностью зафиксируйте локти в верхней точке."
            )
        ),
        Exercise(
            id = "50000000-0000-0000-0000-000000000003",
            name = "Dumbbell Lateral Raise",
            primaryMuscleGroup = "shoulders",
            secondaryMuscleGroups = emptyList(),
            equipment = "dumbbell",
            instructionsList = listOf(
                "Поднимайте гантели через стороны до уровня плеч, ведя движение локтями."
            )
        ),
        Exercise(
            id = "60000000-0000-0000-0000-000000000001",
            name = "Barbell Bicep Curl",
            primaryMuscleGroup = "biceps",
            secondaryMuscleGroups = emptyList(),
            equipment = "barbell",
            instructionsList = listOf(
                "Прижмите локти к корпусу и поднимите штангу на бицепс без раскачки."
            )
        ),
        Exercise(
            id = "70000000-0000-0000-0000-000000000001",
            name = "Tricep Rope Pushdown",
            primaryMuscleGroup = "triceps",
            secondaryMuscleGroups = emptyList(),
            equipment = "cable",
            instructionsList = listOf(
                "Разгибайте руки с канатной рукоятью вниз до полного сокращения трицепса."
            )
        )
    )

    // Matches PREBUILT_ROUTINES from frontend/src/pages/workouts/ui/WorkoutsPage.tsx
    val prebuiltRoutines: List<Routine> = listOf(
        Routine(
            id = "prebuilt-push",
            name = "Жим (Грудь, Плечи, Трицепс)",
            notes = "Классическая жимовая тренировка для развития грудных, дельт и трицепса.",
            isPrebuilt = true,
            category = "Push/Pull/Legs",
            exercises = listOf(
                RoutineExercise("10000000-0000-0000-0000-000000000001", "Barbell Bench Press", 0, 4, 6, 8),
                RoutineExercise("10000000-0000-0000-0000-000000000002", "Incline Barbell Bench Press", 1, 3, 8, 10),
                RoutineExercise("50000000-0000-0000-0000-000000000001", "Overhead Press", 2, 3, 8, 10),
                RoutineExercise("50000000-0000-0000-0000-000000000003", "Dumbbell Lateral Raise", 3, 4, 12, 15),
                RoutineExercise("70000000-0000-0000-0000-000000000001", "Tricep Rope Pushdown", 4, 3, 12, 15)
            )
        ),
        Routine(
            id = "prebuilt-pull",
            name = "Тяга (Спина, Бицепс, Задняя дельта)",
            notes = "Тяговая тренировка на ширину и толщину спины + прокачка бицепса.",
            isPrebuilt = true,
            category = "Push/Pull/Legs",
            exercises = listOf(
                RoutineExercise("20000000-0000-0000-0000-000000000001", "Deadlift", 0, 3, 5, 5),
                RoutineExercise("20000000-0000-0000-0000-000000000003", "Pull Up", 1, 4, 8, 12),
                RoutineExercise("20000000-0000-0000-0000-000000000002", "Barbell Bent Over Row", 2, 3, 8, 10),
                RoutineExercise("60000000-0000-0000-0000-000000000001", "Barbell Bicep Curl", 3, 3, 10, 12)
            )
        ),
        Routine(
            id = "prebuilt-legs",
            name = "Ноги (Квадрицепс, Бицепс бедра, Икры)",
            notes = "Полноценный день ног с акцентом на базовый присед и румынскую тягу.",
            isPrebuilt = true,
            category = "Push/Pull/Legs",
            exercises = listOf(
                RoutineExercise("30000000-0000-0000-0000-000000000001", "Barbell Squat", 0, 4, 6, 8),
                RoutineExercise("40000000-0000-0000-0000-000000000001", "Romanian Deadlift", 1, 3, 8, 10),
                RoutineExercise("30000000-0000-0000-0000-000000000003", "Leg Press", 2, 3, 10, 12)
            )
        ),
        Routine(
            id = "prebuilt-upper",
            name = "Верх тела (Сила и Масса)",
            notes = "Интенсивная проработка всего плечевого пояса, груди и спины за одну сессию.",
            isPrebuilt = true,
            category = "Upper/Lower",
            exercises = listOf(
                RoutineExercise("10000000-0000-0000-0000-000000000001", "Barbell Bench Press", 0, 4, 6, 8),
                RoutineExercise("20000000-0000-0000-0000-000000000002", "Barbell Bent Over Row", 1, 4, 8, 10),
                RoutineExercise("50000000-0000-0000-0000-000000000001", "Overhead Press", 2, 3, 8, 10),
                RoutineExercise("20000000-0000-0000-0000-000000000003", "Pull Up", 3, 3, 8, 12),
                RoutineExercise("60000000-0000-0000-0000-000000000001", "Barbell Bicep Curl", 4, 3, 10, 12)
            )
        ),
        Routine(
            id = "prebuilt-fullbody",
            name = "Фулбоди (Базовая тренировка)",
            notes = "Тяжёлые базовые многосуставные движения на всё тело в одной тренировке.",
            isPrebuilt = true,
            category = "Full Body",
            exercises = listOf(
                RoutineExercise("30000000-0000-0000-0000-000000000001", "Barbell Squat", 0, 3, 5, 5),
                RoutineExercise("10000000-0000-0000-0000-000000000001", "Barbell Bench Press", 1, 3, 5, 5),
                RoutineExercise("20000000-0000-0000-0000-000000000002", "Barbell Bent Over Row", 2, 3, 8, 8),
                RoutineExercise("50000000-0000-0000-0000-000000000001", "Overhead Press", 3, 3, 8, 8)
            )
        )
    )
}

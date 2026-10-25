package dev.neverpaid.health.data.catalog

import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.Routine

object StandardExerciseCatalog {

    val standardExercises: List<Exercise> = listOf(
        // CHEST
        Exercise(
            id = "10000000-0000-0000-0000-000000000001",
            name = "Barbell Bench Press",
            primaryMuscleGroup = "chest",
            secondaryMuscleGroups = listOf("triceps", "shoulders"),
            equipment = "barbell",
            instructions = "Lie flat on the bench, grip barbell slightly wider than shoulder-width. Lower bar to mid-chest with elbows tucked at 45 degrees, then press up forcefully."
        ),
        Exercise(
            id = "10000000-0000-0000-0000-000000000002",
            name = "Incline Barbell Bench Press",
            primaryMuscleGroup = "chest",
            secondaryMuscleGroups = listOf("shoulders", "triceps"),
            equipment = "barbell",
            instructions = "Set bench at 30 degrees incline. Lower barbell controlled to upper chest, drive up locking out shoulders."
        ),
        Exercise(
            id = "10000000-0000-0000-0000-000000000004",
            name = "Dumbbell Bench Press",
            primaryMuscleGroup = "chest",
            secondaryMuscleGroups = listOf("triceps", "shoulders"),
            equipment = "dumbbell",
            instructions = "Lower dumbbells with neutral or slight angle grip until deep chest stretch, press upwards converging at the top."
        ),
        Exercise(
            id = "10000000-0000-0000-0000-000000000005",
            name = "Incline Dumbbell Press",
            primaryMuscleGroup = "chest",
            secondaryMuscleGroups = listOf("shoulders", "triceps"),
            equipment = "dumbbell",
            instructions = "Incline at 30-45 degrees, press dumbbells overhead maintaining core engagement."
        ),
        Exercise(
            id = "10000000-0000-0000-0000-000000000007",
            name = "Cable Crossover",
            primaryMuscleGroup = "chest",
            secondaryMuscleGroups = emptyList(),
            equipment = "cable",
            instructions = "Set pulleys high, bring hands down and forward in a hugging arc with slight bend in elbows."
        ),
        Exercise(
            id = "10000000-0000-0000-0000-000000000008",
            name = "Chest Dip",
            primaryMuscleGroup = "chest",
            secondaryMuscleGroups = listOf("triceps", "shoulders"),
            equipment = "bodyweight",
            instructions = "Lean torso forward 30 degrees, flare elbows slightly, lower until 90 degree arm angle, press up."
        ),

        // BACK
        Exercise(
            id = "20000000-0000-0000-0000-000000000001",
            name = "Deadlift",
            primaryMuscleGroup = "back",
            secondaryMuscleGroups = listOf("hamstrings", "core", "quads"),
            equipment = "barbell",
            instructions = "Stand hip-width, grip bar outside legs. Hinge at hips, brace core, pull bar close to shins and lock hips out at top."
        ),
        Exercise(
            id = "20000000-0000-0000-0000-000000000002",
            name = "Barbell Bent Over Row",
            primaryMuscleGroup = "back",
            secondaryMuscleGroups = listOf("biceps"),
            equipment = "barbell",
            instructions = "Hinge forward 45 degrees, pull barbell to sternum driving elbows back, squeeze scapulae."
        ),
        Exercise(
            id = "20000000-0000-0000-0000-000000000003",
            name = "Pull Up",
            primaryMuscleGroup = "back",
            secondaryMuscleGroups = listOf("biceps"),
            equipment = "bodyweight",
            instructions = "Pronated grip wider than shoulders, pull chin above bar depressing shoulder blades down."
        ),
        Exercise(
            id = "20000000-0000-0000-0000-000000000005",
            name = "Lat Pulldown",
            primaryMuscleGroup = "back",
            secondaryMuscleGroups = listOf("biceps"),
            equipment = "cable",
            instructions = "Pull bar smoothly to clavicle, arch upper spine slightly, control the eccentric stretch."
        ),
        Exercise(
            id = "20000000-0000-0000-0000-000000000006",
            name = "Seated Cable Row",
            primaryMuscleGroup = "back",
            secondaryMuscleGroups = listOf("biceps"),
            equipment = "cable",
            instructions = "Keep upright posture, pull handle to belly button while driving elbows backwards."
        ),
        Exercise(
            id = "20000000-0000-0000-0000-000000000007",
            name = "One-Arm Dumbbell Row",
            primaryMuscleGroup = "back",
            secondaryMuscleGroups = listOf("biceps"),
            equipment = "dumbbell",
            instructions = "Support knee and hand on flat bench, pull dumbbell to hip pocket keeping elbow tucked."
        ),

        // LEGS (QUADS & GLUTES)
        Exercise(
            id = "30000000-0000-0000-0000-000000000001",
            name = "Barbell Squat",
            primaryMuscleGroup = "quads",
            secondaryMuscleGroups = listOf("hamstrings", "core", "glutes"),
            equipment = "barbell",
            instructions = "Bar rested on upper traps, brace core, break at hips and knees simultaneously to parallel depth or below."
        ),
        Exercise(
            id = "30000000-0000-0000-0000-000000000002",
            name = "Front Squat",
            primaryMuscleGroup = "quads",
            secondaryMuscleGroups = listOf("core"),
            equipment = "barbell",
            instructions = "Clean grip or cross-arm with bar on clavicles, maintain vertical torso throughout squatting motion."
        ),
        Exercise(
            id = "30000000-0000-0000-0000-000000000003",
            name = "Leg Press",
            primaryMuscleGroup = "quads",
            secondaryMuscleGroups = listOf("glutes"),
            equipment = "machine",
            instructions = "Feet shoulder-width on sled, lower knees to 90 degrees without rounding lower back, press up."
        ),
        Exercise(
            id = "30000000-0000-0000-0000-000000000005",
            name = "Bulgarian Split Squat",
            primaryMuscleGroup = "quads",
            secondaryMuscleGroups = listOf("glutes"),
            equipment = "dumbbell",
            instructions = "Rear foot elevated on bench, descend front thigh parallel to ground, drive through front heel."
        ),

        // HAMSTRINGS & POSTERIOR CHAIN
        Exercise(
            id = "40000000-0000-0000-0000-000000000001",
            name = "Romanian Deadlift",
            primaryMuscleGroup = "hamstrings",
            secondaryMuscleGroups = listOf("back", "glutes"),
            equipment = "barbell",
            instructions = "Slight knee bend, hinge hips backwards keeping bar glued to legs until deep hamstring tension."
        ),
        Exercise(
            id = "40000000-0000-0000-0000-000000000003",
            name = "Lying Leg Curl",
            primaryMuscleGroup = "hamstrings",
            secondaryMuscleGroups = emptyList(),
            equipment = "machine",
            instructions = "Pad behind ankles, curl heels towards glutes smoothly without hyperextending lower spine."
        ),
        Exercise(
            id = "40000000-0000-0000-0000-000000000005",
            name = "Barbell Hip Thrust",
            primaryMuscleGroup = "glutes",
            secondaryMuscleGroups = listOf("hamstrings"),
            equipment = "barbell",
            instructions = "Upper back against bench, barbell cushioned on hips. Drive through heels to full hip extension."
        ),

        // SHOULDERS
        Exercise(
            id = "50000000-0000-0000-0000-000000000001",
            name = "Overhead Press",
            primaryMuscleGroup = "shoulders",
            secondaryMuscleGroups = listOf("triceps", "core"),
            equipment = "barbell",
            instructions = "Standing, press barbell straight overhead, push head through window at top lockout."
        ),
        Exercise(
            id = "50000000-0000-0000-0000-000000000002",
            name = "Seated Dumbbell Shoulder Press",
            primaryMuscleGroup = "shoulders",
            secondaryMuscleGroups = listOf("triceps"),
            equipment = "dumbbell",
            instructions = "Seated on 90 degree upright bench, press dumbbells in a slight arch to touch at top."
        ),
        Exercise(
            id = "50000000-0000-0000-0000-000000000003",
            name = "Dumbbell Lateral Raise",
            primaryMuscleGroup = "shoulders",
            secondaryMuscleGroups = emptyList(),
            equipment = "dumbbell",
            instructions = "Raise dumbbells out to side up to shoulder height, leading with elbows and pinkies."
        ),
        Exercise(
            id = "20000000-0000-0000-0000-000000000009",
            name = "Face Pull",
            primaryMuscleGroup = "shoulders",
            secondaryMuscleGroups = listOf("back"),
            equipment = "cable",
            instructions = "Rope attachment at eye level, pull to forehead flaring elbows high and externally rotating."
        ),

        // ARMS (BICEPS & TRICEPS)
        Exercise(
            id = "60000000-0000-0000-0000-000000000001",
            name = "Barbell Bicep Curl",
            primaryMuscleGroup = "biceps",
            secondaryMuscleGroups = emptyList(),
            equipment = "barbell",
            instructions = "Elbows pinned to sides, curl barbell in smooth arc contracting biceps fully."
        ),
        Exercise(
            id = "60000000-0000-0000-0000-000000000002",
            name = "Incline Dumbbell Curl",
            primaryMuscleGroup = "biceps",
            secondaryMuscleGroups = emptyList(),
            equipment = "dumbbell",
            instructions = "Seated incline bench, maximize long head stretch, curl with supinated palms."
        ),
        Exercise(
            id = "70000000-0000-0000-0000-000000000001",
            name = "Tricep Rope Pushdown",
            primaryMuscleGroup = "triceps",
            secondaryMuscleGroups = emptyList(),
            equipment = "cable",
            instructions = "Push rope down extending elbows fully and spreading rope tips apart at bottom lockout."
        ),
        Exercise(
            id = "70000000-0000-0000-0000-000000000002",
            name = "Skull Crusher (Lying Tricep Ext)",
            primaryMuscleGroup = "triceps",
            secondaryMuscleGroups = emptyList(),
            equipment = "barbell",
            instructions = "Lie on flat bench, lower EZ-bar to forehead, press up isolating triceps."
        ),

        // CORE
        Exercise(
            id = "80000000-0000-0000-0000-000000000001",
            name = "Hanging Knee / Leg Raise",
            primaryMuscleGroup = "core",
            secondaryMuscleGroups = emptyList(),
            equipment = "bodyweight",
            instructions = "Hang from pull-up bar, tuck pelvis and raise knees/toes to 90 degrees or chest."
        ),
        Exercise(
            id = "80000000-0000-0000-0000-000000000002",
            name = "Cable Woodchopper",
            primaryMuscleGroup = "core",
            secondaryMuscleGroups = emptyList(),
            equipment = "cable",
            instructions = "Diagonal rotational drive across body engaging obliques, pivoting on back foot."
        )
    )

    val prebuiltRoutines: List<Routine> = listOf(
        Routine(
            id = "routine-push-a",
            title = "Push Day (Hypertrophy)",
            description = "Chest, Shoulders & Triceps focus with high mechanical tension",
            exercises = listOf(
                standardExercises.first { it.name == "Barbell Bench Press" },
                standardExercises.first { it.name == "Incline Dumbbell Press" },
                standardExercises.first { it.name == "Dumbbell Lateral Raise" },
                standardExercises.first { it.name == "Tricep Rope Pushdown" }
            )
        ),
        Routine(
            id = "routine-pull-a",
            title = "Pull Day (Hypertrophy)",
            description = "Back width, thickness, rear delts and biceps volume",
            exercises = listOf(
                standardExercises.first { it.name == "Deadlift" },
                standardExercises.first { it.name == "Pull Up" },
                standardExercises.first { it.name == "Barbell Bent Over Row" },
                standardExercises.first { it.name == "Face Pull" },
                standardExercises.first { it.name == "Barbell Bicep Curl" }
            )
        ),
        Routine(
            id = "routine-legs-a",
            title = "Leg Day (Quad & Chain)",
            description = "Squat variations, Bulgarian split squats and posterior chain",
            exercises = listOf(
                standardExercises.first { it.name == "Barbell Squat" },
                standardExercises.first { it.name == "Romanian Deadlift" },
                standardExercises.first { it.name == "Leg Press" },
                standardExercises.first { it.name == "Lying Leg Curl" }
            )
        ),
        Routine(
            id = "routine-full-body",
            title = "Full Body Power",
            description = "Heavy compound movements for maximum athletic strength",
            exercises = listOf(
                standardExercises.first { it.name == "Barbell Squat" },
                standardExercises.first { it.name == "Barbell Bench Press" },
                standardExercises.first { it.name == "Barbell Bent Over Row" },
                standardExercises.first { it.name == "Overhead Press" }
            )
        )
    )
}

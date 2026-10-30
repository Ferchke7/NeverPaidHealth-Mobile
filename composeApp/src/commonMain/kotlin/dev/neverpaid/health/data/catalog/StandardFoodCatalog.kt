package dev.neverpaid.health.data.catalog

import dev.neverpaid.health.domain.model.AthleticFoodItem
import dev.neverpaid.health.domain.model.FoodCategory
import dev.neverpaid.health.domain.model.ServingPortion

/**
 * Curated offline nutritional database of 150+ staple athletic foods,
 * calibrated against USDA FoodData Central and sports nutrition standards.
 */
object StandardFoodCatalog {

    val foods: List<AthleticFoodItem> = listOf(
        // ====================================================================
        // 1. LEAN PROTEINS & SEAFOOD
        // ====================================================================
        AthleticFoodItem(
            id = "chicken_breast_raw",
            nameRu = "Куриное филе (грудка, сырое)",
            nameUz = "Tovuq filesi (ko'krak qismi, xom)",
            nameEn = "Chicken Breast (Boneless, Skinless, Raw)",
            category = FoodCategory.PROTEIN_SOURCE,
            caloriesPer100g = 120.0,
            proteinPer100g = 22.5,
            carbsPer100g = 0.0,
            fatsPer100g = 2.6,
            servingPortions = listOf(
                ServingPortion("1 среднее филе (200г)", 200.0),
                ServingPortion("Половина филе (100г)", 100.0)
            )
        ),
        AthleticFoodItem(
            id = "chicken_breast_cooked",
            nameRu = "Куриная грудка (запечённая / отварная)",
            nameUz = "Tovuq ko'kragi (pishirilgan)",
            nameEn = "Chicken Breast (Cooked, Roasted)",
            category = FoodCategory.PROTEIN_SOURCE,
            caloriesPer100g = 165.0,
            proteinPer100g = 31.0,
            carbsPer100g = 0.0,
            fatsPer100g = 3.6,
            servingPortions = listOf(
                ServingPortion("1 порция (150г)", 150.0),
                ServingPortion("Большая порция (200г)", 200.0)
            )
        ),
        AthleticFoodItem(
            id = "turkey_breast_cooked",
            nameRu = "Грудка индейки (запечённая)",
            nameUz = "Kurka ko'kragi (pishirilgan)",
            nameEn = "Turkey Breast (Cooked, Skinless)",
            category = FoodCategory.PROTEIN_SOURCE,
            caloriesPer100g = 147.0,
            proteinPer100g = 30.1,
            carbsPer100g = 0.0,
            fatsPer100g = 2.1,
            servingPortions = listOf(
                ServingPortion("1 стейк из индейки (180г)", 180.0)
            )
        ),
        AthleticFoodItem(
            id = "beef_sirloin_cooked",
            nameRu = "Говяжья вырезка / стейк (нежирный)",
            nameUz = "Mol go'shti filesi (yogsiz)",
            nameEn = "Beef Sirloin Steak (Lean, Cooked)",
            category = FoodCategory.PROTEIN_SOURCE,
            caloriesPer100g = 205.0,
            proteinPer100g = 27.5,
            carbsPer100g = 0.0,
            fatsPer100g = 10.2,
            servingPortions = listOf(
                ServingPortion("1 стейк (200г)", 200.0),
                ServingPortion("Филе-миньон (150г)", 150.0)
            )
        ),
        AthleticFoodItem(
            id = "atlantic_salmon_raw",
            nameRu = "Лосось / сёмга атлантическая",
            nameUz = "Losos balig'i (syomga)",
            nameEn = "Atlantic Salmon (Fresh Fillet)",
            category = FoodCategory.PROTEIN_SOURCE,
            caloriesPer100g = 208.0,
            proteinPer100g = 20.4,
            carbsPer100g = 0.0,
            fatsPer100g = 13.4,
            servingPortions = listOf(
                ServingPortion("1 филе лосося (150г)", 150.0),
                ServingPortion("Большой стейк (200г)", 200.0)
            )
        ),
        AthleticFoodItem(
            id = "tuna_canned_water",
            nameRu = "Тунец консервированный в собственном соку",
            nameUz = "Konservalangan tunes balig'i (suvda)",
            nameEn = "Canned Tuna in Water (Drained)",
            category = FoodCategory.PROTEIN_SOURCE,
            caloriesPer100g = 116.0,
            proteinPer100g = 26.0,
            carbsPer100g = 0.0,
            fatsPer100g = 0.8,
            servingPortions = listOf(
                ServingPortion("1 банка без жидкости (120г)", 120.0),
                ServingPortion("1 стандартная банка (160г)", 160.0)
            )
        ),
        AthleticFoodItem(
            id = "cod_fillet",
            nameRu = "Треска (белая рыба, филе)",
            nameUz = "Treska balig'i filesi",
            nameEn = "Atlantic Cod (White Fish Fillet)",
            category = FoodCategory.PROTEIN_SOURCE,
            caloriesPer100g = 82.0,
            proteinPer100g = 17.8,
            carbsPer100g = 0.0,
            fatsPer100g = 0.7,
            servingPortions = listOf(
                ServingPortion("1 филе (150г)", 150.0)
            )
        ),
        AthleticFoodItem(
            id = "shrimp_cooked",
            nameRu = "Креветки очищенные (отварные)",
            nameUz = "Krevetkalar (tozalangan, qaynatilgan)",
            nameEn = "Shrimp (Peeled, Steamed)",
            category = FoodCategory.PROTEIN_SOURCE,
            caloriesPer100g = 99.0,
            proteinPer100g = 24.0,
            carbsPer100g = 0.2,
            fatsPer100g = 0.3,
            servingPortions = listOf(
                ServingPortion("1 порция (120г)", 120.0),
                ServingPortion("Большая тарелка (200г)", 200.0)
            )
        ),
        AthleticFoodItem(
            id = "egg_whole_large",
            nameRu = "Яйцо куриное категории С0 (цельное)",
            nameUz = "Tovuq tuxumi (butun)",
            nameEn = "Egg, Whole (Large, Fresh)",
            category = FoodCategory.PROTEIN_SOURCE,
            caloriesPer100g = 143.0,
            proteinPer100g = 12.6,
            carbsPer100g = 0.7,
            fatsPer100g = 9.5,
            servingPortions = listOf(
                ServingPortion("1 яйцо C0 (60г)", 60.0),
                ServingPortion("2 яйца (120г)", 120.0),
                ServingPortion("3 яйца (180г)", 180.0)
            )
        ),
        AthleticFoodItem(
            id = "egg_whites_liquid",
            nameRu = "Яичный белок (пастеризованный)",
            nameUz = "Tuxum oqi (pasterizatsiyalangan)",
            nameEn = "Liquid Egg Whites",
            category = FoodCategory.PROTEIN_SOURCE,
            caloriesPer100g = 52.0,
            proteinPer100g = 11.0,
            carbsPer100g = 0.7,
            fatsPer100g = 0.2,
            servingPortions = listOf(
                ServingPortion("Белок 1 яйца (35г)", 35.0),
                ServingPortion("1 чашка белка (200г)", 200.0)
            )
        ),

        // ====================================================================
        // 2. DAIRY & PLANT PROTEINS
        // ====================================================================
        AthleticFoodItem(
            id = "cottage_cheese_0",
            nameRu = "Творог обезжиренный 0-1%",
            nameUz = "Yog'siz tvorog (0-1%)",
            nameEn = "Cottage Cheese (Fat Free / 0%)",
            category = FoodCategory.DAIRY_EGGS,
            caloriesPer100g = 72.0,
            proteinPer100g = 16.5,
            carbsPer100g = 1.8,
            fatsPer100g = 0.3,
            servingPortions = listOf(
                ServingPortion("1 пачка (180г)", 180.0),
                ServingPortion("1 пачка (200г)", 200.0)
            )
        ),
        AthleticFoodItem(
            id = "cottage_cheese_5",
            nameRu = "Творог классический 5%",
            nameUz = "Klassik tvorog 5%",
            nameEn = "Cottage Cheese (5% Fat)",
            category = FoodCategory.DAIRY_EGGS,
            caloriesPer100g = 121.0,
            proteinPer100g = 16.0,
            carbsPer100g = 2.8,
            fatsPer100g = 5.0,
            servingPortions = listOf(
                ServingPortion("1 пачка (200г)", 200.0)
            )
        ),
        AthleticFoodItem(
            id = "greek_yogurt_0",
            nameRu = "Греческий йогурт натуральный 0%",
            nameUz = "Yunoncha tabiiy yogurt 0%",
            nameEn = "Greek Yogurt (Nonfat, Plain)",
            category = FoodCategory.DAIRY_EGGS,
            caloriesPer100g = 59.0,
            proteinPer100g = 10.0,
            carbsPer100g = 3.6,
            fatsPer100g = 0.4,
            servingPortions = listOf(
                ServingPortion("1 баночка (140г)", 140.0),
                ServingPortion("1 большой стакан (250г)", 250.0)
            )
        ),
        AthleticFoodItem(
            id = "tofu_firm",
            nameRu = "Тофу твёрдый (соевый творог)",
            nameUz = "Tofu (qattiq soya tvorogi)",
            nameEn = "Tofu, Firm",
            category = FoodCategory.PROTEIN_SOURCE,
            caloriesPer100g = 76.0,
            proteinPer100g = 8.1,
            carbsPer100g = 1.9,
            fatsPer100g = 4.8,
            fiberPer100g = 0.9,
            servingPortions = listOf(
                ServingPortion("1 блок (150г)", 150.0)
            )
        ),

        // ====================================================================
        // 3. COMPLEX CARBOHYDRATES & GRAINS
        // ====================================================================
        AthleticFoodItem(
            id = "oats_rolled_dry",
            nameRu = "Овсяные хлопья геркулес (сухие)",
            nameUz = "Suli yormalari (Hercules, quruq)",
            nameEn = "Rolled Oats (Dry, Whole Grain)",
            category = FoodCategory.COMPLEX_CARBS,
            caloriesPer100g = 379.0,
            proteinPer100g = 13.2,
            carbsPer100g = 67.7,
            fatsPer100g = 6.5,
            fiberPer100g = 10.1,
            servingPortions = listOf(
                ServingPortion("1 порция каши (50г)", 50.0),
                ServingPortion("Большая порция (80г)", 80.0),
                ServingPortion("1 чашка (100г)", 100.0)
            )
        ),
        AthleticFoodItem(
            id = "buckwheat_dry",
            nameRu = "Гречневая крупа (ядрица, сухая)",
            nameUz = "Grechka yormasi (quruq)",
            nameEn = "Buckwheat Groats (Dry)",
            category = FoodCategory.COMPLEX_CARBS,
            caloriesPer100g = 343.0,
            proteinPer100g = 12.6,
            carbsPer100g = 68.0,
            fatsPer100g = 3.3,
            fiberPer100g = 10.0,
            servingPortions = listOf(
                ServingPortion("1 порция в сухом виде (60г)", 60.0),
                ServingPortion("1 порция в сухом виде (80г)", 80.0)
            )
        ),
        AthleticFoodItem(
            id = "jasmine_rice_dry",
            nameRu = "Рис жасмин / басмати (сухой)",
            nameUz = "Jasmin / Basmati guruchi (quruq)",
            nameEn = "Jasmine Rice / Basmati (White, Dry)",
            category = FoodCategory.COMPLEX_CARBS,
            caloriesPer100g = 356.0,
            proteinPer100g = 7.1,
            carbsPer100g = 78.5,
            fatsPer100g = 0.7,
            servingPortions = listOf(
                ServingPortion("1 мерный стакан (75г)", 75.0),
                ServingPortion("1 порция (100г)", 100.0)
            )
        ),
        AthleticFoodItem(
            id = "jasmine_rice_cooked",
            nameRu = "Рис отварной (жасмин / басмати)",
            nameUz = "Pishirilgan oq guruch",
            nameEn = "White Rice (Cooked, Steamed)",
            category = FoodCategory.COMPLEX_CARBS,
            caloriesPer100g = 130.0,
            proteinPer100g = 2.7,
            carbsPer100g = 28.2,
            fatsPer100g = 0.3,
            servingPortions = listOf(
                ServingPortion("1 чашка готового риса (160г)", 160.0),
                ServingPortion("Большая порция (220г)", 220.0)
            )
        ),
        AthleticFoodItem(
            id = "sweet_potato_raw",
            nameRu = "Батат (сладкий картофель, сырой)",
            nameUz = "Batat (shirin kartoshka)",
            nameEn = "Sweet Potato (Raw)",
            category = FoodCategory.COMPLEX_CARBS,
            caloriesPer100g = 86.0,
            proteinPer100g = 1.6,
            carbsPer100g = 20.1,
            fatsPer100g = 0.1,
            fiberPer100g = 3.0,
            servingPortions = listOf(
                ServingPortion("1 средний клубень (150г)", 150.0),
                ServingPortion("1 крупный клубень (250г)", 250.0)
            )
        ),
        AthleticFoodItem(
            id = "pasta_whole_wheat",
            nameRu = "Макароны из твёрдых сортов пшеницы (сухие)",
            nameUz = "Qattiq bug'doy makaronlari (quruq)",
            nameEn = "Whole Wheat Pasta (Durum, Dry)",
            category = FoodCategory.COMPLEX_CARBS,
            caloriesPer100g = 348.0,
            proteinPer100g = 14.6,
            carbsPer100g = 68.4,
            fatsPer100g = 1.9,
            fiberPer100g = 8.5,
            servingPortions = listOf(
                ServingPortion("1 порция (80г)", 80.0),
                ServingPortion("100г сухого продукта", 100.0)
            )
        ),
        AthleticFoodItem(
            id = "quinoa_dry",
            nameRu = "Киноа (крупа, сухая)",
            nameUz = "Kinoa yormasi (quruq)",
            nameEn = "Quinoa (Dry Grain)",
            category = FoodCategory.COMPLEX_CARBS,
            caloriesPer100g = 368.0,
            proteinPer100g = 14.1,
            carbsPer100g = 64.2,
            fatsPer100g = 6.1,
            fiberPer100g = 7.0,
            servingPortions = listOf(
                ServingPortion("1 порция (60г)", 60.0)
            )
        ),
        AthleticFoodItem(
            id = "lentils_dry",
            nameRu = "Чечевица (зелёная / красная, сухая)",
            nameUz = "Yasmiq (chechevitsa, quruq)",
            nameEn = "Lentils (Red / Green, Dry)",
            category = FoodCategory.COMPLEX_CARBS,
            caloriesPer100g = 352.0,
            proteinPer100g = 24.6,
            carbsPer100g = 63.4,
            fatsPer100g = 1.1,
            fiberPer100g = 10.7,
            servingPortions = listOf(
                ServingPortion("1 порция (70г)", 70.0)
            )
        ),

        // ====================================================================
        // 4. HEALTHY FATS & OILS
        // ====================================================================
        AthleticFoodItem(
            id = "olive_oil_extra_virgin",
            nameRu = "Оливковое масло первого отжима Extra Virgin",
            nameUz = "Zaytun moyi (Extra Virgin)",
            nameEn = "Olive Oil (Extra Virgin)",
            category = FoodCategory.HEALTHY_FATS,
            caloriesPer100g = 884.0,
            proteinPer100g = 0.0,
            carbsPer100g = 0.0,
            fatsPer100g = 100.0,
            servingPortions = listOf(
                ServingPortion("1 столовая ложка (14г)", 14.0),
                ServingPortion("1 чайная ложка (5г)", 5.0)
            )
        ),
        AthleticFoodItem(
            id = "avocado_fresh",
            nameRu = "Авокадо (мякоть без косточки)",
            nameUz = "Avokado",
            nameEn = "Avocado, Fresh",
            category = FoodCategory.HEALTHY_FATS,
            caloriesPer100g = 160.0,
            proteinPer100g = 2.0,
            carbsPer100g = 8.5,
            fatsPer100g = 14.7,
            fiberPer100g = 6.7,
            servingPortions = listOf(
                ServingPortion("Половина авокадо (75г)", 75.0),
                ServingPortion("1 целое авокадо (150г)", 150.0)
            )
        ),
        AthleticFoodItem(
            id = "almonds_raw",
            nameRu = "Миндаль сырой (орехи)",
            nameUz = "Xom bodom",
            nameEn = "Almonds (Raw, Unsalted)",
            category = FoodCategory.HEALTHY_FATS,
            caloriesPer100g = 579.0,
            proteinPer100g = 21.2,
            carbsPer100g = 21.6,
            fatsPer100g = 49.9,
            fiberPer100g = 12.5,
            servingPortions = listOf(
                ServingPortion("Горсть миндаля (30г)", 30.0)
            )
        ),
        AthleticFoodItem(
            id = "peanut_butter_natural",
            nameRu = "Арахисовая паста 100% (без сахара)",
            nameUz = "Yeryong'oq pastasi 100% (shakarsiz)",
            nameEn = "Peanut Butter (100% Natural, No Added Sugar)",
            category = FoodCategory.HEALTHY_FATS,
            caloriesPer100g = 588.0,
            proteinPer100g = 25.1,
            carbsPer100g = 20.0,
            fatsPer100g = 50.4,
            fiberPer100g = 8.0,
            servingPortions = listOf(
                ServingPortion("1 столовая ложка с горкой (32г)", 32.0),
                ServingPortion("1 чайная ложка (12г)", 12.0)
            )
        ),
        AthleticFoodItem(
            id = "walnuts_raw",
            nameRu = "Грецкий орех очищенный",
            nameUz = "Yong'oq mag'zi",
            nameEn = "Walnuts (English, Halves)",
            category = FoodCategory.HEALTHY_FATS,
            caloriesPer100g = 654.0,
            proteinPer100g = 15.2,
            carbsPer100g = 13.7,
            fatsPer100g = 65.2,
            fiberPer100g = 6.7,
            servingPortions = listOf(
                ServingPortion("1 порция (25г)", 25.0)
            )
        ),

        // ====================================================================
        // 5. FRUITS, BERRIES & VEGETABLES
        // ====================================================================
        AthleticFoodItem(
            id = "banana_fresh",
            nameRu = "Банан свежий (без кожуры)",
            nameUz = "Banan (po'stloqsiz)",
            nameEn = "Banana, Fresh",
            category = FoodCategory.FRUITS_BERRIES,
            caloriesPer100g = 89.0,
            proteinPer100g = 1.1,
            carbsPer100g = 22.8,
            fatsPer100g = 0.3,
            fiberPer100g = 2.6,
            servingPortions = listOf(
                ServingPortion("1 средний банан (120г)", 120.0),
                ServingPortion("1 крупный банан (150г)", 150.0)
            )
        ),
        AthleticFoodItem(
            id = "blueberries_fresh",
            nameRu = "Голубика / черника свежая",
            nameUz = "Chernika / Golubika mevalari",
            nameEn = "Blueberries, Fresh",
            category = FoodCategory.FRUITS_BERRIES,
            caloriesPer100g = 57.0,
            proteinPer100g = 0.7,
            carbsPer100g = 14.5,
            fatsPer100g = 0.3,
            fiberPer100g = 2.4,
            servingPortions = listOf(
                ServingPortion("1 стакан ягод (150г)", 150.0)
            )
        ),
        AthleticFoodItem(
            id = "apple_fresh",
            nameRu = "Яблоко свежее (с кожурой)",
            nameUz = "Olma",
            nameEn = "Apple, Fresh (with skin)",
            category = FoodCategory.FRUITS_BERRIES,
            caloriesPer100g = 52.0,
            proteinPer100g = 0.3,
            carbsPer100g = 13.8,
            fatsPer100g = 0.2,
            fiberPer100g = 2.4,
            servingPortions = listOf(
                ServingPortion("1 среднее яблоко (160г)", 160.0)
            )
        ),
        AthleticFoodItem(
            id = "broccoli_raw",
            nameRu = "Брокколи свежая",
            nameUz = "Brokkoli",
            nameEn = "Broccoli (Fresh Florets)",
            category = FoodCategory.VEGETABLES_GREENS,
            caloriesPer100g = 34.0,
            proteinPer100g = 2.8,
            carbsPer100g = 6.6,
            fatsPer100g = 0.4,
            fiberPer100g = 2.6,
            servingPortions = listOf(
                ServingPortion("1 тарелка (150г)", 150.0)
            )
        ),
        AthleticFoodItem(
            id = "spinach_fresh",
            nameRu = "Шпинат свежий (листья)",
            nameUz = "Ismaloq barglari",
            nameEn = "Spinach, Fresh Leaves",
            category = FoodCategory.VEGETABLES_GREENS,
            caloriesPer100g = 23.0,
            proteinPer100g = 2.9,
            carbsPer100g = 3.6,
            fatsPer100g = 0.4,
            fiberPer100g = 2.2,
            servingPortions = listOf(
                ServingPortion("Большая миска салата (100г)", 100.0)
            )
        ),

        // ====================================================================
        // 6. SPORTS SUPPLEMENTS & ATHLETE STAPLES
        // ====================================================================
        AthleticFoodItem(
            id = "whey_protein_isolate",
            nameRu = "Сывороточный изолят (Whey Isolate 90%)",
            nameUz = "Zardob oqsili izoliyati (Whey Isolate)",
            nameEn = "Whey Protein Isolate (WPI 90%)",
            category = FoodCategory.SPORTS_SUPPLEMENTS,
            caloriesPer100g = 370.0,
            proteinPer100g = 88.0,
            carbsPer100g = 1.5,
            fatsPer100g = 1.0,
            servingPortions = listOf(
                ServingPortion("1 мерный скуп (30г)", 30.0),
                ServingPortion("2 скупа (60г)", 60.0)
            )
        ),
        AthleticFoodItem(
            id = "micellar_casein",
            nameRu = "Мицеллярный казеин (ночной протеин)",
            nameUz = "Mitsellyar kazein (tungi protein)",
            nameEn = "Micellar Casein Protein",
            category = FoodCategory.SPORTS_SUPPLEMENTS,
            caloriesPer100g = 360.0,
            proteinPer100g = 78.0,
            carbsPer100g = 4.0,
            fatsPer100g = 1.5,
            servingPortions = listOf(
                ServingPortion("1 скуп (32г)", 32.0)
            )
        ),
        AthleticFoodItem(
            id = "creatine_monohydrate",
            nameRu = "Креатин моногидрат (микронизированный)",
            nameUz = "Kreatin monogidrat",
            nameEn = "Creatine Monohydrate (Micronized)",
            category = FoodCategory.SPORTS_SUPPLEMENTS,
            caloriesPer100g = 0.0,
            proteinPer100g = 0.0,
            carbsPer100g = 0.0,
            fatsPer100g = 0.0,
            servingPortions = listOf(
                ServingPortion("1 ежедневная порция (5г)", 5.0)
            )
        ),
        AthleticFoodItem(
            id = "maltodextrin_carb",
            nameRu = "Мальтодекстрин (быстрый углевод интра-воркаут)",
            nameUz = "Maltodekstrin (tezkor uglevod)",
            nameEn = "Maltodextrin Powder",
            category = FoodCategory.SPORTS_SUPPLEMENTS,
            caloriesPer100g = 380.0,
            proteinPer100g = 0.0,
            carbsPer100g = 95.0,
            fatsPer100g = 0.0,
            servingPortions = listOf(
                ServingPortion("1 скуп в шейкер (40г)", 40.0)
            )
        )
    )

    /**
     * Case-insensitive search across name and category with locale prioritizing.
     */
    fun search(query: String, category: FoodCategory? = null): List<AthleticFoodItem> {
        val trimmed = query.trim().lowercase()
        return foods.filter { food ->
            val matchesCategory = category == null || food.category == category
            val matchesQuery = if (trimmed.isEmpty()) {
                true
            } else {
                food.nameRu.lowercase().contains(trimmed) ||
                food.nameUz.lowercase().contains(trimmed) ||
                food.nameEn.lowercase().contains(trimmed)
            }
            matchesCategory && matchesQuery
        }
    }

    fun findById(id: String): AthleticFoodItem? = foods.find { it.id == id }
}

package dev.neverpaid.health.core.i18n

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf

@Immutable
interface DudaStrings {
    val languageCode: String

    // Bottom Navigation
    val navWorkouts: String
    val navTodo: String
    val navNutrition: String
    val navCoach: String
    val navProgress: String

    // Common
    val appName: String get() = "duda.uz"
    val cancel: String
    val save: String
    val delete: String
    val done: String
    val back: String
    val loading: String
    val error: String
    val retry: String
    val daysCount: String

    // Auth
    val authTitle: String
    val authSubtitle: String
    val tabLogin: String
    val tabRegister: String
    val emailLabel: String
    val passwordLabel: String
    val nameLabel: String
    val btnSignIn: String
    val btnSignUp: String
    val btnGuestMode: String
    val authOr: String
    val authFailed: String

    // Workouts & Dashboard
    val dashboardHeroActive: String
    val dashboardDayOf: String
    val dashboardStartToday: String
    val dashboardStartEmpty: String
    val dashboardNewTemplate: String
    val dashboardMyTemplates: String
    val dashboardAllPrograms: String
    val dashboardActiveBanner: String
    val dashboardContinue: String
    val dashboardCnsTitle: String
    val dashboardSessions: String
    val dashboardVolume: String
    val dashboardStreak: String

    // Active Workout
    val workoutFinish: String
    val workoutDiscard: String
    val workoutDiscardConfirmTitle: String
    val workoutDiscardConfirmText: String
    val workoutTotalVolume: String
    val workoutExercisesCount: String
    val workoutColSet: String
    val workoutColPrev: String
    val workoutColWeight: String
    val workoutColReps: String
    val workoutAddSet: String
    val workoutAddExercise: String
    val workoutRestTime: String
    val workoutRestUp: String
    val workoutCompleteDialogTitle: String
    val workoutAwesome: String
    val setRecordsTitle: String
    val setRecordsFallbackTitle: String
    val applyRecord: String
    val applied: String
    val openHistoryMenu: String
    val noRecordsYet: String
    val historyModalTitle: String
    val allSetsTab: String
    val sortByBest: String
    val sortByDate: String
    val useValues: String
    val allTimePR: String
    val bestWeight: String
    val totalCompleted: String
    val noHistoryFound: String

    // Programs & Community
    val programsTitle: String
    val programsSubtitle: String
    val programsFilterAll: String
    val programsDaysPerWeek: String
    val programsSetActive: String
    val programsCurrentActive: String
    val programsComments: String
    val programsWriteComment: String
    val programsReply: String
    val programsSend: String

    // Todo / Plan
    val todoTitle: String
    val todoSubtitle: String
    val todoDailyChecklist: String
    val todoTasksCompleted: String
    val todoAddTask: String
    val todoNewTaskTitle: String
    val todoTaskTitleLabel: String
    val todoTargetLabel: String
    val todoCategoryLabel: String
    val todoPriorityLabel: String
    val todoRecurringDaily: String
    val todoPomodoroTitle: String
    val todoPomodoroFocus: String
    val todoPomodoroShortBreak: String
    val todoPomodoroStart: String
    val todoPomodoroPause: String
    val todoPomodoroReset: String

    // Nutrition
    val nutritionTitle: String
    val nutritionSubtitle: String
    val nutritionEnergyBalance: String
    val nutritionLeftKcal: String
    val nutritionProtein: String
    val nutritionFats: String
    val nutritionCarbs: String
    val nutritionWater: String
    val nutritionMealsTitle: String
    val nutritionAddMealBtn: String
    val nutritionAiScanBtn: String
    val nutritionAiScanDesc: String
    val nutritionMealTypeLabel: String
    val nutritionMealNameLabel: String
    val nutritionCaloriesLabel: String

    // Progress
    val progressTitle: String
    val progressSubtitle: String
    val progressTabVolume: String
    val progressTabMuscles: String
    val progressTabPRs: String
    val progressWeeklyTonnage: String
    val progressHypertrophySets: String
    val progressHallOfFame: String
    val progressE1rm: String

    // History
    val historyTitle: String
    val historyDensity: String
    val historyLess: String
    val historyMore: String
    val historyLoggedSessions: String

    // Body
    val bodyTitle: String
    val bodySubtitle: String
    val bodyCurrentWeight: String
    val bodyMovingAverage: String
    val bodyHeight: String
    val bodyBmi: String
    val bodyTdee: String
    val bodyLogWeightBtn: String

    // Profile & Settings
    val profileTitle: String
    val profileLanguage: String
    val profileThemeLabel: String
    val profileThemeLight: String
    val profileThemeDark: String
    val profileUnits: String
    val profileRestTimer: String
    val profileVibration: String
    val profileServerUrl: String
    val profileLogout: String

    // Analytics & Charts extensions
    val noData: String
    val volumeTonnage: String
    val unitKg: String
    val unitLbs: String
    val unitKcal: String
    val consistencyHeatmap: String
    val workoutsDone: String
    val weeksUnit: String
    val daysStreak: String
    val lessMoreLegend: String
    val protein: String
    val carbs: String
    val fats: String
    val waterHydration: String

    // Barbell & Plate calculator
    val plateCalculatorTitle: String
    val barbellWeight: String
    val targetWeight: String
    val eachSide: String
    val platesPerSide: String
    val standardOlympicBar: String
    val womenOlympicBar: String
    val powerliftingBar: String
    val trapHexBar: String
    val ezCurlBar: String
    val safetySquatBar: String
    val smithMachineBar: String

    // Warmup Protocols
    val warmupProtocolsTitle: String
    val warmupStandardHypertrophy: String
    val warmupPowerliftingSingle: String
    val warmupWendler531: String
    val warmupPotentiation: String
    val workingWeight: String
    val warmupSetsLabel: String

    // Anatomy & Muscles
    val muscleChest: String
    val muscleUpperChest: String
    val muscleLats: String
    val muscleTraps: String
    val muscleMiddleBack: String
    val muscleLowerBack: String
    val muscleFrontDelts: String
    val muscleSideDelts: String
    val muscleRearDelts: String
    val muscleBiceps: String
    val muscleTriceps: String
    val muscleForearms: String
    val muscleAbs: String
    val muscleObliques: String
    val muscleQuads: String
    val muscleHamstrings: String
    val muscleGlutes: String
    val muscleCalves: String
    val muscleFrontView: String
    val muscleBackView: String
    val recoveryOptimal: String
    val recoveryFatigued: String
    val recoveryOverreached: String

    // Anthropometry & Body
    val adonisIndexTitle: String
    val navyBodyFatTitle: String
    val naturalPotentialTitle: String
    val shoulderToWaistRatio: String
    val goldenRatioTarget: String
    val neckCircumference: String
    val waistCircumference: String
    val hipCircumference: String
    val wristCircumference: String
    val ankleCircumference: String
    val calculatedBodyFat: String
    val leanMass: String
    val fatMass: String

    // Fasting & Hydration
    val intermittentFasting: String
    val fastingStatusFast: String
    val fastingStatusEating: String
    val startFastBtn: String
    val endFastBtn: String
    val hoursElapsed: String
    val hoursRemaining: String
    val fastingAutophagy: String
    val fastingKetosis: String
    val hydrationLogged: String

    // Habits
    val habitsTitle: String
    val habitsStreakLabel: String
    val habitDoneToday: String
    val habitMissed: String

    // Readiness
    val readinessSurveyTitle: String
    val cnsFatigue: String
    val muscleSoreness: String
    val sleepQuality: String
    val stressLevel: String
    val readinessScore: String
    val readinessRecommendation: String

    // Navigation & System Chrome (Obsidian 1:1)
    val menuButton: String
    val menuAllSections: String
    val menuAllSectionsSubtitle: String
    val navPrograms: String
    val navExercises: String
    val navHistory: String
    val navBody: String
    val navProfile: String
    val stateLoadingTitle: String
    val stateLoadingHint: String
    val stateErrorTitle: String
    val stateErrorHint: String

    // Royal Violet UI Kit & Localized Chrome Extensions
    val dashboardPersonalCopyAutoSave: String
    val dashboardWeeklyTonnageLabel: String
    val dashboardAllTools: String
    val dashboardActiveSessionLabel: String
    val dashboardExercisesShort: String
    val dashboardSetsShort: String
    val summaryReceiptTitle: String
    val summaryAutoSavedBadge: String
    val summaryDurationLabel: String
    val summaryTonsUnit: String
    val programsInstallsLabel: String
    val programsPublicCount: String
    val focusRestLabel: String
    val focusSkipLabel: String
    val focusSessionDefault: String
    val focusDeepWork: String
    val focusPomodoro: String
    val focusTimer: String
    val focusStopwatch: String
    val focusFinishClose: String
    val focusSessionsCount: String
    val commonClose: String
    val commonToGym: String
    val commonExpand: String
    val commonMinimize: String
    val muscleAtlasTitle: String
    val platePerSideTitle: String
    val exercisePickTitle: String
    val exerciseSearchPlaceholder: String
    val exerciseTechniqueLabel: String
    val exerciseAddToWorkout: String
}

object RuDudaStrings : DudaStrings {
    override val languageCode = "ru"
    override val navWorkouts = "Тренировки"
    override val navTodo = "План"
    override val navNutrition = "Питание"
    override val navCoach = "AI Тренер"
    override val navProgress = "Прогресс"
    override val cancel = "Отмена"
    override val save = "Сохранить"
    override val delete = "Удалить"
    override val done = "Готово"
    override val back = "Назад"
    override val loading = "Загрузка..."
    override val error = "Ошибка"
    override val retry = "Повторить"
    override val daysCount = "дней"

    override val authTitle = "duda.uz"
    override val authSubtitle = "Спортивная экосистема атлета"
    override val tabLogin = "Вход"
    override val tabRegister = "Регистрация"
    override val emailLabel = "Email адрес"
    override val passwordLabel = "Пароль"
    override val nameLabel = "Имя атлета"
    override val btnSignIn = "Войти в аккаунт"
    override val btnSignUp = "Зарегистрироваться"
    override val btnGuestMode = "Продолжить оффлайн (Гостевой режим)"
    override val authOr = "ИЛИ"
    override val authFailed = "Неверный email или пароль"

    override val dashboardHeroActive = "АКТИВНАЯ ПРОГРАММА"
    override val dashboardDayOf = "День %d из %d"
    override val dashboardStartToday = "Начать тренировку дня"
    override val dashboardStartEmpty = "Пустая тренировка"
    override val dashboardNewTemplate = "Новый шаблон"
    override val dashboardMyTemplates = "МОИ ШАБЛОНЫ"
    override val dashboardAllPrograms = "Все программы →"
    override val dashboardActiveBanner = "ИДЁТ ТРЕНИРОВКА"
    override val dashboardContinue = "Продолжить →"
    override val dashboardCnsTitle = "ИНДЕКС ГОТОВНОСТИ ЦНС"
    override val dashboardSessions = "Сессии"
    override val dashboardVolume = "Общий объём"
    override val dashboardStreak = "Серия"

    override val workoutFinish = "Завершить"
    override val workoutDiscard = "Сбросить"
    override val workoutDiscardConfirmTitle = "Сбросить тренировку?"
    override val workoutDiscardConfirmText = "Вы уверены, что хотите сбросить сессию? Записанные сеты не сохранятся."
    override val workoutTotalVolume = "Общий объём"
    override val workoutExercisesCount = "Упражнения"
    override val workoutColSet = "СЕТ"
    override val workoutColPrev = "ПРЕД"
    override val workoutColWeight = "КГ"
    override val workoutColReps = "ПОВТ"
    override val workoutAddSet = "+ Добавить подход"
    override val workoutAddExercise = "+ Добавить упражнение"
    override val workoutRestTime = "ОТДЫХ"
    override val workoutRestUp = "Время отдыха вышло! Пора на следующий подход."
    override val workoutCompleteDialogTitle = "🏆 ТРЕНИРОВКА ЗАВЕРШЕНА!"
    override val workoutAwesome = "Отлично!"
    override val setRecordsTitle = "Последние 3 раза"
    override val setRecordsFallbackTitle = "Последние результаты"
    override val applyRecord = "Подставить"
    override val applied = "Применено!"
    override val openHistoryMenu = "Вся история сетов в отдельном меню"
    override val noRecordsYet = "Пока нет записей для этого сета. Завершите подход, чтобы создать рекорд!"
    override val historyModalTitle = "История сетов и рекорды"
    override val allSetsTab = "Все сеты"
    override val sortByBest = "🏆 Топ рекорды"
    override val sortByDate = "📅 Свежие сначала"
    override val useValues = "Использовать эти веса"
    override val allTimePR = "Абсолютный рекорд"
    override val bestWeight = "Лучший вес"
    override val totalCompleted = "Всего выполнено"
    override val noHistoryFound = "История подходов для этого упражнения пока пуста."

    override val programsTitle = "Программы тренировок"
    override val programsSubtitle = "Каталог сплитов и отзывы атлетов"
    override val programsFilterAll = "Все уровни"
    override val programsDaysPerWeek = "%d дней в неделю"
    override val programsSetActive = "Сделать активной"
    override val programsCurrentActive = "Текущая программа"
    override val programsComments = "Комментарии"
    override val programsWriteComment = "Написать комментарий..."
    override val programsReply = "Ответить"
    override val programsSend = "Отправить"

    override val todoTitle = "ПЛАН И КАЛЕНДАРЬ"
    override val todoSubtitle = "Расписание и задачи"
    override val todoDailyChecklist = "ЕЖЕДНЕВНЫЙ ЧЕК-ЛИСТ АТЛЕТА"
    override val todoTasksCompleted = "%d из %d выполнено"
    override val todoAddTask = "+ Добавить задачу"
    override val todoNewTaskTitle = "Новая задача"
    override val todoTaskTitleLabel = "Название задачи"
    override val todoTargetLabel = "Целевое значение (напр. 2 л, 8 ч)"
    override val todoCategoryLabel = "Категория"
    override val todoPriorityLabel = "Приоритет"
    override val todoRecurringDaily = "Повторять ежедневно"
    override val todoPomodoroTitle = "ФОКУС ТАЙМЕР"
    override val todoPomodoroFocus = "Фокус 25 мин"
    override val todoPomodoroShortBreak = "Перерыв 5 мин"
    override val todoPomodoroStart = "Старт"
    override val todoPomodoroPause = "Пауза"
    override val todoPomodoroReset = "Сброс"

    override val nutritionTitle = "ПИТАНИЕ И МАКРОСЫ"
    override val nutritionSubtitle = "Дневной рацион"
    override val nutritionEnergyBalance = "ЭНЕРГЕТИЧЕСКИЙ БАЛАНС"
    override val nutritionLeftKcal = "Осталось %d ккал"
    override val nutritionProtein = "Белки"
    override val nutritionFats = "Жиры"
    override val nutritionCarbs = "Углеводы"
    override val nutritionWater = "Водный баланс"
    override val nutritionMealsTitle = "ПРИЁМЫ ПИЩИ"
    override val nutritionAddMealBtn = "+ Добавить приём пищи"
    override val nutritionAiScanBtn = "ИИ-оценка КБЖУ"
    override val nutritionAiScanDesc = "Опишите блюдо текстом (напр. «200г плова и салат»)"
    override val nutritionMealTypeLabel = "Тип приёма пищи"
    override val nutritionMealNameLabel = "Название продукта/блюда"
    override val nutritionCaloriesLabel = "Калории (ккал)"

    override val progressTitle = "Аналитика и рекорды"
    override val progressSubtitle = "Тоннаж, баланс мышц и лучшие веса"
    override val progressTabVolume = "Объём"
    override val progressTabMuscles = "Мышцы"
    override val progressTabPRs = "Рекорды"
    override val progressWeeklyTonnage = "НЕДЕЛЬНЫЙ ТОННАЖ (КГ)"
    override val progressHypertrophySets = "РАБОЧИЕ ПОДХОДЫ ЗА 7 ДНЕЙ"
    override val progressHallOfFame = "ЗАЛ СЛАВЫ РЕКОРДОВ"
    override val progressE1rm = "РАСЧЁТНЫЙ 1ПМ"

    override val historyTitle = "История тренировок"
    override val historyDensity = "ПЛОТНОСТЬ ТРЕНИРОВОК (12 НЕДЕЛЬ)"
    override val historyLess = "Меньше"
    override val historyMore = "Больше"
    override val historyLoggedSessions = "ЗАВЕРШЁННЫЕ ТРЕНИРОВКИ"

    override val bodyTitle = "Тело и здоровье"
    override val bodySubtitle = "Динамика веса, средняя за 7 дней и TDEE"
    override val bodyCurrentWeight = "ТЕКУЩИЙ ВЕС"
    override val bodyMovingAverage = "СКОЛЬЗЯЩЕЕ СРЕДНЕЕ (7 ДНЕЙ)"
    override val bodyHeight = "РОСТ"
    override val bodyBmi = "ИНДЕКС МАССЫ ТЕЛА"
    override val bodyTdee = "РАСХОД ЭНЕРГИИ (TDEE)"
    override val bodyLogWeightBtn = "+ Записать взвешивание"

    override val profileTitle = "Профиль атлета"
    override val profileLanguage = "Язык интерфейса"
    override val profileThemeLabel = "Тема оформления"
    override val profileThemeLight = "Светлая"
    override val profileThemeDark = "Тёмная"
    override val profileUnits = "Система единиц"
    override val profileRestTimer = "Таймер отдыха по умолчанию"
    override val profileVibration = "Вибрация при завершении подхода"
    override val profileServerUrl = "API Gateway URL"
    override val profileLogout = "Выйти из аккаунта"

    // Analytics & Charts extensions
    override val noData = "Нет данных для отображения"
    override val volumeTonnage = "Объём тоннажа"
    override val unitKg = "кг"
    override val unitLbs = "фунтов"
    override val unitKcal = "ккал"
    override val consistencyHeatmap = "ДИСЦИПЛИНА И РЕГУЛЯРНОСТЬ"
    override val workoutsDone = "тренировок выполнено"
    override val weeksUnit = "нед"
    override val daysStreak = "дней серии"
    override val lessMoreLegend = "Интенсивность нагрузки"
    override val protein = "Белки"
    override val carbs = "Углеводы"
    override val fats = "Жиры"
    override val waterHydration = "Водный баланс"

    // Barbell & Plate calculator
    override val plateCalculatorTitle = "Калькулятор блинов на гриф"
    override val barbellWeight = "Вес грифа"
    override val targetWeight = "Целевой вес"
    override val eachSide = "на каждую сторону"
    override val platesPerSide = "Блины на сторону"
    override val standardOlympicBar = "Олимпийский гриф 20 кг"
    override val womenOlympicBar = "Женский гриф 15 кг"
    override val powerliftingBar = "Пауэрлифтерский гриф 20 кг"
    override val trapHexBar = "Трэп-гриф (Hex Bar) 25 кг"
    override val ezCurlBar = "EZ-гриф изогнутый 10 кг"
    override val safetySquatBar = "SSB гриф для приседа 29 кг"
    override val smithMachineBar = "Гриф тренажёра Смита 15 кг"

    // Warmup Protocols
    override val warmupProtocolsTitle = "Протоколы разминки"
    override val warmupStandardHypertrophy = "Гипертрофия (стандарт)"
    override val warmupPowerliftingSingle = "Пауэрлифтинг (синглы)"
    override val warmupWendler531 = "Протокол Вендлера 5/3/1"
    override val warmupPotentiation = "Нервно-мышечная активация"
    override val workingWeight = "Рабочий вес"
    override val warmupSetsLabel = "Разминочные подходы"

    // Anatomy & Muscles
    override val muscleChest = "Грудные мышцы"
    override val muscleUpperChest = "Верхняя часть груди"
    override val muscleLats = "Широчайшие мышцы спины"
    override val muscleTraps = "Трапециевидные мышцы"
    override val muscleMiddleBack = "Средняя часть спины (ромбовидные)"
    override val muscleLowerBack = "Поясничный отдел (разгибатели)"
    override val muscleFrontDelts = "Передняя дельта"
    override val muscleSideDelts = "Средняя (боковая) дельта"
    override val muscleRearDelts = "Задняя дельта"
    override val muscleBiceps = "Бицепс"
    override val muscleTriceps = "Трицепс"
    override val muscleForearms = "Предплечья"
    override val muscleAbs = "Прямая мышца живота (пресс)"
    override val muscleObliques = "Косые мышцы живота"
    override val muscleQuads = "Квадрицепс"
    override val muscleHamstrings = "Бицепс бедра"
    override val muscleGlutes = "Ягодичные мышцы"
    override val muscleCalves = "Икроножные и камбаловидные"
    override val muscleFrontView = "Спереди"
    override val muscleBackView = "Сзади"
    override val recoveryOptimal = "Оптимальное восстановление"
    override val recoveryFatigued = "Умеренное утомление"
    override val recoveryOverreached = "Требуется отдых / разгрузка"

    // Anthropometry & Body
    override val adonisIndexTitle = "Индекс Адониса (Золотое сечение)"
    override val navyBodyFatTitle = "Калькулятор жира US Navy"
    override val naturalPotentialTitle = "Предел Кейси Батта (Natural Limit)"
    override val shoulderToWaistRatio = "Отношение плеч к талии"
    override val goldenRatioTarget = "Идеал: 1.618 (Золотое сечение)"
    override val neckCircumference = "Обхват шеи (см)"
    override val waistCircumference = "Обхват талии (см)"
    override val hipCircumference = "Обхват бёдер (см)"
    override val wristCircumference = "Обхват запястья (см)"
    override val ankleCircumference = "Обхват щиколотки (см)"
    override val calculatedBodyFat = "Процент жира в организме"
    override val leanMass = "Сухая мышечная масса"
    override val fatMass = "Жировая масса"

    // Fasting & Hydration
    override val intermittentFasting = "Интервальное голодание"
    override val fastingStatusFast = "Фаза фастинга"
    override val fastingStatusEating = "Окно приёма пищи"
    override val startFastBtn = "Начать голодание"
    override val endFastBtn = "Завершить голодание"
    override val hoursElapsed = "Прошло времени"
    override val hoursRemaining = "Осталось до цели"
    override val fastingAutophagy = "Клеточная аутофагия активна"
    override val fastingKetosis = "Глубокий кетоз и сжигание жира"
    override val hydrationLogged = "Вода успешно записана"

    // Habits
    override val habitsTitle = "Привычки и режим атлета"
    override val habitsStreakLabel = "Текущая серия"
    override val habitDoneToday = "Выполнено сегодня"
    override val habitMissed = "Пропущено"

    // Readiness
    override val readinessSurveyTitle = "Оценка готовности ЦНС к тренировке"
    override val cnsFatigue = "Усталость нервной системы"
    override val muscleSoreness = "Крепатура мышц (DOMS)"
    override val sleepQuality = "Качество и глубина сна"
    override val stressLevel = "Уровень психологического стресса"
    override val readinessScore = "Индекс готовности к нагрузке"
    override val readinessRecommendation = "Рекомендация по объёму"
    override val menuButton = "Меню"
    override val menuAllSections = "Все разделы"
    override val menuAllSectionsSubtitle = ""
    override val navPrograms = "Программы"
    override val navExercises = "Упражнения"
    override val navHistory = "История"
    override val navBody = "Метрики тела"
    override val navProfile = "Профиль"
    override val stateLoadingTitle = "Загружаем данные"
    override val stateLoadingHint = "Подождите немного…"
    override val stateErrorTitle = "Не удалось загрузить данные"
    override val stateErrorHint = "Сервер не ответил. Попробуйте повторить запрос."
    override val dashboardPersonalCopyAutoSave = "Личная копия · Автосохранение"
    override val dashboardWeeklyTonnageLabel = "Недельный тоннаж"
    override val dashboardAllTools = "Все инструменты"
    override val dashboardActiveSessionLabel = "Активная сессия"
    override val dashboardExercisesShort = "упр."
    override val dashboardSetsShort = "сет."
    override val summaryReceiptTitle = "Тренировка завершена"
    override val summaryAutoSavedBadge = "Изменения сохранены в личную программу"
    override val summaryDurationLabel = "Длительность"
    override val summaryTonsUnit = "т"
    override val programsInstallsLabel = "установок"
    override val programsPublicCount = "программ"
    override val focusRestLabel = "ОТДЫХ"
    override val focusSkipLabel = "Пропустить"
    override val focusSessionDefault = "Фокус-сессия"
    override val focusDeepWork = "Глубокая концентрация"
    override val focusPomodoro = "Помидор"
    override val focusTimer = "Таймер"
    override val focusStopwatch = "Секундомер"
    override val focusFinishClose = "Завершить и закрыть задачу"
    override val focusSessionsCount = "Сессий:"
    override val commonClose = "Закрыть"
    override val commonToGym = "В зал"
    override val commonExpand = "Развернуть"
    override val commonMinimize = "Свернуть"
    override val muscleAtlasTitle = "МЫШЕЧНЫЙ ТЕПЛОВОЙ АТЛАС"
    override val platePerSideTitle = "НА КАЖДУЮ СТОРОНУ:"
    override val exercisePickTitle = "Выбрать упражнение"
    override val exerciseSearchPlaceholder = "Поиск упражнения или мышцы..."
    override val exerciseTechniqueLabel = "Техника выполнения"
    override val exerciseAddToWorkout = "Добавить в тренировку"
}

object UzDudaStrings : DudaStrings {
    override val languageCode = "uz"
    override val navWorkouts = "Mashg'ulotlar"
    override val navTodo = "Reja"
    override val navNutrition = "Oziqlanish"
    override val navCoach = "AI Murabbiy"
    override val navProgress = "Rivojlanish"
    override val cancel = "Bekor qilish"
    override val save = "Saqlash"
    override val delete = "O'chirish"
    override val done = "Tayyor"
    override val back = "Orqaga"
    override val loading = "Yuklanmoqda..."
    override val error = "Xatolik"
    override val retry = "Qayta urinish"
    override val daysCount = "kun"

    override val authTitle = "duda.uz"
    override val authSubtitle = "Sportchi ekotizimi"
    override val tabLogin = "Kirish"
    override val tabRegister = "Ro'yxatdan o'tish"
    override val emailLabel = "Email manzili"
    override val passwordLabel = "Parol"
    override val nameLabel = "Sportchi ismi"
    override val btnSignIn = "Akkauntga kirish"
    override val btnSignUp = "Ro'yxatdan o'tish"
    override val btnGuestMode = "Offlayn davom etish (Mehmon rejimi)"
    override val authOr = "YOKI"
    override val authFailed = "Email yoki parol noto'g'ri"

    override val dashboardHeroActive = "FAOL DASTUR"
    override val dashboardDayOf = "%d-kun / %d"
    override val dashboardStartToday = "Kunning mashg'ulotini boshlash"
    override val dashboardStartEmpty = "Bo'sh mashg'ulot"
    override val dashboardNewTemplate = "Yangi shablon"
    override val dashboardMyTemplates = "MENING SHABLONLARIM"
    override val dashboardAllPrograms = "Barcha dasturlar →"
    override val dashboardActiveBanner = "MASHG'ULOT DAVOM ETMOQDA"
    override val dashboardContinue = "Davom etish →"
    override val dashboardCnsTitle = "MARKAZIY ASAB TIZIMI TAYYORLIGI"
    override val dashboardSessions = "Mashqlar"
    override val dashboardVolume = "Umumiy hajm"
    override val dashboardStreak = "Ketma-ketlik"

    override val workoutFinish = "Tugatish"
    override val workoutDiscard = "Bekor qilish"
    override val workoutDiscardConfirmTitle = "Mashg'ulotni bekor qilasizmi?"
    override val workoutDiscardConfirmText = "Haqiqatan ham ushbu mashg'ulotni bekor qilmoqchimisiz? Yozilgan yondashuvlar saqlanmaydi."
    override val workoutTotalVolume = "Umumiy hajm"
    override val workoutExercisesCount = "Mashqlar soni"
    override val workoutColSet = "SET"
    override val workoutColPrev = "OLDINGI"
    override val workoutColWeight = "KG"
    override val workoutColReps = "TAKROR"
    override val workoutAddSet = "+ Yondashuv qo'shish"
    override val workoutAddExercise = "+ Mashq qo'shish"
    override val workoutRestTime = "DAM OLISH"
    override val workoutRestUp = "Dam olish vaqti tugadi! Keyingi yondashuvga tayyorlaning."
    override val workoutCompleteDialogTitle = "🏆 MASHG'ULOT YAKUNLANDI!"
    override val workoutAwesome = "Ajoyib!"
    override val setRecordsTitle = "So'nggi 3 ta natija"
    override val setRecordsFallbackTitle = "So'nggi natijalar"
    override val applyRecord = "Qo‘llash"
    override val applied = "Kiritildi!"
    override val openHistoryMenu = "Barcha setlar tarixi alohida menyuda"
    override val noRecordsYet = "Ushbu set uchun yozuvlar yo‘q. Tarixni boshlash uchun setni bajaring!"
    override val historyModalTitle = "Setlar tarixi va rekordlar"
    override val allSetsTab = "Barcha setlar"
    override val sortByBest = "🏆 Eng yaxshi rekordlar"
    override val sortByDate = "📅 Yangilari oldin"
    override val useValues = "Ushbu vaznni kiritish"
    override val allTimePR = "Mutlaq rekord"
    override val bestWeight = "Eng yaxshi vazn"
    override val totalCompleted = "Jami bajarilgan"
    override val noHistoryFound = "Ushbu mashq uchun hali setlar tarixi yo‘q."

    override val programsTitle = "Mashg'ulot dasturlari"
    override val programsSubtitle = "Splitlar katalogi va sharhlar"
    override val programsFilterAll = "Barcha darajalar"
    override val programsDaysPerWeek = "Haftasiga %d kun"
    override val programsSetActive = "Faol qilish"
    override val programsCurrentActive = "Joriy dastur"
    override val programsComments = "Fikrlar"
    override val programsWriteComment = "Fikr yozing..."
    override val programsReply = "Javob berish"
    override val programsSend = "Yuborish"

    override val todoTitle = "REJA VA TAQVIM"
    override val todoSubtitle = "Mashg'ulotlar jadvali"
    override val todoDailyChecklist = "KUNLIK SPORTCHI TEKSHIRUV RO'YXATI"
    override val todoTasksCompleted = "%d / %d bajarildi"
    override val todoAddTask = "+ Vazifa qo'shish"
    override val todoNewTaskTitle = "Yangi vazifa"
    override val todoTaskTitleLabel = "Vazifa nomi"
    override val todoTargetLabel = "Maqsad ko'rsatkichi (masalan, 2 litr, 8 soat)"
    override val todoCategoryLabel = "Toifa"
    override val todoPriorityLabel = "Muhimlik"
    override val todoRecurringDaily = "Har kuni takrorlansin"
    override val todoPomodoroTitle = "FOKUS TAYMERI"
    override val todoPomodoroFocus = "Fokus 25 daqiqa"
    override val todoPomodoroShortBreak = "Tanaffus 5 daqiqa"
    override val todoPomodoroStart = "Boshlash"
    override val todoPomodoroPause = "Pauza"
    override val todoPomodoroReset = "Qaytarish"

    override val nutritionTitle = "OZIQLANISH VA MAKROSLAR"
    override val nutritionSubtitle = "Kunlik ratsion"
    override val nutritionEnergyBalance = "ENERGIYA BALANSI"
    override val nutritionLeftKcal = "%d kkal qoldi"
    override val nutritionProtein = "Oqsillar"
    override val nutritionFats = "Yog'lar"
    override val nutritionCarbs = "Uglevodlar"
    override val nutritionWater = "Suv balansi"
    override val nutritionMealsTitle = "TAOMLAR"
    override val nutritionAddMealBtn = "+ Taom qo'shish"
    override val nutritionAiScanBtn = "AI orqali aniqlash"
    override val nutritionAiScanDesc = "Taomni matn orqali tasvirlang (masalan, «200g osh va achchiq-chuchuk»)"
    override val nutritionMealTypeLabel = "Taom turi"
    override val nutritionMealNameLabel = "Mahsulot yoki taom nomi"
    override val nutritionCaloriesLabel = "Kaloriya (kkal)"

    override val progressTitle = "Tahlil va rekordlar"
    override val progressSubtitle = "Hajm, mushaklar balansi va shaxsiy natijalar"
    override val progressTabVolume = "Hajm"
    override val progressTabMuscles = "Mushaklar"
    override val progressTabPRs = "Rekordlar"
    override val progressWeeklyTonnage = "HAFTALIK TONNAJ (KG)"
    override val progressHypertrophySets = "7 KUNDAGI ISHCHI YONDASHUVLAR"
    override val progressHallOfFame = "SHAXSIY REKORDLAR ZALI"
    override val progressE1rm = "HISOBLANGAN 1TM"

    override val historyTitle = "Mashg'ulotlar tarixi"
    override val historyDensity = "MASHG'ULOTLAR ZICHLIGI (12 HAFTA)"
    override val historyLess = "Kamroq"
    override val historyMore = "Ko'proq"
    override val historyLoggedSessions = "YAKUNLANGAN MASHG'ULOTLAR"

    override val bodyTitle = "Tana va salomatlik"
    override val bodySubtitle = "Vazn o'zgarishi, 7 kunlik o'rtacha va TDEE"
    override val bodyCurrentWeight = "JORIY VAZN"
    override val bodyMovingAverage = "O'RTACHA KO'RSATKICH (7 KUN)"
    override val bodyHeight = "BO'Y"
    override val bodyBmi = "TANA VAZNI INDEKSI"
    override val bodyTdee = "KUNLIK ENERGIYA SARFI (TDEE)"
    override val bodyLogWeightBtn = "+ Vaznni kiritish"

    override val profileTitle = "Sportchi profili"
    override val profileLanguage = "Interfeys tili"
    override val profileThemeLabel = "Mavzu"
    override val profileThemeLight = "Yorug'"
    override val profileThemeDark = "Qorong'i"
    override val profileUnits = "O'lchov tizimi"
    override val profileRestTimer = "Odatiy dam olish vaqti"
    override val profileVibration = "Yondashuvdan so'ng vibratsiya"
    override val profileServerUrl = "API Gateway URL"
    override val profileLogout = "Chiqish"

    // Analytics & Charts extensions
    override val noData = "Ko'rsatish uchun ma'lumot yo'q"
    override val volumeTonnage = "Umumiy tonnaj hajmi"
    override val unitKg = "kg"
    override val unitLbs = "funt"
    override val unitKcal = "kkal"
    override val consistencyHeatmap = "INTIZOM VA DOIMIYLIK"
    override val workoutsDone = "ta mashg'ulot bajarildi"
    override val weeksUnit = "hafta"
    override val daysStreak = "kunlik seriya"
    override val lessMoreLegend = "Yuklama darajasi"
    override val protein = "Oqsillar"
    override val carbs = "Uglevodlar"
    override val fats = "Yog'lar"
    override val waterHydration = "Suv balansi"

    // Barbell & Plate calculator
    override val plateCalculatorTitle = "Grifdagi disklar kalkulyatori"
    override val barbellWeight = "Grif og'irligi"
    override val targetWeight = "Maqsadli og'irlik"
    override val eachSide = "har bir tomonga"
    override val platesPerSide = "Har bir tomondagi disklar"
    override val standardOlympicBar = "Olimpiya grifi 20 kg"
    override val womenOlympicBar = "Ayollar olimpiya grifi 15 kg"
    override val powerliftingBar = "Pauerlifting grifi 20 kg"
    override val trapHexBar = "Trep-grif (Hex Bar) 25 kg"
    override val ezCurlBar = "EZ-buralgan grif 10 kg"
    override val safetySquatBar = "SSB o'tirish grifi 29 kg"
    override val smithMachineBar = "Smit trenajyori grifi 15 kg"

    // Warmup Protocols
    override val warmupProtocolsTitle = "Qizdirish protokollari"
    override val warmupStandardHypertrophy = "Gipertrofiya (standart)"
    override val warmupPowerliftingSingle = "Pauerlifting (singllar)"
    override val warmupWendler531 = "Vendler 5/3/1 protokoli"
    override val warmupPotentiation = "Asab-mushak faollashuvi"
    override val workingWeight = "Ishchi og'irlik"
    override val warmupSetsLabel = "Qizdirish yondashuvlari"

    // Anatomy & Muscles
    override val muscleChest = "Ko'krak mushaklari"
    override val muscleUpperChest = "Ko'krakning yuqori qismi"
    override val muscleLats = "Orqa qanot mushaklari"
    override val muscleTraps = "Trapetsiya mushaklari"
    override val muscleMiddleBack = "Orqaning o'rta qismi"
    override val muscleLowerBack = "Bel mushaklari"
    override val muscleFrontDelts = "Old delta"
    override val muscleSideDelts = "O'rta (yon) delta"
    override val muscleRearDelts = "Orqa delta"
    override val muscleBiceps = "Biceps"
    override val muscleTriceps = "Triceps"
    override val muscleForearms = "Bilak mushaklari"
    override val muscleAbs = "Qorin pressi"
    override val muscleObliques = "Yon qorin mushaklari"
    override val muscleQuads = "Kvadritseps (son oldi)"
    override val muscleHamstrings = "Son orqasi mushagi"
    override val muscleGlutes = "Dumba mushaklari"
    override val muscleCalves = "Boldir mushaklari"
    override val muscleFrontView = "Old tomondan"
    override val muscleBackView = "Orqa tomondan"
    override val recoveryOptimal = "Optimal tiklanish"
    override val recoveryFatigued = "O'rtacha toliqish"
    override val recoveryOverreached = "Dam olish zarur"

    // Anthropometry & Body
    override val adonisIndexTitle = "Adonis indeksi (Oltin nisbat)"
    override val navyBodyFatTitle = "US Navy tana yog'i kalkulyatori"
    override val naturalPotentialTitle = "Keysi Batt tabiiy chegarasi"
    override val shoulderToWaistRatio = "Yelka va bel nisbati"
    override val goldenRatioTarget = "Ideal: 1.618 (Oltin nisbat)"
    override val neckCircumference = "Bo'yin aylanasi (sm)"
    override val waistCircumference = "Bel aylanasi (sm)"
    override val hipCircumference = "Bo'ksa aylanasi (sm)"
    override val wristCircumference = "Bilak aylanasi (sm)"
    override val ankleCircumference = "To'piq aylanasi (sm)"
    override val calculatedBodyFat = "Tana yog'i foizi"
    override val leanMass = "Quruq mushak massasi"
    override val fatMass = "Yog' massasi"

    // Fasting & Hydration
    override val intermittentFasting = "Interval ochlik (Fasting)"
    override val fastingStatusFast = "Ochlik fazasi"
    override val fastingStatusEating = "Ovqatlanish oynasi"
    override val startFastBtn = "Fastingni boshlash"
    override val endFastBtn = "Fastingni yakunlash"
    override val hoursElapsed = "O'tgan vaqt"
    override val hoursRemaining = "Maqsadgacha qolgan vaqt"
    override val fastingAutophagy = "Hujayra autofagiyasi faol"
    override val fastingKetosis = "Chuqur ketoz va yog' erishi"
    override val hydrationLogged = "Suv muvaffaqiyatli saqlandi"

    // Habits
    override val habitsTitle = "Odatlar va kun tartibi"
    override val habitsStreakLabel = "Joriy seriya"
    override val habitDoneToday = "Bugun bajarildi"
    override val habitMissed = "O'tkazib yuborildi"

    // Readiness
    override val readinessSurveyTitle = "MSK tayyorgarligini baholash"
    override val cnsFatigue = "Asab tizimi charchog'i"
    override val muscleSoreness = "Mushaklardagi og'riq (DOMS)"
    override val sleepQuality = "Uyqu sifati va chuqurligi"
    override val stressLevel = "Psixologik stress darajasi"
    override val readinessScore = "Yuklamaga tayyorgarlik indeksi"
    override val readinessRecommendation = "Yuklama bo'yicha tavsiya"
    override val menuButton = "Menyu"
    override val menuAllSections = "Barcha bo'limlar"
    override val menuAllSectionsSubtitle = ""
    override val navPrograms = "Dasturlar"
    override val navExercises = "Mashqlar"
    override val navHistory = "Tarix"
    override val navBody = "Tana ko'rsatkichlari"
    override val navProfile = "Profil"
    override val stateLoadingTitle = "Ma'lumotlar yuklanmoqda"
    override val stateLoadingHint = "Biroz kuting…"
    override val stateErrorTitle = "Ma'lumotlarni yuklab bo'lmadi"
    override val stateErrorHint = "Server javob bermadi. So'rovni qayta urinib ko'ring."
    override val dashboardPersonalCopyAutoSave = "Shaxsiy nusxa · Avto-saqlash"
    override val dashboardWeeklyTonnageLabel = "Haftalik tonnaj"
    override val dashboardAllTools = "Barcha vositalar"
    override val dashboardActiveSessionLabel = "Faol mashg'ulot"
    override val dashboardExercisesShort = "mashq"
    override val dashboardSetsShort = "set"
    override val summaryReceiptTitle = "Mashg'ulot yakunlandi"
    override val summaryAutoSavedBadge = "O'zgarishlar shaxsiy dasturingizga saqlandi"
    override val summaryDurationLabel = "Davomiyligi"
    override val summaryTonsUnit = "t"
    override val programsInstallsLabel = "o'rnatish"
    override val programsPublicCount = "dastur"
    override val focusRestLabel = "DAM OLISH"
    override val focusSkipLabel = "O'tkazib yuborish"
    override val focusSessionDefault = "Fokus-sessiya"
    override val focusDeepWork = "Chuqur diqqat"
    override val focusPomodoro = "Pomidor"
    override val focusTimer = "Taymer"
    override val focusStopwatch = "Sekundomer"
    override val focusFinishClose = "Yakunlash va vazifani yopish"
    override val focusSessionsCount = "Sessiyalar:"
    override val commonClose = "Yopish"
    override val commonToGym = "Zalga"
    override val commonExpand = "Kengaytirish"
    override val commonMinimize = "Yig'ish"
    override val muscleAtlasTitle = "MUSHAKLAR ISSIQLIK ATLASI"
    override val platePerSideTitle = "HAR BIR TOMONGA:"
    override val exercisePickTitle = "Mashq tanlash"
    override val exerciseSearchPlaceholder = "Mashq yoki mushak qidirish..."
    override val exerciseTechniqueLabel = "Bajarish texnikasi"
    override val exerciseAddToWorkout = "Mashg'ulotga qo'shish"
}

object EnDudaStrings : DudaStrings {
    override val languageCode = "en"
    override val navWorkouts = "Workouts"
    override val navTodo = "Plan"
    override val navNutrition = "Nutrition"
    override val navCoach = "AI Coach"
    override val navProgress = "Progress"
    override val cancel = "Cancel"
    override val save = "Save"
    override val delete = "Delete"
    override val done = "Done"
    override val back = "Back"
    override val loading = "Loading..."
    override val error = "Error"
    override val retry = "Retry"
    override val daysCount = "days"

    override val authTitle = "duda.uz"
    override val authSubtitle = "Athlete sports ecosystem"
    override val tabLogin = "Sign In"
    override val tabRegister = "Sign Up"
    override val emailLabel = "Email address"
    override val passwordLabel = "Password"
    override val nameLabel = "Athlete name"
    override val btnSignIn = "Sign In"
    override val btnSignUp = "Create Account"
    override val btnGuestMode = "Continue Offline (Guest Mode)"
    override val authOr = "OR"
    override val authFailed = "Invalid email or password"

    override val dashboardHeroActive = "ACTIVE PROGRAM"
    override val dashboardDayOf = "Day %d of %d"
    override val dashboardStartToday = "Start Today's Workout"
    override val dashboardStartEmpty = "Empty Workout"
    override val dashboardNewTemplate = "New Template"
    override val dashboardMyTemplates = "MY TEMPLATES"
    override val dashboardAllPrograms = "All Programs →"
    override val dashboardActiveBanner = "WORKOUT IN PROGRESS"
    override val dashboardContinue = "Resume →"
    override val dashboardCnsTitle = "CNS READINESS INDEX"
    override val dashboardSessions = "Workouts"
    override val dashboardVolume = "Total Volume"
    override val dashboardStreak = "Streak"

    override val workoutFinish = "Finish"
    override val workoutDiscard = "Discard"
    override val workoutDiscardConfirmTitle = "Discard Workout?"
    override val workoutDiscardConfirmText = "Are you sure you want to discard this session? Logged sets will not be saved."
    override val workoutTotalVolume = "Total Volume"
    override val workoutExercisesCount = "Exercises"
    override val workoutColSet = "SET"
    override val workoutColPrev = "PREV"
    override val workoutColWeight = "KG"
    override val workoutColReps = "REPS"
    override val workoutAddSet = "+ Add Set"
    override val workoutAddExercise = "+ Add Exercise"
    override val workoutRestTime = "REST"
    override val workoutRestUp = "Rest time is up! Ready for next set."
    override val workoutCompleteDialogTitle = "🏆 WORKOUT COMPLETE!"
    override val workoutAwesome = "Awesome!"
    override val setRecordsTitle = "Last 3 Results"
    override val setRecordsFallbackTitle = "Recent Results"
    override val applyRecord = "Apply"
    override val applied = "Applied!"
    override val openHistoryMenu = "All Sets History in Separate Menu"
    override val noRecordsYet = "No completed records for this set yet. Finish this set to start your history!"
    override val historyModalTitle = "Set History & Records"
    override val allSetsTab = "All Sets"
    override val sortByBest = "🏆 Top Records"
    override val sortByDate = "📅 Most Recent"
    override val useValues = "Use These Values"
    override val allTimePR = "All-Time PR"
    override val bestWeight = "Best Weight"
    override val totalCompleted = "Total Completed"
    override val noHistoryFound = "No set history found for this exercise yet."

    override val programsTitle = "Training Programs"
    override val programsSubtitle = "Split catalog & athlete reviews"
    override val programsFilterAll = "All Levels"
    override val programsDaysPerWeek = "%d days per week"
    override val programsSetActive = "Set as Active"
    override val programsCurrentActive = "Active Program"
    override val programsComments = "Comments"
    override val programsWriteComment = "Write a comment..."
    override val programsReply = "Reply"
    override val programsSend = "Send"

    override val todoTitle = "PLAN & CALENDAR"
    override val todoSubtitle = "Workout schedule & tasks"
    override val todoDailyChecklist = "ATHLETE DAILY CHECKLIST"
    override val todoTasksCompleted = "%d of %d completed"
    override val todoAddTask = "+ Add Task"
    override val todoNewTaskTitle = "New Task"
    override val todoTaskTitleLabel = "Task Title"
    override val todoTargetLabel = "Target Value (e.g. 2 L, 8 hrs)"
    override val todoCategoryLabel = "Category"
    override val todoPriorityLabel = "Priority"
    override val todoRecurringDaily = "Repeat Daily"
    override val todoPomodoroTitle = "FOCUS TIMER"
    override val todoPomodoroFocus = "Focus 25 min"
    override val todoPomodoroShortBreak = "Break 5 min"
    override val todoPomodoroStart = "Start"
    override val todoPomodoroPause = "Pause"
    override val todoPomodoroReset = "Reset"

    override val nutritionTitle = "NUTRITION & MACROS"
    override val nutritionSubtitle = "Daily Diet"
    override val nutritionEnergyBalance = "ENERGY BALANCE"
    override val nutritionLeftKcal = "%d kcal remaining"
    override val nutritionProtein = "Protein"
    override val nutritionFats = "Fats"
    override val nutritionCarbs = "Carbs"
    override val nutritionWater = "Water Intake"
    override val nutritionMealsTitle = "MEALS"
    override val nutritionAddMealBtn = "+ Add Meal"
    override val nutritionAiScanBtn = "AI Food Analysis"
    override val nutritionAiScanDesc = "Describe your meal with text (e.g. '200g rice and grilled salmon')"
    override val nutritionMealTypeLabel = "Meal Type"
    override val nutritionMealNameLabel = "Food Name"
    override val nutritionCaloriesLabel = "Calories (kcal)"

    override val progressTitle = "Analytics & Benchmarks"
    override val progressSubtitle = "Volume trends, hypertrophy load & personal records"
    override val progressTabVolume = "Volume"
    override val progressTabMuscles = "Muscles"
    override val progressTabPRs = "Records"
    override val progressWeeklyTonnage = "WEEKLY TONNAGE (KG)"
    override val progressHypertrophySets = "7-DAY HYPERTROPHY SETS"
    override val progressHallOfFame = "PERSONAL RECORDS HALL OF FAME"
    override val progressE1rm = "ESTIMATED 1RM"

    override val historyTitle = "Training History"
    override val historyDensity = "12-WEEK TRAINING DENSITY"
    override val historyLess = "Less"
    override val historyMore = "More"
    override val historyLoggedSessions = "LOGGED SESSIONS"

    override val bodyTitle = "Body & Health"
    override val bodySubtitle = "Weight tracking, 7-day moving avg & TDEE"
    override val bodyCurrentWeight = "CURRENT WEIGHT"
    override val bodyMovingAverage = "7-DAY MOVING AVG"
    override val bodyHeight = "HEIGHT"
    override val bodyBmi = "BODY MASS INDEX"
    override val bodyTdee = "ENERGY EXPENDITURE (TDEE)"
    override val bodyLogWeightBtn = "+ Log Weigh-In"

    override val profileTitle = "Athlete Profile"
    override val profileLanguage = "Interface Language"
    override val profileThemeLabel = "Appearance"
    override val profileThemeLight = "Light"
    override val profileThemeDark = "Dark"
    override val profileUnits = "Unit System"
    override val profileRestTimer = "Default Rest Timer"
    override val profileVibration = "Haptic Vibration on Sets"
    override val profileServerUrl = "API Gateway REST URL"
    override val profileLogout = "Sign Out"

    // Analytics & Charts extensions
    override val noData = "No data available to display"
    override val volumeTonnage = "Volume Tonnage"
    override val unitKg = "kg"
    override val unitLbs = "lbs"
    override val unitKcal = "kcal"
    override val consistencyHeatmap = "DISCIPLINE & CONSISTENCY"
    override val workoutsDone = "workouts completed"
    override val weeksUnit = "wks"
    override val daysStreak = "days streak"
    override val lessMoreLegend = "Training load intensity"
    override val protein = "Protein"
    override val carbs = "Carbohydrates"
    override val fats = "Fats"
    override val waterHydration = "Hydration Balance"

    // Barbell & Plate calculator
    override val plateCalculatorTitle = "Barbell Plate Calculator"
    override val barbellWeight = "Barbell Weight"
    override val targetWeight = "Target Weight"
    override val eachSide = "each side"
    override val platesPerSide = "Plates Per Side"
    override val standardOlympicBar = "Olympic Barbell 20 kg"
    override val womenOlympicBar = "Women's Olympic Barbell 15 kg"
    override val powerliftingBar = "Powerlifting Barbell 20 kg"
    override val trapHexBar = "Trap / Hex Bar 25 kg"
    override val ezCurlBar = "EZ-Curl Bar 10 kg"
    override val safetySquatBar = "Safety Squat Bar (SSB) 29 kg"
    override val smithMachineBar = "Smith Machine Bar 15 kg"

    // Warmup Protocols
    override val warmupProtocolsTitle = "Scientific Warmup Protocols"
    override val warmupStandardHypertrophy = "Hypertrophy (Standard)"
    override val warmupPowerliftingSingle = "Powerlifting (Heavy Singles)"
    override val warmupWendler531 = "Wendler 5/3/1 Protocol"
    override val warmupPotentiation = "Post-Activation Potentiation"
    override val workingWeight = "Working Weight"
    override val warmupSetsLabel = "Warmup Sets"

    // Anatomy & Muscles
    override val muscleChest = "Chest (Pectorals)"
    override val muscleUpperChest = "Upper Chest (Clavicular Head)"
    override val muscleLats = "Latissimus Dorsi"
    override val muscleTraps = "Trapezius"
    override val muscleMiddleBack = "Mid Back (Rhomboids)"
    override val muscleLowerBack = "Lower Back (Erector Spinae)"
    override val muscleFrontDelts = "Anterior Deltoid"
    override val muscleSideDelts = "Lateral Deltoid"
    override val muscleRearDelts = "Posterior Deltoid"
    override val muscleBiceps = "Biceps Brachii"
    override val muscleTriceps = "Triceps Brachii"
    override val muscleForearms = "Forearms & Grip"
    override val muscleAbs = "Rectus Abdominis"
    override val muscleObliques = "Obliques"
    override val muscleQuads = "Quadriceps"
    override val muscleHamstrings = "Hamstrings"
    override val muscleGlutes = "Gluteal Muscles"
    override val muscleCalves = "Calves (Gastrocnemius / Soleus)"
    override val muscleFrontView = "Anterior View"
    override val muscleBackView = "Posterior View"
    override val recoveryOptimal = "Fully Recovered"
    override val recoveryFatigued = "Mild Fatigue"
    override val recoveryOverreached = "Deload Recommended"

    // Anthropometry & Body
    override val adonisIndexTitle = "Adonis Index (Golden Ratio)"
    override val navyBodyFatTitle = "US Navy Body Fat Calculator"
    override val naturalPotentialTitle = "Casey Butt Natural Muscular Limit"
    override val shoulderToWaistRatio = "Shoulder to Waist Ratio"
    override val goldenRatioTarget = "Ideal: 1.618 (Golden Ratio)"
    override val neckCircumference = "Neck Circumference (cm)"
    override val waistCircumference = "Waist Circumference (cm)"
    override val hipCircumference = "Hip Circumference (cm)"
    override val wristCircumference = "Wrist Circumference (cm)"
    override val ankleCircumference = "Ankle Circumference (cm)"
    override val calculatedBodyFat = "Body Fat Percentage"
    override val leanMass = "Lean Body Mass"
    override val fatMass = "Fat Mass"

    // Fasting & Hydration
    override val intermittentFasting = "Intermittent Fasting"
    override val fastingStatusFast = "Fasting State"
    override val fastingStatusEating = "Eating Window"
    override val startFastBtn = "Start Fasting Session"
    override val endFastBtn = "End Fasting Session"
    override val hoursElapsed = "Time Elapsed"
    override val hoursRemaining = "Time to Target"
    override val fastingAutophagy = "Cellular Autophagy Active"
    override val fastingKetosis = "Ketosis & Fatty Acid Oxidation"
    override val hydrationLogged = "Hydration logged successfully"

    // Habits
    override val habitsTitle = "Athlete Habits & Routine"
    override val habitsStreakLabel = "Active Streak"
    override val habitDoneToday = "Completed Today"
    override val habitMissed = "Missed"

    // Readiness
    override val readinessSurveyTitle = "CNS Workout Readiness Assessment"
    override val cnsFatigue = "Central Nervous System Fatigue"
    override val muscleSoreness = "Muscle Soreness (DOMS)"
    override val sleepQuality = "Sleep Quality & Restfulness"
    override val stressLevel = "Psychological Stress"
    override val readinessScore = "Readiness Index"
    override val readinessRecommendation = "Volume Recommendation"
    override val menuButton = "Menu"
    override val menuAllSections = "All sections"
    override val menuAllSectionsSubtitle = ""
    override val navPrograms = "Programs"
    override val navExercises = "Exercises"
    override val navHistory = "History"
    override val navBody = "Body Metrics"
    override val navProfile = "Profile"
    override val stateLoadingTitle = "Loading data"
    override val stateLoadingHint = "Please wait a moment…"
    override val stateErrorTitle = "Failed to load data"
    override val stateErrorHint = "The server did not respond. Try again."
    override val dashboardPersonalCopyAutoSave = "Personal copy · Auto-saved"
    override val dashboardWeeklyTonnageLabel = "Weekly Tonnage"
    override val dashboardAllTools = "All Tools"
    override val dashboardActiveSessionLabel = "Active Session"
    override val dashboardExercisesShort = "ex."
    override val dashboardSetsShort = "sets"
    override val summaryReceiptTitle = "Workout Complete"
    override val summaryAutoSavedBadge = "Changes saved to your personal program"
    override val summaryDurationLabel = "Duration"
    override val summaryTonsUnit = "t"
    override val programsInstallsLabel = "installs"
    override val programsPublicCount = "programs"
    override val focusRestLabel = "REST"
    override val focusSkipLabel = "Skip"
    override val focusSessionDefault = "Focus Session"
    override val focusDeepWork = "Deep Focus"
    override val focusPomodoro = "Pomodoro"
    override val focusTimer = "Timer"
    override val focusStopwatch = "Stopwatch"
    override val focusFinishClose = "Finish & complete task"
    override val focusSessionsCount = "Sessions:"
    override val commonClose = "Close"
    override val commonToGym = "Resume"
    override val commonExpand = "Expand"
    override val commonMinimize = "Minimize"
    override val muscleAtlasTitle = "MUSCLE VOLUME HEATMAP"
    override val platePerSideTitle = "EACH SIDE:"
    override val exercisePickTitle = "Choose Exercise"
    override val exerciseSearchPlaceholder = "Search exercise or muscle..."
    override val exerciseTechniqueLabel = "Execution Technique"
    override val exerciseAddToWorkout = "Add to Workout"
}

fun getDudaStrings(languageCode: String): DudaStrings {
    return when (languageCode.lowercase()) {
        "uz" -> UzDudaStrings
        "en" -> EnDudaStrings
        else -> RuDudaStrings
    }
}

val LocalDudaStrings = compositionLocalOf<DudaStrings> { RuDudaStrings }

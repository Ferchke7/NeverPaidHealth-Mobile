# Спецификация API и Бизнес-Логики (`duda-mobile` ↔ `duda.uz`)

Документ описывает точные контракты взаимодействия мобильного приложения `duda-mobile` с API-шлюзом `https://duda.uz/api/v1` и алгоритмы клиентских расчётов.

---

## 1. Карта эндпоинтов `https://duda.uz/api/v1`

Все защищённые запросы передают заголовки:
- `Authorization: Bearer <accessToken>`
- `Accept-Language: ru | uz | en`
- `Content-Type: application/json`

### 1.1. Identity & Auth (`/api/v1/auth`, `/api/v1/me`, `/api/v1/profile`)
| Метод | Путь | Назначение | Тело запроса / Ответа |
| :--- | :--- | :--- | :--- |
| `POST` | `/auth/login` | Вход по email и паролю | Req: `{ email, password }` → Resp: `{ access_token, user }` |
| `POST` | `/auth/register` | Регистрация нового атлета | Req: `{ email, password, name }` → Resp: `{ access_token, user }` |
| `POST` | `/auth/google` | Вход через Google ID Token | Req: `{ credential }` → Resp: `{ access_token, user }` |
| `POST` | `/auth/logout` | Выход из сессии | `204 No Content` |
| `GET` | `/me` | Профиль текущего пользователя | Resp: `UserProfileDto { id, email, name, avatar_url, unit_preference }` |
| `PUT` | `/profile` | Обновление настроек профиля | Req: `{ name, unit_preference }` |

### 1.2. Training, Workouts, Routines & Programs (`/api/v1/workouts`, `/api/v1/routines`, `/api/v1/programs`)
| Метод | Путь | Назначение | Описание логики |
| :--- | :--- | :--- | :--- |
| `GET` | `/workouts` | История тренировок атлета | Возвращает список завершённых сессий с подходами |
| `POST` | `/workouts` | Сохранение завершённой сессии | Отправляет `title`, `start_time`, `end_time`, `exercises[]`, `sets[]` |
| `DELETE`| `/workouts/{id}` | Удаление тренировки из истории | Пересчитывает тоннаж и PR после удаления |
| `GET` | `/routines` | Шаблоны тренировок | Пользовательские и базовые шаблоны |
| `POST` | `/routines` | Создание нового шаблона | Req: `{ title, description, exercises[] }` |
| `GET` | `/programs` | Каталог тренировочных программ | Сплиты с разбивкой по дням (`days[]`), счётчиком лайков и комментариев |
| `POST` | `/programs/{id}/activate` | Активация программы | Делает программу текущей активной для атлета |
| `POST` | `/programs/{id}/like` | Лайк программы | Тоггл лайка программы |
| `GET` | `/programs/{id}/comments` | Комментарии к программе | Древовидный список комментариев (`parent_id` для ответов) |
| `POST` | `/programs/{id}/comments` | Отправка комментария / ответа | Req: `{ content, parent_id? }` (открывается строго в `ModalBottomSheet`) |

### 1.3. Exercise Catalog (`/api/v1/exercises`)
| Метод | Путь | Назначение | Описание логики |
| :--- | :--- | :--- | :--- |
| `GET` | `/exercises` | Каталог упражнений (870+) | Фильтрация по `query`, `muscle`, `equipment`, поддержка локализации |
| `POST` | `/exercises` | Создание кастомного упражнения | Req: `{ name, primary_muscle_group, equipment }` |

### 1.4. Daily Plan & Todo (`/api/v1/todos`, `/api/v1/schedule`, `/api/v1/activity-logs`)
| Метод | Путь | Назначение | Описание логики |
| :--- | :--- | :--- | :--- |
| `GET` | `/todos?date=YYYY-MM-DD` | Список задач на выбранную дату | Возвращает задачи и статус выполнения строго за `YYYY-MM-DD` |
| `POST` | `/todos` | Создание новой задачи | Req: `{ title, target, category, priority, is_recurring, scheduled_date }` |
| `POST` | `/todos/{id}/toggle` | Отметка выполнения за дату | Req: `{ date: "YYYY-MM-DD", completed: true/false }` |
| `DELETE`| `/todos/{id}` | Удаление задачи | Удаляет задачу из плана |
| `GET` | `/activity-logs` | Лог активности по дням | Используется для точного расчёта серии (`streakDays`) и календаря |

### 1.5. Nutrition & AI Coach (`/api/v1/nutrition`, `/api/v1/coach`)
| Метод | Путь | Назначение | Описание логики |
| :--- | :--- | :--- | :--- |
| `GET` | `/nutrition/logs?date=YYYY-MM-DD` | Приёмы пищи за выбранный день | Возвращает список блюд, КБЖУ и выпитую воду |
| `POST` | `/nutrition/logs` | Запись приёма пищи | Req: `{ date, meal_type, name, calories, protein_g, fats_g, carbs_g }` |
| `POST` | `/nutrition/analyze` | ИИ-оценка КБЖУ по тексту/фото | Req: `{ description, image_base64? }` → Resp: `{ name, calories, protein, fats, carbs }` |
| `POST` | `/coach/chat` | Диалог с ИИ-тренером | Req: `{ message, context }` → Resp: `{ reply, suggested_actions[] }` |
| `GET` | `/coach/insights` | Индекс готовности ЦНС | Возвращает `cns_score`, `readiness_level`, `overload_target` |

### 1.6. Progress & Body Metrics (`/api/v1/progress`, `/api/v1/body`)
| Метод | Путь | Назначение | Описание логики |
| :--- | :--- | :--- | :--- |
| `GET` | `/progress/summary` | Сводка прогресса и рекордов | Общий тоннаж, число тренировок, серия, список `PersonalRecord` |
| `GET` | `/body/measurements` | История замеров тела | Список замеров веса, роста, % жира и ИМТ |
| `POST` | `/body/measurements` | Запись нового замера веса | Req: `{ weight_kg, height_cm, body_fat_percent, measured_at }` |

---

## 2. Математические и Доменные Алгоритмы (Клиент + Сервер)

### 2.1. Строгая изоляция задач по датам (`Todo` Date Isolation)
Каждая задача (`TodoTask`) имеет:
- `id: String`
- `title: String`
- `target: String`
- `category: TodoCategory` (`WORKOUT`, `NUTRITION`, `RECOVERY`, `HABIT`)
- `priority: TodoPriority` (`HIGH`, `MEDIUM`, `LOW`)
- `isRecurringDaily: Boolean`
- `scheduledDate: String?` (`YYYY-MM-DD` для разовых задач)
- `completedDates: Set<String>` (набор дат `YYYY-MM-DD`, в которые задача была выполнена)

**Правило проверки выполнения на выбранную дату `selectedDate`:**
```kotlin
fun TodoTask.isCompletedOn(date: String): Boolean = completedDates.contains(date)
```
Благодаря этому:
1. Выполнение регулярной задачи `2026-10-25` добавляет `"2026-10-25"` в `completedDates`.
2. При наступлении `2026-10-26` `isCompletedOn("2026-10-26")` возвращает `false` — день начинается чисто, а прогресс за `2026-10-25` остаётся сохранённым в истории!

### 2.2. Расчёт реального недельного объёма (`weeklyVolumeBars`)
Вместо захардкоженных значений в `ProgressViewModel`:
- Берём текущую дату и рассчитываем 6 недельных интервалов (`W-5`, `W-4`, `W-3`, `W-2`, `W-1`, `Current`).
- Для каждого интервала суммируем `workout.totalVolumeKg` всех завершённых тренировок, у которых `startTimeEpochMs` попадает в границы соответствующей недели.

### 2.3. Расчёт гипертрофической нагрузки по мышцам (`muscleHypertrophySets`)
- Фильтруем завершённые тренировки за последние 7 суток (`now - 7 * 86_400_000L`).
- Для каждого упражнения считаем количество завершённых рабочих подходов (`set.isCompleted && set.type != "warmup"`).
- Группируем сумму подходов по `exercise.primaryMuscleGroup`.

### 2.4. Расчёт индекса готовности ЦНС (`CNS Readiness Score`)
- Вычисляется динамически из усталости за последние 72 часа:
  - Каждая тяжёлая тренировка за последние 24ч временно снижает базовый индекс (`100`) пропорционально выполненному тоннажу и числу отказных подходов (`type == "failure"`).
  - Выполнение задач восстановления (Водный баланс, Сон 8ч) в `Todo` за текущий/вчерашний день восстанавливает бонусные баллы готовности.

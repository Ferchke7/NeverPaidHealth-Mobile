package dev.neverpaid.health.core.di

import dev.neverpaid.health.data.repository.NeverPaidRepositoryImpl
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import dev.neverpaid.health.features.body.BodyViewModel
import dev.neverpaid.health.features.coach.CoachViewModel
import dev.neverpaid.health.features.dashboard.DashboardViewModel
import dev.neverpaid.health.features.exercises.ExercisesViewModel
import dev.neverpaid.health.features.history.HistoryViewModel
import dev.neverpaid.health.features.profile.ProfileViewModel
import dev.neverpaid.health.features.progress.ProgressViewModel
import dev.neverpaid.health.features.workout.WorkoutViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single<NeverPaidRepository> { NeverPaidRepositoryImpl() }

    viewModel { DashboardViewModel(get()) }
    viewModel { WorkoutViewModel(get()) }
    viewModel { ExercisesViewModel(get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { ProgressViewModel(get()) }
    viewModel { CoachViewModel(get()) }
    viewModel { BodyViewModel(get()) }
    viewModel { ProfileViewModel(get()) }
}

fun initKoin() {
    startKoin {
        modules(appModule)
    }
}

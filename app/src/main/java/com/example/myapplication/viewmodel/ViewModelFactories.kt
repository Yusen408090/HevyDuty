package com.example.myapplication.viewmodel

import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.myapplication.AppContainer

fun appFactory(container: AppContainer) = viewModelFactory { initializer { AppViewModel(container.repository) } }
fun catalogFactory(container: AppContainer) = viewModelFactory { initializer { CatalogViewModel(container.repository, createSavedStateHandle()) } }
fun routineFactory(container: AppContainer) = viewModelFactory { initializer { RoutineViewModel(container.repository, createSavedStateHandle()) } }
fun workoutFactory(container: AppContainer) = viewModelFactory { initializer { WorkoutViewModel(container.repository, container.location, createSavedStateHandle()) } }
fun progressFactory(container: AppContainer) = viewModelFactory { initializer { ProgressViewModel(container.repository, createSavedStateHandle()) } }
fun profileFactory(container: AppContainer) = viewModelFactory { initializer { ProfileViewModel(container.repository, container.photos, container.location) } }

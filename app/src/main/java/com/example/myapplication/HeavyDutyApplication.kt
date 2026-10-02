package com.example.myapplication

import android.app.Application
import androidx.room.Room
import com.example.myapplication.data.local.GymDatabase
import com.example.myapplication.data.repository.RoomGymRepository
import com.example.myapplication.domain.repository.GymRepository
import com.example.myapplication.utils.LocationClient
import com.example.myapplication.utils.PhotoStore

class HeavyDutyApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}

// Explicit constructor injection keeps this academic project small and testable.
class AppContainer(application: Application) {
    val database = Room.databaseBuilder(application, GymDatabase::class.java, "heavy-duty.db").build()
    val repository: GymRepository = RoomGymRepository(database)
    val photos = PhotoStore(application)
    val location = LocationClient(application)
}

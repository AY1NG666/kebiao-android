package com.ayng.kebiao

import android.app.Application
import com.ayng.kebiao.data.db.AppDatabase
import com.ayng.kebiao.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class KebiaoApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.build(this)
    }

    val repository by lazy {
        AppRepository(database.courseDao(), database.attendanceDao(), database.salaryRuleDao())
    }

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            repository.deduplicateAttendances()
        }
    }
}

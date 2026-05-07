package com.ayng.kebiao

import android.app.Application
import com.ayng.kebiao.data.db.AppDatabase
import com.ayng.kebiao.data.repository.AppRepository

class KebiaoApp : Application() {

    val database: AppDatabase by lazy {
        AppDatabase.build(this)
    }

    val repository by lazy {
        AppRepository(database.courseDao(), database.attendanceDao(), database.salaryRuleDao())
    }
}

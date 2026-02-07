package com.example.sunseeker_app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Event::class, User::class], version = 1, exportSchema = false)
abstract class SunSeekerDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: SunSeekerDatabase? = null

        fun getDatabase(context: Context): SunSeekerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SunSeekerDatabase::class.java,
                    "sunseeker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
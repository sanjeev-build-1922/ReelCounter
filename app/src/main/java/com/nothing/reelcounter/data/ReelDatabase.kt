package com.nothing.reelcounter.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ReelEntity::class],
    version = 1,
    exportSchema = true
)
abstract class ReelDatabase : RoomDatabase() {

    abstract fun reelDao(): ReelDao

    companion object {
        private const val DATABASE_NAME = "reel_counter_nothing.db"

        @Volatile
        private var INSTANCE: ReelDatabase? = null

        fun getInstance(context: Context): ReelDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ReelDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}

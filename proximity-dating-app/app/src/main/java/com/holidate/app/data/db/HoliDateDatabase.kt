package com.holidate.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

/** Serializes the interests list to a single JSON-array column. */
class Converters {
    @TypeConverter
    fun fromInterests(value: List<String>): String =
        org.json.JSONArray(value).toString()

    @TypeConverter
    fun toInterests(value: String): List<String> {
        if (value.isBlank()) return emptyList()
        val array = org.json.JSONArray(value)
        return List(array.length()) { array.getString(it) }
    }
}

@Database(
    entities = [
        ProfileEntity::class,
        SwipeEntity::class,
        IncomingLikeEntity::class,
        MatchEntity::class,
        ChatMessageEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class HoliDateDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun swipeDao(): SwipeDao
    abstract fun incomingLikeDao(): IncomingLikeDao
    abstract fun matchDao(): MatchDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var instance: HoliDateDatabase? = null

        fun get(context: Context): HoliDateDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    HoliDateDatabase::class.java,
                    "holidate.db",
                ).build().also { instance = it }
            }
    }
}

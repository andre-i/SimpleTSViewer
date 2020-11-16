package com.bezwolos.simplets.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bezwolos.simplets.data.Channel
import com.bezwolos.simplets.data.Field

@Database(entities = [Channel::class , Field::class], version = 1, exportSchema = false)
abstract class DatabaseSimpleTS : RoomDatabase() {

    abstract val channelsDao: ChannelsDao
    abstract val fieldsDao: FieldsDao

    companion object {

        @Volatile
        private var INSTANCE: DatabaseSimpleTS? = null

        fun getInstance(context: Context): DatabaseSimpleTS {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        DatabaseSimpleTS::class.java,
                        "simpleTS_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}








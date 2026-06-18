package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.FavoriteDao
import com.example.data.local.dao.NoteDao
import com.example.data.local.dao.PinDao
import com.example.data.local.dao.ProgressDao
import com.example.data.local.dao.DownloadDao
import com.example.data.local.entities.LocalBookProgress
import com.example.data.local.entities.LocalFavoriteBook
import com.example.data.local.entities.LocalPinnedBook
import com.example.data.local.entities.LocalUserNote
import com.example.data.local.entities.DownloadedBook

@Database(
    entities = [
        LocalBookProgress::class,
        LocalFavoriteBook::class,
        LocalPinnedBook::class,
        LocalUserNote::class,
        DownloadedBook::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun progressDao(): ProgressDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun pinDao(): PinDao
    abstract fun noteDao(): NoteDao
    abstract fun downloadDao(): DownloadDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "muslims_library_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

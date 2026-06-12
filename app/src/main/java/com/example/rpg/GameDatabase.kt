package com.example.rpg

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "game_save")
data class GameSave(
    @PrimaryKey val id: Int = 1,
    val stateJson: String,
    val lastSaved: Long = System.currentTimeMillis()
)

@Dao
interface GameSaveDao {
    @Query("SELECT * FROM game_save WHERE id = :id")
    fun getSaveById(id: Int): Flow<GameSave?>

    @Query("SELECT * FROM game_save WHERE id = :id")
    suspend fun getSaveByIdOneShot(id: Int): GameSave?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSave(save: GameSave)

    @Query("DELETE FROM game_save WHERE id = :id")
    suspend fun deleteSave(id: Int)
}

@Database(entities = [GameSave::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun gameSaveDao(): GameSaveDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "whatisrpg_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

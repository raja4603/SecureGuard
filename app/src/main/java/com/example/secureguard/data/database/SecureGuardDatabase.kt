package com.example.secureguard.data.database

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "threats")
data class Threat(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val appName: String,
    val packageName: String,
    val threatType: String,
    val riskLevel: RiskLevel,
    val description: String
)

@Entity(tableName = "whitelist")
data class WhitelistedApp(
    @PrimaryKey val packageName: String
)

enum class RiskLevel { HIGH, MEDIUM, LOW }

@Dao
interface ThreatDao {
    @Query("SELECT * FROM threats ORDER BY riskLevel ASC")
    fun getAllThreats(): Flow<List<Threat>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(threats: List<Threat>)

    @Query("DELETE FROM threats")
    suspend fun deleteAll()
}

@Dao
interface WhitelistDao {
    @Query("SELECT * FROM whitelist")
    fun getWhitelistedApps(): Flow<List<WhitelistedApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addToWhitelist(app: WhitelistedApp)

    @Delete
    suspend fun removeFromWhitelist(app: WhitelistedApp)
}

@Database(entities = [Threat::class, WhitelistedApp::class], version = 1, exportSchema = false)
abstract class SecureGuardDatabase : RoomDatabase() {
    abstract fun threatDao(): ThreatDao
    abstract fun whitelistDao(): WhitelistDao

    companion object {
        @Volatile
        private var INSTANCE: SecureGuardDatabase? = null

        fun getDatabase(context: Context): SecureGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SecureGuardDatabase::class.java,
                    "secureguard_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}


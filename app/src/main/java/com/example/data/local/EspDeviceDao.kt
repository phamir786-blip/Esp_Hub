package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.EspDevice
import kotlinx.coroutines.flow.Flow

@Dao
interface EspDeviceDao {
    @Query("SELECT * FROM esp_devices ORDER BY isOnline DESC, createdAt DESC")
    fun observeAllDevices(): Flow<List<EspDevice>>

    @Query("SELECT * FROM esp_devices ORDER BY createdAt DESC")
    suspend fun getAllDevicesOnce(): List<EspDevice>

    @Query("SELECT * FROM esp_devices WHERE LOWER(mdnsHostname) = LOWER(:hostname) LIMIT 1")
    suspend fun findByHostname(hostname: String): EspDevice?

    @Query("SELECT * FROM esp_devices WHERE id = :id LIMIT 1")
    suspend fun getDeviceById(id: Long): EspDevice?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDevice(device: EspDevice): Long

    @Update
    suspend fun updateDevice(device: EspDevice)

    @Query(
        """
        UPDATE esp_devices 
        SET isOnline = :isOnline, 
            lastResolvedIp = COALESCE(:resolvedIp, lastResolvedIp), 
            lastPingMs = :pingMs, 
            lastSeenEpochMs = :seenEpochMs 
        WHERE id = :id
        """
    )
    suspend fun updateConnectionStatus(
        id: Long,
        isOnline: Boolean,
        resolvedIp: String?,
        pingMs: Int?,
        seenEpochMs: Long
    )

    @Query("DELETE FROM esp_devices WHERE id = :id")
    suspend fun deleteDeviceById(id: Long)

    @Query("DELETE FROM esp_devices")
    suspend fun deleteAllDevices()

    @Query("SELECT COUNT(*) FROM esp_devices")
    suspend fun getDeviceCount(): Int
}

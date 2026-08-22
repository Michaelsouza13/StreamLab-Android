package com.streamlab.tv.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels")
    fun getAllChannels(): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE `group` = :group")
    fun getChannelsByGroup(group: String): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Query("DELETE FROM channels")
    suspend fun clearChannels()

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: Long): ChannelEntity?

    @Query("SELECT * FROM channels WHERE url = :url LIMIT 1")
    suspend fun getChannelByUrl(url: String): ChannelEntity?

    @androidx.room.Delete
    suspend fun deleteChannel(channel: ChannelEntity)

    @androidx.room.Update
    suspend fun updateChannel(channel: ChannelEntity)
}

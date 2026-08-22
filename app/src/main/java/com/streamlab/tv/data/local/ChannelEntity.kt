package com.streamlab.tv.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val logo: String,
    val url: String,
    val group: String,
    val isFavorite: Boolean = false
)

package com.nothing.reelcounter.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room Entity representing a recorded short-form video consumption event.
 *
 * Stores:
 * - [id]: Auto-generated primary key
 * - [timestamp]: Epoch milliseconds when the reel/short was consumed
 * - [platform]: Platform identifier ("INSTAGRAM" or "YOUTUBE")
 * - [count]: Number of reels recorded in this entry (defaults to 1 per swipe event)
 * - [dateKey]: Local ISO date string (YYYY-MM-DD) for fast daily indexing
 */
@Entity(
    tableName = "reel_logs",
    indices = [
        Index(value = ["timestamp"]),
        Index(value = ["date_key"]),
        Index(value = ["platform"])
    ]
)
data class ReelEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0L,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,

    @ColumnInfo(name = "platform")
    val platform: String,

    @ColumnInfo(name = "count")
    val count: Int = 1,

    @ColumnInfo(name = "date_key")
    val dateKey: String
)

data class PlatformCountTuple(
    @ColumnInfo(name = "platform")
    val platform: String,

    @ColumnInfo(name = "total")
    val total: Int
)

data class DailyCountTuple(
    @ColumnInfo(name = "date_key")
    val dateKey: String,

    @ColumnInfo(name = "total")
    val total: Int
)

package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sent_hugs")
data class SentHug(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val friendName: String,
    val hugMessage: String,
    val pandaEmoji: String,
    val timestamp: Long = System.currentTimeMillis()
)

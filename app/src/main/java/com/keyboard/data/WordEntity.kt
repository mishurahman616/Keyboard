package com.keyboard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class WordEntity(
    @PrimaryKey val word: String,
    val language: String,
    val frequency: Int = 1,
    val nextWordHint: String? = null
)

package com.keyboard.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(word: WordEntity)

    @Query("UPDATE words SET frequency = frequency + 1 WHERE word = :word")
    suspend fun incrementFrequency(word: String)

    @Query("SELECT * FROM words WHERE word LIKE :prefix || '%' ORDER BY frequency DESC LIMIT :limit")
    suspend fun findByPrefix(prefix: String, limit: Int): List<WordEntity>

    @Query("SELECT * FROM words WHERE nextWordHint = :word ORDER BY frequency DESC LIMIT :limit")
    suspend fun getNextWordCandidates(word: String, limit: Int): List<WordEntity>

    @Query("SELECT * FROM words")
    suspend fun getAll(): List<WordEntity>
}

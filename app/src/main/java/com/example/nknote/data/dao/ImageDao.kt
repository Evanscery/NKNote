package com.example.nknote.data.dao

import androidx.room.*
import com.example.nknote.data.entities.Image
import kotlinx.coroutines.flow.Flow

@Dao
interface ImageDao {
    @Query("SELECT * FROM images WHERE noteId = :noteId")
    fun getImagesForNote(noteId: String): Flow<List<Image>>

    @Query("SELECT * FROM images WHERE id = :id")
    fun getImageById(id: String): Flow<Image?>

    @Query("SELECT * FROM images WHERE syncStatus != 1")
    fun getUnsyncedImages(): Flow<List<Image>>

    @Query("SELECT * FROM images WHERE updatedAt > :lastSyncTime")
    fun getImagesUpdatedAfter(lastSyncTime: Long): Flow<List<Image>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImage(image: Image)

    @Update
    suspend fun updateImage(image: Image)

    @Delete
    suspend fun deleteImage(image: Image)

    @Query("DELETE FROM images WHERE id = :id")
    suspend fun deleteImageById(id: String)

    @Query("DELETE FROM images WHERE noteId = :noteId")
    suspend fun deleteAllImagesForNote(noteId: String)
} 
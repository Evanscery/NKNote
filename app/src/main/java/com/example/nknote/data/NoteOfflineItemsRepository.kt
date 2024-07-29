package com.example.nknote.data

import ItemsRepository
import kotlinx.coroutines.flow.Flow

class NoteOfflineItemsRepository(private val itemDao: NoteItemDao) : ItemsRepository {
    override fun getAllItemsStream(): Flow<List<NoteItem>> = itemDao.getAllItems()

    override fun getItemStream(id: Int): Flow<NoteItem?> = itemDao.getItem(id)

    override suspend fun insertItem(item: NoteItem) = itemDao.insert(item)

    override suspend fun deleteItem(item: NoteItem) = itemDao.delete(item)

    override suspend fun updateItem(item: NoteItem) = itemDao.update(item)

    override suspend fun getPictureString(id: Int):Flow<String?> = itemDao.getPicture(id)

    override suspend fun deleteById(id: Int) = itemDao.deleteById(id)
}
package com.example.nknote.data

import ItemsRepository
import android.util.Log
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.toList
import com.example.nknote.data.DataHandler.stringWithMD5
import kotlinx.coroutines.flow.count
import kotlinx.coroutines.flow.takeWhile

class NoteOfflineItemsRepository(private val itemDao: NoteItemDao) : ItemsRepository {
    override fun getAllItemsStream(): Flow<List<NoteItem>> = itemDao.getAllItems()

    override fun getItemStream(id: String): Flow<NoteItem?> = itemDao.getItem(id)

    override suspend fun insertItem(item: NoteItem)
    {
        var isSearching = true
        val searchForId = itemDao.getItem(item.id)
        searchForId.takeWhile{
            isSearching
        }
            .collect{
                value ->
            if(value == null)
            {
                itemDao.insert(item)
                isSearching = false
            }
            else
            {
                val newItem = item.copy(id=stringWithMD5("${item.title} ${item.date}"+"copy"))
                insertItem(newItem)
                isSearching = false
            }
        }
    }


    override suspend fun deleteItem(item: NoteItem) = itemDao.delete(item)

    override suspend fun updateItem(item: NoteItem) = itemDao.update(item)

    override suspend fun getPictureString(id: String):Flow<String?> = itemDao.getPicture(id)

    override suspend fun deleteById(id: String) = itemDao.deleteById(id)

}


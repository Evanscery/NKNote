import com.example.nknote.data.NoteItem
import kotlinx.coroutines.flow.Flow

/**
 * Repository that provides insert, update, delete, and retrieve of [Item] from a given data source.
 */
interface ItemsRepository {
    /**
     * Retrieve all the items from the the given data source.
     */
    fun getAllItemsStream(): Flow<List<NoteItem>>

    /**
     * Retrieve an item from the given data source that matches with the [id].
     */
    fun getItemStream(id: String): Flow<NoteItem?>

    /**
     * Insert item in the data source
     */
    suspend fun insertItem(item: NoteItem)

    /**
     * Delete item from the data source
     */
    suspend fun deleteItem(item: NoteItem)

    /**
     * Update item in the data source
     */
    suspend fun updateItem(item: NoteItem)

    /**
     * Retrieve the json string from the given data source that matches with the [id].
     */
    suspend fun getPictureString(id : String):Flow<String?>

    suspend fun deleteById(id : String)

}
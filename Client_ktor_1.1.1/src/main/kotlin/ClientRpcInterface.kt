import io.ktor.client.HttpClient
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async

interface ClientRpcInterface{
    var client : HttpClient?
    val messageList : MutableList<String>
    val replied : MutableList<String>
    var taskCounter : Int
    var countTask : Int
    suspend fun send(jsonToSend : String, id : Int) : String
    suspend fun close()
}
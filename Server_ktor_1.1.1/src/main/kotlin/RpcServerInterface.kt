import io.ktor.http.cio.websocket.WebSocketSession
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

interface RpcServerInterface{
    val userCounter : AtomicInteger
    val memberNames : ConcurrentHashMap<String, String>
    val members : ConcurrentHashMap<String, MutableList<WebSocketSession>>
    val lastMessages : LinkedList<String>

    suspend fun memberJoin(member : String, socket : WebSocketSession)
    suspend fun memberLeft(member: String, socket: WebSocketSession)
}
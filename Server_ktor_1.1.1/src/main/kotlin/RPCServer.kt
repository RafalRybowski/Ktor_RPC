import io.ktor.http.cio.websocket.WebSocketSession
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

class RPCServer : RpcServerInterface{
    val ERROR_PROCEDURE = "Procedure not found."
    val ERROR_PARSE_JSON_STRING = "Wrong Format of JSON"

    override val userCounter = AtomicInteger()
    override val memberNames = ConcurrentHashMap<String, String>()
    override val members = ConcurrentHashMap<String, MutableList<WebSocketSession>>()
    override val lastMessages = LinkedList<String>()
    override suspend fun memberJoin(member : String, socket : WebSocketSession){
        //val name = memberNames.computeIfAbsent(member){ "userId=${userCounter.incrementAndGet()}" }
        val list = members.computeIfAbsent(member) { CopyOnWriteArrayList<WebSocketSession>() }
        list.add(socket)

    }
    override suspend fun memberLeft(member: String, socket: WebSocketSession) {
        // Removes the socket connection for this member
        val connections = members[member]
        connections?.remove(socket)
    }
}
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.features.json.serializer.KotlinxSerializer
import io.ktor.client.features.websocket.WebSockets
import io.ktor.client.features.websocket.ws
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.serialization.json.JSON


@KtorExperimentalAPI
class ClientRPC(private val host: String = "", private val port: Int = 0, private val path: String = "") : ClientRpcInterface {
    override var client: HttpClient? = null
    override var messageList = mutableListOf<String>()
    override val replied = mutableListOf<String>()
    override var taskCounter: Int = 0
    override var countTask = 0
    var close = false
    init {
        client = HttpClient(CIO).config {
            install(WebSockets)
            install(JsonFeature) {
                serializer = KotlinxSerializer()
            }
        }
    }

    suspend fun connect() = GlobalScope.async {
        if (host == "" && port == 0 && path == "") throw Throwable("Cannot connect with server")
        client!!.ws(host = host, port = port, path = path) {
            while (true) {
                if (close) close(CloseReason(CloseReason.Codes.NORMAL, "Client said BYE"))
                if (countTask > 0) {
                    val frame = incoming.receive()
                    when (frame) {
                        is Frame.Text -> {
                            synchronized(this) {
                                replied.add(frame.readText())
                            }
                            countTask--
                        }
                    }
                }
                if (messageList.isNotEmpty()) {
                    outgoing.send(Frame.Text(messageList[0]))
                    synchronized(this) {
                        messageList.removeAt(0)
                    }
                    countTask++
                }
            }
        }
    }


    override suspend fun send(jsonToSend: String, id: Int): String {
        try{
            JSON.parse(RPCCall.serializer(), jsonToSend)
        } catch(t : Throwable){
            return ""
        }
        synchronized(this) {
            messageList.add(jsonToSend)
        }
        var result = ""
        var indexR = 0
        while (true) {
            var contains = false
            for ((index, item) in replied.withIndex()) {
                val message: RPCRespond = JSON.parse(RPCRespond.serializer(), item)
                if (message.id == id) {
                    contains = true
                    result = item
                    indexR = index
                }
            }
            if (contains) break
        }
        synchronized(this) {
            replied.removeAt(indexR)
        }
        return result
    }


    suspend fun add(firstNumber: Int, secoundNumber: Int): Int {
        val id = taskCounter++
        val model = RPCCall("2.0", id, "add", arrayListOf(firstNumber, secoundNumber))
        val result_message: RPCRespond = JSON.parse(
            RPCRespond.serializer(),
            send(JSON.stringify(RPCCall.serializer(), model), id)
        )
        taskCounter++
        return result_message.result
    }

    suspend fun sub(firstNumber: Int, secondNumber: Int): Int {
        val id = taskCounter++
        val model = RPCCall("2.0", id, "sub", arrayListOf(firstNumber, secondNumber))
        val result_message: RPCRespond = JSON.parse(
            RPCRespond.serializer(),
            send(JSON.stringify(RPCCall.serializer(), model), id)
        )
        return result_message.result
    }

    override suspend fun close() {
        close = true
        client!!.cancel()
    }
}

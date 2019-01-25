import io.ktor.application.Application
import io.ktor.application.ApplicationCallPipeline
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.DefaultHeaders
import io.ktor.http.cio.websocket.CloseReason
import io.ktor.http.cio.websocket.Frame
import io.ktor.http.cio.websocket.close
import io.ktor.http.cio.websocket.readText
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.sessions.*
import io.ktor.util.KtorExperimentalAPI
import io.ktor.util.generateNonce
import io.ktor.websocket.WebSockets
import io.ktor.websocket.webSocket
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import kotlinx.serialization.json.JSON
import java.time.Duration

fun main(args : Array<String>){
    embeddedServer(Netty,  8080, module = Application::main).start(wait = true)
}

fun Application.main(){
    RPCApplication().apply { main() }
}

class RPCApplication : RPCAplicationClass() {
    @KtorExperimentalAPI
    override fun Application.main(){
        install(DefaultHeaders)
        install(WebSockets){
            pingPeriod = Duration.ofSeconds(60)
            timeout = Duration.ofSeconds(15)
            masking = false
            maxFrameSize = Long.MAX_VALUE
        }

        install(Sessions){
            cookie<RPCSession>("SESSION")
        }

        intercept(ApplicationCallPipeline.Features){
            if(call.sessions.get<RPCSession>() == null){
               call.sessions.set(RPCSession(generateNonce()))
            }
        }
        routing{
            webSocket("/RPC"){
                println("onConnect")
                val session = call.sessions.get<RPCSession>()

                if(session == null){
                    close(CloseReason(CloseReason.Codes.VIOLATED_POLICY, "No session"))
                    return@webSocket
                }

                server.memberJoin(session.id, this)
                try{
                    incoming.consumeEach { frame ->
                        if(frame is Frame.Text){
                            println("onMessage")
                            outgoing.send(element = Frame.Text(receivedMessage(session.id, frame.readText())))
                        }
                    }
                } catch (e: ClosedReceiveChannelException) {
                    println("onClose ${closeReason.await()}")
                } catch (e: Throwable) {
                    println("onError ${closeReason.await()}")
                } finally {
                    println("onClose ${closeReason.await()}")
                    server.memberLeft(session.id, this)
                }
            }
        }
    }

    override suspend fun receivedMessage(id : String, command: String) : String  {
        try {
            val message : RPCCall = JSON.parse(RPCCall.serializer(), command)
            when(message.method){
                "add" -> {
                    val result = message.params[0] + message.params[1]
                    return JSON.stringify(RPCRespond.serializer(), RPCRespond( id = message.id, result = result))
                }
                "sub" -> {
                    val result = message.params[0] - message.params[1]
                    return JSON.stringify(RPCRespond.serializer(), RPCRespond(id = message.id, result = result))
                }
                else -> {
                    return JSON.stringify(RPCError.serializer(), RPCError(id = message.id, error = server.ERROR_PROCEDURE))
                }
            }
        } catch (t : Throwable){
            return JSON.stringify(RPCError.serializer(), RPCError(id = null, error = server.ERROR_PARSE_JSON_STRING))
        }
    }
}
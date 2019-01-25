import com.typesafe.config.ConfigException
import io.ktor.application.Application

open class RPCAplicationClass {
    protected data class RPCSession(val id : String)
    protected val server: RPCServer = RPCServer()
    open fun Application.main(){ throw Throwable("Error with Server") }
    open suspend fun receivedMessage(id : String, command: String) : String = throw Throwable("Error with Receiver Message")
}
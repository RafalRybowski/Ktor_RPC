import io.ktor.application.Application

interface RPCApplicationInterface{
    fun Application.main()
    suspend fun receivedMessage(id : String, command: String) : String
}
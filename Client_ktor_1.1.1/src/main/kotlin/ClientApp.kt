import io.ktor.util.KtorExperimentalAPI
import kotlinx.coroutines.*


@KtorExperimentalAPI
suspend fun main(args : Array<String>) {
    val client = ClientRPC("127.0.0.1", 8080, "/RPC")
    val job = GlobalScope.launch {
        client.connect()
        val rb = runBlocking {
            val a = async { client.add(4, 5) }
            val b = async { client.sub(10, 5) }
            println("Suma pierwszych dwoch liczb " + a.await() + " odejmowanie " + b.await())
            if(a.isCompleted && b.isCompleted) client.close()
        }
    }
    job.cancelAndJoin()
}



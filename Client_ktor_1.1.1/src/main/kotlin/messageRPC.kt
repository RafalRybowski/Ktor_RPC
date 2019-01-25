import kotlinx.serialization.Optional
import kotlinx.serialization.Serializable

@Serializable
data class RPCCall(@Optional val jsonrpc : String = "2.0-RPC", val id : Int, val method : String, val params : ArrayList<Int>)
@Serializable
data class RPCRespond(@Optional val jsonrpc : String = "2.0-RPC", val id : Int, val result : Int)
@Serializable
data class RPCError(@Optional val jsonrpc : String = "2.0-RPC", val id : Int?, val error : String)

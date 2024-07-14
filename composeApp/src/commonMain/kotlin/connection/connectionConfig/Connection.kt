package connection.connectionConfig

import com.typesafe.config.ConfigException.Null
import io.ktor.websocket.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.*

class Connection(val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(0)
    }
    val id = lastId.getAndIncrement()
    val sendQueue = LinkedBlockingQueue<String>()
    val receivedQueue = ConcurrentLinkedQueue<String>()
    var closeSession: Boolean = false
}
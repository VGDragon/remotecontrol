package connection.server


import io.ktor.websocket.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import java.time.*
import java.util.*

fun Application.configureSockets(
    websocketConnectionServer: WebsocketConnectionServer,
    port: Int = 8080
) {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    routing {
        webSocket("/") {
            val thisConnection = Connection(this)
            websocketConnectionServer.connections += thisConnection
            while (!thisConnection.closeSession) {
                try {
                    while (!incoming.isEmpty) {
                        val frame = incoming.receive()
                        println(frame)
                        frame as? Frame.Text ?: continue
                        val receivedText = frame.readText()
                        println("${thisConnection.id}: Received $receivedText")
                        thisConnection.receivedQueue.add(receivedText)
                    }
                } catch (e: Exception) {
                    println(e.localizedMessage)
                }

                while (thisConnection.sendQueue.isNotEmpty()) {
                    val message = thisConnection.sendQueue.poll()
                    println("${thisConnection.id}: Sending $message")
                    thisConnection.session.send(message)
                }
                try {
                    Thread.sleep(10)
                } catch (e: InterruptedException) {
                    break
                }
            }
            println("Removing $thisConnection!")
            websocketConnectionServer.connections -= thisConnection
            //thisConnection.session.close()
        }
    }
}
package connection.server


import GlobalVariables
import io.ktor.websocket.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import messages.WebsocketMessageClient
import java.time.*
import java.util.*

fun Application.configureSockets(
    websocketConnectionServer: WebsocketConnectionServer,
    port: Int = 8080
) {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds((GlobalVariables.pingPongDelayTime / 1000).toLong())
        timeout = Duration.ofSeconds((GlobalVariables.pingPongDelayTimeMax / 1000).toLong())
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    launch {
        while (true) {
            val messagesToHandle: MutableList<Connection> = mutableListOf()
            for (connection in websocketConnectionServer.connections) {
                if (connection.receivedQueue.isNotEmpty()) {
                    messagesToHandle.add(connection)
                }
            }
            if (messagesToHandle.isNotEmpty()) {
                for (connection in messagesToHandle) {
                    websocketConnectionServer.lastClientMessageTime[connection] = System.currentTimeMillis()
                    val message = connection.receivedQueue.remove()
                    websocketConnectionServer.handleMessage(connection, message)
                }
            } else {
                websocketConnectionServer.handleSoftwareUpdate()
            }
            val currentTime = System.currentTimeMillis()
            val clientsToRemove = mutableListOf<Connection>()
            websocketConnectionServer.lastClientMessageTime.forEach {
                if (currentTime - it.value > GlobalVariables.pingPongDelayTime * 4) {
                    clientsToRemove.add(it.key)
                    println("Server: ${it.key} - Ping Pong timeout")
                }
            }

            clientsToRemove.forEach {
                if (websocketConnectionServer.lastClientMessageTime[it] != null) {
                    websocketConnectionServer.lastClientMessageTime.remove(it)
                }
                val clientName = websocketConnectionServer.websocketClients.remove(it)
                if (clientName != null) {
                    if (websocketConnectionServer.clientTaskRunningPermission[clientName] != null) {
                        websocketConnectionServer.clientTaskRunningPermission.remove(clientName)
                    }
                }
                it.closeSession = true
            }

            try {
                delay(100)
            } catch (e: InterruptedException) {
                break
            }
        }
    }
    routing {
        webSocket("/") {
            val thisConnection = Connection(this)
            websocketConnectionServer.connections += thisConnection
            launch {
                while (!thisConnection.closeSession) {
                    while (thisConnection.sendQueue.isNotEmpty()) {
                        val message = thisConnection.sendQueue.poll()
                        // println("${thisConnection.id}: Sending $message")
                        thisConnection.session.send(message)
                    }
                    delay(100)
                }
            }


            for (frame in incoming) {
                frame as? Frame.Text ?: continue
                val receivedText = frame.readText()
                // println("${thisConnection.id}: Received $receivedText")
                thisConnection.receivedQueue.add(receivedText)
            }


            println("Removing $thisConnection!")
            websocketConnectionServer.connections -= thisConnection
            thisConnection.session.close()
            websocketConnectionServer.handleClientDisconnect(thisConnection)
        }
    }
}
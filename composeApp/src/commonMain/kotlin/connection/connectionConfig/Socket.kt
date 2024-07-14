package connection.connectionConfig


import connection.WebsocketConnectionServer
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
            run {
                try {
                    incoming.isEmpty
                    for (frame in incoming) {
                        frame as? Frame.Text ?: continue
                        val receivedText = frame.readText()
                        println("${thisConnection.id}: Received $receivedText")
                        thisConnection.receivedQueue.add(receivedText)
                    }
                } catch (e: Exception) {
                    println(e.localizedMessage)
                }
            }

            run {
                try {
                    for (message in thisConnection.sendQueue) {
                        println("${thisConnection.id}: Sending $message")
                        thisConnection.session.send(message)
                    }
                } catch (e: Exception) {
                    println(e.localizedMessage)
                } finally {
                    println("Removing $thisConnection!")
                    websocketConnectionServer.connections -= thisConnection
                }
            }
            run {
                while (!thisConnection.closeSession) {
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
                thisConnection.session.close()
            }
            run {
                while (true) {
                    println("Server: Handling messages")
                    val messagesToHandle: MutableList<Connection> = mutableListOf()
                    for (connection in websocketConnectionServer.connections) {
                        if (connection.receivedQueue.isNotEmpty()) {
                            messagesToHandle.add(connection)
                        }
                    }
                    if (messagesToHandle.isNotEmpty()) {
                        for (connection in messagesToHandle) {
                            val message = connection.receivedQueue.remove()
                            websocketConnectionServer.handleMessage(connection, message)
                            websocketConnectionServer.lastClientMessageTime[connection] = System.currentTimeMillis()
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
                        Thread.sleep(100)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            }
        }
    }


}
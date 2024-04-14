package connection

import filedata.ApplicationData
import messages.*
import messages.base.*
import messages.base.client.*
import messages.base.client.MessageClientClientList
import messages.base.server.MessageServerClientList
import org.java_websocket.WebSocket

class WebsocketServerMessageHandler(val applicationData: ApplicationData) {
    // can be used without creating an instance of the class
    fun handle(websocketConnectionServer: WebsocketConnectionServer, ws: WebSocket, message: WebsocketMessageClient) {
        if (message.apiKey != applicationData.apiKey) {
            println("Server: Invalid API key received")
            websocketConnectionServer.sendMessage(
                ws = ws,
                message = MessageServerResponseCode.toJson(ServerAnswerStatus.INVALID_API_KEY, "Invalid API key")
            )
            ws.close()
        }
        when (message.type) {
            "message" -> {
                println("Server: Message received")
                Thread.sleep(1000)
                websocketConnectionServer.sendMessage(
                    ws = ws,
                    message = MessageServerResponseCode.toJson(ServerAnswerStatus.OK, ""))
            }

            "error" -> {
                println("Server: Error received")
                Thread.sleep(1000)
            }
            "ping" -> {
                //println("Server: Ping received")
                Thread.sleep(1000)
                websocketConnectionServer.sendMessage(
                    ws = ws,
                    message = WebsocketMessageServer(
                        type = "pong",
                        sendFrom = "",
                        data = ""
                    ).toJson()
                )
            }
            MessageClientRegister.TYPE -> {
                val registerMessage = MessageClientRegister.fromJson(message.data)

                if (registerMessage.isExecutable) {
                    websocketConnectionServer.setClientRegisterItem(registerMessage.clientName, ws)
                }
                websocketConnectionServer.setClientConnectedItem(registerMessage.clientName, ws)
                websocketConnectionServer.sendMessage(
                    ws = ws,
                    message = MessageServerResponseCode.toJson(ServerAnswerStatus.OK, ""))
                println("Server: Client registered: ${registerMessage.clientName}")
            }

            MessageClientClientList.TYPE -> {
                val clientList = websocketConnectionServer.getClientRegisteredNames()
                val messageClientList = MessageServerClientList()
                messageClientList.clientNames = clientList.toMutableList()
                val clientListMessage = WebsocketMessageServer(
                    type = MessageServerClientList.TYPE,
                    sendFrom = message.sendFrom,
                    data = messageClientList.toJson()
                )
                websocketConnectionServer.sendMessage(
                    ws = ws,
                    message = clientListMessage.toJson())
            }

            MessageStartTask.TYPE -> {
                val connectedWs = websocketConnectionServer
                    .getClientRegisterItem(message.sendTo)
                if (connectedWs == null) {
                    println("Server: Client not found")
                    websocketConnectionServer.sendMessage(
                        ws = ws,
                        message = MessageServerResponseCode.toJson(ServerAnswerStatus.CLIENT_NOT_FOUND, "Client not found")
                    )
                    return
                }
                message.apiKey = ""
                websocketConnectionServer.sendMessage(
                    ws = connectedWs,
                    message = WebsocketMessageServer(
                        type = MessageStartTask.TYPE,
                        sendFrom = message.sendFrom, data = message.data
                    ).toJson()
                )
                println("Server: Task started")
                websocketConnectionServer.sendMessage(
                    ws = ws,
                    message = MessageServerResponseCode.toJson(ServerAnswerStatus.OK, ""))
            }

            MessageServerResponseCode.TYPE -> {

                val connectedWs = websocketConnectionServer
                    .getClientConnectedItem(message.sendTo)
                if (connectedWs == null) {
                    println("Server: Client not found")
                    websocketConnectionServer.sendMessage(
                        ws = ws,
                        message = MessageServerResponseCode.toJson(ServerAnswerStatus.CLIENT_NOT_FOUND, "Client not found")
                    )
                    return
                }
                websocketConnectionServer.sendMessage(
                    ws = connectedWs,
                    message = WebsocketMessageServer(
                        type = MessageServerResponseCode.TYPE,
                        sendFrom = message.sendFrom, data = message.data
                    ).toJson()
                )
                println("Server: Info received")
            }

            MessageClientScriptList.TYPE -> {
                val connectedWs = websocketConnectionServer
                    .getClientRegisterItem(message.sendTo)
                if (connectedWs == null) {
                    println("Server: Client not found")
                    websocketConnectionServer.sendMessage(
                        ws = ws,
                        message = MessageServerResponseCode.toJson(ServerAnswerStatus.CLIENT_NOT_FOUND, "Client not found")
                    )
                    return
                }
                message.apiKey = ""
                websocketConnectionServer.sendMessage(
                    ws = connectedWs,
                    message = WebsocketMessageServer(
                        type = message.type,
                        sendFrom = message.sendFrom, data = message.data
                    ).toJson()
                )
            }

            else -> {
                println("Server: Unknown message type received")
            }
        }
    }
}
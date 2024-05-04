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
    fun handle(
        websocketConnectionServer: WebsocketConnectionServer,
        ws: WebSocket,
        message: WebsocketMessageClient): String? {
        if (message.apiKey != applicationData.apiKey) {
            println("Server: Invalid API key received")
            //ws.close()
            return null
        }
        when (message.type) {
            "message" -> {
                println("Server: Message received")
                return MessageServerResponseCode.toJson(ServerAnswerStatus.OK, "")
            }

            "error" -> {
                println("Server: Error received")
                return null
            }
            "ping" -> {
                println("Server: Ping received")
                return WebsocketMessageServer(
                        type = "pong",
                        sendFrom = "",
                        data = ""
                    ).toJson()
            }
            MessageClientRegister.TYPE -> {
                val registerMessage = MessageClientRegister.fromJson(message.data)

                if (registerMessage.isExecutable) {
                    websocketConnectionServer.setClientRegisterItem(registerMessage.clientName, ws)
                }
                websocketConnectionServer.setClientConnectedItem(registerMessage.clientName, ws)
                println("Server: Client registered: ${registerMessage.clientName}")
                return MessageServerResponseCode.toJson(ServerAnswerStatus.OK, "")
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
                return clientListMessage.toJson()
            }

            MessageStartTask.TYPE -> {
                val connectedWs = websocketConnectionServer
                    .getClientRegisterItem(message.sendTo)
                if (connectedWs == null) {
                    println("Server: Client not found")
                    return  MessageServerResponseCode.toJson(
                        ServerAnswerStatus.CLIENT_NOT_FOUND,
                        "Client not found")

                }
                message.apiKey = ""
                val messageToSend = WebsocketMessageServer(
                    type = MessageStartTask.TYPE,
                    sendFrom = message.sendFrom, data = message.data
                ).toJson()
                websocketConnectionServer.setWaitingForClient(
                    clientName = message.sendTo,
                    message = messageToSend)
                websocketConnectionServer.sendMessage(
                    ws = connectedWs,
                    message = messageToSend)

                println("Server: Task started")
                return MessageServerResponseCode.toJson(ServerAnswerStatus.OK, "")
            }

            MessageServerResponseCode.TYPE -> {

                val connectedWs = websocketConnectionServer
                    .getClientConnectedItem(message.sendTo)
                if (connectedWs == null) {
                    println("Server: Client not found")
                    return MessageServerResponseCode.toJson(
                        ServerAnswerStatus.CLIENT_NOT_FOUND,
                        "Client not found")

                }
                println("Server: Info received")
                return WebsocketMessageServer(
                        type = MessageServerResponseCode.TYPE,
                        sendFrom = message.sendFrom, data = message.data
                    ).toJson()

            }

            MessageClientScriptList.TYPE -> {
                val connectedWs = websocketConnectionServer
                    .getClientRegisterItem(message.sendTo)
                if (connectedWs == null) {
                    println("Server: Client not found")
                    return MessageServerResponseCode.toJson(
                        ServerAnswerStatus.CLIENT_NOT_FOUND,
                        "Client not found")

                }
                message.apiKey = ""
                return WebsocketMessageServer(
                        type = message.type,
                        sendFrom = message.sendFrom, data = message.data
                    ).toJson()

            }
            MessageClientUpdate.TYPE -> {
                val messageClientUpdate = MessageClientUpdate.fromJson(message.data)
                if (messageClientUpdate.updateOk) {
                    websocketConnectionServer.setClientUpdateStatus(ws, 1)
                } else {
                    websocketConnectionServer.setClientUpdateStatus(ws, -1)
                }
                return null
            }

            else -> {
                println("Server: Unknown message type received")
                return null
            }
        }
    }
}
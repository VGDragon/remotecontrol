package connection

import ApplicationData
import messages.*
import messages.base.*
import messages.base.client.*
import messages.base.server.MessageServerBridgedClients
import messages.base.client.MessageClientClientList
import messages.base.server.MessageServerClientList
import org.java_websocket.WebSocket

class WebsocketServerMessageHandler(val applicationData: ApplicationData) {
    // can be used without creating an instance of the class
    fun handle(websocketConnectionServer: WebsocketConnectionServer, ws: WebSocket, message: WebsocketMessageClient) {
        if (message.apiKey != applicationData.apiKey) {
            println("Server: Invalid API key received")
            ws.send(MessageServerResponseCode.toJson(ServerAnswerStatus.INVALID_API_KEY, "Invalid API key"))
            ws.close()
        }
        when (message.type) {
            "ping" -> {
                println("Server: Ping received")
                Thread.sleep(1000)
                ws.send(WebsocketMessageServer(type = "pong", data = "").toJson())
            }
            "pong" -> {
                println("Server: Pong received")
                Thread.sleep(1000)
                ws.send(WebsocketMessageServer(type = "ping", data = "").toJson())
            }
            "message" -> {
                println("Server: Message received")
                Thread.sleep(1000)
                ws.send(MessageServerResponseCode.toJson(ServerAnswerStatus.OK, ""))
            }
            "error" -> {
                println("Server: Error received")
                Thread.sleep(1000)
            }
            MessageClientRegister.TYPE -> {
                val registerMessage = MessageClientRegister.fromJson(message.data)
                websocketConnectionServer.setClientRegisterItem(registerMessage.clientName, ws)
                println("Server: Client registered: ${registerMessage.clientName}")
            }
            MessageClientConnect.TYPE -> {
                val messageClientConnect = MessageClientConnect.fromJson(message.data)
                val connectedWs = websocketConnectionServer
                    .getClientRegisterItem(messageClientConnect.clientName)
                if (connectedWs == null) {
                    println("Server: Client not found")
                    return
                }
                websocketConnectionServer.setClientBridgeItem(ws, connectedWs)
                websocketConnectionServer.setClientBridgeItem(connectedWs, ws)
                println("Server: Client connected: ${messageClientConnect.clientName}")
                ws.send(MessageServerResponseCode.toJson(ServerAnswerStatus.OK, ""))
            }
            MessageClientDisconnect.TYPE -> {
                val messageClientConnect = MessageClientConnect.fromJson(message.data)
                val connectedWs = websocketConnectionServer
                    .getClientRegisterItem(messageClientConnect.clientName)
                if (connectedWs == null) {
                    println("Server: Client not found")
                    ws.send(MessageServerResponseCode.toJson(ServerAnswerStatus.CLIENT_NOT_FOUND, "Client not found"))
                    return
                }
                websocketConnectionServer.removeClientBridgeItem(ws)
                println("Server: Client disconnected: ${messageClientConnect.clientName}")
                ws.send(MessageServerResponseCode.toJson(ServerAnswerStatus.OK, ""))
            }
            MessageClientClientList.TYPE -> {
                val clientList = websocketConnectionServer.getClientNames()
                val messageClientList = MessageServerClientList()
                messageClientList.clientNames = clientList.toMutableList()
                val clientListMessage = WebsocketMessageServer(
                    type = MessageServerClientList.TYPE,
                    data = messageClientList.toJson()
                )
                ws.send(clientListMessage.toJson())
            }
            MessageClientBridgedClients.TYPE -> {
                val connectedWs = websocketConnectionServer
                    .getClientBridgeItem(ws)
                if (connectedWs == null) {
                    println("Server: Client not found")
                    ws.send(MessageServerResponseCode.toJson(ServerAnswerStatus.CLIENT_NOT_FOUND, "Client not found"))
                    return
                }
                val bridgedClientName = websocketConnectionServer.getClientRegisterNameFromWs(connectedWs)
                if (bridgedClientName == null) {
                    println("Server: Client not found")
                    ws.send(MessageServerResponseCode.toJson(ServerAnswerStatus.CLIENT_NOT_FOUND, "Client not found"))
                    return
                }
                val messageClientBridgedClients = WebsocketMessageServer(
                    type = MessageServerBridgedClients.TYPE,
                    data = MessageServerBridgedClients(clientName = bridgedClientName).toJson()
                )
                ws.send(messageClientBridgedClients.toJson())
            }
            MessageStartTask.TYPE -> {
                val connectedWs = websocketConnectionServer
                    .getClientBridgeItem(ws)
                if (connectedWs == null) {
                    println("Server: Client not found")
                    ws.send(MessageServerResponseCode.toJson(ServerAnswerStatus.CLIENT_NOT_FOUND, "Client not found"))
                    return
                }
                message.apiKey = ""
                connectedWs.send(WebsocketMessageServer(type = MessageStartTask.TYPE, data = message.data).toJson())
                println("Server: Task started")
                ws.send(MessageServerResponseCode.toJson(ServerAnswerStatus.OK, ""))
            }
            MessageServerResponseCode.TYPE -> {

                val connectedWs = websocketConnectionServer
                    .getClientBridgeItem(ws)
                if (connectedWs == null) {
                    println("Server: Client not found")
                    ws.send(MessageServerResponseCode.toJson(ServerAnswerStatus.CLIENT_NOT_FOUND, "Client not found"))
                    return
                }
                connectedWs.send(WebsocketMessageServer(type = MessageServerResponseCode.TYPE, data = message.data).toJson())
                println("Server: Info received")
            }
            else -> {
                println("Server: Unknown message type received")
            }
        }
    }
}
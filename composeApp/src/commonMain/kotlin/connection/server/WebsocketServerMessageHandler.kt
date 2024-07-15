package connection.server

import GlobalVariables
import filedata.ApplicationData
import messages.*
import messages.base.*
import messages.base.client.*
import messages.base.client.MessageClientClientList
import messages.base.server.MessageServerClientList
import messages.base.server.MessageServerRegister

class WebsocketServerMessageHandler(val applicationData: ApplicationData) {
    // can be used without creating an instance of the class
    fun handle(
        websocketConnectionServer: WebsocketConnectionServer,
        ws: Connection,
        message: WebsocketMessageClient
    ) {
        if (message.apiKey != applicationData.apiKey) {
            println("Server: Invalid API key received")
            //ws.close()
            return
        }
        when (message.type) {
            "message" -> {
                println("Server: Message received")
                websocketConnectionServer.sendMessage(
                    ws = ws,
                    message = WebsocketMessageServer(
                        type = MessageServerResponseCode.TYPE,
                        sendFrom = GlobalVariables.computerName,
                        data = MessageServerResponseCode(
                            status = ServerAnswerStatus.OK,
                            message = ""
                        ).toJson()
                    ).toJson()
                )
            }

            "error" -> {
                println("Server: Error received")
                return
            }

            "ping" -> {
                //println("Server: Ping received")
                websocketConnectionServer.sendMessage(
                    ws = ws,
                    message = WebsocketMessageServer(
                        type = "pong",
                        sendFrom = GlobalVariables.computerName,
                        data = ""
                    ).toJson()
                )
                return
            }

            MessageClientRegister.TYPE -> {
                val registerMessage = MessageClientRegister.fromJson(message.data)

                if (registerMessage.isExecutable) {
                    websocketConnectionServer.clientTaskRunningPermission[registerMessage.clientName] = ws
                }
                websocketConnectionServer.websocketClients[ws] = registerMessage.clientName
                println("Server: Client registered: ${registerMessage.clientName}")
                websocketConnectionServer.sendMessage(
                    ws = ws,
                    message = WebsocketMessageServer(
                        type = MessageServerRegister.TYPE,
                        sendFrom = GlobalVariables.computerName,
                        data = MessageServerRegister(
                            clientName = registerMessage.clientName,
                            isExecutable = registerMessage.isExecutable,
                            registered = true
                        ).toJson()
                    ).toJson()
                )
                return
            }

            MessageClientClientList.TYPE -> {
                val clientList = websocketConnectionServer.clientTaskRunningPermission.keys.toList()
                val messageClientList = MessageServerClientList()
                messageClientList.clientNames = clientList.toMutableList()

                websocketConnectionServer.sendMessage(
                    ws = ws,
                    message = WebsocketMessageServer(
                        type = MessageServerClientList.TYPE,
                        sendFrom = message.sendFrom,
                        data = messageClientList.toJson()
                    ).toJson()
                )
                return
            }

            MessageStartTask.TYPE -> {
                val connectedWs = websocketConnectionServer
                    .clientTaskRunningPermission[message.sendTo]
                if (connectedWs == null) {
                    println("Server: Client not found")
                    websocketConnectionServer.sendMessage(
                        ws = ws,
                        message = WebsocketMessageServer(
                            type = MessageServerResponseCode.TYPE,
                            sendFrom = GlobalVariables.computerName,
                            data = MessageServerResponseCode(
                                status = ServerAnswerStatus.CLIENT_NOT_FOUND,
                                message = "Client not found"
                            ).toJson()
                        ).toJson()
                    )
                    return
                }
                //val messageToSend = WebsocketMessageServer(
                //    type = MessageStartTask.TYPE,
                //    sendFrom = message.sendFrom, data = message.data
                //).toJson()
                //websocketConnectionServer.waitingForClientList[message.sendTo] = messageToSend
                websocketConnectionServer.sendMessage(
                    ws = connectedWs,
                    message = WebsocketMessageServer(
                        type = MessageStartTask.TYPE,
                        sendFrom = message.sendFrom,
                        data = message.data
                    ).toJson()
                )
                println("Server: Task started")

                websocketConnectionServer.sendMessage(
                    ws = ws,
                    message = WebsocketMessageServer(
                        type = MessageServerResponseCode.TYPE,
                        sendFrom = GlobalVariables.computerName,
                        data = MessageServerResponseCode(
                            status = ServerAnswerStatus.OK,
                            message = ""
                        ).toJson()
                    ).toJson()
                )
                return
            }

            MessageServerResponseCode.TYPE -> {
                val sendToWs = websocketConnectionServer.websocketClients.firstNotNullOfOrNull {
                    if (it.value == message.sendTo) {
                        it.key
                    } else {
                        null
                    }
                }
                if (sendToWs == null) {
                    println("Server: Client not found")

                    websocketConnectionServer.sendMessage(
                        ws = ws,
                        message = WebsocketMessageServer(
                            type = MessageServerResponseCode.TYPE,
                            sendFrom = GlobalVariables.computerName,
                            data = MessageServerResponseCode(
                                status = ServerAnswerStatus.CLIENT_NOT_FOUND,
                                message = "Client not found"
                            ).toJson()
                        ).toJson()
                    )
                    return
                }

                println("Server: Info received")

                websocketConnectionServer.sendMessage(
                    ws = sendToWs,
                    message = WebsocketMessageServer(
                        type = MessageServerResponseCode.TYPE,
                        sendFrom = GlobalVariables.computerName,
                        data = message.data
                    ).toJson()
                )
                return
            }

            MessageClientScriptList.TYPE -> {
                val connectedWs = websocketConnectionServer
                    .clientTaskRunningPermission[message.sendTo]
                if (connectedWs == null) {
                    println("Server: Client not found")
                    websocketConnectionServer.sendMessage(
                        ws = ws,
                        message = WebsocketMessageServer(
                            type = MessageServerResponseCode.TYPE,
                            sendFrom = GlobalVariables.computerName,
                            data = MessageServerResponseCode(
                                status = ServerAnswerStatus.CLIENT_NOT_FOUND,
                                message = "Client not found"
                            ).toJson()
                        ).toJson()
                    )
                    return
                }
                message.apiKey = ""

                websocketConnectionServer.sendMessage(
                    ws = connectedWs,
                    message = WebsocketMessageServer(
                        type = message.type,
                        sendFrom = message.sendFrom,
                        data = message.data
                    ).toJson()
                )
                return
            }

            MessageClientUpdate.TYPE -> {
                val messageClientUpdate = MessageClientUpdate.fromJson(message.data)
                if (messageClientUpdate.updateFileDownloaded) {
                    websocketConnectionServer.clientUpdateDoneNames[message.sendFrom] = 1
                    return
                }
                if (messageClientUpdate.updateOk) {
                    websocketConnectionServer.clientUpdateDoneNames[message.sendFrom] = 1
                    return
                }
                websocketConnectionServer.clientUpdateDoneNames[message.sendFrom] = -1
                return
            }

            else -> {
                println("Server: Unknown message type received")
                return
            }
        }
    }
}
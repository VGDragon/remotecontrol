package connection.client

import filedata.ApplicationData
import GlobalVariables
import TaskFunctions
import filedata.SoftwareUpdate
import filedata.UpdateStatus
import messages.*
import messages.base.*
import messages.base.client.MessageClientScriptList
import messages.base.client.MessageClientUpdate
import messages.base.server.MessageServerClientList
import messages.base.server.MessageServerRegister
import messages.base.server.MessageServerUpdate
import java.io.File

class WebsocketClientMessageHandler(val applicationData: ApplicationData) {
    // can be used without creating an instance of the class
    fun handle(ws: WebsocketConnectionClient, message: WebsocketMessageServer) {
        when (message.type) {

            "message" -> {
                println("Client: Message received")
                return
            }

            "error" -> {
                println("Client: Error received")
                return
            }

            "pong" -> {
                //println("Client: Pong received")
                return
            }

            MessageServerClientList.TYPE -> {
                println("Client: List received")

                ws.execClientList.clear()
                ws.execClientList.addAll(MessageServerClientList.fromJson(message.data).clientNames)
                ws.serverInfoList.add(MessageServerResponseCode(ServerAnswerStatus.OK, ""))
                return
            }

            MessageServerResponseCode.TYPE -> {
                println("Client: Info received")
                ws.serverInfoList.add(MessageServerResponseCode.fromJson(message.data))
                return
            }

            MessageServerRegister.TYPE -> {
                println("Client: Register received")
                val messageServerRegister = MessageServerRegister.fromJson(message.data)
                ws.computerName = messageServerRegister.clientName

                ws.isRegistered = messageServerRegister.registered
                if (!ws.isRegistered) {
                    ws.isConnectionError = true
                }
                return
            }

            MessageStartTask.TYPE -> {
                if (TaskFunctions.startTaskHandler(ws, MessageStartTask.fromJson(message.data), message.sendFrom)) {
                    println("Client: Task started")
                    ws.sendMessage(
                        message = WebsocketMessageClient(
                            type = MessageServerResponseCode.TYPE,
                            apiKey = ws.applicationData.apiKey,
                            sendFrom = ws.computerName,
                            sendTo = message.sendFrom,
                            data = MessageServerResponseCode(ServerAnswerStatus.OK, "").toJson()
                        ).toJson()
                    )
                } else {
                    println("Client: Task not started")
                    ws.sendMessage(
                        message = WebsocketMessageClient(
                            type = MessageServerResponseCode.TYPE,
                            apiKey = ws.applicationData.apiKey,
                            sendFrom = ws.computerName,
                            sendTo = message.sendFrom,
                            data = MessageServerResponseCode(ServerAnswerStatus.ERROR, "Task not started.").toJson()
                        ).toJson()
                    )
                }
            }

            MessageClientScriptList.TYPE -> {
                println("Client: Script list send")
                ws.sendMessage(
                    message = WebsocketMessageClient(
                        type = MessageServerResponseCode.TYPE,
                        apiKey = ws.applicationData.apiKey,
                        sendFrom = ws.computerName,
                        sendTo = message.sendFrom,
                        data = MessageServerResponseCode(ServerAnswerStatus.OK, getScriptList()).toJson()
                    ).toJson()
                )
            }

            MessageServerUpdate.TYPE -> {
                if (!ws.executeTask) {
                    return
                }
                val messageServerUpdate = MessageServerUpdate.fromJson(message.data)
                if (messageServerUpdate.restart) {
                    if (ws.softwareUpdate == null) {
                        ws.softwareUpdate = SoftwareUpdate.fromServerMessage(messageServerUpdate)
                    }
                    ws.softwareUpdate!!.startUpdate()
                    println("Client: Restarting")
                    return
                }
                if (ws.softwareUpdate == null) {
                    ws.softwareUpdate = SoftwareUpdate.fromServerMessage(messageServerUpdate)
                }
                var softwareDataExists = false
                val softwareUpdateData =
                    ws.softwareUpdateDataList.filter { it.version.equals(messageServerUpdate.version) }
                if (softwareUpdateData.isNotEmpty()) {
                    if (softwareUpdateData[0].updateStatus == UpdateStatus.FINISHED) {
                        softwareDataExists = true
                    }
                }
                if (softwareDataExists) {
                    println("Update already downloaded for version ${messageServerUpdate.version}")
                    ws.sendMessage(
                        message = WebsocketMessageClient(
                            type = MessageClientUpdate.TYPE,
                            apiKey = ws.applicationData.apiKey,
                            sendFrom = ws.computerName,
                            sendTo = message.sendFrom,
                            data = MessageClientUpdate(
                                version = ws.softwareUpdate!!.version,
                                hash = ws.softwareUpdate!!.hashValue,
                                updateOk = true,
                                updateFileDownloaded = true
                            ).toJson()
                        ).toJson()
                    )
                    return
                }
                ws.softwareUpdate!!.writeFilePart(messageServerUpdate)
                if (ws.softwareUpdate!!.updateStatus == UpdateStatus.RUNNING) {
                    return
                }
                val isUpdateFileOk = ws.softwareUpdate!!.checkUpdateFile()
                if (!isUpdateFileOk) {
                    println("Client: Update error")
                    ws.updatePackageNrs = 0L
                    ws.softwareUpdate = null
                }

                ws.sendMessage(
                    message = WebsocketMessageClient(
                        type = MessageClientUpdate.TYPE,
                        apiKey = ws.applicationData.apiKey,
                        sendFrom = ws.computerName,
                        sendTo = message.sendFrom,
                        data = MessageClientUpdate(
                            version = ws.softwareUpdate!!.version,
                            hash = ws.softwareUpdate!!.hashValue,
                            updateOk = isUpdateFileOk
                        ).toJson()
                    ).toJson()
                )

            }

            else -> {
                println("Client: Unknown message type received")
                return
            }
        }
    }

    fun getScriptList(): List<String> {
        val scriptFolderFile = File(GlobalVariables.scriptFolder())
        if (!scriptFolderFile.exists()) {
            return listOf()
        }
        if (!scriptFolderFile.isDirectory) {
            return listOf()
        }
        val scriptList = mutableListOf<String>()
        for (file in scriptFolderFile.listFiles()) {
            if (!file.isFile) {
                continue
            }
            if (!isScriptFile(file)) {
                continue
            }
            scriptList.add(file.name)
        }
        return scriptList
    }

    fun isScriptFile(file: File): Boolean {
        val osName = GlobalVariables.pcOS()
        if (osName == OsType.WINDOWS) {
            return file.extension == "bat"
        } else if (osName == OsType.LINUX) {
            return file.extension == "sh"
        } else if (osName == OsType.MAC) {
            return file.extension == "sh"
        } else {
            println("Unknown OS")
        }
        return file.extension == "bat"
    }
}
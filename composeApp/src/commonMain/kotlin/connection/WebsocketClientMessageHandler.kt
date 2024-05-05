package connection

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
import messages.base.server.MessageServerUpdate
import java.io.File

class WebsocketClientMessageHandler(val applicationData: ApplicationData){
    // can be used without creating an instance of the class
    fun handle(ws: WebsocketConnectionClient, message: WebsocketMessageServer): String? {
        when (message.type) {

            "message" -> {
                println("Client: Message received")
                return null
            }

            "error" -> {
                println("Client: Error received")
                return null
            }
            "pong" -> {
                //println("Client: Pong received")
                return null
            }

            MessageServerClientList.TYPE -> {
                println("Client: List received")
                ws.setExecClientListVariable(MessageServerClientList.fromJson(message.data).clientNames)
                ws.addServerInfo(MessageServerResponseCode(ServerAnswerStatus.OK, ""))
                return null
            }

            MessageServerResponseCode.TYPE -> {
                println("Client: Info received")
                ws.addServerInfo(MessageServerResponseCode.fromJson(message.data))
                return null
            }

            MessageStartTask.TYPE -> {
                if(TaskFunctions.startTaskHandler(ws, MessageStartTask.fromJson(message.data), message.sendFrom)) {
                    println("Client: Task started")
                    return WebsocketMessageClient(
                        type = MessageServerResponseCode.TYPE,
                        apiKey = ws.applicationData.apiKey,
                        sendFrom = ws.computerName,
                        sendTo = message.sendFrom,
                        data = MessageServerResponseCode(ServerAnswerStatus.OK, "").toJson()
                    ).toJson()
                } else {
                    println("Client: Task not started")
                    return WebsocketMessageClient(
                        type = MessageServerResponseCode.TYPE,
                        apiKey = ws.applicationData.apiKey,
                        sendFrom = ws.computerName,
                        sendTo = message.sendFrom,
                        data = MessageServerResponseCode(ServerAnswerStatus.ERROR, "Task not started.").toJson()
                    ).toJson()
                }
            }
            MessageClientScriptList.TYPE -> {
                println("Client: Script list send")
                return WebsocketMessageClient(
                    type = MessageServerResponseCode.TYPE,
                    apiKey = ws.applicationData.apiKey,
                    sendFrom = ws.computerName,
                    sendTo = message.sendFrom,
                    data = MessageServerResponseCode(ServerAnswerStatus.OK, getScriptList()).toJson()
                ).toJson()
            }
            MessageServerUpdate.TYPE -> {
                if (!ws.executeTask){
                    return null
                }
                val messageServerUpdate = MessageServerUpdate.fromJson(message.data)
                if (messageServerUpdate.restart){
                    if (ws.softwareUpdate == null){
                        ws.softwareUpdate = SoftwareUpdate.fromServerMessage(messageServerUpdate)
                    }
                    ws.softwareUpdate!!.startUpdate()
                    println("Client: Restarting")
                    return null
                }
                synchronized(ws.updateDataLock){
                    if (ws.softwareUpdate == null){
                        ws.softwareUpdate = SoftwareUpdate.fromServerMessage(messageServerUpdate)
                    }
                    if (ws.doSoftwareUpdateDataExist(messageServerUpdate.version)){
                        println("Update already installed for version ${messageServerUpdate.version}")
                        return null
                    }
                    ws.softwareUpdate!!.writeFilePart(messageServerUpdate)
                    if (ws.softwareUpdate!!.updateStatus == UpdateStatus.RUNNING){
                        return null
                    }
                    ws.softwareUpdate!!.checkUpdateFile(ws, message.sendFrom)
                    return null
                }
            }

            else -> {
                println("Client: Unknown message type received")
                return null
            }
        }
    }

    fun getScriptList() : List<String> {
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
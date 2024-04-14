package connection

import filedata.ApplicationData
import GlobalVariables
import TaskFunctions
import filedata.SoftwareUpdate
import filedata.UpdateStatus
import messages.*
import messages.base.*
import messages.base.client.MessageClientScriptList
import messages.base.server.MessageServerClientList
import messages.base.server.MessageServerUpdate
import java.io.File

class WebsocketClientMessageHandler(val applicationData: ApplicationData) {
    // can be used without creating an instance of the class
    fun handle(ws: WebsocketConnectionClient, message: WebsocketMessageServer) {
        when (message.type) {

            "message" -> {
                println("Client: Message received")
            }

            "error" -> {
                println("Client: Error received")
            }
            "pong" -> {
                //println("Client: Pong received")
            }

            MessageServerClientList.TYPE -> {
                println("Client: List received")
                ws.setExecClientListVariable(MessageServerClientList.fromJson(message.data).clientNames)
                ws.addServerInfo(MessageServerResponseCode(ServerAnswerStatus.OK, ""))
            }

            MessageServerResponseCode.TYPE -> {
                println("Client: Info received")
                ws.addServerInfo(MessageServerResponseCode.fromJson(message.data))
            }

            MessageStartTask.TYPE -> {
                if(TaskFunctions.startTaskHandler(ws, MessageStartTask.fromJson(message.data), message.sendFrom)) {
                    println("Client: Task started")
                    ws.sendMessage(WebsocketMessageClient(
                        type = MessageServerResponseCode.TYPE,
                        apiKey = ws.applicationData.apiKey,
                        sendFrom = ws.computerName,
                        sendTo = message.sendFrom,
                        data = MessageServerResponseCode(ServerAnswerStatus.OK, "").toJson()
                    ).toJson())
                } else {
                    println("Client: Task not started")
                    ws.sendMessage(WebsocketMessageClient(
                        type = MessageServerResponseCode.TYPE,
                        apiKey = ws.applicationData.apiKey,
                        sendFrom = ws.computerName,
                        sendTo = message.sendFrom,
                        data = MessageServerResponseCode(ServerAnswerStatus.ERROR, "Task not started.").toJson()
                    ).toJson())
                }
            }
            MessageClientScriptList.TYPE -> {
                ws.sendMessage(WebsocketMessageClient(
                    type = MessageServerResponseCode.TYPE,
                    apiKey = ws.applicationData.apiKey,
                    sendFrom = ws.computerName,
                    sendTo = message.sendFrom,
                    data = MessageServerResponseCode(ServerAnswerStatus.OK, getScriptList()).toJson()
                ).toJson())
                println("Client: Script list send")
            }
            MessageServerUpdate.TYPE -> {
                val messageServerUpdate = MessageServerUpdate.fromJson(message.data)
                synchronized(ws.updateDataLock){
                    if (ws.softwareUpdate == null){
                        ws.softwareUpdate = SoftwareUpdate.fromServerMessage(messageServerUpdate)
                    }
                    if (ws.doSoftwareUpdateDataExist(messageServerUpdate.version)){
                        println("Update already installed for version ${messageServerUpdate.version}")
                        return
                    }
                    ws.softwareUpdate!!.writeFilePart(messageServerUpdate)

                    if (ws.softwareUpdate!!.updateStatus == UpdateStatus.FINISHED){
                        ws.softwareUpdate!!.startUpdate()
                        return
                    }
                    if (ws.softwareUpdate!!.updateStatus == UpdateStatus.ERROR) {
                        println("Client: Update error")
                        ws.updatePackageNrs = 0L
                        ws.softwareUpdate = null
                    }
                }
            }

            else -> {
                println("Client: Unknown message type received")
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
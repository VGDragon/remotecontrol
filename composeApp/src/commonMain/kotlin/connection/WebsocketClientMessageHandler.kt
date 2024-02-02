package connection

import filedata.ApplicationData
import GlobalVariables
import TaskFunctions
import interfaces.TaskInterface
import interfaces.TaskMessageInterface
import messages.*
import messages.base.*
import messages.base.client.MessageClientScriptList
import messages.base.server.MessageServerClientList
import java.io.File
import java.util.*

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
                    ws.send(WebsocketMessageClient(
                        type = MessageServerResponseCode.TYPE,
                        apiKey = ws.applicationData.apiKey,
                        sendFrom = ws.computerName,
                        sendTo = message.sendFrom,
                        data = MessageServerResponseCode(ServerAnswerStatus.OK, "").toJson()
                    ).toJson())
                } else {
                    println("Client: Task not started")
                    ws.send(WebsocketMessageClient(
                        type = MessageServerResponseCode.TYPE,
                        apiKey = ws.applicationData.apiKey,
                        sendFrom = ws.computerName,
                        sendTo = message.sendFrom,
                        data = MessageServerResponseCode(ServerAnswerStatus.ERROR, "Task not started.").toJson()
                    ).toJson())
                }
            }
            MessageClientScriptList.TYPE -> {
                println("Client: Script list received")

                ws.send(WebsocketMessageClient(
                    type = MessageServerResponseCode.TYPE,
                    apiKey = ws.applicationData.apiKey,
                    sendFrom = ws.computerName,
                    sendTo = message.sendFrom,
                    data = MessageServerResponseCode(ServerAnswerStatus.OK, getScriptList()).toJson()
                ).toJson())
            }

            else -> {
                println("Client: Unknown message type received")
            }
        }
    }

    fun getScriptList() : List<String> {
        val scriptFolderFile = File(GlobalVariables.scriptFolder)
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
        val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
        if (osName.contains("windows")) {
            return file.extension == "bat"
        } else if (osName.contains("linux")) {
            return file.extension == "sh"
        } else if (osName.contains("mac")) {
            return file.extension == "sh"
        } else {
            println("Unknown OS")
        }
        return file.extension == "bat"
    }
}
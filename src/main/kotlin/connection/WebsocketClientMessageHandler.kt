package connection

import ApplicationData
import GlobalVariables
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
            "ping" -> {
                println("Client: Ping received")
                Thread.sleep(1000)
                ws.send(WebsocketMessageClient(type = "pong", apiKey = applicationData.apiKey, data = "").toJson())
            }

            "pong" -> {
                println("Client: Pong received")
                //Thread.sleep(1000)
                //ws.send(WebsocketMessageClient(type = "ping", apiKey = applicationData.apiKey, data = "").toJson())
            }

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
                if(startTaskHandler(ws, MessageStartTask.fromJson(message.data))) {
                    println("Client: Task started")
                    ws.send(MessageServerResponseCode.toJson(ServerAnswerStatus.OK, ""))
                } else {
                    println("Client: Task not started")
                    ws.send(MessageServerResponseCode.toJson(ServerAnswerStatus.ERROR, "Task not started."))
                }
            }
            MessageClientScriptList.TYPE -> {
                println("Client: Script list received")
                ws.send(MessageServerResponseCode(ServerAnswerStatus.OK, getScriptList()).toJson())
            }

            else -> {
                println("Client: Unknown message type received")
            }
        }
    }

    fun startTaskHandler(ws: WebsocketConnectionClient, messageTaskList: List<TaskMessageInterface>): Boolean {
        messageTaskList.reversed()
        var lastTask: TaskInterface? = null
        for (taskMessage in messageTaskList) {
            val task = taskMessage.toTask(ws, lastTask)
            lastTask = task
        }
        if (lastTask == null) {
            return false
        }
        lastTask.start()
        return true
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
            scriptList.add(file.absolutePath)
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
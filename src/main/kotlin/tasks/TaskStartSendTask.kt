package tasks

import connection.WebsocketConnectionClient
import interfaces.TaskInterface
import messages.WebsocketMessageClient
import messages.base.MessageServerResponseCode
import messages.base.MessageStartTask
import messages.base.ServerAnswerStatus
import messages.base.client.MessageClientAddClientBridge
import messages.base.client.MessageClientBridgedClients
import messages.base.client.MessageClientRemoveClientBridge
import messages.base.server.MessageServerBridgedClients

class TaskStartSendTask(
    val clientName: String,
    val websocketConnectionClient: WebsocketConnectionClient,
    override val nextTask: TaskInterface?,
    val messageStartTask: String
) : TaskInterface {
    override val taskName: String = "SendTask"
    override var taskThread: Thread? = null
    override val taskThreadLock = Object()
    override fun start() {

        synchronized(taskThreadLock) {
            if (taskThread != null) {
                return
            }
            taskThread = Thread {
                websocketConnectionClient.send(
                    WebsocketMessageClient(
                        type = MessageClientBridgedClients.TYPE,
                        apiKey = websocketConnectionClient.applicationData.apiKey,
                        data = ""
                    ).toJson()
                )
                var responce = websocketConnectionClient.waitForResponse()
                var clientConnected = ""
                if (responce.status != ServerAnswerStatus.OK) {
                    println("Error: ${responce.message}")

                } else {
                    clientConnected = MessageServerBridgedClients.fromJson(responce.message as String).clientName
                }
                connectToClient(clientName)
                responce = websocketConnectionClient.waitForResponse()

                if (responce.status != ServerAnswerStatus.OK) {
                    println("Error: ${responce.message}")
                    if (clientConnected != "") {
                        connectToClient(clientConnected)
                        websocketConnectionClient.waitForResponse()
                    }
                } else {
                    //TODO testing
                    websocketConnectionClient.send(
                        WebsocketMessageClient(
                            type = MessageStartTask.TYPE,
                            apiKey = websocketConnectionClient.applicationData.apiKey,
                            data = messageStartTask
                        ).toJson()
                    )
                    websocketConnectionClient.waitForResponse()
                    connectToClient(clientConnected)
                    websocketConnectionClient.waitForResponse()
                }
                if (nextTask != null) {
                    nextTask.start()
                }
                websocketConnectionClient.removeTask(this)
            }
            websocketConnectionClient.addTask(this)
            taskThread!!.start()
        }
        println("Running SendTask")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    fun connectToClient(clientName: String) {
        websocketConnectionClient.send(
            WebsocketMessageClient(
                type = MessageClientAddClientBridge.TYPE,
                apiKey = websocketConnectionClient.applicationData.apiKey,
                data = MessageClientAddClientBridge(
                    clientName = clientName
                ).toJson()
            ).toJson()
        )
    }
}
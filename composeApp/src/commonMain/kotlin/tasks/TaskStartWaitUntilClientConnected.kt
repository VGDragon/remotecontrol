package tasks

import connection.WebsocketConnectionClient
import interfaces.TaskInterface
import messages.WebsocketMessageClient
import messages.base.MessageStartTask
import messages.base.client.MessageClientClientList

class TaskStartWaitUntilClientConnected(
    val websocketConnectionClient: WebsocketConnectionClient,
    override val nextTask: TaskInterface?,
    val clientToWaitFor: String,
    override val startedFrom: String
) : TaskInterface {
    override val taskName: String = TaskStartWaitUntilClientConnected.taskName
    override var taskThread: Thread? = null
    override val taskThreadLock = Object()
    override fun start() {

        synchronized(taskThreadLock) {
            if (taskThread != null) {
                return
            }
            taskThread = Thread {
                var stillWaiting = true
                while (stillWaiting) {
                    websocketConnectionClient.send(
                        WebsocketMessageClient(
                            type = MessageClientClientList.TYPE,
                            apiKey = websocketConnectionClient.applicationData.apiKey,
                            sendFrom = "",
                            sendTo = "",
                            data = ""
                        ).toJson()
                    )
                    websocketConnectionClient.waitForResponse()
                    val clientList = websocketConnectionClient.getExecClientListVariable()
                    if (clientList.contains(clientToWaitFor)) {
                        stillWaiting = false
                    } else {
                        Thread.sleep(1000)
                    }
                }

                if (nextTask != null) {
                    nextTask.start()
                }
                websocketConnectionClient.removeTask(this)
            }
            websocketConnectionClient.addTask(this)
            taskThread!!.start()
        }
        println("Running waitUntilClientConnectedTask")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    companion object {
        val taskName: String = "waitUntilClientConnectedTask"
    }

}
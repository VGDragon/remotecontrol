package tasks

import connection.WebsocketConnectionClient
import interfaces.TaskInterface
import messages.WebsocketMessageClient
import messages.base.MessageStartTask

class TaskStartSendTask(
    val clientName: String,
    val websocketConnectionClient: WebsocketConnectionClient,
    override val nextTask: TaskInterface?,
    val messageStartTask: String,
    override val startedFrom: String
) : TaskInterface {
    override val taskName: String = TaskStartSendTask.taskName
    override var taskThread: Thread? = null
    override val taskThreadLock = Object()
    override fun start() {

        synchronized(taskThreadLock) {
            if (taskThread != null) {
                return
            }
            taskThread = Thread {
                websocketConnectionClient.sendMessage(
                    WebsocketMessageClient(
                        type = MessageStartTask.TYPE,
                        apiKey = websocketConnectionClient.applicationData.apiKey,
                        sendFrom = startedFrom,
                        sendTo = clientName,
                        data = messageStartTask
                    ).toJson()
                )

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

    companion object {
        val taskName: String = "SendTask"
    }

}
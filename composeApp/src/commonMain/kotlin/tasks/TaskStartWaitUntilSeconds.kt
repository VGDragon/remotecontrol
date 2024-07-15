package tasks

import connection.client.WebsocketConnectionClient
import interfaces.TaskInterface

class TaskStartWaitUntilSeconds(
    val websocketConnectionClient: WebsocketConnectionClient,
    override val nextTask: TaskInterface?,
    val waitUntilSeconds: Long,
    override val startedFrom: String
) : TaskInterface {
    override val taskName: String = TaskStartWaitUntilSeconds.taskName
    override var taskThread: Thread? = null
    override val taskThreadLock = Object()
    override fun start() {

        synchronized(taskThreadLock) {
            if (taskThread != null) {
                return
            }
            taskThread = Thread {
                var startTime = System.currentTimeMillis()
                while (System.currentTimeMillis() - startTime < waitUntilSeconds * 1000) {
                    Thread.sleep(1000)
                }
                if (nextTask != null) {
                    nextTask.start()
                }
                websocketConnectionClient.removeTask(this)
            }
            websocketConnectionClient.addTask(this)
            taskThread!!.start()
        }
        println("Running waitUntilSeconds")
    }

    override fun stop() {
        TODO("Not yet implemented")
    }

    companion object {
        val taskName: String = "waitUntilSeconds"
    }

}
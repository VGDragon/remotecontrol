package messages.tasks
import com.google.gson.Gson
import connection.WebsocketConnectionClient
import interfaces.TaskInterface
import interfaces.TaskMessageInterface
import tasks.TaskStartSendTask

class MessageStartTaskSendTask(override val type: String,
                               override val clientTo: String,
                               val sendMessageTo: String,
                               val messageToSend: String): TaskMessageInterface {
    override fun toJson(): String {
        return Gson().toJson(this)
    }

    override fun toTask(websocketConnectionClient: WebsocketConnectionClient, nextTask: TaskInterface?, startedFrom: String): TaskInterface {
        return TaskStartSendTask(
            clientName = sendMessageTo,
            websocketConnectionClient = websocketConnectionClient,
            nextTask = nextTask,
            messageStartTask = messageToSend,
            startedFrom = startedFrom)
    }

    companion object {
        fun fromJson(json: String): MessageStartTaskSendTask {
            return Gson().fromJson(json, MessageStartTaskSendTask::class.java)
        }
        const val TYPE = "SendTask"


    }
}
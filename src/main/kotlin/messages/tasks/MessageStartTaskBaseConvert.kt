package messages.tasks

import com.google.gson.Gson
import connection.WebsocketConnectionClient
import interfaces.TaskInterface
import interfaces.TaskMessageInterface
import tasks.TaskStartScript

class MessageStartTaskBaseConvert(override val type: String) : TaskMessageInterface {
    override fun toJson(): String {
        return Gson().toJson(this)
    }

    override fun toTask(websocketConnectionClient: WebsocketConnectionClient, nextTask: TaskInterface?): TaskInterface {
        return TaskStartScript(scriptName = "",
            websocketConnectionClient = websocketConnectionClient,
            nextTask = nextTask)
    }


    companion object {
        fun fromJson(json: String): MessageStartTaskBaseConvert {
            return Gson().fromJson(json, MessageStartTaskBaseConvert::class.java)
        }

        fun convertToTaskMessageType(type: String, message: String): TaskMessageInterface {
            return when (type) {
                MessageStartTaskScript.TYPE -> MessageStartTaskScript.fromJson(message)
                // TODO add new task types here
                else -> MessageStartTaskBaseConvert(type)
            }
        }
    }
}
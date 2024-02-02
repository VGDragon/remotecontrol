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

    override fun toTask(websocketConnectionClient: WebsocketConnectionClient, nextTask: TaskInterface?, startedFrom: String): TaskInterface {
        return TaskStartScript(scriptName = "",
            websocketConnectionClient = websocketConnectionClient,
            nextTask = nextTask, startedFrom = startedFrom)
    }


    companion object {
        fun fromJson(json: String): MessageStartTaskBaseConvert {
            return Gson().fromJson(json, MessageStartTaskBaseConvert::class.java)
        }
    }
}
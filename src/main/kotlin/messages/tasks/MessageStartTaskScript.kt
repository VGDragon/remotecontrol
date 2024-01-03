package messages.tasks

import com.google.gson.Gson
import connection.WebsocketConnectionClient
import interfaces.TaskInterface
import interfaces.TaskMessageInterface
import tasks.TaskStartScript

class MessageStartTaskScript(override val type: String, val scriptName: String = ""): TaskMessageInterface {
    override fun toJson(): String {
        return Gson().toJson(this)
    }

    override fun toTask(websocketConnectionClient: WebsocketConnectionClient, nextTask: TaskInterface?): TaskInterface {
        return TaskStartScript(scriptName = scriptName,
            websocketConnectionClient = websocketConnectionClient,
            nextTask = nextTask)
    }

    companion object {
        fun fromJson(json: String): MessageStartTaskScript {
            return Gson().fromJson(json, MessageStartTaskScript::class.java)
        }
        const val TYPE = "startScript"
    }
}
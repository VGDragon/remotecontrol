package messages.tasks
import com.google.gson.Gson
import connection.WebsocketConnectionClient
import interfaces.TaskInterface
import interfaces.TaskMessageInterface
import tasks.TaskStartWaitUntilClientConnected
import tasks.TaskStartWaitUntilSeconds

class MessageStartTaskWaitUntilSeconds(override val type: String, override val clientTo: String, val waitUntilSeconds: Long,): TaskMessageInterface {
    /*
     type: String - the type of the message (MessageStartTaskScript.TYPE)
        clientTo: String - the client to send the message to

        scriptName: String - the name of the script to start
     */
    override fun toJson(): String {
        return Gson().toJson(this)
    }

    override fun toTask(websocketConnectionClient: WebsocketConnectionClient, nextTask: TaskInterface?, startedFrom: String): TaskInterface {
        /*
        websocketConnectionClient: WebsocketConnectionClient - the websocket connection client
        nextTask: TaskInterface? - the next task to run
        startedFrom: String - the name of the client that started the task chain
         */
        return TaskStartWaitUntilSeconds(
            waitUntilSeconds = waitUntilSeconds,
            websocketConnectionClient = websocketConnectionClient,
            nextTask = nextTask,
            startedFrom = startedFrom)
    }

    companion object {
        fun fromJson(json: String): MessageStartTaskWaitUntilSeconds {
            return Gson().fromJson(json, MessageStartTaskWaitUntilSeconds::class.java)
        }
        const val TYPE = "waitUntilSeconds"


    }
}
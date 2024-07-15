package interfaces

import com.google.gson.Gson
import connection.client.WebsocketConnectionClient

interface TaskMessageInterface {
    val type: String
    val clientTo: String
    fun toJson(): String
    fun toTask(websocketConnectionClient: WebsocketConnectionClient, nextTask: TaskInterface?, startedFrom: String): TaskInterface

    companion object {
        fun fromJson(json: String): TaskMessageInterface {
            return Gson().fromJson(json, TaskMessageInterface::class.java)
        }
        val TYPE: String = ""
    }




}
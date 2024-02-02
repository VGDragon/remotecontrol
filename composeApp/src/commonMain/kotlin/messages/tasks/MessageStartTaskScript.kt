package messages.tasks

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import com.google.gson.Gson
import connection.WebsocketConnectionClient
import interfaces.TaskInterface
import interfaces.TaskMessageInterface
import tasks.TaskStartScript

class MessageStartTaskScript(override val type: String, val scriptName: String = ""): TaskMessageInterface {
    override fun toJson(): String {
        return Gson().toJson(this)
    }

    override fun toTask(websocketConnectionClient: WebsocketConnectionClient, nextTask: TaskInterface?, startedFrom: String): TaskInterface {
        return TaskStartScript(scriptName = scriptName,
            websocketConnectionClient = websocketConnectionClient,
            nextTask = nextTask,
            startedFrom = startedFrom)
    }

    companion object {
        fun fromJson(json: String): MessageStartTaskScript {
            return Gson().fromJson(json, MessageStartTaskScript::class.java)
        }
        const val TYPE = "startScript"
        fun fromMap(map: Map<String, String>): MessageStartTaskScript {
            return MessageStartTaskScript(type = map["type"]!!, scriptName = map["scriptName"]!!)
        }


    }
}
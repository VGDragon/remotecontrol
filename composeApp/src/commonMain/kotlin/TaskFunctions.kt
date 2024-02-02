import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import connection.WebsocketConnectionClient
import filedata.ApplicationData
import interfaces.TaskInterface
import interfaces.TaskMessageInterface
import messages.WebsocketMessageClient
import messages.base.client.MessageClientClientList
import messages.base.client.MessageClientScriptList
import messages.tasks.MessageStartTaskBaseConvert
import messages.tasks.MessageStartTaskScript
import tasks.TaskStartScript
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty0

class TaskFunctions {
    companion object {
        // TODO add new task types here
        fun convertToTaskMessageType(type: String, message: String): TaskMessageInterface {
            return when (type) {
                MessageStartTaskScript.TYPE -> MessageStartTaskScript.fromJson(message)
                else -> MessageStartTaskBaseConvert(type)
            }
        }
        fun getTaskFromGuiData(taskType: String, entryTypeData: MutableMap<String, String>): MessageStartTaskScript? {
            when (taskType) {
                MessageStartTaskScript.TYPE -> {
                    return MessageStartTaskScript(type=taskType, scriptName = entryTypeData["scriptName"]!!)
                }
                else ->
                    return null
            }

        }
        fun entryTypeList(): List<Triple<KProperty0<String>, KFunction1<Map<String, String>, MessageStartTaskScript>, Nothing?>> {
            return listOf(
                Triple(MessageStartTaskScript::TYPE, MessageStartTaskScript::fromMap, null),
                Triple(MessageStartTaskScript::TYPE, MessageStartTaskScript::fromMap, null)
            )
        }
        fun startTaskHandler(ws: WebsocketConnectionClient, messageTaskList: List<TaskMessageInterface>, startedFrom: String): Boolean {
            messageTaskList.reversed()
            var lastTask: TaskInterface? = null
            for (taskMessage in messageTaskList) {
                val task = taskMessage.toTask(ws, lastTask, startedFrom)
                lastTask = task
            }
            if (lastTask == null) {
                return false
            }
            lastTask.start()
            return true
        }

    }

}
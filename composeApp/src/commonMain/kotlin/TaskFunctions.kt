import connection.WebsocketConnectionClient
import interfaces.TaskInterface
import interfaces.TaskMessageInterface
import messages.tasks.MessageStartTaskBaseConvert
import messages.tasks.MessageStartTaskScript
import messages.tasks.MessageStartTaskSendTask
import messages.tasks.MessageStartTaskWaitUntilClientConnected
import kotlin.reflect.KProperty0

class TaskFunctions {
    companion object {
        fun convertToTaskMessageType(type: String, message: String): TaskMessageInterface {
            return when (type) {
                MessageStartTaskScript.TYPE -> MessageStartTaskScript.fromJson(message)
                MessageStartTaskSendTask.TYPE -> MessageStartTaskSendTask.fromJson(message)
                MessageStartTaskWaitUntilClientConnected.TYPE -> MessageStartTaskWaitUntilClientConnected.fromJson(message)
                // TODO add new task types here
                else -> MessageStartTaskBaseConvert(type, "")
            }
        }
        fun getTaskFromGuiData(taskType: String,
                               client: String,
                               entryTypeData: MutableMap<String, String>): TaskMessageInterface? {
            when (taskType) {
                MessageStartTaskScript.TYPE -> {
                    if (entryTypeData["scriptName"].isNullOrBlank()){
                        return null
                    }
                    return MessageStartTaskScript(type=taskType, clientTo=client, scriptName = entryTypeData["scriptName"]!!)
                }
                MessageStartTaskWaitUntilClientConnected.TYPE -> {
                    if (entryTypeData["clientToWaitFor"].isNullOrBlank()){
                        return null
                    }
                    return MessageStartTaskWaitUntilClientConnected(type=taskType, clientTo=client, clientToWaitFor = entryTypeData["clientToWaitFor"]!!)
                }
                // TODO add new task types here
                else ->
                    return null
            }

        }
        fun entryTypeList(): List<Triple<KProperty0<String>, Nothing?, Nothing?>> {
            return listOf(
                Triple(MessageStartTaskScript::TYPE, null, null),
                Triple(MessageStartTaskWaitUntilClientConnected::TYPE, null, null),
                // TODO add new task types here
                //Triple(MessageStartTaskScript::TYPE, null, null)
            )
        }
        fun startTaskHandler(ws: WebsocketConnectionClient, messageTaskList: List<TaskMessageInterface>, startedFrom: String): Boolean {
            var workMessageTaskList = messageTaskList.toMutableList()
            workMessageTaskList.reverse()
            var lastTask: TaskInterface? = null
            for (taskMessage in workMessageTaskList) {
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
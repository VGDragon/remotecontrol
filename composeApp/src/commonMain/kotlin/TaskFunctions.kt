import connection.WebsocketConnectionClient
import interfaces.TaskInterface
import interfaces.TaskMessageInterface
import messages.tasks.MessageStartTaskBaseConvert
import messages.tasks.MessageStartTaskScript
import messages.tasks.MessageStartTaskSendTask
import kotlin.reflect.KProperty0

class TaskFunctions {
    companion object {
        fun convertToTaskMessageType(type: String, message: String): TaskMessageInterface {
            return when (type) {
                MessageStartTaskScript.TYPE -> MessageStartTaskScript.fromJson(message)
                MessageStartTaskSendTask.TYPE -> MessageStartTaskSendTask.fromJson(message)
                // TODO add new task types here
                else -> MessageStartTaskBaseConvert(type, "")
            }
        }
        fun getTaskFromGuiData(taskType: String,
                               client: String,
                               entryTypeData: MutableMap<String, String>): MessageStartTaskScript? {
            when (taskType) {
                MessageStartTaskScript.TYPE -> {
                    if (entryTypeData["scriptName"].isNullOrBlank()){
                        return null
                    }
                    return MessageStartTaskScript(type=taskType, clientTo=client, scriptName = entryTypeData["scriptName"]!!)
                }
                // TODO add new task types here
                else ->
                    return null
            }

        }
        fun entryTypeList(): List<Triple<KProperty0<String>, Nothing?, Nothing?>> {
            return listOf(
                Triple(MessageStartTaskScript::TYPE, null, null),
                // TODO add new task types here
                //Triple(MessageStartTaskScript::TYPE, null, null)
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
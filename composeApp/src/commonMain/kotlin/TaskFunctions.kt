import connection.client.WebsocketConnectionClient
import interfaces.TaskInterface
import interfaces.TaskMessageInterface
import messages.tasks.*

class TaskFunctions {
    companion object {
        fun convertToTaskMessageType(type: String, message: String): TaskMessageInterface {
            return when (type) {
                MessageStartTaskScript.TYPE -> MessageStartTaskScript.fromJson(message)
                MessageStartTaskSendTask.TYPE -> MessageStartTaskSendTask.fromJson(message)
                MessageStartTaskWaitUntilClientConnected.TYPE -> MessageStartTaskWaitUntilClientConnected.fromJson(message)
                MessageStartTaskWaitUntilSeconds.TYPE -> MessageStartTaskWaitUntilSeconds.fromJson(message)
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
                    if (entryTypeData["clientToWaitForClient"].isNullOrBlank()){
                        return null
                    }
                    return MessageStartTaskWaitUntilClientConnected(type=taskType, clientTo=client, clientToWaitFor = entryTypeData["clientToWaitForClient"]!!)
                }
                MessageStartTaskWaitUntilSeconds.TYPE -> {
                    if (entryTypeData["clientToWaitForSeconds"].isNullOrBlank()){
                        return null
                    }
                    return MessageStartTaskWaitUntilSeconds(type=taskType, clientTo=client, waitUntilSeconds = entryTypeData["clientToWaitForSeconds"]!!.toLong())
                }
                // TODO add new task types here
                else ->
                    return null
            }

        }
        fun entryTypeList(): List<String> {
            return listOf(
                MessageStartTaskScript.TYPE,
                MessageStartTaskWaitUntilClientConnected.TYPE,
                MessageStartTaskWaitUntilSeconds.TYPE,
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
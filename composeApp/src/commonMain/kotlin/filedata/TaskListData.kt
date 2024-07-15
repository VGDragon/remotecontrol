package filedata

import com.google.gson.Gson
import connection.client.WebsocketConnectionClient
import messages.WebsocketMessageClient
import messages.base.MessageStartTask
import messages.tasks.MessageStartTaskSendTask

class TaskListData(
    val fileName: String = "",
    var taskName: String = "",
    var taskActionDataList: MutableList<TaskActionData> = mutableListOf()
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    fun fileExists(): Boolean {
        val file = java.io.File(GlobalVariables.taskFolder(), fileName)
        return file.exists()
    }

    fun saveToFile(): TaskListData {
        // save the api key to a file
        val folder = java.io.File(GlobalVariables.taskFolder())
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = java.io.File(GlobalVariables.taskFolder(), fileName)
        file.writeText(this.toJson())
        return this
    }

    fun startTaskList(ws: WebsocketConnectionClient) {
        val taskList = taskActionDataList.toMutableList()
        if (taskList.isEmpty()) {
            return
        }
        taskList.reverse()
        val taskMessageList: MutableList<String> = mutableListOf()

        var lastClientName = taskList[0].clientName
        for (taskActionData in taskList) {
            if (taskActionData.clientName.equals(lastClientName)) {
                taskMessageList.add(0, taskActionData.taskData)
                continue
            }
            val sendTo = lastClientName
            val taskMessageListCopy = taskMessageList.toMutableList()
            taskMessageList.clear()

            // TODO: create send task message
            taskMessageList.add(0,
                MessageStartTaskSendTask(
                    type = MessageStartTaskSendTask.TYPE,
                    clientTo = taskActionData.clientName,
                    sendMessageTo = sendTo,
                    messageToSend = Gson().toJson(taskMessageListCopy)
                ).toJson()
            )
            lastClientName = taskActionData.clientName
            taskMessageList.add(0, taskActionData.taskData)
        }

        ws.sendMessage(
            WebsocketMessageClient(
                type = MessageStartTask.TYPE,
                apiKey = ws.applicationData.apiKey,
                sendFrom = ws.computerName,
                sendTo = lastClientName,
                data = Gson().toJson(taskMessageList)
            ).toJson()
        )
    }

    fun deleteTaskListDataFile() {
        val file = java.io.File(GlobalVariables.taskFolder(), fileName)
        if (file.exists()) {
            file.delete()
        }
    }

    companion object {
        fun getTaskListDataFiles(): MutableList<TaskListData> {
            val folder = java.io.File(GlobalVariables.taskFolder())
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val fileList = folder.listFiles()
            val taskListDataList = mutableListOf<TaskListData>()
            for (file in fileList!!) {
                if (file.isFile && file.name.endsWith(".json")) {
                    val json = file.readText()
                    val taskListData = fromJson(json)
                    taskListDataList.add(taskListData)
                }
            }
            return taskListDataList
        }

        fun fromJson(json: String): TaskListData {
            return Gson().fromJson(json, TaskListData::class.java)
        }

        fun fromFile(fileName: String): TaskListData {
            val file = java.io.File(GlobalVariables.taskFolder(), fileName)
            if (file.exists()) {
                val json = file.readText()
                return fromJson(json)
            }
            return TaskListData().saveToFile()
        }
    }
}
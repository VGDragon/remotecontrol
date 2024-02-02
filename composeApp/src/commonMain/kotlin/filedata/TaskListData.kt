package filedata

import TaskFunctions
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotMutableState
import com.google.gson.Gson
import connection.WebsocketConnectionClient
import messages.WebsocketMessageClient
import messages.base.MessageStartTask

class TaskListData(
    val fileName: String = "",
    var taskName: String = "",
    var taskActionDataList: MutableList<TaskActionData> = mutableListOf()
) {
    fun toJson(): String {
        return Gson().toJson(this)
    }

    fun fileExists(): Boolean {
        val file = java.io.File(GlobalVariables.taskFolder, fileName)
        return file.exists()
    }

    fun saveToFile(): TaskListData {
        // save the api key to a file
        val folder = java.io.File(GlobalVariables.taskFolder)
        if (!folder.exists()) {
            folder.mkdirs()
        }
        val file = java.io.File(GlobalVariables.taskFolder, fileName)
        file.writeText(this.toJson())
        return this
    }

    fun startTaskList(ws: WebsocketConnectionClient) {
        val taskList = taskActionDataList.toMutableList()
        if (taskList.isEmpty()) {
            return
        }
        val sendTo = taskList[0].clientName
        val taskMessageList: MutableList<String> = mutableListOf()
        for (task in taskList) {
            taskMessageList.add(task.taskData)
        }
        ws.send(
            WebsocketMessageClient(
                type = MessageStartTask.TYPE,
                apiKey = ws.applicationData.apiKey,
                sendFrom = ws.computerName,
                sendTo = sendTo,
                data = Gson().toJson(taskMessageList)
            ).toJson()
        )
    }

    companion object {
        fun getTaskListDataFiles(): MutableList<TaskListData> {
            val folder = java.io.File(GlobalVariables.taskFolder)
            if (!folder.exists()) {
                folder.mkdirs()
            }
            val fileList = folder.listFiles()
            val taskListDataList = mutableListOf<TaskListData>()
            for (file in fileList) {
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
            val file = java.io.File(GlobalVariables.taskFolder, fileName)
            if (file.exists()) {
                val json = file.readText()
                return fromJson(json)
            }
            return TaskListData().saveToFile()
        }
    }
}
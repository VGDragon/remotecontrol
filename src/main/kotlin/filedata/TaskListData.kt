package filedata

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotMutableState
import com.google.gson.Gson

class TaskListData(
    val fileName: String = "",
    var taskName: String = "",
    var taskActionDataList: MutableList<TaskActionData> = mutableListOf()
) {
    val taskActionDataListState: MutableState<List<TaskActionData>> = mutableStateOf(taskActionDataList)
    fun removeTaskActionData(itemToRemove: TaskActionData) {
        taskActionDataList.remove(itemToRemove)
        // Update the state to trigger recomposition
        taskActionDataListState.value = taskActionDataList.toList()
    }
    fun addTaskActionData(itemToAdd: TaskActionData) {
        taskActionDataList.add(itemToAdd)
        // Update the state to trigger recomposition
        taskActionDataListState.value = taskActionDataList.toList()
    }
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
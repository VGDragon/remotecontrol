package messages.base

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import interfaces.TaskMessageInterface
import messages.tasks.MessageStartTaskBaseConvert

class MessageStartTask(
    var taskList: List<String> = listOf()
) {

    fun toJson(): String {
        return Gson().toJson(taskList)
    }

    // add the companion object from the interface
    companion object {
        fun fromJson(json: String): List<TaskMessageInterface> {
            //return Gson().fromJson(json, Array<TaskMessageInterface>::class.java).toList()
            val typeToken = object : TypeToken<List<String>>() {}.type
            val jsonStringList = Gson().fromJson<List<String>>(json, typeToken)

            val taskMessageList: MutableList<TaskMessageInterface> = mutableListOf()
            for (jsonString in jsonStringList) {
                val messageStartTaskBaseConvert = MessageStartTaskBaseConvert.fromJson(jsonString)
                taskMessageList.add(TaskFunctions.convertToTaskMessageType(messageStartTaskBaseConvert.type, jsonString))
            }

            return taskMessageList
        }

        val TYPE = "startTask"
    }

}
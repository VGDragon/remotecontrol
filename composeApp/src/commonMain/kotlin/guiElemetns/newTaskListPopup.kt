package guiElemetns

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import filedata.ApplicationData
import filedata.TaskListData

@Composable
fun newTaskListPopup (applicationData: ApplicationData,
                      taskListDataMap: SnapshotStateMap<Int, TaskListData>) {

    val stateVertical by remember { mutableStateOf(ScrollState(0)) }
    val stateHorizontal by remember { mutableStateOf(ScrollState(0)) }


    var newTaskListName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    var newTaskListPopup by remember { mutableStateOf(false) }
    var saveEntry by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(stateVertical)
            .padding(end = 12.dp, bottom = 12.dp)
            .horizontalScroll(stateHorizontal)
    ) {

        Column {
            Button(
                onClick = {
                    newTaskListPopup = false
                    applicationData.saveToFile()
                    errorText = ""
                }) {
                Text("Close")
            }
            Column {
                Text("New Task List")
            }
            Column {
                Text("Name:")
                TextField(
                    value = newTaskListName,
                    onValueChange = { name ->
                        newTaskListName = name
                    })
            }
            rowSmallSeperator()
            writeErrorText(errorText)
            rowSmallSeperator()
            Column {
                Button(
                    onClick = {
                        saveEntry = true
                    }) {
                    Text("Save")
                }
            }
        }
    }

    if (saveEntry) {
        saveEntry = false
        var toSave = false
        var toSaveFile = false
        val tempTaskListData = TaskListData(
            fileName = newTaskListName + ".json",
            taskName = newTaskListName,
        )
        if (!tempTaskListData.fileExists()) {
            tempTaskListData.saveToFile()
            if (taskListDataMap.keys.isEmpty()) {
                taskListDataMap[0] = tempTaskListData
            } else {
                taskListDataMap[taskListDataMap.keys.max() + 1] = tempTaskListData
            }
            toSaveFile = true
            newTaskListPopup = false
            newTaskListName = ""
        } else {
            errorText = "Task list already exists"
            println("Task list already exists")
        }
        if (toSaveFile) {
            applicationData.saveToFile()
        }

    }

}
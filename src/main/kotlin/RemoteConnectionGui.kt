import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import connection.WebsocketConnectionClient
import filedata.ApplicationData
import filedata.TaskActionData
import filedata.TaskListData
import messages.WebsocketMessageClient
import messages.base.client.MessageClientClientList
import messages.base.client.MessageClientRegister
import messages.base.client.MessageClientRemoveClientBridge
import messages.base.client.MessageClientScriptList
import messages.tasks.MessageStartTaskScript


@Composable
@Preview
fun App() {
    var connectButtonText by remember { mutableStateOf("Connect") }
    var disconnectButtonText by remember { mutableStateOf("Disconnect") }

    val applicationData by remember { mutableStateOf(ApplicationData.fromFile()) }
    var websocketConnectionClient: WebsocketConnectionClient? by remember { mutableStateOf(null) }

    // connection variables
    var connectionAddress by remember { mutableStateOf(TextFieldValue(applicationData.address)) }
    var connectionPort by remember { mutableStateOf(TextFieldValue(applicationData.port.toString())) }


    // scroll states
    val stateVertical by remember { mutableStateOf(ScrollState(0)) }
    val stateHorizontal by remember { mutableStateOf(ScrollState(0)) }

    // client information
    var selectedClient by remember { mutableStateOf("") }

    // connect and disconnect buttons
    val connectButtonActive = remember { mutableStateOf(true) }
    val disconnectButtonActive = remember { mutableStateOf(false) }

    var saveEntry by remember { mutableStateOf(false) }
    var newTaskListPopup by remember { mutableStateOf(false) }
    var editTaskListPopup by remember { mutableStateOf(false) }

    // new task list items
    var newTaskListName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    // task list data
    var taskListDataList by remember { mutableStateOf(TaskListData.getTaskListDataFiles()) }
    var taskListDataSelected by remember { mutableStateOf("") }
    var taskListDataSelectedIndex by remember { mutableStateOf(0) }
    var isTaskListDataDropdownActive by remember { mutableStateOf(false) }

    //val taskActionDataListState = taskListDataList.taskActionDataListState.collectAsState()

    // add task entry
    var addTaskEntry by remember { mutableStateOf(false) }
    val entryTypeList = TaskFunctions.entryTypeList()
    var newTaskEntryName by remember { mutableStateOf("") }
    var entryTypeSelectedIndex by remember { mutableStateOf(0) }
    var isEntryTypeDropdownActive by remember { mutableStateOf(false) }

    // map
    val taskEntryData by remember { mutableStateOf(mutableStateMapOf<String, String>()) }

    // task data: start script
    var scriptNames: MutableList<String> = mutableListOf()
    var scriptNamesDropdownActive by remember { mutableStateOf(false) }
    var scriptNameSelectedIndex by remember { mutableStateOf(0) }

    MaterialTheme {

        if (newTaskListPopup) {

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
                    Column {
                        Text(errorText)
                    }
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
        } else if (editTaskListPopup) {
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
                            editTaskListPopup = false
                            applicationData.saveToFile()
                        },
                        contentPadding = PaddingValues(0.dp),
                        modifier = Modifier
                            .padding(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 2.dp)
                    ) {
                        Text("Close")
                    }
                    rowSmallSeperator()
                    Row {
                        Text("Edit Task List")
                    }
                    rowSmallSeperator()
                    Row {
                        Text("Name: ${taskListDataList[taskListDataSelectedIndex].taskName}")
                    }
                    rowSmallSeperator()
                    Row {
                        Text("Task List")
                    }
                    // TODO: Test this
                    taskListDataList[taskListDataSelectedIndex].taskActionDataList.forEach { taskActionData ->
                        Row {
                            Text(taskActionData.taskName)
                            Button(
                                onClick = {
                                    // Call the function to remove the item
                                    taskListDataList[taskListDataSelectedIndex].removeTaskActionData(taskActionData)
                                    // Trigger recomposition by updating the state variable
                                    taskListDataList = taskListDataList.toMutableList()
                                },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier
                                    .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
                            ) {
                                Text("Remove", fontSize = 15.sp)
                            }
                        }
                    }

                    //for (taskActionData in taskListDataList[taskListDataSelectedIndex].taskActionDataList) {
                    //    Row {
                    //        Text(taskActionData.taskName)
                    //        Button(
                    //            onClick = {
                    //                val tempTaskListData = taskListDataList[taskListDataSelectedIndex]
                    //                tempTaskListData.removeTaskActionData(taskActionData)
                    //                //updateGui = !updateGui
                    //                // TODO: gui doesn't update
                    //            },
                    //            contentPadding = PaddingValues(0.dp),
                    //            modifier = Modifier
                    //                .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
                    //        ) {
                    //            Text("Remove", fontSize = 15.sp)
                    //        }
                    //    }
                    //}
                    rowSmallSeperator()
                    Row {
                        Button(
                            onClick = {
                                taskEntryData.clear()
                                //taskEntryData["type"] = entryTypeList[entryTypeSelectedIndex].first.get()
                                //taskEntryData["scriptName"] = ""
                                entryTypeSelectedIndex = 0
                                editTaskListPopup = false
                                addTaskEntry = true
                            }) {
                            Text("Add task")
                        }
                    }
                    rowSmallSeperator()
                    Row {
                        Button(
                            onClick = {
                                saveEntry = true
                            }) {
                            Text("Save")
                        }
                    }

                }
            }
        } else if (addTaskEntry) {

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
                            addTaskEntry = false
                            editTaskListPopup = true
                            applicationData.saveToFile()
                        }) {
                        Text("Close")
                    }
                    rowSmallSeperator()
                    // make it map depending on the type
                    Row {
                        Text("Name:")
                        TextField(
                            value = newTaskEntryName,
                            onValueChange = { name ->
                                newTaskEntryName = name
                            })
                    }
                    rowSmallSeperator()
                    Row {
                        Text("Type:")
                        Text(
                            entryTypeList[entryTypeSelectedIndex].first.get(),
                            modifier = Modifier.clickable(onClick = { isEntryTypeDropdownActive = true })
                                .background(Color.LightGray)
                        )
                        DropdownMenu(
                            expanded = isEntryTypeDropdownActive,
                            onDismissRequest = { isEntryTypeDropdownActive = false },
                            modifier = Modifier.height(170.dp)
                        ) {
                            entryTypeList.forEachIndexed { index, entryType ->
                                DropdownMenuItem(
                                    onClick = {
                                        //entryTypeSelected = entryType.first.get()
                                        entryTypeSelectedIndex = index
                                        isEntryTypeDropdownActive = false
                                    }, modifier = Modifier.background(
                                        if (entryTypeSelectedIndex == index) {
                                            Color.LightGray
                                        } else {
                                            Color.White
                                        }
                                    )
                                ) {
                                    Text(entryType.first.get())
                                }
                            }
                        }
                    }
                    rowSmallSeperator()

                    // TODO add new task types here
                    when (entryTypeList[entryTypeSelectedIndex].first.get()) {
                        MessageStartTaskScript.TYPE -> {
                            if (scriptNames.isEmpty()) {
                                websocketConnectionClient!!.send(
                                    WebsocketMessageClient(
                                        type = MessageClientScriptList.TYPE,
                                        apiKey = applicationData.apiKey,
                                        sendTo = selectedClient,
                                        sendFrom = websocketConnectionClient!!.computerName,
                                        data = ""
                                    )
                                        .toJson()
                                )
                                val answer = websocketConnectionClient!!.waitForResponse()
                                scriptNames = answer.message as MutableList<String>
                                taskEntryData["scriptName"] = scriptNames[scriptNameSelectedIndex]
                            }
                            Row {
                                Text("script")

                                Text(
                                    scriptNames[scriptNameSelectedIndex],
                                    modifier = Modifier.clickable(onClick = { scriptNamesDropdownActive = true })
                                        .background(Color.LightGray)
                                )
                                DropdownMenu(
                                    expanded = scriptNamesDropdownActive,
                                    onDismissRequest = { scriptNamesDropdownActive = false },
                                    modifier = Modifier.height(170.dp)
                                ) {
                                    websocketConnectionClient!!.send(
                                        WebsocketMessageClient(
                                            type = MessageClientScriptList.TYPE,
                                            apiKey = applicationData.apiKey,
                                            sendTo = selectedClient,
                                            sendFrom = websocketConnectionClient!!.computerName,
                                            data = ""
                                        )
                                            .toJson()
                                    )
                                    val answer = websocketConnectionClient!!.waitForResponse()
                                    scriptNames = answer.message as MutableList<String>
                                    scriptNames.forEachIndexed { index, scriptName ->
                                        DropdownMenuItem(
                                            onClick = {
                                                scriptNameSelectedIndex = index
                                                scriptNamesDropdownActive = false
                                            }, modifier = Modifier.background(
                                                if (scriptNameSelectedIndex == index) {
                                                    Color.LightGray
                                                } else {
                                                    Color.White
                                                }
                                            )
                                        ) {
                                            if (scriptNameSelectedIndex == index) {
                                                taskEntryData["scriptName"] = scriptName
                                            }
                                            Text(scriptNames[scriptNameSelectedIndex])
                                        }
                                    }
                                }
                            }
                        }

                        else -> Text("unknown task type")
                    }
                    rowSmallSeperator()
                    Row {
                        Button(
                            onClick = {
                                saveEntry = true
                            }) {
                            Text("Save")
                        }
                    }

                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(stateVertical)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .horizontalScroll(stateHorizontal)
            ) {
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                ) {

                    if (connectButtonActive.value) {

                        // add link and port entries
                        Row(modifier = Modifier.height(50.dp)) {

                            Column(modifier = Modifier.height(50.dp)) {
                                Text("Address:", fontSize = 20.sp)
                            }

                            Column {
                                TextField(modifier = Modifier.height(50.dp),
                                    value = connectionAddress,
                                    onValueChange = { address ->
                                        connectionAddress = address
                                        applicationData.address = address.text
                                    })

                            }
                        }
                        rowSmallSeperator()
                        Row {
                            Column(modifier = Modifier.height(50.dp)) {
                                Text("Port:", fontSize = 20.sp)
                            }
                            Column {
                                TextField(modifier = Modifier.height(50.dp),
                                    value = connectionPort,
                                    onValueChange = { port ->
                                        connectionPort = port
                                        applicationData.port = port.text.toInt()
                                    })

                            }
                        }
                    } else {
                        Row {
                            Column {
                                Text("Address: ${connectionAddress.text}:${connectionPort.text}", fontSize = 20.sp)
                            }
                        }
                    }
                    rowSmallSeperator()
                    Row {
                        Button(enabled = connectButtonActive.value,
                            onClick = {
                                websocketConnectionClient = connectToServer(applicationData)

                                websocketConnectionClient!!.send(
                                    WebsocketMessageClient(
                                        type = MessageClientRegister.TYPE,
                                        apiKey = applicationData.apiKey,
                                        sendFrom = "",
                                        sendTo = "",
                                        data = MessageClientRegister(
                                            clientName = websocketConnectionClient!!.computerName,
                                            isExecutable = false
                                        ).toJson()
                                    )
                                        .toJson()
                                )
                                websocketConnectionClient!!.waitForResponse()
                                websocketConnectionClient!!.send(
                                    WebsocketMessageClient(
                                        type = MessageClientClientList.TYPE,
                                        apiKey = applicationData.apiKey,
                                        sendFrom = "",
                                        sendTo = "",
                                        data = ""
                                    )
                                        .toJson()
                                )
                                connectButtonActive.value = false
                                disconnectButtonActive.value = true
                                applicationData.saveToFile()
                                websocketConnectionClient!!.waitForResponse()
                            }) {
                            Text(connectButtonText)
                        }
                        Button(enabled = disconnectButtonActive.value,
                            onClick = {
                                websocketConnectionClient!!.send(
                                    WebsocketMessageClient(
                                        type = MessageClientRemoveClientBridge.TYPE,
                                        apiKey = applicationData.apiKey,
                                        sendFrom = "",
                                        sendTo = "",
                                        data = ""
                                    ).toJson()
                                )
                                websocketConnectionClient!!.setIsConnected(false)
                                websocketConnectionClient!!.close()
                                websocketConnectionClient = null
                                connectButtonActive.value = true
                                disconnectButtonActive.value = false
                            }) {
                            Text(disconnectButtonText)
                        }
                    }
                    rowSmallSeperator()

                    Row(modifier = Modifier.requiredHeight(20.dp).requiredWidth(500.dp)) {
                        Text(
                            if (taskListDataSelectedIndex == -1) {
                                "     "
                            } else {
                                taskListDataList[taskListDataSelectedIndex].taskName
                            },
                            modifier = Modifier.clickable(onClick = { isTaskListDataDropdownActive = true })
                                .background(Color.LightGray)
                        )
                        DropdownMenu(
                            expanded = isTaskListDataDropdownActive,
                            onDismissRequest = { isTaskListDataDropdownActive = false },
                            modifier = Modifier.height(170.dp)
                        ) {
                            if (taskListDataList.isEmpty()) {

                                DropdownMenuItem(
                                    onClick = {
                                        taskListDataSelected = "     "
                                        taskListDataSelectedIndex = -1
                                        isTaskListDataDropdownActive = false
                                    },
                                    modifier = Modifier.background(
                                        if (taskListDataSelectedIndex == -1) {
                                            Color.LightGray
                                        } else {
                                            Color.White
                                        }
                                    )
                                ) {
                                    Text("     ")
                                }
                            }
                            taskListDataList.forEachIndexed { index, taskListData ->
                                DropdownMenuItem(
                                    onClick = {
                                        taskListDataSelected = taskListData.taskName
                                        taskListDataSelectedIndex = index
                                        isTaskListDataDropdownActive = false
                                    }, modifier = Modifier.background(
                                        if (taskListDataSelectedIndex == index) {
                                            Color.LightGray
                                        } else {
                                            Color.White
                                        }
                                    )
                                ) {
                                    Text(taskListData.taskName)
                                }
                            }
                        }
                    }
                    rowSmallSeperator()
                    Row {
                        Column {
                            // create new task button
                            Button(
                                onClick = {
                                    newTaskListPopup = true
                                }) {
                                Text("New Task List")
                            }
                        }
                        Column {
                            // create new task button
                            Button(
                                onClick = {
                                    editTaskListPopup = true
                                }) {
                                Text("Edit Task List")
                            }
                        }
                    }
                    rowSmallSeperator()
                    // client list
                    if (websocketConnectionClient != null) {
                        Row {
                            Column {
                                Text("Client List:", fontSize = 20.sp)
                                for (clientName in websocketConnectionClient?.getExecClientListVariable() ?: listOf()) {
                                    Row {
                                        Button(
                                            enabled = selectedClient.isBlank(),
                                            onClick = {
                                                selectedClient = clientName
                                            }) {
                                            Text(clientName)
                                        }
                                    }
                                }

                            }
                        }


                    }

                }
            }
        }
    }

    if (saveEntry) {
        var toSave = false
        var toSaveFile = false
        if (newTaskListPopup) {
            val tempTaskListData = TaskListData(
                fileName = newTaskListName + ".json",
                taskName = newTaskListName,
            )
            if (!tempTaskListData.fileExists()) {
                tempTaskListData.saveToFile()
                taskListDataList.add(tempTaskListData)
                toSaveFile = true
                newTaskListPopup = false
                newTaskListName = ""
            } else {
                errorText = "Task list already exists"
                println("Task list already exists")
            }
        } else if (editTaskListPopup) {
            val tempTaskListData = taskListDataList[taskListDataSelectedIndex]
            tempTaskListData.saveToFile()
            toSaveFile = true
            editTaskListPopup = false
        } else if (addTaskEntry) {
            val tempTaskListData = TaskFunctions.getTaskFromGuiData(
                entryTypeList[entryTypeSelectedIndex].first.get(),
                taskEntryData
            )
            if (tempTaskListData == null) {
                errorText = "Task type not found"
                toSave = false
                addTaskEntry = false
                editTaskListPopup = true
            } else {
                taskListDataList[taskListDataSelectedIndex].addTaskActionData(
                    TaskActionData(
                        clientName = selectedClient,
                        taskName = newTaskEntryName,
                        taskData = tempTaskListData.toJson()
                    )
                )
                newTaskEntryName = ""
                toSave = true
                addTaskEntry = false
                editTaskListPopup = true
            }
            taskEntryData.clear()
        }
        if (toSave) {
            saveEntry = false
        } else if (toSaveFile) {
            applicationData.saveToFile()
            saveEntry = false
        }
    }

}

@Composable
@Preview
fun rowBigSeperator() {
    Row(modifier = Modifier.height(10.dp)) {

    }
}

@Composable
@Preview
fun rowSmallSeperator() {
    Row(modifier = Modifier.height(2.dp)) {

    }
}


fun connectToServer(applicationData: ApplicationData): WebsocketConnectionClient {
    val websocketConnectionClient = WebsocketConnectionClient(applicationData)
    websocketConnectionClient.connect()
    websocketConnectionClient.waitForConnection()
    return websocketConnectionClient
}

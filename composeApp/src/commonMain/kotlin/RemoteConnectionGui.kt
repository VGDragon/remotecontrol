import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import connection.client.WebsocketConnectionClient
import filedata.ApplicationData
import filedata.TaskActionData
import filedata.TaskListData
import guiElemetns.rowBigSeperator
import guiElemetns.rowSmallSeperator
import guiElemetns.writeErrorText
import messages.WebsocketMessageClient
import messages.base.client.MessageClientClientList
import messages.tasks.*
import org.jetbrains.compose.resources.ExperimentalResourceApi


@OptIn(ExperimentalResourceApi::class)
@Composable
fun App() {
    // TODO: deactivate the disconnect button after disconnecting (if the server does it)
    // TODO: if an task list is deleted, set a new index
    //var connectButtonText by remember { mutableStateOf("Connect") }
    //var disconnectButtonText by remember { mutableStateOf("Disconnect") }

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

    // UI state
    val pushedTaskListDataName by remember { mutableStateOf(mutableStateMapOf<String, String>()) }
    var saveEntry by remember { mutableStateOf(false) }
    var newTaskListPopup by remember { mutableStateOf(false) }
    var editTaskListPopup by remember { mutableStateOf(false) }

    // new task list items
    var newTaskListName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    // task list data
    // change to map

    var taskListDataMap by remember {
        mutableStateOf(
            TaskListData.getTaskListDataFiles().mapIndexed { index: Int, s: TaskListData -> index to s }
                .toMutableStateMap()
        )
    }
    //var taskListDataList by remember { mutableStateOf(TaskListData.getTaskListDataFiles()) }
    var taskListDataSelected by remember { mutableStateOf("") }
    var taskListDataSelectedIndex by remember {
        mutableStateOf(
            if (taskListDataMap.size > 0) 0 else {
                -1
            }
        )
    }
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
    /*
    var scriptNames: MutableList<String> = mutableListOf()
    var scriptNamesDropdownActive by remember { mutableStateOf(false) }
    var scriptNameSelectedIndex by remember { mutableStateOf(0) }
    */

    // gpt idea
    // state for triggering GUI update after removing task action data
    //var updateGui by remember { mutableStateOf(1) }

    MaterialTheme {

        Row(modifier = Modifier.height(50.dp)) {
        }

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
                            errorText = ""
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
                    //Row {
                    //    Text("Name: ${taskListDataList[taskListDataSelectedIndex].taskName}")
                    //}
                    Row(modifier = Modifier.requiredHeight(20.dp).requiredWidth(500.dp)) {
                        Text(
                            if (taskListDataSelectedIndex == -1) {
                                "Name:      "
                            } else {
                                "Name: ${taskListDataMap[taskListDataSelectedIndex]!!.taskName}"
                            },
                            modifier = Modifier.clickable(onClick = { isTaskListDataDropdownActive = true })
                                .background(Color.LightGray)
                        )
                        DropdownMenu(
                            expanded = isTaskListDataDropdownActive,
                            onDismissRequest = { isTaskListDataDropdownActive = false },
                            modifier = Modifier.height(170.dp)
                        ) {
                            if (taskListDataMap.isEmpty()) {

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
                            taskListDataMap.forEach { (index, taskListData) ->
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
                        Text("Task List")
                    }

                    //if (updateGui > 0) {
                    if (taskListDataSelectedIndex != -1) {
                        for (taskActionData in taskListDataMap[taskListDataSelectedIndex]!!.taskActionDataList) {
                            rowSmallSeperator()
                            Row(modifier = Modifier.background(color = Color(200, 0, 0, 20))) {
                                Text(taskActionData.taskName)
                                Button(
                                    onClick = {
                                        val tempTaskListData = taskListDataMap[taskListDataSelectedIndex]
                                        tempTaskListData!!.taskActionDataList.remove(taskActionData)
                                        //updateGui += 1
                                        // TODO: gui doesn't update
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .padding(start = 4.dp, top = 4.dp, end = 4.dp, bottom = 4.dp)
                                ) {
                                    Text("Remove", fontSize = 15.sp)
                                }
                            }
                        }
                    }
                    //    updateGui = 1
                    //}
                    rowSmallSeperator()
                    Row {
                        Button(
                            enabled = selectedClient.isNotBlank() && taskListDataSelectedIndex != -1,
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
                    // client list
                    if (websocketConnectionClient != null) {
                        Row {
                            Column {

                                websocketConnectionClient!!.sendMessage(
                                    WebsocketMessageClient(
                                        type = MessageClientClientList.TYPE,
                                        apiKey = applicationData.apiKey,
                                        sendFrom = "",
                                        sendTo = "",
                                        data = ""
                                    )
                                        .toJson()
                                )
                                websocketConnectionClient!!.waitForResponse()
                                Text("Client List:", fontSize = 20.sp)
                                for (clientName in websocketConnectionClient?.execClientList?.toList()
                                    ?: listOf()) {
                                    Row {
                                        Button(
                                            enabled = !selectedClient.equals(clientName),
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
                    rowSmallSeperator()
                    writeErrorText(errorText)
                    rowSmallSeperator()
                    Row {
                        Button(
                            onClick = {
                                saveEntry = true
                            }) {
                            Text("Save")
                        }
                    }
                    rowSmallSeperator()
                    Row {
                        Button(
                            onClick = {
                                taskListDataMap.remove(taskListDataSelectedIndex)!!.deleteTaskListDataFile()
                                applicationData.saveToFile()
                                editTaskListPopup = false
                            }) {
                            Text("Delete")
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
                            //updateGui += 1
                            errorText = ""
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
                            entryTypeList[entryTypeSelectedIndex],
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
                                    Text(entryType)
                                }
                            }
                        }
                    }
                    rowSmallSeperator()

                    // TODO add new task types here
                    // TODO: GUI task information
                    when (entryTypeList[entryTypeSelectedIndex]) {
                        MessageStartTaskScript.TYPE -> {
                            messageStartTaskScriptGuiElements(
                                websocketConnectionClient=websocketConnectionClient!!,
                                selectedClient=selectedClient,
                                taskEntryData=taskEntryData,
                                apiKey = applicationData.apiKey)
                        }
                        MessageStartTaskWaitUntilClientConnected.TYPE -> {
                            messageStartTaskWaitUntilClientConnectedGuiElements(
                                websocketConnectionClient=websocketConnectionClient!!,
                                taskEntryData=taskEntryData,
                                apiKey = applicationData.apiKey
                            )
                        }

                        MessageStartTaskWaitUntilSeconds.TYPE -> {
                            Row {
                                Text("Seconds to wait: ")
                                TextField(
                                    value = taskEntryData["clientToWaitForSeconds"] ?: "",
                                    onValueChange = { seconds ->
                                        taskEntryData["clientToWaitForSeconds"] = seconds.filter { it.isDigit() }
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }

                        else -> Text("unknown task type")
                    }
                    rowSmallSeperator()
                    writeErrorText(errorText)
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

                            Column(modifier = Modifier.height(60.dp)) {
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

                                Thread {
                                    try {
                                        var websocketConnectionClientTemp = WebsocketConnectionClient(
                                            applicationData = applicationData,
                                            executeTask = false)
                                        websocketConnectionClientTemp.connectAndRegister(doJoin = false)
                                        //websocketConnectionClientTemp.sendMessage(
                                        //    WebsocketMessageClient(
                                        //        type = MessageClientClientList.TYPE,
                                        //        apiKey = applicationData.apiKey,
                                        //        sendFrom = "",
                                        //        sendTo = "",
                                        //        data = ""
                                        //    )
                                        //        .toJson()
                                        //)
                                        //websocketConnectionClientTemp.waitForResponse()
                                        applicationData.saveToFile()
                                        websocketConnectionClient = websocketConnectionClientTemp
                                        connectButtonActive.value = false
                                        disconnectButtonActive.value = true
                                    } catch (e: Exception) {

                                        println("Error: Connection failed")
                                        errorText = "Connection failed"
                                        connectButtonActive.value = false
                                        disconnectButtonActive.value = true
                                        applicationData.saveToFile()
                                        e.printStackTrace()
                                    }


                                }.start()
                            }) {
                            Text("Connect")
                        }
                        Button(enabled = disconnectButtonActive.value,
                            onClick = {
                                websocketConnectionClient!!.isConnected = false
                                websocketConnectionClient!!.stopConnection()
                                websocketConnectionClient = null
                                connectButtonActive.value = true
                                disconnectButtonActive.value = false
                            }) {
                            Text("Disconnect")
                        }
                    }
                    rowSmallSeperator()
                    if (disconnectButtonActive.value) {
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
                                        //updateGui += 1
                                    }) {
                                    Text("Edit Task List")
                                }
                            }
                        }
                        rowBigSeperator()
                        taskListDataMap.forEach { (index, it) ->
                            Row {
                                Button(
                                    enabled = !pushedTaskListDataName.contains(it.taskName),
                                    onClick = {
                                        pushedTaskListDataName[it.taskName] = ""
                                        Thread {
                                            Thread.sleep(1000)
                                            pushedTaskListDataName.remove(it.taskName)
                                        }.start()
                                        it.startTaskList(ws = websocketConnectionClient!!)
                                        //updateGui += 1
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier
                                        .padding(start = 2.dp, top = 2.dp, end = 2.dp, bottom = 2.dp)
                                ) {
                                    Text(it.taskName, fontSize = 15.sp)
                                }
                            }
                        }
                    }

                }
            }
        }
    }

    if (saveEntry) {
        saveEntry = false
        var toSave = false
        var toSaveFile = false
        if (newTaskListPopup) {
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
        } else if (editTaskListPopup) {
            val tempTaskListData = taskListDataMap[taskListDataSelectedIndex]!!
            tempTaskListData.saveToFile()
            toSaveFile = true
            editTaskListPopup = false
        } else if (addTaskEntry) {
            val tempTaskListData = TaskFunctions.getTaskFromGuiData(
                taskType = entryTypeList[entryTypeSelectedIndex],
                client = selectedClient,
                entryTypeData = taskEntryData
            )
            if (tempTaskListData == null) {
                errorText = "Task type not found"
                toSave = false
            } else {
                taskListDataMap[taskListDataSelectedIndex]!!.taskActionDataList.add(
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
            //updateGui += 1
            taskEntryData.clear()
        }
        if (toSaveFile) {
            applicationData.saveToFile()
        }
    }
}


fun connectToServer(applicationData: ApplicationData): WebsocketConnectionClient? {
    try {
        val websocketConnectionClient = WebsocketConnectionClient(applicationData)
        websocketConnectionClient.startConnection()

        while (!websocketConnectionClient.isConnected) {
            Thread.sleep(100)
        }
        return websocketConnectionClient
    } catch (e: Exception) {
        println("Error: ${e.message}")
        return null
    }
}

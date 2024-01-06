import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import connection.WebsocketConnectionClient
import filedata.ApplicationData
import filedata.TaskListData
import messages.WebsocketMessageClient
import messages.base.ServerAnswerStatus
import messages.base.client.MessageClientAddClientBridge
import messages.base.client.MessageClientClientList
import messages.base.client.MessageClientRemoveClientBridge


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

    // connect and disconnect buttons
    val connectButtonActive = remember { mutableStateOf(true) }
    val disconnectButtonActive = remember { mutableStateOf(false) }

    // client bridged
    var doBrideClient by remember { mutableStateOf("") }
    var isClientBridged by remember { mutableStateOf("") }
    var doRemoveBrideClient by remember { mutableStateOf("") }

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
                        }) {
                        Text("Close")
                    }
                    Column {
                        Text("Edit Task List")
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

                    Row(modifier = Modifier.requiredHeight(20.dp). requiredWidth(500.dp)) {
                        Text(if (taskListDataSelectedIndex == -1){"     "} else {taskListDataList[taskListDataSelectedIndex].taskName},
                            modifier = Modifier.clickable(onClick = { isTaskListDataDropdownActive = true })
                                .background(Color.LightGray))
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
                                DropdownMenuItem(onClick = {
                                    taskListDataSelected = taskListData.taskName
                                    taskListDataSelectedIndex = index
                                    isTaskListDataDropdownActive = false
                                }, modifier = Modifier.background(
                                    if (taskListDataSelectedIndex == index){ Color.LightGray} else {Color.White})) {
                                    Text(taskListData.taskName)
                                }
                            }
                        }
                    }

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
                                        type = MessageClientClientList.TYPE,
                                        apiKey = applicationData.apiKey,
                                        data = ""
                                    )
                                        .toJson()
                                )
                                connectButtonActive.value = false
                                disconnectButtonActive.value = true
                                isClientBridged = ""
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
                    // seperator
                    rowBigSeperator()
                    Row { Text("Server Info:", fontSize = 20.sp) }
                    rowSmallSeperator()
                    // client list
                    if (websocketConnectionClient != null) {
                        Row {
                            Column {
                                Text("Client List:", fontSize = 20.sp)
                                for (clientName in websocketConnectionClient?.getExecClientListVariable() ?: listOf()) {
                                    // add button for each client
                                    if (!isClientBridged.isBlank() && isClientBridged != clientName) {
                                        continue
                                    }
                                    Button(
                                        enabled = isClientBridged.isBlank() && doBrideClient.isBlank(),
                                        onClick = {
                                            doBrideClient = clientName
                                        }) {
                                        Text(clientName)
                                    }
                                }

                                // add remove bridge button
                                if (!isClientBridged.isBlank()) {
                                    Button(
                                        onClick = {
                                            doRemoveBrideClient = isClientBridged
                                        }) {
                                        Text("Remove Bridge")
                                    }
                                }

                            }
                        }


                    }

                    if (!isClientBridged.isBlank()) {
                        // add task list
                        // TODO: add task list
                        Row {
                            Column {
                                // create new task button
                                Button(
                                    onClick = {
                                        newTaskListPopup = true
                                    }) {
                                    Text("Add Client task")
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    if (!doBrideClient.isBlank()) {
        val clientNameToBridgeTo = doBrideClient
        val bridgingThread = Thread {
            isClientBridged = brideClient(websocketConnectionClient!!, clientNameToBridgeTo, applicationData)
        }
        doBrideClient = ""
        bridgingThread.start()
    }
    if (!doRemoveBrideClient.isBlank()) {
        val clientNameToBridgeTo = doRemoveBrideClient
        val bridgingThread = Thread {
            removeClientBridge(websocketConnectionClient!!, clientNameToBridgeTo, applicationData)
            isClientBridged = ""
        }
        doRemoveBrideClient = ""
        bridgingThread.start()
    }
    if (saveEntry) {
        var toSave = false
        if (newTaskListPopup) {
            val tempTaskListData = TaskListData(
                fileName = newTaskListName + ".json",
                taskName = newTaskListName,
            )
            if (!tempTaskListData.fileExists()) {
                tempTaskListData.saveToFile()
                taskListDataList.add(tempTaskListData)
                toSave = true
                newTaskListPopup = false
                newTaskListName = ""
            } else {
                errorText = "Task list already exists"
                println("Task list already exists")
            }
        }
        if (toSave) {
            applicationData.saveToFile()
            saveEntry = false
        }
    }

}

fun brideClient(
    websocketConnectionClient: WebsocketConnectionClient,
    clientName: String,
    applicationData: ApplicationData
): String {
    println("Client: Adding client bridge - $clientName")
    websocketConnectionClient.send(
        WebsocketMessageClient(
            type = MessageClientAddClientBridge.TYPE,
            apiKey = applicationData.apiKey,
            data = MessageClientAddClientBridge(
                clientName = clientName
            ).toJson()
        ).toJson()
    )
    val response = websocketConnectionClient.waitForResponse()
    if (response.status == ServerAnswerStatus.OK) {
        println("Client: Client bridge added")
        return clientName
    } else {
        println("Client: Client bridge not added")
        return ""
    }
}

fun removeClientBridge(
    websocketConnectionClient: WebsocketConnectionClient,
    clientName: String,
    applicationData: ApplicationData
) {
    println("Client: Removing client bridge - $clientName")

    websocketConnectionClient.send(
        WebsocketMessageClient(
            type = MessageClientRemoveClientBridge.TYPE,
            apiKey = applicationData.apiKey,
            data = ""
        ).toJson()
    )
    val response = websocketConnectionClient.waitForResponse()
    if (response.status == ServerAnswerStatus.OK) {
        println("Client: Client bridge removed")
    } else {
        println("Client: Client bridge not removed")
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
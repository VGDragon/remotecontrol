import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import connection.WebsocketConnectionClient
import messages.WebsocketMessageClient
import messages.base.ServerAnswerStatus
import messages.base.client.MessageClientAddClientBridge
import messages.base.client.MessageClientBridgedClients
import messages.base.server.MessageServerBridgedClients
import messages.base.client.MessageClientClientList
import messages.base.client.MessageClientRemoveClientBridge
import kotlin.concurrent.thread


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
    val stateVertical  by remember { mutableStateOf(ScrollState(0)) }
    val stateHorizontal  by remember { mutableStateOf(ScrollState(0)) }

    // connect and disconnect buttons
    val connectButtonActive = remember { mutableStateOf(true) }
    val disconnectButtonActive = remember { mutableStateOf(false) }

    // client bridged
    var doBrideClient by remember { mutableStateOf("") }
    var isClientBridged by remember { mutableStateOf("") }
    var doRemoveBrideClient by remember { mutableStateOf("") }

    // GUI Data Class
    //val guiDataClass by remember { mutableStateOf(GuiDataClass()) }

    MaterialTheme {
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
                rowSmallSeperator()
                Row {
                    Button(enabled = connectButtonActive.value,
                        onClick = {
                            websocketConnectionClient = connectToServer(applicationData)
                            websocketConnectionClient!!.send(
                                WebsocketMessageClient(
                                    type = MessageClientClientList.TYPE,
                                    apiKey = applicationData.apiKey,
                                    data = "")
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

                            // add task list
                            // TODO: add task list
                        }
                    }
                }
            }
        }
    }

    if (!doBrideClient.isBlank()){
        val clientNameToBridgeTo = doBrideClient
        val bridgingThread = Thread {
            isClientBridged = brideClient(websocketConnectionClient!!, clientNameToBridgeTo, applicationData)
        }
        doBrideClient = ""
        bridgingThread.start()
    }
    if (!doRemoveBrideClient.isBlank()){
        val clientNameToBridgeTo = doRemoveBrideClient
        val bridgingThread = Thread {
            removeClientBridge(websocketConnectionClient!!, clientNameToBridgeTo, applicationData)
            isClientBridged = ""
        }
        doRemoveBrideClient = ""
        bridgingThread.start()
    }

}

fun brideClient(websocketConnectionClient: WebsocketConnectionClient, clientName: String, applicationData: ApplicationData): String {
    println("Client: Adding client bridge - $clientName")
    websocketConnectionClient.send(
        WebsocketMessageClient(
            type = MessageClientAddClientBridge.TYPE,
            apiKey = applicationData.apiKey,
            data = MessageClientAddClientBridge(clientName = clientName
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

fun removeClientBridge(websocketConnectionClient: WebsocketConnectionClient, clientName: String, applicationData: ApplicationData) {
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
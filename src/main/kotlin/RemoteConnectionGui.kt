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



@Composable
@Preview
fun App() {
    var connectButtonText by remember { mutableStateOf("Connect") }
    var disconnectButtonText by remember { mutableStateOf("Disconnect") }

    val applicationData = ApplicationData.fromFile()
    var websocketConnectionClient: WebsocketConnectionClient? = null

    // connection variables
    var connectionAddress by remember { mutableStateOf(TextFieldValue(applicationData.address)) }
    var connectionPort by remember { mutableStateOf(TextFieldValue(applicationData.port.toString())) }


    // scroll states
    val stateVertical  by remember { mutableStateOf(ScrollState(0)) }
    val stateHorizontal  by remember { mutableStateOf(ScrollState(0)) }

    // connect and disconnect buttons
    val connectButtonActive = remember { mutableStateOf(true) }
    val disconnectButtonActive = remember { mutableStateOf(false) }

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
                                Button(onClick = {
                                    websocketConnectionClient!!.send(
                                        WebsocketMessageClient(
                                            type = MessageClientAddClientBridge.TYPE,
                                            apiKey = applicationData.apiKey,
                                            data = MessageClientAddClientBridge(clientName = clientName
                                            ).toJson()
                                        ).toJson()
                                    )
                                    val response = websocketConnectionClient!!.waitForResponse()
                                    if (response.status == ServerAnswerStatus.OK) {
                                        println("Client: Client bridge added")
                                    } else {
                                        println("Client: Client bridge not added")
                                    }
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
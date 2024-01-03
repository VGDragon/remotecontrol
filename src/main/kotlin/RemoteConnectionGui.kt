import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import connection.WebsocketConnectionClient
import messages.WebsocketMessageClient
import messages.base.client.MessageClientClientList

@Composable
@Preview
fun App() {
    var connectButtonText by remember { mutableStateOf("Connect") }
    var disconnectButtonText by remember { mutableStateOf("Disconnect") }

    val applicationData = ApplicationData.fromFile()
    var websocketConnectionClient: WebsocketConnectionClient? = null

    // connection variables
    var connectionAddress by remember { mutableStateOf(TextFieldValue(applicationData.ip)) }
    var connectionPort by remember { mutableStateOf(TextFieldValue(applicationData.port.toString())) }


    // scroll states
    val stateVertical  by remember { mutableStateOf(ScrollState(0)) }
    val stateHorizontal  by remember { mutableStateOf(ScrollState(0)) }

    // connect and disconnect buttons
    val connectButtonActive = remember { mutableStateOf(true) }
    val disconnectButtonActive = remember { mutableStateOf(false) }

    // GUI Data Class
    val guiDataClass by remember { mutableStateOf(GuiDataClass()) }

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
                                applicationData.ip = address.text
                            })

                    }
                }
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
            }
        }
    }
}


@Composable
@Preview
fun addressRow() {
    Row(modifier = Modifier.height(50.dp)) {

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
    Row(modifier = Modifier.height(10.dp)) {

    }
}

fun connectToServer(applicationData: ApplicationData): WebsocketConnectionClient {
    val websocketConnectionClient = WebsocketConnectionClient(applicationData)
    websocketConnectionClient.connect()
    return websocketConnectionClient
}
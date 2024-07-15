package messages.tasks
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import connection.client.WebsocketConnectionClient
import interfaces.TaskInterface
import interfaces.TaskMessageInterface
import messages.WebsocketMessageClient
import messages.base.client.MessageClientClientList
import tasks.TaskStartWaitUntilClientConnected

class MessageStartTaskWaitUntilClientConnected(override val type: String, override val clientTo: String, val clientToWaitFor: String,): TaskMessageInterface {
    /*
     type: String - the type of the message (MessageStartTaskScript.TYPE)
        clientTo: String - the client to send the message to

        scriptName: String - the name of the script to start
     */
    override fun toJson(): String {
        return Gson().toJson(this)
    }

    override fun toTask(websocketConnectionClient: WebsocketConnectionClient, nextTask: TaskInterface?, startedFrom: String): TaskInterface {
        /*
        websocketConnectionClient: WebsocketConnectionClient - the websocket connection client
        nextTask: TaskInterface? - the next task to run
        startedFrom: String - the name of the client that started the task chain
         */
        return TaskStartWaitUntilClientConnected(
            clientToWaitFor = clientToWaitFor,
            websocketConnectionClient = websocketConnectionClient,
            nextTask = nextTask,
            startedFrom = startedFrom)
    }

    companion object {
        fun fromJson(json: String): MessageStartTaskWaitUntilClientConnected {
            return Gson().fromJson(json, MessageStartTaskWaitUntilClientConnected::class.java)
        }
        const val TYPE = "waitUntilClientConnectedTask"


    }
}

@Composable
fun messageStartTaskWaitUntilClientConnectedGuiElements(websocketConnectionClient: WebsocketConnectionClient?,
                                                        taskEntryData: MutableMap<String, String>,
                                                        apiKey: String) {

    var waitUntilClientOnlineSelectedName by remember { mutableStateOf("") }
    var waitUntilClientOnlineDropdownActive by remember { mutableStateOf(false) }

    websocketConnectionClient!!.sendMessage(
        WebsocketMessageClient(
            type = MessageClientClientList.TYPE,
            apiKey = apiKey,
            sendFrom = websocketConnectionClient.computerName,
            sendTo = "",
            data = ""
        ).toJson()
    )
    websocketConnectionClient.waitForResponse()
    val clientList = websocketConnectionClient.execClientList.toList()
    if (clientList.isEmpty()) {
        waitUntilClientOnlineSelectedName = ""
    } else if (waitUntilClientOnlineSelectedName.isBlank()) {
        waitUntilClientOnlineSelectedName = clientList[0]
    }
    Row {
        Text("Client to wait for: ")
        Text(
            waitUntilClientOnlineSelectedName, modifier = Modifier.clickable(
                onClick = {
                    waitUntilClientOnlineDropdownActive = true
                })
                .background(Color.LightGray)
        )
        DropdownMenu(
            expanded = waitUntilClientOnlineDropdownActive,
            onDismissRequest = { waitUntilClientOnlineDropdownActive = false },
            modifier = Modifier.height(170.dp)
        ) {
            websocketConnectionClient.sendMessage(
                WebsocketMessageClient(
                    type = MessageClientClientList.TYPE,
                    apiKey = apiKey,
                    sendFrom = "",
                    sendTo = "",
                    data = ""
                )
                    .toJson()
            )
            websocketConnectionClient.waitForResponse()
            val clientListTemp = websocketConnectionClient.execClientList.toList()
            if (clientListTemp.isEmpty()) {
                waitUntilClientOnlineSelectedName = ""
                DropdownMenuItem(
                    onClick = {
                        waitUntilClientOnlineSelectedName = ""
                        waitUntilClientOnlineDropdownActive = false
                    }, modifier = Modifier.background(
                        Color.LightGray
                    )
                ) {
                    Text("     ")
                }
            } else {
                if (waitUntilClientOnlineSelectedName.isBlank()) {
                    waitUntilClientOnlineSelectedName = clientListTemp[0]
                }
                clientListTemp.forEachIndexed { index, clientName ->
                    DropdownMenuItem(
                        onClick = {
                            taskEntryData["clientToWaitForClient"] = clientName
                            waitUntilClientOnlineSelectedName = clientName
                            waitUntilClientOnlineDropdownActive = false
                        }, modifier = Modifier.background(
                            if (waitUntilClientOnlineSelectedName == clientName) {
                                Color.LightGray
                            } else {
                                Color.White
                            }
                        )
                    ) {
                        Text(clientName)
                    }
                }
            }
        }


    }

}
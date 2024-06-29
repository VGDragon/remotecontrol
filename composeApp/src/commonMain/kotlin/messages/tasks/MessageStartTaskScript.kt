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
import connection.WebsocketConnectionClient
import interfaces.TaskInterface
import interfaces.TaskMessageInterface
import messages.WebsocketMessageClient
import messages.base.client.MessageClientScriptList
import tasks.TaskStartScript

class MessageStartTaskScript(override val type: String, override val clientTo: String, val scriptName: String = ""): TaskMessageInterface {
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
        return TaskStartScript(scriptName = scriptName,
            websocketConnectionClient = websocketConnectionClient,
            nextTask = nextTask,
            startedFrom = startedFrom)
    }

    companion object {
        fun fromJson(json: String): MessageStartTaskScript {
            return Gson().fromJson(json, MessageStartTaskScript::class.java)
        }
        fun getTaskFromGuiData(entryTypeData: MutableMap<String, String>, clientTo: String): TaskMessageInterface? {
            if (entryTypeData["scriptName"].isNullOrBlank()){
                return null
            }
            return MessageStartTaskScript(type= TYPE, clientTo=clientTo, scriptName = entryTypeData["scriptName"]!!)

        }
        const val TYPE = "StartScrip"

    }
}
@Composable
fun messageStartTaskScriptGuiElements(websocketConnectionClient: WebsocketConnectionClient?,
                                      selectedClient: String,
                                      taskEntryData: MutableMap<String, String>,
                                      apiKey: String) {

    var scriptNames: MutableList<String> = mutableListOf()
    var scriptNamesDropdownActive by remember { mutableStateOf(false) }
    var scriptNameSelectedIndex by remember { mutableStateOf(0) }

    websocketConnectionClient!!.sendMessage(
        WebsocketMessageClient(
            type = MessageClientScriptList.TYPE,
            apiKey = apiKey,
            sendTo = selectedClient,
            sendFrom = websocketConnectionClient.computerName,
            data = ""
        ).toJson()
    )
    val answer = websocketConnectionClient!!.waitForResponse()
    scriptNames = answer.message as MutableList<String>
    if (scriptNames.isEmpty()) {
        taskEntryData["scriptName"] = ""
        scriptNameSelectedIndex = -1
    } else {
        taskEntryData["scriptName"] = scriptNames[scriptNameSelectedIndex]
    }

    Row {
        Text("script")

        Text(
            if (scriptNameSelectedIndex == -1) {
                "     "
            } else {
                scriptNames[scriptNameSelectedIndex]
            },
            modifier = Modifier.clickable(onClick = { scriptNamesDropdownActive = true })
                .background(Color.LightGray)
        )
        DropdownMenu(
            expanded = scriptNamesDropdownActive,
            onDismissRequest = { scriptNamesDropdownActive = false },
            modifier = Modifier.height(170.dp)
        ) {
            websocketConnectionClient!!.sendMessage(
                WebsocketMessageClient(
                    type = MessageClientScriptList.TYPE,
                    apiKey = apiKey,
                    sendTo = selectedClient,
                    sendFrom = websocketConnectionClient!!.computerName,
                    data = ""
                )
                    .toJson()
            )
            val answer = websocketConnectionClient!!.waitForResponse()
            scriptNames = answer.message as MutableList<String>
            if (scriptNames.isEmpty()) {
                taskEntryData["scriptName"] = ""
                scriptNameSelectedIndex = -1

                DropdownMenuItem(
                    onClick = {
                        scriptNameSelectedIndex = -1
                        scriptNamesDropdownActive = false
                    }, modifier = Modifier.background(
                        Color.LightGray
                    )
                ) {
                    Text("     ")
                }

            } else {
                taskEntryData["scriptName"] = scriptNames[scriptNameSelectedIndex]

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
                        Text(scriptName)
                    }
                }
            }
        }
    }
}
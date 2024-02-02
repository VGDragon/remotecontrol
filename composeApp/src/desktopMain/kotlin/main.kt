import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import connection.WebsocketConnectionClient
import connection.WebsocketConnectionServer
import filedata.ApplicationData
import java.io.File


fun main(args: Array<String>) {
    val argumentList = mutableListOf<String>()
    val argumentMap = mutableMapOf<String, String>()
    argumentMap["server"] = "false"
    argumentMap["client"] = "true"
    argumentMap["port"] = "8088"
    argumentMap["help"] = "false"
    argumentMap["address"] = "127.0.0.1"
    argumentMap["exec"] = "false"

    var lastOption = ""
    for (arg in args) {
        if (arg.startsWith("--")) {
            lastOption = arg.substring(2)
            argumentMap[lastOption] = "true"
        } else {
            if (lastOption == "") {
                argumentList.add(arg)
            } else {
                argumentMap[lastOption] = arg
            }
        }
    }

    if (argumentMap["server"] == "true") {
        val applicationData = ApplicationData.fromFile()
        val websocketConnectionServer = WebsocketConnectionServer(applicationData)
        websocketConnectionServer.start()
        while (true) {
            Thread.sleep(1000)
        }
    } else if(argumentMap["exec"] == "true" && argumentMap["client"] == "true"){
        val scriptFolderFile = File(GlobalVariables.scriptFolder)
        if (!scriptFolderFile.exists()){
            scriptFolderFile.mkdirs()
        }
        val applicationData = ApplicationData.fromFile()
        val websocketConnectionClient = WebsocketConnectionClient(applicationData, argumentMap["exec"] == "true")
        websocketConnectionClient.connectAndRegister()
    } else if(argumentMap["client"] == "true") {
        application {
            Window(onCloseRequest = ::exitApplication) {
                App()
            }
        }
    }
}

fun phaseArguments(args: Array<String>) : Pair<List<String>, Map<String, String>> {
    val arguments = mutableListOf<String>()
    val options = mutableMapOf<String, String>()
    options["server"] = "false"
    options["client"] = "true"
    options["port"] = "8088"
    options["help"] = "false"
    options["address"] = "127.0.0.1"
    options["exec"] = "false"

    var lastOption = ""
    for (arg in args) {
        if (arg.startsWith("--")) {
            lastOption = arg.substring(2)
            options[lastOption] = "true"
        } else {
            if (lastOption == "") {
                arguments.add(arg)
            } else {
                options[lastOption] = arg
            }
        }
    }
    return Pair(arguments, options)
}


@Preview
@Composable
fun AppDesktopPreview() {
    App()
}
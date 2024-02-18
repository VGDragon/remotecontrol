import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import connection.WebsocketConnectionClient
import connection.WebsocketConnectionServer
import filedata.ApplicationData
import interfaces.TaskMessageInterface
import messages.tasks.MessageStartTaskScript
import java.io.File


fun testingClientExecScript(){
    GlobalVariables.appFolderName = File("data", "client_exec_1").absolutePath
    GlobalVariables.computerName = System.getenv("COMPUTERNAME") + "_1"
    GlobalVariables.createFolders()
    val applicationData = ApplicationData.fromFile()

    val ws_client_exec = WebsocketConnectionClient(applicationData, true)
    ws_client_exec.connectAndRegister(doJoin = true)
    println()
}

fun main(args: Array<String>) {
    testingClientExecScript()
}

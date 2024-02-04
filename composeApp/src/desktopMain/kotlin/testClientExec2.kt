import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import connection.WebsocketConnectionClient
import connection.WebsocketConnectionServer
import filedata.ApplicationData
import interfaces.TaskMessageInterface
import messages.tasks.MessageStartTaskScript
import java.io.File


fun testingClientExecScript2(){
    GlobalVariables.computerName = System.getenv("COMPUTERNAME") + "_2"
    GlobalVariables.createFolders()
    // storing a class in a variable and create a class object from it
    val applicationData = ApplicationData.fromFile()

    val ws_client_exec = WebsocketConnectionClient(applicationData, true)
    ws_client_exec.connectAndRegister(doJoin = true)
}

fun main(args: Array<String>) {
    testingClientExecScript2()
}

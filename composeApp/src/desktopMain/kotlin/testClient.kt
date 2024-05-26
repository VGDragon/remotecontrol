import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import connection.WebsocketConnectionClient
import filedata.ApplicationData
import filedata.TaskActionData
import filedata.TaskListData
import messages.tasks.MessageStartTaskScript


fun testingClientScript(){

    GlobalVariables.computerName = System.getenv("COMPUTERNAME")
    // storing a class in a variable and create a class object from it
    val applicationData = ApplicationData.fromFile()
    //val ws_server = WebsocketConnectionServer(applicationData)
    //ws_server.start()
    //Thread.sleep(3000)

    //val ws_client_exec = WebsocketConnectionClient(applicationData, true)
    val ws_client = WebsocketConnectionClient(applicationData)
    //ws_client_exec.connectAndRegister(doJoin = true)

    application {
        Window(onCloseRequest = ::exitApplication) {
            App()
        }
    }
}

fun main(args: Array<String>) {
    testingClientScript()
}

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import connection.WebsocketConnectionClient
import connection.WebsocketConnectionServer
import filedata.ApplicationData
import interfaces.TaskMessageInterface
import messages.tasks.MessageStartTaskScript
import java.io.File

class testServer {
    fun websocketServer(): WebsocketConnectionServer {
        val applicationData = ApplicationData.fromFile()
        val ws = WebsocketConnectionServer(applicationData)
        ws.start()
        return ws
    }
    fun websocketClient(): WebsocketConnectionClient {
        val applicationData = ApplicationData.fromFile()
        val ws = WebsocketConnectionClient(applicationData)
        ws.connect()
        return ws
    }

    fun start(){
        val applicationData = ApplicationData.fromFile()
        val ws = WebsocketConnectionServer(applicationData)
        ws.start()
        while (ws.getKeepRunning()) {
            Thread.sleep(1000)
        }
        ws.stop()
    }
}

fun startServerWithGuiTest(){
    val applicationData = ApplicationData.fromFile()


    val websocketConnectionServer = WebsocketConnectionServer(applicationData)
    websocketConnectionServer.start()
    Thread.sleep(3000)


    val scriptFolderFile = File(GlobalVariables.scriptFolder())
    if (!scriptFolderFile.exists()){
        scriptFolderFile.mkdirs()
    }
    val websocketConnectionClient = WebsocketConnectionClient(applicationData, true)
    websocketConnectionClient.connectAndRegister(doJoin = false)

    Thread.sleep(2000)

    application {
        Window(onCloseRequest = ::exitApplication) {
            App()
        }
    }
}

fun testingServerScript(){
    GlobalVariables.computerName = System.getenv("COMPUTERNAME")
    // storing a class in a variable and create a class object from it
    val applicationData = ApplicationData.fromFile()
    val ws_server = WebsocketConnectionServer(applicationData)
    ws_server.start()
    Thread.sleep(3000)

    //val ws_client_exec = WebsocketConnectionClient(applicationData, true)
    //val ws_client = WebsocketConnectionClient(applicationData)
    //ws_client_exec.connectAndRegister(doJoin = false)
    //ws_client.connectAndRegister(doJoin = false)


    //var messageStartTaskBaseConvertObject = messageStartTaskBaseConvertClass.first(functionVariableMap)
    //task list
    val taskList = mutableListOf<TaskMessageInterface>()
    taskList.add(MessageStartTaskScript(type = MessageStartTaskScript.TYPE, clientTo = GlobalVariables.computerName + "_executable", scriptName = "testScript"))
    taskList.add(MessageStartTaskScript(type = MessageStartTaskScript.TYPE, clientTo = "Hearuhi", scriptName = "testScript"))
    //TaskFunctions.startTaskHandler(ws_client_exec, taskList, GlobalVariables.computerName)
    println()
    //ws_client.stopConnection()
    //ws_client_exec.stopConnection()
    //ws_server.stop()
}

fun main(args: Array<String>) {
    testingServerScript()
    //startServerWithGuiTest()
}

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import connection.WebsocketConnectionClient
import connection.WebsocketConnectionServer
import filedata.ApplicationData
import messages.*
import messages.base.server.MessageServerClientList
import messages.base.MessageStartTask
import messages.base.ServerAnswerStatus
import messages.base.client.MessageClientClientList
import messages.tasks.MessageStartTaskScript
import java.io.File
import kotlin.reflect.cast
import kotlin.reflect.safeCast

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


    val scriptFolderFile = File(GlobalVariables.scriptFolder)
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

fun testingScript(){
    // storing a class in a variable and create a class object from it
    var functionVariableMap: Map<String, String> = mapOf("type" to "testScript", "scriptName" to "testScript")
    var functionvariableList: List<String> = listOf("type", "scriptName")
    var messageStartTaskBaseConvertClass = Pair(
        MessageStartTaskScript::toJson, MessageStartTaskScript::fromMap)

    //var messageStartTaskBaseConvertObject = messageStartTaskBaseConvertClass.first(functionVariableMap)
    println()
}

fun main(args: Array<String>) {
    //testingScript()
    startServerWithGuiTest()
}

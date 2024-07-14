import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import connection.RestServer
import connection.WebsocketConnectionClient
import connection.WebsocketConnectionServer
import filedata.ApplicationData
import java.io.File
import java.net.InetAddress

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
        while (true){
            try {
                Thread.sleep(1000)
            } catch (e: InterruptedException){
               break
            }
        }
        ws.stop()
    }
}

fun startServerWithGuiTest(){
    val applicationData = ApplicationData.fromFile()


    val websocketConnectionServer = WebsocketConnectionServer(applicationData)
    //websocketConnectionServer.start()
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
    GlobalVariables.applicationFolderName = File(File("data", "server"), "server")
        .absoluteFile.relativeTo(File("").absoluteFile).path
    GlobalVariables.jarFolder = File("data", "server").relativeTo(File(""))
        .absoluteFile.relativeTo(File("").absoluteFile).path
    val applicationsData = ApplicationData.fromFile()
    if (applicationsData.computerName.isEmpty()){
        val name = InetAddress.getLocalHost().hostName
        applicationsData.computerName = name
        applicationsData.saveToFile()
        GlobalVariables.computerName = applicationsData.computerName
    } else {
        GlobalVariables.computerName = applicationsData.computerName
    }

    var name = InetAddress.getLocalHost().hostName
    GlobalVariables.computerName = name
    GlobalVariables.createFolders()
    // storing a class in a variable and create a class object from it
    val applicationData = ApplicationData.fromFile()
    val wsServer = WebsocketConnectionServer(applicationData)
    val restServer = RestServer().build(applicationData.port + 1)
    restServer.start(wait = false)
    wsServer.start()
}

fun main(args: Array<String>) {
    testingServerScript()
    //startServerWithGuiTest()
}

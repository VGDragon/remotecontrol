import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import connection.RestServer
import connection.WebsocketConnectionClient
import connection.WebsocketConnectionServer
import filedata.ApplicationData
import java.io.File
import java.net.InetAddress


fun main(args: Array<String>) {

    var name = InetAddress.getLocalHost().hostName
    GlobalVariables.computerName = name

    if (args.isNotEmpty()) {
        val startType = args[0]
        if (startType == "--help"){
            println("you search for 'prepare'")
            return
        }
        if (startType == "prepare") {
            GlobalVariables.createFolders()
            ApplicationData.fromFile()
            return
        } else {
            println("Unknown start type")
            return
        }
    }
    GlobalVariables.createFolders()
    val applicationData = ApplicationData.fromFile()

    if (applicationData.isServer) {
        val websocketConnectionServer = WebsocketConnectionServer(applicationData)
        val restServer = RestServer().build(applicationData.port + 1)
        restServer.start(wait = false)
        websocketConnectionServer.start()
        while (true) {
            Thread.sleep(1000)
        }
    } else if(applicationData.exec && applicationData.isClient){
        val scriptFolderFile = File(GlobalVariables.scriptFolder())
        if (!scriptFolderFile.exists()){
            scriptFolderFile.mkdirs()
        }
        val websocketConnectionClient = WebsocketConnectionClient(applicationData, true)
        websocketConnectionClient.connectAndRegister()
    } else if(applicationData.isClient) {
        application {
            Window(onCloseRequest = ::exitApplication) {
                App()
            }
        }
    }
}


@Preview
@Composable
fun AppDesktopPreview() {
    App()
}
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


    if (args.isNotEmpty()) {
        val startType = args[0]
        if (startType.equals("--help", ignoreCase = true)){
            println("prepare")
            println("key [client_name]")
            return
        }
        if (startType.equals("prepare", ignoreCase = true)) {
            GlobalVariables.createFolders()
            ApplicationData.fromFile()
            return
        } else if (startType.equals("key", ignoreCase = true)){
            if (args.size < 2){
                println("key [client_name]")
                return
            }
            GlobalVariables.keyPairsFolder = "prepared_keypairs"
            GlobalVariables.crateKeyPairsFolder()
            val applicationData= ApplicationData.fromFile()
            applicationData.isClient = true
            applicationData.exec = true
            applicationData.isServer = false
            applicationData.computerName = args[1]
            val websocketConnectionClient = WebsocketConnectionClient(applicationData, true)

            websocketConnectionClient.connectAndRegister(doJoin = true)
            return
        } else {
            println("Unknown start type")
            return
        }
    }

    GlobalVariables.createFolders()
    val applicationData = ApplicationData.fromFile()
    if (applicationData.computerName.isEmpty()){
        val name = InetAddress.getLocalHost().hostName
        applicationData.computerName = name
        applicationData.saveToFile()
        GlobalVariables.computerName = applicationData.computerName
    } else {
        GlobalVariables.computerName = applicationData.computerName
    }

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
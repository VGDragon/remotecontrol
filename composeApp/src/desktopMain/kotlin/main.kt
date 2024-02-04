import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import connection.WebsocketConnectionClient
import connection.WebsocketConnectionServer
import filedata.ApplicationData
import java.io.File


fun main(args: Array<String>) {
    GlobalVariables.computerName = System.getenv("COMPUTERNAME")
    GlobalVariables.createFolders()
    val applicationData = ApplicationData.fromFile()

    if (applicationData.isServer) {
        val websocketConnectionServer = WebsocketConnectionServer(applicationData)
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
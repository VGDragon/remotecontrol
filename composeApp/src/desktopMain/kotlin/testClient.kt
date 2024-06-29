import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import connection.WebsocketConnectionClient
import filedata.ApplicationData
import filedata.TaskActionData
import filedata.TaskListData
import messages.tasks.MessageStartTaskScript
import java.io.File
import java.net.InetAddress


fun testingClientScript(){

    GlobalVariables.applicationFolderName = File(File("data", "client"), "client")
        .absoluteFile.relativeTo(File("").absoluteFile).path
    GlobalVariables.createFolders()
    GlobalVariables.jarFolder = File(GlobalVariables.applicationFolderName).absoluteFile.parentFile.canonicalPath
    // get path of the JAR file

    val applicationData = ApplicationData.fromFile()
    if (applicationData.computerName.isEmpty()){
        val name = InetAddress.getLocalHost().hostName
        applicationData.computerName = name
        applicationData.saveToFile()
        GlobalVariables.computerName = applicationData.computerName
    } else {
        GlobalVariables.computerName = applicationData.computerName
    }

    application {
        Window(onCloseRequest = ::exitApplication) {
            App()
        }
    }
}

fun main(args: Array<String>) {
    testingClientScript()
}

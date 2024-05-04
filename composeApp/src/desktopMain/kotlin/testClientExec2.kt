import connection.WebsocketConnectionClient
import filedata.ApplicationData
import java.io.File


fun testingClientExecScript2(){
    GlobalVariables.applicationFolderName = File(File("data", "client_exec_2"), "client")
        .absoluteFile.relativeTo(File("").absoluteFile).path
    GlobalVariables.jarFolder = File("data", "client_exec_2").relativeTo(File(""))
        .absoluteFile.relativeTo(File("").absoluteFile).path
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

import connection.WebsocketConnectionClient
import filedata.ApplicationData
import java.io.File


fun testingClientExecScript2(){
    GlobalVariables.applicationFolderName = File("data", "client_exec_2").absolutePath
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

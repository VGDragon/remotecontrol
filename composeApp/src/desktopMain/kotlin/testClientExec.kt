import connection.WebsocketConnectionClient
import filedata.ApplicationData
import java.io.File


fun testingClientExecScript(){
    GlobalVariables.applicationFolderName = File(File("data", "client_exec_1"), "client")
        .absoluteFile.relativeTo(File("").absoluteFile).path
    GlobalVariables.jarFolder = File("data", "client_exec_1").relativeTo(File(""))
        .absoluteFile.relativeTo(File("").absoluteFile).path
    GlobalVariables.computerName = System.getenv("COMPUTERNAME") + "_1"
    GlobalVariables.createFolders()
    GlobalVariables.jarFolder = File(GlobalVariables.applicationFolderName).absoluteFile.parentFile.canonicalPath
    // get path of the JAR file
    val applicationData = ApplicationData.fromFile()
    GlobalVariables.jarName = File(applicationData.jarFilePath).name
    GlobalVariables.jarFolder = File(applicationData.jarFilePath).parentFile.canonicalPath
    GlobalVariables.createUpdateFolder()

    val ws_client_exec = WebsocketConnectionClient(applicationData, true)
    ws_client_exec.connectAndRegister(doJoin = true)
    println()

}

fun main(args: Array<String>) {
    testingClientExecScript()
}

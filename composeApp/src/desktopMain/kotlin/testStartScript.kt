import connection.client.WebsocketConnectionClient
import filedata.ApplicationData
import java.io.File


fun testStartScript() {
    GlobalVariables.applicationFolderName = File("data", "test_script").absolutePath
    GlobalVariables.keyPairsFolder = "prepared_keypairs"
    GlobalVariables.crateKeyPairsFolder()
    val applicationData = ApplicationData.fromFile()
    applicationData.isClient = true
    applicationData.exec = true
    applicationData.isServer = false
    applicationData.computerName = "gaming_server"
    GlobalVariables.computerName = applicationData.computerName
    val websocketConnectionClient = WebsocketConnectionClient(applicationData, true)
    if(GlobalVariables.preparedKeyPairExists(websocketConnectionClient.computerName)){
        println("Key pairs already exist")
        return
    }
    websocketConnectionClient.prepareConnection()


}

fun main(args: Array<String>) {
    testStartScript()
}

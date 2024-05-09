import connection.ConnectionKeyPair
import connection.RestServer
import filedata.ApplicationData
import filedata.SoftwareUpdate
import filedata.UpdateStatus
import messages.WebsocketMessageServer
import messages.base.server.MessageServerUpdate
import java.io.File
import java.net.InetAddress

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun sendPost(urlString: String = "http://127.0.0.1:8080/test",
             bodyContent: String = "test") {
    val client = HttpClient.newBuilder().build();
    val request = HttpRequest.newBuilder()
        .uri(URI.create(urlString))
        //.header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(bodyContent))
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    val answer = response.body()
    println(answer)
}

class TempScript {


    fun temp() {
        val connectionKeyPair = ConnectionKeyPair("test", "test").generateKeyPair()
        connectionKeyPair.privateKeyTarget = connectionKeyPair.ownPrivateKey
        val testString = "This is a test."
        var sendString = ""
        for (i in 0..1000) {
            sendString += testString
        }
        val encryptedString = connectionKeyPair.encrypt(sendString)
        //println("Encrypted: $encryptedString")
        val decryptedString = connectionKeyPair.decrypt(encryptedString)
        //println("Decrypted: $decryptedString")
        println(sendString == decryptedString)
        return
        val restServer = RestServer().build(8080)
        restServer.start(wait = false)
        Thread.sleep(1000)
        sendPost()
        restServer.stop()

        while (true) {
            println("Server is running")
            Thread.sleep(1000)
        }
    }
}

fun updatefile() {
    GlobalVariables.applicationFolderName = File("data", "server").absolutePath
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
    GlobalVariables.jarFolder = File(GlobalVariables.applicationFolderName).absoluteFile.parentFile.absolutePath
    GlobalVariables.computerName = name
    GlobalVariables.createFolders()

    var softwareUpdate: SoftwareUpdate? = SoftwareUpdate.newUpdateFile() ?: return
    var fileBytes = softwareUpdate!!.readFilePart()
    val messageList: MutableList<String> = mutableListOf()
    while (fileBytes != null) {
        val message = WebsocketMessageServer(
            type = MessageServerUpdate.TYPE,
            sendFrom = GlobalVariables.computerName,
            data = softwareUpdate.toMessageJson()
        ).toJson()
        messageList.add(message)
        fileBytes = softwareUpdate.readFilePart()
    }

    softwareUpdate = null
    val softwareUpdateDataList = SoftwareUpdate.fromFile()
    while (true){
        for (message in messageList){
            var websocketMessageServer: WebsocketMessageServer = WebsocketMessageServer.fromJson(message)
            //websocketMessageServer.data
            var messageServerUpdate = MessageServerUpdate.fromJson(websocketMessageServer.data)

            if (softwareUpdate == null){
                softwareUpdate = SoftwareUpdate.fromServerMessage(messageServerUpdate)
                softwareUpdate!!.version = "0.0.4"
            }
            val softwareUpdateData = softwareUpdateDataList.filter { it.version.equals(softwareUpdate.version) }
            if (!softwareUpdateData.isEmpty()){
                if (softwareUpdateData[0].updateStatus == UpdateStatus.FINISHED){
                    println("Update already installed for version ${softwareUpdate.version}")
                    continue
                }
            }
            softwareUpdate.writeFilePart(messageServerUpdate)
            if (softwareUpdate.updateStatus == UpdateStatus.FINISHED){
                break
            }
        }
        break
    }

}

fun main() {
    //val test = TempTestScript()
    //test.test()
    updatefile()
}
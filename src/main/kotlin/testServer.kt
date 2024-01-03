import connection.WebsocketConnectionClient
import connection.WebsocketConnectionServer
import messages.*
import messages.base.server.MessageServerClientList
import messages.base.client.MessageClientConnect
import messages.base.MessageStartTask
import messages.base.ServerAnswerStatus
import messages.base.client.MessageClientClientList
import messages.tasks.MessageStartTaskScript

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
        while (ws.getKeepRunning()) {
            Thread.sleep(1000)
        }
        ws.stop()
    }
}

fun testingScript(){
    val applicationData = ApplicationData.fromFile()
    val websocketConnectionClient = WebsocketConnectionClient(applicationData, false)
    websocketConnectionClient.connect()
    websocketConnectionClient.waitForConnection()
    if (websocketConnectionClient.getIsConnectionError()) {
        println("Connection error")
        return
    }
    // get list of clients
    websocketConnectionClient.send(
        WebsocketMessageClient(
            type = MessageClientClientList.TYPE,
            apiKey = applicationData.apiKey,
            data = "")
            .toJson())
    var serverInfo = websocketConnectionClient.waitForResponse()
    if (serverInfo.status != ServerAnswerStatus.OK) {
        println("Server response: ${serverInfo.status} ${serverInfo.message}")
        return
    }
    val messageClientList = serverInfo.message as MessageServerClientList
    if (messageClientList.clientNames.isEmpty()) {
        println("No clients found")
        return
    }
    // connect to first client
    websocketConnectionClient.send(
        WebsocketMessageClient(
            type = MessageClientConnect.TYPE,
            apiKey = applicationData.apiKey,
            data = MessageClientConnect(
                clientName = messageClientList.clientNames[0])
                .toJson())
            .toJson())
    if (websocketConnectionClient.waitForResponse().status != ServerAnswerStatus.OK) {
        println("Client not found")
        return
    }
    // send task
    val taskList = mutableListOf<String>()
    taskList.add(
        MessageStartTaskScript(
            type = MessageStartTaskScript.TYPE,
            scriptName = "testing.bat").toJson()
    )
    websocketConnectionClient.send(
        WebsocketMessageClient(
            type = MessageStartTask.TYPE,
            apiKey = applicationData.apiKey,
            data = MessageStartTask(
                taskList = taskList)
                .toJson())
            .toJson())
    serverInfo = websocketConnectionClient.waitForResponse()
    if (serverInfo.status != ServerAnswerStatus.OK) {
        println("Server response: ${serverInfo.status} ${serverInfo.message}")
        return
    }
    websocketConnectionClient.close()
    println(serverInfo)
}

fun main(args: Array<String>) {
    testingScript()
}

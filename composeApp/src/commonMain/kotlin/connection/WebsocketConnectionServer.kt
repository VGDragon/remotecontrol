package connection

import GlobalVariables
import badclient.BadClientHandler
import connection.connectionConfig.Connection
import connection.connectionConfig.ConnectionData
import connection.connectionConfig.configureRouting
import connection.connectionConfig.configureSockets
import filedata.ApplicationData
import filedata.SoftwareUpdate
import filedata.UpdateStatus
import messages.WebsocketMessageClient
import messages.WebsocketMessageServer
import messages.base.MessageBase
import messages.base.MessageReceived
import messages.base.server.MessageServerUpdate
import java.util.*

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

// ktor websocket
fun Application.module() {
    val websocketConnectionServer = ConnectionData.websocketConnectionServer!!
    val port = ConnectionData.port!!
    configureRouting()
    configureSockets(
        websocketConnectionServer,
        port = port
    )
}

class WebsocketConnectionServer {
    val applicationData: ApplicationData
    var websocketServerMessageHandler: WebsocketServerMessageHandler
    var keepWsRunning: Boolean = true
    var embeddedServer: NettyApplicationEngine? = null

    var websocketClients: MutableMap<Connection, String?> = mutableMapOf()
    val clientTaskRunningPermission: MutableMap<String, Connection> = mutableMapOf()

    // key map for encryption
    val keyMap: MutableMap<String, ConnectionKeyPair> = mutableMapOf()
    val lastServerMessageMap: MutableMap<Connection, String> = mutableMapOf()
    val lastClientMessageMap: MutableMap<Connection, String> = mutableMapOf()

    // last message time
    val lastClientMessageTime: MutableMap<Connection, Long> = mutableMapOf()

    // counter for messages
    val serverMessageIdCounter: MutableMap<String, Long> = mutableMapOf()
    val clientMessageId: MutableMap<String, Long> = mutableMapOf()

    //// software update
    val clientUpdateDoneNames: MutableMap<String, Int> = mutableMapOf()
    val waitingForClientList: MutableMap<String, String?> = mutableMapOf()
    var softwareUpdate: SoftwareUpdate? = null
    var clientsToUpdate: List<String>? = null
    var updateUploadFinishedTime = 0L
    var restartMessageSendTime = 0L


    ///////// messages
    //val handleMessageThread: Thread

    val connections = Collections.synchronizedSet<Connection?>(LinkedHashSet())

    constructor(applicationData: ApplicationData) {
        this.applicationData = applicationData
        this.websocketServerMessageHandler = WebsocketServerMessageHandler(applicationData)
        println("Server: Port ${applicationData.port} started")
        /*
        this.handleMessageThread = Thread {
            while (true) {
                println("Server: Handling messages")
                val messagesToHandle: MutableList<Connection> = mutableListOf()
                for (connection in connections) {
                    if (connection.receivedQueue.isNotEmpty()) {
                        messagesToHandle.add(connection)
                    }
                }
                if (messagesToHandle.isNotEmpty()) {
                    for (connection in messagesToHandle) {
                        val message = connection.receivedQueue.remove()
                        handleMessage(connection, message)
                        lastClientMessageTime[connection] = System.currentTimeMillis()
                    }
                } else {
                    handleSoftwareUpdate()
                }
                val currentTime = System.currentTimeMillis()
                val clientsToRemove = mutableListOf<Connection>()
                lastClientMessageTime.forEach {
                    if (currentTime - it.value > GlobalVariables.pingPongDelayTime * 4) {
                        clientsToRemove.add(it.key)
                        println("Server: ${it.key} - Ping Pong timeout")
                    }
                }

                clientsToRemove.forEach {
                    if (lastClientMessageTime[it] != null) {
                        lastClientMessageTime.remove(it)
                    }
                    val clientName = websocketClients.remove(it)
                    if (clientName != null) {
                        if (clientTaskRunningPermission[clientName] != null) {
                            clientTaskRunningPermission.remove(clientName)
                        }
                    }
                    it.closeSession = true
                }

                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        */
        ConnectionData.websocketConnectionServer = this
        ConnectionData.port = applicationData.port
        embeddedServer = embeddedServer(Netty, port = applicationData.port, module = Application::module)
    }

    fun start(wait: Boolean = true){
        startThreads()
        embeddedServer!!.start(wait = wait)
    }

    fun stop() {
        keepWsRunning = false
        stopThreads()
        for (connection in connections) {
            connection.closeSession = true
        }
        embeddedServer!!.stop(1000, 1000)
    }

    fun startThreads() {
        //handleMessageThread.start()
    }

    fun stopThreads() {
        //handleMessageThread.interrupt()

    }


    fun handleMessage(ws: Connection, message: String) {

        if (BadClientHandler.badClientMap.contains(ws)) {
            println("bad client")
            BadClientHandler.handleBadClient(ws)
            return
        }
        // send server name if client name is empty
        val messageBase = MessageBase.fromJson(message)
        if (messageBase.name.isEmpty()) {
            ws.sendQueue.add(MessageBase("", GlobalVariables.computerName, 0L).toJson())
            return
        }
        // decrypt message
        val receivedMessage = decryptMessage(
            clientName = messageBase.name,
            message = messageBase.msg
        ) ?: return BadClientHandler.handleBadClient(ws)
        if (!clientMessageId.containsKey(messageBase.name)) {
            clientMessageId[messageBase.name] = 0
        }

        val messageClass = WebsocketMessageClient.fromJson(receivedMessage)
        if (messageClass.type == MessageReceived.TYPE) {
            waitingForClientList[messageBase.name] = null
            return
        }
        val nextClientMessageId = clientMessageId[messageBase.name]!! + 1
        if (nextClientMessageId != messageBase.messageId) {
            return
        }
        sendMessage(
            ws = ws,
            message = WebsocketMessageClient(
                type = MessageReceived.TYPE,
                apiKey = applicationData.apiKey,
                sendFrom = GlobalVariables.computerName,
                sendTo = "",
                data = messageBase.messageId.toString()
            ).toJson(),
            increate_message_id = false
        )

        if (!clientMessageId.containsKey(messageBase.name)) {
            clientMessageId[messageBase.name] = 0
        }
        clientMessageId[messageBase.name] = clientMessageId[messageBase.name]!! + 1
        lastClientMessageMap[ws] = message
        websocketServerMessageHandler.handle(this, ws, messageClass)

    }

    fun handleClientDisconnect(ws: Connection) {

        if (lastClientMessageTime[ws] != null) {
            lastClientMessageTime.remove(ws)
        }

        val clientName = websocketClients.remove(ws) ?: return

        clientTaskRunningPermission.remove(clientName)
        waitingForClientList.remove(clientName)
        serverMessageIdCounter.remove(clientName)
        clientMessageId.remove(clientName)

    }

    fun sendMessage(
        ws: Connection,
        message: String,
        increate_message_id: Boolean = true,
        saveMessage: Boolean = true
    ) {
        if (message.isEmpty()) {
            return
        }
        val clientName = websocketClients[ws] ?: return
        waitingForClientList[clientName] = message

        if (!keyMap.containsKey(clientName)) {
            return
        }

        val messageToSend = encryptMessage(clientName, message) ?: return
        if (!serverMessageIdCounter.containsKey(clientName)) {
            serverMessageIdCounter[clientName] = 0
        }
        if (increate_message_id) {
            serverMessageIdCounter[clientName] = serverMessageIdCounter[clientName]!! + 1
        }
        val serverMessageId = serverMessageIdCounter[clientName]!!
        val encryptedMessageToSend =
            MessageBase(
                name = GlobalVariables.computerName,
                msg = messageToSend,
                messageId = serverMessageId
            ).toJson()
        if (saveMessage) {
            lastServerMessageMap[ws] = encryptedMessageToSend
        }
        ws.sendQueue.add(encryptedMessageToSend)
    }

    fun decryptMessage(clientName: String, message: String): String? {
        if (!keyMap.containsKey(clientName)) {
            val connectionKeyPair = ConnectionKeyPair.loadFile(clientName) ?: return null
            keyMap[clientName] = connectionKeyPair
        }
        return keyMap[clientName]?.decrypt(message)

    }


    fun encryptMessage(clientName: String, message: String): String? {
        if (!keyMap.containsKey(clientName)) {
            val connectionKeyPair = ConnectionKeyPair.loadFile(clientName) ?: return null
            keyMap[clientName] = connectionKeyPair
        }
        return keyMap[clientName]?.encrypt(message)

    }

    fun handleSoftwareUpdate() {
        for (message in waitingForClientList.values) {
            if (message != null) {
                return
            }
        }
        if (softwareUpdate == null) {
            softwareUpdate = SoftwareUpdate.newUpdateFile() ?: return
        }
        val tempSoftwareUpdate = softwareUpdate ?: return
        if (tempSoftwareUpdate.updateStatus == UpdateStatus.EVERYTHING_DONE) {
            if (System.currentTimeMillis() - restartMessageSendTime < 1000 * 5) {
                return
            }
            tempSoftwareUpdate.startUpdate()
            return
        }

        if (tempSoftwareUpdate.updateStatus == UpdateStatus.NOT_STARTED) {
            clientsToUpdate = clientTaskRunningPermission.keys.toList()
            clientsToUpdate!!.forEach {
                clientUpdateDoneNames[it] = 0

            }
            tempSoftwareUpdate.updateStatus = UpdateStatus.RUNNING
        }
        if (tempSoftwareUpdate.partAmount <= tempSoftwareUpdate.currentpart) {
            tempSoftwareUpdate.updateStatus = UpdateStatus.FINISHED
        }

        if (tempSoftwareUpdate.updateStatus == UpdateStatus.RUNNING) {
            tempSoftwareUpdate.readFilePart()

            if (tempSoftwareUpdate.partAmount - tempSoftwareUpdate.currentpart < 10) {
                println("Server: Sending part ${tempSoftwareUpdate.currentpart} of ${tempSoftwareUpdate.partAmount}")
            } else if (tempSoftwareUpdate.currentpart % 10 == 0L) {
                println("Server: Sending part ${tempSoftwareUpdate.currentpart} of ${tempSoftwareUpdate.partAmount}")
            }

            val message = WebsocketMessageServer(
                type = MessageServerUpdate.TYPE,
                sendFrom = GlobalVariables.computerName,
                data = tempSoftwareUpdate.toMessageJson()
            ).toJson()
            for (clientName in clientsToUpdate!!) {
                if (clientUpdateDoneNames[clientName] == 1) {
                    continue
                }
                val ws = clientTaskRunningPermission[clientName] ?: continue
                sendMessage(ws, message)
                waitingForClientList[clientName] = message
            }
            return
        }

        if (tempSoftwareUpdate.updateStatus == UpdateStatus.FINISHED) {
            updateUploadFinishedTime = System.currentTimeMillis()
            var allDone = 1
            clientUpdateDoneNames.forEach {
                if (it.value == 0) {
                    allDone = 0
                }
                if (it.value < 0) {
                    allDone = -1
                    return@forEach
                }
            }
            if (allDone == -1) {
                tempSoftwareUpdate.updateStatus = UpdateStatus.ERROR
            } else if (allDone == 1) {
                tempSoftwareUpdate.updateStatus = UpdateStatus.CLIENTS_DONE
            }
        }
        if (tempSoftwareUpdate.updateStatus == UpdateStatus.FINISHED &&
            System.currentTimeMillis() - updateUploadFinishedTime > 1000 * 60 * 5
        ) {
            println("Server: Update taking too long")
            // TODO: handle error
        }

        if (tempSoftwareUpdate.updateStatus == UpdateStatus.ERROR) {
            softwareUpdate = null
            clientsToUpdate = null
            println("Server: Update finished")
            return
        }

        if (tempSoftwareUpdate.updateStatus != UpdateStatus.CLIENTS_DONE) {
            return
        }


        tempSoftwareUpdate.toMessageJson()
        val message = MessageServerUpdate(
            version = tempSoftwareUpdate.version,
            hash = tempSoftwareUpdate.hashValue,
            size = tempSoftwareUpdate.partSize,
            packageNr = tempSoftwareUpdate.currentpart,
            packageAmount = tempSoftwareUpdate.partAmount,
            restart = true
        ).toJson()

        for (clientName in clientsToUpdate!!) {
            val ws = clientTaskRunningPermission[clientName] ?: continue
            sendMessage(
                ws,
                WebsocketMessageServer(
                    type = MessageServerUpdate.TYPE,
                    sendFrom = GlobalVariables.computerName,
                    data = message
                ).toJson()
            )
            waitingForClientList[clientName] = message
        }
        tempSoftwareUpdate.updateStatus = UpdateStatus.EVERYTHING_DONE
        restartMessageSendTime = System.currentTimeMillis()
    }
}





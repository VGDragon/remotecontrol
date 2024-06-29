package connection

import GlobalVariables
import badclient.BadClientHandler
import filedata.ApplicationData
import filedata.SoftwareUpdate
import filedata.UpdateStatus
import messages.WebsocketMessageClient
import messages.WebsocketMessageServer
import messages.base.MessageBase
import messages.base.MessageReceived
import messages.base.server.MessageServerUpdate
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.LinkedBlockingQueue


class WebsocketConnectionServer : WebSocketServer {
    val applicationData: ApplicationData
    var websocketServerMessageHandler: WebsocketServerMessageHandler
    var keepWsRunning: Boolean = true

    var websocketClients: MutableMap<WebSocket, String?> = mutableMapOf()
    val clientTaskRunningPermission: MutableMap<String, WebSocket> = mutableMapOf()

    // key map for encryption
    val keyMap: MutableMap<String, ConnectionKeyPair> = mutableMapOf()
    val lastServerMessageMap: MutableMap<WebSocket, String> = mutableMapOf()
    val lastClientMessageMap: MutableMap<WebSocket, String> = mutableMapOf()
    // last message time
    val lastClientMessageTime: MutableMap<WebSocket, Long> = mutableMapOf()

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
    //val messageReceivedQueue = LinkedList<Pair<WebSocket, String>>()
    //val messageSendQueue = LinkedList<Pair<WebSocket, String>>()
    val messageReceivedQueue = ConcurrentLinkedQueue<Pair<WebSocket, String>>()
    val messageSendQueue = LinkedBlockingQueue<Pair<WebSocket, String>>()
    val handleMessageThread: Thread
    val sendMessageThread: Thread = Thread {
        while (true) {
            val messagePair = messageSendQueue.take()
            messagePair.first.send(messagePair.second)
        }
    }

    constructor(applicationData: ApplicationData) : super(InetSocketAddress(applicationData.port)) {
        this.applicationData = applicationData
        this.websocketServerMessageHandler = WebsocketServerMessageHandler(applicationData)
        println("Server: Port ${applicationData.port} started")

        this.handleMessageThread = Thread {
            while (true) {
                if (messageReceivedQueue.isNotEmpty()) {
                    val messagePair = messageReceivedQueue.remove()
                    val ws = messagePair.first
                    val message = messagePair.second
                    lastClientMessageTime[ws] = System.currentTimeMillis()
                    handleMessage(ws, message)
                } else {
                    handleSoftwareUpdate()
                }
                val currentTime = System.currentTimeMillis()
                val clientsToRemove = mutableListOf<WebSocket>()
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
                    it.close()
                }

                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
    }

    fun startThreads() {
        handleMessageThread.start()
        sendMessageThread.start()
    }

    fun stopThreads() {
        handleMessageThread.interrupt()
        sendMessageThread.interrupt()
    }

    override fun onOpen(p0: WebSocket?, p1: ClientHandshake?) {
        if (p0 == null) {
            return
        }
        websocketClients[p0] = null
        lastClientMessageTime[p0] = System.currentTimeMillis()
        println("Server: Client connected")
    }

    override fun onMessage(p0: WebSocket?, p1: String?) {
        if (p1 == null || p0 == null) {
            return
        }
        lastClientMessageTime[p0] = System.currentTimeMillis()
        messageReceivedQueue.add(Pair(p0, p1))

    }

    override fun onError(p0: WebSocket?, p1: Exception?) {
        println("Server: Error")

        if (p1 != null) {
            p1.printStackTrace()
        }
        if (p0 == null) {
            return
        }
        handleClientDisconnect(p0)
        p0.close()
    }

    override fun onClose(p0: WebSocket?, p1: Int, p2: String?, p3: Boolean) {
        println("Server: Client disconnected")
        if (p0 == null) {
            return
        }
        handleClientDisconnect(p0)
    }


    fun handleMessage(ws: WebSocket, message: String) {

        if (BadClientHandler.badClientMap.contains(ws)) {
            println("bad client")
            BadClientHandler.handleBadClient(ws)
            return
        }
        // send server name if client name is empty
        val messageBase = MessageBase.fromJson(message)
        if (messageBase.name.isEmpty()) {
            ws.send(MessageBase("", GlobalVariables.computerName, 0L).toJson())
            return
        }
        // decrypt message
        val receivedMessage = decryptMessage(
            clientName = messageBase.name,
            message = messageBase.msg
        ) ?: return BadClientHandler.handleBadClient(ws)
        if (!clientMessageId.containsKey(messageBase.name)){
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

    fun handleClientDisconnect(ws: WebSocket) {

        if (lastClientMessageTime[ws] != null) {
            lastClientMessageTime.remove(ws)
        }

        val clientName = websocketClients.remove(ws) ?: return

        clientTaskRunningPermission.remove(clientName)
        waitingForClientList.remove(clientName)
        serverMessageIdCounter.remove(clientName)
        clientMessageId.remove(clientName)

    }

    override fun onStart() {
        startThreads()
    }

    override fun stop() {
        super.stop()
        keepWsRunning = false
        stopThreads()
    }

    fun sendMessage(ws: WebSocket,
                    message: String,
                    increate_message_id: Boolean = true,
                    saveMessage: Boolean = true) {
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
        ws.send(encryptedMessageToSend)
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
        for ( message in waitingForClientList.values) {
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

        if (tempSoftwareUpdate.updateStatus == UpdateStatus.RUNNING){
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
            if (allDone == -1){
                tempSoftwareUpdate.updateStatus = UpdateStatus.ERROR
            } else if (allDone == 1) {
                tempSoftwareUpdate.updateStatus = UpdateStatus.CLIENTS_DONE
            }
        }
        if (tempSoftwareUpdate.updateStatus == UpdateStatus.FINISHED &&
            System.currentTimeMillis() - updateUploadFinishedTime > 1000 * 60 * 5) {
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





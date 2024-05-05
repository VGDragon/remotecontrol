package connection

import GlobalVariables
import badclient.BadClientHandler
import filedata.ApplicationData
import filedata.SoftwareUpdate
import messages.WebsocketMessageClient
import messages.WebsocketMessageServer
import messages.base.MessageBase
import messages.base.MessageReceived
import messages.base.client.MessageClientRequestMessage
import messages.base.server.MessageServerRequestMessage
import messages.base.server.MessageServerUpdate
import okhttp3.internal.wait
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress


class WebsocketConnectionServer : WebSocketServer {
    val applicationData: ApplicationData
    var websocketClients: MutableList<WebSocket>
    var websocketClientsLock = Object()
    var websocketClientMessageHandler: WebsocketServerMessageHandler
    var keepWsRunning: Boolean = true
    var keepWsRunningLock = Object()
    val clientTaskRunningPermission: MutableMap<String, WebSocket> = mutableMapOf()
    val clientTaskRunningPermissionLock = Object()
    val clientConnectedNames: MutableMap<String, WebSocket> = mutableMapOf()
    val clientConnectedNamesLock = Object()
    val keyMap: MutableMap<String, ConnectionKeyPair> = mutableMapOf()
    val keyMapLock = Object()
    val lastClientMessageTime: MutableMap<String, Long> = mutableMapOf()
    val lastClientMessageTimeLock = Object()
    var pingPongThread: Thread? = null
    val pingPongThreadLock = Object()
    val pingPongDelayTime = GlobalVariables.pingPongDelayTime
    var updateJarThread: Thread? = null
    val updateJarThreadLock = Object()

    val lastServerMessagesWithId: MutableMap<String, MutableMap<Long, String>> = mutableMapOf()
    val lastServerMessagesWithIdLock = Object()
    val serverMessageIdCounter: MutableMap<String, Long> = mutableMapOf()
    val serverMessageIdCounterLock = Object()

    val clientMessageId: MutableMap<String, Long> = mutableMapOf()
    val clientMessageIdLock = Object()
    val clientUpdateDoneNames: MutableMap<String, Int> = mutableMapOf()
    val clientUpdateDoneNamesLock = Object()

    val waitingForClientList: MutableMap<String, String?> = mutableMapOf()
    val waitingForClientListLock = Object()

    constructor(applicationData: ApplicationData) : super(InetSocketAddress(applicationData.port)) {
        this.applicationData = applicationData
        this.websocketClients = mutableListOf()
        this.websocketClientMessageHandler = WebsocketServerMessageHandler(applicationData)
    }

    override fun onOpen(p0: WebSocket?, p1: ClientHandshake?) {
        if (p0 != null) {
            synchronized(websocketClientsLock) {
                websocketClients.add(p0)
            }
        }
        println("Server: Client connected")
    }

    override fun onMessage(p0: WebSocket?, p1: String?) {
        if (p1 == null || p0 == null) {
            return
        }

        synchronized(BadClientHandler.badClientMapLock) {
            if (BadClientHandler.badClientMap.contains(p0)) {
                println("bad client")
                BadClientHandler.handleBadClient(p0)
                return
            }
        }
        // send server name if client name is empty
        val messageBase = MessageBase.fromJson(p1)
        if (messageBase.name.isEmpty()) {
            p0.send(MessageBase("", GlobalVariables.computerName, 0L).toJson())
            return
        }

        // update last client message time
        updateLastClientMessageTime(messageBase.name)
        // decrypt message
        val message = decryptMessage(messageBase) ?: return BadClientHandler.handleBadClient(p0)

        val messageClass = WebsocketMessageClient.fromJson(message)
        if (messageClass.type == MessageReceived.TYPE) {
            setWaitingForClient(clientName = messageBase.name, message = null)
            return
        }
        sendMessage(
            ws = p0,
            message = WebsocketMessageClient(
                type = MessageReceived.TYPE,
                apiKey = applicationData.apiKey,
                sendFrom = GlobalVariables.computerName,
                sendTo = "",
                data = messageBase.messageId.toString()
            ).toJson(),
            increate_message_id = false
        )
        waitForClient(messageBase.name, "")
        val nextClientMessageId = getNextClientMessageId(messageBase.name)
        if (nextClientMessageId != messageBase.messageId) {
            return
        }
        incrementClientMessageId(messageBase.name)
        val messageToSend = websocketClientMessageHandler.handle(this, p0, messageClass)
        setWaitingForClient(messageBase.name, messageToSend)
        if (messageToSend != null) {
            sendMessage(p0, messageToSend)
        }

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
    fun handleClientDisconnect(ws: WebSocket){
        synchronized(websocketClientsLock) {
            websocketClients.remove(ws)
        }
        val clientName = getClientConnectedNameFromWs(ws)
        removeClientRegisterItem(ws)
        removeClientConnectedItem(ws)
        if (clientName == null) {
            return
        }
        removeWaitingForClient(clientName)
        removeServerMessageId(clientName)
        removeClientMessageId(clientName)
    }

    override fun onStart() {
        startPingPong()
        updateJarThread()
    }


    fun sendMessageHistory(ws: WebSocket, messageId: Long) {
        val clientName = synchronized(clientConnectedNamesLock) {
            var clientName: String? = null
            clientConnectedNames.forEach {
                if (it.value == ws) {
                    clientName = it.key
                    return@forEach
                }
            }
            if (clientName == null) {
                return
            }
            clientName!!
        }

        synchronized(serverMessageIdCounterLock) {
            synchronized(lastServerMessagesWithIdLock) {
                if (!serverMessageIdCounter.containsKey(clientName)) {
                    serverMessageIdCounter[clientName] = 0
                }
                serverMessageIdCounter[clientName] == messageId
                while (true) {
                    val messageToSend = lastServerMessagesWithId[clientName]!![serverMessageIdCounter[clientName]]
                    if (messageToSend == null) {
                        serverMessageIdCounter[clientName] = serverMessageIdCounter[clientName]!! - 1
                        break
                    }

                    val message = synchronized(keyMapLock) {
                        if (!keyMap.containsKey(clientName)) {
                            return
                        }
                        keyMap[clientName]!!.encrypt(messageToSend)
                    }
                    ws.send(
                        MessageBase(
                            name = GlobalVariables.computerName,
                            msg = message,
                            messageId = serverMessageIdCounter[clientName]!!
                        ).toJson()
                    )
                    serverMessageIdCounter[clientName] = serverMessageIdCounter[clientName]!! + 1
                }
            }
        }
    }

    fun sendMessage(ws: WebSocket, message: String, increate_message_id: Boolean = true) {
        if (message.isEmpty()) {
            return
        }
        val clientName = getClientConnectedNameFromWs(ws) ?: return

        removeServerMessageIfToBig(clientName)
        synchronized(serverMessageIdCounterLock) {
            synchronized(lastServerMessagesWithIdLock) {
                val messageToSend = synchronized(keyMapLock) {
                    if (!keyMap.containsKey(clientName)) {
                        return
                    }
                    val messageToSend = keyMap[clientName]!!.encrypt(message)
                    messageToSend
                }
                if (!serverMessageIdCounter.containsKey(clientName)) {
                    serverMessageIdCounter[clientName] = 0
                }
                if (increate_message_id) {
                    serverMessageIdCounter[clientName] = serverMessageIdCounter[clientName]!! + 1
                }
                val serverMessageId = serverMessageIdCounter[clientName]!!

                if (!lastServerMessagesWithId.containsKey(clientName)) {
                    lastServerMessagesWithId[clientName] = mutableMapOf()
                }
                lastServerMessagesWithId[clientName]!![serverMessageId] = message
                ws.send(
                    MessageBase(
                        name = GlobalVariables.computerName,
                        msg = messageToSend,
                        messageId = serverMessageId
                    ).toJson()
                )

            }
        }
    }

    fun decryptMessage(messageBase: MessageBase): String? {
        synchronized(keyMapLock) {
            val connectionKeyPair = if (!keyMap.containsKey(messageBase.name)) {
                var connectionKeyPair = ConnectionKeyPair.loadFile(messageBase.name) ?: return null
                connectionKeyPair = ConnectionKeyPair.loadFile(messageBase.name) ?: return null
                keyMap[messageBase.name] = connectionKeyPair
                connectionKeyPair
            } else {
                keyMap[messageBase.name]!!
            }
            return connectionKeyPair.decrypt(messageBase.msg)
        }
    }

    fun updateJarThread() {
        synchronized(updateJarThreadLock) {
            if (updateJarThread != null) {
                updateJarThread!!.interrupt()
                updateJarThread = null
            }
            if (updateJarThread == null) {
                updateJarThread = Thread {
                    while (keepWsRunning) {
                        try {
                            Thread.sleep(1000)
                        } catch (e: InterruptedException) {
                            break
                        }
                        val softwareUpdate: SoftwareUpdate = SoftwareUpdate.newUpdateFile() ?: continue
                        val clientNames: List<String> = getClientRegisteredNames()
                        synchronized(clientUpdateDoneNamesLock) {
                            clientNames.forEach {
                                clientUpdateDoneNames[it] = 0
                            }
                        }
                        var fileBytes = softwareUpdate.readFilePart()
                        var i = 1
                        while (fileBytes != null) {
                            if (softwareUpdate.partAmount - i < 10){
                                println("Server: Sending part $i of ${softwareUpdate.partAmount}")
                            } else if (i % 10 == 0) {
                                println("Server: Sending part $i of ${softwareUpdate.partAmount}")
                            }
                            val message = WebsocketMessageServer(
                                type = MessageServerUpdate.TYPE,
                                sendFrom = GlobalVariables.computerName,
                                data = softwareUpdate.toMessageJson()
                            ).toJson()
                            for (clientName in clientNames) {
                                val ws = getClientRegisterItem(clientName) ?: continue
                                waitForClient(clientName, "")
                                sendMessage(ws, message)
                                setWaitingForClient(clientName = clientName, message = message)
                            }
                            fileBytes = softwareUpdate.readFilePart()
                            i++
                        }

                        for (clientName in clientNames) {
                            waitForClient(clientName, null)
                        }
                        var waitTimeStart = System.currentTimeMillis()
                        var gotError = false
                        while (true) {
                            var allDone = 1
                            synchronized(clientUpdateDoneNamesLock) {
                                clientUpdateDoneNames.forEach {
                                    if (it.value == 0) {
                                        allDone = 0
                                    }
                                    if (it.value < 0) {
                                        allDone = -1
                                    }
                                }
                            }
                            if (allDone == 1) {
                                break
                            }
                            if (allDone == -1) {
                                gotError = true
                                break
                            }
                            if (System.currentTimeMillis() - waitTimeStart > 1000 * 60 * 5) {
                                break
                            }
                            try {
                                Thread.sleep(1000)
                            } catch (e: InterruptedException) {
                                return@Thread
                            }
                        }
                        if (gotError) {
                            continue
                        }
                        try {
                            Thread.sleep(1000)
                        } catch (e: InterruptedException) {
                            return@Thread
                        }
                        softwareUpdate.toMessageJson()
                        val message = MessageServerUpdate(
                            version = softwareUpdate.version,
                            hash = softwareUpdate.hashValue,
                            size = softwareUpdate.partSize,
                            packageNr = softwareUpdate.currentpart,
                            packageAmount = softwareUpdate.partAmount,
                            restart = true
                        ).toJson()
                        for (clientName in clientNames) {
                            val ws = getClientRegisterItem(clientName) ?: continue
                            waitForClient(clientName, "")
                            sendMessage(ws,
                                WebsocketMessageServer(
                                    type = MessageServerUpdate.TYPE,
                                    sendFrom = GlobalVariables.computerName,
                                    data = message
                                ).toJson()
                            )
                            setWaitingForClient(clientName = clientName, message = message)
                            waitForClient(clientName, null)
                        }
                        softwareUpdate.startUpdate()
                    }
                }
                updateJarThread!!.start()
            }
        }
    }

    fun startPingPong() {
        synchronized(pingPongThreadLock) {
            if (pingPongThread == null) {
                pingPongThread = Thread {
                    while (keepWsRunning) {
                        try {
                            Thread.sleep(100)
                        } catch (e: InterruptedException) {
                            break
                        }
                        val currentTime = System.currentTimeMillis()
                        val clientsToRemove = mutableListOf<String>()
                        synchronized(lastClientMessageTimeLock) {
                            lastClientMessageTime.forEach {
                                if (currentTime - it.value > pingPongDelayTime * 4) {
                                    clientsToRemove.add(it.key)
                                    println("Server: ${it.key} - Ping Pong timeout")
                                }
                            }
                        }
                        clientsToRemove.forEach {
                            synchronized(lastClientMessageTimeLock) {
                                if (lastClientMessageTime[it] != null) {
                                    lastClientMessageTime.remove(it)
                                }
                            }
                            synchronized(clientConnectedNamesLock) {
                                if (clientConnectedNames[it] != null) {
                                    clientConnectedNames[it]!!.close()
                                    clientConnectedNames.remove(it)
                                }
                            }
                            synchronized(clientTaskRunningPermissionLock) {
                                if (clientTaskRunningPermission[it] != null) {
                                    clientTaskRunningPermission.remove(it)
                                }
                            }
                            synchronized(lastClientMessageTimeLock) {
                                if (lastClientMessageTime[it] != null) {
                                    lastClientMessageTime.remove(it)
                                }
                            }
                        }
                    }
                }
                pingPongThread!!.start()
            }
        }
    }

    // getters and setters
    fun updateLastClientMessageTime(clientName: String) {
        synchronized(lastClientMessageTimeLock) {
            lastClientMessageTime[clientName] = System.currentTimeMillis()
        }
    }

    fun getLastClientMessageTime(clientName: String): Long {
        synchronized(lastClientMessageTimeLock) {
            return lastClientMessageTime[clientName] ?: 0
        }
    }

    fun deleteLastClientMessageTime(clientName: String) {
        synchronized(lastClientMessageTimeLock) {
            lastClientMessageTime.remove(clientName)
        }
    }

    fun getClientRegisteredNames(): List<String> {
        synchronized(clientTaskRunningPermissionLock) {
            return clientTaskRunningPermission.keys.toList()
        }
    }

    fun getClientRegisterItem(key: String): WebSocket? {
        synchronized(clientTaskRunningPermissionLock) {
            return clientTaskRunningPermission[key]
        }
    }

    fun setClientRegisterItem(key: String, value: WebSocket) {
        synchronized(clientTaskRunningPermissionLock) {
            clientTaskRunningPermission[key] = value
        }
    }

    fun getClientRegisterNameFromWs(ws: WebSocket): String? {
        synchronized(clientTaskRunningPermissionLock) {
            for ((key, value) in clientTaskRunningPermission) {
                if (value == ws) {
                    return key
                }
            }
        }
        return null
    }

    fun removeClientRegisterItem(key: String) {
        synchronized(clientTaskRunningPermissionLock) {
            clientTaskRunningPermission.remove(key)
        }
    }

    fun removeClientRegisterItem(ws: WebSocket) {
        val key = getClientRegisterNameFromWs(ws)
        synchronized(clientTaskRunningPermissionLock) {
            if (key != null) {
                clientTaskRunningPermission.remove(key)
            }
        }
    }

    fun getClientConnectedNames(): List<String> {
        synchronized(clientConnectedNamesLock) {
            return clientConnectedNames.keys.toList()
        }
    }

    fun getClientConnectedItem(key: String): WebSocket? {
        synchronized(clientConnectedNamesLock) {
            return clientConnectedNames[key]
        }
    }

    fun setClientConnectedItem(key: String, value: WebSocket) {
        synchronized(clientConnectedNamesLock) {
            clientConnectedNames[key] = value
        }
    }

    fun getClientConnectedNameFromWs(ws: WebSocket): String? {
        synchronized(clientConnectedNamesLock) {
            for ((key, value) in clientConnectedNames) {
                if (value == ws) {
                    return key
                }
            }
        }
        return null
    }

    fun removeClientConnectedItem(key: String) {
        synchronized(clientConnectedNamesLock) {
            clientConnectedNames.remove(key)
        }
    }

    fun removeClientConnectedItem(ws: WebSocket) {
        val key = getClientConnectedNameFromWs(ws)
        synchronized(clientConnectedNamesLock) {
            if (key != null) {
                clientConnectedNames.remove(key)
            }
        }
    }

    // server message ID
    fun getServerMessageWithId(clientName: String, id: Long): String? {
        synchronized(lastServerMessagesWithIdLock) {
            val serverMessageHistory = lastServerMessagesWithId[clientName] ?: return null
            return serverMessageHistory[id]
        }
    }

    fun setServerMessageWithId(clientName: String, id: Long, message: String) {
        synchronized(lastServerMessagesWithIdLock) {
            if (!lastServerMessagesWithId.containsKey(clientName)) {
                lastServerMessagesWithId[clientName] = mutableMapOf()
            }
            lastServerMessagesWithId[clientName]!![id] = message
        }
    }

    fun removeServerMessageIfToBig(clientName: String) {
        synchronized(lastServerMessagesWithIdLock) {
            val serverMessageHistory = lastServerMessagesWithId[clientName] ?: return
            if (serverMessageHistory.size > GlobalVariables.messageHistorySize) {
                serverMessageHistory.remove(serverMessageHistory.keys.first())
            }
        }
    }

    fun getNextServerMessageId(clientName: String): Long {
        synchronized(serverMessageIdCounterLock) {
            if (!serverMessageIdCounter.containsKey(clientName)) {
                serverMessageIdCounter[clientName] = 0
            }
            return serverMessageIdCounter[clientName]!! + 1
        }
    }

    fun incrementServerMessageId(clientName: String): Long {
        synchronized(serverMessageIdCounterLock) {
            if (!serverMessageIdCounter.containsKey(clientName)) {
                serverMessageIdCounter[clientName] = 0
            }
            serverMessageIdCounter[clientName] = serverMessageIdCounter[clientName]!! + 1
            return serverMessageIdCounter[clientName]!!
        }
    }

    fun resetServerMessageId(clientName: String) {
        synchronized(serverMessageIdCounterLock) {
            serverMessageIdCounter[clientName] = 0
        }
    }
    fun removeServerMessageId(clientName: String) {
        synchronized(serverMessageIdCounterLock) {
            serverMessageIdCounter.remove(clientName)
        }
    }

    // client message ID
    fun getNextClientMessageId(clientName: String): Long {
        synchronized(clientMessageIdLock) {
            if (!clientMessageId.containsKey(clientName)) {
                clientMessageId[clientName] = 0
            }
            return clientMessageId[clientName]!! + 1
        }
    }

    fun setNextClientMessageId(clientName: String, id: Long) {
        synchronized(clientMessageIdLock) {
            if (!clientMessageId.containsKey(clientName)) {
                clientMessageId[clientName] = 0
            }
            clientMessageId[clientName] = id
        }
    }

    fun incrementClientMessageId(clientName: String): Long {
        synchronized(clientMessageIdLock) {
            if (!clientMessageId.containsKey(clientName)) {
                clientMessageId[clientName] = 0
            }
            clientMessageId[clientName] = clientMessageId[clientName]!! + 1
            return clientMessageId[clientName]!!
        }
    }

    fun resetClientMessageId(clientName: String) {
        synchronized(clientMessageIdLock) {
            clientMessageId[clientName] = 0
        }
    }
    fun removeClientMessageId(clientName: String) {
        synchronized(clientMessageIdLock) {
            clientMessageId.remove(clientName)
        }
    }

    fun setClientUpdateStatus(ws: WebSocket, status: Int) {
        val clientName = getClientConnectedNameFromWs(ws) ?: return
        synchronized(clientUpdateDoneNamesLock) {
            clientUpdateDoneNames[clientName] = status
        }
    }

    fun setWaitingForClient(clientName: String, message: String?) {
        synchronized(waitingForClientListLock) {
            waitingForClientList[clientName] = message
        }
    }

    fun getWaitingForClient(clientName: String): String? {
        synchronized(waitingForClientListLock) {
            return waitingForClientList[clientName]
        }
    }

    fun removeWaitingForClient(clientName: String) {
        synchronized(waitingForClientListLock) {
            waitingForClientList.remove(clientName)
        }
    }

    fun waitForClient(clientName: String, setValue: String? = null) {
        val waitingStartTime = System.currentTimeMillis()
        while (true) {
            synchronized(waitingForClientListLock) {
                if (waitingForClientList[clientName] == null) {
                    waitingForClientList[clientName] = setValue
                    return
                }
                if (System.currentTimeMillis() - waitingStartTime > GlobalVariables.pingPongDelayTime) {
                    val ws = getClientConnectedItem(clientName) ?: return
                    sendMessage(
                        ws = ws,
                        message = waitingForClientList[clientName]!!,
                        increate_message_id = false
                    )
                }
            }
            try {
                Thread.sleep(1)
            } catch (e: InterruptedException) {
                break
            }
        }
    }

}





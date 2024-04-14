package connection

import GlobalVariables
import badclient.BadClientHandler
import filedata.ApplicationData
import filedata.SoftwareUpdate
import messages.WebsocketMessageClient
import messages.WebsocketMessageServer
import messages.base.MessageBase
import messages.base.client.MessageClientRequestMessage
import messages.base.server.MessageServerRequestMessage
import messages.base.server.MessageServerUpdate
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

    override fun onClose(p0: WebSocket?, p1: Int, p2: String?, p3: Boolean) {
        println("Server: Client disconnected")
        if (p0 == null) {
            return
        }
        synchronized(websocketClientsLock) {
            websocketClients.remove(p0)
        }
        removeClientRegisterItem(p0)
        removeClientConnectedItem(p0)
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
        if (messageBase.name.isEmpty()){
            p0.send(MessageBase("", GlobalVariables.computerName, 0L).toJson())
            return
        }

        // update last client message time
        updateLastClientMessageTime(messageBase.name)

        // decrypt message
        val message = synchronized(keyMapLock) {
            val connectionKeyPair = if (!keyMap.containsKey(messageBase.name)) {
                var connectionKeyPair = ConnectionKeyPair.loadFile(messageBase.name)
                if (connectionKeyPair == null) {
                    BadClientHandler.handleBadClient(p0)
                    return
                }
                connectionKeyPair = ConnectionKeyPair.loadFile(messageBase.name)

                if (connectionKeyPair == null) {
                    BadClientHandler.handleBadClient(p0)
                    return
                }
                keyMap[messageBase.name] = connectionKeyPair
                connectionKeyPair
            } else {
                keyMap[messageBase.name]!!
            }

            connectionKeyPair.decrypt(messageBase.msg)
        }
        val messageClass = WebsocketMessageClient.fromJson(message)
        // if a message with a specific ID is requested
        if (messageClass.type == MessageClientRequestMessage.TYPE) {
            val messageRequestId = messageClass.data.toLong()
            val messageToSend = getServerMessageWithId(clientName = messageBase.name, id=messageRequestId)
            if (messageToSend == null) {
                println("Server: ${messageBase.name} - No message with ID $messageRequestId")
                p0.close()
                return
            }
            sendMessageHistory(p0, messageRequestId)
            return
        }
        // if the message does not have the correct message ID
        val nextClientMessageId = getNextClientMessageId(messageBase.name)
        if (nextClientMessageId != messageBase.messageId) {
            sendMessage(p0,
                WebsocketMessageServer(
                    type = MessageServerRequestMessage.TYPE,
                    data = nextClientMessageId.toString(),
                    sendFrom = GlobalVariables.computerName
                ).toJson(), increate_message_id = false
            )
            //setNextClientMessageId(messageBase.name, nextClientMessageId)
            println("Server: ${messageBase.name} - Repeat sending message with ID $nextClientMessageId")
            return
        }
        incrementClientMessageId(messageBase.name)
        websocketClientMessageHandler.handle(this, p0, messageClass)
    }

    override fun onError(p0: WebSocket?, p1: Exception?) {
        println("Server: Error")

        if (p1 != null) {
            p1.printStackTrace()
        }
        if (p0 == null) {
            return
        }
        removeClientRegisterItem(p0)
        removeClientConnectedItem(p0)
        p0.close()
    }

    override fun onStart() {
        startPingPong()
    }


    fun sendMessageHistory(ws: WebSocket, messageId: Long) {
        val clientName = synchronized(clientConnectedNamesLock){
            var clientName: String? = null
            clientConnectedNames.forEach {
                if (it.value == ws){
                    clientName = it.key
                    return@forEach
                }
            }
            if (clientName == null){
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

                    val message = synchronized(keyMapLock){
                        if (!keyMap.containsKey(clientName)){
                            return
                        }
                        keyMap[clientName]!!.encrypt(messageToSend)
                    }
                    ws.send(MessageBase(
                        name = GlobalVariables.computerName,
                        msg = message,
                        messageId = serverMessageIdCounter[clientName]!!).toJson())
                    serverMessageIdCounter[clientName] = serverMessageIdCounter[clientName]!! + 1
                }
            }
        }
    }

    fun sendMessage(ws: WebSocket, message: String, increate_message_id: Boolean = true) {
        val clientName = synchronized(clientConnectedNamesLock){
            var clientName: String? = null
            clientConnectedNames.forEach {
                if (it.value == ws){
                    clientName = it.key
                    return@forEach
                }
            }
            if (clientName == null){
                return
            }
            clientName!!
        }

        removeServerMessageIfToBig(clientName)
        synchronized(serverMessageIdCounterLock) {
            synchronized(lastServerMessagesWithIdLock) {
                val messageToSend =synchronized(keyMapLock){
                    if (!keyMap.containsKey(clientName)){
                        return
                    }
                    keyMap[clientName]!!.encrypt(message)
                }
                if (!serverMessageIdCounter.containsKey(clientName)) {
                    serverMessageIdCounter[clientName] = 0
                }
                if(increate_message_id){
                    serverMessageIdCounter[clientName] = serverMessageIdCounter[clientName]!! + 1
                }
                val serverMessageId = serverMessageIdCounter[clientName]!!

                if (!lastServerMessagesWithId.containsKey(clientName)) {
                    lastServerMessagesWithId[clientName] = mutableMapOf()
                }
                lastServerMessagesWithId[clientName]!![serverMessageId] = message
                ws.send(MessageBase(
                    name = GlobalVariables.computerName,
                    msg = messageToSend,
                    messageId = serverMessageId).toJson())
            }
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
                        var fileBytes = softwareUpdate.readFilePart()
                        while (fileBytes != null) {
                            val message = WebsocketMessageServer(
                                type = MessageServerUpdate.TYPE,
                                sendFrom = GlobalVariables.computerName,
                                data = softwareUpdate.toMessageJson()
                            ).toJson()
                            for (clientName in clientNames) {
                                val ws = getClientRegisterItem(clientName) ?: continue
                                sendMessage(ws, message)
                            }
                            fileBytes = softwareUpdate.readFilePart()
                        }
                        try {
                            Thread.sleep(1000)
                        } catch (e: InterruptedException) {
                            break
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


}





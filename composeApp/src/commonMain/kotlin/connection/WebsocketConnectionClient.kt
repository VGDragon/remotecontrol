package connection


import GlobalVariables
import filedata.ApplicationData
import filedata.SoftwareUpdate
import filedata.UpdateStatus
import interfaces.TaskInterface
import messages.*
import messages.base.MessageBase
import messages.base.MessageServerResponseCode
import messages.base.ServerAnswerStatus
import messages.base.client.MessageClientRegister
import messages.base.client.MessageClientRequestMessage
import messages.base.server.MessageServerRequestMessage
import messages.base.server.MessageServerUpdate
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import rest.message.RestMessageKeyExchange
import java.net.URI


class WebsocketConnectionClient : WebSocketClient {
    val applicationData: ApplicationData
    var websocketClientMessageHandler: WebsocketClientMessageHandler
    var connectionKeyPair: ConnectionKeyPair? = null
    val connectionKeyPairLock = Object()
    val executeTask: Boolean
    var isConnected: Boolean
    val isConnectedLock = Object()
    var isRegistered: Boolean = false
    val isRegisteredLock = Object()
    var isConnectionError: Boolean = false
    val isConnectionErrorLock = Object()
    var onlyPrepareConnectionKeyPair = false
    var lastServerMessageRecivedTime: Long = 0L
    val lastServerMessageRecivedLock = Object()
    val pingPongDelayTime = GlobalVariables.pingPongDelayTime
    var pingPongThread: Thread? = null
    val pingPongThreadLock = Object()

    // a list of clients that are connected to the server
    val execClientList: MutableList<String> = mutableListOf()
    val execClientListLock = Object()

    // a list of tasks that are running on the server
    val taskList = mutableListOf<TaskInterface>()
    val taskListLock = Object()

    val serverInfoList = mutableListOf<MessageServerResponseCode>()
    var computerName: String

    var lastClinetMessagesWithId: MutableMap<Long, String> = mutableMapOf()
    var lastClientMessagesWithIdLock = Object()
    var clientMessageIdCounter = 0L
    var clientMessageIdCounterLock = Object()

    var serverMessageIdCounter = 0L
    var serverMessageIdCounterLock = Object()

    var updateData: MutableMap<Long, MessageServerUpdate> = mutableMapOf()
    var updatePackageNrs: Long = 0L
    var softwareUpdate: SoftwareUpdate? = null
    var updateDataLock = Object()
    var softwareUpdateDataList = SoftwareUpdate.fromFile()
    val softwareUpdateDataListLock = Object()


    constructor(applicationData: ApplicationData, executeTask: Boolean = false) :
            super(URI("ws://${applicationData.address}:${applicationData.port}")) {
        this.applicationData = applicationData
        this.websocketClientMessageHandler = WebsocketClientMessageHandler(applicationData)
        this.executeTask = executeTask
        this.isConnected = false
        this.computerName = GlobalVariables.computerName
        if (this.executeTask) {
            computerName += "_executable"
        }
    }

    fun connectAndRegister(doJoin: Boolean = true) {
        this.connect()
        waitForConnection()
        if (getIsConnectionError()) {
            println("Client: Connection error")
            return
        }
        setConnectionKeyPair()
        this.sendMessage(
            WebsocketMessageClient(
                type = MessageClientRegister.TYPE,
                apiKey = applicationData.apiKey,
                sendFrom = "",
                sendTo = "",
                data = MessageClientRegister(
                    clientName = computerName,
                    isExecutable = executeTask
                )
                    .toJson()
            )
                .toJson(),
            increate_message_id = false
        )
        while (!isRegistered) {
            Thread.sleep(100)
            if (getServerInfoSize() > 0) {
                val serverInfo = getServerInfo(0)
                if (serverInfo.status == ServerAnswerStatus.OK) {
                    setIsRegistered(true)
                    println("Client: Registered")
                } else {
                    println("Client: Server response: ${serverInfo.status} ${serverInfo.message}")
                    setIsConnected(false)
                }
            }
        }
        pingPong()
        if (doJoin) {
            while (isConnected) {
                Thread.sleep(100)
            }
            this.close()
        }
    }

    fun prepareConnection() {
        this.connect()
        waitForConnection()
        if (getIsConnectionError()) {
            println("Client: Connection error")
            return
        }
        onlyPrepareConnectionKeyPair = true
        setConnectionKeyPair()
    }

    fun waitForResponse(): MessageServerResponseCode {
        while (getServerInfoSize() == 0) {
            Thread.sleep(100)
        }
        return getServerInfo(0)
    }

    fun waitForConnection() {
        while (!getIsConnected() && !getIsConnectionError()) {
            Thread.sleep(100)
        }

    }

    fun stopConnection() {
        this.close()
    }

    fun setConnectionKeyPair() {
        val message = MessageBase("", "", incrementClientMessageIdCounter())
        val messageString = message.toJson()
        setClientMessagesWithId(message.messageId, messageString)

        this.send(messageString)
        while (connectionKeyPair == null) {
            Thread.sleep(100)
        }
    }
    fun getConnectionKeyPair(serverName: String) {
        synchronized(connectionKeyPairLock) {
            val connectionKeyPair = ConnectionKeyPair.loadFile(serverName)
            if (connectionKeyPair != null) {
                this.connectionKeyPair = connectionKeyPair
                return
            }
        }
        generateKeyPair(serverName)
    }


    fun sendMessageHistory(messageId: Long) {
        synchronized(lastClientMessagesWithIdLock) {
            synchronized(clientMessageIdCounterLock) {
                clientMessageIdCounter = messageId
                while (true){
                    val message = lastClinetMessagesWithId[messageId]
                    if (message == null){
                        clientMessageIdCounter--
                        break
                    }
                    val messageToSend = synchronized(connectionKeyPairLock) {
                        if (connectionKeyPair == null) {
                            return
                        }
                        connectionKeyPair!!.encrypt(message)
                    }
                    this.send(
                        MessageBase(
                            name = computerName,
                            msg = messageToSend,
                            messageId = clientMessageIdCounter
                        ).toJson()
                    )
                    clientMessageIdCounter++
                }
            }
        }
    }

    fun sendMessage(message: String, increate_message_id: Boolean = true) {
        removeClientMessageIfToBig()
        synchronized(lastClientMessagesWithIdLock) {
            synchronized(clientMessageIdCounterLock) {
                val messageToSend = synchronized(connectionKeyPairLock) {
                    if (connectionKeyPair == null) {
                        return
                    }
                    connectionKeyPair!!.encrypt(message)
                }
                if (increate_message_id) {
                    clientMessageIdCounter++
                }
                val clientMessageId = clientMessageIdCounter

                lastClinetMessagesWithId[clientMessageId] = message
                // TODO: testing if message is not send
                if (clientMessageId != 20L) {
                    this.send(
                        MessageBase(
                            name = computerName,
                            msg = messageToSend,
                            messageId = clientMessageId
                        ).toJson()
                    )
                }
            }
        }
    }

    override fun onOpen(p0: ServerHandshake?) {
        println("Client: Opened connection")
        setIsConnected(true)
    }

    override fun onMessage(p0: String?) {
        if (p0 == null) {
            return
        }
        setLastServerMessageTime(System.currentTimeMillis())

        // if the message is a key exchange message
        val messageBase = MessageBase.fromJson(p0)
        if (messageBase.name.isEmpty()) {
            if (messageBase.msg.isEmpty()) {
                return
            }
            getConnectionKeyPair(messageBase.msg)
            return
        }

        // decrypt the message
        val message = synchronized(connectionKeyPairLock) {
            if (connectionKeyPair == null) {
                connectionKeyPair = ConnectionKeyPair.loadFile(messageBase.name)
            }
            if (connectionKeyPair == null) {
                generateKeyPair(messageBase.name)
            }
            if (connectionKeyPair == null) {
                setIsConnectionError(true)
                setIsConnected(false)
                return
            }
            connectionKeyPair!!.decrypt(messageBase.msg)
        }

        // if a message with a specific ID is requested
        val messageClass = WebsocketMessageServer.fromJson(message)
        if (messageClass.type == "pong") {

            return
        }
        if (messageClass.type == MessageServerRequestMessage.TYPE) {
            val messageRequestId = messageClass.data.toLong()
            sendMessageHistory(messageRequestId)
            return
        }

        // if the message does not have the correct message ID
        val nextServerMessageId = getNextServerMessageIdCounter()
        if (nextServerMessageId != messageBase.messageId) {
            sendMessage(WebsocketMessageClient(
                type = MessageClientRequestMessage.TYPE,
                apiKey = applicationData.apiKey,
                sendFrom = computerName,
                sendTo = "",
                data = nextServerMessageId.toString()
            ).toJson(), increate_message_id = false)
            return
        }
        incrementServerMessageIdCounter()

        websocketClientMessageHandler.handle(this, messageClass)
    }

    override fun onClose(p0: Int, p1: String?, p2: Boolean) {
        println("Client: Closed connection")
    }

    override fun onError(p0: Exception?) {
        println("Client: Error")
        if (p0 != null) {
            p0.printStackTrace()
        }
        if (!getIsConnected()) {
            setIsConnectionError(true)
        }
    }

    // keyPair
    fun generateKeyPair(name: String) {
        val connectionKeyPair = ConnectionKeyPair(name, name).generateKeyPair()
        val restMessageKeyExchange = RestMessageKeyExchange(
            keyOwner = computerName,
            keyAlias = computerName,
            keyCryptoMethode = connectionKeyPair.keyCryptoMethode,
            keyCryptoMethodeInstance = connectionKeyPair.keyCryptoMethodeInstance,
            privateKey = connectionKeyPair.ownPrivateKey
        )
        val urlString = "http://${applicationData.address}:${applicationData.port + 1}/key-exchange"
        try {
            val client = OkHttpClient()
            val body: RequestBody = RequestBody.create(
                "application/json".toMediaType(), restMessageKeyExchange.toJson())
            val request = Request.Builder()
                .url(urlString)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val answer = response.body!!.string()
            val serverRestMessageKeyExchange = RestMessageKeyExchange.fromJson(answer)
            connectionKeyPair.privateKeyTarget = serverRestMessageKeyExchange.privateKey
            if(onlyPrepareConnectionKeyPair){
                connectionKeyPair.saveKeyPair(fileEnding = "." + this.computerName.substring(0, this.computerName.length - "_executable".length))
                this.connectionKeyPair = connectionKeyPair
                this.close()
            } else {
                connectionKeyPair.saveKeyPair()
                this.connectionKeyPair = connectionKeyPair
            }
        } catch (e: InterruptedException){
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

    }

    fun pingPong() {
        synchronized(pingPongThreadLock) {
            if (pingPongThread != null) {
                pingPongThread!!.interrupt()
            }
            pingPongThread = Thread {
                while (isConnected) {
                    try {
                        Thread.sleep(pingPongDelayTime)
                        if (!isConnected) {
                            return@Thread
                        }
                        val time = System.currentTimeMillis()
                        if (time - getLastServerMessageTime() < pingPongDelayTime * 4) {
                            sendMessage(WebsocketMessageClient(
                                type = "ping",
                                apiKey = applicationData.apiKey,
                                sendFrom = computerName,
                                sendTo = "",
                                data = ""
                            ).toJson())
                            continue
                        }
                    } catch (e: InterruptedException) {
                        return@Thread
                    }
                    setIsConnected(false)
                    this.close()
                    println("Client: Ping Pong timeout")
                }
            }
            setLastServerMessageTime(System.currentTimeMillis())
            pingPongThread!!.start()
        }
    }


    // getters and setters
    fun getLastServerMessageTime(): Long {
        synchronized(lastServerMessageRecivedLock) {
            return lastServerMessageRecivedTime
        }
    }
    fun setLastServerMessageTime(value: Long) {
        synchronized(lastServerMessageRecivedLock) {
            lastServerMessageRecivedTime = value
        }
    }

    fun getIsConnected(): Boolean {
        synchronized(isConnectedLock) {
            return isConnected
        }
    }

    fun setIsConnected(value: Boolean) {
        synchronized(isConnectedLock) {
            isConnected = value
        }
    }

    fun getIsConnectionError(): Boolean {
        synchronized(isConnectionErrorLock) {
            return isConnectionError
        }
    }

    fun setIsConnectionError(value: Boolean) {
        synchronized(isConnectionErrorLock) {
            isConnectionError = value
        }
    }

    fun getIsRegistered(): Boolean {
        synchronized(isRegisteredLock) {
            return isRegistered
        }
    }

    fun setIsRegistered(value: Boolean) {
        synchronized(isRegisteredLock) {
            isRegistered = value
        }
    }

    fun addTask(task: TaskInterface) {
        synchronized(taskListLock) {
            taskList.add(task)
        }
    }

    fun removeTask(task: TaskInterface) {
        synchronized(taskListLock) {
            taskList.remove(task)
        }
    }

    fun removeTask(index: Int) {
        synchronized(taskListLock) {
            taskList.removeAt(index)
        }
    }

    fun getTask(index: Int): TaskInterface {
        synchronized(taskListLock) {
            return taskList[index]
        }
    }

    fun getTaskSize(): Int {
        synchronized(taskListLock) {
            return taskList.size
        }
    }

    fun addServerInfo(serverInfo: MessageServerResponseCode) {
        synchronized(serverInfoList) {
            serverInfoList.add(serverInfo)
        }
    }

    fun removeServerInfo(serverInfo: MessageServerResponseCode) {
        synchronized(serverInfoList) {
            serverInfoList.remove(serverInfo)
        }
    }

    fun removeServerInfo(index: Int) {
        synchronized(serverInfoList) {
            serverInfoList.removeAt(index)
        }
    }

    fun getServerInfo(index: Int): MessageServerResponseCode {
        synchronized(serverInfoList) {
            return serverInfoList.removeAt(index)
        }
    }

    fun getServerInfoSize(): Int {
        synchronized(serverInfoList) {
            return serverInfoList.size
        }
    }

    fun addToExecClientList(clientName: String) {
        synchronized(execClientListLock) {
            execClientList.add(clientName)
        }
    }

    fun removeFromExecClientList(clientName: String) {
        synchronized(execClientListLock) {
            execClientList.remove(clientName)
        }
    }

    fun getFromExecClientList(index: Int): String {
        synchronized(execClientListLock) {
            return execClientList[index]
        }
    }

    fun getExecClientListVariable(): List<String> {
        synchronized(execClientListLock) {
            return execClientList.toList()
        }
    }

    fun setExecClientListVariable(value: List<String>) {
        synchronized(execClientListLock) {
            execClientList.clear()
            execClientList.addAll(value)
        }
    }
    // client message ID
    fun getClientMessageWithId(id: Long): String? {
        synchronized(lastClientMessagesWithIdLock) {
            return lastClinetMessagesWithId[id]
        }
    }
    fun setClientMessagesWithId(id: Long, message: String) {
        synchronized(lastClientMessagesWithIdLock) {
            lastClinetMessagesWithId[id] = message
        }
    }
    fun removeClientMessageIfToBig() {
        synchronized(lastClientMessagesWithIdLock) {
            if (lastClinetMessagesWithId.size > GlobalVariables.messageHistorySize) {
                lastClinetMessagesWithId.remove(lastClinetMessagesWithId.keys.first())
            }
        }
    }
    fun getNextClientMessageIdCounter(): Long {
        synchronized(clientMessageIdCounterLock) {
            return clientMessageIdCounter + 1
        }
    }
    fun incrementClientMessageIdCounter(): Long {
        synchronized(clientMessageIdCounterLock) {
            clientMessageIdCounter++
            return clientMessageIdCounter
        }
    }
    fun resetClientMessageIdCounter() {
        synchronized(clientMessageIdCounterLock) {
            clientMessageIdCounter = 0
        }
    }
    // server message ID
    fun getNextServerMessageIdCounter(): Long {
        synchronized(serverMessageIdCounterLock) {
            return serverMessageIdCounter + 1
        }
    }
    fun incrementServerMessageIdCounter(): Long {
        synchronized(serverMessageIdCounterLock) {
            serverMessageIdCounter++
            return serverMessageIdCounter
        }
    }
    fun resetServerMessageIdCounter() {
        synchronized(serverMessageIdCounterLock) {
            serverMessageIdCounter = 0
        }
    }
    fun doSoftwareUpdateDataExist(version: String): Boolean {
        synchronized(softwareUpdateDataListLock) {
            val softwareUpdateData = softwareUpdateDataList.filter { it.version.equals(version) }
            if (!softwareUpdateData.isEmpty()){
                if (softwareUpdateData[0].updateStatus == UpdateStatus.FINISHED){
                    return true
                }
            }
        }
        return false
    }
}
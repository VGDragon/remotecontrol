package connection


import GlobalVariables
import filedata.ApplicationData
import filedata.SoftwareUpdate
import interfaces.TaskInterface
import messages.*
import messages.base.MessageBase
import messages.base.MessageReceived
import messages.base.MessageServerResponseCode
import messages.base.ServerAnswerStatus
import messages.base.client.MessageClientRegister
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import rest.message.RestMessageKeyExchange
import java.net.URI
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicLong


class WebsocketConnectionClient : WebSocketClient {
    val applicationData: ApplicationData
    var websocketClientMessageHandler: WebsocketClientMessageHandler
    var connectionKeyPair: ConnectionKeyPair? = null
    val executeTask: Boolean
    var isConnected = false
    var isRegistered: Boolean = false
    var isConnectionError: Boolean = false
    var onlyPrepareConnectionKeyPair = false
    var lastServerMessageReceivedTime = AtomicLong(0L)
    var serverName = ""

    // a list of clients that are connected to the server
    val execClientList: MutableList<String> = mutableListOf()

    // a list of tasks that are running on the server
    val taskList = mutableListOf<TaskInterface>()
    val taskListLock = Object()

    val serverInfoList = mutableListOf<MessageServerResponseCode>()
    var computerName: String

    var lastClinetMessagesWithId: MutableMap<Long, String> = mutableMapOf()
    var clientMessageIdCounter = 0L

    var serverMessageIdCounter = 0L

    var updatePackageNrs: Long = 0L
    var softwareUpdate: SoftwareUpdate? = null
    var softwareUpdateDataList = SoftwareUpdate.fromFile()

    var waitingForServerMessage: String? = null

    ///
    val messageReceivedQueue = LinkedList<String>()
    val messageSendQueue = LinkedList<String>()
    var handleMessageThread: Thread
    var sendMessageThread: Thread = Thread {
        while (true) {
            if (messageSendQueue.size == 0) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    close()
                    return@Thread
                }
                continue
            }
            val message = messageSendQueue.remove()
            this.send(message)
        }
    }


    constructor(applicationData: ApplicationData, executeTask: Boolean = false) :
            super(URI("ws://${applicationData.address}:${applicationData.port}")) {
        this.applicationData = applicationData
        this.websocketClientMessageHandler = WebsocketClientMessageHandler(applicationData)
        this.executeTask = executeTask
        this.computerName = GlobalVariables.computerName
        if (this.executeTask) {
            computerName += "_executable"
        }
        this.handleMessageThread = Thread {
            while (true) {
                if (messageReceivedQueue.size > 0) {

                    val message = messageReceivedQueue.remove()
                    handleMessage(message)
                    continue
                }

                val time = System.currentTimeMillis()

                val lastServerMessageReceivedTime = lastServerMessageReceivedTime.get()
                if (time - lastServerMessageReceivedTime > GlobalVariables.pingPongDelayTimeMax) {
                    break
                } else if (time - lastServerMessageReceivedTime > GlobalVariables.pingPongDelayTime) {
                    val pingPongMessage =
                        WebsocketMessageClient(
                            type = "ping",
                            apiKey = applicationData.apiKey,
                            sendFrom = computerName,
                            sendTo = "",
                            data = ""
                        ).toJson()
                    sendMessage(pingPongMessage)
                }
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    close()
                    return@Thread
                }
            }
            isConnected = false
            this.close()
            println("Client: Ping Pong timeout")
        }
    }

    fun connectAndRegister(doJoin: Boolean = true) {
        println("Client: Connect to uri: ${uri}")
        this.connect()
        lastServerMessageReceivedTime.set(System.currentTimeMillis())

        while (!isConnected) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                return
            }
            if (isConnectionError) {
                println("Client: Connection error")
                return
            }
        }
        if (isConnectionError) {
            println("Client: Connection error")
            return
        }
        startThreads()

        requestConnectionKeyPair()
        this.sendMessage(
            WebsocketMessageClient(
                type = MessageClientRegister.TYPE,
                apiKey = applicationData.apiKey,
                sendFrom = computerName,
                sendTo = "",
                data = MessageClientRegister(
                    clientName = computerName,
                    isExecutable = executeTask
                )
                    .toJson()
            )
                .toJson(),
            increaseMessageId = false
        )
        while (!isRegistered) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                close()
                return
            }
            if (isConnectionError) {
                println("Client: Connection error")
                return
            }
        }
        if (doJoin) {
            while (isConnected) {
                try {
                    Thread.sleep(100)
                } catch (e: InterruptedException) {
                    break
                }
            }
            this.close()
        }
    }

    fun prepareConnection() {
        this.connect()

        while (!isConnected) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                close()
                return
            }
            if (isConnectionError) {
                println("Client: Connection error")
                return
            }
        }
        if (isConnectionError) {
            println("Client: Connection error")
            return
        }
        onlyPrepareConnectionKeyPair = true
        requestConnectionKeyPair()
    }

    fun waitForResponse(): MessageServerResponseCode {
        while (serverInfoList.size == 0) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                close()
                return MessageServerResponseCode(ServerAnswerStatus.ERROR, "")
            }
        }
        return serverInfoList.removeAt(0)
    }

    fun stopConnection() {
        stopThreads()
        this.close()
    }

    fun requestConnectionKeyPair() {
        clientMessageIdCounter++
        val message = MessageBase("", "", clientMessageIdCounter)
        val messageString = message.toJson()

        this.send(messageString)
        while (connectionKeyPair == null) {
            Thread.sleep(100)
        }
    }

    fun loadConnectionKeyPair() {
        if (connectionKeyPair != null) {
            return
        }
        if (serverName.isEmpty()) {
            return
        }
        connectionKeyPair = ConnectionKeyPair.loadFile(serverName)
        if (connectionKeyPair != null) {
            return
        }
        generateKeyPair(serverName)
        if (connectionKeyPair == null) {
            isConnectionError = true
            isConnected = false
        }
    }

    fun sendMessage(message: String, increaseMessageId: Boolean = true) {
        if (connectionKeyPair == null) {
            return
        }
        waitingForServerMessage = message
        val messageToSend = connectionKeyPair!!.encrypt(message)
        if (increaseMessageId) {
            clientMessageIdCounter++
        }
        val clientMessageId = clientMessageIdCounter

        lastClinetMessagesWithId[clientMessageId] = message

        messageSendQueue.add(
            MessageBase(
                name = computerName,
                msg = messageToSend,
                messageId = clientMessageId
            ).toJson()
        )
    }

    override fun onOpen(p0: ServerHandshake?) {
        println("Client: Opened connection")
        isConnected = true
    }

    override fun onMessage(p0: String?) {
        if (p0 == null) {
            return
        }
        lastServerMessageReceivedTime.set(System.currentTimeMillis())
        messageReceivedQueue.add(p0)
    }

    override fun onClose(p0: Int, p1: String?, p2: Boolean) {
        println("Client: Closed connection")
        stopThreads()
    }

    override fun onError(p0: Exception?) {
        println("Client: Error")
        if (p0 != null) {
            p0.printStackTrace()
        }
        if (!isConnected) {
            isConnectionError = true
        }
    }

    fun handleMessage(serverMessage: String) {

        // if the message is a key exchange message
        val messageBase = MessageBase.fromJson(serverMessage)
        if (messageBase.name.isEmpty()) {
            if (messageBase.msg.isEmpty()) {
                return
            }
            serverName = messageBase.msg
            loadConnectionKeyPair()
            return
        }
        // decrypt the message
        val message = decryptMessage(messageBase.msg) ?: return
        val messageClass = WebsocketMessageServer.fromJson(message)
        if (messageClass.type == MessageReceived.TYPE) {
            waitingForServerMessage = null
            return
        }

        val nextServerMessageId = serverMessageIdCounter + 1
        if (nextServerMessageId != messageBase.messageId) {
            return
        }
        sendMessage(
            message = WebsocketMessageClient(
                type = MessageReceived.TYPE,
                apiKey = applicationData.apiKey,
                sendFrom = computerName,
                sendTo = messageClass.sendFrom,
                data = messageBase.messageId.toString()
            ).toJson(),
            increaseMessageId = false
        )
        serverMessageIdCounter++
        websocketClientMessageHandler.handle(this, messageClass)

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
                "application/json".toMediaType(), restMessageKeyExchange.toJson()
            )
            val request = Request.Builder()
                .url(urlString)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val answer = response.body!!.string()
            val serverRestMessageKeyExchange = RestMessageKeyExchange.fromJson(answer)
            connectionKeyPair.privateKeyTarget = serverRestMessageKeyExchange.privateKey
            if (onlyPrepareConnectionKeyPair) {
                connectionKeyPair.saveKeyPair(
                    fileEnding = "." + this.computerName.substring(
                        0,
                        this.computerName.length - "_executable".length
                    )
                )
                this.connectionKeyPair = connectionKeyPair
                this.close()
            } else {
                connectionKeyPair.saveKeyPair()
                this.connectionKeyPair = connectionKeyPair
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

    }

    fun decryptMessage(message: String): String? {
        if (message.isEmpty()) {
            return ""
        }
        loadConnectionKeyPair()
        if (connectionKeyPair == null) {
            return null
        }
        return try {
            connectionKeyPair!!.decrypt(message)
        } catch (e: Exception) {
            null
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

    // getters and setters

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

}
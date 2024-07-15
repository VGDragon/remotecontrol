package connection.client


import GlobalVariables
import connection.ConnectionKeyPair
import filedata.ApplicationData
import filedata.SoftwareUpdate
import interfaces.TaskInterface
import io.ktor.client.*
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
import rest.message.RestMessageKeyExchange
import java.util.LinkedList
import java.util.concurrent.atomic.AtomicLong

import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.runBlocking

class WebsocketConnectionClient {
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

    // connection
    val client = HttpClient{
        install(WebSockets)
    }
    val connectionThread: Thread



    constructor(applicationData: ApplicationData, executeTask: Boolean = false) {
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
                    println("Client: Received message: $message")
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
                    stopConnection()
                    return@Thread
                }
            }
            isConnected = false
            stopConnection()
            println("Client: Ping Pong timeout")
        }

        this.connectionThread = Thread {
            runBlocking {
                client.webSocket(method = HttpMethod.Get, host = applicationData.address, port = applicationData.port, path = "/") {
                    lastServerMessageReceivedTime.set(System.currentTimeMillis())
                    isConnected = true
                    var didSomething = false
                    while(true) {
                        if (!incoming.isEmpty) {
                            val frame = incoming.receive()
                            println(frame)
                            val incomingMessage = frame as? Frame.Text ?: continue

                            val message = incomingMessage.readText()
                            messageReceivedQueue.add(message)
                            didSomething = true
                        }
                        if (!messageSendQueue.isEmpty()){
                            val sendMessage = messageSendQueue.remove()
                            send(sendMessage)
                            didSomething = true
                        }
                        if (!didSomething) {
                            try {
                                Thread.sleep(10)
                            } catch (e: InterruptedException) {
                                break
                            }
                        }

                        didSomething = false
                    }
                }
            }
        }
    }


    fun startConnection(){
        connectionThread.start()
    }

    fun connectAndRegister(doJoin: Boolean = true) {
        println("Client: Connect to uri: ${applicationData.address}:${applicationData.port}")
        startConnection()
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
                stopConnection()
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
            stopConnection()
        }
    }

    fun prepareConnection() {
        startConnection()

        while (!isConnected) {
            try {
                Thread.sleep(100)
            } catch (e: InterruptedException) {
                stopConnection()
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
                stopConnection()
                return MessageServerResponseCode(ServerAnswerStatus.ERROR, "")
            }
        }
        return serverInfoList.removeAt(0)
    }

    fun stopConnection() {
        stopThreads()
        client.close()
        connectionThread.interrupt()
    }

    fun requestConnectionKeyPair() {
        clientMessageIdCounter++
        val message = MessageBase("", "", clientMessageIdCounter)
        val messageString = message.toJson()

        this.messageSendQueue.add(messageString)
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
        println("Client: Send message: $message")
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
                stopConnection()
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
    }
    fun stopThreads() {
        handleMessageThread.interrupt()
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
package connection

import filedata.ApplicationData
import interfaces.TaskInterface
import messages.*
import messages.base.MessageBase
import messages.base.client.MessageClientRegister
import messages.base.MessageServerResponseCode
import messages.base.ServerAnswerStatus
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI


import rest.message.RestMessageKeyExchange
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.jvm.Throws

class WebsocketConnectionClient : WebSocketClient {
    val applicationData: ApplicationData
    var websocketClientMessageHandler: WebsocketClientMessageHandler
    var connectionKeyPair: ConnectionKeyPair? = null
    val connectionKeyPairLock = Object()
    var keepWsRunning = true
    var keepWsRunningLock = Object()
    val executeTask: Boolean
    var isConnected: Boolean
    val isConnectedLock = Object()
    var isRegistered: Boolean = false
    val isRegisteredLock = Object()
    var isConnectionError: Boolean = false
    val isConnectionErrorLock = Object()

    // a list of clients that are connected to the server
    val execClientList: MutableList<String> = mutableListOf()
    val execClientListLock = Object()

    // a list of tasks that are running on the server
    val taskList = mutableListOf<TaskInterface>()
    val taskListLock = Object()

    val serverInfoList = mutableListOf<MessageServerResponseCode>()
    var computerName: String

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
                    clientName = computerName
                )
                    .toJson()
            )
                .toJson()
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
        if (doJoin) {
            while (isConnected) {
                Thread.sleep(100)
            }
            this.close()
        }
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
        setKeepRunning(false)
        this.close()
    }

    fun setConnectionKeyPair() {

    }

    fun sendMessage(message: String) {
        val messageToSend = synchronized(connectionKeyPairLock){
            if (connectionKeyPair == null){
                return
            }
            connectionKeyPair!!.encrypt(message)
        }
        this.send(messageToSend)
    }

    override fun onOpen(p0: ServerHandshake?) {
        println("Client: Opened connection")
        setIsConnected(true)
    }

    override fun onMessage(p0: String?) {
        if (p0 == null) {
            return
        }
        val messageBase = MessageBase.fromJson(p0)
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
        val messageClass = WebsocketMessageServer.fromJson(message)
        websocketClientMessageHandler.handle(this, messageClass)
    }

    override fun onClose(p0: Int, p1: String?, p2: Boolean) {
        println("Client: Closed connection")
        Thread.sleep(1000)
        if (getKeepRunning()) {
            connect()
        }
    }

    override fun onError(p0: Exception?) {
        println("Client: Error")
        if (!getIsConnected()) {
            setIsConnectionError(true)
        }
    }

    // keyPair
    fun generateKeyPair(name: String) {
        val connectionKeyPair = ConnectionKeyPair(name, computerName).generateKeyPair()
        val restMessageKeyExchange = RestMessageKeyExchange(
            keyOwner = computerName,
            keyAlias = computerName,
            keyCryptoMethode = connectionKeyPair.keyCryptoMethode,
            publicKey = connectionKeyPair.ownPublicKey
        )
        val urlString = "http://${applicationData.address}:${applicationData.port + 1}/key-exchange"
        try {

            val client = HttpClient.newBuilder().build();
            val request = HttpRequest.newBuilder()
                .uri(URI.create(urlString))
                //.header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(restMessageKeyExchange.toJson()))
                .build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofString())
            val answer = response.body()
            val serverRestMessageKeyExchange = RestMessageKeyExchange.fromJson(answer)
            connectionKeyPair.publicKeyTarget = serverRestMessageKeyExchange.publicKey
            connectionKeyPair.saveKeyPair()
            this.connectionKeyPair = connectionKeyPair
        } catch (e: InterruptedException){
            Thread.currentThread().interrupt()
        } catch (e: Exception) {
            return
        }

    }

    // getters and setters

    fun getKeepRunning(): Boolean {
        synchronized(keepWsRunningLock) {
            return keepWsRunning
        }
    }

    fun setKeepRunning(value: Boolean) {
        synchronized(keepWsRunningLock) {
            keepWsRunning = value
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
}
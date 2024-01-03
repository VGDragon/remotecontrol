package connection

import ApplicationData
import interfaces.TaskInterface
import messages.*
import messages.base.client.MessageClientRegister
import messages.base.MessageServerResponseCode
import messages.base.ServerAnswerStatus
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class WebsocketConnectionClient : WebSocketClient {
    val applicationData: ApplicationData
    var websocketClientMessageHandler: WebsocketClientMessageHandler
    var keepWsRunning = true
    var keepWsRunningLock = Object()
    val executeTask: Boolean
    var isConnected: Boolean
    val isConnectedLock = Object()
    var isRegistered: Boolean = false
    val isRegisteredLock = Object()
    var isConnectionError: Boolean = false
    val isConnectionErrorLock = Object()

    // a list of tasks that are running on the server
    val taskList = mutableListOf<TaskInterface>()
    val taskListLock = Object()

    val serverInfoList = mutableListOf<MessageServerResponseCode>()

    constructor(applicationData: ApplicationData, executeTask: Boolean = false) :
            super(URI("ws://${applicationData.ip}:${applicationData.port}")) {
        this.applicationData = applicationData
        this.websocketClientMessageHandler = WebsocketClientMessageHandler(applicationData)
        this.executeTask = executeTask
        this.isConnected = false
    }

    fun connectAndRegister(){
        this.connect()
        waitForConnection()
        if (getIsConnectionError()) {
            println("Client: Connection error")
            return
        }
        val computerName = System.getenv("COMPUTERNAME")
        this.send(
            WebsocketMessageClient(
            type = MessageClientRegister.TYPE,
            apiKey = applicationData.apiKey,
            data = MessageClientRegister(
                clientName = computerName)
                .toJson())
            .toJson())
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
        while (isConnected) {
            Thread.sleep(100)
        }
        this.close()
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

    override fun onOpen(p0: ServerHandshake?) {
        println("Client: Opened connection")
        setIsConnected(true)
    }

    override fun onMessage(p0: String?) {
        if (p0 == null) {
            return
        }
        val messageClass = WebsocketMessageServer.fromJson(p0)
        websocketClientMessageHandler.handle(this, messageClass)
    }

    override fun onClose(p0: Int, p1: String?, p2: Boolean) {
        println("Client: Closed connection")
        Thread.sleep(1000)
        connect()
    }

    override fun onError(p0: Exception?) {
        println("Client: Error")
        if (!getIsConnected()) {
            setIsConnectionError(true)
        }
    }

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
}
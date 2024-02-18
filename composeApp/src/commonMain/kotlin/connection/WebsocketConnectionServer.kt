package connection

import GlobalVariables
import badclient.BadClientHandler
import filedata.ApplicationData
import messages.WebsocketMessageClient
import messages.base.MessageBase
import org.java_websocket.WebSocket
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.lang.Exception
import java.net.InetSocketAddress


class WebsocketConnectionServer : WebSocketServer {
    val applicationData: ApplicationData
    var websocketClients: MutableList<WebSocket>
    var websocketClientMessageHandler: WebsocketServerMessageHandler
    var keepWsRunning: Boolean = true
    var keepWsRunningLock = Object()
    val clientTaskRunningPermission: MutableMap<String, WebSocket> = mutableMapOf()
    val clientTaskRunningPermissionLock = Object()
    val clientConnectedNames: MutableMap<String, WebSocket> = mutableMapOf()
    val clientConnectedNamesLock = Object()
    val keyMap: MutableMap<String, ConnectionKeyPair> = mutableMapOf()
    val keyMapLock = Object()

    constructor(applicationData: ApplicationData) : super(InetSocketAddress(applicationData.port)) {
        this.applicationData = applicationData
        this.websocketClients = mutableListOf()
        this.websocketClientMessageHandler = WebsocketServerMessageHandler(applicationData)
    }

    override fun onOpen(p0: WebSocket?, p1: ClientHandshake?) {
        if (p0 != null) {
            websocketClients.add(p0)
        }
        println("Server: Client connected")
    }

    override fun onClose(p0: WebSocket?, p1: Int, p2: String?, p3: Boolean) {
        println("Server: Client disconnected")
        if (p0 == null) {
            return
        }
        websocketClients.remove(p0)
    }

    override fun onMessage(p0: WebSocket?, p1: String?) {
        if (p1 == null || p0 == null) {
            return
        }
        synchronized(BadClientHandler.badClientMapLock) {
            if (BadClientHandler.badClientMap.contains(p0)) {
                BadClientHandler.handleBadClient(p0)
                return
            }
        }
        val messageBase = MessageBase.fromJson(p1)
        if (messageBase.name.isEmpty()){
            p0.send(MessageBase("", GlobalVariables.computerName).toJson())
            return
        }
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
        websocketClientMessageHandler.handle(this, p0, messageClass)
    }

    override fun onError(p0: WebSocket?, p1: Exception?) {

    }

    override fun onStart() {
    }

    //
    fun sendMessage(ws: WebSocket, message: String) {
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
            clientName
        }
        val messageToSend =synchronized(keyMapLock){
            if (!keyMap.containsKey(clientName)){
                return
            }
             keyMap[clientName]!!.encrypt(message)
        }
        ws.send(MessageBase(GlobalVariables.computerName, messageToSend).toJson())
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

    fun removeClientConnectedItem(key: String) {
        synchronized(clientConnectedNamesLock) {
            clientConnectedNames.remove(key)
        }
    }

}





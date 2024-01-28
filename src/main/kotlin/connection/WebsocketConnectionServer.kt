package connection

import filedata.ApplicationData
import messages.WebsocketMessageClient
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
        val messageClass = WebsocketMessageClient.fromJson(p1)
        websocketClientMessageHandler.handle(this, p0, messageClass)
    }

    override fun onError(p0: WebSocket?, p1: Exception?) {

    }

    override fun onStart() {
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





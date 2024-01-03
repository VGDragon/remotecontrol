package connection

import ApplicationData
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
    val clientRegisterMap: MutableMap<String, WebSocket> = mutableMapOf()
    val clientRegisterMapLock = Object()
    val clientBridge: MutableMap<WebSocket, WebSocket> = mutableMapOf()
    val clientBridgeLock = Object()

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
        removeClientBridgeItem(p0)
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
    fun getClientBridgeItem(key: WebSocket): WebSocket? {
        synchronized(clientBridgeLock) {
            return clientBridge[key]
        }
    }
    fun setClientBridgeItem(key: WebSocket, value: WebSocket) {
        synchronized(clientBridgeLock) {
            clientBridge[key] = value
        }
    }
    fun connectBride(ws1: WebSocket, ws2: WebSocket) {
        synchronized(clientBridgeLock) {
            if (clientBridge.containsValue(ws1)) {
                clientBridge.remove(clientBridge.filterValues { it == ws1 }.keys.first())
            }
            if (clientBridge.containsValue(ws2)) {
                clientBridge.remove(clientBridge.filterValues { it == ws2 }.keys.first())
            }
            clientBridge[ws1] = ws2
            clientBridge[ws2] = ws1
        }
    }
    fun removeClientBridgeItem(key: WebSocket) {
        synchronized(clientBridgeLock) {
            clientBridge.remove(key)
        }
    }
    fun getClientNames(): List<String> {
        synchronized(clientRegisterMapLock) {
            return clientRegisterMap.keys.toList()
        }
    }
    fun getClientRegisterItem(key: String): WebSocket? {
        synchronized(clientRegisterMapLock) {
            return clientRegisterMap[key]
        }
    }
    fun setClientRegisterItem(key: String, value: WebSocket) {
        synchronized(clientRegisterMapLock) {
            clientRegisterMap[key] = value
        }
    }

    fun getClientRegisterNameFromWs(ws: WebSocket): String? {
        synchronized(clientRegisterMapLock) {
            for ((key, value) in clientRegisterMap) {
                if (value == ws) {
                    return key
                }
            }
        }
        return null
    }

}





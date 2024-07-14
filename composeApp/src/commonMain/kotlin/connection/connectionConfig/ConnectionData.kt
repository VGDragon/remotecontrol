package connection.connectionConfig

import connection.WebsocketConnectionServer

class ConnectionData {
    companion object {
        var websocketConnectionServer: WebsocketConnectionServer? = null
        var port: Int? = null
    }
}
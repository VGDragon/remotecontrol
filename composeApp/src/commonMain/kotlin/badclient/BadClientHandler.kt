package badclient

import messages.base.MessageBase
import org.java_websocket.WebSocket
import kotlin.random.Random

class BadClientHandler (val p0: WebSocket, val messageNumber: Int){

    companion object {

        val badClientMap: MutableMap<WebSocket, BadClientHandler> = mutableMapOf()
        val badClientMapLock = Object()
        fun handleBadClient(p0: WebSocket) {
            println("Handling bad client")
            val badClient = synchronized(badClientMapLock) {
                if (!badClientMap.containsKey(p0)) {
                    badClientMap[p0] = BadClientHandler(p0, 0)
                }
                badClientMap[p0]!!
            }
            Thread.sleep(1000)
            synchronized(badClientMapLock) {
                if (badClient.messageNumber > 100) {
                    p0.close()
                    return
                }
                if (badClient.messageNumber == 0) {
                    p0.send(MessageBase(GlobalVariables.computerName, "Hi").toJson())
                }
                if (Random.nextBoolean()) {
                    p0.send(MessageBase(GlobalVariables.computerName, "Done").toJson())
                } else {
                    p0.send(MessageBase(GlobalVariables.computerName, "Error").toJson())
                }
            }
        }
    }
}
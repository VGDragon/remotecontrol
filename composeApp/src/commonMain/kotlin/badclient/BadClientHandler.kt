package badclient

import connection.server.Connection
import messages.base.MessageBase
import kotlin.random.Random

class BadClientHandler (val p0: Connection, var messageNumber: Int){

    companion object {

        val badClientMap: MutableMap<Connection, BadClientHandler> = mutableMapOf()
        val badClientMapLock = Object()
        fun handleBadClient(p0: Connection) {
            println("Handling bad client")
            val badClient = synchronized(badClientMapLock) {
                if (!badClientMap.containsKey(p0)) {
                    badClientMap[p0] = BadClientHandler(p0, 0)
                }
                badClientMap[p0]!!
            }
            Thread.sleep(1000)
            synchronized(badClientMapLock) {
                badClient.messageNumber++
                if (badClient.messageNumber > 100) {
                    p0.closeSession = true
                    return
                }
                if (badClient.messageNumber == 0) {
                    p0.sendQueue.add(MessageBase(GlobalVariables.computerName,
                        "Hi",
                        badClient.messageNumber.toLong()).toJson())
                }
                if (Random.nextBoolean()) {
                    p0.sendQueue.add(MessageBase(GlobalVariables.computerName,
                        "Done",
                        badClient.messageNumber.toLong()).toJson())
                } else {
                    p0.sendQueue.add(MessageBase(GlobalVariables.computerName,
                        "Error",
                        badClient.messageNumber.toLong()).toJson())
                }
            }
        }
    }
}
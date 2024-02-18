package connection

import GlobalVariables
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.netty.Netty
import io.ktor.server.request.receive
import io.ktor.server.response.respondText
import io.ktor.server.routing.routing
import io.ktor.server.routing.post
import rest.message.RestMessageKeyExchange

class RestServer {
    var server: NettyApplicationEngine? = null
    fun build(port: Int): NettyApplicationEngine {
        val server = embeddedServer(Netty, port = port) {
            routing {
                post("/key-exchange") {
                    val jsonString = call.receive<String>()
                    val restMessageKeyExchange = RestMessageKeyExchange.fromJson(jsonString)
                    call.respondText(keyExchange(restMessageKeyExchange))
                }
            }
        }
        this.server = server
        return server
    }
    fun stop(){
        if (server == null){
            return
        }
        server!!.stop(1000, 1000)
    }
    fun keyExchange(restMessageKeyExchange: RestMessageKeyExchange): String {
        var connectionKeyPair = ConnectionKeyPair.loadFile(restMessageKeyExchange.keyAlias)
        if (connectionKeyPair != null){
            connectionKeyPair.deleteKeyPair()
        }

        connectionKeyPair = ConnectionKeyPair(
            keyOwner = restMessageKeyExchange.keyOwner,
            keyAlias = restMessageKeyExchange.keyAlias,
            keyCryptoMethode = restMessageKeyExchange.keyCryptoMethode
        )
        connectionKeyPair.generateKeyPair()
        connectionKeyPair.privateKeyTarget = restMessageKeyExchange.privateKey
        connectionKeyPair.saveKeyPair()

        return RestMessageKeyExchange(
            keyOwner = GlobalVariables.computerName,
            keyAlias = GlobalVariables.computerName,
            keyCryptoMethode = restMessageKeyExchange.keyCryptoMethode,
            keyCryptoMethodeInstance = restMessageKeyExchange.keyCryptoMethodeInstance,
            privateKey = connectionKeyPair.ownPrivateKey).toJson()
    }
}
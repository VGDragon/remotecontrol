import connection.ConnectionKeyPair
import connection.RestServer

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

fun sendPost(urlString: String = "http://127.0.0.1:8080/test",
             bodyContent: String = "test") {
    val client = HttpClient.newBuilder().build();
    val request = HttpRequest.newBuilder()
        .uri(URI.create(urlString))
        //.header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(bodyContent))
        .build()
    val response = client.send(request, HttpResponse.BodyHandlers.ofString())
    val answer = response.body()
    println(answer)
}

class TempTestScript {


    fun test() {
        val connectionKeyPair = ConnectionKeyPair("test", "test").generateKeyPair()
        connectionKeyPair.privateKeyTarget = connectionKeyPair.ownPrivateKey
        val testString = "This is a test."
        var sendString = ""
        for (i in 0..1000) {
            sendString += testString
        }
        val encryptedString = connectionKeyPair.encrypt(sendString)
        //println("Encrypted: $encryptedString")
        val decryptedString = connectionKeyPair.decrypt(encryptedString)
        //println("Decrypted: $decryptedString")
        println(sendString == decryptedString)
        return
        val restServer = RestServer().build(8080)
        restServer.start(wait = false)
        Thread.sleep(1000)
        sendPost()
        restServer.stop(0, 0)

        while (true) {
            println("Server is running")
            Thread.sleep(1000)
        }
    }
}

fun main() {
    val test = TempTestScript()
    test.test()
}
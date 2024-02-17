import connection.ConnectionKeyPair
import connection.RestServer

import java.net.URI
import java.net.URL
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
        val testString = "This is a test"
        val encryptedString = connectionKeyPair.encrypt(testString)
        println("Encrypted: $encryptedString")
        val decryptedString = connectionKeyPair.decrypt(encryptedString)
        println("Decrypted: $decryptedString")
        println(testString == decryptedString)
        val restServer = RestServer().build(8080)
        restServer.start(wait = false)
        Thread.sleep(1000)
        sendPost()
        restServer.stop(0, 0)
        return

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
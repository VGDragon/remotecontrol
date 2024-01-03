package messages.base.client

import com.google.gson.Gson

class MessageClientBrideToClient(val clientName: String) {
    fun toJson(): String{
        return Gson().toJson(this)
    }
    companion object{
        fun fromJson(json: String): MessageClientBrideToClient {
            return Gson().fromJson(json, MessageClientBrideToClient::class.java)
        }
        const val TYPE = "brideToClient"
    }
}
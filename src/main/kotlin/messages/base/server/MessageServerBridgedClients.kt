package messages.base.server

import com.google.gson.Gson

class MessageServerBridgedClients(
    var clientName: String = ""
) {


    fun toJson(): String{
        return Gson().toJson(this)
    }

    companion object{
        fun fromJson(json: String): MessageServerBridgedClients {
            return Gson().fromJson(json, MessageServerBridgedClients::class.java)
        }
        const val TYPE = "bridgedClients"
    }
}
package messages.base.client

import com.google.gson.Gson

class MessageClientAddClientBridge (val clientName: String = ""){


    fun toJson(): String{
        return Gson().toJson(this)
    }

    // be able to use the function without creating an instance of the class
    companion object{
        fun fromJson(json: String): MessageClientAddClientBridge {
            return Gson().fromJson(json, MessageClientAddClientBridge::class.java)
        }
        const val TYPE = "connect"
    }
}
package messages.base.client

import com.google.gson.Gson

class MessageClientConnect (val clientName: String = ""){


    fun toJson(): String{
        return Gson().toJson(this)
    }

    // be able to use the function without creating an instance of the class
    companion object{
        fun fromJson(json: String): MessageClientConnect {
            return Gson().fromJson(json, MessageClientConnect::class.java)
        }
        const val TYPE = "connect"
    }
}
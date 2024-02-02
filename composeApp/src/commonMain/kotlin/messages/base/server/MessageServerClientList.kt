package messages.base.server

import com.google.gson.Gson

class MessageServerClientList {
    var clientNames: List<String> = listOf()


    fun toJson(): String{
        return Gson().toJson(this)
    }

    // be able to use the function without creating an instance of the class
    companion object{
        fun fromJson(json: String): MessageServerClientList {
            return Gson().fromJson(json, MessageServerClientList::class.java)
        }
        const val TYPE = "clientList"
    }

}
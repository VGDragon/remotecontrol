package messages.base.client

import com.google.gson.Gson

class MessageClientRegister(val clientName: String = "", val isExecutable: Boolean = true) {

    var connectedClientName: String = ""

    fun toJson(): String{
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): MessageClientRegister {
            return Gson().fromJson(json, MessageClientRegister::class.java)
        }
        const val TYPE = "register"
    }
}
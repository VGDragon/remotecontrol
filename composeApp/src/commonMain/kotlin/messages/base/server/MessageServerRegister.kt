package messages.base.server

import com.google.gson.Gson

class MessageServerRegister (val clientName: String = "",
                             val isExecutable: Boolean = true,
                             val registered: Boolean = true) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
    companion object {
        const val TYPE = "register"

        fun fromJson(json: String): MessageServerRegister {
            return Gson().fromJson(json, MessageServerRegister::class.java)
        }

    }
}
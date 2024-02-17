package messages.base

import com.google.gson.Gson

class MessageBase(val name: String, val msg: String) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
    companion object {
        fun fromJson(json: String): MessageBase {
            return Gson().fromJson(json, MessageBase::class.java)
        }
    }
}
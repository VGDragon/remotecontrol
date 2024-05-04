package messages.base.client

import com.google.gson.Gson

class MessageClientUpdate (val version: String,
                           val hash: String,
                           val updateOk: Boolean) {
    fun toJson(): String {
        return Gson().toJson(this)
    }
    companion object {
        fun fromJson(json: String): MessageClientUpdate {
            return Gson().fromJson(json, MessageClientUpdate::class.java)
        }
        val TYPE = "clientUpdate"
    }
}
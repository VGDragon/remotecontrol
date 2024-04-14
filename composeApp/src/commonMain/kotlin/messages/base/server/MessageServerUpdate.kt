package messages.base.server

import com.google.gson.Gson

class MessageServerUpdate (val version: String,
                           val hash: String,
                           val size: Long,
                           val packageNr: Long,
                           val packageAmount: Long) {
    var data: String = ""

    fun setData(data: ByteArray): MessageServerUpdate {
        this.data = String(data, Charsets.UTF_8)
        return this
    }
    fun getData(): ByteArray {
        return this.data.toByteArray(Charsets.UTF_8)
    }

    fun toJson(): String {
        return Gson().toJson(this)
    }
    companion object {
        fun fromJson(json: String): MessageServerUpdate {
            return Gson().fromJson(json, MessageServerUpdate::class.java)
        }
        val TYPE = "update"
    }
}
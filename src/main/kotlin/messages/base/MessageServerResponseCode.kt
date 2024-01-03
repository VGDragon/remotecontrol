package messages.base

import com.google.gson.Gson
import messages.WebsocketMessageServer

class MessageServerResponseCode (val status: ServerAnswerStatus, val message: Any){
    fun toJson(): String{
        return Gson().toJson(this)
    }
    companion object{
        fun fromJson(json: String): MessageServerResponseCode {
            return Gson().fromJson(json, MessageServerResponseCode::class.java)
        }
        fun toJson(status: ServerAnswerStatus, message: String): String {
            return WebsocketMessageServer(
                type = TYPE,
                data = MessageServerResponseCode(
                    status = status,
                    message=message)
                    .toJson())
                .toJson()
        }
        val TYPE = "serverResponseCode"

    }
}
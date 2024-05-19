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
        val TYPE = "serverResponseCode"

    }
}
package messages

import com.google.gson.Gson

class WebsocketMessageServer (
    val type: String,
    val data: String){

    fun toJson(): String{
        return Gson().toJson(this)
    }

    // be able to use the function without creating an instance of the class
    companion object{
        fun fromJson(json: String): WebsocketMessageServer{

            return Gson().fromJson(json, WebsocketMessageServer::class.java)
        }
    }
}
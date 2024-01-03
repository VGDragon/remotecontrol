package messages

import com.google.gson.Gson

class WebsocketMessageClient (
    val type: String,
    var apiKey: String,
    val data: String){

    fun toJson(): String{
        return Gson().toJson(this)
    }

    // be able to use the function without creating an instance of the class
    companion object{
        fun fromJson(json: String): WebsocketMessageClient{
            return Gson().fromJson(json, WebsocketMessageClient::class.java)
        }
    }
}
package rest.message

import com.google.gson.Gson

class RestMessageKeyExchange(val keyOwner: String,
                             val keyAlias: String,
                             val keyCryptoMethode: String,
                             val publicKey: String) {

    fun toJson(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun fromJson(json: String): RestMessageKeyExchange {
            return Gson().fromJson(json, RestMessageKeyExchange::class.java)
        }
    }
}
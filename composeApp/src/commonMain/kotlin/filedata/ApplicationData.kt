package filedata

import GlobalVariables
import com.google.gson.Gson
import java.io.File

class ApplicationData{
    var apiKey: String = ""
    var address: String = "localhost"
    var port: Int = 8899
    var isServer: Boolean = false
    var isClient: Boolean = true
    var exec: Boolean = false
    var computerName: String = ""
    var jarFilePath: String = GlobalVariables.jarPath()

    init {
        if (apiKey.isEmpty()){
            // create a new api key
            apiKey = "1234567890"
        }
    }


    fun toJson(): String{
        return Gson().toJson(this)
    }

    fun saveToFile(): ApplicationData {
        if (computerName.isEmpty()){
            computerName = GlobalVariables.computerName
        }
        // save the api key to a file
        val file = File(GlobalVariables.applicationDataFile())
        file.writeText(this.toJson())
        return this
    }
    companion object{
        fun fromJson(json: String): ApplicationData {
            return Gson().fromJson(json, ApplicationData::class.java)
        }
        fun fromFile(): ApplicationData {
            val file = File(GlobalVariables.applicationDataFile())
            if (file.exists()){
                val json = file.readText()
                return fromJson(json)
            }
            return ApplicationData().saveToFile()
        }
    }

}
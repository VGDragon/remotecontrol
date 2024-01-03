import com.google.gson.Gson

class ApplicationData{
    var apiKey: String = ""
    var ip: String = "localhost"
    var port: Int = 8080
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
        // save the api key to a file
        val file = java.io.File(GlobalVariables.applicationDataFile)
        file.writeText(this.toJson())
        return this
    }
    companion object{
        fun fromJson(json: String): ApplicationData {
            return Gson().fromJson(json, ApplicationData::class.java)
        }
        fun fromFile(): ApplicationData {
            val file = java.io.File(GlobalVariables.applicationDataFile)
            if (file.exists()){
                val json = file.readText()
                return fromJson(json)
            }
            return ApplicationData().saveToFile()
        }
    }

}
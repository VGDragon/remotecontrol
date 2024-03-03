import java.io.File

class GlobalVariables {
    companion object {
        val applicationDataFile: String = "data.json"
        val scriptFolder: String = "scripts"
        val taskFolder: String = "tasks"
        var keyPairsFolder = "keypairs"
        var computerName: String = ""

        var appFolderName = File("").absolutePath
        fun applicationDataFile(): String {
            return File(appFolderName, applicationDataFile).absolutePath
        }
        fun scriptFolder(): String {
            return File(appFolderName, scriptFolder).absolutePath
        }
        fun taskFolder(): String {
            return File(appFolderName, taskFolder).absolutePath
        }
        fun keyPairsFolder(): String {
            return File(appFolderName, keyPairsFolder).absolutePath
        }
        fun crateKeyPairsFolder(){
            val keyPairsFolder = File(keyPairsFolder())
            if (!keyPairsFolder.exists()){
                keyPairsFolder.mkdirs()
            }
        }
        fun createFolders(){
            val scriptFolder = File(scriptFolder())
            if (!scriptFolder.exists()){
                scriptFolder.mkdirs()
            }
            val taskFolder = File(taskFolder())
            if (!taskFolder.exists()){
                taskFolder.mkdirs()
            }
            val keyPairsFolder = File(keyPairsFolder())
            if (!keyPairsFolder.exists()){
                keyPairsFolder.mkdirs()
            }
        }

        fun preparedKeyPairExists(computerName: String): Boolean {
            val seach_folder = File(keyPairsFolder())
            if (seach_folder.exists()){
                val files = seach_folder.listFiles()
                if (files != null){
                    for (file in files){
                        if (!file.isFile){
                            continue
                        }
                        if (file.name.endsWith(".${computerName}")){
                            return true
                        }
                    }
                }
            }
            return false
        }
    }
}
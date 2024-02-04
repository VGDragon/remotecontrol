import java.io.File

class GlobalVariables {
    companion object {
        val applicationDataFile: String = "data.json"
        val scriptFolder: String = "scripts"
        val taskFolder: String = "tasks"
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
        fun createFolders(){
            val scriptFolder = File(scriptFolder())
            if (!scriptFolder.exists()){
                scriptFolder.mkdirs()
            }
            val taskFolder = File(taskFolder())
            if (!taskFolder.exists()){
                taskFolder.mkdirs()
            }
        }
    }
}
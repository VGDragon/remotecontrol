import java.io.File
import java.security.MessageDigest
import java.util.*

class GlobalVariables {
    companion object {
        val applicationDataFile: String = "data.json"
        val scriptFolder: String = "scripts"
        val taskFolder: String = "tasks"
        var keyPairsFolder = "keypairs"
        var updateFolder = "updates"
        var updateDataName = "updates.json"
        var computerName: String = ""
        val pingPongDelayTime: Long = 1000L
        val messageHistorySize = 20

        //val execClientServiceName = "remotecontrol_exec_client.service"
        //val serverServiceName = "remotecontrol_server.service"

        var applicationFolderName = File("").absolutePath
        var jarName: String = "remotecontrol.jar"
        var jarFolder: String = File(applicationFolderName).absoluteFile.parentFile.absolutePath
        val jarMaxTransferSize = 1024L * 1024L
        fun applicationDataFile(): String {
            return File(applicationFolderName, applicationDataFile).absolutePath
        }
        fun scriptFolder(): String {
            return File(applicationFolderName, scriptFolder).absolutePath
        }
        fun taskFolder(): String {
            return File(applicationFolderName, taskFolder).absolutePath
        }
        fun keyPairsFolder(): String {
            return File(applicationFolderName, keyPairsFolder).absolutePath
        }
        fun jarPath(): String {
            return File(jarFolder, jarName).absolutePath
        }
        fun crateKeyPairsFolder(){
            val keyPairsFolder = File(keyPairsFolder())
            if (!keyPairsFolder.exists()){
                keyPairsFolder.mkdirs()
            }
        }
        fun updateFolder(): String {
            return File(jarFolder, updateFolder).absolutePath
        }
        fun createUpdateFolder(){
            val updateFolder = File(updateFolder())
            if (!updateFolder.exists()){
                updateFolder.mkdirs()
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
        fun applicationName(): String {
            return jarName.substring(0, jarName.length - ".jar".length)
        }
        fun pcOS(): OsType {
            val osName = System.getProperty("os.name").lowercase(Locale.getDefault())
            return if (osName.contains("windows")) {
                OsType.WINDOWS
            } else if (osName.contains("linux")) {
                OsType.LINUX
            } else if (osName.contains("mac")) {
                OsType.MAC
            } else {
                OsType.UNKNOWN
            }
        }
        fun getHashValue(file: File): String {
            val bytes = file.readBytes()
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(bytes)
            return digest.fold("", { str, it -> str + "%02x".format(it) })
        }
        fun closeApplication(){
            System.exit(0)
        }

    }
}
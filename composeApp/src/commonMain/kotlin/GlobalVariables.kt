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
        val pingPongDelayTime: Long = 2000L
        val pingPongDelayTimeMax: Long = 5000L
        val messageHistorySize = 100
        val logName: String = "log.txt"

        //val execClientServiceName = "remotecontrol_exec_client.service"
        //val serverServiceName = "remotecontrol_server.service"

        var applicationFolderName = File("").absoluteFile.relativeTo(File(".").absoluteFile).path
        var jarName: String = "remotecontrol.jar"
        var jarFolder: String = if(File(applicationFolderName).absoluteFile.path.equals("/")){
            ""
        } else {
            File(applicationFolderName).absoluteFile.parentFile.relativeTo(File("").absoluteFile).path
        }
        val jarMaxTransferSize = 1024L * 50L
        fun applicationDataFile(): String {
            if (applicationFolderName.isEmpty()){
                return File(applicationDataFile).path
            }
            return File(applicationFolderName, applicationDataFile).path
        }
        fun scriptFolder(): String {
            if (applicationFolderName.isEmpty()){
                return File(scriptFolder).path
            }
            return File(applicationFolderName, scriptFolder).path
        }
        fun taskFolder(): String {
            if (applicationFolderName.isEmpty()){
                return File(taskFolder).path
            }
            return File(applicationFolderName, taskFolder).path
        }
        fun keyPairsFolder(): String {
            if (applicationFolderName.isEmpty()){
                return File(keyPairsFolder).path
            }
            return File(applicationFolderName, keyPairsFolder).path
        }
        fun jarPath(): String {
            return File(jarFolder, jarName).path
        }
        fun logPath(): String {
            if (applicationFolderName.isEmpty()){
                return File(logName).path
            }
            return File(applicationFolderName, logName).path
        }
        fun crateKeyPairsFolder(){
            val keyPairsFolder = File(keyPairsFolder())
            if (!keyPairsFolder.exists()){
                keyPairsFolder.mkdirs()
            }
        }
        fun updateFolder(): String {
            return File(jarFolder, updateFolder).path
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
        fun waitForFileFinishedCreating(file: File): Boolean {
            val maxEditDifference = 1000L
            while (true){
                if (!file.exists()){
                    return false
                }
                if (System.currentTimeMillis() - file.lastModified() > maxEditDifference){
                    return true
                }
                try {
                    Thread.sleep(1000L)
                } catch (e: InterruptedException){
                    return false
                }
            }
        }
        fun writeToLog(message: String){
            val logFile = File(logPath())
            logFile.appendText("${Date()} - $message\n")
        }
    }
}
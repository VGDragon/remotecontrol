package filedata

import GlobalVariables
import GlobalVariables.Companion.waitForFileFinishedCreating
import com.google.gson.Gson
import connection.WebsocketConnectionClient
import messages.WebsocketMessageClient
import messages.base.client.MessageClientUpdate
import messages.base.server.MessageServerUpdate
import okio.ByteString.Companion.toByteString
import java.io.File
import java.io.FileInputStream

class SoftwareUpdate(
    val applicationName: String,
    var version: String,
    val hashValue: String,
    var updateStatus: UpdateStatus = UpdateStatus.NOT_STARTED
) {
    var partAmount: Long = -1L
    var currentpart: Long = 0L

    //var updateFile: File? = null
    var partSize: Long = GlobalVariables.jarMaxTransferSize
    var inputStream: FileInputStream? = null
    var readBuffer: ByteArray = ByteArray(GlobalVariables.jarMaxTransferSize.toInt())

    fun writeFilePart(messageServerUpdate: MessageServerUpdate) {
        partSize = messageServerUpdate.size
        readBuffer = ByteArray(partSize.toInt())
        setData(messageServerUpdate.data)

        val updateFolder = File(GlobalVariables.updateFolder())
        if (!updateFolder.exists()) {
            updateFolder.mkdirs()
        }
        val updateFile = File(updateFolder, fileName())
        if (!updateFile.exists()) {
            updateFile.writeBytes(readBuffer)
            currentpart++
            if (currentpart % 10 == 0L) {
                println("Part: $currentpart of $partAmount")
            }
            updateStatus = UpdateStatus.RUNNING
            return
        }
        updateFile.appendBytes(readBuffer)
        currentpart++
        if (partAmount - currentpart < 10) {
            println("Part: $currentpart of $partAmount")
        } else if (currentpart % 10 == 0L) {
            println("Part: $currentpart of $partAmount")
        }
        if (partAmount != messageServerUpdate.packageNr) {
            updateStatus = UpdateStatus.RUNNING
            return
        }
        val checkHashTime = System.currentTimeMillis()
        if (GlobalVariables.getHashValue(updateFile).equals(hashValue)) {
            updateStatus = UpdateStatus.FINISHED
            saveToFile()
        } else {
            updateStatus = UpdateStatus.ERROR
        }
        println("Check hash time: ${System.currentTimeMillis() - checkHashTime}")
        return
    }

    fun checkUpdateFile(): Boolean {
        val updateFile = File(File(GlobalVariables.updateFolder()), fileName())
        if (!GlobalVariables.getHashValue(updateFile).equals(hashValue)) {
            updateStatus = UpdateStatus.ERROR
            return false
        }
        saveToFile()
        updateStatus = UpdateStatus.FINISHED
        return true
    }

    fun startUpdate(): Boolean {
        val updateFolder = File(GlobalVariables.updateFolder())
        if (!updateFolder.exists()) {
            updateFolder.mkdirs()
        }

        val newVersionFile = File(updateFolder, fileName())
        if (!newVersionFile.exists()) {
            return false
        }

        val hashValue = GlobalVariables.getHashValue(newVersionFile)
        if (hashValue != this.hashValue) {
            return false
        }
        // copy new version to the application folder
        var updateFile = File(File(GlobalVariables.jarPath()).parentFile, GlobalVariables.jarName + ".update")
        if (updateFile.exists()) {
            return false
        }
        saveToFile()
        newVersionFile.copyTo(updateFile)
        GlobalVariables.closeApplication()
        return true
    }

    fun fileName(): String {
        return "${applicationName}-${version}.jar"
    }

    fun toMessageJson(): String {
        val updateMessage: MessageServerUpdate = MessageServerUpdate(
            version = version,
            hash = hashValue,
            size = partSize,
            packageNr = currentpart,
            packageAmount = partAmount
        )
        updateMessage.data = readBuffer.toByteString().hex()
        //readBuffer.toHexString(HexFormat.UpperCase).hexToByteArray()
        return Gson().toJson(updateMessage)
    }

    fun saveToFile(): SoftwareUpdate {
        val updates = SoftwareUpdateData.fromFile()
        updates.add(this)
        SoftwareUpdateData.toFile(updates)
        return this
    }

    fun readFilePart(): ByteArray? {
        currentpart++
        val updateFolder = File(GlobalVariables.updateFolder())
        if (!updateFolder.exists()) {
            updateFolder.mkdirs()
        }
        val updateFile = File(updateFolder, fileName())
        if (!updateFile.exists()) {
            return null
        }
        if (partAmount == -1L) {
            val fileSize = updateFile.length()
            partAmount = fileSize / GlobalVariables.jarMaxTransferSize
            if (fileSize % GlobalVariables.jarMaxTransferSize > 0) {
                partAmount++
            }
        }
        if (inputStream == null) {
            inputStream = updateFile.inputStream()
        }
        //https://stackoverflow.com/questions/55416615/kotlin-reading-from-file-into-byte-array
        val sizeRead = inputStream!!.read(readBuffer)
        partSize = sizeRead.toLong()
        if (sizeRead == -1) {
            return null
        }
        return readBuffer
    }

    fun setData(data: String): SoftwareUpdate {
        @OptIn(ExperimentalStdlibApi::class)
        var byteArray = data.hexToByteArray()
        if (byteArray.size > partSize) {
            byteArray = byteArray.slice(0 until partSize.toInt()).toByteArray()
        }
        readBuffer = byteArray
        return this
    }

    companion object {
        fun fromJson(json: String): SoftwareUpdate {
            return Gson().fromJson(json, SoftwareUpdate::class.java)
        }

        fun fromServerMessage(messageServerUpdate: MessageServerUpdate): SoftwareUpdate {
            val softwareUpdate = SoftwareUpdate(
                applicationName = GlobalVariables.applicationName(),
                version = messageServerUpdate.version,
                hashValue = messageServerUpdate.hash
            )
            softwareUpdate.partAmount = messageServerUpdate.packageAmount
            return softwareUpdate
        }

        fun fromFile(): MutableList<SoftwareUpdate> {
            return SoftwareUpdateData.fromFile()
        }

        fun fromJarFile(file: File): SoftwareUpdate? {
            waitForFileFinishedCreating(file)
            if (file.extension != "jar") {
                return null
            }
            val parts = file.name.split("-")
            var fileName = ""
            for (i in 0 until parts.size - 1) {
                fileName += parts[i]
            }

            return SoftwareUpdate(
                applicationName = fileName,
                version = parts[parts.size - 1].substring(0, parts[parts.size - 1].length - ".jar".length),
                hashValue = GlobalVariables.getHashValue(file)
            )
        }

        fun newUpdateFile(): SoftwareUpdate? {
            val softwareUpdateFolder: File = File(GlobalVariables.updateFolder())
            if (!softwareUpdateFolder.exists()) {
                softwareUpdateFolder.mkdirs()
            }
            val softwareUpdates: MutableList<SoftwareUpdate> = fromFile()
            val softwareUpdateFileNames = softwareUpdates.map { it.fileName() }
            val files = softwareUpdateFolder.listFiles() ?: return null
            val newFiles: MutableList<File> = mutableListOf()
            for (file in files) {
                if (!file.isFile) {
                    continue
                }
                if (!file.name.endsWith(".jar")) {
                    continue
                }
                if (softwareUpdateFileNames.contains(file.name)) {
                    continue
                }
                newFiles.add(file)
            }
            if (newFiles.size == 0) {
                return null
            }
            newFiles.sortBy { it.name }
            return fromJarFile(newFiles[newFiles.size - 1])
        }
    }
}
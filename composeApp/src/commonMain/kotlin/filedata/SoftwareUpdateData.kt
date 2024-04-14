package filedata

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class SoftwareUpdateData(
    val applicationName: String,
    var version: String,
    val hashValue: String,
    val updateStatus: UpdateStatus
) {

    companion object {
        fun fromFile(): MutableList<SoftwareUpdate> {
            val updateFolder = File(GlobalVariables.updateFolder())
            if (!updateFolder.exists()) {
                updateFolder.mkdirs()
            }
            val updatesFile = File(updateFolder, GlobalVariables.updateDataName)
            if (!updatesFile.exists()) {
                return mutableListOf()
            }
            val json = updatesFile.readText()
            val typeToken = object : TypeToken<List<SoftwareUpdateData>>() {}.type
            val softwareUpdateDataList =
                Gson().fromJson<List<SoftwareUpdateData>>(json, typeToken) as MutableList ?: mutableListOf()
            val returnList: MutableList<SoftwareUpdate> = mutableListOf()
            for (softwareUpdateData in softwareUpdateDataList) {
                returnList.add(
                    SoftwareUpdate(
                        applicationName = softwareUpdateData.applicationName,
                        version = softwareUpdateData.version,
                        hashValue = softwareUpdateData.hashValue,
                        updateStatus = softwareUpdateData.updateStatus
                    )
                )
            }
            return returnList
        }

        fun toFile(softwareUpdates: List<SoftwareUpdate>) {
            val softwareUpdateDataList: MutableList<SoftwareUpdateData> = mutableListOf()
            for (softwareUpdate in softwareUpdates) {
                softwareUpdateDataList.add(
                    SoftwareUpdateData(
                        applicationName = softwareUpdate.applicationName,
                        version = softwareUpdate.version,
                        hashValue = softwareUpdate.hashValue,
                        updateStatus = softwareUpdate.updateStatus
                    )
                )
            }
            val json = Gson().toJson(softwareUpdateDataList)
            val updatesFile = File(GlobalVariables.updateFolder(), GlobalVariables.updateDataName)
            updatesFile.writeText(json)
        }
    }

}
package net.vgdragon.remotecontrol

import App
import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import filedata.ApplicationData

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var fileDir= getFilesDir().absolutePath
            if (!fileDir.endsWith("/")){
                fileDir += "/"
            }
            GlobalVariables.applicationFolderName = fileDir
            GlobalVariables.createFolders()
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ){
                println("ok")
            }
            val applicationsData = ApplicationData.fromFile()
            if (applicationsData.computerName.isEmpty()){
                val name = (getSystemService(BluetoothManager::class.java) as BluetoothManager).adapter.name
                applicationsData.computerName = name
                applicationsData.saveToFile()
                GlobalVariables.computerName = applicationsData.computerName
            } else {
                GlobalVariables.computerName = applicationsData.computerName
            }
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
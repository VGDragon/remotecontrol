package net.vgdragon.remotecontrol

import App
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var fileDir= getFilesDir().absolutePath
            if (!fileDir.endsWith("/")){
                fileDir += "/"
            }
            GlobalVariables.appFolderName = fileDir
            GlobalVariables.createFolders()
            var name = android.os.Build.DEVICE
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            )
                println("ok")
            name = android.bluetooth.BluetoothAdapter.getDefaultAdapter().name
            // get device name
            GlobalVariables.computerName = name

            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
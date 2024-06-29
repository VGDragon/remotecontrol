package guiElemetns

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun rowBigSeperator() {
    Row(modifier = Modifier.height(10.dp)) {

    }
}

@Composable
fun rowSmallSeperator() {
    Row(modifier = Modifier.height(2.dp)) {

    }
}

@Composable
fun writeErrorText(errorText: String) {
    Row() {
        if (errorText.isNotBlank()) {
            Text("Error: ${errorText}")
        } else {
            Text(" ")
        }
    }
}
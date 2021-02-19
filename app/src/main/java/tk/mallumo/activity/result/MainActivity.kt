package tk.mallumo.activity.result

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Providers
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import tk.mallumo.activity.result.ui.theme.ActivityresultTheme

val url: Uri by lazy {
    Uri.parse("https://github.com/mallumoSK?tab=repositories")
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ActivityresultTheme {
                Providers(AmbientActivityResult provides ActivityResult.get()) {
                    // A surface container using the 'background' color from the theme
                    Surface(color = MaterialTheme.colors.background) {
                        ContentUI()
                    }
                }
            }
        }
    }
}

@Composable
fun ContentUI() {
    Column {
        PermissionUI()
        Spacer(modifier = Modifier.size(30.dp))
        ActivityCallUI()
    }
}


@Composable
fun ActivityCallUI() {
    val activityCallResult = remember { mutableStateOf("NOT CALLED") }
    val ar = AmbientActivityResult.current
    Button(onClick = {
        //call as simply as possible :)
        //there is alternative for inline call as -> ar.activity<SomeActivity>()
        //second parameter is launchOpt, basically ... animation between activities
        ar.activity(
            Intent(Intent.ACTION_VIEW, url)
        ) { resultCode, data ->
            activityCallResult.value = """
                resultCode: $resultCode
                any extras: ${data?.extras?.isEmpty ?: false}
            """.trimIndent()
        }
    }) {
        Text(text = "Call intent (www)")
    }
    Text(text = "ActivityResult:\n ${activityCallResult.value}")
}

@Composable
fun PermissionUI() {
    val ar = AmbientActivityResult.current
    val permissionInfo = remember { mutableStateOf("") }
    Button(onClick = {
        // call permission
        ar.permission( // multiple permissions
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) {
            granted { // Function is "called" when all permissions are granted
                permissionInfo.value = "All granted: \n${it.keys}"
            }
            denied { //Function is "called" when one or more permissions are denied
                permissionInfo.value = """
                        Relected: ${it.entries.filter { !it.value }.map { it.key }}
                        GRANTED: ${it.entries.filter { it.value }.map { it.key }}
                    """.trimIndent()
            }
        }
    }) {
        Text(text = "Call permission")
    }
    Text(text = "PermissionResult:\n ${permissionInfo.value}")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Providers(AmbientActivityResult provides ActivityResult.get()) {
        ActivityresultTheme {
            ContentUI()
        }
    }
}
# activity-result

## handling activity and permission result inside Jetpack Compose

```
kotlin_version = '1.4.21'
compose_version = '1.0.0-alpha10'
```

![https://mallumo.jfrog.io/artifactory/gradle-dev-local/tk/mallumo/activity-result/](https://img.shields.io/maven-metadata/v?color=%234caf50&metadataUrl=https%3A%2F%2Fmallumo.jfrog.io%2Fartifactory%2Fgradle-dev-local%2Ftk%2Fmallumo%2Factivity-result%2Fmaven-metadata.xml&style=for-the-badge "Version")

#### Repository:

```groovy
repositories {
    maven {
        url = uri("https://mallumo.jfrog.io/artifactory/gradle-dev-local")
    }
}
dependencies {
    implementation "tk.mallumo:activity-result:$version_activity_result"
}
```

## Example

### Register provider

```kotlin
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Providers(AmbientActivityResult provides ActivityResult.get()) {
                SampleTheme(darkTheme = true) {
                    // rest of code
                }
            }
        }
    }
}
```

### Activity CALL

```kotlin
@Composable
fun ActivityCallUI() {
    val activityCallResult = remember { mutableStateOf("NOT CALLED") }
    val ar = AmbientActivityResult.current
    Button(onClick = {
        //call as simply as possible :)
        //there is alternative for inline call as -> ar.activity<SomeActivity>()
        //second parameter is launchOpt, basically ... animation between activities
        ar.activity(
            Intent(Intent.ACTION_VIEW,url)
        ) { resultCode, data ->
            activityCallResult.value = """
                resultCode: $resultCode
                any extras: ${data?.extras?.isEmpty?:false}
            """.trimIndent()
        }
    }) {
        Text(text = "Call intent (www)")
    }
    Text(text = "ActivityResult:\n ${activityCallResult.value}")
}
```

### Permission CALL

```kotlin
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
```
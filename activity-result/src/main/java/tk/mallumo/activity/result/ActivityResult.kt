package tk.mallumo.activity.result

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticAmbientOf
import androidx.compose.ui.platform.AmbientContext
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass


/**
 * ## Tool for handling android activity response
 * ### Example:
 * ```@kotlin
 * Providers(AmbientActivityResult provides ActivityResult.get()) {
 *      //...
 * }
 * ```
 * @see ActivityResult
 * */
@Suppress("unused")
val AmbientActivityResult = staticAmbientOf<ActivityResult> { error("Unexpected error") }

/**
 * ## Tool for handling android activity response
 * ### Example:
 * ```@kotlin
 * Providers(AmbientActivityResult provides ActivityResultHolder.get()) {
 *      //...
 * }
 * ```
 * @see ActivityResult.activity
 * @see ActivityResult.permission
 */
abstract class ActivityResult {

    companion object {
        /**
         * Instance creator in compose context
         */
        @Composable
        fun get(): ActivityResult {
            @SuppressLint("ComposableNaming")
            val ctx = AmbientContext.current
            return remember(ctx) {
                if (ctx is ComponentActivity) ActivityResultImpl(context = ctx)
                else defaultPreview
            }
        }

        /**
         * Instance creator outside of compose
         */
        fun get(activity: ComponentActivity): ActivityResult {
            return ActivityResultImpl(context = activity)
        }

        private val defaultPreview by lazy {
            object : ActivityResult() {
                override fun permission(
                    vararg permission: String,
                    response: Permission.() -> Unit
                ) {

                }

                override fun activity(
                    intent: Intent,
                    launchOpt: ActivityOptionsCompat?,
                    response: (resultCode: Int, data: Intent?) -> Unit
                ) {
                }

                override fun <T : Activity> activity(
                    activityClass: KClass<T>,
                    launchOpt: ActivityOptionsCompat?,
                    response: (resultCode: Int, data: Intent?) -> Unit
                ) {

                }
            }
        }
    }

    /**
     * Call uses-permission
     * @param permission array of permissions
     * @param response ambda callback for permissionResult
     * @see Permission.granted
     * @see Permission.denied
     * @see Manifest.permission
     */
    abstract fun permission(vararg permission: String, response: Permission.() -> Unit)

    /**
     * Call activity by intent
     * @param intent of activity
     * @param launchOpt customizable launch option, default is fade IN/OUT
     * @param response lambda callback on activity response
     * @see ActivityOptionsCompat
     */
    abstract fun activity(
        intent: Intent,
        launchOpt: ActivityOptionsCompat? = null,
        response: (resultCode: Int, data: Intent?) -> Unit = { _, _ -> }
    )

    abstract fun <T : Activity> activity(
        activityClass: KClass<T>,
        launchOpt: ActivityOptionsCompat? = null,
        response: (resultCode: Int, data: Intent?) -> Unit = { _, _ -> }
    )

    @Suppress("MemberVisibilityCanBePrivate")
    inline fun <reified T : Activity> activity(
        launchOpt: ActivityOptionsCompat? = null,
        noinline response: (resultCode: Int, data: Intent?) -> Unit = { _, _ -> }
    ) {
        activity(T::class, launchOpt, response)
    }


    class Permission(val permission: List<String>) {

        internal var grantedImpl: (Map<String, Boolean>) -> Unit = {}
        internal var deniedImpl: (Map<String, Boolean>) -> Unit = {}

        /**
         * Function is "called" when all permissions are granted
         */
        fun granted(body: (Map<String, Boolean>) -> Unit) {
            grantedImpl = body
        }

        /**
         * Function is "called" when one or more permissions are denied
         */
        fun denied(body: (Map<String, Boolean>) -> Unit) {
            deniedImpl = body
        }
    }
}

private class ActivityResultImpl(
    context: Context,
    private val component: ComponentActivity = context as ComponentActivity
) : ActivityResult() {

    private val activityResult get() = ActivityResultContracts.StartActivityForResult()
    private val permissionResult get() = ActivityResultContracts.RequestMultiplePermissions()

    companion object {
        private val requestCodeGenerator by lazy {
            AtomicInteger(0)
        }
    }

    /**
     * @param permission multiple permissions at once
     * @param response callback, solve response of permission call
     * @see ActivityResult.Permission.granted
     * @see ActivityResult.Permission.denied
     */
    override fun permission(vararg permission: String, response: Permission.() -> Unit) {
        val request = Permission(permission.toList())
        response(request)

        val allGranted = permission.all {
            ActivityCompat.checkSelfPermission(
                component,
                it
            ) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            request.grantedImpl(hashMapOf<String, Boolean>().apply {
                putAll(permission.map { Pair(it, true) })
            })
        } else {

            var requester: ActivityResultLauncher<Array<String>>? = null
            val key = "ActivityResultHolder-${requestCodeGenerator.getAndIncrement()}"

            requester = component.activityResultRegistry
                .register(key, permissionResult) { permissionResponse ->
                    requester?.unregister()
                    try {
                        if (permissionResponse.values.all { it }) {
                            request.grantedImpl(permissionResponse)
                        } else {
                            request.deniedImpl(permissionResponse)
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            requester.launch(request.permission.toTypedArray())
        }
    }


    /**
     * @param intent somekind of activity
     * @param launchOpt customizable animation
     * @param response callback, calls when activity returns
     */
    override fun activity(
        intent: Intent,
        launchOpt: ActivityOptionsCompat?,
        response: (resultCode: Int, data: Intent?) -> Unit
    ) {
        var request: ActivityResultLauncher<Intent>? = null
        val key = "ActivityResultHolder-${requestCodeGenerator.getAndIncrement()}"
        request = component.activityResultRegistry
            .register(key, activityResult) {
                request?.unregister()
                try {
                    response(it.resultCode, it.data)
                } catch (e: Throwable) {
                    e.printStackTrace()
                }
            }

        request.launch(
            intent,
            launchOpt ?: ActivityOptionsCompat.makeCustomAnimation(
                component,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
        )
    }

    override fun <T : Activity> activity(
        activityClass: KClass<T>,
        launchOpt: ActivityOptionsCompat?,
        response: (resultCode: Int, data: Intent?) -> Unit
    ) {
        activity(Intent(component, activityClass.java), launchOpt, response)
    }


}



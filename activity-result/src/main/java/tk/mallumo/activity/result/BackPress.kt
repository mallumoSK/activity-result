package tk.mallumo.activity.result

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.LifecycleOwner

@Suppress("unused")
val LocalBackPress = staticCompositionLocalOf<BackPress> { error("Unexpected error") }

abstract class BackPress {

    companion object {
        /**
         * Instance creator in compose context
         */
        @Composable
        fun get(): BackPress {
            val ctx = LocalContext.current
            val owner = LocalLifecycleOwner.current
            return remember(ctx) {
                if (ctx is ComponentActivity) BackPressImpl(
                    context = ctx,
                    owner = owner
                )
                else defaultPreview
            }
        }

        /**
         * Instance creator outside of compose
         */
        fun get(
            activity: ComponentActivity,
            owner: LifecycleOwner = activity
        ): BackPress {
            return BackPressImpl(
                context = activity,
                owner = owner
            )
        }

        private val defaultPreview by lazy {
            object : BackPress() {
                override fun registerBackPress(key: String, consumer: () -> Unit) {}

                override fun unRegisterBackPress(key: String) {}

                override fun virtualBackPress() {}
            }
        }
    }

    /**
     * * register bac-press callback by key
     *
     * @param key unique id of callback
     * @param consumer is invoked on back-press event
     * @see unRegisterBackPress
     */
    abstract fun registerBackPress(key: String, consumer: () -> Unit)

    /**
     * * remove previously registered callback consumer by key
     *
     *
     * @param key unique id of callback
     * @see registerBackPress
     */
    abstract fun unRegisterBackPress(key: String)

    /**
     * * invoke back-press from source code
     */
    abstract fun virtualBackPress()
}

private class BackPressImpl(
    context: Context,
    private val owner: LifecycleOwner,
    private val component: ComponentActivity = context as ComponentActivity
) : BackPress() {

    private val stack = hashMapOf<String, OnBackPressedCallback>()

    override fun registerBackPress(key: String, consumer: () -> Unit) {
        unRegisterBackPress(key)
        stack[key] = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                consumer()
            }
        }
        component.onBackPressedDispatcher.addCallback(owner, stack[key]!!)
    }

    override fun unRegisterBackPress(key: String) {
        stack[key]?.isEnabled = false
        stack.remove(key)
    }

    override fun virtualBackPress() {
        component.onBackPressedDispatcher.onBackPressed()
    }
}



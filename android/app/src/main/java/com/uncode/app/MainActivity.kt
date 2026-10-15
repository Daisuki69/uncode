package com.uncode.app

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import com.getcapacitor.BridgeActivity

class MainActivity : BridgeActivity() {

    companion object {
        @Volatile
        private var sIsRunning: Boolean = false

        @Volatile
        private var sInstance: MainActivity? = null

        @JvmStatic
        fun isRunning(): Boolean {
            val inst = sInstance
            return sIsRunning && inst != null && !inst.isFinishing && !inst.isDestroyed
        }

        @JvmStatic
        fun getInstance(): MainActivity? {
            return sInstance
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        sIsRunning = true
        sInstance = this
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        registerPlugin(LockPlugin::class.java)
        super.onCreate(savedInstanceState)
        QiezkaNotificationHelper.postAlwaysActiveResourceQueryNotification(this)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                try {
                    val handle = bridge?.getPlugin("LockPlugin")
                    val plugin = handle?.instance as? LockPlugin
                    if (plugin != null && plugin.hasBackListeners()) {
                        plugin.triggerBackPressed()
                        return
                    }
                } catch (_: Exception) {}

                // Fallback: minimize task to home screen
                moveTaskToBack(true)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        sIsRunning = true
        sInstance = this
        QiezkaWidgetProvider.syncLauncherIconVisibility(this)
        QiezkaNotificationListener.requestRebind(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        bridge?.onNewIntent(intent)
    }

    override fun onDestroy() {
        sIsRunning = false
        if (sInstance == this) {
            sInstance = null
        }
        super.onDestroy()
    }
}

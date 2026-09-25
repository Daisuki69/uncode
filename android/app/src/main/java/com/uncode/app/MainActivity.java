package com.uncode.app;

import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.PluginHandle;

public class MainActivity extends BridgeActivity {
    private static volatile boolean sIsRunning = false;
    private static volatile MainActivity sInstance = null;

    public static boolean isRunning() {
        return sIsRunning && sInstance != null && !sInstance.isFinishing() && !sInstance.isDestroyed();
    }

    public static MainActivity getInstance() {
        return sInstance;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        sIsRunning = true;
        sInstance = this;
        registerPlugin(LockPlugin.class);
        super.onCreate(savedInstanceState);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                try {
                    PluginHandle handle = getBridge() != null ? getBridge().getPlugin("LockPlugin") : null;
                    LockPlugin plugin = handle != null ? (LockPlugin) handle.getInstance() : null;
                    if (plugin != null && plugin.hasBackListeners()) {
                        plugin.triggerBackPressed();
                        return;
                    }
                } catch (Exception ignored) {}

                // Fallback: minimize task to home screen
                moveTaskToBack(true);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        sIsRunning = true;
        sInstance = this;
    }

    @Override
    public void onDestroy() {
        sIsRunning = false;
        if (sInstance == this) {
            sInstance = null;
        }
        super.onDestroy();
    }
}

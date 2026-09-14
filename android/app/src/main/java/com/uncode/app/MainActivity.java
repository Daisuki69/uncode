package com.uncode.app;

import android.os.Bundle;
import androidx.activity.OnBackPressedCallback;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.PluginHandle;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
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
}

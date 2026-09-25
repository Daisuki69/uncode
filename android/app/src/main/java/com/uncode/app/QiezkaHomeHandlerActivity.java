package com.uncode.app;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;

/**
 * QiezkaHomeHandlerActivity
 *
 * Serves as QIEZKA's Home Role activity and secondary app drawer launcher.
 * Implements the two-tier Home Shell architecture:
 * 1. Checks if MainActivity is running.
 * 2. If not running, displays "MainActivity is not running, launching it now...." and restarts it.
 * 3. If running, displays "MainActivity is running" and enables delegation to the user's real launcher.
 */
public class QiezkaHomeHandlerActivity extends Activity {
    private static final String TAG = "QiezkaHomeHandler";
    public static final String PREFS_NAME = "qiezka_home_proxy";
    public static final String KEY_SELECTED_LAUNCHER_PKG = "selected_launcher_pkg";
    public static final String KEY_SELECTED_LAUNCHER_CLS = "selected_launcher_cls";

    private TextView tvMainStatus;
    private TextView tvStatusDetail;
    private TextView tvTargetLauncher;
    private Button btnOpenMain;
    private Button btnDelegateHome;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home_handler);

        tvMainStatus = findViewById(R.id.tvMainStatus);
        tvStatusDetail = findViewById(R.id.tvStatusDetail);
        tvTargetLauncher = findViewById(R.id.tvTargetLauncher);
        btnOpenMain = findViewById(R.id.btnOpenMain);
        btnDelegateHome = findViewById(R.id.btnDelegateHome);

        btnOpenMain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openMainActivity();
            }
        });

        btnDelegateHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delegateToRealLauncher();
            }
        });

        evaluateLivenessAndSetup();
    }

    @Override
    protected void onResume() {
        super.onResume();
        evaluateLivenessAndSetup();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        evaluateLivenessAndSetup();
    }

    private void evaluateLivenessAndSetup() {
        // Step 1: Resolve the real visual launcher
        ComponentName targetLauncher = getSelectedLauncherComponent(this);
        if (targetLauncher != null) {
            String label = getAppLabelForPackage(targetLauncher.getPackageName());
            tvTargetLauncher.setText(label + " (" + targetLauncher.getPackageName() + ")");
        } else {
            tvTargetLauncher.setText("No external launcher found (Default OEM)");
        }

        // Step 2: Check if MainActivity is currently running
        boolean isRunning = MainActivity.isRunning();
        if (isRunning) {
            tvMainStatus.setText("🟢 MainActivity is running");
            tvMainStatus.setTextColor(Color.parseColor("#10B981"));
            tvStatusDetail.setText("Enforcement is operational. Background state and services are fully active.");
        } else {
            tvMainStatus.setText("🟡 MainActivity is not running, launching it now....");
            tvMainStatus.setTextColor(Color.parseColor("#F59E0B"));
            tvStatusDetail.setText("Auto-starting QIEZKA MainActivity to restore focus enforcement and web guards...");

            // Auto open / launch / restart MainActivity as required by architecture
            mainHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (!isFinishing() && !isDestroyed()) {
                        openMainActivity();
                    }
                }
            }, 600);
        }
    }

    private void openMainActivity() {
        try {
            Intent mainIntent = new Intent(this, MainActivity.class);
            mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(mainIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to launch MainActivity", e);
        }
    }

    private void delegateToRealLauncher() {
        try {
            ComponentName targetLauncher = getSelectedLauncherComponent(this);
            if (targetLauncher != null) {
                Intent forwardIntent = new Intent(Intent.ACTION_MAIN);
                forwardIntent.addCategory(Intent.CATEGORY_HOME);
                forwardIntent.setComponent(targetLauncher);
                forwardIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                                     | Intent.FLAG_ACTIVITY_NO_ANIMATION 
                                     | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                startActivity(forwardIntent);
                finish();
                overridePendingTransition(0, 0);
            } else {
                openMainActivity();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to delegate HOME intent", e);
        }
    }

    private String getAppLabelForPackage(String pkg) {
        try {
            PackageManager pm = getPackageManager();
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    public static ComponentName getSelectedLauncherComponent(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String savedPkg = prefs.getString(KEY_SELECTED_LAUNCHER_PKG, null);
        String savedCls = prefs.getString(KEY_SELECTED_LAUNCHER_CLS, null);

        // Sanitize existing preferences: If it was saved as com.android.settings or vetoed by Stage 1, purge it!
        if (savedPkg != null && AppClassifier.isStage1Vetoed(savedPkg, null)) {
            prefs.edit()
                .remove(KEY_SELECTED_LAUNCHER_PKG)
                .remove(KEY_SELECTED_LAUNCHER_CLS)
                .apply();
            savedPkg = null;
            savedCls = null;
        }

        PackageManager pm = context.getPackageManager();
        if (savedPkg != null && savedCls != null) {
            try {
                pm.getActivityInfo(new ComponentName(savedPkg, savedCls), 0);
                return new ComponentName(savedPkg, savedCls);
            } catch (Exception ignore) {}
        }

        // Auto-discover installed real launchers (excluding QIEZKA itself and fallback/settings activities)
        Intent queryIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        List<ResolveInfo> candidates = pm.queryIntentActivities(queryIntent, PackageManager.MATCH_DEFAULT_ONLY);

        for (ResolveInfo info : candidates) {
            if (InstalledLauncherDetector.isRealLauncher(info, context.getPackageName())) {
                String pkg = info.activityInfo.packageName;
                prefs.edit()
                    .putString(KEY_SELECTED_LAUNCHER_PKG, pkg)
                    .putString(KEY_SELECTED_LAUNCHER_CLS, info.activityInfo.name)
                    .apply();
                return new ComponentName(pkg, info.activityInfo.name);
            }
        }
        return null;
    }
}

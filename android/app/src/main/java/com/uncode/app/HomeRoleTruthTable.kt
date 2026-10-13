package com.uncode.app

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import java.util.Locale

/**
 * Authoritative 4-State Home Role Truth Table Engine.
 *
 * Consolidates all scattered default home launcher detection, role dialog interception,
 * and Truth Table policy evaluation into a single, unified gate.
 *
 * Truth Table Matrix:
 * State 1: SIREN IS Default + Session Active     -> INTERCEPT (Evict role prompt, assert Lock screen)
 * State 2: SIREN IS Default + Session Idle       -> ALLOW (Permitted home setup, bypass Stage 1 Master Veto)
 * State 3: SIREN NOT Default + Session Active    -> ALLOW (Permitted! Student must be able to select SIREN)
 * State 4: SIREN NOT Default + Session Idle      -> ALLOW (Permitted! Normal home app selection outside hours)
 */
object HomeRoleTruthTable {
    private const val TAG = "HomeRoleTruthTable"

    enum class Decision {
        INTERCEPT,     // State 1: Evasion attempt during active lock -> evict immediately
        ALLOW,         // States 2, 3, 4: Permitted Home Role surface -> allow completely, bypass Stage 1 Master Veto
        NOT_HOME_ROLE  // Not a Home Role surface -> proceed to standard app / settings classifier
    }

    /**
     * Determines whether the given event, window root, package, or activity represents a
     * Home Role assignment dialog, default home picker, or role change trigger.
     */
    @JvmStatic
    fun isHomeRoleSurface(
        context: Context?,
        event: AccessibilityEvent?,
        root: AccessibilityNodeInfo?,
        pkg: String?,
        activityCls: String?
    ): Boolean {
        var lowerPkg = pkg?.lowercase(Locale.US) ?: ""
        if (lowerPkg.isEmpty() && event?.packageName != null) {
            lowerPkg = event.packageName.toString().lowercase(Locale.US)
        }

        val myPkg = context?.packageName ?: "com.uncode.app"
        // 1. Unconditional exclusions: Self, SystemUI, and SIREN HomeProxy
        if (lowerPkg == myPkg || lowerPkg == "com.android.systemui" || lowerPkg == "com.siren.homeproxy") {
            return false
        }

        // 2. Direct Activity class match (language-independent sub-microsecond memory check)
        var classStr = activityCls?.lowercase(Locale.US) ?: ""
        if (classStr.isEmpty()) {
            val eventCls = event?.className?.toString()?.lowercase(Locale.US)
            if (eventCls != null) classStr = eventCls
        }
        if (classStr.isNotEmpty() && AppClassifier.isHomeRoleActivity(classStr)) {
            return true
        }

        // 3. Proactive tap interception on trigger text / buttons (e.g. "Set as default", "Default launcher")
        if (event != null && event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED) {
            // Immediate match if click occurred inside permissioncontroller role UI
            if (lowerPkg.contains("permissioncontroller") || classStr.contains("requestroleactivity") ||
                classStr.contains("defaulthomepicker")) {
                return true
            }
            val texts = event.text
            if (texts != null) {
                for (t in texts) {
                    if (t != null && LockAccessibilityService.isHomeAppChangeTriggerText(t.toString())) {
                        return true
                    }
                }
            }
            val cd = event.contentDescription
            if (cd != null && LockAccessibilityService.isHomeAppChangeTriggerText(cd.toString())) {
                return true
            }
            val src = event.source
            if (src != null) {
                try {
                    val resId = src.viewIdResourceName?.lowercase(Locale.US)
                    if (resId != null) {
                        if (resId.contains("default_home") || resId.contains("defaulthome") ||
                            resId.contains("role_home") || resId.contains("default_launcher") ||
                            resId.contains("defaultlauncher")) {
                            return true
                        }
                    }
                    val txt = src.text
                    if (txt != null && LockAccessibilityService.isHomeAppChangeTriggerText(txt.toString())) {
                        return true
                    }
                } finally {
                    try { src.recycle() } catch (_: Exception) {}
                }
            }
        }

        // 4. Structural node hierarchy inspection
        if (root != null) {
            val effectivePkg = root.packageName?.toString()?.lowercase(Locale.US) ?: lowerPkg
            if (effectivePkg == myPkg || effectivePkg == "com.android.systemui" || effectivePkg == "com.siren.homeproxy") {
                return false
            }
            if (LockAccessibilityService.isHomeAppSelectionPackageOrRoot(context, effectivePkg, root)) {
                return true
            }
        }

        // 5. Window audit via context.windows (Essential for Android 14/15/16 modal dialogs when root is null)
        if (root == null && context is android.accessibilityservice.AccessibilityService && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            try {
                val winList = context.windows
                if (!winList.isNullOrEmpty()) {
                    for (w in winList) {
                        if ((w.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_APPLICATION ||
                             w.type == android.view.accessibility.AccessibilityWindowInfo.TYPE_SYSTEM) &&
                            (w.isActive || w.isFocused)) {
                            val wt = w.title
                            if (wt != null && LockAccessibilityService.isHomeAppSelectionTitle(wt.toString())) {
                                return true
                            }
                            val wRoot = w.root
                            if (wRoot != null) {
                                try {
                                    val wp = wRoot.packageName?.toString()?.lowercase(Locale.US) ?: ""
                                    if (wp != myPkg && wp != "com.android.systemui" && wp != "com.siren.homeproxy") {
                                        if (LockAccessibilityService.isHomeAppSelectionPackageOrRoot(context, wp, wRoot)) {
                                            return true
                                        }
                                    }
                                } finally {
                                    wRoot.recycle()
                                }
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        return false
    }

    /**
     * Authoritative Truth Table Evaluator.
     * Evaluates the active environment and returns INTERCEPT, ALLOW, or NOT_HOME_ROLE.
     */
    @JvmStatic
    fun evaluate(
        context: Context?,
        event: AccessibilityEvent?,
        root: AccessibilityNodeInfo?,
        pkg: String?,
        activityCls: String?,
        prefs: SharedPreferences?
    ): Decision {
        if (!isHomeRoleSurface(context, event, root, pkg, activityCls)) {
            return Decision.NOT_HOME_ROLE
        }

        // The surface is definitively a Home Role / Default Home picker!
        val isSirenDefault = LockAccessibilityService.isSirenDefaultLauncher(context)
        val hasActiveSession = LockAccessibilityService.isSessionOrScheduleActive(context, prefs)

        return if (isSirenDefault && hasActiveSession) {
            // STATE 1: SIREN is default AND focus session is active -> INTERCEPT
            Log.w(TAG, "🛡️ Home Truth Table [STATE 1 - INTERCEPT]: SIREN is default & session active -> Evicting change attempt")
            Decision.INTERCEPT
        } else {
            // STATES 2, 3, 4:
            // State 2: SIREN default + idle session -> ALLOW
            // State 3: SIREN NOT default + active session -> ALLOW (allows student to select SIREN)
            // State 4: SIREN NOT default + idle session -> ALLOW
            Log.i(TAG, "🛡️ Home Truth Table [STATE 2/3/4 - ALLOW]: isSirenDefault=$isSirenDefault, hasActiveSession=$hasActiveSession -> Permitted")
            Decision.ALLOW
        }
    }
}

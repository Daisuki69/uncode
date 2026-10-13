package com.uncode.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.app.Notification
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.net.Uri
import android.net.VpnService
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.accessibility.AccessibilityManager
import android.view.inputmethod.InputMethodManager
import androidx.activity.result.ActivityResult
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.getcapacitor.JSArray
import com.getcapacitor.JSObject
import com.getcapacitor.PermissionState
import com.getcapacitor.Plugin
import com.getcapacitor.PluginCall
import com.getcapacitor.PluginMethod
import com.getcapacitor.annotation.ActivityCallback
import com.getcapacitor.annotation.CapacitorPlugin
import com.getcapacitor.annotation.Permission
import com.getcapacitor.annotation.PermissionCallback
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.nio.charset.StandardCharsets
import java.util.Locale

@CapacitorPlugin(
    name = "LockPlugin",
    permissions = [
        Permission(
            alias = "notifications",
            strings = [android.Manifest.permission.POST_NOTIFICATIONS]
        )
    ]
)
class LockPlugin : Plugin() {

    companion object {
        private const val TAG = "LockPlugin"
        private const val PREFS_NAME = "uncode_lock"

        @JvmStatic
        fun clearLockdownState(context: Context?) {
            if (context == null) return
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val activeScheduleId = prefs.getString("active_schedule_id", "")
                val timeOffset = prefs.getLong("time_offset", 0L)
                val effectiveNow = System.currentTimeMillis() + timeOffset
                val currentLockEnd = prefs.getLong("lock_end_time", effectiveNow)

                val editor = prefs.edit()
                    .putBoolean("lockdown_active", false)
                    .putBoolean("consequence_active", false)
                    .remove("consequence_schedule_id")
                    .remove("lock_end_time")
                    .remove("active_schedule_id")
                    .putStringSet("whitelist", HashSet<String>())

                if (!activeScheduleId.isNullOrBlank()) {
                    editor.putLong("last_completed_window_end_$activeScheduleId", currentLockEnd)
                    Log.i(TAG, "clearLockdownState: recorded completed window for $activeScheduleId until $currentLockEnd")
                }
                editor.apply()
                AppClassifier.clearCache()

                // 1. Cancel Alarms
                AlarmReceiver.cancelLockEndAlarm(context)

                // 2. Stop Floating Overlay Service
                FloatingOverlayService.stopService(context)

                // 3. Stop Local DNS Sinkhole VPN
                LocalDnsVpnService.stopVpn(context)

                // 4. Cancel Notifications
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (nm != null) {
                    nm.cancel(AlarmReceiver.NOTIF_ID_STATUS)
                    nm.cancel(AlarmReceiver.NOTIF_ID_COMPLETED)
                }

                Log.i(TAG, "clearLockdownState: complete and authoritative unlock executed successfully")
            } catch (e: Exception) {
                Log.e(TAG, "clearLockdownState error", e)
            }
        }

        @JvmStatic
        fun startConsequenceState(context: Context?, scheduleId: String?, whitelist: Set<String>?) {
            if (context == null) return
            try {
                val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val editor = prefs.edit()
                    .putBoolean("consequence_active", true)
                    .putBoolean("lockdown_active", true)
                    .remove("lock_end_time")

                if (!scheduleId.isNullOrBlank()) {
                    editor.putString("consequence_schedule_id", scheduleId)
                }
                if (whitelist != null && whitelist.isNotEmpty()) {
                    val cleanWhitelist = HashSet<String>()
                    for (p in whitelist) {
                        if (p != null && !UnifiedPolicyRegistry.isPackageRegisteredInAnyService(p) &&
                            p != "com.google.android.googlequicksearchbox") {
                            cleanWhitelist.add(p)
                        }
                    }
                    editor.putStringSet("whitelist", cleanWhitelist)
                }
                editor.apply()

                // 1. Clear classification cache so whitelist takes immediate effect
                AppClassifier.clearCache()

                // 3. Start Local DNS Sinkhole VPN if configured (Dual Hybrid Mode)
                val webMode = prefs.getString("web_protection_mode", "accessibility")
                if ("dual_hybrid".equals(webMode, ignoreCase = true)) {
                    LocalDnsVpnService.startVpn(context)
                }

                // 4. Start Floating Overlay Service with Consequence Mode indicator
                FloatingOverlayService.startService(context, 0L, "Consequence Mode")

                // 5. Post Status Notification
                val timeOffset = prefs.getLong("time_offset", 0L)
                val effectiveNow = System.currentTimeMillis() + timeOffset
                val inOperatingHours = LockAccessibilityService.isInOperatingHours(context, effectiveNow)
                val isHardcore = "hardcore".equals(prefs.getString("operating_mode", "safemode"), ignoreCase = true)
                val notifMsg = if (isHardcore) {
                    "Distracting apps remain restricted 24/7 (Hardcore Mode) until homework is rescheduled and passed."
                } else {
                    if (inOperatingHours)
                        "Distracting apps are restricted until homework is rescheduled and passed."
                    else
                        "Consequence active: Enforcement paused during daytime (resumes at 7:00 PM)."
                }

                AlarmReceiver.createNotificationChannels(context)
                AlarmReceiver.showNotificationStatic(
                    context,
                    AlarmReceiver.NOTIF_ID_STATUS,
                    AlarmReceiver.CHANNEL_ID_STATUS,
                    "⚠️ QIEZKA Consequence Mode",
                    notifMsg,
                    Notification.PRIORITY_HIGH,
                    true
                )

                // 6. Immediately kick user out if currently inside a blocked app (if in operating hours)
                if (inOperatingHours) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        try {
                            val service = LockAccessibilityService.getInstance()
                            if (service != null) {
                                val activePkg = service.detectCurrentForegroundPackage()
                                if (activePkg != null && service.isPackageBlocked(activePkg)) {
                                    service.enforceBlock(activePkg)
                                }
                            }
                        } catch (_: Exception) {}
                    }, 300L)
                }

                Log.i(TAG, "startConsequenceState: complete consequence enforcement engaged (inOperatingHours=$inOperatingHours)")
            } catch (e: Exception) {
                Log.e(TAG, "startConsequenceState error", e)
            }
        }
    }

    private var dpm: DevicePolicyManager? = null
    private var adminComponent: ComponentName? = null
    private var prefs: SharedPreferences? = null

    override fun load() {
        val act = activity ?: return
        dpm = act.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        adminComponent = ComponentName(act, AdminReceiver::class.java)
        prefs = act.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getLongFromCall(call: PluginCall?, key: String): Long? {
        if (call == null || call.data == null) return null
        val obj = call.data.opt(key)
        if (obj is Number) {
            return obj.toLong()
        }
        if (obj is String) {
            try {
                return obj.toLong()
            } catch (_: NumberFormatException) {}
        }
        return null
    }

    @PluginMethod
    fun startLockdown(call: PluginCall) {
        try {
            val act = activity ?: throw IllegalStateException("Activity not attached")
            val sharedPrefs = prefs ?: act.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val allowedAppIds = call.getArray("allowedAppIds")
            val rawDuration = call.getInt("durationMinutes", 25) ?: 25
            val durationMinutes = rawDuration.coerceIn(1, 90).toLong()
            val customEndTime = getLongFromCall(call, "lockEndTime")
            val scheduleId = call.getString("scheduleId", "") ?: ""

            val timeOffset = sharedPrefs.getLong("time_offset", 0L)
            val effectiveNow = System.currentTimeMillis() + timeOffset

            val currentlyActive = sharedPrefs.getBoolean("lockdown_active", false)
            val existingEndTime = sharedPrefs.getLong("lock_end_time", 0L)

            var lockEndTime = when {
                customEndTime != null && customEndTime > 0 -> customEndTime
                currentlyActive && existingEndTime > effectiveNow -> {
                    Log.i(TAG, "startLockdown: Preserving active ongoing lockEndTime=$existingEndTime")
                    existingEndTime
                }
                else -> effectiveNow + (durationMinutes * 60L * 1000L)
            }

            val maxAllowedEnd = effectiveNow + (durationMinutes * 60L * 1000L)
            if (lockEndTime > maxAllowedEnd) {
                Log.w(TAG, "startLockdown: Clamping lockEndTime ($lockEndTime) to maxAllowedEnd ($maxAllowedEnd)")
                lockEndTime = maxAllowedEnd
            }

            val whitelist = HashSet<String>()
            whitelist.add(act.packageName) // Always allow QIEZKA itself

            // Always silently whitelist all system and third-party keyboards so typing never bricks lockdown
            try {
                val defaultIme = Settings.Secure.getString(act.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                if (defaultIme != null && defaultIme.contains("/")) {
                    whitelist.add(defaultIme.split("/")[0])
                }
                val imm = act.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                if (imm != null) {
                    val imis = imm.inputMethodList
                    if (imis != null) {
                        for (imi in imis) {
                            if (imi?.packageName != null) {
                                whitelist.add(imi.packageName)
                            }
                        }
                    }
                    val enabledImis = imm.enabledInputMethodList
                    if (enabledImis != null) {
                        for (imi in enabledImis) {
                            if (imi?.packageName != null) {
                                whitelist.add(imi.packageName)
                            }
                        }
                    }
                }
            } catch (_: Exception) {}

            if (allowedAppIds != null) {
                for (i in 0 until allowedAppIds.length()) {
                    val appId = allowedAppIds.getString(i)
                    if (appId != null && !AppClassifier.isSettingsOrDeviceManager(appId, null) &&
                        !AppClassifier.isPwaOrWebApk(appId) &&
                        !UnifiedPolicyRegistry.isPackageRegisteredInAnyService(appId) &&
                        appId != "com.google.android.googlequicksearchbox") {
                        whitelist.add(appId)
                    }
                }
            }

            // Save whitelist and timestamp for AccessibilityService
            sharedPrefs.edit()
                .putStringSet("whitelist", whitelist)
                .putBoolean("lockdown_active", true)
                .putLong("lock_end_time", lockEndTime)
                .putString("active_schedule_id", scheduleId)
                .apply()

            AppClassifier.clearCache()

            // Schedule exact lock end auto-release alarm
            AlarmReceiver.scheduleLockEndAlarm(act, lockEndTime, scheduleId)

            // Start Floating Assistive Timer Ball Overlay
            FloatingOverlayService.startService(act, lockEndTime, scheduleId)

            // Start Local DNS Sinkhole if web protection mode is dual_hybrid
            val webMode = sharedPrefs.getString("web_protection_mode", "accessibility")
            if ("dual_hybrid".equals(webMode, ignoreCase = true)) {
                LocalDnsVpnService.startVpn(act)
            }

            val ret = JSObject()
            ret.put("lockEndTime", lockEndTime)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "startLockdown failed", e)
            call.reject("startLockdown failed: " + e.message)
        }
    }

    @PluginMethod
    fun endLockdown(call: PluginCall) {
        try {
            clearLockdownState(activity)
            call.resolve()
        } catch (e: Exception) {
            Log.e(TAG, "endLockdown failed", e)
            call.reject("endLockdown failed: " + e.message)
        }
    }

    @PluginMethod
    fun setConsequenceActive(call: PluginCall) {
        try {
            val active = call.getBoolean("active", false) ?: false
            val scheduleId = call.getString("scheduleId", "") ?: ""

            if (active) {
                val rawWhitelist = call.getArray("whitelist")
                val whitelist = HashSet<String>()
                if (rawWhitelist != null) {
                    for (i in 0 until rawWhitelist.length()) {
                        whitelist.add(rawWhitelist.getString(i))
                    }
                }
                startConsequenceState(activity, scheduleId, whitelist)
            } else {
                clearLockdownState(activity)
            }
            call.resolve()
        } catch (e: Exception) {
            Log.e(TAG, "setConsequenceActive failed", e)
            call.reject("setConsequenceActive failed: " + e.message)
        }
    }

    @PluginMethod
    fun setOperatingMode(call: PluginCall) {
        try {
            val mode = call.getString("mode", "safemode") ?: "safemode"
            val sharedPrefs = prefs ?: activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPrefs?.edit()?.putString("operating_mode", mode)?.apply()
            Log.i(TAG, "Operating mode set to: $mode")

            val ret = JSObject()
            ret.put("success", true)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "setOperatingMode failed", e)
            call.reject("setOperatingMode failed: " + e.message)
        }
    }

    @PluginMethod
    fun setWebProtectionMode(call: PluginCall) {
        try {
            val rawMode = call.getString("mode", "accessibility")
            val mode = if ("dual_hybrid".equals(rawMode, ignoreCase = true)) "dual_hybrid" else "accessibility"
            val act = activity
            val sharedPrefs = prefs ?: act?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPrefs?.edit()?.putString("web_protection_mode", mode)?.apply()
            Log.i(TAG, "Web protection mode set to: $mode")

            val isLockActive = (sharedPrefs?.getBoolean("lockdown_active", false) == true) ||
                               (sharedPrefs?.getBoolean("consequence_active", false) == true)
            if (isLockActive && act != null) {
                if ("dual_hybrid".equals(mode, ignoreCase = true)) {
                    LocalDnsVpnService.startVpn(act)
                } else {
                    LocalDnsVpnService.stopVpn(act)
                }
            } else if (act != null) {
                LocalDnsVpnService.stopVpn(act)
            }

            val ret = JSObject()
            ret.put("success", true)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "setWebProtectionMode failed", e)
            call.reject("setWebProtectionMode failed: " + e.message)
        }
    }

    @PluginMethod
    fun setAllowYoutube(call: PluginCall) {
        try {
            val allow = call.getBoolean("allow", false) == true
            val sharedPrefs = prefs ?: activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentServices = sharedPrefs?.getStringSet("active_unified_services", HashSet<String>()) ?: HashSet<String>()
            val activeServices = HashSet<String>(currentServices)
            if (allow) {
                activeServices.add("youtube")
            } else {
                activeServices.remove("youtube")
            }

            val domains = UnifiedPolicyRegistry.getDomainsForServices(activeServices)

            sharedPrefs?.edit()
                ?.putBoolean("allow_youtube", allow)
                ?.putStringSet("active_unified_services", activeServices)
                ?.putStringSet("allowed_domains", domains)
                ?.apply()

            AppClassifier.clearCache()
            WebClassifier.clearCache()
            Log.i(TAG, "Allow YouTube set to: $allow (active_unified_services=$activeServices)")
            val ret = JSObject()
            ret.put("success", true)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "setAllowYoutube failed", e)
            call.reject("setAllowYoutube failed: " + e.message)
        }
    }

    @PluginMethod
    fun setServicePolicy(call: PluginCall) {
        try {
            val serviceId = call.getString("serviceId", "")
            if (serviceId.isNullOrBlank()) {
                call.reject("serviceId is required")
                return
            }
            val key = serviceId.lowercase(Locale.US).trim()
            val allowed = call.getBoolean("allowed", false) == true

            val sharedPrefs = prefs ?: activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentServices = sharedPrefs?.getStringSet("active_unified_services", HashSet<String>()) ?: HashSet<String>()
            val activeServices = HashSet<String>(currentServices)
            if (allowed) {
                activeServices.add(key)
            } else {
                activeServices.remove(key)
            }

            val editor = sharedPrefs?.edit()
            editor?.putStringSet("active_unified_services", activeServices)
            if ("youtube" == key) {
                editor?.putBoolean("allow_youtube", allowed)
            }
            val domains = UnifiedPolicyRegistry.getDomainsForServices(activeServices)
            editor?.putStringSet("allowed_domains", domains)
            editor?.apply()

            AppClassifier.clearCache()
            WebClassifier.clearCache()

            Log.i(TAG, "Unified service policy updated: $key=$allowed (active=$activeServices)")

            val ret = JSObject()
            ret.put("success", true)
            val activeArr = JSArray()
            for (s in activeServices) {
                activeArr.put(s)
            }
            ret.put("activeServices", activeArr)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "setServicePolicy failed", e)
            call.reject("setServicePolicy failed: " + e.message)
        }
    }

    @PluginMethod
    fun getActiveServices(call: PluginCall) {
        try {
            val sharedPrefs = prefs ?: activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val currentServices = sharedPrefs?.getStringSet("active_unified_services", HashSet<String>()) ?: HashSet<String>()
            val activeServices = HashSet<String>(currentServices)
            if (sharedPrefs?.getBoolean("allow_youtube", false) == true) {
                activeServices.add("youtube")
            }
            val ret = JSObject()
            val activeArr = JSArray()
            for (s in activeServices) {
                activeArr.put(s)
            }
            ret.put("activeServices", activeArr)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "getActiveServices failed", e)
            call.reject("getActiveServices failed: " + e.message)
        }
    }

    @PluginMethod
    fun getRegisteredServices(call: PluginCall) {
        try {
            val servicesArr = JSArray()
            for (svc in UnifiedPolicyRegistry.SERVICES.values) {
                val obj = JSObject()
                obj.put("id", svc.id)
                obj.put("name", svc.displayName)
                obj.put("displayName", svc.displayName)
                obj.put("description", svc.description)
                obj.put("badge", svc.badge)
                obj.put("iconName", svc.iconName)
                obj.put("themeColor", svc.themeColor)

                val pkgArr = JSArray()
                for (p in svc.packages) {
                    pkgArr.put(p)
                }
                obj.put("packages", pkgArr)

                val domainArr = JSArray()
                for (d in svc.domains) {
                    domainArr.put(d)
                }
                obj.put("domains", domainArr)

                servicesArr.put(obj)
            }
            val ret = JSObject()
            ret.put("services", servicesArr)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "getRegisteredServices failed", e)
            call.reject("getRegisteredServices failed: " + e.message)
        }
    }

    @PluginMethod
    fun setBlockWebGames(call: PluginCall) {
        try {
            // Milestone 18: Web games blocking is permanently active and cannot be turned off
            val sharedPrefs = prefs ?: activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPrefs?.edit()?.putBoolean("block_web_games", true)?.apply()
            Log.i(TAG, "Block web games locked to: true")
            val ret = JSObject()
            ret.put("success", true)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "setBlockWebGames failed", e)
            call.reject("setBlockWebGames failed: " + e.message)
        }
    }

    @PluginMethod
    fun setDnsFilterProfile(call: PluginCall) {
        try {
            val sharedPrefs = prefs ?: activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPrefs?.edit()?.putString("dns_filter_profile", "webclassifier")?.apply()
            Log.i(TAG, "DNS filter profile set to WebClassifier")
            activity?.let { LocalDnsVpnService.updateNotification(it) }
            val ret = JSObject()
            ret.put("success", true)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "setDnsFilterProfile failed", e)
            call.reject("setDnsFilterProfile failed: " + e.message)
        }
    }

    @PluginMethod
    fun setEnforceSafeSearch(call: PluginCall) {
        try {
            val enforce = call.getBoolean("enforce", true) == true
            val sharedPrefs = prefs ?: activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPrefs?.edit()?.putBoolean("enforce_safesearch", enforce)?.apply()
            Log.i(TAG, "Enforce SafeSearch set to: $enforce")
            activity?.let { LocalDnsVpnService.updateNotification(it) }
            val ret = JSObject()
            ret.put("success", true)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "setEnforceSafeSearch failed", e)
            call.reject("setEnforceSafeSearch failed: " + e.message)
        }
    }

    @PluginMethod
    fun requestVpnPermission(call: PluginCall) {
        try {
            val act = activity ?: throw IllegalStateException("Activity not attached")
            val vpnIntent = VpnService.prepare(act)
            if (vpnIntent == null) {
                val ret = JSObject()
                ret.put("granted", true)
                call.resolve(ret)
            } else {
                startActivityForResult(call, vpnIntent, "vpnPermissionCallback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "requestVpnPermission failed", e)
            val ret = JSObject()
            ret.put("granted", false)
            call.resolve(ret)
        }
    }

    @ActivityCallback
    private fun vpnPermissionCallback(call: PluginCall?, result: ActivityResult?) {
        val ret = JSObject()
        val granted = result != null && result.resultCode == Activity.RESULT_OK
        ret.put("granted", granted)
        call?.resolve(ret)
    }

    @PluginMethod
    fun getLockStatus(call: PluginCall) {
        try {
            val sharedPrefs = prefs ?: activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var isActive = sharedPrefs?.getBoolean("lockdown_active", false) == true
            val isConsequence = sharedPrefs?.getBoolean("consequence_active", false) == true
            val lockEndTime = sharedPrefs?.getLong("lock_end_time", 0L) ?: 0L
            val scheduleId = sharedPrefs?.getString("active_schedule_id", "") ?: ""

            val timeOffset = sharedPrefs?.getLong("time_offset", 0L) ?: 0L
            val effectiveNow = System.currentTimeMillis() + timeOffset

            // Native timestamp auto-expire only when NOT in consequence mode
            if (isActive && !isConsequence && lockEndTime > 0 && effectiveNow >= lockEndTime) {
                isActive = false
                clearLockdownState(activity)
            }

            val ret = JSObject()
            ret.put("isLockActive", isActive)
            ret.put("isConsequenceActive", isConsequence)
            ret.put("lockEndTime", lockEndTime)
            ret.put("activeScheduleId", scheduleId)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "getLockStatus failed", e)
            call.reject("getLockStatus failed: " + e.message)
        }
    }

    @PluginMethod
    fun syncTimeOffset(call: PluginCall) {
        try {
            val timeOffset = getLongFromCall(call, "timeOffset") ?: 0L
            val sharedPrefs = prefs ?: activity?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPrefs?.edit()?.putLong("time_offset", timeOffset)?.apply()
            Log.i(TAG, "Synchronized timeOffset to native: ${timeOffset}ms")

            activity?.let { ScheduleManager.rescheduleAll(it) }

            val ret = JSObject()
            ret.put("success", true)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "syncTimeOffset failed", e)
            call.reject("syncTimeOffset failed: " + e.message)
        }
    }

    @PluginMethod
    fun syncSchedules(call: PluginCall) {
        try {
            val act = activity ?: throw IllegalStateException("Activity not attached")
            val schedulesArr = call.getArray("schedules")
            val allowedAppIds = call.getArray("allowedAppIds")

            val schedulesJson = schedulesArr?.toString() ?: "[]"

            val whitelist = HashSet<String>()
            whitelist.add(act.packageName)
            if (allowedAppIds != null) {
                for (i in 0 until allowedAppIds.length()) {
                    val appId = allowedAppIds.getString(i)
                    if (appId != null && !AppClassifier.isSettingsOrDeviceManager(appId, null)) {
                        whitelist.add(appId)
                    }
                }
            }

            ScheduleManager.syncSchedules(act, schedulesJson, whitelist)
            AppClassifier.clearCache()

            val ret = JSObject()
            ret.put("success", true)
            call.resolve(ret)
        } catch (e: Exception) {
            Log.e(TAG, "syncSchedules failed", e)
            call.reject("syncSchedules failed: " + e.message)
        }
    }

    @PluginMethod
    fun getInstalledApps(call: PluginCall) {
        val task = Runnable {
            try {
                val act = activity ?: run {
                    call.reject("Activity unavailable")
                    return@Runnable
                }
                val pm = act.packageManager
                val myPkg = act.packageName

                // 1. Single-pass pre-queries (run once, not inside any package loop)
                val browserPackages = HashSet<String>()
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))
                    browserIntent.addCategory(Intent.CATEGORY_BROWSABLE)
                    val bList = pm.queryIntentActivities(browserIntent, 0)
                    for (r in bList) {
                        r.activityInfo?.packageName?.let { browserPackages.add(it) }
                    }
                } catch (_: Exception) {}

                val musicPackages = HashSet<String>()
                try {
                    val musicIntent = Intent(Intent.ACTION_MAIN)
                    musicIntent.addCategory(Intent.CATEGORY_APP_MUSIC)
                    val mList = pm.queryIntentActivities(musicIntent, 0)
                    for (r in mList) {
                        r.activityInfo?.packageName?.let { musicPackages.add(it) }
                    }
                    val audioIntent = Intent(Intent.ACTION_VIEW)
                    audioIntent.setDataAndType(Uri.parse("file://test.mp3"), "audio/*")
                    val aList = pm.queryIntentActivities(audioIntent, 0)
                    for (r in aList) {
                        r.activityInfo?.packageName?.let { musicPackages.add(it) }
                    }
                } catch (_: Exception) {}

                val cameraPackages = HashSet<String>()
                try {
                    val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    val cList = pm.queryIntentActivities(cameraIntent, 0)
                    for (r in cList) {
                        r.activityInfo?.packageName?.let { cameraPackages.add(it) }
                    }
                } catch (_: Exception) {}

                val keyboardPackages = HashSet<String>()
                try {
                    val defaultIme = Settings.Secure.getString(act.contentResolver, Settings.Secure.DEFAULT_INPUT_METHOD)
                    if (defaultIme != null && defaultIme.contains("/")) {
                        keyboardPackages.add(defaultIme.split("/")[0])
                    }
                    val imm = act.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    if (imm != null) {
                        val imis = imm.inputMethodList
                        if (imis != null) {
                            for (imi in imis) {
                                if (imi?.packageName != null) {
                                    keyboardPackages.add(imi.packageName)
                                }
                            }
                        }
                    }
                } catch (_: Exception) {}

                // 2. Query user-launchable apps directly to avoid iterating hundreds of hidden system daemons
                val launcherIntent = Intent(Intent.ACTION_MAIN)
                launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER)
                val launcherList = pm.queryIntentActivities(launcherIntent, 0)

                val candidatePackages = LinkedHashSet<String>()
                for (r in launcherList) {
                    r.activityInfo?.packageName?.let { candidatePackages.add(it) }
                }
                candidatePackages.addAll(browserPackages)
                candidatePackages.addAll(musicPackages)
                candidatePackages.addAll(cameraPackages)

                val apps = JSArray()
                val addedPackages = HashSet<String>()

                for (pkg in candidatePackages) {
                    if (pkg == null || pkg == myPkg || addedPackages.contains(pkg)) {
                        continue
                    }
                    if (AppClassifier.isSettingsOrDeviceManager(pkg, null) ||
                        AppClassifier.isStage1Bloat(pkg, null) ||
                        AppClassifier.isForbiddenDistraction(act, pkg) ||
                        AppClassifier.isPwaOrWebApk(pkg) ||
                        KnownDistracting.isKnownDistracting(pkg)) {
                        continue // Strictly omit anti-tamper, bloatware, games, PWAs/WebAPKs, and social media from candidate selection
                    }
                    val isBaselineSafe = KnownSafe.BASELINE_SAFE_PACKAGES.contains(pkg)
                    if (!isBaselineSafe && (UnifiedPolicyRegistry.isPackageRegisteredInAnyService(pkg) || isAiAppKeywords(pkg, null))) {
                        continue // Governed strictly via Unified Service Policy Engine in Settings
                    }
                    if (keyboardPackages.contains(pkg) || isKeyboardAppKeywords(pkg)) {
                        continue // Keyboards are silently exempted in lockdown, hidden from whitelist UI
                    }

                    try {
                        val appInfo = pm.getApplicationInfo(pkg, 0)
                        val appLabel = pm.getApplicationLabel(appInfo).toString()

                        if (isHiddenInfrastructureApp(pkg, appLabel)) {
                            continue // Hide camera extension proxies, aperture lens launchers, etc.
                        }
                        if (AppClassifier.isSettingsOrDeviceManager(pkg, appLabel) ||
                            AppClassifier.isStage1Bloat(pkg, appLabel) ||
                            AppClassifier.isPwaOrWebApk(pkg) ||
                            KnownDistracting.isKnownDistracting(pkg, appLabel) ||
                            (!isBaselineSafe && isAiAppKeywords(pkg, appLabel))) {
                            continue
                        }

                        val isLauncher = KnownSafe.isLauncherApp(act, pkg)
                        if (isLauncher) {
                            continue // Home Launchers have their own dedicated section in Settings; do not display in Allowed Apps
                        }

                        // Stage 3 Universal System Gateway (SYSALLOW) UI Non-Rendering Principle:
                        if (AppClassifier.isPassedStage3SystemAllow(appInfo, pkg, appLabel)) {
                            continue
                        }

                        val isHardcoded = KnownSafe.isHardcodedApp(act, appInfo, pkg, appLabel)

                        // Strict Whitelist Invariant: User can ONLY whitelist apps that are in KnownSafe or detected as safe
                        if (!isHardcoded && !isBaselineSafe && AppClassifier.isPackageBlocked(act, pkg, null)) {
                            continue // Omit unverified or distracting third-party apps
                        }

                        val isBrowser = browserPackages.contains(pkg) || isBrowserAppKeywords(pkg)
                        val isMusic = musicPackages.contains(pkg) || isMusicAppKeywords(pkg)
                        val isCamera = cameraPackages.contains(pkg) || isCameraAppKeywords(pkg)
                        val isAuthenticator = isAuthenticatorAppKeywords(pkg, appLabel)
                        val isNotes = isNotesAppKeywords(pkg, appLabel)
                        val isStudentApp = isBaselineSafe || isStudentAppKeywords(pkg, appLabel)
                        val isAi = isAiAppKeywords(pkg, appLabel)
                        val isMessaging = AppClassifier.isMessagingApp(pkg, appLabel)

                        addedPackages.add(pkg)

                        val app = JSObject()
                        app.put("id", pkg)
                        app.put("name", appLabel)

                        val isSimOrCarrier = AppClassifier.isSimOrCarrierService(pkg, appLabel)

                        var iconName = "LayoutGrid"
                        if (isLauncher) iconName = "Home"
                        else if (isBrowser) iconName = "Globe"
                        else if (isMusic) iconName = "Music"
                        else if (isCamera) iconName = "Camera"
                        else if (isAuthenticator) iconName = "ShieldCheck"
                        else if (isAi) iconName = "Sparkles"
                        else if (isNotes) iconName = "FileText"
                        else if (isStudentApp) iconName = "BookOpen"
                        else if (isMessaging || isSimOrCarrier) iconName = "MessageSquare"

                        val isAutoAllowed = !isHardcoded && !isLauncher

                        app.put("iconName", iconName)
                        app.put("isHardcoded", isHardcoded)
                        app.put("isLauncher", isLauncher)
                        app.put("isAutoAllowed", isAutoAllowed)
                        app.put("isMessaging", isMessaging)
                        app.put("isBrowser", isBrowser)
                        app.put("isMusic", isMusic)
                        app.put("isCamera", isCamera)
                        app.put("isAuthenticator", isAuthenticator)
                        app.put("isNotes", isNotes)
                        app.put("isStudentApp", isStudentApp)
                        app.put("isAi", isAi)

                        try {
                            val icon = pm.getApplicationIcon(appInfo)
                            val base64Icon = getBase64Icon(icon)
                            if (base64Icon != null) {
                                app.put("iconBase64", base64Icon)
                            }
                        } catch (_: Exception) {}

                        apps.put(app)
                    } catch (_: PackageManager.NameNotFoundException) {}
                }

                val result = JSObject()
                result.put("apps", apps)
                call.resolve(result)
            } catch (e: Exception) {
                Log.e(TAG, "getInstalledApps failed", e)
                call.reject("getInstalledApps failed: " + e.message)
            }
        }

        if (bridge != null) {
            bridge.execute(task)
        } else {
            Thread(task).start()
        }
    }

    private fun isAuthenticatorAppKeywords(packageName: String?, label: String?): Boolean {
        if (packageName != null) {
            val lower = packageName.lowercase(Locale.US)
            if (lower.contains("authenticator") || lower.contains("twofas") || lower.contains("duomobile") || lower.contains("yubioath")) {
                return true
            }
        }
        if (label != null) {
            val lowerLabel = label.lowercase(Locale.US)
            if (lowerLabel.contains("authenticator") || lowerLabel.contains("2fa") || lowerLabel.contains("otp")) {
                return true
            }
        }
        return false
    }

    private fun isNotesAppKeywords(packageName: String?, label: String?): Boolean {
        if (packageName != null) {
            val lower = packageName.lowercase(Locale.US)
            if (lower.contains("keep") || lower.contains("onenote") || lower.contains("obsidian") ||
                lower.contains("notion") || lower.contains("notepad") || lower.contains(".notes") ||
                lower.contains("memo") || lower.contains("simplenote") || lower.contains("colornote")) {
                return true
            }
        }
        if (label != null) {
            val lowerLabel = label.lowercase(Locale.US)
            if (lowerLabel.contains("notes") || lowerLabel.contains("notepad") || lowerLabel.contains("memo") ||
                lowerLabel.contains("keep") || lowerLabel.contains("onenote") || lowerLabel.contains("notion") ||
                lowerLabel.contains("obsidian") || lowerLabel.contains("journal")) {
                return true
            }
        }
        return false
    }

    private fun isStudentAppKeywords(packageName: String?, label: String?): Boolean {
        if (packageName != null) {
            val lower = packageName.lowercase(Locale.US)
            if (lower.contains("classroom") || lower.contains("canvas") || lower.contains("blackboard") ||
                lower.contains("schoology") || lower.contains("quizlet") || lower.contains("anki") ||
                lower.contains("gizmo") || lower.contains("saveall") || lower.contains("quiz") ||
                lower.contains("flashcard") || lower.contains("cram") || lower.contains("brainscape") ||
                lower.contains("studysmarter") || lower.contains("kahoot") || lower.contains("quizizz") ||
                lower.contains("desmos") || lower.contains("geogebra") || lower.contains("calculator") ||
                lower.contains("docs.editors") || (lower.contains("google") && lower.contains("docs")) ||
                lower.contains("photomath") || lower.contains("wolfram") || lower.contains("adobe.reader") ||
                lower.contains("camscanner") || lower.contains("translate") || lower.contains("deepl") ||
                lower.contains("duolingo") || lower.contains("khanacademy") || lower.contains("symbolab") ||
                lower.contains("mathway") || lower.contains("chegg") || lower.contains("termux") ||
                lower.contains("pydroid") || lower.contains("skydrive") || lower.contains("dropbox") ||
                lower.contains("readera") || lower.contains("wps")) {
                return true
            }
        }
        if (label != null) {
            val lowerLabel = label.lowercase(Locale.US)
            if (lowerLabel.contains("classroom") || lowerLabel.contains("canvas") || lowerLabel.contains("blackboard") ||
                lowerLabel.contains("schoology") || lowerLabel.contains("quizlet") || lowerLabel.contains("anki") ||
                lowerLabel.contains("gizmo") || lowerLabel.contains("quiz") || lowerLabel.contains("quizzes") ||
                lowerLabel.contains("flashcard") || lowerLabel.contains("flashcards") || lowerLabel.contains("tutor") ||
                lowerLabel.contains("brainscape") || lowerLabel.contains("studysmarter") || lowerLabel.contains("kahoot") ||
                lowerLabel.contains("quizizz") || lowerLabel.contains("exam") || lowerLabel.contains("testprep") ||
                lowerLabel.contains("desmos") || lowerLabel.contains("geogebra") || lowerLabel.contains("calculator") ||
                lowerLabel.contains("photomath") || lowerLabel.contains("docs") || lowerLabel.contains("sheets") ||
                lowerLabel.contains("slides") || lowerLabel.contains("drive") || lowerLabel.contains("student") ||
                lowerLabel.contains("acrobat") || lowerLabel.contains("scanner") || lowerLabel.contains("translate") ||
                lowerLabel.contains("dictionary") || lowerLabel.contains("duolingo") || lowerLabel.contains("khan academy") ||
                lowerLabel.contains("symbolab") || lowerLabel.contains("mathway") || lowerLabel.contains("chegg") ||
                lowerLabel.contains("termux") || lowerLabel.contains("onedrive") || lowerLabel.contains("dropbox") ||
                lowerLabel.contains("readera") || lowerLabel.contains("wps office")) {
                return true
            }
        }
        return false
    }

    private fun isAiAppKeywords(packageName: String?, label: String?): Boolean {
        if (packageName != null) {
            val lower = packageName.lowercase(Locale.US)
            if (lower.contains("chatgpt") || lower.contains("bard") || lower.contains("gemini") ||
                lower.contains("claude") || lower.contains("copilot") || lower.contains("perplexity") ||
                lower.contains("deepseek") || lower.contains(".poe") || lower.contains("grok")) {
                return true
            }
        }
        if (label != null) {
            val lowerLabel = label.lowercase(Locale.US)
            if (lowerLabel.contains("chatgpt") || lowerLabel.contains("gemini") || lowerLabel.contains("claude") ||
                lowerLabel.contains("copilot") || lowerLabel.contains("perplexity") || lowerLabel.contains("deepseek") ||
                lowerLabel.contains("ai assistant") || lowerLabel.contains("poe") || lowerLabel.contains("grok")) {
                return true
            }
        }
        return false
    }

    private fun isBrowserAppKeywords(packageName: String?): Boolean {
        if (packageName == null) return false
        val lower = packageName.lowercase(Locale.US)
        return lower.contains("chrome") || lower.contains("browser") || lower.contains("firefox") || lower.contains("opera") || lower.contains("brave") || lower.contains("duckduckgo")
    }

    private fun isMusicAppKeywords(packageName: String?): Boolean {
        if (packageName == null) return false
        val lower = packageName.lowercase(Locale.US)
        return lower.contains("music") || lower.contains("spotify") || lower.contains("tidal") || lower.contains("deezer") || lower.contains("soundcloud") || lower.contains("aspiro")
    }

    private fun isCameraAppKeywords(packageName: String?): Boolean {
        if (packageName == null) return false
        val lower = packageName.lowercase(Locale.US)
        return lower.contains("camera")
    }

    private fun isKeyboardAppKeywords(packageName: String?): Boolean {
        if (packageName == null) return false
        val lower = packageName.lowercase(Locale.US)
        return lower.contains("inputmethod") ||
            lower.contains("honeyboard") ||
            lower.contains("keyboard") ||
            lower.contains("gboard") ||
            lower.contains("swiftkey") ||
            lower.contains(".ime")
    }

    private fun isHiddenInfrastructureApp(packageName: String?, label: String?): Boolean {
        if (packageName != null) {
            val lower = packageName.lowercase(Locale.US)
            if (lower.contains("cameraextension") ||
                lower.contains("extensionproxy") ||
                lower.contains("lenslauncher") ||
                lower.contains("aperturelenslauncher") ||
                lower.contains("opensourcemusicplayer") ||
                lower.contains("androidopensourcemusicplayer") ||
                lower.contains("packageinstaller") ||
                lower.contains(".installer") ||
                lower == "com.android.vending" ||
                lower == "com.google.android.feedback" ||
                lower == "com.google.android.gms") {
                return true
            }
        }
        if (label != null) {
            val lowerLabel = label.lowercase(Locale.US).replace(" ", "")
            if (lowerLabel.contains("cameraextensionproxy") ||
                lowerLabel.contains("cameraextension") ||
                lowerLabel.contains("lenslauncher") ||
                lowerLabel.contains("aperturelenslauncher") ||
                lowerLabel.contains("aperaturelenslauncher") ||
                lowerLabel.contains("androidopensourcemusicplayer") ||
                lowerLabel.contains("opensourcemusicplayer") ||
                lowerLabel.contains("packageinstaller") ||
                lowerLabel.contains("googleplaystore") ||
                lowerLabel.contains("playstore")) {
                return true
            }
        }
        return false
    }

    private fun getBase64Icon(icon: Drawable?): String? {
        if (icon == null) return null
        return try {
            val targetDim = 96
            val bitmap = Bitmap.createBitmap(targetDim, targetDim, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            icon.setBounds(0, 0, targetDim, targetDim)
            icon.draw(canvas)

            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 75, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            null
        }
    }

    @PluginMethod
    override fun checkPermissions(call: PluginCall) {
        val result = JSObject()
        val act = activity ?: run {
            call.reject("Activity unavailable")
            return
        }

        result.put("isAdminActive", dpm != null && adminComponent != null && dpm!!.isAdminActive(adminComponent!!))

        var accessibilityEnabled = false

        // 1. Primary Check: Query active AccessibilityManager services
        try {
            val am = act.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            if (am != null) {
                val runningServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                if (runningServices != null) {
                    val pkg = act.packageName
                    for (s in runningServices) {
                        if (s.id != null && s.id.contains(pkg)) {
                            accessibilityEnabled = true
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "AccessibilityManager query failed", e)
        }

        // 2. Secondary Check: Fallback to Settings.Secure
        if (!accessibilityEnabled) {
            try {
                val enabled = Settings.Secure.getInt(act.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)
                if (enabled == 1) {
                    val services = Settings.Secure.getString(act.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                    if (services != null) {
                        val pkg = act.packageName
                        if (services.contains("$pkg/") || services.contains("LockAccessibilityService")) {
                            accessibilityEnabled = true
                        }
                    }
                }
            } catch (_: Exception) {}
        }

        result.put("isAccessibilityEnabled", accessibilityEnabled)

        // 3. Detect Installation Source
        var installSource = "On-Device Package Installer"
        var isAdbInstall = false
        try {
            val pm = act.packageManager
            var installer: String? = null
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val info = pm.getInstallSourceInfo(act.packageName)
                if (info != null) {
                    installer = info.installingPackageName ?: info.initiatingPackageName
                }
            } else {
                @Suppress("DEPRECATION")
                installer = pm.getInstallerPackageName(act.packageName)
            }

            if (installer == null || "com.android.shell" == installer) {
                isAdbInstall = true
                installSource = "ADB (PC Script / USB)"
            } else if (installer.contains("vending")) {
                installSource = "Google Play Store"
            } else if (installer.contains("packageinstaller")) {
                installSource = "On-Device Package Installer"
            } else {
                installSource = "Installer: $installer"
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query installer info", e)
        }

        val sharedPrefs = prefs ?: act.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        try {
            val launchIntent = act.intent
            if (launchIntent != null && "adb" == launchIntent.getStringExtra("setup_source")) {
                isAdbInstall = true
                installSource = "ADB (PC Script / USB)"
                sharedPrefs.edit().putBoolean("configured_via_adb", true).apply()
            } else if (sharedPrefs.getBoolean("configured_via_adb", false)) {
                isAdbInstall = true
                installSource = "ADB (PC Script / USB)"
            }
        } catch (_: Exception) {}

        result.put("isAdbInstall", isAdbInstall)
        result.put("installSource", installSource)

        var isBatteryIgnored = false
        try {
            val pm = act.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null) {
                isBatteryIgnored = pm.isIgnoringBatteryOptimizations(act.packageName)
            }
        } catch (_: Exception) {}
        result.put("isBatteryOptimizationIgnored", isBatteryIgnored)

        var isNotificationGranted = true
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                isNotificationGranted = ContextCompat.checkSelfPermission(
                    act, android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                isNotificationGranted = NotificationManagerCompat.from(act).areNotificationsEnabled()
            }
        } catch (_: Exception) {}
        result.put("isNotificationGranted", isNotificationGranted)

        var isExactAlarmGranted = true
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = act.getSystemService(Context.ALARM_SERVICE) as? android.app.AlarmManager
                if (am != null) {
                    isExactAlarmGranted = am.canScheduleExactAlarms()
                }
            }
        } catch (_: Exception) {}
        result.put("isExactAlarmGranted", isExactAlarmGranted)

        call.resolve(result)
    }

    @PluginMethod
    fun requestBatteryOptimization(call: PluginCall) {
        val act = activity ?: run {
            call.reject("Activity unavailable")
            return
        }
        try {
            val pm = act.getSystemService(Context.POWER_SERVICE) as? PowerManager
            if (pm != null && !pm.isIgnoringBatteryOptimizations(act.packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:" + act.packageName)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                act.startActivity(intent)
            }
            call.resolve()
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                act.startActivity(fallback)
                call.resolve()
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to request battery optimization", ex)
                call.reject("Failed to request battery optimization: " + ex.message)
            }
        }
    }

    @PluginMethod
    fun requestNotificationPermission(call: PluginCall) {
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                if (getPermissionState("notifications") != PermissionState.GRANTED) {
                    requestPermissionForAlias("notifications", call, "notificationPermCallback")
                    return
                }
            }
            val ret = JSObject()
            ret.put("granted", true)
            call.resolve(ret)
        } catch (_: Exception) {
            val act = activity ?: run {
                call.reject("Activity unavailable")
                return
            }
            try {
                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, act.packageName)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                act.startActivity(intent)
                call.resolve()
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to request notification permission", ex)
                call.reject("Failed to request notification permission: " + ex.message)
            }
        }
    }

    @PermissionCallback
    private fun notificationPermCallback(call: PluginCall?) {
        val ret = JSObject()
        ret.put("granted", getPermissionState("notifications") == PermissionState.GRANTED)
        call?.resolve(ret)
    }

    @PluginMethod
    fun openNotificationSettings(call: PluginCall) {
        val act = activity ?: run {
            call.reject("Activity unavailable")
            return
        }
        try {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, act.packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            act.startActivity(intent)
            call.resolve()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open notification settings", e)
            call.reject("Failed to open notification settings: " + e.message)
        }
    }

    @PluginMethod
    fun openAccessibilitySettings(call: PluginCall) {
        val act = activity ?: run {
            call.reject("Activity unavailable")
            return
        }
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        act.startActivity(intent)
        call.resolve()
    }

    @PluginMethod
    fun openAppInfo(call: PluginCall) {
        val act = activity ?: run {
            call.reject("Activity unavailable")
            return
        }
        try {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:" + act.packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            act.startActivity(intent)
            call.resolve()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app info", e)
            call.reject("Failed to open app info: " + e.message)
        }
    }

    @PluginMethod
    fun openDeviceAdminSettings(call: PluginCall) {
        try {
            val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            intent.putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Activate QIEZKA as Device Administrator to prevent uninstallation during lockdown."
            )
            startActivityForResult(call, intent, "deviceAdminResult")
        } catch (e: Exception) {
            Log.e(TAG, "Direct ADD_DEVICE_ADMIN failed, opening settings list", e)
            openDeviceAdminListFallback(call)
        }
    }

    @PluginMethod
    fun openDeviceAdminList(call: PluginCall) {
        openDeviceAdminListFallback(call)
    }

    private fun openDeviceAdminListFallback(call: PluginCall) {
        val act = activity ?: run {
            call.reject("Activity unavailable")
            return
        }
        try {
            val intent = Intent("android.settings.DEVICE_ADMIN_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            act.startActivity(intent)
            call.resolve()
        } catch (_: Exception) {
            try {
                val fallback = Intent(Settings.ACTION_SECURITY_SETTINGS)
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                act.startActivity(fallback)
                call.resolve()
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to open device admin settings", ex)
                call.reject("Failed to open device admin settings: " + ex.message)
            }
        }
    }

    @ActivityCallback
    private fun deviceAdminResult(call: PluginCall?, result: ActivityResult?) {
        if (call == null) return
        val isAdmin = dpm != null && adminComponent != null && dpm!!.isAdminActive(adminComponent!!)
        val ret = JSObject()
        ret.put("isAdminActive", isAdmin)
        call.resolve(ret)
    }

    @PluginMethod
    fun exportBackup(call: PluginCall) {
        val tempFileName = call.getString("tempFileName")
        val defaultName = call.getString("defaultName", "backup.json") ?: "backup.json"

        if (tempFileName == null) {
            call.reject("Must provide tempFileName")
            return
        }

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "application/json"
        intent.putExtra(Intent.EXTRA_TITLE, defaultName)

        startActivityForResult(call, intent, "exportBackupResult")
    }

    @ActivityCallback
    private fun exportBackupResult(call: PluginCall?, result: ActivityResult?) {
        if (call == null) return
        if (result != null && result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null && data.data != null) {
                val uri = data.data!!
                val tempFileName = call.getString("tempFileName")

                try {
                    val cacheDir = context.cacheDir
                    val tempFile = File(cacheDir, tempFileName ?: "")

                    if (!tempFile.exists()) {
                        call.reject("Temp file not found")
                        return
                    }

                    val input = FileInputStream(tempFile)
                    val out: OutputStream? = context.contentResolver.openOutputStream(uri)

                    val buffer = ByteArray(8192)
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        out?.write(buffer, 0, read)
                    }

                    input.close()
                    if (out != null) {
                        out.flush()
                        out.close()
                    }

                    tempFile.delete()

                    val ret = JSObject()
                    ret.put("success", true)
                    call.resolve(ret)
                } catch (e: Exception) {
                    call.reject("Failed to copy file: " + e.message, e)
                }
            } else {
                call.reject("No URI returned")
            }
        } else {
            val ret = JSObject()
            ret.put("canceled", true)
            call.resolve(ret)
        }
    }

    @PluginMethod
    fun importBackup(call: PluginCall) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.type = "*/*"
        val mimeTypes = arrayOf("application/json", "text/plain", "application/octet-stream", "*/*")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)

        startActivityForResult(call, intent, "importBackupResult")
    }

    @ActivityCallback
    private fun importBackupResult(call: PluginCall?, result: ActivityResult?) {
        if (call == null) return
        if (result != null && result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null && data.data != null) {
                val uri = data.data!!
                try {
                    val inStream = context.contentResolver.openInputStream(uri)
                    val reader = BufferedReader(InputStreamReader(inStream, StandardCharsets.UTF_8))
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line).append("\n")
                    }
                    reader.close()
                    val ret = JSObject()
                    ret.put("content", sb.toString())
                    ret.put("success", true)
                    call.resolve(ret)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to read imported backup", e)
                    call.reject("Failed to read file: " + e.message, e)
                }
            } else {
                call.reject("No file selected")
            }
        } else {
            val ret = JSObject()
            ret.put("canceled", true)
            call.resolve(ret)
        }
    }

    fun hasBackListeners(): Boolean {
        return hasListeners("backPressed")
    }

    fun triggerBackPressed() {
        notifyListeners("backPressed", JSObject())
    }

    @PluginMethod
    fun sanitizeImportApps(call: PluginCall) {
        try {
            val candidateArray = call.getArray("candidatePackageIds")
            val activeServicesArray = call.getArray("activeServices")

            val activeServiceIds = HashSet<String>()
            if (activeServicesArray != null) {
                for (i in 0 until activeServicesArray.length()) {
                    try {
                        val s = activeServicesArray.getString(i)
                        if (!s.isNullOrBlank()) {
                            activeServiceIds.add(s.trim().lowercase(Locale.US))
                        }
                    } catch (_: Exception) {}
                }
            }

            val cleanPackageIds = JSArray()
            val purgedPackageIds = JSArray()
            val processed = HashSet<String>()

            if (candidateArray != null) {
                val pm = context?.packageManager
                for (i in 0 until candidateArray.length()) {
                    var pkg: String? = null
                    try {
                        pkg = candidateArray.getString(i)
                    } catch (_: Exception) {
                        continue
                    }
                    if (pkg == null) continue
                    pkg = pkg.trim()
                    if (pkg.isEmpty() || processed.contains(pkg)) continue
                    processed.add(pkg)

                    var appInfo: ApplicationInfo? = null
                    var appLabel = ""
                    if (pm != null) {
                        try {
                            appInfo = pm.getApplicationInfo(pkg, 0)
                            val lbl = pm.getApplicationLabel(appInfo)
                            if (lbl != null) appLabel = lbl.toString()
                        } catch (_: Exception) {}
                    }

                    // Stage 1: Master Veto Gate (Settings, Device Admins, Bloatware)
                    if (AppClassifier.isSettingsOrDeviceManager(pkg, appLabel) || AppClassifier.isStage1Bloat(pkg, appLabel)) {
                        purgedPackageIds.put(pkg)
                        continue
                    }

                    // Check if package belongs to any Unified Policy Service (e.g. YouTube, AI) or is Google App
                    if (UnifiedPolicyRegistry.isPackageRegisteredInAnyService(pkg) ||
                        pkg == "com.google.android.googlequicksearchbox" ||
                        isAiAppKeywords(pkg, appLabel)) {
                        purgedPackageIds.put(pkg)
                        continue
                    }

                    // Check KnownDistracting & ForbiddenDistraction (Games, TikTok, Social, Screen shares, Coxeta)
                    if (KnownDistracting.isKnownDistracting(pkg, appLabel) ||
                        AppClassifier.isForbiddenDistraction(context, pkg)) {
                        purgedPackageIds.put(pkg)
                        continue
                    }

                    // Check Hidden Infrastructure (camera lens proxy, internal installers)
                    if (isHiddenInfrastructureApp(pkg, appLabel)) {
                        purgedPackageIds.put(pkg)
                        continue
                    }

                    // Check Home Launchers (launchers belong in Always Allowed by System, not custom allowedApps)
                    if (KnownSafe.isLauncherApp(context, pkg)) {
                        purgedPackageIds.put(pkg)
                        continue
                    }

                    // Stage 3 Universal System Gateway (SYSALLOW) Non-Rendering Principle:
                    if (appInfo != null && AppClassifier.isPassedStage3SystemAllow(appInfo, pkg, appLabel)) {
                        purgedPackageIds.put(pkg)
                        continue
                    }

                    // S3: Safe / Permitted Study App
                    cleanPackageIds.put(pkg)
                }
            }

            val ret = JSObject()
            ret.put("cleanPackageIds", cleanPackageIds)
            ret.put("purgedPackageIds", purgedPackageIds)
            ret.put("purgedCount", purgedPackageIds.length())
            call.resolve(ret)
        } catch (t: Throwable) {
            Log.e(TAG, "sanitizeImportApps failed: " + t.message, t)
            call.reject("Security sanitization error: " + t.message)
        }
    }

    @PluginMethod
    fun exitToHome(call: PluginCall) {
        val act = activity
        if (act != null) {
            act.runOnUiThread {
                act.moveTaskToBack(true)
                call.resolve()
            }
        } else {
            call.resolve()
        }
    }

    @PluginMethod
    fun showToast(call: PluginCall) {
        val message = call.getString("message", "")
        val act = activity
        if (!message.isNullOrBlank() && act != null) {
            act.runOnUiThread {
                android.widget.Toast.makeText(act, message, android.widget.Toast.LENGTH_SHORT).show()
                call.resolve()
            }
        } else {
            call.resolve()
        }
    }

    @PluginMethod
    fun getInstalledLaunchers(call: PluginCall) {
        try {
            val launchers = InstalledLauncherDetector.getInstalledLaunchersJson(context)
            val ret = JSObject()
            ret.put("launchers", launchers)
            call.resolve(ret)
        } catch (t: Throwable) {
            Log.e(TAG, "getInstalledLaunchers failed: " + t.message, t)
            call.reject("Failed to get installed launchers: " + t.message)
        }
    }

    @PluginMethod
    fun setSelectedLauncher(call: PluginCall) {
        val pkg = call.getString("packageName")
        val cls = call.getString("className", "") ?: ""
        if (pkg.isNullOrBlank()) {
            call.reject("Package name is required")
            return
        }
        val ok = LauncherStateManager.saveSelectedLauncher(context, pkg, cls)
        if (ok) {
            val ret = JSObject()
            ret.put("success", true)
            ret.put("packageName", pkg)
            ret.put("className", cls)
            call.resolve(ret)
        } else {
            call.reject("Package is vetoed by Stage 1 security or invalid")
        }
    }

    @PluginMethod
    fun clearSelectedLauncher(call: PluginCall) {
        try {
            LauncherStateManager.clearSelectedLauncher(context)
            val ret = JSObject()
            ret.put("success", true)
            call.resolve(ret)
        } catch (t: Throwable) {
            Log.e(TAG, "clearSelectedLauncher failed: " + t.message, t)
            call.reject("Failed to clear selected launcher: " + t.message)
        }
    }

    @PluginMethod
    fun getSelectedLauncher(call: PluginCall) {
        try {
            val launcher = LauncherStateManager.getSelectedLauncher(context)
            val ret = JSObject()
            if (launcher != null) {
                ret.put("packageName", launcher.packageName)
                ret.put("className", launcher.className)
            } else {
                ret.put("packageName", null)
                ret.put("className", null)
            }
            call.resolve(ret)
        } catch (t: Throwable) {
            Log.e(TAG, "getSelectedLauncher failed: " + t.message, t)
            call.reject("Failed to get selected launcher: " + t.message)
        }
    }

    @PluginMethod
    fun getSirenStatus(call: PluginCall) {
        try {
            val pm = context.packageManager
            var installed = false
            try {
                pm.getPackageInfo("com.siren.homeproxy", 0)
                installed = true
            } catch (_: PackageManager.NameNotFoundException) {}

            var isDefaultHome = false
            val homeIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
            val defaultHome = pm.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY)
            if (defaultHome != null && defaultHome.activityInfo != null) {
                isDefaultHome = "com.siren.homeproxy" == defaultHome.activityInfo.packageName
            }

            val ret = JSObject()
            ret.put("installed", installed)
            ret.put("isDefaultHome", isDefaultHome)
            ret.put("packageName", "com.siren.homeproxy")
            call.resolve(ret)
        } catch (t: Throwable) {
            Log.e(TAG, "getSirenStatus failed: " + t.message, t)
            call.reject("Failed to get SIREN status: " + t.message)
        }
    }

    @PluginMethod
    fun requestSirenHomeRole(call: PluginCall) {
        try {
            val intent = Intent("com.siren.homeproxy.ACTION_REQUEST_HOME")
            intent.setPackage("com.siren.homeproxy")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val pm = context.packageManager
            if (pm.resolveActivity(intent, 0) != null) {
                context.startActivity(intent)
                val ret = JSObject()
                ret.put("success", true)
                call.resolve(ret)
            } else {
                val homeSettings = Intent(Settings.ACTION_HOME_SETTINGS)
                homeSettings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    context.startActivity(homeSettings)
                } catch (_: Exception) {
                    val fallback = Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
                    fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallback)
                }
                val ret = JSObject()
                ret.put("success", true)
                call.resolve(ret)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "requestSirenHomeRole failed: " + t.message, t)
            call.reject("Failed to request SIREN home role: " + t.message)
        }
    }
}

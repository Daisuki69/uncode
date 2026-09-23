package com.uncode.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.provider.MediaStore;
import android.provider.Settings;
import android.provider.Browser;
import android.net.Uri;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodInfo;

import android.app.Notification;
import android.widget.Toast;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONObject;

public class LockAccessibilityService extends AccessibilityService {

    private static final String TAG = "LockAccessService";
    private static final String PREFS_NAME = "uncode_lock";

    /**
     * Packages that are always exempt from blocking.
     * Core OS, Telephony, In-Call UI, SIM Card Toolkit (STK), Carrier Services, MMS & Emergency Broadcasts.
     * NOTE: com.android.settings is intentionally NOT here — it should be blockable.
     * The launcher/home is also exempt because we actively send users there on block.
     */
    public static final Set<String> ALWAYS_EXEMPT = new HashSet<>(Arrays.asList(
        // Core OS framework & System UI
        "android",
        "com.android.systemui",

        // Core Telephony, In-Call UI & Telecom
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.incallui",
        "com.google.android.dialer",
        "com.samsung.android.dialer",
        "com.samsung.android.incallui",
        "com.samsung.android.app.telephonyui",
        "com.sec.android.app.servicemodeapp",
        "com.miui.telephonyui",
        "com.oppo.telephonyui",
        "com.coloros.telephonyui",
        "com.vivo.telephonyui",
        "com.asus.telephonyui",

        // SIM Card Toolkit (STK) & SIM Application Services (AOSP, Samsung, MTK, Transsion, Qualcomm, etc.)
        "com.android.stk",                         // AOSP SIM Toolkit
        "com.android.stk2",                        // AOSP Dual-SIM STK slot 2
        "com.google.android.stk",                  // Google SIM Toolkit
        "com.sec.android.app.simappdialog",        // Samsung STK dialog & Flash SMS popup (Critical for Globe/Smart)
        "com.sec.android.app.simsetting",          // Samsung SIM card manager
        "com.sec.android.app.simsettings",         // Samsung SIM settings variant
        "com.mediatek.stk",                        // MediaTek SIM Toolkit (Infinix, Tecno, etc.)
        "com.mediatek.stk2",                       // MediaTek SIM Toolkit slot 2
        "com.mediatek.simprocessor",               // MediaTek SIM Processor
        "com.mediatek.engineermode",               // MediaTek Engineer Mode
        "com.transsion.simtoolkit",                // Transsion SIM Toolkit (Infinix, Tecno, Itel)
        "com.transsion.stk",                       // Transsion STK
        "com.qualcomm.qti.simcontacts",            // Qualcomm SIM Contacts
        "com.qualcomm.qti.uim",                    // Qualcomm User Identity Module
        "com.qualcomm.qti.modemtestmode",          // Qualcomm Modem test
        "com.vivo.stk",                            // Vivo SIM Toolkit
        "com.coloros.simsettings",                 // Oppo / Realme SIM Settings
        "com.oppo.stk",                            // Oppo STK
        "com.coloros.stk",                         // ColorOS STK
        "com.huawei.stk",                          // Huawei SIM Toolkit
        "com.motorola.stk",                        // Motorola STK
        "com.zte.stk",                             // ZTE STK
        "com.oneplus.stk",                         // OnePlus STK

        // MMS & Native Carrier Messaging Services
        "com.android.mms",                         // AOSP Messaging / MMS
        "com.android.mms.service",                 // AOSP MMS Service
        "com.google.android.apps.messaging",       // Google Messages / RCS / Class 0 Flash SMS (Default on Infinix/Pixel/Samsung)
        "com.samsung.android.messaging",           // Samsung Messages / MMS
        "com.transsion.mms",                       // Transsion MMS / SMS
        "com.coloros.mms",                         // ColorOS / Oppo MMS
        "com.vivo.mms",                            // Vivo MMS
        "com.huawei.message",                      // Huawei Messaging
        "com.motorola.messaging",                  // Motorola Messaging
        "com.asus.message",                        // ASUS Messaging
        "com.zte.mms",                             // ZTE MMS

        // Cell Broadcast, Wireless Emergency Alerts (WEA) & Flash Alerts
        "com.android.cellbroadcastreceiver",       // AOSP Cell Broadcast
        "com.android.cellbroadcastreceiver.module",// Android Mainline Cell Broadcast Module
        "com.android.cellbroadcastservice",        // AOSP Cell Broadcast Service
        "com.google.android.cellbroadcastreceiver",// Google Emergency Alerts
        "com.google.android.cellbroadcastservice", // Google Cell Broadcast Service
        "com.mediatek.cellbroadcastreceiver",      // MediaTek Cell Broadcast
        "com.transsion.cellbroadcastreceiver",     // Transsion Cell Broadcast
        "com.oplus.cellbroadcastreceiver",         // Oppo / Realme Cell Broadcast
        "com.qualcomm.qti.cellbroadcastreceiver",  // Qualcomm Cell Broadcast
        "com.sec.android.app.wlantest",            // Samsung carrier wireless test
        "com.sec.android.app.safetyinformation",   // Samsung Safety / Emergency Information

        // Carrier Default Apps, Carrier Configuration & RCS/IMS
        "com.android.carrierdefaultapp",           // Android Carrier Default App
        "com.android.carrierconfig",               // Carrier Config
        "com.google.android.carrierconfig",        // Google Carrier Config
        "com.google.android.ims",                  // Google Carrier Services / RCS
        "com.samsung.android.ims",                 // Samsung IMS
        "com.sec.android.carrier.carrierwifi",     // Samsung Carrier Wi-Fi
        "com.shannon.imsservice",                  // Samsung Exynos IMS Service
        "com.mediatek.ims",                        // MediaTek IMS

        // Philippine Carrier Ecosystem (Globe Telecom, Smart Communications, DITO)
        "ph.com.globe",                            // Globe Telecom Carrier Services
        "ph.com.globe.globeathome",                // Globe at Home
        "ph.com.globe.globeonesuperapp",           // GlobeOne
        "com.globe.services",                      // Globe Services / SIM Menu
        "com.globe.telecom",                       // Globe Telecom
        "ph.com.smart",                            // Smart Communications
        "com.smart.services",                      // Smart Services / SIM Menu
        "ph.dito.telecommunity",                   // DITO Telecommunity
        "com.dito.services"                        // DITO Services
    ));

    public static final Set<String> TELEPHONY_AND_SIM_EXEMPT = ALWAYS_EXEMPT;

    /**
     * Known launcher packages. These are always allowed so the user can freely use
     * the home screen.
     */
    private static final Set<String> KNOWN_LAUNCHERS = new HashSet<>(Arrays.asList(
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher",
        "com.android.launcher2",
        "com.android.launcher3",
        "com.miui.home",
        "com.sec.android.app.launcher",
        "com.huawei.android.launcher",
        "com.oneplus.launcher",
        "com.oppo.launcher",
        "com.coloros.launcher",
        "com.realme.launcher",
        "com.transsion.launcher",
        "com.bbk.launcher2",
        "com.vivo.launcher"
    ));

    /**
     * Common OEM cameras, galleries, and system file/photo pickers.
     * These must be exempt so users can take photos or upload images to submit homework.
     */
    public static final Set<String> MEDIA_AND_FILE_EXEMPT = new HashSet<>(Arrays.asList(
        // Camera apps
        "com.google.android.GoogleCamera",
        "com.sec.android.app.camera",
        "com.android.camera",
        "com.android.camera2",
        "com.miui.camera",
        "com.huawei.camera",
        "com.oppo.camera",
        "com.coloros.camera",
        "com.oneplus.camera",
        "com.vivo.camera",
        "org.codeaurora.snapcam",
        "net.sourceforge.opencamera",

        // Gallery & Photos
        "com.google.android.apps.photos",
        "com.google.android.apps.photosgo",
        "com.sec.android.gallery3d",
        "com.android.gallery3d",
        "com.android.gallery",
        "com.miui.gallery",
        "com.coloros.gallery3d",
        "com.oneplus.gallery",
        "com.huawei.photos",
        "com.vivo.gallery",

        // Document pickers, Media providers & File managers
        "com.android.documentsui",
        "com.google.android.documentsui",
        "com.google.android.providers.media.module",
        "com.android.providers.media",
        "com.sec.android.app.myfiles",
        "com.google.android.apps.nbu.files",
        "com.mi.android.globalFileexplorer",
        "com.coloros.filemanager",
        "com.oneplus.filemanager",
        "com.huawei.filemanager",
        "com.vivo.FileManager",
        "com.motorola.filemanager",
        "com.asus.filemanager"
    ));

    /**
     * Known Music & Audio player packages that are hardcoded to be allowed during lock.
     */
    private static final Set<String> KNOWN_MUSIC_APPS = new HashSet<>(Arrays.asList(
        "com.spotify.music",
        "com.google.android.apps.youtube.music",
        "com.apple.android.music",
        "com.amazon.mp3",
        "com.aspiro.tidal",
        "deezer.android.app",
        "com.soundcloud.android",
        "com.sec.android.app.music",
        "com.miui.player",
        "com.android.music",
        "com.oppo.music",
        "com.vivo.musicplayer"
    ));

    /**
     * Common OEM and popular third-party keyboard packages (Input Method Editors).
     * These are permanently hardcoded as exempt to prevent the device from bricking during typing.
     */
    private static final Set<String> KNOWN_KEYBOARDS = new HashSet<>(Arrays.asList(
        "com.google.android.inputmethod.latin", // Gboard
        "com.samsung.android.honeyboard",       // Samsung Keyboard
        "com.touchtype.swiftkey",              // Microsoft SwiftKey
        "com.touchtype.swiftkey.beta",
        "com.android.inputmethod.latin",        // AOSP Keyboard
        "com.miui.voiceassist",
        "com.sohu.inputmethod.sogou.xiaomi",
        "com.huawei.ohos.inputmethod",
        "com.oppo.keyboard",
        "com.coloros.keyboard",
        "com.vivo.keyboard",
        "com.syntellia.fleksy.keyboard",
        "org.pocketworkstation.pckeyboard",
        "org.dslul.openboard.inputmethod.latin",
        "com.menny.android.anysoftkeyboard",
        "com.grammarly.android.keyboard",
        "com.baidu.input",
        "com.sohu.inputmethod.sogou",
        "com.google.android.tts"                // Google Speech Services / Voice Typing IME
    ));

    /**
     * Known 2FA Authenticator application package IDs.
     * Always exempt natively so students can sign into school portals and 2FA accounts.
     */
    private static final Set<String> KNOWN_AUTHENTICATORS = new HashSet<>(Arrays.asList(
        "com.google.android.apps.authenticator2",
        "com.azure.authenticator",
        "com.duosecurity.duomobile",
        "com.authy.authy",
        "com.twofasapp",
        "com.beemdevelopment.aegis",
        "com.bitwarden.authenticator",
        "com.lastpass.authenticator",
        "org.fedorahosted.freeotp",
        "com.yubico.yubioath"
    ));

    /**
     * Known Notes & Productivity applications permanently allowed to prevent procrastination
     * while enabling students to take notes, study, and complete homework.
     */
    private static final Set<String> KNOWN_NOTES_APPS = new HashSet<>(Arrays.asList(
        "com.google.android.keep",
        "com.samsung.android.app.notes",
        "com.microsoft.office.onenote",
        "notion.id",
        "md.obsidian",
        "com.evernote",
        "com.socialnmobile.dictapps.notepad.color.note",
        "com.zoho.notebook",
        "com.automattic.simplenote",
        "com.steadfastinnovation.android.furret",
        "com.nebula.notes",
        "com.colornote.notepad",
        "com.acadoid.lecturenotes"
    ));

    /**
     * Known Student, Coursework & Educational applications permanently allowed.
     */
    private static final Set<String> KNOWN_STUDENT_APPS = new HashSet<>(Arrays.asList(
        "com.google.android.apps.classroom",
        "com.google.android.apps.docs",
        "com.google.android.apps.docs.editors.docs",
        "com.google.android.apps.docs.editors.sheets",
        "com.google.android.apps.docs.editors.slides",
        "com.instructure.candroid",
        "com.blackboard.android.bbmatx",
        "com.schoology.app",
        "com.quizlet.quizletandroid",
        "com.ichi2.anki",
        "com.microblink.photomath",
        "com.desmos.calculator",
        "org.geogebra.android",
        "com.wolfram.android.alpha",
        "com.microsoft.office.officehubrow",
        "com.microsoft.office.word",
        "com.microsoft.office.excel",
        "com.microsoft.office.powerpoint",

        // Document Scanners & PDF Worksheets
        "com.adobe.reader",
        "com.adobe.scan.android",
        "com.intsig.camscanner",
        "cn.wps.moffice_eng",
        "com.microsoft.office.officelens",
        "org.readera",
        "com.xodo.pdf.reader",
        "com.foxit.mobile.pdf.lite",

        // Translation & Language Learning
        "com.google.android.apps.translate",
        "com.deepl.mobiletranslator",
        "com.duolingo",
        "com.merriamwebster",
        "com.mobisystems.msdict.embedded.wireless.oxford.dictionaryofenglish",
        "org.cambridge.cclae",

        // STEM Homework Solvers & Learning Hubs
        "org.khanacademy.android",
        "com.devsense.symbolab",
        "com.bagatrix.mathway.android",
        "com.chegg",
        "org.brilliant.android",
        "mendeleev.redlime",
        "com.cymath.cymath",

        // Cloud Storage & Sync
        "com.microsoft.skydrive",
        "com.dropbox.android",
        "net.box.android",

        // CS & Coding Environments
        "com.termux",
        "com.foxdebug.acode",
        "ru.iiec.pydroid3",
        "com.github.android"
    ));

    /**
     * Known AI Assistants & Research tools permanently allowed for study assistance.
     */
    private static final Set<String> KNOWN_AI_APPS = new HashSet<>(Arrays.asList(
        "com.google.android.apps.bard",
        "com.google.android.apps.gemini",
        "com.google.android.apps.googleassistant",
        "com.openai.chatgpt",
        "com.anthropic.claude",
        "com.microsoft.copilot",
        "ai.perplexity.app.android",
        "com.deepseek.chat",
        "com.quora.poe.android",
        "com.poe.android",
        "ai.inflection.pi"
    ));

    /**
     * Legitimate App Store and Package Installer services.
     * Package installers are explicitly allowed during lockdown and consequence modes per user configuration.
     */
    public static final Set<String> KNOWN_INSTALLER_AND_STORE_PACKAGES = new HashSet<>(Arrays.asList(
        "com.android.vending",                     // Google Play Store
        "com.google.android.feedback",             // Play Store feedback
        "com.google.android.gms",                  // Google Play Services
        "com.google.android.packageinstaller",     // Google Package Installer (Allowed)
        "com.android.packageinstaller",            // AOSP Package Installer (Allowed)
        "com.sec.android.app.samsungapps"          // Samsung Galaxy Store
    ));

    public static boolean isInstallerOrStoreApp(String pkg) {
        if (pkg == null) return false;
        return KNOWN_INSTALLER_AND_STORE_PACKAGES.contains(pkg) || pkg.toLowerCase(Locale.US).contains("packageinstaller");
    }

    /**
     * Dynamically identifies SIM Card Toolkit (STK), MMS, Carrier Notifications (e.g. Globe Telecom, Smart, DITO),
     * USSD, Flash SMS (Class 0), and Cell Broadcast alerts across various Android OEMs and carriers.
     */
    public static boolean isSimOrCarrierService(String pkg, String appLabel) {
        if (pkg == null) return false;
        if (ALWAYS_EXEMPT.contains(pkg)) return true;

        String lowerPkg = pkg.toLowerCase(Locale.US);

        // SIM Toolkit (STK) package patterns
        if (lowerPkg.equals("com.android.stk") || lowerPkg.equals("com.android.stk2") ||
            lowerPkg.contains(".stk") || lowerPkg.endsWith(".stk") ||
            lowerPkg.contains("simtoolkit") || lowerPkg.contains("simapp") ||
            lowerPkg.contains("simsetting") || lowerPkg.contains("simprocessor") ||
            lowerPkg.contains("simcard") || lowerPkg.contains("simcontacts") ||
            lowerPkg.contains(".uim")) {
            return true;
        }

        // MMS & native messaging services
        if (lowerPkg.equals("com.android.mms") || lowerPkg.contains(".mms") || lowerPkg.endsWith(".mms") ||
            lowerPkg.contains("mms.service") || lowerPkg.equals("com.google.android.apps.messaging") ||
            lowerPkg.equals("com.samsung.android.messaging") || lowerPkg.contains("messaging")) {
            // Guard: ensure it is not a third-party social messenger (e.g. Facebook Messenger)
            if (!lowerPkg.contains("facebook") && !lowerPkg.contains("orca") && !lowerPkg.contains("telegram") && !lowerPkg.contains("whatsapp")) {
                return true;
            }
        }

        // Cell Broadcast & Emergency alerts
        if (lowerPkg.contains("cellbroadcast") || lowerPkg.contains("emergencyalert") || lowerPkg.contains(".cbr")) {
            return true;
        }

        // Carrier default apps and configurations (including Globe, Smart, DITO)
        if (lowerPkg.contains("carrierdefaultapp") || lowerPkg.contains("carrierconfig") ||
            lowerPkg.contains("telephonyui") || lowerPkg.contains(".ims") || lowerPkg.contains("imsservice")) {
            return true;
        }

        // Philippine carriers (Globe, Smart, DITO)
        if (lowerPkg.startsWith("ph.com.globe") || lowerPkg.startsWith("com.globe") ||
            lowerPkg.startsWith("ph.com.smart") || lowerPkg.startsWith("com.smart") ||
            lowerPkg.startsWith("ph.dito") || lowerPkg.startsWith("com.dito") ||
            (lowerPkg.contains("globe") && (lowerPkg.contains("sim") || lowerPkg.contains("service") || lowerPkg.contains("carrier")))) {
            return true;
        }

        // Application Label inspection (catches OEM-customized STK and carrier dialogs)
        if (appLabel != null && !appLabel.trim().isEmpty()) {
            String lowerLabel = appLabel.toLowerCase(Locale.US);
            if (lowerLabel.equals("sim toolkit") || lowerLabel.equals("sim card toolkit") ||
                lowerLabel.equals("sim menu") || lowerLabel.equals("menu ng sim") ||
                lowerLabel.equals("stk") || lowerLabel.startsWith("stk ") ||
                lowerLabel.contains("globe services") || lowerLabel.contains("smart menu") ||
                lowerLabel.contains("dito menu") || lowerLabel.contains("cell broadcast") ||
                lowerLabel.contains("emergency alert") || lowerLabel.contains("wireless alerts") ||
                lowerLabel.contains("wireless emergency alerts") || lowerLabel.equals("mms service") ||
                lowerLabel.equals("carrier default app") || lowerLabel.contains("carrier services")) {
                return true;
            }
        }

        return false;
    }

    public static boolean isSimOrCarrierService(String pkg) {
        return isSimOrCarrierService(pkg, null);
    }

    public static volatile LockAccessibilityService instance = null;

    public static LockAccessibilityService getInstance() {
        return instance;
    }

    private final Set<String> dynamicExemptPackages = new HashSet<>();
    private final Set<String> dynamicKeyboardPackages = new HashSet<>();
    public static final Set<String> dynamicLauncherPackages = new HashSet<>();
    private SharedPreferences prefs;

    public static boolean isLauncherApp(Context context, String pkg) {
        if (pkg == null) return false;
        if (KNOWN_LAUNCHERS.contains(pkg) || dynamicLauncherPackages.contains(pkg)) return true;
        if (context != null) {
            try {
                PackageManager pm = context.getPackageManager();
                if (pm != null) {
                    Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    homeIntent.addCategory(Intent.CATEGORY_HOME);
                    homeIntent.setPackage(pkg);
                    List<ResolveInfo> list = pm.queryIntentActivities(homeIntent, 0);
                    if (list != null && !list.isEmpty()) {
                        dynamicLauncherPackages.add(pkg);
                        return true;
                    }
                }
            } catch (Exception ignore) {}
        }
        return false;
    }

    private String lastForegroundPackage = null;
    private String lastBlockedPackage = null;
    private final Set<String> firedWarningKeys = new HashSet<>();
    private final Handler tickerHandler = new Handler(Looper.getMainLooper());
    private final Runnable tickerRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                onTickerTick();
            } catch (Exception e) {
                Log.e(TAG, "Ticker error: " + e.getMessage());
            } finally {
                tickerHandler.postDelayed(this, 1000L);
            }
        }
    };

    public void startTicker() {
        tickerHandler.removeCallbacks(tickerRunnable);
        tickerHandler.post(tickerRunnable);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        refreshDynamicExemptPackages();
        startTicker();
        Log.i(TAG, "LockAccessibilityService onCreate — ticker started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTicker();
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i(TAG, "onTaskRemoved: app task cleared from recents — keeping ticker alive");
        startTicker();
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.packageNames = null; // Watch all packages
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                          AccessibilityEvent.TYPE_WINDOWS_CHANGED |
                          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                          AccessibilityEvent.TYPE_VIEW_CLICKED |
                          AccessibilityEvent.TYPE_VIEW_FOCUSED |
                          AccessibilityEvent.TYPE_VIEW_SCROLLED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                     AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS |
                     AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
        info.notificationTimeout = 50;
        setServiceInfo(info);

        refreshDynamicExemptPackages();
        startTicker();

        // Immediate evaluation on reconnect
        try {
            boolean active = prefs.getBoolean("lockdown_active", false);
            if (!active) {
                active = checkAndAutoStartScheduledLock(prefs);
            }
            if (active) {
                long lockEnd = prefs.getLong("lock_end_time", 0L);
                FloatingOverlayService.startService(this, lockEnd, "Study Session");
                String activePkg = detectCurrentForegroundPackage();
                if (activePkg != null && isPackageBlocked(activePkg)) {
                    enforceBlock(activePkg);
                }
            }
        } catch (Exception ignore) {}

        Log.i(TAG, "LockAccessibilityService connected and ticker running");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        tickerHandler.removeCallbacks(tickerRunnable);
    }

    /**
     * Dynamically discovers all installed apps that handle camera capture,
     * photo picking, and image document selection on this specific device.
     */
    private void refreshDynamicExemptPackages() {
        try {
            PackageManager pm = getPackageManager();
            if (pm == null) return;

            // Camera capture handlers
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            List<ResolveInfo> cameraApps = pm.queryIntentActivities(cameraIntent, 0);
            for (ResolveInfo info : cameraApps) {
                if (info.activityInfo != null && info.activityInfo.packageName != null) {
                    dynamicExemptPackages.add(info.activityInfo.packageName);
                }
            }

            // Photo picker & Gallery handlers
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            List<ResolveInfo> galleryApps = pm.queryIntentActivities(galleryIntent, 0);
            for (ResolveInfo info : galleryApps) {
                if (info.activityInfo != null && info.activityInfo.packageName != null) {
                    dynamicExemptPackages.add(info.activityInfo.packageName);
                }
            }

            // Document / image file picker handlers
            Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
            getContentIntent.setType("image/*");
            List<ResolveInfo> fileApps = pm.queryIntentActivities(getContentIntent, 0);
            for (ResolveInfo info : fileApps) {
                if (info.activityInfo != null && info.activityInfo.packageName != null) {
                    dynamicExemptPackages.add(info.activityInfo.packageName);
                }
            }

            // Web browser handlers (always hardcoded to be allowed during lockdown)
            try {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"));
                browserIntent.addCategory(Intent.CATEGORY_BROWSABLE);
                List<ResolveInfo> browserApps = pm.queryIntentActivities(browserIntent, 0);
                for (ResolveInfo info : browserApps) {
                    if (info.activityInfo != null && info.activityInfo.packageName != null) {
                        String bPkg = info.activityInfo.packageName;
                        if (!BlacklistConstants.isBlacklisted(bPkg) && !bPkg.contains("webapk")) {
                            dynamicExemptPackages.add(bPkg);
                        }
                    }
                }
            } catch (Exception ignore) {}

            // Music / Audio player category handlers
            try {
                Intent musicIntent = new Intent(Intent.ACTION_MAIN);
                musicIntent.addCategory(Intent.CATEGORY_APP_MUSIC);
                List<ResolveInfo> musicApps = pm.queryIntentActivities(musicIntent, 0);
                for (ResolveInfo info : musicApps) {
                    if (info.activityInfo != null && info.activityInfo.packageName != null) {
                        String mPkg = info.activityInfo.packageName;
                        if (!BlacklistConstants.isBlacklisted(mPkg)) {
                            dynamicExemptPackages.add(mPkg);
                        }
                    }
                }
            } catch (Exception ignore) {}

            // ── Input Method Editors (Keyboards) handlers ──
            try {
                // Query active/default IME from Settings
                String defaultIme = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
                if (defaultIme != null && defaultIme.contains("/")) {
                    String defaultImePkg = defaultIme.split("/")[0];
                    dynamicKeyboardPackages.add(defaultImePkg);
                }

                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    List<InputMethodInfo> imis = imm.getInputMethodList();
                    if (imis != null) {
                        for (InputMethodInfo imi : imis) {
                            if (imi != null && imi.getPackageName() != null) {
                                dynamicKeyboardPackages.add(imi.getPackageName());
                            }
                        }
                    }
                    List<InputMethodInfo> enabledImis = imm.getEnabledInputMethodList();
                    if (enabledImis != null) {
                        for (InputMethodInfo imi : enabledImis) {
                            if (imi != null && imi.getPackageName() != null) {
                                dynamicKeyboardPackages.add(imi.getPackageName());
                            }
                        }
                    }
                }
            } catch (Exception ignore) {}

            // Add authenticators, notes, student apps, and AI assistants to dynamic exemptions
            dynamicExemptPackages.addAll(KNOWN_AUTHENTICATORS);
            dynamicExemptPackages.addAll(KNOWN_NOTES_APPS);
            dynamicExemptPackages.addAll(KNOWN_STUDENT_APPS);
            dynamicExemptPackages.addAll(KNOWN_AI_APPS);

            // Dynamically discover all installed Home Launchers (CATEGORY_HOME)
            try {
                Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                homeIntent.addCategory(Intent.CATEGORY_HOME);
                List<ResolveInfo> homeApps = pm.queryIntentActivities(homeIntent, 0);
                if (homeApps != null) {
                    for (ResolveInfo info : homeApps) {
                        if (info.activityInfo != null && info.activityInfo.packageName != null) {
                            dynamicLauncherPackages.add(info.activityInfo.packageName);
                        }
                    }
                }
            } catch (Exception ignore) {}

            Log.d(TAG, "Discovered dynamic exempt packages: media=" + dynamicExemptPackages.size() + ", keyboards=" + dynamicKeyboardPackages.size() + ", launchers=" + dynamicLauncherPackages.size());
        } catch (Exception e) {
            Log.w(TAG, "Error resolving dynamic media/keyboard packages: " + e.getMessage());
        }
    }

    private boolean isAuthenticatorApp(String pkg) {
        if (pkg == null) return false;
        if (KNOWN_AUTHENTICATORS.contains(pkg)) return true;
        String lower = pkg.toLowerCase();
        return lower.contains("authenticator") || lower.contains("twofas") || lower.contains("duomobile") || lower.contains("yubioath");
    }

    private boolean isNotesApp(String pkg) {
        if (pkg == null) return false;
        if (KNOWN_NOTES_APPS.contains(pkg)) return true;
        String lower = pkg.toLowerCase();
        return lower.contains("keep") || 
               lower.contains("onenote") || 
               lower.contains("obsidian") || 
               lower.contains("notion") || 
               lower.contains("notepad") || 
               lower.contains(".notes") || 
               lower.contains("memo") || 
               lower.contains("simplenote") || 
               lower.contains("colornote") ||
               lower.contains("creation"); // Xiaomi Mi Canvas
    }

    private boolean isStudentApp(String pkg) {
        if (pkg == null) return false;
        if (KNOWN_STUDENT_APPS.contains(pkg)) return true;
        String lower = pkg.toLowerCase(Locale.US);
        // Do not exempt disguised vaults, lockers, or cloners that contain 'calculator' in name
        if (lower.contains("vault") || lower.contains("hide") || lower.contains("secret") || lower.contains("privac") || lower.contains("clone")) {
            return false;
        }
        return lower.contains("classroom") || 
               lower.contains("canvas") || 
               lower.contains("blackboard") || 
               lower.contains("schoology") || 
               lower.contains("quizlet") || 
               lower.contains("anki") || 
               lower.contains("desmos") || 
               lower.contains("geogebra") || 
               lower.contains("calculator") || 
               lower.contains("docs.editors") || 
               (lower.contains("google") && lower.contains("docs")) || 
               lower.contains("photomath") || 
               lower.contains("wolfram") ||
               lower.contains("adobe.reader") ||
               lower.contains("camscanner") ||
               lower.contains("scanner") ||
               lower.contains("translate") ||
               lower.contains("deepl") ||
               lower.contains("duolingo") ||
               lower.contains("khanacademy") ||
               lower.contains("symbolab") ||
               lower.contains("mathway") ||
               lower.contains("chegg") ||
               lower.contains("termux") ||
               lower.contains("pydroid") ||
               lower.contains("skydrive") ||
               lower.contains("dropbox") ||
               lower.contains("readera") ||
               lower.contains("wps");
    }

    private boolean isAiApp(String pkg) {
        if (pkg == null) return false;
        if (KNOWN_AI_APPS.contains(pkg)) return true;
        String lower = pkg.toLowerCase();
        return lower.contains("chatgpt") || 
               lower.contains("bard") || 
               lower.contains("gemini") || 
               lower.contains("claude") || 
               lower.contains("copilot") || 
               lower.contains("perplexity") || 
               lower.contains("deepseek") || 
               lower.contains(".poe");
    }

    private boolean isKeyboardApp(String pkg) {
        if (pkg == null) return false;
        if (KNOWN_KEYBOARDS.contains(pkg) || dynamicKeyboardPackages.contains(pkg)) return true;
        String lower = pkg.toLowerCase();
        if (lower.contains("inputmethod") || 
            lower.contains("honeyboard") || 
            lower.contains("keyboard") || 
            lower.contains("gboard") || 
            lower.contains("swiftkey") || 
            lower.contains(".ime")) {
            return true;
        }
        try {
            String defaultIme = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            if (defaultIme != null && defaultIme.startsWith(pkg + "/")) {
                dynamicKeyboardPackages.add(pkg);
                return true;
            }
        } catch (Exception ignore) {}
        return false;
    }

    private boolean isHiddenInfrastructureApp(String pkg) {
        if (pkg == null) return false;
        String lower = pkg.toLowerCase();
        return lower.contains("cameraextension") ||
               lower.contains("extensionproxy") ||
               lower.contains("lenslauncher") ||
               lower.contains("aperturelenslauncher") ||
               lower.contains("opensourcemusicplayer") ||
               lower.contains("androidopensourcemusicplayer");
    }

    public static boolean isInOperatingHours(Context context, long effectiveNow) {
        if (context != null) {
            SharedPreferences p = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String mode = p.getString("operating_mode", "safemode");
            if ("hardcore".equalsIgnoreCase(mode)) {
                return true;
            }
        }
        return isClockInOperatingHours(effectiveNow);
    }

    public static boolean isInOperatingHours(long effectiveNow) {
        if (instance != null && instance.prefs != null) {
            String mode = instance.prefs.getString("operating_mode", "safemode");
            if ("hardcore".equalsIgnoreCase(mode)) {
                return true;
            }
        }
        return isClockInOperatingHours(effectiveNow);
    }

    private static boolean isClockInOperatingHours(long effectiveNow) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(effectiveNow);
        int hour = cal.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = cal.get(java.util.Calendar.MINUTE);
        int totalMins = hour * 60 + minute;
        // Operating window: 19:00 (1140 mins) to 23:59 (1439 mins) OR 00:00 (0 mins) to 02:59 (179 mins)
        return (totalMins >= 1140 || totalMins < 180);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        if (prefs == null) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }

        // Track foreground app switches
        int eventType = event.getEventType();
        CharSequence pkgChar = event.getPackageName();
        if (pkgChar != null) {
            String pkgStr = pkgChar.toString();
            if ((eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                 eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED ||
                 eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED) && !isSystemOrLauncher(pkgStr)) {
                lastForegroundPackage = pkgStr;
            }
        }

        boolean isLockdownActive = prefs.getBoolean("lockdown_active", false);
        boolean isConsequenceActive = prefs.getBoolean("consequence_active", false);
        long lockEndTime = prefs.getLong("lock_end_time", 0L);
        long timeOffset = prefs.getLong("time_offset", 0L);
        long effectiveNow = System.currentTimeMillis() + timeOffset;

        boolean inOperatingHours = isInOperatingHours(effectiveNow);

        // ── Native Timestamp Handling on Timer Expiration ──
        if (isLockdownActive && !isConsequenceActive && lockEndTime > 0 && effectiveNow >= lockEndTime) {
            isConsequenceActive = true;
            prefs.edit()
                    .putBoolean("consequence_active", true)
                    .remove("lock_end_time")
                    .apply();
            AlarmReceiver.cancelLockEndAlarm(this);
            AlarmReceiver.createNotificationChannels(this);
            boolean isHardcore = "hardcore".equalsIgnoreCase(prefs.getString("operating_mode", "safemode"));
            String notifTitle = isHardcore
                    ? "⚠️ Homework Expired — Consequence Active (Hardcore)"
                    : "⚠️ Homework Expired — Consequence Active";
            String notifMsg = isHardcore
                    ? "Study timer expired without passing. Distracting apps remain restricted 24/7 (Hardcore Mode) until rescheduled and passed."
                    : "Study timer expired without passing. Distracting apps remain restricted during operating hours (7 PM – 3 AM) until rescheduled and passed.";
            AlarmReceiver.showNotificationStatic(
                    this,
                    AlarmReceiver.NOTIF_ID_COMPLETED,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    notifTitle,
                    notifMsg,
                    Notification.PRIORITY_HIGH,
                    false
            );
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(AlarmReceiver.NOTIF_ID_STATUS);
            }
            Log.i(TAG, "Lockdown expired — entered Consequence Mode");
        }

        // ── Consequence Mode Operating Hours Enforcement ──
        if (isConsequenceActive) {
            if (!inOperatingHours) {
                // Safemode outside operating hours (3:00 AM to 7:00 PM):
                // User flowchart: CHECK_HOURS -- "No (Daytime 3am-7pm)" --> ALLOW_IDLE
                return;
            }
            isLockdownActive = true;
        } else if (!isLockdownActive) {
            isLockdownActive = checkAndAutoStartScheduledLock(prefs);
        }

        if (!isLockdownActive) return;

        // ── Anti-Tamper Shield (Gated: only active during lockdown or consequence mode) ──
        if (pkgChar != null) {
            String pkgStr = pkgChar.toString();
            if (!pkgStr.equals(getPackageName())) {
                if (interceptHomeLauncherChangeAttempt(event, null, pkgStr)) {
                    return;
                }
                if (interceptUninstallAttempt(event, pkgStr)) {
                    return;
                }
            }
        }

        // Milestone 20: Immediate evaluation on window stack reordering (such as Recents task switch)
        // In Android, TYPE_WINDOWS_CHANGED events dispatched by WindowManager have event.getPackageName() == null!
        // It MUST be evaluated here before any null package check.
        if (eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            String currentFg = detectCurrentForegroundPackage();
            if (currentFg != null && !currentFg.equals(getPackageName()) && !isSystemOrLauncher(currentFg)) {
                if (isPackageBlocked(currentFg)) {
                    enforceBlock(currentFg);
                    return;
                } else if (isBrowserPackage(currentFg)) {
                    handleBrowserUrlInspection(event, currentFg);
                    return;
                }
            }
        }

        if (pkgChar == null) return;
        String pkg = pkgChar.toString();

        // Never inspect or block QIEZKA itself
        if (pkg.equals(getPackageName())) return;

        // ── SystemUI handling ──
        if (pkg.equals("com.android.systemui")) {
            handleSystemUiEvent(event);
            return;
        }

        // Milestone 19/20: Drop pure background notifications from non-active apps
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            return;
        }

        if (isPackageBlocked(pkg)) {
            // Milestone 20: PWA & Recents Task-Switch Interceptor
            boolean isTransition = (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                                    eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED);
            boolean isInteraction = (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED || 
                                     eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                                     eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED);

            // Direct inspection of event source node visibility on screen:
            boolean isVisibleOnScreen = false;
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                try {
                    isVisibleOnScreen = source.isVisibleToUser();
                } finally {
                    source.recycle();
                }
            }

            String currentForeground = detectCurrentForegroundPackage();
            boolean isForegroundApp = pkg.equals(currentForeground);

            if (isTransition || isInteraction || isVisibleOnScreen || isForegroundApp) {
                enforceBlock(pkg);
            } else {
                Log.d(TAG, "Ignored background event from blocked package: " + pkg + " (event=" + eventType + ")");
            }
            return;
        } else if (isBrowserPackage(pkg)) {
            // Typing Immunity: Ignore keystrokes and text selection events while user is editing in address bars
            if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
                eventType == AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED) {
                return;
            }
            boolean isTransition = (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || 
                                    eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED);
            boolean isInteraction = (eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED || 
                                     eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
                                     eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED);

            boolean isVisibleOnScreen = false;
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                try {
                    isVisibleOnScreen = source.isVisibleToUser();
                } finally {
                    source.recycle();
                }
            }

            String currentForeground = detectCurrentForegroundPackage();
            boolean isForegroundApp = pkg.equals(currentForeground);
            if (isTransition || isInteraction || isVisibleOnScreen || isForegroundApp) {
                handleBrowserUrlInspection(event, pkg);
            }
        }
    }

    private static final long BROWSER_REMEDIATION_COOLDOWN_MS = 2200L;
    private static final long BROWSER_REMEDIATION_TIMEOUT_MS = 6500L;
    private static final long BROWSER_TOAST_COOLDOWN_MS = 3000L;

    private long lastBrowserToastTime = 0L;
    private long lastBrowserRemediationTime = 0L;
    private String lastRemediatedUrl = null;

    private static final Set<String> KNOWN_BROWSER_PACKAGES = new HashSet<>(Arrays.asList(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "com.sec.android.app.sbrowser",
        "com.sec.android.app.sbrowser.beta",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "org.mozilla.fenix",
        "com.brave.browser",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.duckduckgo.mobile.android",
        "com.vivaldi.browser",
        "mark.via.gp"
    ));

    // Web distraction, gaming domains, and academic safe-lists are centralized in WebBlocklistConstants.java

    private boolean isBrowserPackage(String pkg) {
        if (pkg == null) return false;
        if (KNOWN_BROWSER_PACKAGES.contains(pkg)) return true;
        String lower = pkg.toLowerCase(Locale.US);
        return lower.contains("browser") || lower.contains("chrome");
    }

    private boolean isSystemOrLauncher(String pkg) {
        if (pkg == null) return false;
        if (pkg.contains("permissioncontroller") || pkg.contains("packageinstaller")) {
            return false;
        }
        if (pkg.equals("com.android.systemui") || isLauncherApp(this, pkg) || SystemUadAllowlist.isUadSystemAllowed(pkg)) {
            return true;
        }
        // Universal System Partition Gateway for non-browser, non-settings system overlays
        try {
            PackageManager pm = getPackageManager();
            if (pm != null) {
                android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                if ((ai.flags & android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0 ||
                    (ai.flags & android.content.pm.ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    if (!isBrowserPackage(pkg) && !AppClassifier.isSettingsOrDeviceManager(pkg, null)) {
                        return true;
                    }
                }
            }
        } catch (Exception ignore) {}
        return false;
    }

    /**
     * Resolves the title of the active or topmost application window from WindowManager.
     */
    private String resolveActiveWindowTitle(AccessibilityNodeInfo root) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && root != null) {
                AccessibilityWindowInfo win = root.getWindow();
                if (win != null) {
                    CharSequence t = win.getTitle();
                    if (t != null && t.length() > 0) return t.toString();
                }
            }
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null && !windows.isEmpty()) {
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION && (w.isActive() || w.isFocused())) {
                        CharSequence t = w.getTitle();
                        if (t != null && t.length() > 0) return t.toString();
                    }
                }
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        CharSequence t = w.getTitle();
                        if (t != null && t.length() > 0) return t.toString();
                    }
                }
            }
        } catch (Exception ignore) {}
        return null;
    }

    /**
     * Inspects whether an active window or event in a browser package represents a standalone
     * Progressive Web App (PWA) / WebAPK / TWA / CustomTab rather than normal browser tab navigation.
     */
    private boolean isStandalonePwa(String pkg, String eventClass, String windowTitle, AccessibilityNodeInfo root) {
        if (pkg == null) return false;
        // Known general web browsers are NEVER standalone PWAs during standard web browsing
        if (isBrowserPackage(pkg)) {
            if (eventClass != null) {
                String lowerClass = eventClass.toLowerCase(Locale.US);
                if (lowerClass.contains("webappactivity") || 
                    lowerClass.contains("sametaskwebapkactivity") || 
                    lowerClass.contains("webapplauncheractivity")) {
                    return true;
                }
            }
            return false;
        }
        // Non-browser packages running custom tabs or WebAPKs
        if (eventClass != null) {
            String lowerClass = eventClass.toLowerCase(Locale.US);
            if (lowerClass.contains("customtab") || lowerClass.contains("webapk") || lowerClass.contains("webapp")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Inspects a browser window during continuous ticker execution to catch standalone PWAs.
     */
    private void inspectBrowserForBlockedPwaOrContent(AccessibilityNodeInfo root, String pkg) {
        if (root == null || pkg == null) return;
        String windowTitle = resolveActiveWindowTitle(root);
        if (windowTitle != null) {
            if (WebBlocklistConstants.isAcademicExempt(windowTitle)) {
                resetBrowserRemediationState();
                return; // Academic window is unconditionally immune
            }
            if (!isBrowserPackage(pkg) && BlacklistConstants.isBlacklisted("", windowTitle)) {
                Log.w(TAG, "Ticker caught blocked PWA window title: " + windowTitle + " (pkg=" + pkg + ")");
                enforceBlock(pkg);
                return;
            }
        }

        String url = extractUrlFromBrowser(root, pkg);
        boolean allowYoutube = prefs != null && prefs.getBoolean("allow_youtube", false);

        if (url == null) {
            // General browsers with hidden/scrolled address bars or typing in progress
            // are NEVER standalone PWAs. Never scan DOM and never evict!
            if (isBrowserPackage(pkg)) {
                return;
            }
            WebClassifier.ClassificationResult res = WebClassifier.classifyStandalonePwa(windowTitle, root, allowYoutube);
            if (res.isBlocked) {
                Log.w(TAG, "Ticker caught blocked standalone PWA content: " + (windowTitle != null ? windowTitle : "DOM") + " (" + res.reason + ")");
                enforceBlock(pkg);
            }
        } else {
            WebClassifier.ClassificationResult res = WebClassifier.classify(url, root, allowYoutube);
            if (res.isBlocked) {
                Log.w(TAG, "Ticker caught blocked browser URL: " + url + " (" + res.reason + ")");
                remediateBlockedBrowserTab(pkg, url, res.reason, root);
            } else {
                resetBrowserRemediationState();
            }
        }
    }

    private void handleBrowserUrlInspection(AccessibilityEvent event, String pkg) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            String url = extractUrlFromBrowser(root, pkg);
            boolean allowYoutube = prefs != null && prefs.getBoolean("allow_youtube", false);
            String eventClass = event != null && event.getClassName() != null ? event.getClassName().toString() : null;
            String windowTitle = resolveActiveWindowTitle(root);

            if (windowTitle != null && WebBlocklistConstants.isAcademicExempt(windowTitle)) {
                resetBrowserRemediationState();
                return; // Academic window title is unconditionally safe
            }

            if (url == null) {
                // If this is a general browser, url == null indicates that the address bar is hidden,
                // scrolled, being typed into, or animating. Normal browser tab usage must NEVER be evicted.
                if (isBrowserPackage(pkg)) {
                    return;
                }

                // Standalone PWA / WebAPK / TWA / CustomTab evaluation (non-browser packages only)
                if (windowTitle != null && BlacklistConstants.isBlacklisted("", windowTitle)) {
                    Log.w(TAG, "Blocked standalone PWA by window title: " + windowTitle + " (pkg=" + pkg + ")");
                    enforceBlock(pkg);
                    return;
                }

                // Deep semantic classification on PWA window title & DOM
                WebClassifier.ClassificationResult pwaRes = WebClassifier.classifyStandalonePwa(windowTitle, root, allowYoutube);
                if (pwaRes.isBlocked) {
                    Log.w(TAG, "WebClassifier blocked standalone PWA: " + (windowTitle != null ? windowTitle : "DOM") + " (" + pwaRes.reason + ")");
                    enforceBlock(pkg);
                    return;
                }
                return;
            }

            WebClassifier.ClassificationResult result = WebClassifier.classify(url, root, allowYoutube);

            if (result.isBlocked) {
                Log.w(TAG, "WebClassifier blocked browser content: " + (url != null ? url : "DOM Content") + " (" + result.reason + ")");
                remediateBlockedBrowserTab(pkg, url, result.reason, root);
            } else {
                resetBrowserRemediationState();
            }
        } finally {
            root.recycle();
        }
    }

    private int consecutiveBlockedUrlHits = 0;

    private static String extractHost(String rawUrl) {
        if (rawUrl == null) return "";
        String clean = rawUrl.trim().toLowerCase(Locale.US);
        if (clean.startsWith("https://")) clean = clean.substring(8);
        else if (clean.startsWith("http://")) clean = clean.substring(7);
        int slash = clean.indexOf('/');
        if (slash != -1) clean = clean.substring(0, slash);
        int colon = clean.indexOf(':');
        if (colon != -1) clean = clean.substring(0, colon);
        if (clean.startsWith("www.")) clean = clean.substring(4);
        return clean;
    }

    private boolean isSameWebTarget(String url1, String url2) {
        if (url1 == null || url2 == null) return false;
        if (url1.equals(url2)) return true;
        String host1 = extractHost(url1);
        String host2 = extractHost(url2);
        return !host1.isEmpty() && host1.equals(host2);
    }

    private void resetBrowserRemediationState() {
        if (consecutiveBlockedUrlHits > 0 || lastRemediatedUrl != null) {
            consecutiveBlockedUrlHits = 0;
            lastRemediatedUrl = null;
        }
    }

    /**
     * Remediates a blocked web page inside a general web browser without locking the user out indefinitely
     * and without tab accumulation ("blowing up tabs").
     * Strategy (Smart Back with Safety Bailout):
     * 1. Primary (Auto-Back, Hits 1-2): Invoke performGlobalAction(GLOBAL_ACTION_BACK) with a 2.2s cooldown.
     *    If student navigated to the blocked URL from an academic/safe page (e.g. GitHub or Wikipedia) or a chain
     *    of pages, the browser smoothly reverses through history to that safe page in the same tab without stutter.
     * 2. Secondary (Home Button Reset, Hit 3): If the blocked URL persists after 2 back attempts (e.g. fresh tab
     *    with 0 history stack, or JS back-button traps), click the native browser Home button (id/home_button)
     *    to cleanly reset the tab in-place to the New Tab Page without tab accumulation or closing Chrome.
     * 3. Tertiary (Safe Tab Intent Fallback): If accessibility toolbar nodes cannot be found, dispatch explicit Intent
     *    with EXTRA_APPLICATION_ID = getPackageName() ("com.uncode.app") and EXTRA_CREATE_NEW_TAB = false to reuse at most 1 tab.
     */
    private void remediateBlockedBrowserTab(String pkg, String url, String reason, AccessibilityNodeInfo root) {
        long now = System.currentTimeMillis();

        // 1. Debounce: If a remediation action (Auto-Back or Home button) was triggered recently (< 2200ms)
        // for this exact same target origin, allow the browser transition to finish smoothly without interruption.
        if (now - lastBrowserRemediationTime < BROWSER_REMEDIATION_COOLDOWN_MS && isSameWebTarget(url, lastRemediatedUrl)) {
            return;
        }

        // 2. Escalation counter: If the browser is STILL on the same blocked origin after the cooldown window
        // (between 2200ms and 6500ms), advance to next hit. Otherwise reset to 1.
        if (isSameWebTarget(url, lastRemediatedUrl) && (now - lastBrowserRemediationTime < BROWSER_REMEDIATION_TIMEOUT_MS)) {
            consecutiveBlockedUrlHits++;
        } else {
            consecutiveBlockedUrlHits = 1;
        }

        lastBrowserRemediationTime = now;
        lastRemediatedUrl = url;

        Log.w(TAG, "remediateBlockedBrowserTab on " + pkg + " for " + url + " (" + reason + ") hits=" + consecutiveBlockedUrlHits);

        // 1. Display educational Toast notification informing student
        if (now - lastBrowserToastTime > BROWSER_TOAST_COOLDOWN_MS) {
            lastBrowserToastTime = now;
            final String finalReason = reason != null ? reason : "Distracting website";
            new Handler(Looper.getMainLooper()).post(() -> {
                Toast.makeText(getApplicationContext(), "⚠️ " + finalReason + ". Returning to safe page.", Toast.LENGTH_SHORT).show();
            });
        }

        // 2. Primary Remediation: Auto-Back (Hits 1 and 2)
        // Reverses browser history to the prior safe page (e.g. GitHub, Wikipedia, Google, New Tab Page).
        // With 2200ms cooldown, Chrome has sufficient time to complete the back navigation without stutter.
        if (consecutiveBlockedUrlHits <= 2) {
            boolean backed = performGlobalAction(GLOBAL_ACTION_BACK);
            Log.i(TAG, "remediateBlockedBrowserTab: executed auto-back (hit " + consecutiveBlockedUrlHits + ") -> " + backed);
            return;
        }

        // 3. Secondary Remediation: Repeat hit after multiple backs means no back history in this tab or JS trap — reset tab in-place via Home button
        Log.i(TAG, "remediateBlockedBrowserTab: repeat hit detected after auto-back, resetting tab in-place via home button");
        boolean homeClicked = clickBrowserHomeButton(root, pkg);
        if (homeClicked) {
            Log.i(TAG, "remediateBlockedBrowserTab: active tab neutralized in-place via home button (0 new tabs created)");
            return;
        }

        // 4. Tertiary Remediation: Reusable safe tab via Intent
        Log.w(TAG, "remediateBlockedBrowserTab: home button unavailable, falling back to reused safe tab intent");
        navigateBrowserToSafeBlank(pkg);
        consecutiveBlockedUrlHits = 0;
    }

    private boolean clickBrowserHomeButton(AccessibilityNodeInfo passedRoot, String pkg) {
        AccessibilityNodeInfo root = passedRoot;
        boolean shouldRecycleRoot = false;
        if (root == null) {
            try {
                root = getRootInActiveWindow();
                shouldRecycleRoot = true;
            } catch (Exception ignore) {}
        }
        if (root == null) return false;

        try {
            String[] homeButtonIds = {
                "com.android.chrome:id/home_button",
                "com.sec.android.app.sbrowser:id/location_bar_home_button",
                "org.chromium.chrome:id/home_button",
                "com.microsoft.emmx:id/home_button"
            };

            for (String homeId : homeButtonIds) {
                List<AccessibilityNodeInfo> homeNodes = root.findAccessibilityNodeInfosByViewId(homeId);
                if (homeNodes != null && !homeNodes.isEmpty()) {
                    for (AccessibilityNodeInfo homeNode : homeNodes) {
                        if (homeNode != null) {
                            try {
                                if (homeNode.isClickable()) {
                                    boolean clicked = homeNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                                    Log.i(TAG, "clickBrowserHomeButton: ACTION_CLICK on " + homeId + " -> " + clicked);
                                    if (clicked) {
                                        return true;
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Failed home button click on " + homeId + ": " + e.getMessage());
                            } finally {
                                homeNode.recycle();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "clickBrowserHomeButton error: " + e.getMessage());
        } finally {
            if (shouldRecycleRoot && root != null) {
                root.recycle();
            }
        }
        return false;
    }

    private void navigateBrowserToSafeBlank(String pkg) {
        try {
            Intent safeIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("about:blank"));
            safeIntent.setPackage(pkg);
            // CRITICAL: DO NOT pass 'pkg' ("com.android.chrome") as EXTRA_APPLICATION_ID!
            // Passing Chrome's own package name triggers Chromium's DONT_CLOBBER_TABS_WITH_CHROME_APP_ID,
            // which returns TabOpenType.OPEN_NEW_TAB on every call and blows up open tabs.
            // Instead, passing getPackageName() ("com.uncode.app") triggers TabOpenType.REUSE_APP_ID_MATCHING_TAB_ELSE_NEW_TAB,
            // ensuring Chrome reuses at most ONE single safety tab rather than accumulating tabs.
            safeIntent.putExtra(Browser.EXTRA_APPLICATION_ID, getPackageName());
            safeIntent.putExtra("com.android.browser.application_id", getPackageName());
            safeIntent.putExtra(Browser.EXTRA_CREATE_NEW_TAB, false);
            safeIntent.putExtra("create_new_tab", false);
            safeIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(safeIntent);
        } catch (Exception e) {
            Log.e(TAG, "Failed to navigate browser to safe tab: " + e.getMessage());
        }
    }

    private String extractUrlFromBrowser(AccessibilityNodeInfo root, String pkg) {
        if (root == null) return null;

        // Layer 1: Input Focus Guard. If any address bar or search input is actively focused by the user,
        // typing or autocomplete is in progress. Eviction must NEVER occur during active typing.
        AccessibilityNodeInfo focusNode = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT);
        if (focusNode != null) {
            try {
                if (focusNode.isFocused() || focusNode.isEditable()) {
                    String focusId = focusNode.getViewIdResourceName();
                    if (focusId != null) {
                        String lowerFocusId = focusId.toLowerCase(Locale.US);
                        if (lowerFocusId.contains("url") || lowerFocusId.contains("location") ||
                            lowerFocusId.contains("search") || lowerFocusId.contains("toolbar") ||
                            lowerFocusId.contains("address") || lowerFocusId.contains("omnibox") ||
                            lowerFocusId.contains("line_1")) {
                            return null; // User is actively typing in the address bar
                        }
                    }
                    CharSequence focusClass = focusNode.getClassName();
                    if (focusClass != null && focusClass.toString().toLowerCase(Locale.US).contains("edittext")) {
                        return null; // Active input focus in an editable text field
                    }
                }
            } finally {
                focusNode.recycle();
            }
        }

        // 1. Chrome / Chromium family: Query multiple known address bar view IDs
        String[] chromeViewIds = {
            "com.android.chrome:id/url_bar",
            "com.android.chrome:id/search_box_text",
            "com.android.chrome:id/location_bar",
            "com.android.chrome:id/toolbar",
            "com.android.chrome:id/omnibox_text_field",
            "org.chromium.chrome:id/url_bar"
        };
        for (String id : chromeViewIds) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node != null) {
                        if (node.isFocused()) {
                            return null; // Active typing or autocomplete in progress
                        }
                        CharSequence text = node.getText();
                        if (text != null && text.length() > 0) return text.toString();
                        CharSequence desc = node.getContentDescription();
                        if (desc != null && desc.length() > 0) return desc.toString();
                    }
                }
            }
        }

        // 2. Samsung Internet
        String[] sbrowserViewIds = {
            "com.sec.android.app.sbrowser:id/location_bar_edit_text",
            "com.sec.android.app.sbrowser:id/url_bar",
            "com.sec.android.app.sbrowser:id/location_bar"
        };
        for (String id : sbrowserViewIds) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node != null) {
                        if (node.isFocused()) {
                            return null; // Active typing or autocomplete in progress
                        }
                        CharSequence text = node.getText();
                        if (text != null && text.length() > 0) return text.toString();
                        CharSequence desc = node.getContentDescription();
                        if (desc != null && desc.length() > 0) return desc.toString();
                    }
                }
            }
        }

        // 3. Firefox
        String[] firefoxViewIds = {
            "org.mozilla.firefox:id/url_bar_title",
            "org.mozilla.firefox:id/toolbar",
            "org.mozilla.firefox:id/mozac_browser_toolbar_url_view"
        };
        for (String id : firefoxViewIds) {
            List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(id);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node != null) {
                        if (node.isFocused()) {
                            return null; // Active typing or autocomplete in progress
                        }
                        CharSequence text = node.getText();
                        if (text != null && text.length() > 0) return text.toString();
                    }
                }
            }
        }

        // 4. Edge
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId("com.microsoft.emmx:id/url_bar");
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null) {
                    if (node.isFocused()) {
                        return null; // Active typing or autocomplete in progress
                    }
                    CharSequence text = node.getText();
                    if (text != null && text.length() > 0) return text.toString();
                    CharSequence desc = node.getContentDescription();
                    if (desc != null && desc.length() > 0) return desc.toString();
                }
            }
        }

        // 5. Brave
        nodes = root.findAccessibilityNodeInfosByViewId("com.brave.browser:id/url_bar");
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null) {
                    if (node.isFocused()) {
                        return null; // Active typing or autocomplete in progress
                    }
                    CharSequence text = node.getText();
                    if (text != null && text.length() > 0) return text.toString();
                    CharSequence desc = node.getContentDescription();
                    if (desc != null && desc.length() > 0) return desc.toString();
                }
            }
        }

        // 6. Generic Heuristic Fallback through hierarchy (Targeting address bar & input nodes only)
        return findUrlInHierarchy(root, 0);
    }

    private String findUrlInHierarchy(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 8) return null;

        String resId = node.getViewIdResourceName();
        boolean isAddressOrInput = false;
        if (resId != null) {
            String lowerResId = resId.toLowerCase(Locale.US);
            if (lowerResId.contains("url_bar") || lowerResId.contains("location_bar") || 
                lowerResId.contains("address_bar") || lowerResId.contains("omnibar") ||
                lowerResId.contains("url_field")) {
                isAddressOrInput = true;
            }
        }

        // Native address bars must have a valid identifier.
        // We explicitly do NOT match anonymous EditText or editable nodes without a toolbar/url resource ID,
        // because those represent in-page HTML form fields (such as Wikipedia search, login forms, etc.).
        if (isAddressOrInput) {
            if (node.isFocused()) {
                return null; // Active typing or autocomplete in progress
            }
            CharSequence text = node.getText();
            if (text != null && text.length() > 0) {
                String t = text.toString().trim();
                if (t.contains(".") || t.startsWith("http://") || t.startsWith("https://") || t.contains("/")) {
                    return t;
                }
            }
            CharSequence desc = node.getContentDescription();
            if (desc != null && desc.length() > 0) {
                String d = desc.toString().trim();
                if (d.contains(".") || d.startsWith("http://") || d.startsWith("https://") || d.contains("/")) {
                    return d;
                }
            }
        }

        // Do not descend into web page render views (WebView / WebContents) looking for address bars.
        // The address bar is native browser chrome and will never be inside web content.
        CharSequence className = node.getClassName();
        if (className != null) {
            String lowerClass = className.toString().toLowerCase(Locale.US);
            if (lowerClass.contains("webview") || lowerClass.contains("render") || lowerClass.contains("contentview")) {
                return null;
            }
        }

        int childCount = node.getChildCount();
        for (int i = 0; i < childCount; i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                String found = findUrlInHierarchy(child, depth + 1);
                if (found != null) return found;
            }
        }
        return null;
    }

    public boolean isPackageBlocked(String pkg) {
        if (pkg == null) return false;
        if (pkg.equals(getPackageName())) return false;
        if (ALWAYS_EXEMPT.contains(pkg)) return false;

        // Package installers & App store updates are explicitly allowed
        if (isInstallerOrStoreApp(pkg)) return false;

        // Query app label for fast-path blacklist matching
        String appLabel = null;
        try {
            PackageManager pm = getPackageManager();
            if (pm != null) {
                android.content.pm.ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
                CharSequence lbl = pm.getApplicationLabel(ai);
                if (lbl != null) appLabel = lbl.toString();
            }
        } catch (Exception ignore) {}

        // SIM Toolkit (STK), MMS, Carrier notifications (Globe, Smart, DITO, etc.) are always allowed
        if (isSimOrCarrierService(pkg, appLabel)) return false;

        // Standalone Universal Android Debloater (UAD-NG) System Allowlist
        // (Emergency services, AOSP Screenshot Markup, IntentResolver/Chooser, Captive Portal, etc.)
        if (SystemUadAllowlist.isUadSystemAllowed(pkg, appLabel)) return false;

        // STAGE 1 — MASTER VETO GATE: Anti-Tamper Shield (Android Settings, MIUI Security, OEM Phone Managers)
        if (AppClassifier.isSettingsOrDeviceManager(pkg, appLabel)) return true;

        // STAGE 1 — MASTER VETO GATE: Hardware Bloatware & Game Boosters (Joyose, GameCenter, PalmStore, Glance)
        if (AppClassifier.isStage1Bloat(pkg, appLabel)) return true;

        if (isKeyboardApp(pkg)) return false;
        if (isAuthenticatorApp(pkg)) return false;
        if (isNotesApp(pkg)) return false;
        if (isAiApp(pkg)) return false;
        if (isStudentApp(pkg)) return false;
        if (isHiddenInfrastructureApp(pkg)) return false;
        if (isLauncherApp(this, pkg)) return false;
        if (pkg.contains("documentsui")) return false;
        if (MEDIA_AND_FILE_EXEMPT.contains(pkg) || dynamicExemptPackages.contains(pkg) || KNOWN_MUSIC_APPS.contains(pkg)) return false;

        Set<String> whitelist = prefs.getStringSet("whitelist", new HashSet<>());

        // ── STAGE 2: On-Device Local App Classifier (Metadata, Categories & Unified Whitelist) ──
        return AppClassifier.isPackageBlocked(this, pkg, whitelist);
    }

    private void onTickerTick() {
        if (prefs == null) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }

        long timeOffset = prefs.getLong("time_offset", 0L);
        long effectiveNow = System.currentTimeMillis() + timeOffset;
        boolean inOperatingHours = isInOperatingHours(effectiveNow);

        boolean isLockdownActive = prefs.getBoolean("lockdown_active", false);
        boolean isConsequenceActive = prefs.getBoolean("consequence_active", false);
        long lockEndTime = prefs.getLong("lock_end_time", 0L);

        // 1. Check if lockdown timer has expired
        if (isLockdownActive && !isConsequenceActive && lockEndTime > 0 && effectiveNow >= lockEndTime) {
            Log.i(TAG, "Ticker: lockdown timer expired without passing (effectiveNow=" + effectiveNow + " >= " + lockEndTime + ") -> consequence active");
            prefs.edit()
                    .putBoolean("consequence_active", true)
                    .remove("lock_end_time")
                    .apply();
            AlarmReceiver.cancelLockEndAlarm(this);
            AlarmReceiver.createNotificationChannels(this);
            boolean isHardcore = "hardcore".equalsIgnoreCase(prefs.getString("operating_mode", "safemode"));
            String notifTitle = isHardcore
                    ? "⚠️ Homework Expired — Consequence Active (Hardcore)"
                    : "⚠️ Homework Expired — Consequence Active";
            String notifMsg = isHardcore
                    ? "Study timer expired without passing. Distracting apps remain restricted 24/7 (Hardcore Mode) until rescheduled and passed."
                    : "Study timer expired without passing. Distracting apps remain restricted during operating hours (7 PM – 3 AM) until rescheduled and passed.";
            AlarmReceiver.showNotificationStatic(
                    this,
                    AlarmReceiver.NOTIF_ID_COMPLETED,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    notifTitle,
                    notifMsg,
                    Notification.PRIORITY_HIGH,
                    false
            );
            isConsequenceActive = true;
        }

        // 1b. Check active lockdown countdown warnings
        if (isLockdownActive && lockEndTime > 0) {
            long remainingSec = (lockEndTime - effectiveNow) / 1000L;
            if (remainingSec > 0) {
                checkAndFireLockdownTimerWarning(remainingSec);
            }
        }

        // 1c. Synchronize LocalDnsVpnService with active lockdown and consequence penalty
        String webMode = prefs.getString("web_protection_mode", "accessibility");
        boolean shouldVpnRun = (isLockdownActive || isConsequenceActive)
                && ("dns_vpn".equalsIgnoreCase(webMode) || "dual_hybrid".equalsIgnoreCase(webMode));

        if (shouldVpnRun && !LocalDnsVpnService.isRunning) {
            LocalDnsVpnService.startVpn(this);
        } else if (!shouldVpnRun && LocalDnsVpnService.isRunning) {
            LocalDnsVpnService.stopVpn(this);
        }

        // 2. Check schedules from schedules_json
        String schedulesJson = prefs.getString("schedules_json", null);
        if (schedulesJson != null && !schedulesJson.trim().isEmpty()) {
            try {
                JSONArray arr = new JSONArray(schedulesJson);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject s = arr.getJSONObject(i);
                    if (!s.optBoolean("isActive", true)) continue;

                    String timeStr = s.optString("activationTime", "");
                    if (!timeStr.contains(":")) continue;

                    String[] parts = timeStr.split(":");
                    int schedHour = Integer.parseInt(parts[0].trim());
                    int schedMin = Integer.parseInt(parts[1].trim());
                    int durationMinutes = s.optInt("durationMinutes", 25);
                    long durationMs = durationMinutes * 60L * 1000L;

                    Calendar schedCal = Calendar.getInstance();
                    schedCal.setTimeInMillis(effectiveNow);
                    schedCal.set(Calendar.HOUR_OF_DAY, schedHour);
                    schedCal.set(Calendar.MINUTE, schedMin);
                    schedCal.set(Calendar.SECOND, 0);
                    schedCal.set(Calendar.MILLISECOND, 0);

                    long schedStartTime = schedCal.getTimeInMillis();
                    long schedEndTime = schedStartTime + durationMs;

                    // A) Check if schedule should start RIGHT NOW (today's window or midnight cross)
                    boolean inWindow = false;
                    long windowEnd = 0;
                    if (effectiveNow >= schedStartTime && effectiveNow < schedEndTime) {
                        inWindow = true;
                        windowEnd = schedEndTime;
                    } else {
                        long yesterdayStart = schedStartTime - (24L * 3600L * 1000L);
                        long yesterdayEnd = yesterdayStart + durationMs;
                        if (effectiveNow >= yesterdayStart && effectiveNow < yesterdayEnd) {
                            inWindow = true;
                            windowEnd = yesterdayEnd;
                        }
                    }

                    if (inWindow && !isLockdownActive) {
                        long lastCompletedEnd = prefs.getLong("last_completed_window_end_" + s.optString("id", ""), 0L);
                        if (windowEnd <= lastCompletedEnd) {
                            // This schedule session was already completed or ended! Do NOT restart lockdown!
                            continue;
                        }

                        Log.i(TAG, "Ticker: schedule " + s.optString("id") + " starting NOW! (windowEnd=" + windowEnd + ")");
                        prefs.edit()
                                .putBoolean("lockdown_active", true)
                                .putLong("lock_end_time", windowEnd)
                                .putString("active_schedule_id", s.optString("id", ""))
                                .apply();
                        AlarmReceiver.scheduleLockEndAlarm(this, windowEnd, s.optString("id", ""));
                        AlarmReceiver.showNotificationStatic(
                                this,
                                AlarmReceiver.NOTIF_ID_STATUS,
                                AlarmReceiver.CHANNEL_ID_STATUS,
                                "🔒 QIEZKA Lockdown Engaged",
                                s.optString("title", "Study Session") + " is active (" + durationMinutes + " min). Distracting apps are blocked.",
                                Notification.PRIORITY_HIGH,
                                true
                        );
                        isLockdownActive = true;
                        FloatingOverlayService.startService(this, windowEnd, s.optString("title", "Study Session"));

                        // Immediately kick out if user is currently inside a blocked app
                        String activePkg = detectCurrentForegroundPackage();
                        if (activePkg != null && isPackageBlocked(activePkg)) {
                            enforceBlock(activePkg);
                        }
                    }

                    // B) Check advance countdown warnings before schedule start
                    long diffSec = (schedStartTime - effectiveNow) / 1000L;
                    if (diffSec > 0 && diffSec <= 1800) {
                        checkAndFireTickerWarning(s.optString("id", ""), diffSec);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Ticker schedule check error: " + e.getMessage());
            }
        }

        // 3. Stage 1 & Stage 2 Enforcement Gate
        // User Flowchart: Standby / System Idle when neither lockdown nor consequence is active
        boolean isEnforcing = isLockdownActive || (isConsequenceActive && inOperatingHours);
        if (!isEnforcing) {
            return; // ALLOW ALL (System Idle)
        }

        // ── STAGE 1: Anti-Tamper & Task Killer Shield (Active Lockdown / Consequence Only) ──
        AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
        if (activeRoot != null) {
            try {
                CharSequence p = activeRoot.getPackageName();
                if (p != null) {
                    String rootPkg = p.toString();
                    if (!rootPkg.equals(getPackageName())) {
                        if (interceptHomeLauncherChangeAttempt(null, activeRoot, rootPkg)) {
                            return;
                        }
                        if (interceptUninstallAttempt(null, rootPkg)) {
                            return;
                        }
                        if (!isSystemOrLauncher(rootPkg)) {
                            if (isPackageBlocked(rootPkg)) {
                                enforceBlock(rootPkg);
                                return;
                            } else if (isBrowserPackage(rootPkg)) {
                                inspectBrowserForBlockedPwaOrContent(activeRoot, rootPkg);
                            }
                        }
                    }
                }
            } finally {
                activeRoot.recycle();
            }
        }

        String currentForegroundPkg = detectCurrentForegroundPackage();
        if (currentForegroundPkg != null && !isSystemOrLauncher(currentForegroundPkg) && !currentForegroundPkg.equals(getPackageName())) {
            if (isPackageBlocked(currentForegroundPkg)) {
                enforceBlock(currentForegroundPkg);
            } else if (isBrowserPackage(currentForegroundPkg)) {
                String winTitle = resolveActiveWindowTitle(null);
                if (winTitle != null && BlacklistConstants.isBlacklisted("", winTitle)) {
                    Log.w(TAG, "Ticker caught blocked PWA by window title: " + winTitle);
                    enforceBlock(currentForegroundPkg);
                }
            }
        }
    }

    private void checkAndFireTickerWarning(String schedId, long diffSec) {
        String warnKey = null;
        String warnText = null;

        if (diffSec <= 1800 && diffSec >= 1795) {
            warnKey = schedId + "_30m";
            warnText = "⏰ Upcoming study session in 30 minutes! Wrap up your tasks.";
        } else if (diffSec <= 900 && diffSec >= 895) {
            warnKey = schedId + "_15m";
            warnText = "⏰ Study session starts in 15 minutes! Prepare your materials.";
        } else if (diffSec <= 300 && diffSec >= 295) {
            warnKey = schedId + "_5m";
            warnText = "⚠️ Lockdown in 5 minutes! Save your work on other apps.";
        } else if (diffSec <= 60 && diffSec >= 55) {
            warnKey = schedId + "_1m";
            warnText = "⚠️ Lockdown in 1 minute! QIEZKA is preparing to lock.";
        } else if (diffSec <= 30 && diffSec >= 25) {
            warnKey = schedId + "_30s";
            warnText = "🚨 Lockdown starting in 30 seconds! QIEZKA is engaging.";
        } else if (diffSec <= 10 && diffSec >= 5) {
            warnKey = schedId + "_10s";
            warnText = "🚨 Lockdown starting in 10 seconds!";
        }

        if (warnKey != null && !firedWarningKeys.contains(warnKey)) {
            firedWarningKeys.add(warnKey);
            Log.i(TAG, "Firing ticker advance warning: " + warnText);
            AlarmReceiver.showNotificationStatic(
                    this,
                    AlarmReceiver.NOTIF_ID_WARNING,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    "⏰ QIEZKA Upcoming Lockdown",
                    warnText,
                    Notification.PRIORITY_HIGH,
                    false
            );
        }
    }

    private void checkAndFireLockdownTimerWarning(long remSec) {
        String warnKey = null;
        String warnText = null;

        if (remSec <= 1800 && remSec >= 1795) {
            warnKey = "lock_timer_30m";
            warnText = "⏳ 30 minutes remaining in active lockdown.";
        } else if (remSec <= 900 && remSec >= 895) {
            warnKey = "lock_timer_15m";
            warnText = "⏳ 15 minutes remaining in active lockdown.";
        } else if (remSec <= 300 && remSec >= 295) {
            warnKey = "lock_timer_5m";
            warnText = "⏳ 5 minutes remaining! Finish your homework submission.";
        } else if (remSec <= 60 && remSec >= 55) {
            warnKey = "lock_timer_1m";
            warnText = "⏳ 1 minute remaining before lockdown auto-unlocks.";
        } else if (remSec <= 30 && remSec >= 25) {
            warnKey = "lock_timer_30s";
            warnText = "⏳ 30 seconds remaining!";
        }

        if (warnKey != null && !firedWarningKeys.contains(warnKey)) {
            firedWarningKeys.add(warnKey);
            Log.i(TAG, "Firing lockdown timer warning: " + warnText);
            AlarmReceiver.showNotificationStatic(
                    this,
                    AlarmReceiver.NOTIF_ID_WARNING,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    "⏳ QIEZKA Lock Timer",
                    warnText,
                    Notification.PRIORITY_HIGH,
                    false
            );
        }
    }

    private long lastBlockTimestamp = 0L;

    public void enforceBlock(String pkg) {
        long now = System.currentTimeMillis();
        if (now - lastBlockTimestamp < 350L && pkg != null && pkg.equals(lastBlockedPackage)) {
            return; // Debounce rapid accessibility events from the same launch attempt
        }
        lastBlockTimestamp = now;
        lastBlockedPackage = pkg;

        Log.w(TAG, "enforceBlock on distracting app: " + pkg + " — directly bringing QIEZKA lock to front (no home redirection)");
        
        // Directly bring QIEZKA lock screen to front immediately
        launchLockOverlay();

        tickerHandler.postDelayed(() -> {
            String fg = detectCurrentForegroundPackage();
            if (fg != null && !fg.equals(getPackageName()) && !isSystemOrLauncher(fg)) {
                if (isPackageBlocked(fg)) {
                    launchLockOverlay();
                }
            }
        }, 200L);
    }

    public void onScheduleStartTriggered() {
        try {
            String activePkg = detectCurrentForegroundPackage();
            if (activePkg != null && !activePkg.equals(getPackageName()) && isPackageBlocked(activePkg)) {
                enforceBlock(activePkg);
            }
        } catch (Exception e) {
            Log.e(TAG, "onScheduleStartTriggered error: " + e.getMessage());
        }
    }

    public String detectCurrentForegroundPackage() {
        // 1. Z-Order Window Audit: Check if any active or visible application window belongs to a blocked package.
        // This defeats TWA/CustomTab host wrapping (e.g. id.kisskh.twa wrapped in com.android.chrome),
        // split-screen multi-window bypasses, and floating windows.
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null && !windows.isEmpty()) {
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        AccessibilityNodeInfo root = w.getRoot();
                        if (root != null) {
                            try {
                                CharSequence p = root.getPackageName();
                                if (p != null) {
                                    String pkg = p.toString();
                                    if (!isSystemOrLauncher(pkg) && isPackageBlocked(pkg)) {
                                        lastForegroundPackage = pkg;
                                        return pkg;
                                    }
                                }
                            } finally {
                                root.recycle();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {}

        // 2. Primary: Inspect active root window directly
        try {
            AccessibilityNodeInfo activeRoot = getRootInActiveWindow();
            if (activeRoot != null) {
                CharSequence p = activeRoot.getPackageName();
                activeRoot.recycle();
                if (p != null) {
                    String rootPkg = p.toString();
                    if (!isSystemOrLauncher(rootPkg)) {
                        lastForegroundPackage = rootPkg;
                        return rootPkg;
                    }
                }
            }
        } catch (Exception ignore) {}

        // 3. Secondary: Inspect interactive application windows in Z-order
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null && !windows.isEmpty()) {
                // First pass: Active or Focused application window
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION && (w.isActive() || w.isFocused())) {
                        AccessibilityNodeInfo root = w.getRoot();
                        if (root != null) {
                            try {
                                CharSequence p = root.getPackageName();
                                if (p != null) {
                                    String pkg = p.toString();
                                    if (!isSystemOrLauncher(pkg)) {
                                        lastForegroundPackage = pkg;
                                        return pkg;
                                    }
                                }
                            } finally {
                                root.recycle();
                            }
                        }
                    }
                }
                // Second pass: Top visible application window
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        AccessibilityNodeInfo root = w.getRoot();
                        if (root != null) {
                            try {
                                CharSequence p = root.getPackageName();
                                if (p != null) {
                                    String pkg = p.toString();
                                    if (!isSystemOrLauncher(pkg)) {
                                        lastForegroundPackage = pkg;
                                        return pkg;
                                    }
                                }
                            } finally {
                                root.recycle();
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {}

        // 4. Fallback: last recorded non-system/non-launcher foreground package from events
        if (lastForegroundPackage != null && !isSystemOrLauncher(lastForegroundPackage)) {
            return lastForegroundPackage;
        }

        return lastForegroundPackage;
    }

    private void launchLockOverlay() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                boolean isConsequence = prefs != null && prefs.getBoolean("consequence_active", false);
                launch.putExtra("route", isConsequence ? "logs" : "locked");
                startActivity(launch);
            }
        } catch (Exception e) {
            Log.e(TAG, "launchLockOverlay failed: " + e.getMessage());
        }
    }


    /**
     * Handles SystemUI events.
     * - Allows the notification shade to be pulled down.
     * - Collapses the Quick Settings (QS) tile panel immediately when expanded.
     */
    private void handleSystemUiEvent(AccessibilityEvent event) {
        CharSequence classNameChar = event.getClassName();
        if (classNameChar == null) return;
        String cls = classNameChar.toString().toLowerCase();

        // Collapse QS tile panel (but allow the plain notification shade)
        if (cls.contains("qs")
                || cls.contains("quicksetting")
                || cls.contains("brightnesscontroller")
                || cls.contains("tilerecord")
                || cls.contains("quickstatusbar")) {
            collapseQsPanel();
        }
    }

    private void collapseQsPanel() {
        try {
            Object statusBarService = getSystemService("statusbar");
            if (statusBarService != null) {
                Method collapseMethod = statusBarService.getClass().getMethod("collapsePanels");
                collapseMethod.setAccessible(true);
                collapseMethod.invoke(statusBarService);
                return;
            }
        } catch (Exception e) {
            Log.w(TAG, "StatusBarManager reflection failed: " + e.getMessage());
        }
        // Fallback
        try {
            performGlobalAction(GLOBAL_ACTION_BACK);
        } catch (Exception e) {
            Log.e(TAG, "Fallback collapse failed: " + e.getMessage());
        }
    }

    private boolean checkAndAutoStartScheduledLock(SharedPreferences prefs) {
        String schedulesJson = prefs.getString("schedules_json", null);
        if (schedulesJson == null || schedulesJson.trim().isEmpty()) return false;

        try {
            JSONArray arr = new JSONArray(schedulesJson);
            long timeOffset = prefs.getLong("time_offset", 0L);
            long effectiveNow = System.currentTimeMillis() + timeOffset;

            for (int i = 0; i < arr.length(); i++) {
                JSONObject s = arr.getJSONObject(i);
                if (!s.optBoolean("isActive", true)) continue;

                String timeStr = s.optString("activationTime", "");
                if (!timeStr.contains(":")) continue;

                String[] parts = timeStr.split(":");
                int schedHour = Integer.parseInt(parts[0].trim());
                int schedMin = Integer.parseInt(parts[1].trim());
                int durationMinutes = s.optInt("durationMinutes", 25);
                long durationMs = durationMinutes * 60L * 1000L;

                Calendar schedCal = Calendar.getInstance();
                schedCal.setTimeInMillis(effectiveNow);
                schedCal.set(Calendar.HOUR_OF_DAY, schedHour);
                schedCal.set(Calendar.MINUTE, schedMin);
                schedCal.set(Calendar.SECOND, 0);
                schedCal.set(Calendar.MILLISECOND, 0);

                long schedStartTime = schedCal.getTimeInMillis();
                long schedEndTime = schedStartTime + durationMs;

                // Check today's window
                if (effectiveNow >= schedStartTime && effectiveNow < schedEndTime) {
                    long lastCompletedEnd = prefs.getLong("last_completed_window_end_" + s.optString("id", ""), 0L);
                    if (schedEndTime <= lastCompletedEnd) {
                        continue;
                    }
                    prefs.edit()
                            .putBoolean("lockdown_active", true)
                            .putLong("lock_end_time", schedEndTime)
                            .putString("active_schedule_id", s.optString("id", ""))
                            .apply();
                    AlarmReceiver.scheduleLockEndAlarm(this, schedEndTime, s.optString("id", ""));
                    AlarmReceiver.createNotificationChannels(this);
                    AlarmReceiver.showNotificationStatic(
                            this,
                            AlarmReceiver.NOTIF_ID_STATUS,
                            AlarmReceiver.CHANNEL_ID_STATUS,
                            "🔒 QIEZKA Lockdown Engaged",
                            s.optString("title", "Study Session") + " is active (" + durationMinutes + " min). Distracting apps are blocked.",
                            Notification.PRIORITY_HIGH,
                            true
                    );
                    FloatingOverlayService.startService(this, schedEndTime, s.optString("title", "Study Session"));
                    Log.i(TAG, "Native auto-start triggered for schedule: " + s.optString("id", "") + ", lockEnd=" + schedEndTime);
                    return true;
                }

                // Check yesterday's window (crossed midnight)
                long yesterdayStart = schedStartTime - (24L * 3600L * 1000L);
                long yesterdayEnd = yesterdayStart + durationMs;
                if (effectiveNow >= yesterdayStart && effectiveNow < yesterdayEnd) {
                    long lastCompletedEnd = prefs.getLong("last_completed_window_end_" + s.optString("id", ""), 0L);
                    if (yesterdayEnd <= lastCompletedEnd) {
                        continue;
                    }
                    prefs.edit()
                            .putBoolean("lockdown_active", true)
                            .putLong("lock_end_time", yesterdayEnd)
                            .putString("active_schedule_id", s.optString("id", ""))
                            .apply();
                    AlarmReceiver.scheduleLockEndAlarm(this, yesterdayEnd, s.optString("id", ""));
                    AlarmReceiver.createNotificationChannels(this);
                    AlarmReceiver.showNotificationStatic(
                            this,
                            AlarmReceiver.NOTIF_ID_STATUS,
                            AlarmReceiver.CHANNEL_ID_STATUS,
                            "🔒 QIEZKA Lockdown Engaged",
                            s.optString("title", "Study Session") + " is active (" + durationMinutes + " min). Distracting apps are blocked.",
                            Notification.PRIORITY_HIGH,
                            true
                    );
                    FloatingOverlayService.startService(this, yesterdayEnd, s.optString("title", "Study Session"));
                    Log.i(TAG, "Native auto-start (midnight-cross) triggered for schedule: " + s.optString("id", ""));
                    return true;
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "checkAndAutoStartScheduledLock error: " + e.getMessage());
        }
        return false;
    }

    /**
     * Intercepts uninstallation popups and dialogs targeting QIEZKA,
     * providing a secondary anti-tamper layer when Device Admin privileges are not granted.
     */
    private boolean interceptUninstallAttempt(AccessibilityEvent event, String pkg) {
        if (pkg == null) return false;
        String lowerPkg = pkg.toLowerCase(Locale.US);

        // Check if package is a package installer, settings, or home launcher
        boolean isPotentialUninstallHost = lowerPkg.contains("packageinstaller") || 
                                           lowerPkg.contains("settings") ||
                                           KNOWN_LAUNCHERS.contains(pkg);

        if (!isPotentialUninstallHost) return false;

        // Check event class name
        String classStr = "";
        if (event != null && event.getClassName() != null) {
            classStr = event.getClassName().toString().toLowerCase(Locale.US);
        }
        boolean isUninstallClass = classStr.contains("uninstall") || classStr.contains("uninstaller");

        AccessibilityNodeInfo root = null;
        try {
            root = getRootInActiveWindow();
            if (root == null && event != null) {
                root = event.getSource();
            }
            if (root == null) return false;

            boolean mentionsQiezka = false;
            boolean mentionsUninstall = isUninstallClass;
            AccessibilityNodeInfo cancelButton = null;

            // 1. Search for app label or name "QIEZKA" / "com.uncode.app"
            List<AccessibilityNodeInfo> labelNodes = root.findAccessibilityNodeInfosByViewId("com.android.packageinstaller:id/app_label");
            if (labelNodes != null && !labelNodes.isEmpty()) {
                for (AccessibilityNodeInfo node : labelNodes) {
                    CharSequence text = node.getText();
                    if (text != null && text.toString().toLowerCase(Locale.US).contains("qiezka")) {
                        mentionsQiezka = true;
                        break;
                    }
                }
            }

            if (!mentionsQiezka) {
                List<AccessibilityNodeInfo> textNodes = root.findAccessibilityNodeInfosByText("QIEZKA");
                if (textNodes != null && !textNodes.isEmpty()) {
                    mentionsQiezka = true;
                }
            }

            if (!mentionsQiezka) {
                List<AccessibilityNodeInfo> idNodes = root.findAccessibilityNodeInfosByText(getPackageName());
                if (idNodes != null && !idNodes.isEmpty()) {
                    mentionsQiezka = true;
                }
            }

            // 2. Search for uninstall terms or title
            List<AccessibilityNodeInfo> titleNodes = root.findAccessibilityNodeInfosByViewId("com.android.packageinstaller:id/alertTitle");
            if (titleNodes != null && !titleNodes.isEmpty()) {
                for (AccessibilityNodeInfo node : titleNodes) {
                    CharSequence text = node.getText();
                    if (text != null) {
                        String t = text.toString().toLowerCase(Locale.US);
                        if (t.contains("uninstall") || t.contains("delete") || t.contains("remove")) {
                            mentionsUninstall = true;
                            break;
                        }
                    }
                }
            }

            if (!mentionsUninstall) {
                List<AccessibilityNodeInfo> uninstallTextNodes = root.findAccessibilityNodeInfosByText("Uninstall");
                if (uninstallTextNodes != null && !uninstallTextNodes.isEmpty()) {
                    mentionsUninstall = true;
                }
            }

            // 3. Find Cancel button to auto-dismiss
            List<AccessibilityNodeInfo> cancelButtons = root.findAccessibilityNodeInfosByViewId("android:id/button2");
            if (cancelButtons != null && !cancelButtons.isEmpty()) {
                cancelButton = cancelButtons.get(0);
            } else {
                List<AccessibilityNodeInfo> cancelTextNodes = root.findAccessibilityNodeInfosByText("Cancel");
                if (cancelTextNodes != null && !cancelTextNodes.isEmpty()) {
                    cancelButton = cancelTextNodes.get(0);
                }
            }

            if (mentionsQiezka && mentionsUninstall) {
                Log.w(TAG, "🛡️ INTERCEPTED UNINSTALL ATTEMPT TARGETING QIEZKA! Auto-cancelling...");

                // Step 1: Click "Cancel" if available
                if (cancelButton != null && cancelButton.isClickable()) {
                    cancelButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                }

                // Step 2: Perform global Back action
                performGlobalAction(GLOBAL_ACTION_BACK);

                // Step 3: Bring QIEZKA back to foreground immediately
                launchLockOverlay();

                // Step 4: Show anti-tamper warning Toast
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(getApplicationContext(),
                        "🛡️ QIEZKA Anti-Tamper Shield: App cannot be uninstalled while active!",
                        Toast.LENGTH_LONG).show();
                });

                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in interceptUninstallAttempt: " + e.getMessage());
        } finally {
            if (root != null && (event == null || root != event.getSource())) {
                try {
                    root.recycle();
                } catch (Exception ignore) {}
            }
        }
        return false;
    }

    /**
     * Intercepts default home launcher selection screens, popups, and role requests
     * (e.g. from Nova Launcher, RoleManager, or ResolverActivity), preventing users or third-party
     * apps from modifying the established home launcher.
     */
    private boolean interceptHomeLauncherChangeAttempt(AccessibilityEvent event, AccessibilityNodeInfo providedRoot, String pkg) {
        if (pkg == null) return false;
        String lowerPkg = pkg.toLowerCase(Locale.US);

        // Home selection is hosted by PermissionController, Android framework resolver, Settings, or 3rd-party launchers
        boolean isPotentialHost = lowerPkg.contains("permissioncontroller") || 
                                  lowerPkg.equals("android") || 
                                  lowerPkg.contains("settings") ||
                                  (lowerPkg.contains("launcher") && !KNOWN_LAUNCHERS.contains(pkg));

        if (!isPotentialHost) return false;

        String classStr = "";
        if (event != null && event.getClassName() != null) {
            classStr = event.getClassName().toString().toLowerCase(Locale.US);
        }

        AccessibilityNodeInfo root = providedRoot;
        boolean needsRecycle = false;
        try {
            if (root == null) {
                root = getRootInActiveWindow();
                if (root != null) needsRecycle = true;
            }
            if (root == null && event != null) {
                root = event.getSource();
            }

            // Quick class-level match for Role / Default app activities
            if (classStr.contains("defaultappactivity") || classStr.contains("requestroleactivity") || classStr.contains("homesettingsactivity")) {
                evictHomeLauncherChange();
                return true;
            }

            if (root == null) return false;

            // 1. Direct window title inspection
            String winTitle = resolveActiveWindowTitle(root);
            if (winTitle != null) {
                String lowerTitle = winTitle.toLowerCase(Locale.US);
                if (lowerTitle.contains("home app") || 
                    lowerTitle.contains("select a home") || 
                    lowerTitle.contains("choose home") ||
                    lowerTitle.contains("use as home") ||
                    (lowerTitle.contains("home") && lowerTitle.contains("default"))) {
                    evictHomeLauncherChange();
                    return true;
                }
            }

            // 2. Search for launcher and home indicators in hierarchy
            if (lowerPkg.contains("permissioncontroller") || lowerPkg.contains("settings") || lowerPkg.equals("android")) {
                List<AccessibilityNodeInfo> launcherNodes = root.findAccessibilityNodeInfosByText("Launcher");
                if (launcherNodes == null || launcherNodes.isEmpty()) {
                    launcherNodes = root.findAccessibilityNodeInfosByText("launcher");
                }

                List<AccessibilityNodeInfo> homeNodes = root.findAccessibilityNodeInfosByText("Home");
                if (homeNodes == null || homeNodes.isEmpty()) {
                    homeNodes = root.findAccessibilityNodeInfosByText("home");
                }

                // If PermissionController / Settings is displaying both launcher items and home references
                if (launcherNodes != null && !launcherNodes.isEmpty() && homeNodes != null && !homeNodes.isEmpty()) {
                    evictHomeLauncherChange();
                    return true;
                }

                // Description string in AOSP / Google PermissionController: "Apps, often called launchers..."
                List<AccessibilityNodeInfo> phraseNodes = root.findAccessibilityNodeInfosByText("launchers");
                if (phraseNodes != null && !phraseNodes.isEmpty()) {
                    evictHomeLauncherChange();
                    return true;
                }

                phraseNodes = root.findAccessibilityNodeInfosByText("Default home");
                if (phraseNodes != null && !phraseNodes.isEmpty()) {
                    evictHomeLauncherChange();
                    return true;
                }
            }

            // 3. Third party launchers (e.g. Nova Launcher prompt to set default launcher)
            if (lowerPkg.contains("launcher") && !KNOWN_LAUNCHERS.contains(pkg)) {
                List<AccessibilityNodeInfo> defaultHomeNodes = root.findAccessibilityNodeInfosByText("Default");
                if (defaultHomeNodes != null && !defaultHomeNodes.isEmpty()) {
                    List<AccessibilityNodeInfo> launcherNodes = root.findAccessibilityNodeInfosByText("Launcher");
                    if (launcherNodes != null && !launcherNodes.isEmpty()) {
                        evictHomeLauncherChange();
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Error checking home launcher change attempt: " + e.getMessage());
        } finally {
            if (root != null && needsRecycle) {
                try {
                    root.recycle();
                } catch (Exception ignore) {}
            }
        }
        return false;
    }

    private void evictHomeLauncherChange() {
        Log.w(TAG, "🛡️ Intercepted Default Home App / Launcher change attempt! Dismissing and returning home.");
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(getApplicationContext(),
                "🛡️ Changing default home launcher is restricted by QIEZKA",
                Toast.LENGTH_SHORT).show();
        });

        // Global BACK to dismiss any popup/dialog, then HOME to restore launcher
        performGlobalAction(GLOBAL_ACTION_BACK);
        performGlobalAction(GLOBAL_ACTION_HOME);
    }

    @Override
    public void onInterrupt() {
        // Required override
    }
}

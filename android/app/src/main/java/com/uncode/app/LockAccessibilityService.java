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
import android.net.Uri;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodInfo;

import android.app.Notification;
import android.widget.Toast;
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
     * NOTE: com.android.settings is intentionally NOT here — it should be blockable.
     * The launcher/home is also exempt because we actively send users there on block.
     */
    private static final Set<String> ALWAYS_EXEMPT = new HashSet<>(Arrays.asList(
        "android",                   // Core OS framework
        "com.android.systemui",      // Status bar, nav bar, recents UI
        "com.android.phone",         // Core telephony
        "com.android.server.telecom",// Core telecom
        "com.android.incallui",      // In-call UI (incoming phone calls)
        "com.google.android.dialer", // Google Phone app
        "com.samsung.android.dialer",// Samsung Phone app
        "com.samsung.android.incallui" // Samsung In-call UI
    ));

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
     * Known System Package Installers and App Store packages.
     * These are hardcoded system exemptions so that manual APK installations,
     * updates, and Play Store update calls are never blocked during lockdown.
     */
    public static final Set<String> KNOWN_INSTALLER_AND_STORE_PACKAGES = new HashSet<>(Arrays.asList(
        "com.android.vending",                     // Google Play Store
        "com.google.android.feedback",             // Play Store feedback
        "com.google.android.gms",                  // Google Play Services
        "com.google.android.packageinstaller",     // Google Package Installer
        "com.android.packageinstaller",            // AOSP Package Installer
        "com.samsung.android.packageinstaller",    // Samsung Package Installer
        "com.sec.android.app.samsungapps",         // Samsung Galaxy Store
        "com.miui.packageinstaller",               // Xiaomi Package Installer
        "com.xiaomi.mipicks",                      // Xiaomi GetApps
        "com.coloros.packageinstaller",            // ColorOS Package Installer
        "com.oppo.packageinstaller",               // Oppo Package Installer
        "com.heytap.market",                       // Oppo / Realme App Market
        "com.vivo.packageinstaller",               // Vivo Package Installer
        "com.vivo.appstore",                       // Vivo App Store
        "com.transsion.packageinstaller",          // Transsion Package Installer
        "com.huawei.appmarket",                    // Huawei AppGallery
        "com.hihonor.appmarket",                   // Honor App Market
        "com.lenovo.safecenter"                    // Lenovo Package Installer / Security
    ));

    public static boolean isInstallerOrStoreApp(String pkg) {
        if (pkg == null) return false;
        if (KNOWN_INSTALLER_AND_STORE_PACKAGES.contains(pkg)) return true;
        String lower = pkg.toLowerCase(Locale.US);
        return lower.contains("packageinstaller") || 
               lower.endsWith(".packageinstaller") ||
               lower.contains(".installer");
    }

    public static volatile LockAccessibilityService instance = null;

    public static LockAccessibilityService getInstance() {
        return instance;
    }

    private final Set<String> dynamicExemptPackages = new HashSet<>();
    private final Set<String> dynamicKeyboardPackages = new HashSet<>();
    private SharedPreferences prefs;

    private String lastForegroundPackage = null;
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
                          AccessibilityEvent.TYPE_VIEW_CLICKED |
                          AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED;
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
                        if (!BlacklistConstants.isBlacklisted(bPkg)) {
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

            Log.d(TAG, "Discovered dynamic exempt packages: media=" + dynamicExemptPackages.size() + ", keyboards=" + dynamicKeyboardPackages.size());
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
               lower.contains("colornote");
    }

    private boolean isStudentApp(String pkg) {
        if (pkg == null) return false;
        if (KNOWN_STUDENT_APPS.contains(pkg)) return true;
        String lower = pkg.toLowerCase();
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
        CharSequence pkgChar = event.getPackageName();
        if (pkgChar != null) {
            String pkgStr = pkgChar.toString();
            if (!pkgStr.equals("com.android.systemui")) {
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
            AlarmReceiver.showNotificationStatic(
                    this,
                    AlarmReceiver.NOTIF_ID_COMPLETED,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    "⚠️ Homework Expired — Consequence Active",
                    "Study timer expired without passing. Distracting apps remain restricted during operating hours (7 PM – 3 AM) until rescheduled and passed.",
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
                // Outside operating hours (3:00 AM to 7:00 PM): QIEZKA does not operate.
                // Allow normal phone access for daytime / sleep.
                return;
            }
            isLockdownActive = true;
        } else if (!isLockdownActive) {
            isLockdownActive = checkAndAutoStartScheduledLock(prefs);
        }

        if (!isLockdownActive) return;

        if (pkgChar == null) return;
        String pkg = pkgChar.toString();

        // Never inspect or block QIEZKA itself
        if (pkg.equals(getPackageName())) return;

        // ── SystemUI handling ──
        if (pkg.equals("com.android.systemui")) {
            handleSystemUiEvent(event);
            return;
        }

        // If QIEZKA is already the active foreground window, ignore background events from other apps
        if (getPackageName().equals(detectCurrentForegroundPackage())) {
            return;
        }

        if (isPackageBlocked(pkg)) {
            enforceBlock(pkg);
        } else if (isBrowserPackage(pkg)) {
            handleBrowserUrlInspection(event, pkg);
        }
    }

    private long lastBrowserToastTime = 0L;

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

    private static final Set<String> BLACKLISTED_WEB_DOMAINS = new HashSet<>(Arrays.asList(
        "tiktok.com",
        "instagram.com",
        "facebook.com", "fb.com",
        "twitter.com", "x.com",
        "reddit.com",
        "threads.net",
        "snapchat.com",
        "discord.com",
        "twitch.tv",
        "netflix.com",
        "disneyplus.com",
        "hulu.com",
        "primevideo.com",
        "webtoons.com",
        "mangadex.org",
        "mangakakalot.com",
        "bilibili.tv", "bilibili.com",
        "roblox.com",
        "9gag.com",
        "pinterest.com",
        "tumblr.com"
    ));

    // Comprehensive Web-Based Games Database (250+ Domains)
    private static final Set<String> WEB_GAMING_DOMAINS = new HashSet<>(Arrays.asList(
        // A. Mega Portals & Aggregators
        "poki.com", "poki-gdn.com", "poki.cz", "poki.nl", "poki.com.br",
        "crazygames.com", "crazygames.co.uk", "crazygames.fr", "crazygames.io",
        "coolmathgames.com", "coolmath-games.com", "coolmath.com",
        "kongregate.com", "kongregate.io",
        "armorgames.com", "armorgamesonline.com",
        "newgrounds.com", "ungrounded.net",
        "y8.com", "y8games.com", "id.net",
        "friv.com", "friv5.me", "friv.cm", "friv.today", "friv.cool", "frivclassic.com", "friv-2017.com",
        "miniplay.com", "minijuegos.com",
        "addictinggames.com", "silvergames.com",
        "kizi.com", "kizi10.org",
        "gamepix.com", "lagged.com",
        "agame.com", "a-game.com", "gamesgames.com",
        "snokido.com", "snokido.fr", "snokido.net",
        "kbhgames.com", "playhop.com", "1001games.com",
        "twoplayergames.org", "2playergames.com", "pomu.com", "paisdelosjuegos.com",
        "games2girls.com", "girlsgogames.com", "mousebreaker.com", "stickpage.com",
        "speele.nl", "jetztspielen.de", "gry.pl", "jeuxjeuxjeux.fr",
        "arkadium.com", "gameforge.com", "gameflare.com", "gamepost.com",
        "titotu.io", "kevin.games", "zone.msn.com", "plays.org", "bubbleshooter.net",

        // B. Viral .IO & Multiplayer Arena Games
        "slither.io", "slitherio.org", "agar.io", "agar.pro", "agariogame.club",
        "diep.io", "krunker.io", "yendis.ch",
        "1v1.lol", "1v1.school", "justfall.lol",
        "paper.io", "paper-io.com", "paperio2.com",
        "hole.io", "hole-io.com",
        "surviv.io", "survev.io", "suroi.io",
        "skribbl.io", "gartic.io", "garticphone.com", "drawasaurus.org",
        "shellshock.io", "eggcombat.com", "shellshockers.io",
        "deeeep.io", "bloxd.io", "voxiom.io", "smashkarts.io",
        "ev.io", "zombs.io", "zombsroyale.io", "starve.io", "moomoo.io",
        "narrow.one", "venge.io", "bonk.io", "bonk2.io",
        "wings.io", "brutal.io", "splix.io", "lordz.io",
        "flyordie.io", "evojaws.io", "evoworld.io",
        "digdig.io", "yohoho.io", "taming.io", "betrayal.io",
        "battledudes.io", "lolbeans.io", "warbrokers.io",
        "curvefever.pro", "curvefever.com", "littlebigsnake.com", "arrow.io",
        "iogames.space", "iogames.onl", "io-games.io",

        // C. Cloud Gaming & Web APK Streaming Backdoors
        "now.gg", "nowgg.me", "nowgg.io",
        "play.geforcenow.com", "geforcenow.com",
        "luna.amazon.com",
        "boosteroid.com", "cloud.boosteroid.com",
        "shadow.tech", "vortex.gg", "airgpu.com",

        // D. Indie Web Runtimes & CDNs
        "itch.zone", "itch.io", "gamejolt.com", "gx.games",
        "simmer.io", "lexaloffle.com", "flowlab.io", "arcade.construct.net",

        // E. Web Emulators & Retro Gaming
        "emulatoronline.com", "retrogames.cc", "playretrogames.com", "vimm.net",
        "emupedia.net", "emupedia.org", "afterplay.io", "eclipseemu.me",
        "webretro.org", "game-oldies.com", "ssega.com", "playminigames.net",
        "playclassic.games", "dosgames.com", "playdosgames.com",
        "online-emulators.com", "retrogames.onl", "myabandonware.com",
        "consoleroms.com", "archaic-bingo.com", "wowroms.com", "freeroms.com",

        // F. Unblocked Games Dedicated Networks & Mirrors
        "unblocked-games.com", "unblockedgames66.com", "unblockedgames66plus.com", "unblockedgames66ez.com",
        "unblockedgames76.com", "unblockedgames77.com", "unblockedgames99.com",
        "unblockedgames500.com", "unblockedgames119.com", "unblockedgames24h.com",
        "classroom6x.com", "classroom-6x.org",
        "slope-game.com", "slopeunblocked.org", "slopegame.online",
        "hoodamath.com", "mathplayground.com", "abcya.com", "primarygames.com",
        "freeonlinegames.com", "b-games.com", "unblocked-games-76.com",
        "unblockedgame76.com", "unblockedgamesworld.com", "unblockedgamespod.com",
        "unblockedgames.me", "unblocked-games-s.com", "unblockedgame.io", "unblockedgamesfree.com",

        // G. Casual, Board, Puzzle & Incremental Games
        "chess.com", "lichess.org", "chess24.com", "chessbomb.com",
        "geoguessr.com", "worldle.teuteuf.fr", "globle-game.com", "geoguess.games", "city-guesser.com",
        "2048game.com", "play2048.co", "2048.io",
        "sudoku.com", "websudoku.com", "nonograms.org",
        "wordlewebsite.com", "wordle.org", "quordle.com", "octordle.com", "sedecordle.com",
        "solitaired.com", "cardgames.io", "solitaireparadise.com", "247solitaire.com",
        "sporcle.com", "jetpunk.com", "tetr.io", "jstris.jezevec10.com", "cookieclicker.ee"
    ));

    // Academic & Developer Resources Immune to Heuristic Game Filters
    private static final Set<String> ACADEMIC_EXEMPT_DOMAINS = new HashSet<>(Arrays.asList(
        "wikipedia.org", "wikimedia.org",
        "khanacademy.org", "coursera.org", "edx.org", "udemy.com",
        "quizlet.com", "brainly.com", "chegg.com", "duolingo.com",
        "developer.mozilla.org", "github.com",
        "stackoverflow.com", "stackexchange.com",
        "docs.unity3d.com", "docs.godotengine.org", "unrealengine.com",
        "arxiv.org", "researchgate.net", "jstor.org", "nature.com", "sciencedirect.com"
    ));

    private boolean isBrowserPackage(String pkg) {
        if (pkg == null) return false;
        if (KNOWN_BROWSER_PACKAGES.contains(pkg)) return true;
        String lower = pkg.toLowerCase(Locale.US);
        return lower.contains("browser") || lower.contains("chrome");
    }

    private void handleBrowserUrlInspection(AccessibilityEvent event, String pkg) {
        String webMode = prefs != null ? prefs.getString("web_protection_mode", "accessibility") : "accessibility";
        if ("off".equalsIgnoreCase(webMode) || "dns_vpn".equalsIgnoreCase(webMode)) {
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        try {
            String url = extractUrlFromBrowser(root, pkg);
            if (url == null || url.trim().isEmpty()) return;

            String lowerUrl = url.toLowerCase(Locale.US).trim();
            boolean allowYoutube = prefs != null && prefs.getBoolean("allow_youtube", false);

            boolean isBlocked = false;
            String reason = "Distracting website blocked during focus mode";

            // Check YouTube:
            // Native YouTube app is completely blocked (only accessible through browser).
            // By default, YouTube web is also completely blocked.
            // When allow_youtube is true, educational/long-form videos on YouTube web are allowed,
            // but YouTube Shorts (/shorts/*) are strictly blocked.
            if (lowerUrl.contains("youtube.com") || lowerUrl.contains("youtu.be")) {
                if (!allowYoutube) {
                    isBlocked = true;
                    reason = "YouTube is blocked during focus mode";
                } else {
                    if (lowerUrl.contains("/shorts") || lowerUrl.contains("#shorts")) {
                        isBlocked = true;
                        reason = "YouTube Shorts are blocked during study sessions";
                    }
                }
            }

            // Check general distraction web blacklist
            if (!isBlocked) {
                for (String domain : BLACKLISTED_WEB_DOMAINS) {
                    if (lowerUrl.contains(domain)) {
                        isBlocked = true;
                        break;
                    }
                }
            }

            // Check Web-Based Games & Dynamic Heuristics (250+ domains, unblocked mirrors, cloud APKs, search mini-games)
            boolean blockWebGames = prefs != null && prefs.getBoolean("block_web_games", true);
            if (!isBlocked && blockWebGames) {
                // 1. Browser Internal Game Schemes (Chrome Dino, Edge Surf)
                if (lowerUrl.startsWith("chrome://dino") || lowerUrl.contains("chrome://network-error/-106") ||
                    lowerUrl.startsWith("edge://surf") || lowerUrl.startsWith("opera://game")) {
                    isBlocked = true;
                    reason = "Browser mini-games are blocked during study sessions";
                }

                // 2. Google Search & Bing Embedded Canvas Games
                if (!isBlocked && (lowerUrl.contains("google.") || lowerUrl.contains("bing.com")) &&
                    (lowerUrl.contains("/search") || lowerUrl.contains("?q=") || lowerUrl.contains("&q="))) {
                    String[] searchTriggers = {
                        "snake", "play+snake", "play%20snake",
                        "tic+tac+toe", "tic%20tac%20toe",
                        "pacman", "pac-man", "pac%20man",
                        "minesweeper", "solitaire",
                        "atari+breakout", "atari%20breakout",
                        "dreidel", "fidget+spinner", "fidget%20spinner",
                        "earth+day+quiz", "memory+game", "play+game"
                    };
                    for (String trigger : searchTriggers) {
                        if (lowerUrl.contains("q=" + trigger) || lowerUrl.contains("&q=" + trigger) || lowerUrl.contains("query=" + trigger)) {
                            isBlocked = true;
                            reason = "Search-embedded mini-games are blocked during focus mode";
                            break;
                        }
                    }
                }

                // 3. Direct Web Gaming Domain Database Matching (250+ domains)
                if (!isBlocked) {
                    for (String domain : WEB_GAMING_DOMAINS) {
                        if (lowerUrl.contains(domain)) {
                            isBlocked = true;
                            reason = "Web-based game blocked during focus mode";
                            break;
                        }
                    }
                }

                // 4. Unblocked Game Mirrors & Dynamic Path Patterns (with Academic Safe-List Immunity)
                if (!isBlocked) {
                    boolean isAcademicExempt = false;
                    for (String exempt : ACADEMIC_EXEMPT_DOMAINS) {
                        if (lowerUrl.contains(exempt)) {
                            isAcademicExempt = true;
                            break;
                        }
                    }
                    if (!isAcademicExempt && (lowerUrl.contains(".edu/") || lowerUrl.endsWith(".edu"))) {
                        isAcademicExempt = true;
                    }

                    if (!isAcademicExempt) {
                        if (lowerUrl.contains(".itch.zone") || lowerUrl.contains("poki-gdn.com")) {
                            isBlocked = true;
                            reason = "Web-based game runtime blocked during focus mode";
                        } else if (lowerUrl.contains("unblocked-games") || lowerUrl.contains("unblockedgames") ||
                                   lowerUrl.contains("unblocked_games") || lowerUrl.contains("classroom6x") ||
                                   lowerUrl.contains("slope-game") || lowerUrl.contains("slopeunblocked") ||
                                   lowerUrl.contains("slopegame") || lowerUrl.contains("retrobowl") ||
                                   lowerUrl.contains("moto-x3m")) {
                            isBlocked = true;
                            reason = "Unblocked games mirror blocked during focus mode";
                        } else if (lowerUrl.contains("sites.google.com/") &&
                                  (lowerUrl.contains("unblocked") || lowerUrl.contains("game") || lowerUrl.contains("arcade") ||
                                   lowerUrl.contains("slope") || lowerUrl.contains("1v1") || lowerUrl.contains("retro") || lowerUrl.contains("emulator"))) {
                            isBlocked = true;
                            reason = "Google Sites game mirror blocked during focus mode";
                        } else if (lowerUrl.contains(".github.io/") &&
                                  (lowerUrl.contains("unblocked") || lowerUrl.contains("game") || lowerUrl.contains("slope") ||
                                   lowerUrl.contains("1v1") || lowerUrl.contains("retro") || lowerUrl.contains("emulator"))) {
                            isBlocked = true;
                            reason = "GitHub Pages game mirror blocked during focus mode";
                        } else if (lowerUrl.contains("nytimes.com/games") || lowerUrl.contains("nytimes.com/crosswords") ||
                                   lowerUrl.contains("nytimes.com/puzzles") || lowerUrl.contains("washingtonpost.com/crossword") ||
                                   lowerUrl.contains("theguardian.com/crosswords") || lowerUrl.contains("zone.msn.com") ||
                                   lowerUrl.contains("yandex.com/games") || lowerUrl.contains("yandex.ru/games") ||
                                   lowerUrl.contains("scratch.mit.edu/projects/")) {
                            isBlocked = true;
                            reason = "Web games section blocked during study sessions";
                        }
                    }
                }
            }

            if (isBlocked) {
                Log.w(TAG, "Blocked browser URL: " + url + " (" + reason + ")");
                performGlobalAction(GLOBAL_ACTION_BACK);

                long now = System.currentTimeMillis();
                if (now - lastBrowserToastTime > 2500L) {
                    lastBrowserToastTime = now;
                    final String finalReason = reason;
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(getApplicationContext(), "⚠️ " + finalReason, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        } finally {
            root.recycle();
        }
    }

    private String extractUrlFromBrowser(AccessibilityNodeInfo root, String pkg) {
        if (root == null) return null;

        // 1. Chrome / Chromium family
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId("com.android.chrome:id/url_bar");
        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo node = nodes.get(0);
            CharSequence text = node.getText();
            if (text != null && text.length() > 0) return text.toString();
        }

        // 2. Samsung Internet
        nodes = root.findAccessibilityNodeInfosByViewId("com.sec.android.app.sbrowser:id/location_bar_edit_text");
        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo node = nodes.get(0);
            CharSequence text = node.getText();
            if (text != null && text.length() > 0) return text.toString();
        }

        // 3. Firefox
        nodes = root.findAccessibilityNodeInfosByViewId("org.mozilla.firefox:id/url_bar_title");
        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo node = nodes.get(0);
            CharSequence text = node.getText();
            if (text != null && text.length() > 0) return text.toString();
        }

        // 4. Edge
        nodes = root.findAccessibilityNodeInfosByViewId("com.microsoft.emmx:id/url_bar");
        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo node = nodes.get(0);
            CharSequence text = node.getText();
            if (text != null && text.length() > 0) return text.toString();
        }

        // 5. Brave
        nodes = root.findAccessibilityNodeInfosByViewId("com.brave.browser:id/url_bar");
        if (nodes != null && !nodes.isEmpty()) {
            AccessibilityNodeInfo node = nodes.get(0);
            CharSequence text = node.getText();
            if (text != null && text.length() > 0) return text.toString();
        }

        // 6. Generic Heuristic Fallback
        return findUrlInHierarchy(root, 0);
    }

    private String findUrlInHierarchy(AccessibilityNodeInfo node, int depth) {
        if (node == null || depth > 8) return null;

        String resId = node.getViewIdResourceName();
        if (resId != null) {
            String lowerResId = resId.toLowerCase(Locale.US);
            if (lowerResId.contains("url_bar") || lowerResId.contains("location_bar") || lowerResId.contains("address_bar") || lowerResId.contains("omnibox")) {
                CharSequence text = node.getText();
                if (text != null && text.length() > 0) return text.toString();
            }
        }

        CharSequence text = node.getText();
        if (text != null) {
            String t = text.toString().toLowerCase(Locale.US).trim();
            if (t.startsWith("http://") || t.startsWith("https://") || t.contains(".com/") || t.contains(".net/") || t.contains(".org/")) {
                return t;
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
        if (isInstallerOrStoreApp(pkg)) return false;
        if (isKeyboardApp(pkg)) return false;
        if (isAuthenticatorApp(pkg)) return false;
        if (isNotesApp(pkg)) return false;
        if (isStudentApp(pkg)) return false;
        if (isAiApp(pkg)) return false;
        if (isHiddenInfrastructureApp(pkg)) return false;

        // Hardcoded Distraction Blacklist Check (Strictly Takes Precedence)
        if (BlacklistConstants.isBlacklisted(pkg)) return true;

        if (KNOWN_LAUNCHERS.contains(pkg)) return false;
        if (pkg.contains("documentsui")) return false;
        if (MEDIA_AND_FILE_EXEMPT.contains(pkg) || dynamicExemptPackages.contains(pkg) || KNOWN_MUSIC_APPS.contains(pkg)) return false;

        Set<String> whitelist = prefs.getStringSet("whitelist", new HashSet<>());
        if (whitelist.contains(pkg)) return false;

        // ── Tier 3: On-Device Local App Classifier (Metadata, Categories & Heuristics) ──
        return AppClassifier.isPackageBlocked(this, pkg, whitelist);
    }

    private void onTickerTick() {
        if (prefs == null) {
            prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        }

        long timeOffset = prefs.getLong("time_offset", 0L);
        long effectiveNow = System.currentTimeMillis() + timeOffset;

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
            AlarmReceiver.showNotificationStatic(
                    this,
                    AlarmReceiver.NOTIF_ID_COMPLETED,
                    AlarmReceiver.CHANNEL_ID_ALERTS,
                    "⚠️ Homework Expired — Consequence Active",
                    "Study timer expired without passing. Distracting apps remain restricted during operating hours (7 PM – 3 AM) until rescheduled and passed.",
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

        // 1c. Synchronize LocalDnsVpnService with active lockdown and operating hours
        String webMode = prefs.getString("web_protection_mode", "accessibility");
        boolean inOperatingHours = isInOperatingHours(effectiveNow);
        boolean shouldVpnRun = (isLockdownActive || (isConsequenceActive && inOperatingHours))
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

        // 3. If consequence mode or lockdown is currently active in operating hours, continuously enforce blocking!
        if (isConsequenceActive) {
            if (inOperatingHours) {
                String currentForegroundPkg = detectCurrentForegroundPackage();
                if (currentForegroundPkg != null && isPackageBlocked(currentForegroundPkg)) {
                    enforceBlock(currentForegroundPkg);
                }
            }
        } else if (isLockdownActive) {
            String currentForegroundPkg = detectCurrentForegroundPackage();
            if (currentForegroundPkg != null && isPackageBlocked(currentForegroundPkg)) {
                enforceBlock(currentForegroundPkg);
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
        if (now - lastBlockTimestamp < 800L) {
            return; // Debounce rapid accessibility events from the same launch attempt
        }
        lastBlockTimestamp = now;

        Log.w(TAG, "enforceBlock on distracting app: " + pkg + " — bringing QIEZKA lock screen to front");
        if (Settings.canDrawOverlays(this)) {
            launchLockOverlay();
        } else {
            goHome();
        }
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
        // 1. Inspect interactive application windows
        try {
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null && !windows.isEmpty()) {
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        if (w.isActive() || w.isFocused()) {
                            AccessibilityNodeInfo root = w.getRoot();
                            if (root != null) {
                                CharSequence p = root.getPackageName();
                                root.recycle();
                                if (p != null) {
                                    String pkg = p.toString();
                                    if (!pkg.equals("com.android.systemui")) {
                                        lastForegroundPackage = pkg;
                                        return pkg;
                                    }
                                }
                            }
                        }
                    }
                }
                for (AccessibilityWindowInfo w : windows) {
                    if (w.getType() == AccessibilityWindowInfo.TYPE_APPLICATION) {
                        AccessibilityNodeInfo root = w.getRoot();
                        if (root != null) {
                            CharSequence p = root.getPackageName();
                            root.recycle();
                            if (p != null) {
                                String pkg = p.toString();
                                if (!pkg.equals("com.android.systemui")) {
                                    lastForegroundPackage = pkg;
                                    return pkg;
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignore) {}

        // 2. Fallback: getRootInActiveWindow()
        try {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                CharSequence p = root.getPackageName();
                root.recycle();
                if (p != null) {
                    String pkg = p.toString();
                    if (!pkg.equals("com.android.systemui")) {
                        lastForegroundPackage = pkg;
                        return pkg;
                    }
                }
            }
        } catch (Exception ignore) {}

        // 3. Fallback: last recorded foreground package from events
        if (lastForegroundPackage != null && !lastForegroundPackage.equals("com.android.systemui")) {
            return lastForegroundPackage;
        }

        return null;
    }

    private void launchLockOverlay() {
        try {
            Intent launch = getPackageManager().getLaunchIntentForPackage(getPackageName());
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                boolean isConsequence = prefs != null && prefs.getBoolean("consequence_active", false);
                launch.putExtra("route", isConsequence ? "logs" : "locked");
                startActivity(launch);
            }
        } catch (Exception e) {
            Log.e(TAG, "launchLockOverlay failed: " + e.getMessage());
        }
    }

    /**
     * Sends the user to the Home screen instead of forcing them back into QIEZKA.
     * This is the correct behaviour: QIEZKA is not a kiosk, just a selective blocker.
     */
    private void goHome() {
        try {
            performGlobalAction(GLOBAL_ACTION_HOME);
        } catch (Exception e) {
            Log.e(TAG, "goHome failed: " + e.getMessage());
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

    @Override
    public void onInterrupt() {
        // Required override
    }
}

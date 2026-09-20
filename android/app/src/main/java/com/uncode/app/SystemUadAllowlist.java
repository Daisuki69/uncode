package com.uncode.app;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * SystemUadAllowlist:
 * Dedicated, standalone system allowlist extracted from Universal Android Debloater Next Generation (UAD-NG).
 *
 * Divided strictly by Phone Manufacturer / Brand / Operating System.
 * Houses essential system infrastructure:
 * - Life Safety & Emergency SOS (Car crash detection, 911/emergency dialers, wireless alerts)
 * - Screenshot Capture, Image Cropping & Markup (Fixes AOSP "Edit" false-positive)
 * - Share Sheets & Intent Resolvers (Fixes AOSP "Share" / com.android.intentresolver false-positive)
 * - Captive Portal Wi-Fi login, Wi-Fi dialogs & VPN consent prompts
 * - Modern Photo Picker (Android 13+) & Storage Access Framework
 * - Print Spooler (PDF & homework worksheet printing)
 * - Accessibility Suite (TalkBack, Sound Amplifier, Switch Access)
 *
 * Maintained as an independent authority file to avoid polluting QIEZKA's internal app blocklists.
 */
public final class SystemUadAllowlist {

    private SystemUadAllowlist() {}

    // ── AOSP & Google Pixel Ecosystem (78 packages) ──
    public static final Set<String> AOSP_AND_GOOGLE_PACKAGES = new HashSet<>(Arrays.asList(new String[] {
        "com.android.phone.product.res.overlay.no_imei_emergency_call", // [Emergency & Safety] Unsure but I would just leave it as you need to stay safe. Find out mo
        "com.android.screenshot", // [Screenshot & Markup] Default android screenshot tool
        "com.android.stk2", // [Telephony & Telecom] SIM toolkit Special package for dual-sim devices? Enables carriers to 
        "com.android.stk", // [Telephony & Telecom] SIM toolkit Enables carriers to initiate\"value-added services\". Basi
        "com.android.captiveportallogin", // [Captive Portal & System Dialogs] Support for captive portal: https://en.wikipedia.org/wiki/Captive_port
        "com.android.cellbroadcastreceiver", // [Emergency & Safety] Cell broadcast is designed to deliver messages to multiple users in an
        "com.android.cellbroadcastreceiver.module", // [Emergency & Safety] Same as com.android.cellbroadcastreceiver. Cell broadcasting used to s
        "com.android.emergency", // [Emergency & Safety] Emergency rescue Shows emergency info on lockscreen and power menu. Sa
        "com.android.intentresolver", // [Share Sheet & Intent Resolver] 'Share' functionality will be disabled after uninstalling this package
        "com.android.printservice.recommendation", // [Print Spooler] Recommends 3rd-party print services apps in the PlayStore. Printing wi
        "com.android.printspooler", // [Print Spooler] Print Spooler Manages the printing process. Runs on boot, but not beyo
        "com.android.certinstaller", // [Captive Portal & System Dialogs] Certificate installer Used for accepting and revoking Internet certifi
        "com.android.documentsui", // [Media Picker & Documents] Occasionally runs in the background. File selector for other apps. Sto
        "com.android.documentsui.a_overlay", // [Media Picker & Documents] Some overlay for for\"Files\"?
        "com.android.keychain", // [Captive Portal & System Dialogs] Enables apps to use system wide credential KeyChain (shared credential
        "com.android.mtp", // [Core Hardware Prompts] MTP Host Handles MTP(Media Transfer Protocol), a protocol for transfer
        "com.android.server.telecom", // [Telephony & Telecom] Manages calls via your network provider or SIM and controls the phone 
        "com.android.server.telecom.a_overlay", // [Telephony & Telecom] Overlay for com.android.server.telecom?
        "com.android.vpndialogs", // [Captive Portal & System Dialogs] Provide VPN support to Android https://en.wikipedia.org/wiki/Dialog_(s
        "com.android.bluetooth.tempow", // [Core Hardware Prompts] Implementation of a improved bluetooth protocol (developed by french c
        "com.android.dialer", // [Emergency & Safety] AOSP Dialer/Phone app Default phone app on some older phones(like Onep
        "com.android.cellbroadcastreceiver.basiccolorblack.overlay", // [Emergency & Safety] Dark theme overlay for com.android.cellbroadcastreceiver
        "com.android.cellbroadcastreceiver.basiccolorwhite.overlay", // [Emergency & Safety] Light theme overlay for com.android.cellbroadcastreceiver
        "com.android.cellbroadcastreceiver.overlay.common", // [Emergency & Safety] In code I found unused things: show_brazil_settings, show_cmas_setting
        "com.android.cellbroadcastservice", // [Emergency & Safety] is designed to deliver messages to multiple users in an area. This is 
        "com.android.emergency.basiccolorblack.overlay", // [Emergency & Safety] Dark theme for Emergency rescue?
        "com.android.emergency.basiccolorwhite.overlay", // [Emergency & Safety] Dark theme for Emergency rescue?
        "com.android.server.telecom.overlay.common", // [Telephony & Telecom] Location and dialer things.
        "com.google.android.cellbroadcastreceiver", // [Emergency & Safety] Wireless emergency alerts Cell broadcast is designed to deliver messag
        "com.google.android.cellbroadcastreceiver.overlay.miui", // [Emergency & Safety] Disable opt out dialog to cellbroadcastreceiver? I never seen that. Un
        "com.google.android.cellbroadcastservice.overlay.miui", // [Emergency & Safety] cross sim duplicate detection disable? I never seen this. Unused.
        "com.google.android.cellbroadcastservice", // [Emergency & Safety] Cell broadcast is designed to deliver messages to multiple users in an
        "com.google.android.overlay.modules.captiveportallogin.forframework", // [Captive Portal & System Dialogs] Configs default captiveportallogin. (not needed) No effects after remo
        "com.google.android.overlay.modules.cellbroadcastservice", // [Emergency & Safety] 
        "com.google.android.overlay.modules.documentsui", // [Media Picker & Documents] WARNING: Uninstalling it breaks the Scheduled power on/off page of the
        "com.android.server.telecom.overlay.miui", // [Telephony & Telecom] Manage calls it's used for manage calls ui. it's important overlay to 
        "com.google.android.photopicker", // [Media Picker & Documents] Photo picker Provides a browsable interface that presents the user wit
        "com.android.cellbroadcastreceiver.overlay.base.s600ww", // [Emergency & Safety] Nokia theme overlay for com.android.cellbroadcastreceiver
        "com.android.providers.media.module", // [Media Picker & Documents] Media picker module Not recommended to remove this as it can break app
        "com.google.android.wfcactivation", // [Emergency & Safety] Carrier Setup Needed for IMS, WiFi calling and have emergency things.
        "com.android.cellbroadcastreceiver.overlay.pixel", // [Emergency & Safety] Useless code to cellbroadcastreceiver
        "com.android.cellbroadcastservice.overlay.pixel", // [Emergency & Safety] Useless code to cellbroadcastservice
        "com.google.android.documentsui.theme.pixel", // [Media Picker & Documents] Useless code to documentsui.
        "com.android.captiveportallogin.overlay", // [Captive Portal & System Dialogs] it's overlay only to icon color.
        "com.android.server.telecom.auto_generated_rro_product__", // [Telephony & Telecom] Configs to server telecom auto generated but better keep.
        "com.google.android.documentsui.icon_overlay", // [Media Picker & Documents] Another useless icon overlay.
        "com.google.android.cellbroadcastreceiver.rro_common", // [Emergency & Safety] Useless overlay code for cellbroadcastreceiver
        "com.google.android.cellbroadcastservice.rro_common", // [Emergency & Safety] Useless overlay code for cellbroadcastservice
        "com.android.cellbroadcast.overlay", // [Emergency & Safety] Unused overlay without code. Only for Test message.
        "com.android.cellbroadcastservice.overlay", // [Emergency & Safety] Unused overlay. Cross-SIM duplicate detection false.
        "com.android.ztescreenshot", // [Screenshot & Markup] ScreenCapture Needed for screenshots.
        "com.google.android.cellbroadcastreceiver.overlay", // [Emergency & Safety] CellBroadcastReceiver overlay. Overlays work by mapping resources defi
        "com.google.android.cellbroadcastservice.overlay", // [Emergency & Safety] CellBroadcastService overlay. Overlays work by mapping resources defin
        "com.google.android.configupdater", // [Emergency & Safety] Config Update Intents used to provide unbundled updates of system data
        "com.google.android.markup", // [Screenshot & Markup] Google Markup app made for modifying pictures, shipped by default on e
        "com.google.android.marvin.talkback", // [Accessibility Services] Android Accessibility Suite (https://play.google.com/store/apps/detail
        "com.google.android.printservice.recommendation", // [Print Spooler] I think this has to do with recommending a printservice app you can ge
        "com.google.marvin.talkback", // [Accessibility Services] Android Accessibility Suite (https://play.google.com/store/apps/detail
        "com.google.android.dialer", // [Telephony & Telecom] Google Dialer (https://play.google.com/store/apps/details?id=com.googl
        "com.google.android.documentsui", // [Media Picker & Documents] Occasionally runs in the background. File selector for other apps. Sto
        "com.google.android.accessibility.soundamplifier", // [Accessibility Services] Accessibility sound amplifier (https://play.google.com/store/apps/deta
        "com.google.android.captiveportallogin", // [Captive Portal & System Dialogs] it's the same as (com.android.captiveportallogin). Support for captive
        "com.android.bips", // [Print Spooler] Disabling this can cause Settings app to crash and general device slow
        "com.google.android.overlay.modules.cellbroadcastreceiver", // [Emergency & Safety] Overlay (theme/notification) module for com.google.android.cellbroadca
        "com.google.android.apps.safetyhub", // [Emergency & Safety] Personal Safety (https://play.google.com/store/apps/details?id=com.goo
        "com.android.wifi.dialog", // [Captive Portal & System Dialogs] Needed for wifi dialogs. Can brick basic functionality android.
        "com.android.overlay.gmstelecomm", // [Telephony & Telecom] Overlay app to 'com.google.android.dialer', 'com.android.incallui'.
        "com.android.systemui.accessibility.accessibilitymenu", // [Accessibility Services] Hidden menu that only shows 2 buttons: Large buttons - that increases 
        "com.android.bluetooth.oplus.overlay", // [Core Hardware Prompts] Overlay to 'com.android.bluetooth'.
        "com.android.qns", // [Emergency & Safety] Apn service, emergency calling, not sure how useful this app is.
        "com.android.overlay.dialer", // [Telephony & Telecom] Overlay to 'com.android.dialer' png icons.
        "com.android.overlay.emergency", // [Emergency & Safety] Overlay to 'com.android.emergency' icons png.
        "com.android.overlay.stk", // [Telephony & Telecom] Overlay to 'com.android.stk' icons png.
        "com.android.stk.overlay.miui", // [Telephony & Telecom] 'SIM Toolkit' name app only found.
        "com.google.android.accessibility.switchaccess", // [Accessibility Services] Switch Access https://play.google.com/store/apps/details?id=com.google
        "com.android.systemui.accessibility.accessibilitymenu.auto_generated_rro_product__", // [Accessibility Services] Product RRO for Accessibility menu.
        "com.google.android.wifi.dialog", // [Captive Portal & System Dialogs] Wi-Fi dialog. App to launch user dialogs requested by the Wi-Fi servic
        "com.android.sos", // [Emergency & Safety] Emergency SOS app for quick access to emergency services and contacts.
    }));

    // ── Samsung Electronics (One UI) (28 packages) ──
    public static final Set<String> SAMSUNG_ONEUI_PACKAGES = new HashSet<>(Arrays.asList(new String[] {
        "com.samsung.android.app.talkback", // [Accessibility Services] Voice assistant. Accessibility feature Screen Reader to provide audibl
        "com.samsung.android.aware.service", // [Share Sheet & Intent Resolver] Samsung Quick Share Agent Use Wifi direct to share files between 2 Sam
        "com.samsung.android.app.sharelive", // [Share Sheet & Intent Resolver] Samsung Quick Share (from the Galaxy Store) Breaks Quick Share if you 
        "com.samsung.android.mdx.kit", // [Share Sheet & Intent Resolver] Quick Share Connectivity / MDE Service Framework MDX = Multi Devices E
        "com.samsung.knox.keychain", // [Captive Portal & System Dialogs] Knox Key Chain Allows apps to sign data using system-wide private key/
        "com.sec.android.app.safetyassurance", // [Emergency & Safety] Safety assurance is related to emergency features. It is especially us
        "com.sec.android.mimage.gear360editor", // [Screenshot & Markup] 360 Photo Editor Lets you edit the 360-degree photos you took.
        "com.samsung.android.app.smartcapture", // [Screenshot & Markup] Samsung Capture Samsung's implementation of screenshot. Show the botto
        "com.samsung.android.app.smartwidget", // [Screenshot & Markup] Samsung screenshot You will still be able to take screenshots without 
        "com.samsung.android.app.contacts", // [Emergency & Safety] Samsung Contacts Safe to debloat if you use another contacts app. NOTE
        "com.sec.android.emergencylauncher", // [Emergency & Safety] Samsung Launcher when in emergency(low battery) mode.
        "com.sec.android.emergencymode.service", // [Emergency & Safety] Emergency mode enables you to extend your device’s standby time when i
        "com.sec.android.mimage.photoretouching", // [Screenshot & Markup] Samsung Photo Editor Disabling this will disable the inbuilt photo edi
        "com.sec.android.provider.badge", // [Emergency & Safety] Provider for the notification badges (which are not very useful IMHO) 
        "com.sec.android.provider.emergencymode", // [Emergency & Safety] Provider for emergency mode (com.sec.android.emergencylauncher) Conten
        "com.samsung.android.incallui", // [Telephony & Telecom] UI when\"being called/in call\". It's basically the screen that shows 
        "com.samsung.android.dialer", // [Telephony & Telecom] Stock Samsung Phone dialer
        "com.sec.android.autodoodle.service", // [Screenshot & Markup] Samsung Auto Doodle for Photo Editor https://galaxystore.samsung.com/p
        "com.samsung.android.accessibility.talkback", // [Accessibility Services] Samsung TalkBack in accessibility. https://galaxystore.samsung.com/pre
        "com.samsung.android.watch.watchface.emergency", // [Emergency & Safety] Watchface in the emergency launcher.
        "com.samsung.android.watch.screencapture", // [Screenshot & Markup] Provides the ability to take screenshots from the smart watch.
        "com.samsung.emergencyreporthelper", // [Emergency & Safety] Only for KOREA emergency report. Also no activities, only warn.
        "com.samsung.android.emergency", // [Emergency & Safety] Emergency SOS Has a lot of permissions, lets you send SOS messages and
        "com.samsung.android.mtp", // [Core Hardware Prompts] MTP application It doesn't cause a bootloop, but its highly not recomm
        "com.sec.android.app.wallpaperchooser", // [Share Sheet & Intent Resolver] This app provides the functionality to select and manage wallpapers on
        "com.samsung.android.app.scrollcapture", // [Screenshot & Markup] Samsung's app for taking screenshots.
        "com.sec.android.app.SmartClipService", // [Screenshot & Markup] Default image editor on older Samsung devices.
        "com.sec.android.directshare", // [Share Sheet & Intent Resolver] Part of Samsung's SBeam service. SBeam has known security vulnerabilit
    }));

    // ── Xiaomi, Redmi & POCO (MIUI / HyperOS) (4 packages) ──
    public static final Set<String> XIAOMI_MIUI_HYPEROS_PACKAGES = new HashSet<>(Arrays.asList(new String[] {
        "com.miui.gallery", // [Screenshot & Markup] MIUI Gallery app. Note: Removing the Gallery will break the send scree
        "com.miui.screenshot", // [Screenshot & Markup] MIUI Screenshot Screenshots will not work.
        "com.miui.mishare.connectivity", // [Share Sheet & Intent Resolver] Mi Share Unified file sharing service between Xiaomi, Oppo, Realme and
        "com.miui.mediaeditor", // [Screenshot & Markup] Xiaomi Gallery Editor Extension for MIUI Gallery that's used to edit p
    }));

    // ── Oppo, OnePlus & Realme (ColorOS / OxygenOS) (14 packages) ──
    public static final Set<String> OPPO_ONEPLUS_REALME_COLOROS_PACKAGES = new HashSet<>(Arrays.asList(new String[] {
        "com.oneplus.android.cellbroadcast.overlay", // [Emergency & Safety] Wireless emergency alerts Theme pack Guessing it's a pack of themes fo
        "com.oneplus.screenshot", // [Screenshot & Markup] Needed for Power + Volume Down screenshot.
        "com.oneplus.commonoverlay.com.google.android.networkstack", // [Captive Portal & System Dialogs] Needed for DHCP hostname?, captive portal HTTP URLs?
        "com.oneplus.commonoverlay.com.google.android.networkstack.cn", // [Captive Portal & System Dialogs] Needed for DHCP hostname?, captive portal HTTP URLs? Generate_204 Chin
        "com.oneplus.commonoverlay.com.android.networkstack.inprocess", // [Captive Portal & System Dialogs] Needed for DHCP hostname?, captive portal HTTP URLs?
        "com.oneplus.commonoverlay.com.android.networkstack.inprocess.cn", // [Captive Portal & System Dialogs] Needed for DHCP hostname?, captive portal HTTP URLs? Generate_204 Chin
        "com.coloros.screenshot", // [Screenshot & Markup] Screenshot Needed for screenshots. Very useful app.
        "com.oplus.android.overlay.modules.documentsui", // [Media Picker & Documents] Useless overlay to documentsui bools.xml: is_launcher_enabled true
        "com.coloros.oshare", // [Screenshot & Markup] Oppo Share File sharing app to transfer data from/to Oppo devices only
        "com.oplus.sos", // [Emergency & Safety] Emergency Alert service by clicking power button 5 times. It will auto
        "com.oppo.sos", // [Emergency & Safety] Emergency Alert service by clicking power button 5 times. It will auto
        "com.oplus.exsystemservice", // [Screenshot & Markup] Has a lot of permissions. The screenshot function will stop working wh
        "com.oneplus.dialer", // [Telephony & Telecom] OnePlus Dialer used in OxygenOS 11 and lower. Note: don't forget to do
        "com.oplus.screenshot", // [Screenshot & Markup] Screenshot Needed for screenshots. Very useful app.
    }));

    // ── Vivo & iQOO (FuntouchOS / OriginOS) (2 packages) ──
    public static final Set<String> VIVO_IQOO_FUNTOUCH_PACKAGES = new HashSet<>(Arrays.asList(new String[] {
        "com.vivo.easyshare", // [Share Sheet & Intent Resolver] Easy share Sharing apps to another phone. https://play.google.com/stor
        "com.vivo.smartshot", // [Screenshot & Markup] S-capture Used for screenshots?
    }));

    // ── Motorola / Lenovo (MyUX / HelloUI) (2 packages) ──
    public static final Set<String> MOTOROLA_MYUX_PACKAGES = new HashSet<>(Arrays.asList(new String[] {
        "com.motorola.screenshoteditor", // [Screenshot & Markup] Screenshot Editor Edit and share your screenshots right away, or take 
        "com.motorola.photoeditor", // [Screenshot & Markup] Photo Editor Moto Photo Editor.
    }));

    // ── Huawei & Honor (EMUI / MagicOS) (11 packages) ──
    public static final Set<String> HUAWEI_HONOR_EMUI_MAGICOS_PACKAGES = new HashSet<>(Arrays.asList(new String[] {
        "com.huawei.HwMultiScreenShot", // [Screenshot & Markup] Scrolling screenshot feature
        "com.huawei.printservice", // [Print Spooler] Print service
        "com.huawei.desktop.systemui", // [Screenshot & Markup] Huawei desktop mode switching It has also Take Screenshot Service. Req
        "com.huawei.sos", // [Emergency & Safety] SOS Emergency things.
        "com.huawei.lbs", // [Emergency & Safety] Location Based Services Location-based services (LBS) are applications
        "com.huawei.smartshot", // [Screenshot & Markup] Smart screenshots Needed for screenshots.
        "com.hihonor.keychain", // [Captive Portal & System Dialogs] Face recognition and fingerprint authentication. Probably a bad idea t
        "com.hihonor.HnMultiScreenShot", // [Screenshot & Markup] Scrolling screenshot feature
        "com.hihonor.smartshot", // [Screenshot & Markup] Smart screenshots Needed for screenshots.
        "com.hihonor.printservice", // [Print Spooler] Print service
        "com.hihonor.lbs", // [Emergency & Safety] Location Based Services Location-based services (LBS) are applications
    }));

    // ── Transsion Holdings (Infinix, Tecno, Itel - XOS / HiOS) (1 packages) ──
    public static final Set<String> TRANSSION_INFINIX_TECNO_XOS_PACKAGES = new HashSet<>(Arrays.asList(new String[] {
        "com.transsion.screencapture", // [Screenshot & Markup] Needed for screenshots.
    }));

    // ── Sony, ASUS, TCL, LG & Other OEMs (13 packages) ──
    public static final Set<String> SONY_ASUS_TCL_OTHERS_PACKAGES = new HashSet<>(Arrays.asList(new String[] {
        "com.sonyericsson.mtp.extension.factoryreset", // [Core Hardware Prompts] Sony phone says 'lock screen settings'. Removing it can cause bootloop
        "com.sonyericsson.photoeditor", // [Screenshot & Markup] Sony's photo editor: https://www.apkmirror.com/apk/sony-mobile-communi
        "com.sonymobile.cellbroadcast.notification", // [Emergency & Safety] Cell information Cell broadcast is designed to deliver messages to mul
        "com.tcl.screenshotex", // [Screenshot & Markup] 
        "com.tcl.sos", // [Emergency & Safety] 
        "com.asus.dialer", // [Telephony & Telecom] ASUS Dialer Contains Google trackers.
        "com.asus.cellbroadcastreceiver.overlay", // [Emergency & Safety] useless overlay for cellbroadcast
        "com.asus.cellbroadcastservice.overlay", // [Emergency & Safety] useless overlay for cellbroadcast
        "com.lge.icecontacts", // [Emergency & Safety] Emergency and video calling things.
        "com.lge.cmas", // [Emergency & Safety] Emergency Alerts it's only for alerts so it's safe to remove.
        "com.lge.nextcapture", // [Screenshot & Markup] Needed for screenshots?
        "cn.nubia.photoeditor", // [Screenshot & Markup] Photo Editor Photo Editor. It has Baidu location so better turn off ne
        "cn.nubia.supersnap", // [Screenshot & Markup] Super Snap Shot, Screenshot
    }));

    // ── Universal Core Framework & Carrier Telecom (16 packages) ──
    public static final Set<String> UNIVERSAL_CORE_FRAMEWORK_PACKAGES = new HashSet<>(Arrays.asList(new String[] {
        "com.orange.mysosh", // [Emergency & Safety] My Sosh (https://play.google.com/store/apps/details?id=com.orange.myso
        "com.sec.app.samsungprintservice", // [Print Spooler] Samsung Print Service Plugin. Published by HP, see: https://play.googl
        "com.qualcomm.qti.xrcb", // [Emergency & Safety] Receive xrcb network signals for radio side. it's about emergency aler
        "com.mediatek.cellbroadcastuiresoverlay", // [Emergency & Safety] This app have something to emergency.
        "asusims.overlay.aosp.documentsui", // [Media Picker & Documents] Not needed overlay for hide documentsui icon. No effects after remove.
        "asusims.overlay.documentsui", // [Media Picker & Documents] Not needed overlay for hide documentsui icon. No effects after remove.
        "com.realme.movieshot", // [Screenshot & Markup] Combine captions It's for partial screenshot, SaveLongshot.
        "com.tct.dialer", // [Telephony & Telecom] Phone Stock Phone app
        "com.tct.screenshotex", // [Screenshot & Markup] Screenshot it's used for screenshots and editing them.
        "com.qeexo.smartshot", // [Screenshot & Markup] Smart Screenshots Smart Screenshots? Disable it doesnt affecting norma
        "com.agui.newsos", // [Emergency & Safety] SOS Notify emergency contacts. When triggered, will also put the phone
        "com.agui.screenshot", // [Screenshot & Markup] Screenshot Screenshot utility triggered when double tapping the Red Bu
        "com.spreadtrum.csvt", // [Emergency & Safety] potentially riskware, (sends information to another infamously hacked 
        "com.epson.mobilephone.samsungprintservice", // [Print Spooler] Completely empty package. Useless.
        "com.hp.android.printservice", // [Print Spooler] Print to your HP printer using your Android phone. Most reviews on Goo
        "com.siso.app.genericprintservice", // [Print Spooler] Completely empty package. Useless.
    }));

    // ── Unified Standalone UAD-NG Allowlist ──
    public static final Set<String> UAD_SYSTEM_ALLOWLIST = new HashSet<>();

    static {
        UAD_SYSTEM_ALLOWLIST.addAll(AOSP_AND_GOOGLE_PACKAGES);
        UAD_SYSTEM_ALLOWLIST.addAll(SAMSUNG_ONEUI_PACKAGES);
        UAD_SYSTEM_ALLOWLIST.addAll(XIAOMI_MIUI_HYPEROS_PACKAGES);
        UAD_SYSTEM_ALLOWLIST.addAll(OPPO_ONEPLUS_REALME_COLOROS_PACKAGES);
        UAD_SYSTEM_ALLOWLIST.addAll(VIVO_IQOO_FUNTOUCH_PACKAGES);
        UAD_SYSTEM_ALLOWLIST.addAll(MOTOROLA_MYUX_PACKAGES);
        UAD_SYSTEM_ALLOWLIST.addAll(HUAWEI_HONOR_EMUI_MAGICOS_PACKAGES);
        UAD_SYSTEM_ALLOWLIST.addAll(TRANSSION_INFINIX_TECNO_XOS_PACKAGES);
        UAD_SYSTEM_ALLOWLIST.addAll(SONY_ASUS_TCL_OTHERS_PACKAGES);
        UAD_SYSTEM_ALLOWLIST.addAll(UNIVERSAL_CORE_FRAMEWORK_PACKAGES);
    }

    /**
     * Fast O(1) membership check against the UAD system allowlist.
     */
    public static boolean isUadSystemAllowed(String pkg) {
        if (pkg == null || pkg.isEmpty()) return false;
        return UAD_SYSTEM_ALLOWLIST.contains(pkg);
    }

    /**
     * Dynamic heuristic pattern check to catch unlisted OEM variations of
     * Emergency SOS, Screenshot Markup, Intent Resolvers, and Captive Portals.
     */
    public static boolean isUadSystemPattern(String pkg, String appLabel) {
        if (pkg == null) return false;
        String lowerPkg = pkg.toLowerCase(Locale.US);

        // Emergency & Life Safety patterns
        if (lowerPkg.contains(".emergency") || lowerPkg.endsWith(".emergency") ||
            lowerPkg.contains(".sos") || lowerPkg.endsWith(".sos") ||
            lowerPkg.contains("safetyhub") || lowerPkg.contains("safetycore") ||
            lowerPkg.contains("emergencyalert") || lowerPkg.contains(".cbr")) {
            return true;
        }

        // Screenshot, Markup & Photo Editors
        if (lowerPkg.contains(".markup") || lowerPkg.endsWith(".markup") ||
            lowerPkg.contains("screenshot") || lowerPkg.contains("smartcapture") ||
            lowerPkg.contains("photoeditor") || lowerPkg.contains("screenshoteditor") ||
            lowerPkg.contains("mediaeditor")) {
            return true;
        }

        // Share Sheets & Intent Resolvers
        if (lowerPkg.contains("intentresolver") || lowerPkg.contains("chooser") ||
            lowerPkg.contains("sharelive") || lowerPkg.contains("mishare") ||
            lowerPkg.contains("directshare") || lowerPkg.contains("easyshare") ||
            lowerPkg.contains("oshare")) {
            return true;
        }

        // Captive portal & Wi-Fi system dialogs
        if (lowerPkg.contains("captiveportallogin") || lowerPkg.contains("vpndialogs") ||
            lowerPkg.contains("certinstaller") || lowerPkg.contains("wifi.dialog") ||
            lowerPkg.contains("photopicker") || lowerPkg.contains("printspooler")) {
            return true;
        }

        // App Label semantic inspection
        if (appLabel != null && !appLabel.trim().isEmpty()) {
            String lowerLabel = appLabel.toLowerCase(Locale.US);
            if (lowerLabel.equals("emergency") || lowerLabel.contains("emergency alert") ||
                lowerLabel.contains("emergency sos") || lowerLabel.contains("personal safety") ||
                lowerLabel.contains("markup") || lowerLabel.contains("photo editor") ||
                lowerLabel.contains("screenshot") || lowerLabel.contains("intent resolver") ||
                lowerLabel.contains("captive portal") || lowerLabel.contains("print spooler")) {
                return true;
            }
        }

        return false;
    }

    public static boolean isUadSystemAllowed(String pkg, String appLabel) {
        return isUadSystemAllowed(pkg) || isUadSystemPattern(pkg, appLabel);
    }
}

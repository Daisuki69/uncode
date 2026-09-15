import { AllowedApp } from '../types';

/**
 * Known messaging application package IDs.
 * On initial setup, any of these apps found installed on the user's device
 * are automatically added to the user's allowed apps whitelist by default.
 * Users can freely remove them from the Dashboard.
 */
export const KNOWN_MESSAGING_PACKAGES: string[] = [
  'com.whatsapp',
  'com.whatsapp.w4b',
  'org.telegram.messenger',
  'org.thunderdog.challegram',
  'org.telegram.plus',
  'com.google.android.apps.messaging',
  'com.samsung.android.messaging',
  'com.android.mms',
  'com.facebook.orca',
  'com.facebook.mlite',
  'org.thoughtcrime.securesms', // Signal
  'com.tencent.mm',             // WeChat
  'jp.naver.line.android',      // LINE
  'com.viber.voip',             // Viber
  'com.apple.MobileSMS',        // iOS / simulated
];

const MESSAGING_SET = new Set(KNOWN_MESSAGING_PACKAGES.map(p => p.toLowerCase()));

export function isMessagingPackage(packageId?: string | null): boolean {
  if (!packageId) return false;
  const lower = packageId.trim().toLowerCase();
  return MESSAGING_SET.has(lower) || lower.includes('messaging') || lower.includes('sms');
}

/**
 * Known Web Browser packages.
 * These are hardcoded by system policy to always be permitted during lockdown.
 */
export const KNOWN_BROWSER_PACKAGES: string[] = [
  'com.android.chrome',
  'com.google.chrome',
  'org.mozilla.firefox',
  'com.sec.android.app.sbrowser',
  'com.microsoft.emmx',
  'com.opera.browser',
  'com.opera.mini.native',
  'com.brave.browser',
  'com.duckduckgo.mobile.android',
];

const BROWSER_SET = new Set(KNOWN_BROWSER_PACKAGES.map(p => p.toLowerCase()));

export function isBrowserPackage(packageId?: string | null): boolean {
  if (!packageId) return false;
  const lower = packageId.trim().toLowerCase();
  return BROWSER_SET.has(lower) || lower.includes('browser') || lower.includes('chrome');
}

/**
 * Known Music / Audio Player packages.
 * These are hardcoded by system policy to always be permitted during lockdown.
 */
export const KNOWN_MUSIC_PACKAGES: string[] = [
  'com.spotify.music',
  'com.google.android.apps.youtube.music',
  'com.apple.android.music',
  'com.amazon.mp3',
  'com.aspiro.tidal',
  'deezer.android.app',
  'com.soundcloud.android',
  'com.sec.android.app.music',
  'com.miui.player',
];

const MUSIC_SET = new Set(KNOWN_MUSIC_PACKAGES.map(p => p.toLowerCase()));

export function isMusicPackage(packageId?: string | null, appName?: string | null): boolean {
  if (isHiddenSystemExemptApp(packageId, appName)) return false;
  if (!packageId) return false;
  const lower = packageId.trim().toLowerCase();
  return MUSIC_SET.has(lower) || lower.includes('music');
}

/**
 * Known Camera application package IDs.
 * These are hardcoded by system policy to always be permitted during lockdown for homework photo proof.
 */
export const KNOWN_CAMERA_PACKAGES: string[] = [
  'com.android.camera',
  'com.android.camera2',
  'com.google.android.GoogleCamera',
  'com.sec.android.app.camera',
  'com.samsung.android.camera',
  'com.huawei.camera',
  'com.oppo.camera',
  'com.oneplus.camera',
  'com.xiaomi.camera',
  'com.motorola.camera',
  'com.motorola.camera2',
  'com.sonyericsson.android.camera',
  'org.lineageos.snap',
];

const CAMERA_SET = new Set(KNOWN_CAMERA_PACKAGES.map(p => p.toLowerCase()));

export function isCameraPackage(packageId?: string | null, appName?: string | null): boolean {
  if (isHiddenSystemExemptApp(packageId, appName)) return false;
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (CAMERA_SET.has(lowerId) || lowerId.includes('camera')) return true;
  }
  if (appName) {
    const lowerName = appName.trim().toLowerCase();
    if (lowerName === 'camera' || lowerName.includes('camera') || lowerName === 'kamera') return true;
  }
  return false;
}

/**
 * Known Keyboard / Input Method Editor (IME) package IDs.
 * These are system infrastructure exemptions that are always exempt natively,
 * but must NEVER be shown in the UI whitelisted apps section or modal.
 */
export const KNOWN_KEYBOARD_PACKAGES: string[] = [
  'com.google.android.inputmethod.latin', // Gboard
  'com.samsung.android.honeyboard',       // Samsung Keyboard
  'com.touchtype.swiftkey',              // Microsoft SwiftKey
  'com.touchtype.swiftkey.beta',
  'com.android.inputmethod.latin',        // AOSP Keyboard
  'com.miui.voiceassist',
  'com.sohu.inputmethod.sogou.xiaomi',
  'com.huawei.ohos.inputmethod',
  'com.oppo.keyboard',
  'com.coloros.keyboard',
  'com.vivo.keyboard',
  'com.syntellia.fleksy.keyboard',
  'org.pocketworkstation.pckeyboard',
  'org.dslul.openboard.inputmethod.latin',
  'com.menny.android.anysoftkeyboard',
  'com.grammarly.android.keyboard',
  'com.baidu.input',
  'com.sohu.inputmethod.sogou',
  'com.google.android.tts',
];

const KEYBOARD_SET = new Set(KNOWN_KEYBOARD_PACKAGES.map(p => p.toLowerCase()));

export function isKeyboardPackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (KEYBOARD_SET.has(lowerId) || 
        lowerId.includes('inputmethod') || 
        lowerId.includes('honeyboard') || 
        lowerId.includes('keyboard') || 
        lowerId.includes('gboard') || 
        lowerId.includes('swiftkey') ||
        lowerId.includes('fleksy') ||
        lowerId.includes('openboard') ||
        lowerId.includes('.ime')) {
      return true;
    }
  }
  if (appName) {
    const lowerName = appName.trim().toLowerCase();
    if (lowerName.includes('keyboard') || 
        lowerName.includes('gboard') || 
        lowerName.includes('swiftkey') || 
        lowerName.includes('input method') ||
        lowerName.includes('keypad')) {
      return true;
    }
  }
  return false;
}

/**
 * Known 2-Factor Authentication (2FA) / OTP Authenticator package IDs.
 * These are hardcoded by system policy to always be permitted during lockdown
 * so students are never locked out of school portals, Google Classroom, Canvas, or university email.
 */
export const KNOWN_AUTHENTICATOR_PACKAGES: string[] = [
  'com.google.android.apps.authenticator2', // Google Authenticator
  'com.azure.authenticator',                // Microsoft Authenticator
  'com.duosecurity.duomobile',              // Duo Mobile
  'com.authy.authy',                        // Twilio Authy
  'com.twofasapp',                          // 2FAS Authenticator
  'com.beemdevelopment.aegis',              // Aegis Authenticator
  'com.bitwarden.authenticator',            // Bitwarden Authenticator
  'com.lastpass.authenticator',             // LastPass Authenticator
  'org.fedorahosted.freeotp',               // FreeOTP
  'com.yubico.yubioath',                    // Yubico Authenticator
];

const AUTHENTICATOR_SET = new Set(KNOWN_AUTHENTICATOR_PACKAGES.map(p => p.toLowerCase()));

export function isAuthenticatorPackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
      AUTHENTICATOR_SET.has(lowerId) || 
      lowerId.includes('authenticator') || 
      lowerId.includes('twofas') || 
      lowerId.includes('duomobile') || 
      lowerId.includes('yubioath')
    ) {
      return true;
    }
  }
  if (appName) {
    const lowerName = appName.trim().toLowerCase();
    if (lowerName.includes('authenticator') || lowerName.includes('2fa') || lowerName.includes('otp')) {
      return true;
    }
  }
  return false;
}

/**
 * Known Note-Taking application package IDs.
 * Hardcoded by system policy to always be permitted during lockdown.
 */
export const KNOWN_NOTES_PACKAGES: string[] = [
  'com.google.android.keep',                              // Google Keep
  'com.samsung.android.app.notes',                        // Samsung Notes
  'com.microsoft.office.onenote',                         // Microsoft OneNote
  'notion.id',                                            // Notion
  'md.obsidian',                                          // Obsidian
  'com.evernote',                                         // Evernote
  'com.automattic.simplenote',                             // Simplenote
  'com.socialnmobile.dictapps.notepad.color.note',        // ColorNote
  'com.zoho.notebook',                                    // Zoho Notebook
  'com.steadfastinnovation.android.pencilbook',           // Squid
  'com.goodnotes.goodnotes',                              // Goodnotes
  'com.myscript.nebo',                                    // Nebo
  'com.upnote.app',                                       // UpNote
  'com.standardnotes',                                    // Standard Notes
  'com.fluidtouch.noteshelf3',                            // Noteshelf
];

const NOTES_SET = new Set(KNOWN_NOTES_PACKAGES.map(p => p.toLowerCase()));

export function isNotesPackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
      NOTES_SET.has(lowerId) ||
      lowerId.includes('keep') ||
      lowerId.includes('onenote') ||
      lowerId.includes('obsidian') ||
      lowerId.includes('notion') ||
      lowerId.includes('notepad') ||
      lowerId.includes('.notes') ||
      lowerId.includes('memo') ||
      lowerId.includes('journal')
    ) {
      return true;
    }
  }
  if (appName) {
    const lowerName = appName.trim().toLowerCase();
    if (
      lowerName.includes('note') ||
      lowerName.includes('keep') ||
      lowerName.includes('memo') ||
      lowerName.includes('journal') ||
      lowerName.includes('obsidian') ||
      lowerName.includes('notion')
    ) {
      return true;
    }
  }
  return false;
}

/**
 * Known Student, Classroom, Drive & Academic application package IDs.
 * Hardcoded by system policy to always be permitted during lockdown.
 */
export const KNOWN_STUDENT_PACKAGES: string[] = [
  'com.google.android.apps.classroom',                    // Google Classroom
  'com.google.android.apps.docs',                         // Google Drive
  'com.google.android.apps.docs.editors.docs',            // Google Docs
  'com.google.android.apps.docs.editors.sheets',          // Google Sheets
  'com.google.android.apps.docs.editors.slides',          // Google Slides
  'com.google.android.apps.pdfviewer',                    // Google PDF Viewer
  'com.instructure.cstudent',                             // Canvas Student
  'com.instructure.cancan',                               // Canvas Teacher
  'com.blackboard.android.bbstudent',                     // Blackboard Learn
  'com.schoology.app',                                    // Schoology
  'com.quizlet.quizletandroid',                           // Quizlet
  'com.ichi2.anki',                                       // AnkiDroid
  'co.brainly',                                           // Brainly
  'com.photomath.android',                                // Photomath
  'com.desmos.calculator',                                // Desmos Graphing Calculator
  'org.geogebra.android',                                 // GeoGebra
  'com.wolfram.android.alpha',                            // WolframAlpha
  'com.microsoft.office.officehubrow',                    // Microsoft 365 (Office)
  'com.microsoft.office.word',                            // Microsoft Word
  'com.microsoft.office.excel',                           // Microsoft Excel
  'com.microsoft.office.powerpoint',                      // Microsoft PowerPoint

  // ── Document Scanners & PDF Worksheets ──
  'com.adobe.reader',                                     // Adobe Acrobat Reader
  'com.adobe.scan.android',                               // Adobe Scan
  'com.intsig.camscanner',                                // CamScanner
  'cn.wps.moffice_eng',                                   // WPS Office
  'com.microsoft.office.officelens',                      // Microsoft Lens
  'org.readera',                                          // ReadEra (PDF/EPUB Reader)
  'com.xodo.pdf.reader',                                  // Xodo PDF Reader
  'com.foxit.mobile.pdf.lite',                            // Foxit PDF Reader

  // ── Translation & Language Learning ──
  'com.google.android.apps.translate',                    // Google Translate
  'com.deepl.mobiletranslator',                           // DeepL Translate
  'com.duolingo',                                         // Duolingo
  'com.merriamwebster',                                   // Merriam-Webster Dictionary
  'com.mobisystems.msdict.embedded.wireless.oxford.dictionaryofenglish', // Oxford Dictionary
  'org.cambridge.cclae',                                  // Cambridge Dictionary

  // ── STEM Homework Solvers & Learning Hubs ──
  'org.khanacademy.android',                              // Khan Academy
  'com.devsense.symbolab',                                // Symbolab Math Solver
  'com.bagatrix.mathway.android',                         // Mathway
  'com.chegg',                                            // Chegg Study
  'org.brilliant.android',                                // Brilliant
  'mendeleev.redlime',                                    // Periodic Table 2024
  'com.cymath.cymath',                                    // Cymath

  // ── Cloud Storage & Sync ──
  'com.microsoft.skydrive',                               // Microsoft OneDrive
  'com.dropbox.android',                                  // Dropbox
  'net.box.android',                                      // Box

  // ── CS & Coding Environments ──
  'com.termux',                                           // Termux
  'com.foxdebug.acode',                                   // Acode (Code Editor)
  'ru.iiec.pydroid3',                                     // Pydroid 3 (Python IDE)
  'com.github.android',                                   // GitHub
];

const STUDENT_SET = new Set(KNOWN_STUDENT_PACKAGES.map(p => p.toLowerCase()));

export function isStudentPackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
      STUDENT_SET.has(lowerId) ||
      lowerId.includes('classroom') ||
      lowerId.includes('canvas') ||
      lowerId.includes('blackboard') ||
      lowerId.includes('schoology') ||
      lowerId.includes('quizlet') ||
      lowerId.includes('anki') ||
      lowerId.includes('desmos') ||
      lowerId.includes('geogebra') ||
      lowerId.includes('calculator') ||
      lowerId.includes('docs.editors') ||
      lowerId.includes('adobe.reader') ||
      lowerId.includes('camscanner') ||
      lowerId.includes('translate') ||
      lowerId.includes('deepl') ||
      lowerId.includes('duolingo') ||
      lowerId.includes('khanacademy') ||
      lowerId.includes('symbolab') ||
      lowerId.includes('mathway') ||
      lowerId.includes('chegg') ||
      lowerId.includes('termux') ||
      lowerId.includes('pydroid') ||
      lowerId.includes('skydrive') ||
      lowerId.includes('dropbox') ||
      lowerId.includes('readera')
    ) {
      return true;
    }
  }
  if (appName) {
    const lowerName = appName.trim().toLowerCase();
    if (
      lowerName.includes('classroom') ||
      lowerName.includes('canvas') ||
      lowerName.includes('blackboard') ||
      lowerName.includes('drive') ||
      lowerName.includes('docs') ||
      lowerName.includes('sheets') ||
      lowerName.includes('slides') ||
      lowerName.includes('quizlet') ||
      lowerName.includes('calculator') ||
      lowerName.includes('calc') ||
      lowerName.includes('acrobat') ||
      lowerName.includes('scanner') ||
      lowerName.includes('translate') ||
      lowerName.includes('dictionary') ||
      lowerName.includes('duolingo') ||
      lowerName.includes('khan academy') ||
      lowerName.includes('symbolab') ||
      lowerName.includes('mathway') ||
      lowerName.includes('chegg') ||
      lowerName.includes('termux') ||
      lowerName.includes('onedrive') ||
      lowerName.includes('dropbox') ||
      lowerName.includes('readera') ||
      lowerName.includes('wps office')
    ) {
      return true;
    }
  }
  return false;
}

/**
 * Known AI Assistant application package IDs.
 * Hardcoded by system policy to always be permitted during lockdown.
 */
export const KNOWN_AI_PACKAGES: string[] = [
  'com.google.android.apps.bard',                         // Google Gemini (Bard)
  'com.google.android.apps.gemini',                       // Google Gemini
  'com.google.android.apps.googleassistant',              // Google Assistant / Gemini
  'com.openai.chatgpt',                                   // ChatGPT
  'com.anthropic.claude',                                 // Claude
  'com.microsoft.copilot',                                // Microsoft Copilot
  'ai.perplexity.app.android',                            // Perplexity AI
  'com.poe.android',                                      // Poe by Quora
  'com.quora.poe.android',
  'com.deepseek.chat',                                    // DeepSeek
  'ai.inflection.pi',                                     // Pi AI
];

const AI_SET = new Set(KNOWN_AI_PACKAGES.map(p => p.toLowerCase()));

export function isAiPackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
      AI_SET.has(lowerId) ||
      lowerId.includes('chatgpt') ||
      lowerId.includes('gemini') ||
      lowerId.includes('claude') ||
      lowerId.includes('copilot') ||
      lowerId.includes('perplexity') ||
      lowerId.includes('deepseek')
    ) {
      return true;
    }
  }
  if (appName) {
    const lowerName = appName.trim().toLowerCase();
    if (
      lowerName.includes('chatgpt') ||
      lowerName.includes('gemini') ||
      lowerName.includes('claude') ||
      lowerName.includes('copilot') ||
      lowerName.includes('perplexity') ||
      lowerName.includes('deepseek')
    ) {
      return true;
    }
  }
  return false;
}

/**
 * Known Document Pickers, Media Module Providers, and OEM File Managers.
 * These are hardcoded system exemptions that are completely exempt from blocking,
 * but hidden from the user-facing Allowed Apps UI so homework uploads and file pickers work invisibly.
 */
export const KNOWN_DOCUMENT_PICKER_PACKAGES: string[] = [
  'com.google.android.documentsui',
  'com.android.documentsui',
  'com.google.android.providers.media.module',
  'com.android.providers.media',
  'com.sec.android.app.myfiles',
  'com.google.android.apps.nbu.files',
  'com.mi.android.globalfileexplorer',
  'com.coloros.filemanager',
  'com.oneplus.filemanager',
  'com.huawei.filemanager',
  'com.vivo.filemanager',
  'com.motorola.filemanager',
  'com.asus.filemanager',
];

const DOCUMENT_PICKER_SET = new Set(KNOWN_DOCUMENT_PICKER_PACKAGES.map(p => p.toLowerCase()));

export function isDocumentPickerPackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (DOCUMENT_PICKER_SET.has(lowerId) || lowerId.includes('documentsui')) {
      return true;
    }
  }
  if (appName) {
    const lowerName = appName.trim().toLowerCase();
    if (
      lowerName === 'files' ||
      lowerName === 'my files' ||
      lowerName === 'file manager' ||
      lowerName === 'documents' ||
      lowerName.includes('documentsui')
    ) {
      return true;
    }
  }
  return false;
}

/**
 * Known Package Installers and App Store package IDs.
 * These are system infrastructure exemptions that are always exempt natively
 * so manual APK installations, updates, and Play Store update calls are never blocked,
 * but hidden from the student's study apps grid.
 */
export const KNOWN_INSTALLER_AND_STORE_PACKAGES: string[] = [
  'com.android.vending',
  'com.google.android.feedback',
  'com.google.android.gms',
  'com.google.android.packageinstaller',
  'com.android.packageinstaller',
  'com.samsung.android.packageinstaller',
  'com.sec.android.app.samsungapps',
  'com.miui.packageinstaller',
  'com.xiaomi.mipicks',
  'com.coloros.packageinstaller',
  'com.oppo.packageinstaller',
  'com.heytap.market',
  'com.vivo.packageinstaller',
  'com.vivo.appstore',
  'com.transsion.packageinstaller',
  'com.huawei.appmarket',
  'com.hihonor.appmarket',
  'com.lenovo.safecenter',
];

const INSTALLER_SET = new Set(KNOWN_INSTALLER_AND_STORE_PACKAGES.map(p => p.toLowerCase()));

export function isInstallerOrStorePackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
      INSTALLER_SET.has(lowerId) ||
      lowerId.includes('packageinstaller') ||
      lowerId.endsWith('.packageinstaller') ||
      lowerId.includes('.installer')
    ) {
      return true;
    }
  }
  if (appName) {
    const lowerName = appName.trim().toLowerCase();
    if (
      lowerName === 'package installer' ||
      lowerName.includes('package installer') ||
      lowerName.includes('app installer') ||
      lowerName === 'google play store' ||
      lowerName === 'play store'
    ) {
      return true;
    }
  }
  return false;
}

/**
 * Returns true if an application is an internal OS infrastructure exemption
 * (keyboards, camera extension proxies, lens launchers, stub players, document pickers, package installers).
 * These apps are completely allowed during lockdown, but their icons are hidden from the UI.
 */
export function isHiddenSystemExemptApp(packageId?: string | null, appName?: string | null): boolean {
  if (isKeyboardPackage(packageId, appName)) return true;
  if (isDocumentPickerPackage(packageId, appName)) return true;
  if (isInstallerOrStorePackage(packageId, appName)) return true;

  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
      lowerId.includes('cameraextension') ||
      lowerId.includes('extensionproxy') ||
      lowerId.includes('lenslauncher') ||
      lowerId.includes('aperturelenslauncher') ||
      lowerId.includes('opensourcemusicplayer') ||
      lowerId.includes('androidopensourcemusicplayer')
    ) {
      return true;
    }
  }

  if (appName) {
    const lowerName = appName.trim().toLowerCase().replace(/\s+/g, '');
    if (
      lowerName.includes('cameraextensionproxy') ||
      lowerName.includes('cameraextension') ||
      lowerName.includes('lenslauncher') ||
      lowerName.includes('aperturelenslauncher') ||
      lowerName.includes('aperaturelenslauncher') ||
      lowerName.includes('androidopensourcemusicplayer') ||
      lowerName.includes('opensourcemusicplayer')
    ) {
      return true;
    }
  }

  return false;
}

/**
 * Returns true if an app is a displayable hardcoded exemption (browser, music player, camera, authenticator, notes, student apps, or AI assistants).
 * Internal infrastructure exemptions (keyboards, proxies, lens launchers) are strictly excluded.
 */
export function isHardcodedApp(app: AllowedApp): boolean {
  if (isHiddenSystemExemptApp(app.id, app.name)) return false;
  if (
    app.isHardcoded || 
    app.isBrowser || 
    app.isMusic || 
    app.isCamera || 
    app.isAuthenticator || 
    app.isNotes || 
    app.isStudentApp || 
    app.isAi
  ) {
    return true;
  }
  return isBrowserPackage(app.id) || 
         isMusicPackage(app.id, app.name) || 
         isCameraPackage(app.id, app.name) || 
         isAuthenticatorPackage(app.id, app.name) ||
         isNotesPackage(app.id, app.name) ||
         isStudentPackage(app.id, app.name) ||
         isAiPackage(app.id, app.name);
}

/**
 * Default fallback hardcoded apps with actual names and icons
 * used when no device apps are loaded or during web preview.
 * These are always positioned before custom allowed apps.
 */
export const DEFAULT_HARDCODED_APPS: AllowedApp[] = [
  { id: 'com.google.chrome', name: 'Chrome', iconName: 'Globe', isHardcoded: true, isBrowser: true },
  { id: 'com.spotify.music', name: 'Spotify', iconName: 'Music', isHardcoded: true, isMusic: true },
  { id: 'com.google.android.apps.youtube.music', name: 'YT Music', iconName: 'Music', isHardcoded: true, isMusic: true },
  { id: 'com.sec.android.app.camera', name: 'Camera', iconName: 'Camera', isHardcoded: true, isCamera: true },
  { id: 'com.google.android.apps.authenticator2', name: 'Authenticator', iconName: 'ShieldCheck', isHardcoded: true, isAuthenticator: true },
  { id: 'com.google.android.keep', name: 'Keep Notes', iconName: 'FileText', isHardcoded: true, isNotes: true },
  { id: 'com.google.android.apps.classroom', name: 'Classroom', iconName: 'BookOpen', isHardcoded: true, isStudentApp: true },
  { id: 'com.google.android.apps.docs', name: 'Drive', iconName: 'BookOpen', isHardcoded: true, isStudentApp: true },
  { id: 'com.openai.chatgpt', name: 'ChatGPT', iconName: 'Sparkles', isHardcoded: true, isAi: true },
  { id: 'com.google.android.apps.bard', name: 'Gemini', iconName: 'Sparkles', isHardcoded: true, isAi: true },
];

export const HARDCODED_SYSTEM_ALLOWED = DEFAULT_HARDCODED_APPS;


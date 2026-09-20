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
  'nekox.messenger',
  'org.forkgram.messenger',
  'org.telegram.BApplication',
  'com.facebook.orca',
  'com.facebook.mlite',
  'com.google.android.apps.messaging',
  'com.samsung.android.messaging',
  'com.android.mms',
  'org.thoughtcrime.securesms', // Signal
  'com.tencent.mm',             // WeChat
  'jp.naver.line.android',      // LINE
  'com.viber.voip',             // Viber
  'com.discord',
  'com.Slack',
  'com.apple.MobileSMS',        // iOS / simulated
];

const MESSAGING_SET = new Set(KNOWN_MESSAGING_PACKAGES.map(p => p.toLowerCase()));

export function isMessagingPackage(packageId?: string | null, appName?: string | null): boolean {
  if (!packageId) return false;
  const lower = packageId.trim().toLowerCase();
  // Exclude social media feeds explicitly
  if (lower.includes('katana') || lower.includes('instagram') || lower.includes('twitter') ||
      lower.includes('reddit') || lower.includes('tiktok') || lower.includes('musically')) {
    return false;
  }
  if (MESSAGING_SET.has(lower) ||
      lower.includes('messaging') ||
      lower.includes('sms') ||
      lower.includes('telegram') ||
      lower.includes('challegram') ||
      lower.includes('nekogram') ||
      lower.includes('nekox') ||
      lower.includes('forkgram') ||
      lower.includes('bgram') ||
      lower.includes('messenger') ||
      lower.includes('whatsapp') ||
      lower.includes('signal') ||
      lower.includes('viber') ||
      lower.includes('wechat') ||
      lower.includes('.line') ||
      lower.includes('orca')) {
    return true;
  }
  if (appName) {
    const lowerName = appName.trim().toLowerCase();
    if (lowerName.includes('facebook') || lowerName.includes('instagram') ||
        lowerName.includes('twitter') || lowerName.includes('reddit') || lowerName.includes('tiktok')) {
      return false;
    }
    if (lowerName.includes('telegram') || lowerName.includes('messenger') ||
        lowerName.includes('whatsapp') || lowerName.includes('signal') ||
        lowerName.includes('viber') || lowerName.includes('wechat') ||
        lowerName.includes('challegram') || lowerName.includes('nekogram')) {
      return true;
    }
  }
  return false;
}

// Legacy static arrays retained as empty aliases for backwards compatibility with any forks
export const KNOWN_BROWSER_PACKAGES: string[] = [];
export const KNOWN_MUSIC_PACKAGES: string[] = [];
export const KNOWN_CAMERA_PACKAGES: string[] = [];
export const KNOWN_KEYBOARD_PACKAGES: string[] = [];
export const KNOWN_AUTHENTICATOR_PACKAGES: string[] = [];
export const KNOWN_NOTES_PACKAGES: string[] = [];
export const KNOWN_STUDENT_PACKAGES: string[] = [];
export const KNOWN_AI_PACKAGES: string[] = [];
export const KNOWN_DOCUMENT_PICKER_PACKAGES: string[] = [];
export const KNOWN_INSTALLER_AND_STORE_PACKAGES: string[] = [];

/**
 * Web Browser detection helper.
 */
export function isBrowserPackage(packageId?: string | null): boolean {
  if (!packageId) return false;
  const lower = packageId.trim().toLowerCase();
  return lower.includes('browser') || lower.includes('chrome') || lower.includes('firefox') || lower.includes('opera');
}

/**
 * Music / Audio Player detection helper.
 */
export function isMusicPackage(packageId?: string | null, appName?: string | null): boolean {
  if (isHiddenSystemExemptApp(packageId, appName)) return false;
  if (packageId) {
    const lower = packageId.trim().toLowerCase();
    if (lower.includes('music') || lower.includes('spotify') || lower.includes('tidal') || lower.includes('deezer') || lower.includes('soundcloud')) {
      return true;
    }
  }
  if (appName) {
    const lowerName = appName.trim().toLowerCase();
    if (lowerName.includes('music') || lowerName.includes('spotify') || lowerName.includes('podcast')) return true;
  }
  return false;
}

/**
 * Camera application detection helper.
 */
export function isCameraPackage(packageId?: string | null, appName?: string | null): boolean {
  if (isHiddenSystemExemptApp(packageId, appName)) return false;
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (lowerId.includes('camera') || lowerId.includes('snap')) return true;
  }
  if (appName) {
    const lowerName = appName.trim().toLowerCase();
    if (lowerName === 'camera' || lowerName.includes('camera') || lowerName === 'kamera') return true;
  }
  return false;
}

/**
 * Keyboard / Input Method Editor (IME) detection helper (hidden system exemption).
 */
export function isKeyboardPackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
      lowerId.includes('inputmethod') || 
      lowerId.includes('honeyboard') || 
      lowerId.includes('keyboard') || 
      lowerId.includes('gboard') || 
      lowerId.includes('swiftkey') ||
      lowerId.includes('fleksy') ||
      lowerId.includes('openboard') ||
      lowerId.includes('.ime')
    ) {
      return true;
    }
  }
  if (appName) {
    const lowerName = appName.trim().toLowerCase();
    if (
      lowerName.includes('keyboard') || 
      lowerName.includes('gboard') || 
      lowerName.includes('swiftkey') || 
      lowerName.includes('input method') ||
      lowerName.includes('keypad')
    ) {
      return true;
    }
  }
  return false;
}

/**
 * 2-Factor Authentication (2FA) / OTP Authenticator detection helper.
 */
export function isAuthenticatorPackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
      lowerId.includes('authenticator') || 
      lowerId.includes('twofas') || 
      lowerId.includes('duomobile') || 
      lowerId.includes('yubioath') ||
      lowerId.includes('authy') ||
      lowerId.includes('freeotp')
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
 * Note-Taking application detection helper.
 */
export function isNotesPackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
      lowerId.includes('keep') ||
      lowerId.includes('onenote') ||
      lowerId.includes('obsidian') ||
      lowerId.includes('notion') ||
      lowerId.includes('notepad') ||
      lowerId.includes('.notes') ||
      lowerId.includes('memo') ||
      lowerId.includes('simplenote') ||
      lowerId.includes('colornote') ||
      lowerId.includes('goodnotes') ||
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
 * Student, Classroom, Drive & Academic application detection helper.
 */
export function isStudentPackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
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
      lowerId.includes('readera') ||
      lowerId.includes('photomath') ||
      lowerId.includes('wolfram') ||
      lowerId.includes('wps')
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
      lowerName.includes('anki') ||
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
 * AI Assistant application detection helper.
 */
export function isAiPackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
      lowerId.includes('chatgpt') ||
      lowerId.includes('gemini') ||
      lowerId.includes('claude') ||
      lowerId.includes('copilot') ||
      lowerId.includes('perplexity') ||
      lowerId.includes('deepseek') ||
      lowerId.includes('bard') ||
      lowerId.includes('poe.android')
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
 * Document Pickers, Media Module Providers, and OEM File Managers (hidden system exemption).
 */
export function isDocumentPickerPackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
      lowerId.includes('documentsui') ||
      lowerId.includes('providers.media') ||
      lowerId.includes('myfiles') ||
      lowerId.includes('filemanager') ||
      lowerId.includes('globalfileexplorer')
    ) {
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
 * Package Installers and App Store package detection (hidden system exemption).
 */
export function isInstallerOrStorePackage(packageId?: string | null, appName?: string | null): boolean {
  if (packageId) {
    const lowerId = packageId.trim().toLowerCase();
    if (
      lowerId.includes('packageinstaller') ||
      lowerId.endsWith('.packageinstaller') ||
      lowerId.includes('.installer') ||
      lowerId.includes('vending') ||
      lowerId.includes('appstore') ||
      lowerId.includes('appmarket') ||
      lowerId.includes('mipicks') ||
      lowerId.includes('palmstore')
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


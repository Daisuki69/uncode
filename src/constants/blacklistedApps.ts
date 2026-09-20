/**
 * Hardcoded blacklist of distracting applications.
 * Strictly prohibited from being whitelisted during system lockdown.
 */
export const HARDCODED_BLACKLISTED_PACKAGES: string[] = [
  // ── Stage 1 Anti-Tamper Shield ──
  'com.android.settings',
  'com.miui.securitycenter',
  'com.coloros.safecenter',
  'com.samsung.android.lool',
  'com.transsion.phonemaster',
  'com.vivo.safecenter',
  'com.iqoo.secure',
  'com.huawei.systemmanager',
  'com.google.android.apps.wellbeing',

  // ── Stage 1 Hardware Bloatware & Game Boosters ──
  'com.xiaomi.joyose',
  'com.miui.gamebooster',
  'com.samsung.android.game.gamehome',
  'com.oplus.games',
  'com.coloros.gamespace',
  'com.xiaomi.mipicks',
  'com.transsion.palmostore',
  'com.heytap.market',
  'com.glance.internet',
];

const BLACKLIST_SET = new Set(HARDCODED_BLACKLISTED_PACKAGES.map(p => p.toLowerCase()));

/**
 * Checks if a given package ID is in the hardcoded blacklist or matches known signature substrings.
 */
export function isAppBlacklisted(packageId?: string | null): boolean {
  if (!packageId) return false;
  const lower = packageId.trim().toLowerCase();
  if (BLACKLIST_SET.has(lower)) return true;

  // Heuristic substring signature checks to automatically block anti-tamper, cloners, and game boosters
  return lower.includes('settings') ||
         lower.includes('securitycenter') ||
         lower.includes('safecenter') ||
         lower.includes('phonemaster') ||
         lower.includes('joyose') ||
         lower.includes('gamecenter') ||
         lower.includes('gamebooster') ||
         lower.includes('palmstore') ||
         lower.includes('mipicks') ||
         lower.includes('parallel') ||
         lower.includes('dualspace') ||
         lower.includes('multispace') ||
         lower.includes('superclone') ||
         lower.includes('appcloner') ||
         lower.includes('2accounts') ||
         lower.includes('secondspace') ||
         lower.includes('calculatorvault') ||
         lower.includes('photovault') ||
         lower.includes('gallerylock') ||
         lower.includes('luckypatcher') ||
         lower.includes('gameguardian') ||
         lower.includes('happymod') ||
         lower.includes('acmarket') ||
         lower.includes('vmos') ||
         lower.includes('f1vm') ||
         lower.includes('vphonegaga') ||
         lower.includes('x8zs') ||
         lower.includes('teamviewer') ||
         lower.includes('anydesk') ||
         lower.includes('rustdesk') ||
         lower.includes('remotedesktop') ||
         lower.includes('screenshare') ||
         lower.includes('screenmirror') ||
         lower.includes('screenstream') ||
         lower.includes('airdroid') ||
         lower.includes('splashtop') ||
         lower.includes('nethunter') ||
         lower.includes('droidvnc') ||
         lower.includes('nhkex') ||
         lower.includes('tiktok') ||
         lower.includes('musically') ||
         lower.includes('trill') ||
         (lower.includes('youtube') && !lower.includes('music')) ||
         lower.includes('reddit');
}

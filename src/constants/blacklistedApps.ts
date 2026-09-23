/**
 * Hardcoded blacklist of Stage 1 Anti-Tamper and Hardware Bloatware applications.
 * Strictly prohibited from being whitelisted during system lockdown.
 * Consumer apps (YouTube, TikTok, Reddit, games, etc.) are dynamically classified
 * on-device by AppClassifier and KnownDistracting.
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
 * Checks if a package ID matches Stage 1 Anti-Tamper or hardware task killer vetoes.
 */
export function isAppBlacklisted(packageId?: string | null): boolean {
  if (!packageId) return false;
  const lower = packageId.trim().toLowerCase();
  if (BLACKLIST_SET.has(lower)) return true;

  // Anti-tamper settings and security manager substrings
  if (lower.includes('settings') ||
      lower.includes('securitycenter') ||
      lower.includes('safecenter') ||
      lower.includes('phonemaster') ||
      lower.includes('joyose') ||
      lower.includes('gamecenter') ||
      lower.includes('gamebooster') ||
      lower.includes('palmstore') ||
      lower.includes('mipicks')) {
    return true;
  }

  // Work profile / multi-user sandboxes that bypass accessibility monitoring
  if (lower.includes('parallel') ||
      lower.includes('dualspace') ||
      lower.includes('multispace') ||
      lower.includes('superclone') ||
      lower.includes('appcloner') ||
      lower.includes('2accounts') ||
      lower.includes('secondspace') ||
      lower.includes('island') ||
      lower.includes('shelter')) {
    return true;
  }

  return false;
}

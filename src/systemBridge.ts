import { registerPlugin } from '@capacitor/core';
import { AllowedApp } from './types';

interface LockPluginInterface {
  startLockdown(options: {
    allowedAppIds: string[];
    durationMinutes?: number;
    lockEndTime?: number;
    scheduleId?: string;
  }): Promise<void>;
  endLockdown(): Promise<void>;
  getInstalledApps(): Promise<{ apps: AllowedApp[] }>;
  checkPermissions(): Promise<{
    isDeviceOwner: boolean;
    isAccessibilityEnabled: boolean;
    isAdminActive: boolean;
    isBatteryOptimizationIgnored: boolean;
    isNotificationGranted: boolean;
    isExactAlarmGranted?: boolean;
    isAdbInstall?: boolean;
    installSource?: string;
  }>;
  openAccessibilitySettings(): Promise<void>;
  openDeviceAdminSettings(): Promise<void>;
  openDeviceAdminList(): Promise<void>;
  openAppInfo(): Promise<void>;
  requestBatteryOptimization(): Promise<void>;
  requestNotificationPermission(): Promise<void>;
  openNotificationSettings(): Promise<void>;
  exportBackup(options: { tempFileName: string; defaultName: string }): Promise<void>;
  syncSchedules(options: { schedules: any[]; allowedAppIds: string[] }): Promise<void>;
  syncTimeOffset(options: { timeOffset: number }): Promise<void>;
  setConsequenceActive(options: { active: boolean; scheduleId?: string }): Promise<void>;
  setOperatingMode(options: { mode: 'safemode' | 'hardcore' }): Promise<{ success: boolean }>;
  setWebProtectionMode(options: { mode: 'accessibility' | 'dns_vpn' | 'dual_hybrid' | 'off' }): Promise<{ success: boolean }>;
  setAllowYoutube(options: { allow: boolean }): Promise<{ success: boolean }>;
  requestVpnPermission(): Promise<{ granted: boolean }>;
  getLockStatus(): Promise<{
    isLockActive: boolean;
    lockEndTime: number;
    activeScheduleId?: string;
    isConsequenceActive?: boolean;
  }>;
  exitToHome(): Promise<void>;
  showToast(options: { message: string }): Promise<void>;
}

// Register the native plugin - falls back gracefully in browser/dev mode
const LockPlugin = registerPlugin<LockPluginInterface>('LockPlugin', {
  web: {
    exitToHome: async () => {
      console.log('[Dev] Simulating exitToHome');
    },
    showToast: async ({ message }: { message: string }) => {
      console.log('[Dev] Toast:', message);
    },
    startLockdown: async (opts: { allowedAppIds: string[]; durationMinutes?: number; lockEndTime?: number; scheduleId?: string }) => {
      console.log('[Dev] Simulating lockdown with:', opts);
    },
    endLockdown: async () => {
      console.log('[Dev] Simulating lockdown release');
    },
    syncSchedules: async (opts: { schedules: any[]; allowedAppIds: string[] }) => {
      console.log('[Dev] Simulating syncSchedules:', opts);
    },
    syncTimeOffset: async (opts: { timeOffset: number }) => {
      console.log('[Dev] Simulating syncTimeOffset:', opts);
    },
    setConsequenceActive: async (opts: { active: boolean; scheduleId?: string }) => {
      console.log('[Dev] Simulating setConsequenceActive:', opts);
    },
    setOperatingMode: async ({ mode }: { mode: 'safemode' | 'hardcore' }) => {
      console.log('[Dev] Simulating setOperatingMode:', mode);
      return { success: true };
    },
    setWebProtectionMode: async (opts: { mode: 'accessibility' | 'dns_vpn' | 'dual_hybrid' | 'off' }) => {
      console.log('[Dev] Simulating setWebProtectionMode:', opts);
      return { success: true };
    },
    setAllowYoutube: async (opts: { allow: boolean }) => {
      console.log('[Dev] Simulating setAllowYoutube:', opts);
      return { success: true };
    },
    requestVpnPermission: async () => {
      console.log('[Dev] Simulating requestVpnPermission: granted');
      return { granted: true };
    },
    getLockStatus: async () => ({
      isLockActive: false,
      lockEndTime: 0,
      activeScheduleId: undefined,
      isConsequenceActive: false,
    }),
    getInstalledApps: async () => ({
      apps: [
        { id: 'com.google.chrome', name: 'Chrome', iconName: 'Globe', isHardcoded: true, isBrowser: true },
        { id: 'com.spotify.music', name: 'Spotify', iconName: 'Music', isHardcoded: true, isMusic: true },
        { id: 'com.google.android.apps.youtube.music', name: 'YT Music', iconName: 'Music', isHardcoded: true, isMusic: true },
        { id: 'com.sec.android.app.camera', name: 'Camera', iconName: 'Camera', isHardcoded: true, isCamera: true },
        { id: 'com.whatsapp', name: 'WhatsApp', iconName: 'MessageSquare' },
        { id: 'org.telegram.messenger', name: 'Telegram', iconName: 'MessageSquare' },
        { id: 'com.apple.calculator', name: 'Calculator', iconName: 'Calculator' },
        { id: 'com.microsoft.word', name: 'Word', iconName: 'FileText' },
        { id: 'notion.id', name: 'Notion', iconName: 'BookOpen' },
        { id: 'ph.edu.ceu.studyhub', name: 'CEU Study Hub', iconName: 'BookOpen', isAutoAllowed: true }
      ]
    }),
    checkPermissions: async () => ({
      isDeviceOwner: true,
      isAccessibilityEnabled: true,
      isAdminActive: true,
      isBatteryOptimizationIgnored: true,
      isNotificationGranted: true,
      isExactAlarmGranted: true,
      isAdbInstall: true,
      installSource: 'ADB (PC Script / USB)',
    }), // Mock true for web dev
    openAccessibilitySettings: async () => console.log('[Dev] Opening Accessibility Settings'),
    openDeviceAdminSettings: async () => console.log('[Dev] Opening Device Admin Settings'),
    openDeviceAdminList: async () => console.log('[Dev] Opening Device Admin List'),
    openAppInfo: async () => console.log('[Dev] Opening App Info'),
    requestBatteryOptimization: async () => console.log('[Dev] Requesting Battery Optimization'),
    requestNotificationPermission: async () => console.log('[Dev] Requesting Notification Permission'),
    openNotificationSettings: async () => console.log('[Dev] Opening Notification Settings'),
    exportBackup: async (opts: { tempFileName: string; defaultName: string }) => console.log('[Dev] Exporting backup', opts),
  },
});

export const getInstalledApps = async (): Promise<AllowedApp[]> => {
  try {
    const result = await LockPlugin.getInstalledApps();
    return result.apps || [];
  } catch (e) {
    console.error('Failed to fetch native apps', e);
    return [];
  }
};

export const checkPermissions = async (): Promise<{
  isDeviceOwner: boolean;
  isAccessibilityEnabled: boolean;
  isAdminActive: boolean;
  isBatteryOptimizationIgnored: boolean;
  isNotificationGranted: boolean;
  isAdbInstall?: boolean;
  installSource?: string;
}> => {
  try {
    return await LockPlugin.checkPermissions();
  } catch (e) {
    console.error('Failed to check permissions', e);
    return {
      isDeviceOwner: false,
      isAccessibilityEnabled: false,
      isAdminActive: false,
      isBatteryOptimizationIgnored: false,
      isNotificationGranted: false,
      isAdbInstall: false,
      installSource: 'Unknown',
    };
  }
};

export const openAccessibilitySettings = async (): Promise<void> => {
  try {
    await LockPlugin.openAccessibilitySettings();
  } catch (e) {
    console.error('Failed to open settings', e);
  }
};

export const openDeviceAdminSettings = async (): Promise<void> => {
  try {
    await LockPlugin.openDeviceAdminSettings();
  } catch (e) {
    console.error('Failed to open device admin settings', e);
  }
};

export const openDeviceAdminList = async (): Promise<void> => {
  try {
    await LockPlugin.openDeviceAdminList();
  } catch (e) {
    console.error('Failed to open device admin list', e);
  }
};

export const openAppInfo = async (): Promise<void> => {
  try {
    await LockPlugin.openAppInfo();
  } catch (e) {
    console.error('Failed to open app info', e);
  }
};

export const requestBatteryOptimization = async (): Promise<void> => {
  try {
    await LockPlugin.requestBatteryOptimization();
  } catch (e) {
    console.error('Failed to request battery optimization', e);
  }
};

export const requestNotificationPermission = async (): Promise<void> => {
  try {
    await LockPlugin.requestNotificationPermission();
  } catch (e) {
    console.error('Failed to request notification permission', e);
  }
};

export const openNotificationSettings = async (): Promise<void> => {
  try {
    await LockPlugin.openNotificationSettings();
  } catch (e) {
    console.error('Failed to open notification settings', e);
  }
};

export const startLockdown = (
  allowedAppIds: string[],
  durationMinutes?: number,
  lockEndTime?: number,
  scheduleId?: string
) => {
  LockPlugin.startLockdown({ allowedAppIds, durationMinutes, lockEndTime, scheduleId }).catch(e => {
    console.error('startLockdown failed', e);
  });
};

export const endLockdown = () => {
  LockPlugin.endLockdown().catch(e => {
    console.error('endLockdown failed', e);
  });
};

export const syncSchedules = async (schedules: any[], allowedAppIds: string[]): Promise<void> => {
  try {
    await LockPlugin.syncSchedules({ schedules, allowedAppIds });
  } catch (e) {
    console.error('syncSchedules failed', e);
  }
};

export const syncTimeOffset = async (timeOffset: number): Promise<void> => {
  try {
    await LockPlugin.syncTimeOffset({ timeOffset });
  } catch (e) {
    console.error('syncTimeOffset failed', e);
  }
};

export const setConsequenceActive = async (active: boolean, scheduleId?: string): Promise<void> => {
  try {
    await LockPlugin.setConsequenceActive({ active, scheduleId });
  } catch (e) {
    console.error('setConsequenceActive failed', e);
  }
};

export const getLockStatus = async (): Promise<{
  isLockActive: boolean;
  lockEndTime: number;
  activeScheduleId?: string;
  isConsequenceActive?: boolean;
}> => {
  try {
    return await LockPlugin.getLockStatus();
  } catch (e) {
    console.error('getLockStatus failed', e);
    return { isLockActive: false, lockEndTime: 0, isConsequenceActive: false };
  }
};

export const exportBackup = async (tempFileName: string, defaultName: string): Promise<void> => {
  await LockPlugin.exportBackup({ tempFileName, defaultName });
};

export const exitToHome = async (): Promise<void> => {
  try {
    await LockPlugin.exitToHome();
  } catch (e) {
    console.error('exitToHome failed', e);
  }
};

export const showToast = async (message: string): Promise<void> => {
  try {
    await LockPlugin.showToast({ message });
  } catch (e) {
    console.log('[Toast fallback]', message);
  }
};

export const addBackListener = async (callback: () => void) => {
  return await LockPlugin.addListener('backPressed', callback);
};

export const setOperatingMode = async (mode: 'safemode' | 'hardcore'): Promise<boolean> => {
  try {
    const res = await LockPlugin.setOperatingMode({ mode });
    return res?.success ?? true;
  } catch (e) {
    console.error('setOperatingMode failed', e);
    return false;
  }
};

export const setWebProtectionMode = async (mode: 'accessibility' | 'dns_vpn' | 'dual_hybrid' | 'off'): Promise<boolean> => {
  try {
    const res = await LockPlugin.setWebProtectionMode({ mode });
    return res?.success ?? true;
  } catch (e) {
    console.error('setWebProtectionMode failed', e);
    return false;
  }
};

export const setAllowYoutube = async (allow: boolean): Promise<boolean> => {
  try {
    const res = await LockPlugin.setAllowYoutube({ allow });
    return res?.success ?? true;
  } catch (e) {
    console.error('setAllowYoutube failed', e);
    return false;
  }
};

export const requestVpnPermission = async (): Promise<boolean> => {
  try {
    const res = await LockPlugin.requestVpnPermission();
    return res?.granted ?? true;
  } catch (e) {
    console.error('requestVpnPermission failed', e);
    return false;
  }
};


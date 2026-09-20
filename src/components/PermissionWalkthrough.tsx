import React, { useState, useEffect, useRef } from 'react';
import { motion } from 'motion/react';
import { ShieldAlert, Settings, CheckCircle, Loader2, Terminal, ExternalLink, Copy, Check, ShieldCheck, Smartphone, Laptop, AlertTriangle, Info, ArrowRight, ListFilter, BatteryCharging, Bell, CheckCircle2, XCircle } from 'lucide-react';
import { checkPermissions, openAccessibilitySettings, openDeviceAdminSettings, openDeviceAdminList, openAppInfo, requestBatteryOptimization, requestNotificationPermission } from '../systemBridge';

interface PermissionWalkthroughProps {
  onComplete: () => void;
}

export function PermissionWalkthrough({ onComplete }: PermissionWalkthroughProps) {
  const [isChecking, setIsChecking] = useState(false);
  const [copied, setCopied] = useState(false);
  const [activeTab, setActiveTab] = useState<'device' | 'pc'>('device');
  const [showRestrictedHelp, setShowRestrictedHelp] = useState(false);
  const tabInitialized = useRef(false);

  const [perms, setPerms] = useState<{
    isAccessibilityEnabled: boolean;
    isAdminActive: boolean;
    isBatteryOptimizationIgnored: boolean;
    isNotificationGranted: boolean;
    isAdbInstall?: boolean;
    installSource?: string;
  }>({
    isAccessibilityEnabled: false,
    isAdminActive: false,
    isBatteryOptimizationIgnored: false,
    isNotificationGranted: false,
    isAdbInstall: false,
    installSource: undefined,
  });

  const gitHubUrl = 'https://github.com/Daisuki69/uncode';

  const verifyPermissions = async () => {
    setIsChecking(true);
    const result = await checkPermissions();
    setPerms(result);
    if (!tabInitialized.current) {
      setActiveTab(result.isAdbInstall ? 'pc' : 'device');
      tabInitialized.current = true;
    }
    setIsChecking(false);
  };

  useEffect(() => {
    verifyPermissions();
  }, []);

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const canProceed = perms.isAccessibilityEnabled;

  return (
    <div className="min-h-screen w-full bg-gray-950 flex flex-col items-center justify-start sm:justify-center p-4 sm:p-6 text-white text-center overflow-y-auto pb-24 pt-8">
      <motion.div 
        initial={{ opacity: 0, scale: 0.95 }}
        animate={{ opacity: 1, scale: 1 }}
        transition={{ duration: 0.4 }}
        className="w-full max-w-lg bg-gray-900 border border-gray-800 rounded-3xl p-6 sm:p-8 flex flex-col items-center shadow-2xl relative my-auto"
      >
        <div className="absolute top-0 left-0 w-full h-2 bg-gradient-to-r from-red-600 via-orange-500 to-amber-500 rounded-t-3xl" />
        
        <div className="w-16 h-16 bg-red-500/20 border border-red-500/30 rounded-2xl flex items-center justify-center mb-4 relative mt-2">
          {canProceed ? (
            <ShieldCheck className="w-8 h-8 text-emerald-400" />
          ) : (
            <ShieldAlert className="w-8 h-8 text-red-500" />
          )}
          {isChecking && (
            <div className="absolute inset-0 border-4 border-red-500/30 border-t-red-500 rounded-2xl animate-spin" />
          )}
        </div>
        
        <h2 className="text-2xl font-black mb-2 tracking-wide text-white uppercase">Setup Permissions</h2>
        
        <p className="text-gray-400 text-sm mb-4 leading-relaxed">
          Configure permissions to enforce lock sessions and protect against uninstallation.
        </p>

        {/* Installation Source Banner */}
        <div className="w-full mb-5 px-3.5 py-2.5 rounded-2xl border flex items-center justify-between text-xs bg-gray-950/80 border-gray-800">
          <div className="flex items-center gap-2.5 text-left">
            {perms.isAdbInstall ? (
              <div className="w-7 h-7 rounded-xl bg-emerald-500/20 border border-emerald-500/30 flex items-center justify-center">
                <Terminal className="w-4 h-4 text-emerald-400" />
              </div>
            ) : (
              <div className="w-7 h-7 rounded-xl bg-sky-500/20 border border-sky-500/30 flex items-center justify-center">
                <Smartphone className="w-4 h-4 text-sky-400" />
              </div>
            )}
            <div>
              <span className="text-[10px] uppercase font-bold text-gray-500 block leading-none mb-0.5">Installed via</span>
              <span className="font-bold text-gray-200 text-xs">
                {perms.installSource || (perms.isAdbInstall ? 'ADB (PC Script / USB)' : 'On-Device Package Installer')}
              </span>
            </div>
          </div>
          <span className={`px-2.5 py-1 rounded-full text-[10px] font-bold uppercase tracking-wider ${
            perms.isAdbInstall ? 'bg-emerald-950 text-emerald-300 border border-emerald-800' : 'bg-sky-950 text-sky-300 border border-sky-800'
          }`}>
            {perms.isAdbInstall ? 'ADB Provisioned' : 'On-Device'}
          </span>
        </div>

        {/* Permission Status Badges (2x2 Grid) */}
        <div className="w-full grid grid-cols-2 gap-2 mb-6">
          <div className={`p-3 rounded-xl border text-xs font-semibold flex items-center justify-between ${perms.isAccessibilityEnabled ? 'bg-emerald-950/40 border-emerald-800 text-emerald-300' : 'bg-gray-950 border-gray-800 text-gray-400'}`}>
            <div className="flex flex-col text-left">
              <span className="text-[10px] text-gray-400 uppercase font-bold">App Blocking</span>
              <span className="font-bold">Accessibility</span>
            </div>
            {perms.isAccessibilityEnabled ? (
              <span className="flex items-center gap-1 text-emerald-400 text-[11px] font-bold"><Check className="w-4 h-4" /> Enabled</span>
            ) : (
              <span className="text-[10px] uppercase font-bold text-red-400">Required</span>
            )}
          </div>

          <div className={`p-3 rounded-xl border text-xs font-semibold flex items-center justify-between ${perms.isAdminActive ? 'bg-emerald-950/40 border-emerald-800 text-emerald-300' : 'bg-gray-950 border-gray-800 text-gray-400'}`}>
            <div className="flex flex-col text-left">
              <span className="text-[10px] text-gray-400 uppercase font-bold">Uninstall Lock</span>
              <span className="font-bold">Device Admin</span>
            </div>
            {perms.isAdminActive ? (
              <span className="flex items-center gap-1 text-emerald-400 text-[11px] font-bold"><Check className="w-4 h-4" /> Active</span>
            ) : (
              <span className="text-[10px] uppercase font-bold text-amber-400">Recommended</span>
            )}
          </div>

          <div className={`p-3 rounded-xl border text-xs font-semibold flex items-center justify-between ${perms.isBatteryOptimizationIgnored ? 'bg-emerald-950/40 border-emerald-800 text-emerald-300' : 'bg-gray-950 border-gray-800 text-gray-400'}`}>
            <div className="flex flex-col text-left">
              <span className="text-[10px] text-gray-400 uppercase font-bold">Background</span>
              <span className="font-bold">Battery Saver</span>
            </div>
            {perms.isBatteryOptimizationIgnored ? (
              <span className="flex items-center gap-1 text-emerald-400 text-[11px] font-bold"><Check className="w-4 h-4" /> Unrestricted</span>
            ) : (
              <span className="text-[10px] uppercase font-bold text-amber-400">Recommended</span>
            )}
          </div>

          <div className={`p-3 rounded-xl border text-xs font-semibold flex items-center justify-between ${perms.isNotificationGranted ? 'bg-emerald-950/40 border-emerald-800 text-emerald-300' : 'bg-gray-950 border-gray-800 text-gray-400'}`}>
            <div className="flex flex-col text-left">
              <span className="text-[10px] text-gray-400 uppercase font-bold">Lock Timers</span>
              <span className="font-bold">Notifications</span>
            </div>
            {perms.isNotificationGranted ? (
              <span className="flex items-center gap-1 text-emerald-400 text-[11px] font-bold"><Check className="w-4 h-4" /> Allowed</span>
            ) : (
              <span className="text-[10px] uppercase font-bold text-amber-400">Recommended</span>
            )}
          </div>
        </div>

        {/* Setup Flow (Auto-detected based on install source) */}
        {activeTab === 'device' ? (
          <div className="w-full flex flex-col gap-4 mb-6">
            {/* Restricted Settings Notice (Android 13+) */}
            <div className="bg-amber-950/25 border border-amber-800/50 rounded-2xl p-4 text-left transition-all">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2 text-amber-300 font-bold text-xs">
                  <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />
                  <span>"Restricted setting" or button won't open?</span>
                </div>
                <button 
                  type="button" 
                  onClick={() => setShowRestrictedHelp(!showRestrictedHelp)} 
                  className="text-[11px] text-amber-400 hover:text-amber-200 font-semibold underline underline-offset-2 ml-2 shrink-0"
                >
                  {showRestrictedHelp ? 'Hide' : 'Show Fix'}
                </button>
              </div>

              {showRestrictedHelp && (
                <div className="mt-3 pt-3 border-t border-amber-900/40 text-xs text-gray-300 flex flex-col gap-2.5">
                  <p className="leading-relaxed text-gray-300">
                    Android 13+ blocks sideloaded apps from toggling Accessibility & Admin until you allow it once:
                  </p>
                  <ol className="list-decimal list-inside space-y-2 text-gray-300 bg-black/40 p-3.5 rounded-xl border border-amber-900/30">
                    <li>
                      First, tap <strong className="text-white">Step 1</strong> or <strong className="text-white">Step 2</strong> below once so Android triggers the restriction denial.
                      <span className="block text-[11px] text-amber-400/90 mt-0.5 ml-4">
                        * The 3 dots menu in App Info only appears AFTER you have attempted a restricted setting!
                      </span>
                    </li>
                    <li>
                      Then tap <strong className="text-white">Open App Info</strong> below.
                    </li>
                    <li>
                      In the top-right corner, tap the <strong className="text-white">3 dots (⋮)</strong>.
                    </li>
                    <li>
                      Tap <strong className="text-amber-300 font-bold">Allow restricted settings</strong> and verify with your PIN or fingerprint.
                    </li>
                    <li>
                      Return to QIEZKA and complete the steps!
                    </li>
                  </ol>
                  <button
                    type="button"
                    onClick={() => openAppInfo()}
                    className="py-2.5 px-4 bg-amber-500 hover:bg-amber-400 text-black font-bold rounded-xl text-xs transition-colors flex items-center justify-center gap-1.5 self-start shadow-md"
                  >
                    <Info className="w-4 h-4" />
                    <span>Open App Info (Allow Restricted Settings)</span>
                  </button>
                </div>
              )}
            </div>

            {/* Step 1: Accessibility */}
            <div className={`border rounded-2xl p-4 text-left transition-all ${perms.isAccessibilityEnabled ? 'bg-emerald-950/20 border-emerald-800/60' : 'bg-black/40 border-gray-800'}`}>
              <div className="flex items-center justify-between mb-1">
                <h3 className="text-xs font-bold text-gray-300 uppercase tracking-wider">Step 1: Enable Accessibility</h3>
                {perms.isAccessibilityEnabled && (
                  <span className="text-xs text-emerald-400 font-bold flex items-center gap-1"><Check className="w-3.5 h-3.5" /> Enabled</span>
                )}
              </div>
              <p className="text-xs text-gray-400 mb-3">Required to block non-whitelisted apps and collapse Quick Settings.</p>
              <button 
                onClick={() => openAccessibilitySettings()}
                className={`w-full py-3 text-white font-bold text-sm rounded-xl shadow-md transition-all flex items-center justify-center space-x-2 ${perms.isAccessibilityEnabled ? 'bg-gray-800 hover:bg-gray-700 border border-gray-700' : 'bg-red-600 hover:bg-red-700'}`}
              >
                <Settings className="w-4 h-4" />
                <span>{perms.isAccessibilityEnabled ? 'Reopen Accessibility Settings' : 'Open Accessibility Settings'}</span>
              </button>
            </div>

            {/* Step 2: Device Admin */}
            <div className={`border rounded-2xl p-4 text-left transition-all ${perms.isAdminActive ? 'bg-emerald-950/20 border-emerald-800/60' : 'bg-black/40 border-gray-800'}`}>
              <div className="flex items-center justify-between mb-1">
                <h3 className="text-xs font-bold text-gray-300 uppercase tracking-wider">Step 2: Activate Device Admin</h3>
                {perms.isAdminActive && (
                  <span className="text-xs text-emerald-400 font-bold flex items-center gap-1"><Check className="w-3.5 h-3.5" /> Active</span>
                )}
              </div>
              <p className="text-xs text-gray-400 mb-3">Prevents QIEZKA from being uninstalled during lockdown (no account removal needed).</p>
              
              <div className="flex flex-col gap-2">
                <button 
                  onClick={() => openDeviceAdminSettings()}
                  className={`w-full py-3 text-white font-bold text-sm rounded-xl border transition-all flex items-center justify-center space-x-2 ${perms.isAdminActive ? 'bg-gray-800 border-emerald-700 text-emerald-300' : 'bg-indigo-600 hover:bg-indigo-700 border-indigo-500'}`}
                >
                  <ShieldCheck className="w-4 h-4" />
                  <span>{perms.isAdminActive ? 'Device Administrator Active' : 'Activate Device Admin'}</span>
                </button>

                {!perms.isAdminActive && (
                  <button 
                    onClick={() => openDeviceAdminList()}
                    type="button"
                    className="w-full py-2 bg-gray-950 hover:bg-gray-800 text-gray-400 hover:text-gray-200 font-medium text-xs rounded-lg border border-gray-800 transition-all flex items-center justify-center gap-1.5"
                  >
                    <ListFilter className="w-3.5 h-3.5" />
                    <span>Prompt didn't open? Open Device Admin Apps list</span>
                  </button>
                )}
              </div>
            </div>

            {/* Step 3: Battery Optimization */}
            <div className={`border rounded-2xl p-4 text-left transition-all ${perms.isBatteryOptimizationIgnored ? 'bg-emerald-950/20 border-emerald-800/60' : 'bg-black/40 border-gray-800'}`}>
              <div className="flex items-center justify-between mb-1">
                <h3 className="text-xs font-bold text-gray-300 uppercase tracking-wider">Step 3: Unrestricted Battery / Background</h3>
                {perms.isBatteryOptimizationIgnored && (
                  <span className="text-xs text-emerald-400 font-bold flex items-center gap-1"><Check className="w-3.5 h-3.5" /> Unrestricted</span>
                )}
              </div>
              <p className="text-xs text-gray-400 mb-3">Prevents aggressive OEM battery savers (Samsung/Xiaomi/Pixel) from killing QIEZKA.</p>
              <button 
                onClick={async () => {
                  await requestBatteryOptimization();
                  setTimeout(verifyPermissions, 1000);
                }}
                className={`w-full py-3 text-white font-bold text-sm rounded-xl border transition-all flex items-center justify-center space-x-2 ${perms.isBatteryOptimizationIgnored ? 'bg-gray-800 border-emerald-700 text-emerald-300' : 'bg-amber-600 hover:bg-amber-700 border-amber-500'}`}
              >
                <BatteryCharging className="w-4 h-4" />
                <span>{perms.isBatteryOptimizationIgnored ? 'Background Usage Unrestricted' : 'Allow Unrestricted Background Usage'}</span>
              </button>
            </div>

            {/* Step 4: Notifications */}
            <div className={`border rounded-2xl p-4 text-left transition-all ${perms.isNotificationGranted ? 'bg-emerald-950/20 border-emerald-800/60' : 'bg-black/40 border-gray-800'}`}>
              <div className="flex items-center justify-between mb-1">
                <h3 className="text-xs font-bold text-gray-300 uppercase tracking-wider">Step 4: Allow Notifications</h3>
                {perms.isNotificationGranted && (
                  <span className="text-xs text-emerald-400 font-bold flex items-center gap-1"><Check className="w-3.5 h-3.5" /> Allowed</span>
                )}
              </div>
              <p className="text-xs text-gray-400 mb-3">Allows ongoing lock timer status, completion alerts, and foreground service priority.</p>
              <button 
                onClick={async () => {
                  await requestNotificationPermission();
                  setTimeout(verifyPermissions, 1000);
                }}
                className={`w-full py-3 text-white font-bold text-sm rounded-xl border transition-all flex items-center justify-center space-x-2 ${perms.isNotificationGranted ? 'bg-gray-800 border-emerald-700 text-emerald-300' : 'bg-sky-600 hover:bg-sky-700 border-sky-500'}`}
              >
                <Bell className="w-4 h-4" />
                <span>{perms.isNotificationGranted ? 'Notifications Allowed' : 'Allow Notifications'}</span>
              </button>
            </div>
          </div>
        ) : (
          <div className="w-full bg-black/40 border border-gray-800 rounded-2xl p-4 sm:p-5 mb-6 text-left flex flex-col gap-3">
            <div className="flex items-center justify-between border-b border-gray-800 pb-2.5">
              <span className="text-xs font-bold text-indigo-400 uppercase tracking-wider flex items-center gap-1.5">
                <Terminal className="w-4 h-4 text-emerald-400" /> ADB Provisioning Status
              </span>
              <span className="text-[10px] text-gray-400 font-mono">
                {perms.isAdbInstall ? 'PC Script Detected' : 'Manual ADB'}
              </span>
            </div>

            <p className="text-xs text-gray-300 leading-relaxed">
              {perms.isAdbInstall ? (
                <>QIEZKA detected setup via <strong>ADB / PC script</strong>. Below is what has been configured on your device:</>
              ) : (
                <>Connect your phone via USB with <strong>USB Debugging</strong> enabled, then run <code className="text-amber-300 bg-black/60 px-1 py-0.5 rounded font-mono">qiezka.bat</code> to grant all permissions automatically.</>
              )}
            </p>

            {/* ADB Checklist */}
            <div className="flex flex-col gap-2 my-1">
              {/* 1. Accessibility */}
              <div className={`p-3 rounded-xl border flex items-center justify-between transition-all ${
                perms.isAccessibilityEnabled ? 'bg-emerald-950/20 border-emerald-800/60' : 'bg-red-950/20 border-red-800/60'
              }`}>
                <div className="flex flex-col text-left pr-2">
                  <div className="flex items-center gap-1.5 mb-0.5">
                    <Settings className="w-3.5 h-3.5 text-gray-400" />
                    <span className="font-bold text-xs text-gray-200">1. App Blocking (Accessibility)</span>
                  </div>
                  <span className="text-[11px] text-gray-400">
                    {perms.isAccessibilityEnabled 
                      ? 'Active: Intercepts distracting apps & collapses Quick Settings.' 
                      : 'Not enabled yet. Tap below to turn on in Accessibility Settings.'}
                  </span>
                </div>
                {perms.isAccessibilityEnabled ? (
                  <span className="px-2 py-1 bg-emerald-950 text-emerald-300 border border-emerald-800 rounded-lg text-[10px] font-bold flex items-center gap-1 shrink-0">
                    <Check className="w-3 h-3" /> Granted
                  </span>
                ) : (
                  <button
                    type="button"
                    onClick={() => openAccessibilitySettings()}
                    className="px-2.5 py-1.5 bg-red-600 hover:bg-red-500 text-white rounded-lg text-xs font-bold shrink-0 transition shadow"
                  >
                    Enable Now
                  </button>
                )}
              </div>

              {/* 2. Device Admin */}
              <div className={`p-3 rounded-xl border flex items-center justify-between transition-all ${
                perms.isAdminActive ? 'bg-emerald-950/20 border-emerald-800/60' : 'bg-gray-900 border-gray-800'
              }`}>
                <div className="flex flex-col text-left pr-2">
                  <div className="flex items-center gap-1.5 mb-0.5">
                    <ShieldCheck className="w-3.5 h-3.5 text-gray-400" />
                    <span className="font-bold text-xs text-gray-200">2. Device Administrator</span>
                  </div>
                  <span className="text-[11px] text-gray-400">
                    {perms.isAdminActive ? 'Active: Prevents uninstallation during active locks.' : 'Inactive: Tap to activate device admin prompt.'}
                  </span>
                </div>
                {perms.isAdminActive ? (
                  <span className="px-2 py-1 bg-emerald-950 text-emerald-300 border border-emerald-800 rounded-lg text-[10px] font-bold flex items-center gap-1 shrink-0">
                    <Check className="w-3 h-3" /> Active
                  </span>
                ) : (
                  <button
                    type="button"
                    onClick={() => openDeviceAdminSettings()}
                    className="px-2.5 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-lg text-xs font-bold shrink-0 transition shadow"
                  >
                    Activate
                  </button>
                )}
              </div>

              {/* 3. Battery Saver */}
              <div className={`p-3 rounded-xl border flex items-center justify-between transition-all ${
                perms.isBatteryOptimizationIgnored ? 'bg-emerald-950/20 border-emerald-800/60' : 'bg-gray-900 border-gray-800'
              }`}>
                <div className="flex flex-col text-left pr-2">
                  <div className="flex items-center gap-1.5 mb-0.5">
                    <BatteryCharging className="w-3.5 h-3.5 text-gray-400" />
                    <span className="font-bold text-xs text-gray-200">3. Battery Saver Whitelist</span>
                  </div>
                  <span className="text-[11px] text-gray-400">
                    {perms.isBatteryOptimizationIgnored ? 'Whitelisted: Safe from aggressive OEM task killers.' : 'Pending: Tap to exempt from battery saver.'}
                  </span>
                </div>
                {perms.isBatteryOptimizationIgnored ? (
                  <span className="px-2 py-1 bg-emerald-950 text-emerald-300 border border-emerald-800 rounded-lg text-[10px] font-bold flex items-center gap-1 shrink-0">
                    <Check className="w-3 h-3" /> Whitelisted
                  </span>
                ) : (
                  <button
                    type="button"
                    onClick={async () => {
                      await requestBatteryOptimization();
                      setTimeout(verifyPermissions, 1000);
                    }}
                    className="px-2.5 py-1.5 bg-amber-600 hover:bg-amber-500 text-white rounded-lg text-xs font-bold shrink-0 transition shadow"
                  >
                    Whitelist
                  </button>
                )}
              </div>

              {/* 4. Notification Alerts */}
              <div className={`p-3 rounded-xl border flex items-center justify-between transition-all ${
                perms.isNotificationGranted ? 'bg-emerald-950/20 border-emerald-800/60' : 'bg-gray-900 border-gray-800'
              }`}>
                <div className="flex flex-col text-left pr-2">
                  <div className="flex items-center gap-1.5 mb-0.5">
                    <Bell className="w-3.5 h-3.5 text-gray-400" />
                    <span className="font-bold text-xs text-gray-200">4. Notification Alerts</span>
                  </div>
                  <span className="text-[11px] text-gray-400">
                    {perms.isNotificationGranted ? 'Granted via ADB pm grant.' : 'Pending: Tap to grant notification access.'}
                  </span>
                </div>
                {perms.isNotificationGranted ? (
                  <span className="px-2 py-1 bg-emerald-950 text-emerald-300 border border-emerald-800 rounded-lg text-[10px] font-bold flex items-center gap-1 shrink-0">
                    <Check className="w-3 h-3" /> Granted
                  </span>
                ) : (
                  <button
                    type="button"
                    onClick={async () => {
                      await requestNotificationPermission();
                      setTimeout(verifyPermissions, 1000);
                    }}
                    className="px-2.5 py-1.5 bg-sky-600 hover:bg-sky-500 text-white rounded-lg text-xs font-bold shrink-0 transition shadow"
                  >
                    Allow
                  </button>
                )}
              </div>

              {/* 5. Restricted Settings */}
              <div className="p-3 rounded-xl border flex items-center justify-between bg-emerald-950/20 border-emerald-800/60">
                <div className="flex flex-col text-left pr-2">
                  <div className="flex items-center gap-1.5 mb-0.5">
                    <ShieldCheck className="w-3.5 h-3.5 text-gray-400" />
                    <span className="font-bold text-xs text-gray-200">5. Android 13/14+ Restricted Settings</span>
                  </div>
                  <span className="text-[11px] text-gray-400">Bypassed via ADB AppOps (no 3-dot menu needed)</span>
                </div>
                <span className="px-2 py-1 bg-emerald-950 text-emerald-300 border border-emerald-800 rounded-lg text-[10px] font-bold flex items-center gap-1 shrink-0">
                  <Check className="w-3 h-3" /> Bypassed
                </span>
              </div>
            </div>

            {/* Quick ADB Copy Command / Re-run */}
            <div className="bg-gray-950 border border-gray-800 rounded-xl p-3 flex items-center justify-between font-mono text-xs mt-1">
              <span className="text-gray-400 truncate mr-2 font-mono text-[11px]">Need to re-run? Use qiezka.bat</span>
              <div className="flex items-center gap-1 shrink-0">
                <button
                  type="button"
                  onClick={() => copyToClipboard('adb shell settings put secure enabled_accessibility_services com.uncode.app/com.uncode.app.LockAccessibilityService && adb shell settings put secure accessibility_enabled 1')}
                  className="px-2 py-1 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 text-[11px] font-sans font-bold transition flex items-center gap-1"
                  title="Copy ADB Accessibility Command"
                >
                  {copied ? <Check className="w-3 h-3 text-emerald-400" /> : <Copy className="w-3 h-3" />}
                  <span>{copied ? 'Copied' : 'Copy ADB Cmd'}</span>
                </button>
                <a
                  href={gitHubUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="p-1.5 rounded-lg bg-gray-800 hover:bg-gray-700 text-gray-300 transition-colors"
                  title="Open GitHub"
                >
                  <ExternalLink className="w-3.5 h-3.5" />
                </a>
              </div>
            </div>
          </div>
        )}

        {/* Action Buttons */}
        <div className="w-full flex flex-col gap-2.5">
          <button 
            onClick={verifyPermissions}
            disabled={isChecking}
            className="w-full py-3 bg-gray-800 hover:bg-gray-700 text-white font-bold rounded-xl border border-gray-700 transition-all uppercase tracking-wider text-xs flex items-center justify-center space-x-2 disabled:opacity-50"
          >
            {isChecking ? <><Loader2 className="w-4 h-4 animate-spin" /><span>Verifying...</span></> : <span>Re-Check Status</span>}
          </button>

          {canProceed && (
            <button
              onClick={onComplete}
              className="w-full py-4 bg-gradient-to-r from-emerald-500 to-teal-600 hover:from-emerald-600 hover:to-teal-700 text-white font-black rounded-xl shadow-lg shadow-emerald-950 transition-all text-sm flex items-center justify-center space-x-2 cursor-pointer animate-pulse hover:animate-none"
            >
              <span>Proceed to Dashboard</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          )}
        </div>

        {/* Having trouble? Switch Setup Flow at the Bottom */}
        <div className="w-full pt-4 mt-2 border-t border-gray-800/80 flex flex-col items-center">
          <p className="text-xs text-gray-400 mb-2">
            Having trouble with {activeTab === 'pc' ? 'ADB / PC script setup' : 'on-device settings'}?
          </p>
          <button
            type="button"
            onClick={() => setActiveTab(activeTab === 'pc' ? 'device' : 'pc')}
            className="px-4 py-2 bg-gray-800/80 hover:bg-gray-800 text-gray-300 hover:text-white border border-gray-700 rounded-xl text-xs font-bold transition-all flex items-center gap-2 shadow-sm"
          >
            {activeTab === 'pc' ? (
              <>
                <Smartphone className="w-3.5 h-3.5 text-sky-400" />
                <span>Switch to On-Device 4-Step Setup</span>
              </>
            ) : (
              <>
                <Terminal className="w-3.5 h-3.5 text-emerald-400" />
                <span>Switch to Automated PC Script Setup</span>
              </>
            )}
          </button>
        </div>
      </motion.div>
    </div>
  );
}

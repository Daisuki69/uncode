import React, { useState, useEffect } from 'react';
import { X, Key, Save, Trash2, Cpu, FileText, Wand2, RefreshCw, ArrowLeft, Clock, Activity, Globe, Shield, Flame, CheckCircle, AlertTriangle, Wifi, Layers, Video, Lock, Gamepad2, Search, Sparkles, Bot } from 'lucide-react';
import { AppSettings, LogEntry, SavedResource, AllowedApp, UNIFIED_SERVICES, UnifiedServiceDefinition } from '../types';
import { defaultPrompts as staticDefaultPrompts } from '../../defaultPrompts';
import { refinePrompt } from '../api/refinePrompt';
import { loadData, saveData } from '../storage';
import { exportBackup, setServicePolicy, getActiveServices } from '../systemBridge';
import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';
import { isAppBlacklisted } from '../constants/blacklistedApps';

interface SettingsOverlayProps {
  settings: AppSettings;
  logs: LogEntry[];
  onSave: (updates: Partial<AppSettings>) => void;
  onClearLogs: () => void;
  onClose: () => void;
  isLockActive?: boolean;
}

export function SettingsOverlay({ settings, logs, onSave, onClearLogs, onClose, isLockActive = false }: SettingsOverlayProps) {
  const isLockedOrConsequence = isLockActive || !!settings.consequenceActive;

  const [activeTab, setActiveTab] = useState<'general' | 'prompts' | 'logs'>('general');
  const [apiKey, setApiKey] = useState(settings.apiKey || '');
  const [apiModel, setApiModel] = useState(settings.apiModel || 'gemini-3.7-flash');
  const [simpleOcrKey, setSimpleOcrKey] = useState(settings.simpleOcrKey || '');
  const [formattedOcrKey, setFormattedOcrKey] = useState(settings.formattedOcrKey || '');
  const [uiScale, setUiScale] = useState(settings.uiScale || 100);
  const [operatingMode, setOperatingModeState] = useState<'safemode' | 'hardcore'>(settings.operatingMode || 'safemode');
  const [showHardcoreModal, setShowHardcoreModal] = useState(false);
  const [modeError, setModeError] = useState<string | null>(null);

  const [webProtectionMode, setWebProtectionModeState] = useState<'accessibility' | 'dns_vpn' | 'dual_hybrid'>(
    settings.webProtectionMode === 'dns_vpn' || settings.webProtectionMode === 'dual_hybrid'
      ? settings.webProtectionMode
      : 'accessibility'
  );
  const [allowYoutube, setAllowYoutubeState] = useState<boolean>(settings.allowYoutube || false);
  const [activeServices, setActiveServicesState] = useState<string[]>(
    settings.activeServices || (settings.allowYoutube ? ['youtube'] : [])
  );
  const blockWebGames = true; // Permanently active & cannot be turned off
  const [webError, setWebError] = useState<string | null>(null);
  
  const [defaultPrompts, setDefaultPrompts] = useState<Record<string, string>>({});
  const [customPrompts, setCustomPrompts] = useState<Record<string, string>>(settings.prompts || {});
  const [refiningKey, setRefiningKey] = useState<string | null>(null);

  useEffect(() => {
    // Load directly from imported file for static/Vercel environments
    setDefaultPrompts(staticDefaultPrompts);
    getActiveServices().then(res => {
      if (res && res.length > 0) {
        setActiveServicesState(prev => Array.from(new Set([...prev, ...res])));
      }
    }).catch(() => {});
  }, []);

  const hasActiveDaytimeSchedule = (settings.schedules || []).some(s => {
    if (!s.isActive || !s.activationTime) return false;
    const [h, m] = s.activationTime.split(':').map(Number);
    const totalMins = (h || 0) * 60 + (m || 0);
    // Daytime is between 03:01 (181 mins) and 18:59 (1139 mins)
    return totalMins >= 180 && totalMins < 1140;
  });

  const hasActiveSchedule = isLockedOrConsequence || (settings.schedules || []).some(s => s.isActive);

  const handleSelectMode = (mode: 'safemode' | 'hardcore') => {
    setModeError(null);
    if (isLockedOrConsequence) {
      setModeError('Operating mode is locked during active lockdown or consequence mode. Complete and pass your session first.');
      return;
    }

    if (mode === 'hardcore') {
      if (operatingMode !== 'hardcore') {
        setShowHardcoreModal(true);
      }
    } else {
      // Switching to Safemode: check if there are daytime schedules
      if (hasActiveDaytimeSchedule) {
        setModeError('Cannot switch to Safemode: You have active scheduled sessions during daytime hours (3:00 AM – 7:00 PM). Please complete or delete these daytime schedules first.');
        return;
      }
      setOperatingModeState('safemode');
    }
  };

  const handleConfirmHardcore = () => {
    setOperatingModeState('hardcore');
    setShowHardcoreModal(false);
    setModeError(null);
  };

  const handleSelectWebMode = async (mode: 'accessibility' | 'dns_vpn' | 'dual_hybrid') => {
    setWebError(null);
    if (isLockedOrConsequence) {
      setWebError('Web protection settings are locked during active lockdown or consequence mode.');
      return;
    }

    if (mode === 'dns_vpn' || mode === 'dual_hybrid') {
      try {
        const { requestVpnPermission } = await import('../systemBridge');
        const granted = await requestVpnPermission();
        if (!granted) {
          setWebError('VPN permission was not granted. Reverting to Accessibility URL Guard.');
          setWebProtectionModeState('accessibility');
          return;
        }
      } catch (e) {
        console.warn('VPN permission request error:', e);
      }
    }
    setWebProtectionModeState(mode);
  };

  const handleToggleAllowYoutube = (allow: boolean) => {
    handleToggleService('youtube', allow);
  };

  const handleToggleService = async (serviceId: string, allowed: boolean) => {
    setWebError(null);
    if (isLockedOrConsequence) {
      setWebError('Service policies are locked during active lockdown or consequence mode.');
      return;
    }
    const next = allowed
      ? Array.from(new Set([...activeServices, serviceId]))
      : activeServices.filter(s => s !== serviceId);
    setActiveServicesState(next);
    if (serviceId === 'youtube') {
      setAllowYoutubeState(allowed);
    }
    try {
      await setServicePolicy(serviceId, allowed);
    } catch (e) {
      console.warn('Failed to setServicePolicy', serviceId, e);
    }
  };

  const handleSaveGeneral = () => {
    onSave({ 
      apiKey: apiKey.trim() || undefined,
      apiModel: apiModel.trim() || 'gemini-3.7-flash',
      simpleOcrKey: simpleOcrKey.trim() || undefined,
      formattedOcrKey: formattedOcrKey.trim() || undefined,
      uiScale: uiScale,
      operatingMode: operatingMode,
      webProtectionMode: webProtectionMode,
      allowYoutube: activeServices.includes('youtube'),
      activeServices: activeServices,
      blockWebGames: blockWebGames,
    });
  };

  const handleClearGeneral = () => {
    setApiKey('');
    setApiModel('gemini-3.7-flash');
    setSimpleOcrKey('');
    setFormattedOcrKey('');
    onSave({ apiKey: undefined, apiModel: 'gemini-3.7-flash', simpleOcrKey: undefined, formattedOcrKey: undefined });
  };

  const handleExport = async () => {
    try {
      const currentSettings = await loadData<AppSettings>('studom_settings', settings);
      const allAllowed = currentSettings.allowedApps || settings.allowedApps || [];
      const customApps = allAllowed.filter(
        (app: AllowedApp) => !isHardcodedApp(app) && !isAppBlacklisted(app.id) && !isHiddenSystemExemptApp(app.id, app.name)
      );

      const allData = {
        settings: {
          ...currentSettings,
          allowedApps: allAllowed
        },
        customApps: customApps,
        allowedApps: allAllowed,
        resources: await loadData<SavedResource[]>('studom_resources', []),
        logs: await loadData<LogEntry[]>('studom_logs', []),
        completedHomeworks: await loadData<any[]>('studom_completed_homeworks', []),
        timeOffset: await loadData<number>('studom_timeOffset', 0)
      };
      
      const jsonString = JSON.stringify(allData, null, 2);
      const tempFileName = `temp_qiezka_backup_${Date.now()}.json`;
      const defaultName = `qiezka_backup_${new Date().toISOString().slice(0,10)}.json`;

      // Write to internal cache
      await Filesystem.writeFile({
        path: tempFileName,
        data: jsonString,
        directory: Directory.Cache,
        encoding: Encoding.UTF8,
      });

      // Hand off to native SAF picker
      await exportBackup(tempFileName, defaultName);
    } catch (err) {
      console.error(err);
      alert("Failed to export data.");
    }
  };

  const handleImport = () => {
    if (isLockedOrConsequence) {
      alert("Backup import is disabled during active lockdown or consequence mode. Complete and pass your homework session first.");
      return;
    }
    const input = document.createElement('input');
    input.type = 'file';
    input.accept = '.json';
    input.onchange = async (e) => {
      const file = (e.target as HTMLInputElement).files?.[0];
      if (!file) return;
      const reader = new FileReader();
      reader.onload = async (e2) => {
        try {
          const text = e2.target?.result as string;
          const data = JSON.parse(text);
          if (data.settings) {
            const rawAllowed = data.customApps || data.allowedApps || data.settings.allowedApps;
            if (rawAllowed && Array.isArray(rawAllowed)) {
              data.settings.allowedApps = rawAllowed.filter(
                (a: AllowedApp) => a && a.id && !isAppBlacklisted(a.id) && !isHiddenSystemExemptApp(a.id, a.name)
              );
              data.settings.allowedAppsInitialized = true;
            }
            await saveData('studom_settings', data.settings);

            // Synchronize with native SharedPreferences
            try {
              const { 
                endLockdown, 
                setConsequenceActive, 
                syncSchedules,
                setOperatingMode,
                setWebProtectionMode,
                setAllowYoutube,
                setBlockWebGames,
                syncTimeOffset
              } = await import('../systemBridge');

              if (data.timeOffset !== undefined) {
                await syncTimeOffset(data.timeOffset);
              }
              if (data.settings.operatingMode) {
                await setOperatingMode(data.settings.operatingMode);
              }
              if (data.settings.webProtectionMode) {
                await setWebProtectionMode(data.settings.webProtectionMode);
              }
              if (data.settings.allowYoutube !== undefined) {
                await setAllowYoutube(data.settings.allowYoutube);
              }
              await setBlockWebGames(true);

              const safeAllowed = (data.settings.allowedApps || []).map((a: AllowedApp) => a.id);
              if (data.settings.schedules) {
                await syncSchedules(data.settings.schedules, safeAllowed);
              }

              if (data.settings.consequenceActive) {
                await setConsequenceActive(true, data.settings.consequenceScheduleId, safeAllowed);
              } else {
                endLockdown();
                await setConsequenceActive(false);
              }
            } catch (nativeErr) {
              console.warn('Failed to sync native settings on import', nativeErr);
            }
          } else if (data.customApps || data.allowedApps) {
            const currentSettings = await loadData<AppSettings>('studom_settings', settings);
            const rawAllowed = data.customApps || data.allowedApps;
            currentSettings.allowedApps = rawAllowed.filter(
              (a: AllowedApp) => a && a.id && !isAppBlacklisted(a.id) && !isHiddenSystemExemptApp(a.id, a.name)
            );
            currentSettings.allowedAppsInitialized = true;
            await saveData('studom_settings', currentSettings);
          }
          if (data.resources) await saveData('studom_resources', data.resources);
          if (data.logs) await saveData('studom_logs', data.logs);
          if (data.completedHomeworks) await saveData('studom_completed_homeworks', data.completedHomeworks);
          if (data.timeOffset !== undefined) await saveData('studom_timeOffset', data.timeOffset);
          
          alert("Import successful! The app will now reload.");
          window.location.reload();
        } catch (err) {
          alert("Failed to parse JSON backup.");
        }
      };
      reader.readAsText(file);
    };
    input.click();
  };

  const handlePromptChange = (key: string, value: string) => {
    setCustomPrompts(prev => ({ ...prev, [key]: value }));
  };

  const handleResetPrompt = (key: string) => {
    setCustomPrompts(prev => {
      const next = { ...prev };
      delete next[key];
      return next;
    });
  };

  const handleResetAllPrompts = () => {
    setCustomPrompts({});
    onSave({ prompts: undefined });
  };

  const handleRefineAndSaveSingle = async (key: string) => {
    const value = customPrompts[key];
    if (!value || !value.trim() || value === defaultPrompts[key]) return;

    setRefiningKey(key);
    try {
      const refined = await refinePrompt({
        promptText: value,
        apiKey,
        apiModel,
      });

      if (refined) {
        const newPrompts = { ...customPrompts, [key]: refined };
        setCustomPrompts(newPrompts);
        onSave({ prompts: newPrompts });
      } else {
        throw new Error('Refined prompt is empty.');
      }
    } catch (e: any) {
      console.error('Failed to refine prompt', key, e);
      alert('Failed to refine prompt: ' + (e.message || 'Unknown error'));
    } finally {
      setRefiningKey(null);
    }
  };

  const handleSaveAllPrompts = () => {
    const cleanedPrompts: Record<string, string> = {};
    for (const [key, value] of Object.entries(customPrompts) as [string, string][]) {
      if (value && value.trim() && value !== defaultPrompts[key]) {
        cleanedPrompts[key] = value;
      }
    }
    setCustomPrompts(cleanedPrompts);
    onSave({ prompts: Object.keys(cleanedPrompts).length > 0 ? cleanedPrompts : undefined });
  };

  return (
    <div className="flex-1 flex items-center justify-center p-4 h-full">
      <div className="bg-white rounded-2xl w-full max-w-5xl max-h-[95vh] shadow-2xl overflow-hidden flex flex-col">
        <div className="p-4 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
          <div className="flex items-center gap-2">
            <button 
              onClick={onClose}
              className="px-3 py-1.5 text-gray-500 hover:text-gray-900 hover:bg-gray-200 rounded-lg flex items-center font-bold text-sm transition-colors"
            >
              <ArrowLeft className="w-4 h-4 mr-1" />
              Back
            </button>
          </div>
          <div className="flex gap-2">
            <button
              onClick={() => setActiveTab('general')}
              className={`text-sm font-bold flex items-center px-3 py-1.5 rounded-lg transition-colors ${activeTab === 'general' ? 'bg-gray-200 text-gray-900' : 'text-gray-500 hover:bg-gray-100'}`}
            >
              <Key className="w-4 h-4 mr-2" />
              General
            </button>
            <button
              onClick={() => setActiveTab('prompts')}
              className={`text-sm font-bold flex items-center px-3 py-1.5 rounded-lg transition-colors ${activeTab === 'prompts' ? 'bg-gray-200 text-gray-900' : 'text-gray-500 hover:bg-gray-100'}`}
            >
              <FileText className="w-4 h-4 mr-2" />
              AI Prompts
            </button>
          </div>
          <div className="w-16"></div>
        </div>
        
        <div className="flex-1 overflow-y-auto p-6">
          {activeTab === 'general' && (
            <div className="max-w-3xl mx-auto">
              {hasActiveSchedule && (
                <div className="mb-6 p-4 bg-amber-50 border border-amber-200 rounded-xl text-amber-800 text-sm flex flex-col shadow-sm">
                  <span className="font-bold uppercase tracking-wider mb-1 flex items-center">
                    <Key className="w-4 h-4 mr-2" />
                    Keys Locked
                  </span>
                  <span>You cannot modify API keys while a schedule is active. Please complete or cancel your active schedule first.</span>
                </div>
              )}
              <div className="mb-6 p-4 bg-indigo-50/80 border border-indigo-200/80 rounded-2xl text-xs text-indigo-950 space-y-2">
                <div className="flex items-center font-bold text-indigo-900 gap-1.5 uppercase tracking-wider text-[11px]">
                  <Globe className="w-3.5 h-3.5 text-indigo-600" />
                  <span>Bring Your Own Key (BYOK) & Connectivity Architecture</span>
                </div>
                <p className="leading-relaxed text-indigo-900/90">
                  <strong>Online Requirement:</strong> QIEZKA connects directly to Google Gemini and OCR.space using your personal API keys for homework evaluation. Keys remain 100% private and stored locally on your device.
                </p>
                <p className="leading-relaxed text-indigo-800">
                  <strong>Offline Mode:</strong> Study resources (notes, syllabi, Word .docx, .txt), timer schedules, and selective app lockdown operate entirely <strong>offline</strong> without an internet connection.
                </p>
              </div>

              <p className="text-sm text-gray-600 mb-6 leading-relaxed">
                Configure your personal API credentials below. Both Gemini and OCR.space offer free tier API keys.
              </p>
              <div className="mb-6">
                <label className="block text-sm font-bold text-gray-700 mb-2 uppercase tracking-wider">
                  Gemini API Key
                </label>
                <input
                  type="password"
                  value={apiKey}
                  onChange={(e) => setApiKey(e.target.value)}
                  disabled={hasActiveSchedule}
                  placeholder="AIzaSy..."
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 font-mono text-sm mb-2"
                />
                {settings.apiKey && (
                  <p className="text-xs text-green-600 font-bold flex items-center">
                    ✓ Custom API key is currently active
                  </p>
                )}
              </div>
              <div className="mb-6">
                <label className="block text-sm font-bold text-gray-700 mb-2 uppercase tracking-wider flex items-center">
                  <Cpu className="w-4 h-4 mr-1" /> AI Model
                </label>
                <select
                  value={apiModel}
                  onChange={(e) => setApiModel(e.target.value)}
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 text-sm mb-2"
                >
                  <optgroup label="Standard fast reasoning models">
                    <option value="gemini-3.7-flash">gemini-3.7-flash</option>
                    <option value="gemini-3.6-flash">gemini-3.6-flash</option>
                    <option value="gemini-3.5-flash">gemini-3.5-flash</option>
                  </optgroup>
                  <optgroup label="Cost-effective, high-throughput models">
                    <option value="gemini-3.5-flash-lite">gemini-3.5-flash-lite</option>
                    <option value="gemini-3.1-flash-lite">gemini-3.1-flash-lite</option>
                  </optgroup>
                  <optgroup label="Advanced problem-solving models">
                    <option value="gemini-3.1-pro-preview">gemini-3.1-pro-preview</option>
                    <option value="gemini-3-pro-preview">gemini-3-pro-preview</option>
                    <option value="gemini-3-flash-preview">gemini-3-flash-preview</option>
                  </optgroup>
                </select>
              </div>
              <div className="mb-6 border-t border-gray-100 pt-6">
                <label className="block text-sm font-bold text-gray-700 mb-2 uppercase tracking-wider flex items-center">
                  Simple OCR API Key
                </label>
                <input
                  type="password"
                  value={simpleOcrKey}
                  onChange={(e) => setSimpleOcrKey(e.target.value)}
                  disabled={hasActiveSchedule}
                  placeholder="Paste your OCR simple key here"
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 text-sm mb-2"
                />
              </div>

              <div className="mb-8">
                <label className="block text-sm font-bold text-gray-700 mb-2 uppercase tracking-wider flex items-center">
                  Formatted OCR API Key
                </label>
                <input
                  type="password"
                  value={formattedOcrKey}
                  onChange={(e) => setFormattedOcrKey(e.target.value)}
                  disabled={hasActiveSchedule}
                  placeholder="Paste your OCR formatted key here"
                  className="w-full px-4 py-3 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 text-sm mb-2"
                />
              </div>

              
              <div className="mb-8 border-t border-gray-200 pt-6">
                <div className="flex justify-between items-center mb-2">
                  <label className="block text-sm font-bold text-gray-700 uppercase tracking-wider flex items-center gap-2">
                    <Shield className="w-4 h-4 text-gray-500" />
                    Operating Mode
                  </label>
                  <span className={`text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full ${
                    operatingMode === 'hardcore'
                      ? 'bg-red-600 text-white animate-pulse'
                      : 'bg-emerald-100 text-emerald-800'
                  }`}>
                    {operatingMode === 'hardcore' ? 'Hardcore 24/7' : 'Safemode'}
                  </span>
                </div>
                <p className="text-xs text-gray-500 mb-4">
                  Controls operating hours, scheduling windows, and whether consequence failure restrictions pause during daytime.
                </p>

                {modeError && (
                  <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 font-bold flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 text-red-500 shrink-0" />
                    <span>{modeError}</span>
                  </div>
                )}

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  {/* Safemode Card */}
                  <div
                    onClick={() => handleSelectMode('safemode')}
                    className={`p-4 rounded-2xl border-2 transition-all cursor-pointer relative ${
                      operatingMode === 'safemode'
                        ? 'border-emerald-500 bg-emerald-50/50 shadow-sm'
                        : 'border-gray-200 bg-white hover:border-gray-300'
                    } ${isLockedOrConsequence ? 'opacity-60 cursor-not-allowed' : ''}`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <div className={`p-2 rounded-xl ${operatingMode === 'safemode' ? 'bg-emerald-600 text-white' : 'bg-gray-100 text-gray-600'}`}>
                          <Shield className="w-4 h-4" />
                        </div>
                        <span className="font-bold text-sm text-gray-900">Safemode</span>
                      </div>
                      {operatingMode === 'safemode' && (
                        <CheckCircle className="w-5 h-5 text-emerald-600" />
                      )}
                    </div>
                    <span className="text-[11px] font-bold text-emerald-700 block mb-1">7:00 PM – 3:00 AM (Default)</span>
                    <p className="text-xs text-gray-500 leading-relaxed">
                      Study hours discipline. Schedule only between 7 PM and 3 AM. If homework expires or fails, restrictions pause during daytime (3 AM – 7 PM) for school and sleep.
                    </p>
                  </div>

                  {/* Hardcore Card */}
                  <div
                    onClick={() => handleSelectMode('hardcore')}
                    className={`p-4 rounded-2xl border-2 transition-all cursor-pointer relative ${
                      operatingMode === 'hardcore'
                        ? 'border-red-500 bg-red-50/50 shadow-sm'
                        : 'border-gray-200 bg-white hover:border-gray-300'
                    } ${isLockedOrConsequence ? 'opacity-60 cursor-not-allowed' : ''}`}
                  >
                    <div className="flex items-center justify-between mb-2">
                      <div className="flex items-center gap-2">
                        <div className={`p-2 rounded-xl ${operatingMode === 'hardcore' ? 'bg-red-600 text-white' : 'bg-gray-100 text-gray-600'}`}>
                          <Flame className="w-4 h-4" />
                        </div>
                        <span className="font-bold text-sm text-gray-900">Hardcore</span>
                      </div>
                      {operatingMode === 'hardcore' && (
                        <CheckCircle className="w-5 h-5 text-red-600" />
                      )}
                    </div>
                    <span className="text-[11px] font-bold text-red-600 block mb-1">24/7 Round-The-Clock</span>
                    <p className="text-xs text-gray-500 leading-relaxed">
                      Uncompromising discipline. Schedule sessions at any hour of the day. Consequence restrictions persist continuously through daytime until rescheduled and passed.
                    </p>
                  </div>
                </div>
              </div>

              {/* Web & Browser Protection Section */}
              <div className="mb-8 border-t border-gray-200 pt-6">
                <div className="flex justify-between items-center mb-2">
                  <label className="block text-sm font-bold text-gray-700 uppercase tracking-wider flex items-center gap-2">
                    <Globe className="w-4 h-4 text-gray-500" />
                    Web & Browser Protection
                  </label>
                  <span className={`text-[10px] font-black uppercase px-2.5 py-0.5 rounded-full ${
                    webProtectionMode === 'dual_hybrid'
                      ? 'bg-purple-600 text-white'
                      : webProtectionMode === 'dns_vpn'
                        ? 'bg-indigo-600 text-white'
                        : 'bg-emerald-100 text-emerald-800'
                  }`}>
                    {webProtectionMode === 'dual_hybrid'
                      ? 'Dual-Layer Hybrid'
                      : webProtectionMode === 'dns_vpn'
                        ? 'DNS Sinkhole (VPN)'
                        : 'Accessibility Guard'}
                  </span>
                </div>
                <p className="text-xs text-gray-500 mb-4">
                  Closes the browser backdoor so students cannot bypass app restrictions via mobile Chrome or other browsers.
                </p>

                {webError && (
                  <div className="mb-4 p-3 bg-red-50 border border-red-200 rounded-xl text-xs text-red-700 font-bold flex items-center gap-2">
                    <AlertTriangle className="w-4 h-4 text-red-500 shrink-0" />
                    <span>{webError}</span>
                  </div>
                )}

                {/* Web Protection Mode Grid (Milestone 18: No Off Option) */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 mb-5">
                  {/* Accessibility Guard */}
                  <div
                    onClick={() => handleSelectWebMode('accessibility')}
                    className={`p-3.5 rounded-2xl border-2 transition-all cursor-pointer relative ${
                      webProtectionMode === 'accessibility'
                        ? 'border-emerald-500 bg-emerald-50/50 shadow-sm'
                        : 'border-gray-200 bg-white hover:border-gray-300'
                    } ${isLockedOrConsequence ? 'opacity-60 cursor-not-allowed' : ''}`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <div className="flex items-center gap-2">
                        <div className={`p-1.5 rounded-lg ${webProtectionMode === 'accessibility' ? 'bg-emerald-600 text-white' : 'bg-gray-100 text-gray-600'}`}>
                          <Shield className="w-3.5 h-3.5" />
                        </div>
                        <span className="font-bold text-xs text-gray-900">Accessibility Guard</span>
                      </div>
                      {webProtectionMode === 'accessibility' && (
                        <CheckCircle className="w-4 h-4 text-emerald-600" />
                      )}
                    </div>
                    <span className="text-[10px] font-bold text-emerald-700 block mb-1">Recommended Baseline • Zero VPN Slot</span>
                    <p className="text-[11px] text-gray-500 leading-relaxed">
                      Inspects website URLs and web pages in real-time. Leaves VPN slot completely free for university VPNs.
                    </p>
                  </div>

                  {/* DNS Sinkhole (VPN) */}
                  <div
                    onClick={() => handleSelectWebMode('dns_vpn')}
                    className={`p-3.5 rounded-2xl border-2 transition-all cursor-pointer relative ${
                      webProtectionMode === 'dns_vpn'
                        ? 'border-indigo-500 bg-indigo-50/50 shadow-sm'
                        : 'border-gray-200 bg-white hover:border-gray-300'
                    } ${isLockedOrConsequence ? 'opacity-60 cursor-not-allowed' : ''}`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <div className="flex items-center gap-2">
                        <div className={`p-1.5 rounded-lg ${webProtectionMode === 'dns_vpn' ? 'bg-indigo-600 text-white' : 'bg-gray-100 text-gray-600'}`}>
                          <Wifi className="w-3.5 h-3.5" />
                        </div>
                        <span className="font-bold text-xs text-gray-900">DNS Sinkhole (VPN)</span>
                      </div>
                      {webProtectionMode === 'dns_vpn' && (
                        <CheckCircle className="w-4 h-4 text-indigo-600" />
                      )}
                    </div>
                    <span className="text-[10px] font-bold text-indigo-700 block mb-1">WebClassifier • Packet-Level</span>
                    <p className="text-[11px] text-gray-500 leading-relaxed">
                      On-device loopback VPN sinkholes distracting domains at the socket layer across all browsers and apps using WebClassifier.
                    </p>
                  </div>

                  {/* Dual-Layer Hybrid */}
                  <div
                    onClick={() => handleSelectWebMode('dual_hybrid')}
                    className={`p-3.5 rounded-2xl border-2 transition-all cursor-pointer relative ${
                      webProtectionMode === 'dual_hybrid'
                        ? 'border-purple-500 bg-purple-50/50 shadow-sm'
                        : 'border-gray-200 bg-white hover:border-gray-300'
                    } ${isLockedOrConsequence ? 'opacity-60 cursor-not-allowed' : ''}`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <div className="flex items-center gap-2">
                        <div className={`p-1.5 rounded-lg ${webProtectionMode === 'dual_hybrid' ? 'bg-purple-600 text-white' : 'bg-gray-100 text-gray-600'}`}>
                          <Layers className="w-3.5 h-3.5" />
                        </div>
                        <span className="font-bold text-xs text-gray-900">Dual-Layer Hybrid</span>
                      </div>
                      {webProtectionMode === 'dual_hybrid' && (
                        <CheckCircle className="w-4 h-4 text-purple-600" />
                      )}
                    </div>
                    <span className="text-[10px] font-bold text-purple-700 block mb-1">Maximum Armor</span>
                    <p className="text-[11px] text-gray-500 leading-relaxed">
                      Runs both Accessibility Guard and DNS Sinkhole concurrently with WebClassifier for maximum coverage.
                    </p>
                  </div>
                </div>

                {/* Unified Service Policy Engine (App + Web) */}
                <div className="bg-gray-50 p-4 rounded-2xl border border-gray-200">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <div className="p-1.5 rounded-lg bg-indigo-600 text-white">
                        <Layers className="w-4 h-4" />
                      </div>
                      <div>
                        <span className="font-bold text-xs text-gray-900 block">Unified Service Policy Engine</span>
                        <span className="text-[10px] text-gray-500">Pairs native app package IDs directly with web domains</span>
                      </div>
                    </div>
                    <span className="text-[10px] font-bold px-2 py-0.5 rounded-md bg-indigo-100 text-indigo-700">
                      App + Web Synchronized
                    </span>
                  </div>

                  <p className="text-[11px] text-gray-500 mb-3 leading-relaxed">
                    Toggling any service simultaneously updates both the native application whitelist and web browser domain allowlist, clearing all decision caches in real time.
                  </p>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    {UNIFIED_SERVICES.map((svc) => {
                      const isAllowed = activeServices.includes(svc.id);
                      return (
                        <div key={svc.id} className="bg-white p-3 rounded-xl border border-gray-200/80 flex flex-col justify-between shadow-xs">
                          <div className="flex items-start justify-between gap-2.5 mb-2">
                            <div className="flex items-start gap-2">
                              <div className={`p-1.5 rounded-lg mt-0.5 ${
                                svc.id === 'youtube'
                                  ? 'bg-red-100 text-red-600'
                                  : svc.id === 'gemini'
                                    ? 'bg-blue-100 text-blue-600'
                                    : svc.id === 'openai'
                                      ? 'bg-emerald-100 text-emerald-600'
                                      : 'bg-purple-100 text-purple-600'
                              }`}>
                                {svc.id === 'youtube' && <Video className="w-3.5 h-3.5" />}
                                {svc.id === 'gemini' && <Sparkles className="w-3.5 h-3.5" />}
                                {svc.id === 'openai' && <Bot className="w-3.5 h-3.5" />}
                                {svc.id === 'claude' && <Cpu className="w-3.5 h-3.5" />}
                              </div>
                              <div>
                                <div className="flex items-center gap-1.5">
                                  <span className="font-bold text-xs text-gray-900">{svc.name}</span>
                                  <span className={`text-[9px] font-bold px-1.5 py-0.2 rounded ${
                                    isAllowed ? 'bg-emerald-100 text-emerald-700' : 'bg-gray-100 text-gray-500'
                                  }`}>
                                    {isAllowed ? 'Allowed' : 'Blocked'}
                                  </span>
                                </div>
                                <span className="text-[10px] text-indigo-600 font-semibold">{svc.badge}</span>
                              </div>
                            </div>

                            <button
                              type="button"
                              role="switch"
                              aria-checked={isAllowed}
                              onClick={() => handleToggleService(svc.id, !isAllowed)}
                              disabled={isLockedOrConsequence}
                              className={`relative inline-flex h-5 w-9 shrink-0 cursor-pointer rounded-full border-2 border-transparent transition-colors duration-200 ease-in-out focus:outline-none ${
                                isAllowed
                                  ? (svc.id === 'youtube' ? 'bg-red-600' : svc.id === 'gemini' ? 'bg-blue-600' : svc.id === 'openai' ? 'bg-emerald-600' : 'bg-purple-600')
                                  : 'bg-gray-300'
                              } ${isLockedOrConsequence ? 'opacity-50 cursor-not-allowed' : ''}`}
                            >
                              <span
                                className={`pointer-events-none inline-block h-4 w-4 transform rounded-full bg-white shadow-sm ring-0 transition duration-200 ease-in-out ${
                                  isAllowed ? 'translate-x-4' : 'translate-x-0'
                                }`}
                              />
                            </button>
                          </div>

                          <p className="text-[10.5px] text-gray-500 leading-tight">
                            {svc.description}
                          </p>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* Web Armor Banner */}
                <div className="bg-purple-50/80 p-3.5 rounded-2xl border border-purple-200 mt-3">
                  <div className="flex items-center justify-between gap-3">
                    <div className="flex items-start gap-2.5">
                      <div className="p-2 rounded-xl bg-purple-600 text-white mt-0.5 shadow-sm">
                        <Gamepad2 className="w-4 h-4" />
                      </div>
                      <div>
                        <div className="flex items-center gap-2">
                          <span className="font-bold text-xs text-purple-950">Multi-Genre Web Armor</span>
                          <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-md bg-purple-200/90 text-purple-900 border border-purple-300">
                            Permanently Active • No Bypass
                          </span>
                        </div>
                        <p className="text-[11px] text-purple-900/80 mt-1 leading-relaxed">
                          Distractions across all major categories (web games, gambling, adult content, proxies, piracy streaming, social media) are classified and blocked on-device by WebClassifier. This defense cannot be turned off.
                        </p>
                      </div>
                    </div>
                    <div className="shrink-0 p-1.5 rounded-full bg-purple-200/60 text-purple-800">
                      <Shield className="w-4 h-4 text-purple-700" />
                    </div>
                  </div>
                </div>
              </div>

              <div className="mb-8 border-t border-gray-200 pt-6">
                <label className="block text-sm font-bold text-gray-700 mb-2 uppercase tracking-wider flex justify-between">
                  <span>UI Zoom Scale</span>
                  <span className="text-gray-500">{uiScale}%</span>
                </label>
                <p className="text-xs text-gray-500 mb-4">Adjust the slider to test different UI sizes. Click save to apply globally.</p>
                <input 
                  type="range" 
                  min="50" 
                  max="150" 
                  value={uiScale} 
                  onChange={e => setUiScale(Number(e.target.value))}
                  className="w-full h-2 bg-gray-200 rounded-lg appearance-none cursor-pointer"
                />
              </div>
                <div className="flex gap-4 pt-4">
                  <button
                    onClick={handleClearGeneral}
                    className="flex-1 flex items-center justify-center gap-2 bg-gray-100 hover:bg-gray-200 text-gray-700 py-3 rounded-xl font-bold transition-colors"
                  >
                    <Trash2 className="w-5 h-5" />
                    Clear
                  </button>
                  <button
                    onClick={handleSaveGeneral}
                    className="flex-1 flex items-center justify-center gap-2 bg-red-600 hover:bg-red-700 text-white py-3 rounded-xl font-bold transition-colors"
                  >
                    <Save className="w-5 h-5" />
                    Save Changes
                  </button>
                </div>

                <div className="mt-8 pt-8 border-t border-gray-100">
                  <h3 className="text-sm font-bold text-gray-400 uppercase tracking-wider mb-4">Data Management</h3>
                  <div className="flex gap-4">
                    <button
                      onClick={handleExport}
                      className="flex-1 flex items-center justify-center gap-2 bg-gray-50 hover:bg-gray-100 text-gray-700 py-3 rounded-xl font-bold transition-colors border border-gray-200 cursor-pointer"
                    >
                      Export Backup
                    </button>
                    <button
                      disabled={isLockedOrConsequence}
                      onClick={handleImport}
                      className={`flex-1 flex items-center justify-center gap-2 py-3 rounded-xl font-bold transition-colors border ${
                        isLockedOrConsequence
                          ? 'bg-gray-100 text-gray-400 border-gray-200 cursor-not-allowed'
                          : 'bg-gray-50 hover:bg-gray-100 text-gray-700 border-gray-200 cursor-pointer'
                      }`}
                      title={isLockedOrConsequence ? 'Backup import is disabled during active lockdown or consequence mode' : 'Import backup JSON'}
                    >
                      {isLockedOrConsequence && <Lock className="w-4 h-4 text-gray-400" />}
                      <span>Import Backup</span>
                    </button>
                  </div>
                  {isLockedOrConsequence ? (
                    <p className="text-xs text-red-500 font-semibold mt-3 text-center">
                      🔒 Backup import is disabled during active lockdown or consequence mode. Complete and pass your session first.
                    </p>
                  ) : (
                    <p className="text-xs text-gray-400 mt-3 text-center">
                      Exports everything including your custom allowed apps, resources, schedules, and logs into a single .json file.
                    </p>
                  )}
                </div>
            </div>
          )}

          {activeTab === 'prompts' && (
            <div className="max-w-5xl mx-auto">
              <div className="flex justify-between items-center mb-6">
                <p className="text-sm text-gray-600 leading-relaxed max-w-xl">
                  View the system prompts used across the application. These prompts are hardcoded and cannot be modified from the UI.
                </p>
              </div>
              
              <div className="space-y-8">
                {Object.keys(defaultPrompts).map(key => (
                  <div key={key} className="bg-gray-50 border border-gray-200 rounded-xl overflow-hidden flex flex-col">
                    <div className="bg-gray-100 px-4 py-3 border-b border-gray-200 flex justify-between items-center">
                      <h3 className="font-bold text-gray-800 text-sm font-mono">{key}</h3>
                    </div>
                    <textarea
                      readOnly
                      value={defaultPrompts[key]}
                      className="w-full h-48 p-4 text-xs font-mono text-gray-700 bg-white resize-none focus:outline-none focus:ring-0 cursor-text"
                    />
                  </div>
                ))}
              </div>
            </div>
          )}
        </div>
      </div>

      {showHardcoreModal && (
        <div className="fixed inset-0 bg-black/70 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-md w-full shadow-2xl border border-gray-100 flex flex-col animate-in fade-in zoom-in-95 duration-200">
            <div className="w-14 h-14 bg-red-100 text-red-600 rounded-2xl flex items-center justify-center mb-4 mx-auto">
              <Flame className="w-7 h-7" />
            </div>
            <h3 className="text-xl font-black text-gray-900 text-center mb-2">
              Enable Hardcore Mode (24/7)?
            </h3>
            <p className="text-sm text-gray-600 text-center leading-relaxed mb-4">
              In <strong>Hardcore Mode</strong>, QIEZKA operates 24 hours a day with <strong>NO daytime pause</strong>.
            </p>
            <div className="bg-red-50 p-4 rounded-2xl border border-red-200 mb-6 space-y-2 text-xs text-red-900">
              <p className="font-bold flex items-center gap-1.5 text-red-700">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                Continuous Daytime Enforcement
              </p>
              <p className="leading-relaxed text-red-800">
                If your homework expires or fails evaluation, distracting apps will remain restricted through morning, school, and work hours until you reschedule and pass AI evaluation.
              </p>
              <p className="text-[11px] text-red-600 font-semibold">
                You cannot switch back to Safemode while any daytime schedules are active.
              </p>
            </div>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setShowHardcoreModal(false)}
                className="flex-1 py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold rounded-xl text-sm transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmHardcore}
                className="flex-1 py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl text-sm transition-colors shadow-md flex items-center justify-center gap-2"
              >
                <Flame className="w-4 h-4" />
                Enable Hardcore
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

import React, { useState, useEffect, useCallback } from 'react';
import { 
  ShieldAlert, 
  ShieldCheck, 
  Lock, 
  Sparkles, 
  ArrowRight, 
  ArrowLeft, 
  Check, 
  CheckCircle2, 
  Clock, 
  Globe, 
  Key, 
  ExternalLink, 
  Camera, 
  Flame, 
  Info, 
  Eye, 
  EyeOff, 
  ChevronDown, 
  ChevronUp,
  Settings,
  Bell,
  BatteryCharging,
  AlertTriangle,
  RotateCw
} from 'lucide-react';
import { AppSettings } from '../types';
import { 
  checkPermissions, 
  openAccessibilitySettings, 
  openDeviceAdminSettings, 
  openDeviceAdminList, 
  openAppInfo, 
  requestBatteryOptimization, 
  requestNotificationPermission, 
  requestVpnPermission 
} from '../systemBridge';

interface OnboardingProps {
  onComplete: (config?: Partial<AppSettings>) => void;
}

export function Onboarding({ onComplete }: OnboardingProps) {
  const [step, setStep] = useState<1 | 2 | 3 | 4 | 5>(1);

  // Step 1: Expanded details for protocol explainer cards
  const [expandedCards, setExpandedCards] = useState<Record<string, boolean>>({
    selective: false,
    schedule: false,
    proof: false,
    consequence: false,
  });

  const toggleCard = (id: string) => {
    setExpandedCards(prev => ({ ...prev, [id]: !prev[id] }));
  };

  // Step 2: Terms and Conditions
  const [termsAccepted, setTermsAccepted] = useState({
    enforcement: false,
    privacy: false,
    proofOfWork: false,
    consequence: false,
  });

  const allTermsAccepted = Object.values(termsAccepted).every(Boolean);

  const handleAcceptAllTerms = () => {
    setTermsAccepted({
      enforcement: true,
      privacy: true,
      proofOfWork: true,
      consequence: true,
    });
  };

  // Step 3: Permissions State
  const [perms, setPerms] = useState<{
    isAccessibilityEnabled: boolean;
    isAdminActive: boolean;
    isDeviceOwner: boolean;
    isBatteryOptimizationIgnored: boolean;
    isNotificationGranted: boolean;
  }>({
    isAccessibilityEnabled: false,
    isAdminActive: false,
    isDeviceOwner: false,
    isBatteryOptimizationIgnored: false,
    isNotificationGranted: false,
  });
  const [isCheckingPerms, setIsCheckingPerms] = useState(false);
  const [permWarning, setPermWarning] = useState<string | null>(null);

  const verifyPermissions = useCallback(async () => {
    setIsCheckingPerms(true);
    try {
      const result = await checkPermissions();
      setPerms({
        isAccessibilityEnabled: result.isAccessibilityEnabled,
        isAdminActive: result.isAdminActive,
        isDeviceOwner: result.isDeviceOwner,
        isBatteryOptimizationIgnored: result.isBatteryOptimizationIgnored,
        isNotificationGranted: result.isNotificationGranted,
      });
      if (result.isAccessibilityEnabled || result.isDeviceOwner) {
        setPermWarning(null);
      }
    } catch (e) {
      console.warn('Failed to verify permissions in onboarding', e);
    } finally {
      setIsCheckingPerms(false);
    }
  }, []);

  useEffect(() => {
    verifyPermissions();
    const handleFocus = () => {
      verifyPermissions();
    };
    window.addEventListener('focus', handleFocus);
    document.addEventListener('visibilitychange', handleFocus);
    return () => {
      window.removeEventListener('focus', handleFocus);
      document.removeEventListener('visibilitychange', handleFocus);
    };
  }, [verifyPermissions]);

  // Step 4: Optional Setup Configuration
  const [apiKey, setApiKey] = useState('');
  const [showApiKey, setShowApiKey] = useState(false);
  const [simpleOcrKey, setSimpleOcrKey] = useState('');
  const [showOcrKey, setShowOcrKey] = useState(false);
  const [operatingMode, setOperatingModeState] = useState<'safemode' | 'hardcore'>('safemode');
  const [webProtectionMode, setWebProtectionModeState] = useState<'accessibility' | 'dual_hybrid' | 'off'>('accessibility');
  const [allowYoutube, setAllowYoutubeState] = useState(false);

  // Modals & Feedback
  const [showHardcoreModal, setShowHardcoreModal] = useState(false);
  const [webNotice, setWebNotice] = useState<string | null>(null);

  const handleSelectWebMode = async (mode: 'accessibility' | 'dual_hybrid') => {
    setWebNotice(null);
    if (mode === 'dual_hybrid') {
      try {
        const granted = await requestVpnPermission();
        if (!granted) {
          setWebNotice('VPN permission was cancelled or not granted. Kept Accessibility URL Guard.');
          setWebProtectionModeState('accessibility');
          return;
        }
        setWebNotice('✓ VPN permission granted. Dual Hybrid mode active.');
      } catch (e) {
        console.warn('VPN permission error in onboarding:', e);
      }
    }
    setWebProtectionModeState(mode);
  };

  const handleSelectOperatingMode = (mode: 'safemode' | 'hardcore') => {
    if (mode === 'hardcore') {
      setShowHardcoreModal(true);
    } else {
      setOperatingModeState('safemode');
    }
  };

  const handleConfirmHardcore = () => {
    setOperatingModeState('hardcore');
    setShowHardcoreModal(false);
  };

  const handleFinish = (skipCustomConfig = false) => {
    if (skipCustomConfig) {
      onComplete({
        role: 'just a guy',
        operatingMode: 'safemode',
        webProtectionMode: 'accessibility',
        allowYoutube: false,
      });
    } else {
      onComplete({
        role: 'just a guy',
        apiKey: apiKey.trim() || undefined,
        simpleOcrKey: simpleOcrKey.trim() || undefined,
        operatingMode,
        webProtectionMode,
        allowYoutube,
      });
    }
  };

  const openExternal = (url: string) => {
    if (typeof window !== 'undefined') {
      window.open(url, '_blank');
    }
  };

  return (
    <div className="fixed inset-0 bg-gray-950 overflow-y-auto p-4 sm:p-6 text-white z-50 flex flex-col items-center min-h-screen py-6 sm:py-8 pb-16 overscroll-contain">
      <div className="max-w-2xl w-full bg-gray-900 border border-gray-800 rounded-3xl p-6 sm:p-8 shadow-2xl my-auto transition-all">
        
        {/* Step Progress Header */}
        <div className="flex items-center justify-between border-b border-gray-800/80 pb-4 mb-6">
          <div className="flex items-center gap-2">
            <span className="w-2.5 h-2.5 rounded-full bg-red-600 animate-pulse" />
            <span className="text-[11px] font-black tracking-widest text-red-400 uppercase">
              {step === 1 && 'Step 1 of 5 • The Protocol'}
              {step === 2 && 'Step 2 of 5 • Nuclear Agreement'}
              {step === 3 && 'Step 3 of 5 • Device Permissions'}
              {step === 4 && 'Step 4 of 5 • Optional Setup'}
              {step === 5 && 'Step 5 of 5 • Ready to Focus'}
            </span>
          </div>
          <div className="flex items-center gap-1.5">
            {[1, 2, 3, 4, 5].map(s => (
              <div
                key={s}
                className={`h-1.5 rounded-full transition-all duration-300 ${
                  s === step ? 'w-6 bg-red-600' : s < step ? 'w-2.5 bg-red-800' : 'w-2 bg-gray-800'
                }`}
              />
            ))}
          </div>
        </div>

        {/* ── STEP 1: The QIEZKA Protocol ── */}
        {step === 1 && (
          <div className="animate-in fade-in duration-200">
            <div className="flex flex-col items-center mb-6 text-center">
              <div className="w-16 h-16 sm:w-20 sm:h-20 bg-red-950/80 text-red-500 rounded-2xl flex items-center justify-center mb-3 border border-red-800 shadow-inner">
                <Lock className="w-8 h-8 sm:w-10 sm:h-10 text-red-500" />
              </div>
              <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white mb-1.5 uppercase">
                Welcome to QIEZKA
              </h1>
              <p className="text-xs sm:text-sm text-gray-400 max-w-md">
                The selective anti-distraction protocol designed to eliminate procrastination and protect deep focus.
              </p>
            </div>

            {/* Interactive Explainer Cards */}
            <div className="space-y-3 mb-6">
              {/* Card 1: Selective Focus */}
              <div 
                onClick={() => toggleCard('selective')}
                className="p-4 bg-gray-800/60 hover:bg-gray-800/90 border border-gray-700/80 rounded-2xl cursor-pointer transition-colors"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-xl bg-emerald-500/20 text-emerald-400 flex items-center justify-center shrink-0">
                      <ShieldCheck className="w-4 h-4" />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-gray-200">Selective Focus, Not a Bricked Kiosk</h3>
                      <p className="text-xs text-gray-400">Essential tools remain accessible; distractions are blocked.</p>
                    </div>
                  </div>
                  {expandedCards.selective ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
                </div>
                {expandedCards.selective && (
                  <div className="mt-3 pt-3 border-t border-gray-700/50 text-xs text-gray-300 leading-relaxed space-y-1">
                    <p>• <strong>Always Allowed:</strong> Phone calls, SMS, Camera, Calculator, Notes, Clock, Authenticator apps, and messaging apps.</p>
                    <p>• <strong>Strictly Blocked:</strong> Mobile games, social media apps (TikTok, Instagram, Facebook), distracting web domains, and YouTube Shorts.</p>
                  </div>
                )}
              </div>

              {/* Card 2: Scheduled Sessions & Floating Bubble */}
              <div 
                onClick={() => toggleCard('schedule')}
                className="p-4 bg-gray-800/60 hover:bg-gray-800/90 border border-gray-700/80 rounded-2xl cursor-pointer transition-colors"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-xl bg-blue-500/20 text-blue-400 flex items-center justify-center shrink-0">
                      <Clock className="w-4 h-4" />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-gray-200">Scheduled Sessions & Floating Timer Ball</h3>
                      <p className="text-xs text-gray-400">Automated lock windows with a live assistive overlay.</p>
                    </div>
                  </div>
                  {expandedCards.schedule ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
                </div>
                {expandedCards.schedule && (
                  <div className="mt-3 pt-3 border-t border-gray-700/50 text-xs text-gray-300 leading-relaxed space-y-1">
                    <p>• Create custom study sessions (e.g. 7:00 PM – 8:00 PM) defining your homework goals and duration.</p>
                    <p>• When active, a non-intrusive floating timer ball floats on screen, showing remaining time and providing 1-tap return to homework.</p>
                  </div>
                )}
              </div>

              {/* Card 3: Proof of Completed Homework */}
              <div 
                onClick={() => toggleCard('proof')}
                className="p-4 bg-gray-800/60 hover:bg-gray-800/90 border border-gray-700/80 rounded-2xl cursor-pointer transition-colors"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-xl bg-purple-500/20 text-purple-400 flex items-center justify-center shrink-0">
                      <Camera className="w-4 h-4" />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-gray-200">Unlocking: Proof of Completed Homework</h3>
                      <p className="text-xs text-gray-400">Unlock early by taking a photograph or screenshot.</p>
                    </div>
                  </div>
                  {expandedCards.proof ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
                </div>
                {expandedCards.proof && (
                  <div className="mt-3 pt-3 border-t border-gray-700/50 text-xs text-gray-300 leading-relaxed space-y-1">
                    <p>• Need to unlock before the timer expires? Submit <strong>proof of completed homework</strong> by <strong>taking a photograph or screenshot</strong> (handwritten notebook work, worksheets, typed code, or textbook exercises).</p>
                    <p>• AI transcribes your photo and verifies that your completed work meets the session rubric requirements.</p>
                  </div>
                )}
              </div>

              {/* Card 4: Consequence Mode */}
              <div 
                onClick={() => toggleCard('consequence')}
                className="p-4 bg-gray-800/60 hover:bg-gray-800/90 border border-gray-700/80 rounded-2xl cursor-pointer transition-colors"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className="w-8 h-8 rounded-xl bg-red-500/20 text-red-400 flex items-center justify-center shrink-0">
                      <Flame className="w-4 h-4" />
                    </div>
                    <div>
                      <h3 className="text-sm font-bold text-gray-200">Consequence Mode Accountability</h3>
                      <p className="text-xs text-gray-400">Failure to complete homework triggers lasting restrictions.</p>
                    </div>
                  </div>
                  {expandedCards.consequence ? <ChevronUp className="w-4 h-4 text-gray-400" /> : <ChevronDown className="w-4 h-4 text-gray-400" />}
                </div>
                {expandedCards.consequence && (
                  <div className="mt-3 pt-3 border-t border-gray-700/50 text-xs text-gray-300 leading-relaxed space-y-1">
                    <p>• If a session timer expires without submitting and passing homework, <strong>Consequence Mode engages</strong>.</p>
                    <p>• Distracting apps remain restricted during operating hours (7:00 PM – 3:00 AM in Safemode, or 24/7 in Hardcore) until you reschedule and pass the failed session.</p>
                  </div>
                )}
              </div>
            </div>

            {/* Step 1 Actions */}
            <button
              onClick={() => setStep(2)}
              className="w-full py-3.5 bg-red-600 hover:bg-red-700 active:bg-red-800 text-white font-bold rounded-xl transition-colors text-sm sm:text-base cursor-pointer flex items-center justify-center gap-2 shadow-lg shadow-red-900/30"
            >
              <span>Continue to Nuclear Agreement</span>
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* ── STEP 2: Nuclear Agreement (Terms & Conditions) ── */}
        {step === 2 && (
          <div className="animate-in fade-in duration-200">
            <div className="flex flex-col items-center mb-6 text-center">
              <div className="w-16 h-16 sm:w-20 sm:h-20 bg-red-950 text-red-500 rounded-2xl flex items-center justify-center mb-3 border border-red-800 shadow-inner">
                <ShieldAlert className="w-8 h-8 sm:w-10 sm:h-10 text-red-500" />
              </div>
              <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white mb-1.5 uppercase">
                Nuclear Agreement
              </h1>
              <p className="text-xs sm:text-sm text-gray-400 max-w-md">
                Acknowledge the core operating rules and enforcement terms before proceeding.
              </p>
            </div>

            <div className="flex justify-end mb-3">
              <button
                type="button"
                onClick={handleAcceptAllTerms}
                className="text-xs font-bold text-red-400 hover:text-red-300 transition-colors cursor-pointer flex items-center gap-1"
              >
                <Check className="w-3.5 h-3.5" />
                <span>Accept All Terms</span>
              </button>
            </div>

            {/* Terms List */}
            <div className="space-y-3 mb-6">
              {/* Term 1 */}
              <label className="flex items-start p-3.5 bg-gray-800/60 hover:bg-gray-800 border border-gray-700/80 rounded-2xl cursor-pointer transition-colors group">
                <div className="relative flex items-center justify-center mt-0.5">
                  <input
                    type="checkbox"
                    className="sr-only"
                    checked={termsAccepted.enforcement}
                    onChange={(e) => setTermsAccepted({ ...termsAccepted, enforcement: e.target.checked })}
                  />
                  <div className={`w-5 h-5 rounded border-2 flex items-center justify-center transition-colors ${
                    termsAccepted.enforcement ? 'bg-red-600 border-red-600' : 'border-gray-600 group-hover:border-red-500'
                  }`}>
                    {termsAccepted.enforcement && <Check className="w-3 h-3 text-white" />}
                  </div>
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-bold text-gray-200">Real System Enforcement</h3>
                  <p className="text-xs text-gray-400 mt-0.5 leading-relaxed">
                    I understand that QIEZKA uses Accessibility & Device Administration. It cannot be swiped away, dismissed with gestures, or bypassed by restarting my device.
                  </p>
                </div>
              </label>

              {/* Term 2 */}
              <label className="flex items-start p-3.5 bg-gray-800/60 hover:bg-gray-800 border border-gray-700/80 rounded-2xl cursor-pointer transition-colors group">
                <div className="relative flex items-center justify-center mt-0.5">
                  <input
                    type="checkbox"
                    className="sr-only"
                    checked={termsAccepted.privacy}
                    onChange={(e) => setTermsAccepted({ ...termsAccepted, privacy: e.target.checked })}
                  />
                  <div className={`w-5 h-5 rounded border-2 flex items-center justify-center transition-colors ${
                    termsAccepted.privacy ? 'bg-red-600 border-red-600' : 'border-gray-600 group-hover:border-red-500'
                  }`}>
                    {termsAccepted.privacy && <Check className="w-3 h-3 text-white" />}
                  </div>
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-bold text-gray-200">Zero Privacy Intrusion</h3>
                  <p className="text-xs text-gray-400 mt-0.5 leading-relaxed">
                    I understand that QIEZKA runs 100% on-device with no third-party tracking. Essential tools (calling, SMS, messaging, camera) are never locked.
                  </p>
                </div>
              </label>

              {/* Term 3 */}
              <label className="flex items-start p-3.5 bg-gray-800/60 hover:bg-gray-800 border border-gray-700/80 rounded-2xl cursor-pointer transition-colors group">
                <div className="relative flex items-center justify-center mt-0.5">
                  <input
                    type="checkbox"
                    className="sr-only"
                    checked={termsAccepted.proofOfWork}
                    onChange={(e) => setTermsAccepted({ ...termsAccepted, proofOfWork: e.target.checked })}
                  />
                  <div className={`w-5 h-5 rounded border-2 flex items-center justify-center transition-colors ${
                    termsAccepted.proofOfWork ? 'bg-red-600 border-red-600' : 'border-gray-600 group-hover:border-red-500'
                  }`}>
                    {termsAccepted.proofOfWork && <Check className="w-3 h-3 text-white" />}
                  </div>
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-bold text-gray-200">Genuine Homework Proof Required</h3>
                  <p className="text-xs text-gray-400 mt-0.5 leading-relaxed">
                    I understand that early unlocking requires taking a photograph or screenshot showing genuine proof of completed homework matching my rubric guidelines.
                  </p>
                </div>
              </label>

              {/* Term 4 */}
              <label className="flex items-start p-3.5 bg-gray-800/60 hover:bg-gray-800 border border-gray-700/80 rounded-2xl cursor-pointer transition-colors group">
                <div className="relative flex items-center justify-center mt-0.5">
                  <input
                    type="checkbox"
                    className="sr-only"
                    checked={termsAccepted.consequence}
                    onChange={(e) => setTermsAccepted({ ...termsAccepted, consequence: e.target.checked })}
                  />
                  <div className={`w-5 h-5 rounded border-2 flex items-center justify-center transition-colors ${
                    termsAccepted.consequence ? 'bg-red-600 border-red-600' : 'border-gray-600 group-hover:border-red-500'
                  }`}>
                    {termsAccepted.consequence && <Check className="w-3 h-3 text-white" />}
                  </div>
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-bold text-gray-200">Consequence Accountability</h3>
                  <p className="text-xs text-gray-400 mt-0.5 leading-relaxed">
                    I understand that if I let a session expire without passing, Consequence Mode will lock distractions during operating hours until I reschedule and pass.
                  </p>
                </div>
              </label>
            </div>

            {/* Step 2 Actions */}
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setStep(1)}
                className="px-4 py-3.5 bg-gray-800 hover:bg-gray-700 text-gray-300 font-bold rounded-xl transition-colors text-sm cursor-pointer flex items-center gap-1.5"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Back</span>
              </button>

              <button
                type="button"
                disabled={!allTermsAccepted}
                onClick={() => setStep(3)}
                className="flex-1 py-3.5 bg-red-600 hover:bg-red-700 disabled:bg-gray-800 disabled:text-gray-600 text-white font-bold rounded-xl transition-colors text-sm sm:text-base cursor-pointer disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-lg shadow-red-900/30"
              >
                <span>I Accept the Consequences</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

        {/* ── STEP 3: Device Permissions ── */}
        {step === 3 && (
          <div className="animate-in fade-in duration-200">
            <div className="flex flex-col items-center mb-4 text-center">
              <div className="w-14 h-14 bg-red-950 text-red-500 rounded-2xl flex items-center justify-center mb-2.5 border border-red-800">
                <Settings className="w-7 h-7 text-red-500" />
              </div>
              <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white mb-1 uppercase">
                Device Permissions
              </h1>
              <p className="text-xs text-gray-400 max-w-md">
                QIEZKA requires accessibility and system permissions to monitor distracting apps and enforce focus windows.
              </p>
            </div>

            <div className="flex justify-between items-center mb-3">
              <span className="text-[11px] font-bold text-gray-400 uppercase tracking-wider">
                Required & Recommended Permissions
              </span>
              <button
                type="button"
                onClick={verifyPermissions}
                disabled={isCheckingPerms}
                className="text-xs font-bold text-red-400 hover:text-red-300 transition-colors flex items-center gap-1 cursor-pointer disabled:opacity-50"
              >
                <RotateCw className={`w-3 h-3 ${isCheckingPerms ? 'animate-spin' : ''}`} />
                <span>Refresh Status</span>
              </button>
            </div>

            <div className="space-y-3 mb-6">
              {/* Permission 1: Accessibility Service (Essential) */}
              <div className={`p-4 rounded-2xl border transition-all ${
                perms.isAccessibilityEnabled || perms.isDeviceOwner 
                  ? 'bg-emerald-950/20 border-emerald-800/60' 
                  : 'bg-gray-800/60 border-red-800/80'
              }`}>
                <div className="flex items-center justify-between mb-1.5">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-bold text-gray-200 uppercase tracking-wider">
                      1. Accessibility Service (Essential)
                    </span>
                  </div>
                  {perms.isAccessibilityEnabled || perms.isDeviceOwner ? (
                    <span className="px-2 py-0.5 bg-emerald-950 text-emerald-300 border border-emerald-800 rounded-lg text-[10px] font-bold flex items-center gap-1">
                      <Check className="w-3 h-3" /> Active
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 bg-red-950 text-red-300 border border-red-800 rounded-lg text-[10px] font-bold">
                      Required
                    </span>
                  )}
                </div>
                <p className="text-xs text-gray-400 mb-3 leading-relaxed">
                  Allows QIEZKA to intercept distracting apps and inspect browser address bars during focus sessions.
                </p>
                <button
                  type="button"
                  onClick={async () => {
                    await openAccessibilitySettings();
                    setTimeout(verifyPermissions, 1000);
                  }}
                  className={`w-full py-2.5 rounded-xl font-bold text-xs transition-colors flex items-center justify-center gap-2 cursor-pointer ${
                    perms.isAccessibilityEnabled || perms.isDeviceOwner
                      ? 'bg-gray-800 border border-emerald-700/60 text-emerald-300'
                      : 'bg-red-600 hover:bg-red-700 text-white shadow-md'
                  }`}
                >
                  <Settings className="w-3.5 h-3.5" />
                  <span>{perms.isAccessibilityEnabled || perms.isDeviceOwner ? 'Accessibility Service Enabled' : 'Grant Accessibility Permission'}</span>
                </button>

                {(!perms.isAccessibilityEnabled && !perms.isDeviceOwner) && (
                  <div className="mt-2.5 pt-2.5 border-t border-gray-700/40 flex items-center justify-between text-[11px] text-gray-400">
                    <span>Grayed out on Android 13+? Allow restricted settings:</span>
                    <button
                      type="button"
                      onClick={() => openAppInfo()}
                      className="text-red-400 hover:text-red-300 font-bold ml-2 underline cursor-pointer shrink-0"
                    >
                      App Info
                    </button>
                  </div>
                )}
              </div>

              {/* Permission 2: Allow Notifications */}
              <div className={`p-4 rounded-2xl border transition-all ${
                perms.isNotificationGranted ? 'bg-emerald-950/20 border-emerald-800/60' : 'bg-gray-800/60 border-gray-700/80'
              }`}>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-xs font-bold text-gray-200 uppercase tracking-wider">
                    2. Allow Notifications (Recommended)
                  </span>
                  {perms.isNotificationGranted ? (
                    <span className="px-2 py-0.5 bg-emerald-950 text-emerald-300 border border-emerald-800 rounded-lg text-[10px] font-bold flex items-center gap-1">
                      <Check className="w-3 h-3" /> Allowed
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 bg-gray-800 text-gray-400 border border-gray-700 rounded-lg text-[10px] font-bold">
                      Recommended
                    </span>
                  )}
                </div>
                <p className="text-xs text-gray-400 mb-3 leading-relaxed">
                  Enables live countdown status, session start warnings, and consequence mode alerts.
                </p>
                <button
                  type="button"
                  onClick={async () => {
                    await requestNotificationPermission();
                    setTimeout(verifyPermissions, 1000);
                  }}
                  className={`w-full py-2.5 rounded-xl font-bold text-xs transition-colors flex items-center justify-center gap-2 cursor-pointer ${
                    perms.isNotificationGranted
                      ? 'bg-gray-800 border border-emerald-700/60 text-emerald-300'
                      : 'bg-sky-600 hover:bg-sky-700 text-white'
                  }`}
                >
                  <Bell className="w-3.5 h-3.5" />
                  <span>{perms.isNotificationGranted ? 'Notifications Allowed' : 'Allow Notifications'}</span>
                </button>
              </div>

              {/* Permission 3: Unrestricted Battery / Background */}
              <div className={`p-4 rounded-2xl border transition-all ${
                perms.isBatteryOptimizationIgnored ? 'bg-emerald-950/20 border-emerald-800/60' : 'bg-gray-800/60 border-gray-700/80'
              }`}>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-xs font-bold text-gray-200 uppercase tracking-wider">
                    3. Unrestricted Background / Battery
                  </span>
                  {perms.isBatteryOptimizationIgnored ? (
                    <span className="px-2 py-0.5 bg-emerald-950 text-emerald-300 border border-emerald-800 rounded-lg text-[10px] font-bold flex items-center gap-1">
                      <Check className="w-3 h-3" /> Unrestricted
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 bg-gray-800 text-gray-400 border border-gray-700 rounded-lg text-[10px] font-bold">
                      Recommended
                    </span>
                  )}
                </div>
                <p className="text-xs text-gray-400 mb-3 leading-relaxed">
                  Prevents aggressive device battery optimizers (Samsung, Xiaomi, Pixel) from stopping focus mode.
                </p>
                <button
                  type="button"
                  onClick={async () => {
                    await requestBatteryOptimization();
                    setTimeout(verifyPermissions, 1000);
                  }}
                  className={`w-full py-2.5 rounded-xl font-bold text-xs transition-colors flex items-center justify-center gap-2 cursor-pointer ${
                    perms.isBatteryOptimizationIgnored
                      ? 'bg-gray-800 border border-emerald-700/60 text-emerald-300'
                      : 'bg-amber-600 hover:bg-amber-700 text-white'
                  }`}
                >
                  <BatteryCharging className="w-3.5 h-3.5" />
                  <span>{perms.isBatteryOptimizationIgnored ? 'Background Usage Unrestricted' : 'Allow Unrestricted Background'}</span>
                </button>
              </div>

              {/* Permission 4: Device Administrator (Optional) */}
              <div className={`p-4 rounded-2xl border transition-all ${
                perms.isAdminActive ? 'bg-emerald-950/20 border-emerald-800/60' : 'bg-gray-800/60 border-gray-700/80'
              }`}>
                <div className="flex items-center justify-between mb-1.5">
                  <span className="text-xs font-bold text-gray-200 uppercase tracking-wider">
                    4. Device Administrator (Optional)
                  </span>
                  {perms.isAdminActive ? (
                    <span className="px-2 py-0.5 bg-emerald-950 text-emerald-300 border border-emerald-800 rounded-lg text-[10px] font-bold flex items-center gap-1">
                      <Check className="w-3 h-3" /> Active
                    </span>
                  ) : (
                    <span className="px-2 py-0.5 bg-gray-800 text-gray-400 border border-gray-700 rounded-lg text-[10px] font-bold">
                      Optional Anti-Uninstall
                    </span>
                  )}
                </div>
                <p className="text-xs text-gray-400 mb-3 leading-relaxed">
                  Blocks uninstalling QIEZKA during active focus sessions. Can be disabled anytime outside lockdown.
                </p>
                <button
                  type="button"
                  onClick={async () => {
                    await openDeviceAdminSettings();
                    setTimeout(verifyPermissions, 1000);
                  }}
                  className={`w-full py-2.5 rounded-xl font-bold text-xs transition-colors flex items-center justify-center gap-2 cursor-pointer ${
                    perms.isAdminActive
                      ? 'bg-gray-800 border border-emerald-700/60 text-emerald-300'
                      : 'bg-indigo-600 hover:bg-indigo-700 text-white'
                  }`}
                >
                  <ShieldCheck className="w-3.5 h-3.5" />
                  <span>{perms.isAdminActive ? 'Device Administrator Active' : 'Activate Device Administrator'}</span>
                </button>
              </div>
            </div>

            {permWarning && (
              <div className="mb-4 p-3 bg-red-950/40 border border-red-800 rounded-xl flex items-start gap-2.5 text-xs text-red-300">
                <AlertTriangle className="w-4 h-4 text-red-400 shrink-0 mt-0.5" />
                <span>{permWarning}</span>
              </div>
            )}

            {/* Step 3 Actions */}
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setStep(2)}
                className="px-4 py-3.5 bg-gray-800 hover:bg-gray-700 text-gray-300 font-bold rounded-xl transition-colors text-sm cursor-pointer flex items-center gap-1.5"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Back</span>
              </button>

              <button
                type="button"
                onClick={() => {
                  if (!perms.isAccessibilityEnabled && !perms.isDeviceOwner) {
                    setPermWarning('Notice: Accessibility Service is required to intercept distracting apps. You can grant it now or configure it later.');
                  }
                  setStep(4);
                }}
                className="flex-1 py-3.5 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl transition-colors text-sm sm:text-base cursor-pointer flex items-center justify-center gap-2 shadow-lg shadow-red-900/30"
              >
                <span>Continue to Quick Setup</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

        {/* ── STEP 4: Optional Quick Setup (Keys & Modes) ── */}
        {step === 4 && (
          <div className="animate-in fade-in duration-200">
            <div className="flex flex-col items-center mb-4 text-center">
              <div className="w-14 h-14 bg-red-950 text-red-500 rounded-2xl flex items-center justify-center mb-2.5 border border-red-800">
                <Key className="w-7 h-7 text-red-500" />
              </div>
              <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white mb-1 uppercase">
                Optional Quick Setup
              </h1>
              <p className="text-xs text-gray-400 max-w-md">
                Pre-configure your AI evaluation, operating mode, and web protection. All items are optional.
              </p>
            </div>

            {/* Banner: 100% Optional */}
            <div className="mb-4 p-3 bg-red-950/40 border border-red-800/60 rounded-xl flex items-center gap-2.5 text-xs text-red-300">
              <Info className="w-4 h-4 text-red-400 shrink-0" />
              <span><strong>100% Optional:</strong> If you don't have API keys handy, tap <strong>"Skip for Now"</strong> below to start immediately.</span>
            </div>

            <div className="space-y-4 mb-6">
              {/* 1. Gemini API Key */}
              <div className="p-4 bg-gray-800/60 border border-gray-700/80 rounded-2xl">
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-xs font-bold text-gray-200 uppercase tracking-wider flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-amber-400" />
                    <span>Gemini API Key (Google AI Studio)</span>
                  </label>
                  <button
                    type="button"
                    onClick={() => openExternal('https://aistudio.google.com/app/apikey')}
                    className="text-[11px] font-bold text-red-400 hover:text-red-300 transition-colors flex items-center gap-1 cursor-pointer"
                  >
                    <span>Get Free Key</span>
                    <ExternalLink className="w-3 h-3" />
                  </button>
                </div>
                <p className="text-[11px] text-gray-400 mb-2 leading-relaxed">
                  Required for online AI homework grading. Scheduling and locks work offline; AI grading uses your key.
                </p>
                <div className="relative">
                  <input
                    type={showApiKey ? 'text' : 'password'}
                    value={apiKey}
                    onChange={(e) => setApiKey(e.target.value)}
                    placeholder="AIzaSy..."
                    className="w-full bg-gray-900 border border-gray-700 rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-gray-600 focus:outline-none focus:border-red-500 transition-colors pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowApiKey(!showApiKey)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300"
                  >
                    {showApiKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* 2. Free OCR Key */}
              <div className="p-4 bg-gray-800/60 border border-gray-700/80 rounded-2xl">
                <div className="flex items-center justify-between mb-1.5">
                  <label className="text-xs font-bold text-gray-200 uppercase tracking-wider flex items-center gap-1.5">
                    <Camera className="w-3.5 h-3.5 text-blue-400" />
                    <span>Free OCR Key (OCR.space)</span>
                  </label>
                  <button
                    type="button"
                    onClick={() => openExternal('https://ocr.space/ocrapi/freekey')}
                    className="text-[11px] font-bold text-red-400 hover:text-red-300 transition-colors flex items-center gap-1 cursor-pointer"
                  >
                    <span>Get Free Key</span>
                    <ExternalLink className="w-3 h-3" />
                  </button>
                </div>
                <p className="text-[11px] text-gray-400 mb-2 leading-relaxed">
                  Used to transcribe handwritten homework photos or screenshots into text.
                </p>
                <div className="relative">
                  <input
                    type={showOcrKey ? 'text' : 'password'}
                    value={simpleOcrKey}
                    onChange={(e) => setSimpleOcrKey(e.target.value)}
                    placeholder="K8..."
                    className="w-full bg-gray-900 border border-gray-700 rounded-xl px-3.5 py-2.5 text-xs text-white placeholder-gray-600 focus:outline-none focus:border-red-500 transition-colors pr-10"
                  />
                  <button
                    type="button"
                    onClick={() => setShowOcrKey(!showOcrKey)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-500 hover:text-gray-300"
                  >
                    {showOcrKey ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* 3. Operating Mode Selection with Hardcore Verification */}
              <div className="p-4 bg-gray-800/60 border border-gray-700/80 rounded-2xl">
                <label className="text-xs font-bold text-gray-200 uppercase tracking-wider block mb-2">
                  Operating Mode
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
                  <div
                    onClick={() => handleSelectOperatingMode('safemode')}
                    className={`p-3 rounded-xl border cursor-pointer transition-all ${
                      operatingMode === 'safemode'
                        ? 'bg-red-950/40 border-red-600 text-white shadow-sm'
                        : 'bg-gray-900/60 border-gray-700/70 text-gray-400 hover:border-gray-600'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-bold text-gray-200">Safemode (Recommended)</span>
                      {operatingMode === 'safemode' && <CheckCircle2 className="w-4 h-4 text-red-500" />}
                    </div>
                    <p className="text-[11px] text-gray-400 leading-tight">
                      Active 7:00 PM – 3:00 AM. Consequence pauses during daytime classes and commutes.
                    </p>
                  </div>

                  <div
                    onClick={() => handleSelectOperatingMode('hardcore')}
                    className={`p-3 rounded-xl border cursor-pointer transition-all ${
                      operatingMode === 'hardcore'
                        ? 'bg-red-950/40 border-red-600 text-white shadow-sm'
                        : 'bg-gray-900/60 border-gray-700/70 text-gray-400 hover:border-gray-600'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-bold text-gray-200">Hardcore (24/7)</span>
                      {operatingMode === 'hardcore' && <CheckCircle2 className="w-4 h-4 text-red-500" />}
                    </div>
                    <p className="text-[11px] text-gray-400 leading-tight">
                      Round-the-clock enforcement. Consequence restrictions remain active continuous 24/7.
                    </p>
                  </div>
                </div>
              </div>

              {/* 4. Web & YouTube Protection with VPN Request */}
              <div className="p-4 bg-gray-800/60 border border-gray-700/80 rounded-2xl">
                <label className="text-xs font-bold text-gray-200 uppercase tracking-wider block mb-2">
                  Web & Browser Protection
                </label>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 mb-2">
                  <div
                    onClick={() => handleSelectWebMode('accessibility')}
                    className={`p-3 rounded-xl border cursor-pointer transition-all ${
                      webProtectionMode === 'accessibility'
                        ? 'bg-red-950/40 border-red-600 text-white shadow-sm'
                        : 'bg-gray-900/60 border-gray-700/70 text-gray-400 hover:border-gray-600'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-bold text-gray-200">Accessibility Guard</span>
                      {webProtectionMode === 'accessibility' && <CheckCircle2 className="w-4 h-4 text-red-500" />}
                    </div>
                    <p className="text-[11px] text-gray-400 leading-tight">
                      Lightweight address bar inspection. Leaves VPN slot free for university or home VPNs.
                    </p>
                  </div>

                  <div
                    onClick={() => handleSelectWebMode('dual_hybrid')}
                    className={`p-3 rounded-xl border cursor-pointer transition-all ${
                      webProtectionMode === 'dual_hybrid'
                        ? 'bg-red-950/40 border-red-600 text-white shadow-sm'
                        : 'bg-gray-900/60 border-gray-700/70 text-gray-400 hover:border-gray-600'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1">
                      <span className="text-xs font-bold text-gray-200">Dual Hybrid (Max Armor)</span>
                      {webProtectionMode === 'dual_hybrid' && <CheckCircle2 className="w-4 h-4 text-red-500" />}
                    </div>
                    <p className="text-[11px] text-gray-400 leading-tight">
                      URL address bar inspection + local on-device DNS loopback sinkhole (VPN).
                    </p>
                  </div>
                </div>

                {webNotice && (
                  <div className={`p-2 rounded-lg text-[11px] font-medium mb-2 ${
                    webNotice.startsWith('✓') ? 'bg-emerald-950/40 text-emerald-300 border border-emerald-800/60' : 'bg-amber-950/40 text-amber-300 border border-amber-800/60'
                  }`}>
                    {webNotice}
                  </div>
                )}

                {/* YouTube Toggle */}
                <label className="flex items-center justify-between pt-2 border-t border-gray-700/50 cursor-pointer">
                  <div>
                    <span className="text-xs font-bold text-gray-200 block">Allow YouTube (Academic Web Only)</span>
                    <span className="text-[11px] text-gray-400">Allows lectures on Chrome; strictly blocks Shorts. Native YouTube app is always blocked.</span>
                  </div>
                  <input
                    type="checkbox"
                    checked={allowYoutube}
                    onChange={(e) => setAllowYoutubeState(e.target.checked)}
                    className="w-4 h-4 accent-red-600 rounded cursor-pointer shrink-0 ml-3"
                  />
                </label>
              </div>
            </div>

            {/* Step 4 Actions */}
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setStep(3)}
                className="px-4 py-3.5 bg-gray-800 hover:bg-gray-700 text-gray-300 font-bold rounded-xl transition-colors text-sm cursor-pointer flex items-center gap-1.5"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Back</span>
              </button>

              <button
                type="button"
                onClick={() => handleFinish(true)}
                className="px-4 py-3.5 bg-gray-800 hover:bg-gray-700 text-gray-300 font-bold rounded-xl transition-colors text-sm cursor-pointer"
              >
                Skip for Now
              </button>

              <button
                type="button"
                onClick={() => setStep(5)}
                className="flex-1 py-3.5 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl transition-colors text-sm sm:text-base cursor-pointer flex items-center justify-center gap-2 shadow-lg shadow-red-900/30"
              >
                <span>Save & Continue</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

        {/* ── STEP 5: Ready to Focus ── */}
        {step === 5 && (
          <div className="animate-in fade-in duration-200">
            <div className="flex flex-col items-center mb-6 text-center">
              <div className="w-16 h-16 sm:w-20 sm:h-20 bg-emerald-950 text-emerald-400 rounded-2xl flex items-center justify-center mb-3 border border-emerald-800 shadow-inner">
                <ShieldCheck className="w-8 h-8 sm:w-10 sm:h-10 text-emerald-400" />
              </div>
              <h1 className="text-2xl sm:text-3xl font-black tracking-tight text-white mb-1.5 uppercase">
                You're Ready to Focus
              </h1>
              <p className="text-xs sm:text-sm text-gray-400 max-w-md">
                QIEZKA is armed and ready. Review your configuration and enter your study command center.
              </p>
            </div>

            {/* Summary Box */}
            <div className="p-4 bg-gray-800/60 border border-gray-700/80 rounded-2xl space-y-2.5 mb-6 text-xs">
              <div className="flex items-center justify-between pb-2 border-b border-gray-700/50">
                <span className="text-gray-400 font-medium">Operating Mode</span>
                <span className="font-bold text-gray-200 uppercase">
                  {operatingMode === 'hardcore' ? '⚡ Hardcore (24/7 continuous)' : '🕒 Safemode (7:00 PM – 3:00 AM)'}
                </span>
              </div>

              <div className="flex items-center justify-between pb-2 border-b border-gray-700/50">
                <span className="text-gray-400 font-medium">Web Protection</span>
                <span className="font-bold text-gray-200">
                  {webProtectionMode === 'dual_hybrid' ? 'Dual Hybrid (URL Guard + DNS Sinkhole)' : 'Accessibility URL Guard'}
                </span>
              </div>

              <div className="flex items-center justify-between pb-2 border-b border-gray-700/50">
                <span className="text-gray-400 font-medium">Permissions Status</span>
                <span className={`font-bold ${perms.isAccessibilityEnabled || perms.isDeviceOwner ? 'text-emerald-400' : 'text-amber-400'}`}>
                  {perms.isAccessibilityEnabled || perms.isDeviceOwner ? '✓ Accessibility Granted' : '⚠ Accessibility Pending'}
                </span>
              </div>

              <div className="flex items-center justify-between pb-2 border-b border-gray-700/50">
                <span className="text-gray-400 font-medium">YouTube Policy</span>
                <span className="font-bold text-gray-200">
                  {allowYoutube ? 'Academic Only (Web, No Shorts)' : 'Completely Blocked'}
                </span>
              </div>

              <div className="flex items-center justify-between">
                <span className="text-gray-400 font-medium">Gemini AI Key</span>
                <span className={`font-bold ${apiKey ? 'text-emerald-400' : 'text-amber-400'}`}>
                  {apiKey ? '✓ Configured' : '⚠ Add Later in Settings'}
                </span>
              </div>
            </div>

            {/* Orientation Tips */}
            <div className="p-4 bg-gray-900/80 border border-gray-800 rounded-2xl space-y-2 mb-6 text-xs text-gray-400">
              <div className="flex items-start gap-2">
                <span className="text-red-500 font-bold">1.</span>
                <span>Tap <strong>"+ Add Schedule"</strong> on the Dashboard to set your homework times and durations.</span>
              </div>
              <div className="flex items-start gap-2">
                <span className="text-red-500 font-bold">2.</span>
                <span>You can upload rubrics, add custom resources, or update allowed apps anytime in Settings.</span>
              </div>
            </div>

            {/* Step 5 Actions */}
            <div className="flex items-center gap-3">
              <button
                type="button"
                onClick={() => setStep(4)}
                className="px-4 py-3.5 bg-gray-800 hover:bg-gray-700 text-gray-300 font-bold rounded-xl transition-colors text-sm cursor-pointer flex items-center gap-1.5"
              >
                <ArrowLeft className="w-4 h-4" />
                <span>Back</span>
              </button>

              <button
                type="button"
                onClick={() => handleFinish(false)}
                className="flex-1 py-3.5 bg-red-600 hover:bg-red-700 active:bg-red-800 text-white font-bold rounded-xl transition-colors text-sm sm:text-base cursor-pointer flex items-center justify-center gap-2 shadow-lg shadow-red-900/30"
              >
                <span>Enter QIEZKA Dashboard</span>
                <ArrowRight className="w-4 h-4" />
              </button>
            </div>
          </div>
        )}

      </div>

      {/* ── Hardcore Confirmation Modal ── */}
      {showHardcoreModal && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-gray-900 border border-gray-700 rounded-3xl p-6 sm:p-7 max-w-md w-full shadow-2xl flex flex-col animate-in fade-in zoom-in-95 duration-200">
            <div className="w-14 h-14 bg-red-950 text-red-500 rounded-2xl flex items-center justify-center mb-4 mx-auto border border-red-800">
              <Flame className="w-7 h-7" />
            </div>
            <h3 className="text-xl font-black text-white text-center mb-2">
              Enable Hardcore Mode (24/7)?
            </h3>
            <p className="text-xs sm:text-sm text-gray-300 text-center leading-relaxed mb-4">
              In <strong>Hardcore Mode</strong>, QIEZKA operates around the clock with <strong>NO daytime pause</strong>.
            </p>
            <div className="bg-red-950/40 p-4 rounded-2xl border border-red-800/80 mb-6 space-y-2 text-xs text-red-300">
              <p className="font-bold flex items-center gap-1.5 text-red-400">
                <AlertTriangle className="w-4 h-4 shrink-0" />
                Continuous Daytime Enforcement
              </p>
              <p className="leading-relaxed text-red-200">
                If your homework expires or fails evaluation, distracting apps will remain restricted through morning, school, and work hours until you reschedule and pass AI evaluation.
              </p>
              <p className="text-[11px] text-red-400 font-semibold">
                You can change this later in Settings (gear icon) on your Dashboard.
              </p>
            </div>
            <div className="flex gap-3">
              <button
                type="button"
                onClick={() => setShowHardcoreModal(false)}
                className="flex-1 py-3 bg-gray-800 hover:bg-gray-700 text-gray-300 font-bold rounded-xl text-xs sm:text-sm transition-colors cursor-pointer"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirmHardcore}
                className="flex-1 py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl text-xs sm:text-sm transition-colors shadow-md flex items-center justify-center gap-2 cursor-pointer"
              >
                <Flame className="w-4 h-4" />
                <span>Enable Hardcore</span>
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

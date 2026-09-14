import React, { useState, useEffect, useCallback, Suspense, useRef } from 'react';
import { AppState, AppSettings, EvaluationResult as IEvaluationResult, ScheduleData, SavedResource, LogEntry, AllowedApp } from './types';
import { Dashboard } from './components/Dashboard';
import { LockScreen } from './components/LockScreen';
import { EvaluationResult } from './components/EvaluationResult';
import { Onboarding } from './components/Onboarding';
import { Loader2, AlertTriangle, X } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { startLockdown, endLockdown, getInstalledApps, checkPermissions, syncSchedules, getLockStatus, syncTimeOffset, requestNotificationPermission, exitToHome, showToast, addBackListener } from './systemBridge';
import { loadData, saveData } from './storage';
import { isAppBlacklisted } from './constants/blacklistedApps';
import { isMessagingPackage, isHiddenSystemExemptApp } from './constants/allowedApps';

// Code-split secondary views to keep initial bundle ultra-lightweight and fast to load
const CreateSchedule = React.lazy(() => import('./components/CreateSchedule').then(m => ({ default: m.CreateSchedule })));
const SettingsOverlay = React.lazy(() => import('./components/SettingsOverlay').then(m => ({ default: m.SettingsOverlay })));
const EditRubric = React.lazy(() => import('./components/EditRubric').then(m => ({ default: m.EditRubric })));
const HomeworksPage = React.lazy(() => import('./components/HomeworksPage').then(m => ({ default: m.HomeworksPage })));
const PermissionWalkthrough = React.lazy(() => import('./components/PermissionWalkthrough').then(m => ({ default: m.PermissionWalkthrough })));

const modalVariants = {
  initial: { opacity: 0 },
  animate: { opacity: 1 },
  exit: { opacity: 0 }
};

const ModalTransition = ({ children, keyStr }: { children: React.ReactNode, keyStr: string }) => (
  <motion.div
    key={keyStr}
    variants={modalVariants}
    initial="initial"
    animate="animate"
    exit="exit"
    transition={{ duration: 0.2 }}
    className="fixed inset-0 z-50 flex items-center justify-center bg-gray-50/80 backdrop-blur-sm"
  >
    {children}
  </motion.div>
);
const pageVariants = {
  initial: (direction: 'forward' | 'backward') => {
    console.log("initial direction:", direction);
    return {
      opacity: 0,
      x: direction === 'forward' ? '100%' : '-100%'
    };
  },
  animate: { opacity: 1, x: 0 },
  exit: (direction: 'forward' | 'backward') => {
    console.log("exit direction:", direction);
    return {
      opacity: 0,
      x: direction === 'forward' ? '-100%' : '100%'
    };
  }
};

const PageTransition = ({ children, keyStr, direction = 'forward' }: { children: React.ReactNode, keyStr: string, direction?: 'forward' | 'backward' }) => (
  <motion.div
    key={keyStr}
    custom={direction}
    variants={pageVariants}
    initial="initial"
    animate="animate"
    exit="exit"
    transition={{ duration: 0.3, ease: 'easeOut' }}
    className="absolute inset-0 overflow-y-auto bg-gray-50 flex flex-col w-full h-full"
  >
    {children}
  </motion.div>
);

export default function App() {
  const [globalError, setGlobalError] = useState<string | null>(null);
  const [isResourceEditing, setIsResourceEditing] = useState(false);
  useEffect(() => {
    if (globalError) {
      const t = setTimeout(() => setGlobalError(null), 10000);
      return () => clearTimeout(t);
    }
  }, [globalError]);

  const [navDirection, setNavDirection] = useState<'forward' | 'backward'>('forward');
  const navigate = useCallback((newState: AppState, direction: 'forward' | 'backward' = 'forward') => {
    setNavDirection(direction);
    setAppState(newState);
  }, []);

  const [isLoaded, setIsLoaded] = useState(false);
  const [appState, setAppState] = useState<AppState>('onboarding');
  
  const [settings, setSettings] = useState<AppSettings>({ 
    onboardingComplete: false,
    role: 'just a guy',
    schedules: []
  });

  const [evaluationResult, setEvaluationResult] = useState<IEvaluationResult | null>(null);
  const [isEvaluating, setIsEvaluating] = useState(false);
  const [timeOffset, setTimeOffset] = useState<number>(0);

  const [tick, setTick] = useState(0);
  useEffect(() => {
    const timer = setInterval(() => setTick(t => t + 1), 1000);
    return () => clearInterval(timer);
  }, []);

  useEffect(() => {
    if (isLoaded) {
      saveData('studom_timeOffset', timeOffset);
      syncTimeOffset(timeOffset);
    }
  }, [timeOffset, isLoaded]);

  const [timeUntilLock, setTimeUntilLock] = useState<number | null>(null);
  const [nextActivationDate, setNextActivationDate] = useState<Date | null>(null);
  const [lockEndTime, setLockEndTime] = useState<number | null>(null);
  const [lockPauseTime, setLockPauseTime] = useState<number | null>(null);
  const [activeScheduleId, setActiveScheduleId] = useState<string | null>(null);
  const [editingRubricScheduleId, setEditingRubricScheduleId] = useState<string | null>(null);

  // Global Permission Checking (on mount and on resume)
  useEffect(() => {
    // Prompt for Android 13+ POST_NOTIFICATIONS runtime permission on startup
    requestNotificationPermission().catch(() => {});

    if (appState === 'onboarding' || appState === 'permission_walkthrough') return;
    
    const verify = async () => {
      const perms = await checkPermissions();
      if (!perms.isDeviceOwner && !perms.isAccessibilityEnabled) {
        navigate('permission_walkthrough');
      }
    };
    
    verify();
  }, [appState, navigate]);

  const [backToastVisible, setBackToastVisible] = useState(false);
  const lastBackPressRef = useRef<number>(0);

  // Hardware/Gesture Back Button Handling (Option 1: Pop sub-screens, double-back on Dashboard)
  useEffect(() => {
    let cleanupListener: (() => void) | null = null;

    addBackListener(() => {
      // 0. Check if any inner modal (e.g. popups, sub-dialogs) handles the back press
      const backEvent = new CustomEvent('qiezka-back-press', { cancelable: true });
      const wasConsumed = !window.dispatchEvent(backEvent);
      if (wasConsumed) return;

      // 1. Pop sub-screens back to dashboard
      if (
        appState === 'settings' || 
        appState === 'logs' || 
        appState === 'create_schedule' || 
        appState === 'edit_rubric'
      ) {
        navigate('dashboard', 'backward');
        return;
      }

      if (appState === 'result') {
        endLockdown();
        setActiveScheduleId(null);
        setLockEndTime(null);
        setLockPauseTime(null);
        navigate('dashboard', 'backward');
        return;
      }

      if (appState === 'permission_walkthrough') {
        navigate('dashboard', 'backward');
        return;
      }

      // 2. In active lock mode: pressing back minimizes app so allowed apps can be opened (overlay ball stays active)
      if (appState === 'locked') {
        exitToHome();
        return;
      }

      // 3. On dashboard / root screens: double press to exit
      const now = Date.now();
      if (now - lastBackPressRef.current < 2000) {
        setBackToastVisible(false);
        exitToHome();
      } else {
        lastBackPressRef.current = now;
        showToast('Press back again to exit');
        setBackToastVisible(true);
        setTimeout(() => {
          setBackToastVisible(false);
        }, 2000);
      }
    }).then(sub => {
      cleanupListener = () => sub.remove();
    }).catch(err => {
      console.warn('Failed to attach backPressed listener', err);
    });

    return () => {
      if (cleanupListener) cleanupListener();
    };
  }, [appState, navigate]);

  const resumeLock = () => {
    if (lockPauseTime && lockEndTime) {
      setLockEndTime(lockEndTime + (Date.now() - lockPauseTime));
      setLockPauseTime(null);
    }
    navigate('locked');
  };

  const [logs, setLogs] = useState<LogEntry[]>([]);

  useEffect(() => {
    if (isLoaded) saveData('studom_logs', logs);
  }, [logs, isLoaded]);

  const [completedHomeworks, setCompletedHomeworks] = useState<import('./types').CompletedHomework[]>([]);

  useEffect(() => {
    if (isLoaded) saveData('studom_completed_homeworks', completedHomeworks);
  }, [completedHomeworks, isLoaded]);


  const addLog = (action: string, details?: string) => {
    setLogs(prev => [{
      id: crypto.randomUUID(),
      timestamp: Date.now(),
      action,
      details
    }, ...prev].slice(0, 500));
  };

  const [resources, setResources] = useState<SavedResource[]>([]);
  const [installedApps, setInstalledApps] = useState<AllowedApp[]>(() => {
    try {
      const cached = localStorage.getItem('studom_installed_apps');
      return cached ? JSON.parse(cached) : [];
    } catch {
      return [];
    }
  });

  useEffect(() => {
    if (isLoaded) saveData('studom_settings', settings);
  }, [settings, isLoaded]);

  useEffect(() => {
    if (isLoaded) saveData('studom_resources', resources);
  }, [resources, isLoaded]);

  // Load all data on mount
  useEffect(() => {
    async function loadAll() {
      // 1. Parallelize all persistent storage reads (finishes in <10ms)
      const [loadedSettings, loadedResources, loadedLogs, loadedCompletedHomeworks, loadedTimeOffset] = await Promise.all([
        loadData<AppSettings>('studom_settings', { onboardingComplete: false, role: 'just a guy', schedules: [] }),
        loadData<SavedResource[]>('studom_resources', []),
        loadData<LogEntry[]>('studom_logs', []),
        loadData<import('./types').CompletedHomework[]>('studom_completed_homeworks', []),
        loadData<number>('studom_timeOffset', 0)
      ]);
      
      // Migrate old schedule
      if ((loadedSettings as any).schedule && !loadedSettings.schedules) {
        loadedSettings.schedules = [{ ...(loadedSettings as any).schedule, id: crypto.randomUUID() }];
        delete (loadedSettings as any).schedule;
      } else if (!loadedSettings.schedules) {
        loadedSettings.schedules = [];
      }

      if (loadedSettings.allowedApps) {
        loadedSettings.allowedApps = loadedSettings.allowedApps.filter(a => !isAppBlacklisted(a.id) && !isHiddenSystemExemptApp(a.id, a.name));
      }

      // Check cached installed apps for initial messaging app auto-population if needed
      if (!loadedSettings.allowedAppsInitialized && installedApps.length > 0) {
        const messagingApps = installedApps.filter(a => isMessagingPackage(a.id) && !isAppBlacklisted(a.id) && !isHiddenSystemExemptApp(a.id, a.name));
        if (messagingApps.length > 0) {
          loadedSettings.allowedApps = messagingApps;
        }
        loadedSettings.allowedAppsInitialized = true;
      }

      setSettings(loadedSettings);
      setResources(loadedResources);
      setLogs(loadedLogs);
      setCompletedHomeworks(loadedCompletedHomeworks);
      setTimeOffset(loadedTimeOffset);

      // Check native lock status immediately upon loading
      try {
        const lockStatus = await getLockStatus();
        if (lockStatus && lockStatus.isLockActive) {
          if (lockStatus.lockEndTime > 0) setLockEndTime(lockStatus.lockEndTime);
          if (lockStatus.activeScheduleId) setActiveScheduleId(lockStatus.activeScheduleId);
          setAppState('locked');
        } else {
          setAppState(loadedSettings.onboardingComplete ? 'dashboard' : 'onboarding');
        }
      } catch {
        setAppState(loadedSettings.onboardingComplete ? 'dashboard' : 'onboarding');
      }

      // Immediately mark as loaded to instantly render the app screen
      setIsLoaded(true);

      // 2. Query fresh installed apps asynchronously in background without holding the loading screen
      getInstalledApps().then(installed => {
        if (installed && installed.length > 0) {
          setInstalledApps(installed);
          try { localStorage.setItem('studom_installed_apps', JSON.stringify(installed)); } catch {}

          setSettings(prev => {
            if (!prev.allowedAppsInitialized) {
              const messagingApps = installed.filter(a => isMessagingPackage(a.id) && !isAppBlacklisted(a.id) && !isHiddenSystemExemptApp(a.id, a.name));
              return {
                ...prev,
                allowedApps: messagingApps.length > 0 ? messagingApps : prev.allowedApps,
                allowedAppsInitialized: true
              };
            }
            return prev;
          });
        }
      }).catch(e => {
        console.warn("Background getInstalledApps failed", e);
      });
    }
    loadAll();
  }, []);

  const getCurrentTime = useCallback(() => Date.now() + timeOffset, [timeOffset]);

  // Keep UI in sync with native lock status when app is brought to foreground or resumes
  useEffect(() => {
    const handleVisibilityChange = async () => {
      if (document.visibilityState === 'visible') {
        try {
          const status = await getLockStatus();
          if (status && status.isLockActive) {
            if (status.lockEndTime > 0) {
              setLockEndTime(prev => (prev !== status.lockEndTime ? status.lockEndTime : prev));
            }
            if (status.activeScheduleId) {
              setActiveScheduleId(prev => (prev !== status.activeScheduleId ? status.activeScheduleId : prev));
            }
            setAppState(prev => (prev !== 'evaluating' && prev !== 'result' ? 'locked' : prev));
          } else {
            setAppState(prev => (prev === 'locked' ? 'dashboard' : prev));
          }
        } catch (e) {
          console.warn('Failed to refresh lock status on visibility change', e);
        }
      }
    };

    document.addEventListener('visibilitychange', handleVisibilityChange);
    window.addEventListener('focus', handleVisibilityChange);
    return () => {
      document.removeEventListener('visibilitychange', handleVisibilityChange);
      window.removeEventListener('focus', handleVisibilityChange);
    };
  }, []);

  // Synchronize schedules & allowed apps with native AlarmManager and SharedPreferences
  useEffect(() => {
    if (!isLoaded) return;
    const safeAllowedApps = (settings.allowedApps || []).filter(a => !isAppBlacklisted(a.id) && !isHiddenSystemExemptApp(a.id, a.name));
    syncSchedules(settings.schedules || [], safeAllowedApps.map(a => a.id));
  }, [settings.schedules, settings.allowedApps, isLoaded]);

  useEffect(() => {
    if (!isLoaded) return;

    const activeSchedules = settings.schedules?.filter(s => s.isActive) || [];
    
    if (activeSchedules.length === 0) {
      if (appState === 'locked') {
        getLockStatus().then(st => {
          if (!st || !st.isLockActive) {
            endLockdown();
            navigate('dashboard');
          }
        }).catch(() => {
          endLockdown();
          navigate('dashboard');
        });
      }
      setTimeUntilLock(null);
      setNextActivationDate(null);
      return;
    }

    if (appState === 'create_schedule' || appState === 'edit_rubric') {
      return; // Pause lock checking while editing schedules
    }

    const checkSchedule = () => {
      const now = new Date(getCurrentTime());
      const currentHours = now.getHours();

      let nearestUpcomingTime: number | null = null;
      let nearestScheduleDate: Date | null = null;
      let foundActive = false;

      for (const schedule of activeSchedules) {
        const [actHours, actMinutes] = schedule.activationTime.split(':').map(Number);
        
        const scheduledTime = new Date(now);
        scheduledTime.setHours(actHours, actMinutes, 0, 0);
        
        const yesterdayScheduledTime = new Date(scheduledTime);
        yesterdayScheduledTime.setDate(yesterdayScheduledTime.getDate() - 1);

        const durationMs = schedule.durationMinutes * 60000;
        
        let inWindow = false;
        let lockEnd = 0;

        // Check if currently in today's lock window
        if (now.getTime() >= scheduledTime.getTime() && now.getTime() < scheduledTime.getTime() + durationMs) {
          inWindow = true;
          lockEnd = scheduledTime.getTime() + durationMs;
        } 
        // Check if currently in yesterday's lock window (e.g. crossed midnight)
        else if (now.getTime() >= yesterdayScheduledTime.getTime() && now.getTime() < yesterdayScheduledTime.getTime() + durationMs) {
          inWindow = true;
          lockEnd = yesterdayScheduledTime.getTime() + durationMs;
        }

        if (inWindow) {
          setActiveScheduleId(schedule.id);
          if (appState !== 'locked' && appState !== 'evaluating' && appState !== 'result') {
            setLockEndTime(lockEnd);
            const safeAllowedApps = (settings.allowedApps || []).filter(a => !isAppBlacklisted(a.id) && !isHiddenSystemExemptApp(a.id, a.name));
            startLockdown(safeAllowedApps.map(a => a.id), schedule.durationMinutes, lockEnd, schedule.id);
            navigate('locked');
          } else if (appState === 'locked' && lockEndTime !== lockEnd) {
            setLockEndTime(lockEnd);
            const safeAllowedApps = (settings.allowedApps || []).filter(a => !isAppBlacklisted(a.id) && !isHiddenSystemExemptApp(a.id, a.name));
            startLockdown(safeAllowedApps.map(a => a.id), schedule.durationMinutes, lockEnd, schedule.id);
          }
          foundActive = true;
          return; // Exit out of checkSchedule completely
        }

        // Calculate next future activation date for this schedule
        if (now.getTime() >= scheduledTime.getTime()) {
          scheduledTime.setDate(scheduledTime.getDate() + 1);
        }

        const timeUntilMs = scheduledTime.getTime() - now.getTime();
        if (nearestUpcomingTime === null || timeUntilMs < nearestUpcomingTime) {
          nearestUpcomingTime = timeUntilMs;
          nearestScheduleDate = scheduledTime;
        }
      }

      if (!foundActive) {
        if (appState === 'locked') {
          endLockdown();
          navigate('dashboard');
        }

        if (nearestUpcomingTime !== null && nearestScheduleDate !== null) {
          setNextActivationDate(nearestScheduleDate);
          const diffSeconds = Math.max(0, Math.floor(nearestUpcomingTime / 1000));
          setTimeUntilLock(diffSeconds);
        } else {
          setTimeUntilLock(null);
          setNextActivationDate(null);
        }
      }
    };

    checkSchedule();
    const interval = setInterval(checkSchedule, 1000);
    return () => clearInterval(interval);
  }, [settings.schedules, appState, timeOffset, isLoaded, lockEndTime]);

  const notifyUser = (message: string) => {
    if (typeof window === 'undefined' || !('Notification' in window)) return;
    if (Notification.permission === 'granted') {
      new Notification('QIEZKA ALARM', { body: message });
    } else if (Notification.permission !== 'denied') {
      Notification.requestPermission().then(permission => {
        if (permission === 'granted') {
          new Notification('QIEZKA ALARM', { body: message });
        }
      });
    }
  };

  const handleTimeOverride = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!e.target.value) return;
    const [hours, minutes, seconds] = e.target.value.split(':').map(Number);
    const now = new Date();
    const simDate = new Date(now.getFullYear(), now.getMonth(), now.getDate(), hours, minutes, seconds || 0);
    const newOffset = simDate.getTime() - Date.now();
    setTimeOffset(newOffset);
  };

  const handleTimeout = useCallback((skipped?: boolean) => {
    endLockdown(); // Always release native lock if timer runs out or is skipped
    notifyUser(skipped ? 'Lock skipped (Test mode)' : 'Lock duration expired. Device access restored.');
    
    // Add failed session to completedHomeworks log
    const activeSchedule = (settings.schedules || []).find(s => s.id === activeScheduleId);
    if (activeSchedule) {
      setCompletedHomeworks(prev => [{
        id: crypto.randomUUID(),
        title: activeSchedule.title || 'Untitled Session',
        homeworkContent: activeSchedule.homeworkContent || '',
        rubricContent: activeSchedule.rubricContent || '',
        transcribedText: '(No submission — timer expired)',
        feedback: skipped ? 'Lock session was skipped in test mode.' : 'Session failed: Timer expired before homework was submitted and passed.',
        passed: false,
        timestamp: Date.now(),
        durationMinutes: activeSchedule.durationMinutes,
        selectedResourceIds: activeSchedule.selectedResourceIds,
      }, ...prev]);
    }

    if (activeScheduleId) {
      setSettings(prev => ({
        ...prev,
        schedules: skipped
          ? (prev.schedules || []).filter(s => s.id !== activeScheduleId)
          : (prev.schedules || []).map(s => s.id === activeScheduleId ? { ...s, isActive: false } : s)
      }));
    }
    navigate('dashboard', 'backward');
  }, [activeScheduleId, settings.schedules]);

  const handleCompleteOnboarding = async (role: 'student' | 'teacher' | 'just a guy') => {
    let initialAllowed = (settings.allowedApps || []).filter(a => !isAppBlacklisted(a.id) && !isHiddenSystemExemptApp(a.id, a.name));
    if (!settings.allowedAppsInitialized) {
      try {
        const installed = installedApps.length > 0 ? installedApps : await getInstalledApps();
        const messagingApps = (installed || []).filter(a => isMessagingPackage(a.id) && !isAppBlacklisted(a.id) && !isHiddenSystemExemptApp(a.id, a.name));
        if (messagingApps.length > 0) {
          initialAllowed = messagingApps;
        }
      } catch (e) {}
    }
    setSettings(prev => ({ ...prev, onboardingComplete: true, role, allowedApps: initialAllowed, allowedAppsInitialized: true }));
    navigate('dashboard', 'forward');
  };

  const handleSaveSchedule = (schedule: ScheduleData) => {
    const safeSchedule: ScheduleData = {
      ...schedule,
      durationMinutes: Math.min(90, Math.max(1, schedule.durationMinutes || 25))
    };
    setSettings(prev => {
      const schedules = prev.schedules || [];
      const isExisting = schedules.some(s => s.id === safeSchedule.id);
      return {
        ...prev,
        schedules: isExisting 
          ? schedules.map(s => s.id === safeSchedule.id ? safeSchedule : s)
          : [...schedules, safeSchedule]
      };
    });
    navigate('dashboard', 'backward');
  };

  const handleAddResource = (title: string, content: string, type: 'lecture_notes' | 'case_study' = 'lecture_notes'): SavedResource => {
    const newRes: SavedResource = { id: crypto.randomUUID(), title, content, createdAt: Date.now(), type };
    setResources(prev => [...prev, newRes]);
    return newRes;
  };

  const handleUpdateResource = (id: string, title: string, content: string, type?: 'lecture_notes' | 'case_study') => {
    setResources(resources.map(r => r.id === id ? { ...r, title, content, ...(type ? { type } : {}) } : r));
  };

  const handleRemoveResource = (id: string) => {
    setResources(resources.filter(r => r.id !== id));
  };

  const handleCombineResources = (id1: string, id2: string) => {
    setResources(prev => {
      const r1 = prev.find(r => r.id === id1);
      const r2 = prev.find(r => r.id === id2);
      if (!r1 || !r2) return prev;
      
      const combinedContent = `${r1.content}\n\n---\n\n${r2.content}`;
      const combinedTitle = `${r1.title} & ${r2.title}`;
      
      const newResource: SavedResource = {
        id: crypto.randomUUID(),
        title: combinedTitle.length > 50 ? combinedTitle.substring(0, 47) + '...' : combinedTitle,
        content: combinedContent,
        createdAt: Date.now()
      };
      
      return [...prev.filter(r => r.id !== id1 && r.id !== id2), newResource];
    });
  };

  const handleSubmitHomework = async (file: File, scheduleId: string, ocrType: 'simple' | 'formatted', transcribedText?: string) => {
    if (file.size > 5 * 1024 * 1024) {
      alert("This image is too large (over 5MB). Please compress it or take a lower resolution photo before uploading.");
      return;
    }
    if (!transcribedText) {
      setGlobalError('Evaluation Error: No transcribed text found. Please run OCR first.');
      return;
    }
    setLockPauseTime(Date.now());
    navigate('evaluating');
    setIsEvaluating(true);

    const activeSchedule = settings.schedules?.find(s => s.id === scheduleId);

    try {
      const { evaluate } = await import('./api/evaluate');
      const data = await evaluate({
        transcribedText,
        rubric: activeSchedule?.rubricContent || '',
        apiKey: settings.apiKey || '',
        apiModel: settings.apiModel || 'gemini-2.0-flash',
        customPrompts: settings.prompts,
      });

      setEvaluationResult(data);
      setCompletedHomeworks(prev => [{
        id: crypto.randomUUID(),
        title: activeSchedule?.title || 'Untitled Schedule',
        homeworkContent: activeSchedule?.homeworkContent || '',
        rubricContent: activeSchedule?.rubricContent || '',
        transcribedText: data.transcribedText || '',
        feedback: data.feedback || '',
        passed: data.passed || false,
        timestamp: Date.now(),
        durationMinutes: activeSchedule?.durationMinutes,
        selectedResourceIds: activeSchedule?.selectedResourceIds,
      }, ...prev]);

      if (data.passed) {
        // Auto-Harvesting Logic
        if (activeSchedule?.selectedResourceIds?.includes('ai-general-knowledge')) {
          const textToHarvest = activeSchedule.aiAnswer || transcribedText || data.transcribedText;
          if (textToHarvest) {
            const realResources = (activeSchedule.selectedResourceIds || []).filter(id => id !== 'ai-general-knowledge');
            import('./api/declutterResource').then(({ declutterResource }) => {
              if (realResources.length > 0) {
                const targetRes = resources.find(r => r.id === realResources[0]);
                const combinedContent = (targetRes ? targetRes.content + '\n\n' : '') + textToHarvest;
                declutterResource({
                  title: targetRes?.title || 'AI Research: ' + (activeSchedule.title || 'Topic'),
                  content: combinedContent,
                  apiKey: settings.apiKey || '',
                  apiModel: settings.apiModel || 'gemini-2.0-flash',
                  customPrompts: settings.prompts,
                }).then(harvestData => {
                  setResources(prev => prev.map(r => r.id === realResources[0] ? { ...r, content: harvestData.content } : r));
                }).catch(console.error);
              } else {
                declutterResource({
                  title: 'AI Research: ' + (activeSchedule.title || 'Topic'),
                  content: textToHarvest,
                  apiKey: settings.apiKey || '',
                  apiModel: settings.apiModel || 'gemini-2.0-flash',
                  customPrompts: settings.prompts,
                }).then(harvestData => {
                  setResources(prev => [...prev, { id: crypto.randomUUID(), title: harvestData.title || 'AI Research', content: harvestData.content, createdAt: Date.now() }]);
                }).catch(console.error);
              }
            });
          }
        }

        // Deactivate the completed schedule immediately so neither React nor native background re-locks it
        const updatedSchedules = (settings.schedules || []).map(s => 
          s.id === scheduleId ? { ...s, isActive: false } : s
        );
        setSettings(prev => ({
          ...prev,
          schedules: updatedSchedules
        }));

        const safeAllowedApps = (settings.allowedApps || []).filter(a => !isAppBlacklisted(a.id) && !isHiddenSystemExemptApp(a.id, a.name));
        syncSchedules(updatedSchedules, safeAllowedApps.map(a => a.id));

        // Clean up lockscreen local storage
        localStorage.removeItem(`lockscreen_data_${scheduleId}`);
        localStorage.removeItem('lockscreen_last_draft');

        endLockdown(); // Release the OS lock and dismiss FloatingOverlayService ball!
        setActiveScheduleId(null);
        setLockEndTime(null);
        setLockPauseTime(null);
        navigate('result');
      } else {
        navigate('result');
      }
    } catch (error: any) {
      setGlobalError(`Evaluation Error: ${error.message || 'Unknown error'}`);
      resumeLock();
    } finally {
      setIsEvaluating(false);
    }
  };

  const displayTime = new Date(getCurrentTime());
  
  const formatForInput = (d: Date) => {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  };

  const zoomStyle = settings.uiScale && settings.uiScale !== 100 ? { zoom: `${settings.uiScale}%` } as any : {};

  if (!isLoaded) {
    return (
      <div className="min-h-screen bg-gray-950 flex flex-col items-center justify-center text-red-500 font-black">
        <Loader2 className="w-12 h-12 animate-spin mb-4" />
        <span className="uppercase tracking-widest text-xs font-bold text-gray-500">Decrypting QIEZKA...</span>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 flex flex-col font-sans selection:bg-red-200 overflow-hidden" style={zoomStyle}>
      {globalError && (
        <div className="fixed top-4 left-1/2 -translate-x-1/2 z-[100] w-full max-w-lg px-4">
          <div className="bg-red-600 text-white p-4 rounded-2xl shadow-2xl flex items-start space-x-3">
            <AlertTriangle className="w-6 h-6 flex-shrink-0 mt-0.5" />
            <div className="flex-1 whitespace-pre-wrap text-sm font-medium">
              {globalError}
            </div>
            <button onClick={() => setGlobalError(null)} className="p-1 hover:bg-red-700 rounded-full transition-colors">
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>
      )}

      {backToastVisible && (
        <div className="fixed bottom-10 left-1/2 -translate-x-1/2 z-[200] bg-gray-950/95 backdrop-blur-md text-white border border-gray-700/80 px-5 py-2.5 rounded-full shadow-2xl flex items-center gap-2 text-xs font-bold tracking-wide pointer-events-none animate-in fade-in slide-in-from-bottom duration-200">
          <span>Press back again to exit</span>
        </div>
      )}
      
      <AnimatePresence mode="wait">
        {appState === 'onboarding' && (
          <motion.div key="onboarding" custom={navDirection} variants={pageVariants} initial="initial" animate="animate" exit="exit" transition={{ duration: 0.3, ease: 'easeOut' }} className="absolute inset-0 overflow-y-auto bg-gray-50 flex flex-col w-full h-full">
            <Onboarding onComplete={handleCompleteOnboarding} />
          </motion.div>
        )}
      </AnimatePresence>

      

      <div className="relative flex-1 w-full h-full overflow-hidden">
        <AnimatePresence custom={navDirection}>
          {['dashboard', 'settings', 'logs'].includes(appState) && (
          <motion.div key="dashboard" custom={navDirection} variants={pageVariants} initial="initial" animate="animate" exit="exit" transition={{ duration: 0.3, ease: 'easeOut' }} className="absolute inset-0 overflow-y-auto bg-gray-50 flex flex-col w-full h-full">
            <Dashboard showError={setGlobalError}
              displayTime={displayTime}
              timeOffset={timeOffset}
              onTimeOverride={handleTimeOverride}
              onResetTime={() => setTimeOffset(0)}
              onSettingsChange={(updates) => setSettings(prev => ({ ...prev, ...updates }))}
              onResourceEditStateChange={setIsResourceEditing} 
              settings={settings}
              timeUntilLock={timeUntilLock}
              nextActivationDate={nextActivationDate}
              onCreateSchedule={() => navigate('create_schedule')}
              onClearSchedule={(id) => {
                if (id) {
                  setSettings(prev => ({ ...prev, schedules: prev.schedules.filter(s => s.id !== id) }));
                  addLog('Deleted Schedule');
                } else {
                  setSettings(prev => ({ ...prev, schedules: [] }));
                  addLog('Cleared All Schedules');
                }
              }}
              onOpenSettings={() => navigate('settings')}
              onOpenLogs={() => navigate('logs')}
              resources={resources}
              onAddResource={handleAddResource}
              onUpdateResource={handleUpdateResource}
              onRemoveResource={handleRemoveResource}
              onCombineResources={handleCombineResources}
              onViewRubric={(schedule) => {
                setEditingRubricScheduleId(schedule.id);
                navigate('edit_rubric');
              }}
              installedApps={installedApps}
            />
          </motion.div>
        )}

        {appState === 'create_schedule' && (
          <motion.div key="create_schedule" custom={navDirection} variants={pageVariants} initial="initial" animate="animate" exit="exit" transition={{ duration: 0.3, ease: 'easeOut' }} className="absolute inset-0 overflow-y-auto bg-gray-50 flex flex-col w-full h-full">
            <Suspense fallback={<div className="flex-1 flex items-center justify-center p-12 text-red-500"><Loader2 className="w-8 h-8 animate-spin" /></div>}>
              <CreateSchedule showError={setGlobalError} 
                role={settings.role}
                resources={resources}
                apiKey={settings.apiKey}
                apiModel={settings.apiModel}
                existingSchedules={settings.schedules || []}
                settings={settings}
                addLog={addLog}
                onSave={handleSaveSchedule}
                onAddResource={handleAddResource}
                onCancel={() => navigate('dashboard', 'backward')}
              />
            </Suspense>
          </motion.div>
        )}

        {appState === 'edit_rubric' && editingRubricScheduleId && (
          <motion.div key="edit_rubric" custom={navDirection} variants={pageVariants} initial="initial" animate="animate" exit="exit" transition={{ duration: 0.3, ease: 'easeOut' }} className="absolute inset-0 overflow-y-auto bg-gray-50 flex flex-col w-full h-full">
            <Suspense fallback={<div className="flex-1 flex items-center justify-center p-12 text-red-500"><Loader2 className="w-8 h-8 animate-spin" /></div>}>
              <EditRubric
                schedule={settings.schedules.find(s => s.id === editingRubricScheduleId)!}
                resources={resources}
                apiKey={settings.apiKey}
                apiModel={settings.apiModel}
                role={settings.role}
                onSave={(updatedSchedule) => {
                  setSettings(prev => ({
                    ...prev,
                    schedules: prev.schedules.map(s => s.id === updatedSchedule.id ? updatedSchedule : s)
                  }));
                  navigate('dashboard', 'backward');
                }}
                onCancel={() => navigate('dashboard', 'backward')}
                showError={setGlobalError}
              />
            </Suspense>
          </motion.div>
        )}

        {appState === 'locked' && lockEndTime && (
          <motion.div key="locked" custom={navDirection} variants={pageVariants} initial="initial" animate="animate" exit="exit" transition={{ duration: 0.3, ease: 'easeOut' }} className="absolute inset-0 overflow-y-auto bg-gray-50 flex flex-col w-full h-full">
            <LockScreen 
              schedule={
                (settings.schedules || []).find(s => s.id === activeScheduleId) ||
                (settings.schedules && settings.schedules.length > 0 ? settings.schedules[0] : null) || {
                  id: activeScheduleId || 'active-session',
                  title: 'Study Session',
                  homeworkContent: 'Stay focused on your task until the timer expires.',
                  rubricMode: 'points',
                  rubricContent: 'Do not exit until timer completes.',
                  selectedResourceIds: [],
                  activationTime: '00:00',
                  durationMinutes: 25,
                  isActive: true,
                }
              }
              settings={settings}
              resources={resources}
              lockEndTime={lockEndTime}
              onSubmitHomework={(file, ocrType, text) => handleSubmitHomework(file, activeScheduleId || 'active-session', ocrType, text)}
              onTimeout={handleTimeout}
              getCurrentTime={getCurrentTime}
              onTimeOverride={handleTimeOverride}
              timeOffset={timeOffset}
              onResetTime={() => setTimeOffset(0)}
              onSettingsChange={(updates) => setSettings(prev => ({ ...prev, ...updates }))}
              installedApps={installedApps}
            />
          </motion.div>
        )}

        {appState === 'result' && evaluationResult && (
          <motion.div key="result" custom={navDirection} variants={pageVariants} initial="initial" animate="animate" exit="exit" transition={{ duration: 0.3, ease: 'easeOut' }} className="absolute inset-0 overflow-y-auto bg-gray-50 flex flex-col w-full h-full" style={{ WebkitOverflowScrolling: 'touch' }}>
            <EvaluationResult 
              result={evaluationResult}
              onReset={() => {
                endLockdown();
                setActiveScheduleId(null);
                setLockEndTime(null);
                setLockPauseTime(null);
                navigate('dashboard', 'backward');
              }}
              onRetry={resumeLock}
            />
          </motion.div>
        )}

        

        {appState === 'permission_walkthrough' && (
          <motion.div key="permission_walkthrough" custom={navDirection} variants={pageVariants} initial="initial" animate="animate" exit="exit" transition={{ duration: 0.3, ease: 'easeOut' }} className="absolute inset-0 overflow-y-auto flex flex-col w-full h-full z-50">
            <Suspense fallback={<div className="flex-1 flex items-center justify-center p-12 text-red-500"><Loader2 className="w-8 h-8 animate-spin" /></div>}>
              <PermissionWalkthrough onComplete={() => navigate('dashboard')} />
            </Suspense>
          </motion.div>
        )}

        </AnimatePresence>
      </div>

      <AnimatePresence>
        {appState === 'logs' && (
          <ModalTransition keyStr="logs">
            <Suspense fallback={<div className="p-8 flex items-center justify-center text-red-500"><Loader2 className="w-8 h-8 animate-spin" /></div>}>
              <HomeworksPage 
                homeworks={completedHomeworks}
                schedules={settings.schedules || []}
                onBack={() => navigate('dashboard', 'backward')}
                onClear={() => setCompletedHomeworks([])}
                onReschedule={(updatedSchedules) => {
                  setSettings(prev => {
                    const inactive = (prev.schedules || []).filter(s => !s.isActive);
                    return {
                      ...prev,
                      schedules: [...updatedSchedules, ...inactive]
                    };
                  });
                  addLog('Emergency Reschedule', `Cascaded ${updatedSchedules.length} active schedules`);
                }}
              />
            </Suspense>
          </ModalTransition>
        )}
        {appState === 'settings' && (
          <ModalTransition keyStr="settings">
            <Suspense fallback={<div className="p-8 flex items-center justify-center text-red-500"><Loader2 className="w-8 h-8 animate-spin" /></div>}>
              <SettingsOverlay
                settings={settings}
                logs={logs}
                onClearLogs={() => setLogs([])}
                onSave={(updates) => {
                  setSettings(prev => ({ ...prev, ...updates }));
                  navigate('dashboard', 'backward');
                }}
                onClose={() => navigate('dashboard', 'backward')}
              />
            </Suspense>
          </ModalTransition>
        )}
      </AnimatePresence>

      <AnimatePresence>{appState === 'evaluating' && (
        <ModalTransition keyStr="evaluating">
          <div className="flex flex-col items-center justify-center text-white">
            <Loader2 className="w-16 h-16 text-red-500 animate-spin mb-6" />
            <h2 className="text-2xl font-black tracking-widest text-gray-900 mb-2 uppercase">AI is grading your work</h2>
            <p className="text-gray-500 text-center font-mono">Comparing handwritten submission against rubric...</p>
          </div>
        </ModalTransition>
      )}
      </AnimatePresence>
    </div>
  );
}

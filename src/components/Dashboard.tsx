import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Clock, BookOpen, Trash2, Plus, Sparkles, Pencil, Upload, Loader2, Settings, ShieldAlert, X, GitMerge, FileText, Calculator, Music, Globe, MessageSquare, MonitorPlay, Check, LayoutGrid, Camera, ShieldCheck, AlertTriangle, Home } from 'lucide-react';
import { AppSettings, SavedResource, ScheduleData, AllowedApp } from '../types';
import { getInstalledApps } from '../systemBridge';
import { isAppBlacklisted } from '../constants/blacklistedApps';

interface DashboardProps {
  settings: AppSettings;
  timeUntilLock: number | null;
  nextActivationDate: Date | null;
  onCreateSchedule: () => void;
  onClearSchedule: (id?: string) => void;
  onOpenSettings: () => void;
  showError: (msg: string) => void;
  resources: SavedResource[];
  onAddResource: (title: string, content: string, type?: 'lecture_notes' | 'case_study') => void;
  onUpdateResource: (id: string, title: string, content: string, type?: 'lecture_notes' | 'case_study') => void;
  onRemoveResource: (id: string) => void;
  onCombineResources: (id1: string, id2: string) => void;
  onViewRubric: (schedule: ScheduleData) => void;
  onOpenLogs: () => void;
  onResourceEditStateChange?: (isEditing: boolean) => void;
  displayTime: Date;
  timeOffset: number;
  onTimeOverride: (e: React.ChangeEvent<HTMLInputElement>) => void;
  onResetTime: () => void;
  onSettingsChange?: (updates: Partial<AppSettings>) => void;
  installedApps?: AllowedApp[];
  completedHomeworks?: import('../types').CompletedHomework[];
  onClearConsequence?: () => void;
  onResetLockdown?: () => void;
}

const pageVariants = {
  initial: (direction: 'forward' | 'backward') => ({
    opacity: 0,
    x: direction === 'forward' ? '100%' : '-100%'
  }),
  animate: { opacity: 1, x: 0 },
  exit: (direction: 'forward' | 'backward') => ({
    opacity: 0,
    x: direction === 'forward' ? '-100%' : '100%'
  })
};

const SIMULATED_APPS: AllowedApp[] = [
  { id: 'com.instagram.android', name: 'Instagram', iconName: 'Camera' },
  { id: 'com.zhiliaoapp.musically', name: 'TikTok', iconName: 'Music' },
  { id: 'com.twitter.android', name: 'X / Twitter', iconName: 'MessageSquare' },
  { id: 'com.google.android.youtube', name: 'YouTube', iconName: 'MonitorPlay' },
  { id: 'com.reddit.frontpage', name: 'Reddit', iconName: 'MessageSquare' },
  { id: 'com.facebook.katana', name: 'Facebook', iconName: 'Globe' },
  { id: 'com.discord', name: 'Discord', iconName: 'MessageSquare' },
  { id: 'com.dts.freefireth', name: 'Free Fire', iconName: 'LayoutGrid' },
  { id: 'com.mobile.legends', name: 'Mobile Legends', iconName: 'LayoutGrid' },
  { id: 'com.roblox.client', name: 'Roblox', iconName: 'LayoutGrid' },
  { id: 'com.netflix.mediaclient', name: 'Netflix', iconName: 'MonitorPlay' },
  { id: 'tv.twitch.android.app', name: 'Twitch', iconName: 'MonitorPlay' }
];

export function Dashboard({ 
  settings, 
  timeUntilLock,
  nextActivationDate,
  onCreateSchedule,
  onClearSchedule,
  onOpenSettings,
  showError,
  resources,
  onAddResource,
  onUpdateResource,
  onRemoveResource,
  onCombineResources,
  onViewRubric,
  onOpenLogs,
  onResourceEditStateChange,
  displayTime,
  timeOffset,
  onTimeOverride,
  onResetTime,
  onSettingsChange,
  installedApps = [],
  completedHomeworks = [],
  onClearConsequence,
  onResetLockdown
}: DashboardProps) {
  const [isAppSelectorOpen, setIsAppSelectorOpen] = useState(false);
  const [availableApps, setAvailableApps] = useState<AllowedApp[]>([]);
  const [isLoadingApps, setIsLoadingApps] = useState(false);
  
  useEffect(() => {
    if (isAppSelectorOpen) {
      setIsLoadingApps(true);
      getInstalledApps().then(nativeApps => {
        if (nativeApps && nativeApps.length > 0) {
          setAvailableApps(nativeApps.filter(app => !isAppBlacklisted(app.id)));
        } else {
          // Dev/web fallback only — on real Android this should always return apps
          setAvailableApps(SIMULATED_APPS.filter(app => !isAppBlacklisted(app.id)));
        }
        setIsLoadingApps(false);
      }).catch(() => {
        setAvailableApps(SIMULATED_APPS.filter(app => !isAppBlacklisted(app.id)));
        setIsLoadingApps(false);
      });
    }
  }, [isAppSelectorOpen]);
  
  const [navDirection, setNavDirection] = useState<'forward' | 'backward'>('forward');
  const [editingResource, setEditingResource] = useState<SavedResource | 'new' | null>(null);
  const [mergingResource, setMergingResource] = useState<SavedResource | null>(null);
  const [newResTitle, setNewResTitle] = useState('');
  const [newResContent, setNewResContent] = useState('');
  const [isParsing, setIsParsing] = useState(false);
  const [ocrType, setOcrType] = useState<'simple' | 'formatted'>(settings.defaultOcrType || 'simple');
  const [isDecluttering, setIsDecluttering] = useState(false);
  const [viewingHomeworkSchedule, setViewingHomeworkSchedule] = useState<ScheduleData | null>(null);

  useEffect(() => {
    const handleBack = (e: Event) => {
      if (isAppSelectorOpen) {
        e.preventDefault();
        setIsAppSelectorOpen(false);
        return;
      }
      if (viewingHomeworkSchedule) {
        e.preventDefault();
        setViewingHomeworkSchedule(null);
        return;
      }
      if (editingResource) {
        e.preventDefault();
        setEditingResource(null);
        return;
      }
      if (mergingResource) {
        e.preventDefault();
        setMergingResource(null);
        return;
      }
    };
    window.addEventListener('qiezka-back-press', handleBack);
    return () => window.removeEventListener('qiezka-back-press', handleBack);
  }, [isAppSelectorOpen, viewingHomeworkSchedule, editingResource, mergingResource]);
  
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  const isCurrentResourceLocked = editingResource && editingResource !== 'new' && settings.schedules.some(s => s.isActive && (s.selectedResourceIds || []).includes(editingResource.id));

  const formatForInput = (d: Date) => {
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
  };

  useEffect(() => {
    if (onResourceEditStateChange) {
      onResourceEditStateChange(editingResource !== null);
    }
  }, [editingResource, onResourceEditStateChange]);


  const [resourceType, setResourceType] = useState<'lecture_notes' | 'case_study'>('lecture_notes');

  const openNewResource = (type: 'lecture_notes' | 'case_study' = 'lecture_notes') => {
    setNavDirection('forward');
    setResourceType(type);
    setEditingResource('new');
    setNewResTitle('');
    setNewResContent('');
  };

  const openEditResource = (res: SavedResource) => {
    setNavDirection('forward');
    setResourceType(res.type || 'lecture_notes');
    setEditingResource(res);
    setNewResTitle(res.title);
    setNewResContent(res.content);
  };

  
  const handleSaveAndCleanup = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newResTitle.trim() && !newResContent.trim()) return;

    setIsDecluttering(true);
    let finalTitle = newResTitle;
    let finalContent = newResContent;

    try {
      if (settings.apiKey) {
        const { declutterResource } = await import('../api/declutterResource');
        const data = await declutterResource({
          title: newResTitle,
          content: newResContent,
          apiKey: settings.apiKey,
          apiModel: settings.apiModel || 'gemini-2.0-flash',
          resourceType,
          customPrompts: settings.prompts,
        });
        if (!newResTitle.trim() && data.title) finalTitle = data.title;
        if (data.content) finalContent = data.content;
      }
    } catch (err: any) {
      console.error('Error in declutter:', err);
      let errMsg = err.message || 'Failed to clean up.';
      if (errMsg.includes('429') || errMsg.includes('RESOURCE_EXHAUSTED') || errMsg.includes('quota')) {
        showError('AI Rate Limit Exceeded. Your resource was NOT saved. Please wait a minute or update your API Key.');
      } else {
        showError(`AI Error: ${errMsg} Your resource was NOT saved.`);
      }
      setIsDecluttering(false);
      return;
    } finally {
      setIsDecluttering(false);
    }

    if (editingResource === 'new') {
      onAddResource(finalTitle || 'Untitled Resource', finalContent, resourceType);
    } else if (editingResource) {
      onUpdateResource(editingResource.id, finalTitle || 'Untitled Resource', finalContent, resourceType);
    }
    { setNavDirection('backward'); setEditingResource(null); };
  };


  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 25 * 1024 * 1024) {
      showError('File is too large. Please upload a file smaller than 25MB.');
      if (fileInputRef.current) fileInputRef.current.value = '';
      return;
    }

    setIsParsing(true);
    try {
      const { parseResource } = await import('../api/parseResource');
      const data = await parseResource({
        file,
        type: 'transcription',
        apiKey: settings.apiKey || '',
        apiModel: settings.apiModel || 'gemini-2.0-flash',
        ocrType,
        simpleOcrKey: settings.simpleOcrKey || '',
        formattedOcrKey: settings.formattedOcrKey || '',
        customPrompts: settings.prompts,
      });

      if (data.error) throw new Error(data.error);
      setNewResContent(data.content);
      // Leave title empty as requested
    } catch (err: any) {
      showError(`Failed to parse document: ${err.message}`);
    } finally {
      setIsParsing(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const formatTimeLeft = (seconds: number | null) => {
    if (seconds === null) return '--:--:--';
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    return `${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const format12Hour = (time24: string) => {
    const [h, m] = time24.split(':').map(Number);
    const ampm = h >= 12 ? 'PM' : 'AM';
    const h12 = h % 12 || 12;
    return `${h12}:${m.toString().padStart(2, '0')} ${ampm}`;
  };

  const schedules = settings.schedules || [];
  const activeSchedules = schedules.filter(s => s.isActive);

  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false);

  return (
    <div className="relative w-full h-full flex-1 overflow-hidden">
      <AnimatePresence initial={false} custom={navDirection}>
        {editingResource ? (
          <motion.div
            key="edit-resource"
            custom={navDirection}
            variants={pageVariants}
            initial="initial"
            animate="animate"
            exit="exit"
            transition={{ duration: 0.25, ease: 'easeOut' }}
          className="max-w-4xl mx-auto w-full p-6 flex flex-col absolute inset-0 bg-gray-50 overflow-y-auto"
          >
        <div className="flex justify-between items-center mb-8">
          <h2 className="text-3xl font-black text-gray-900">
            {editingResource === 'new' 
              ? (resourceType === 'case_study' ? 'Add Case Study / Article' : 'Add New Resource') 
              : isCurrentResourceLocked 
                ? 'View Resource (Locked)' 
                : (resourceType === 'case_study' ? 'Edit Case Study / Article' : 'Edit Resource')}
          </h2>
          <button onClick={() => { setNavDirection('backward'); setEditingResource(null); }} className="p-2 hover:bg-gray-100 rounded-full transition-colors">
            <X className="w-6 h-6 text-gray-500" />
          </button>
        </div>
        
        <form onSubmit={handleSaveAndCleanup} className="flex-1 flex flex-col gap-6 bg-white p-8 rounded-3xl border border-gray-200 shadow-sm">
          {editingResource === 'new' && (
            <div>
              <input 
                type="file" 
                ref={fileInputRef} 
                onChange={handleFileUpload} 
                accept="text/plain, image/*, .docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                className="hidden" 
              />
              <div className="flex justify-between items-center mb-4">
                <span className="text-sm font-bold text-gray-700">Image OCR Type:</span>
                <select
                  value={ocrType}
                  onChange={(e) => setOcrType(e.target.value as 'simple' | 'formatted')}
                  className="bg-gray-50 text-sm text-gray-700 px-3 py-1.5 rounded-lg border border-gray-300 focus:outline-none focus:border-gray-400 font-medium"
                >
                  <option value="simple">Simple OCR</option>
                  <option value="formatted">Formatted OCR (Tables)</option>
                </select>
              </div>
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={isParsing}
                className="w-full py-8 border-2 border-dashed border-gray-300 rounded-2xl flex flex-col items-center justify-center text-gray-500 hover:border-gray-400 hover:bg-gray-50 transition-colors disabled:opacity-50 disabled:cursor-not-allowed bg-white"
              >
                {isParsing ? (
                  <Loader2 className="w-8 h-8 mb-2 animate-spin text-gray-400" />
                ) : (
                  <Upload className="w-8 h-8 mb-2 text-gray-400" />
                )}
                <span className="font-bold text-gray-700">{isParsing ? 'Parsing document...' : 'Upload File'}</span>
                <span className="text-sm text-gray-400 mt-1">DOCX, PNG, JPG, TXT</span>
              </button>
              <div className="relative mt-8 mb-4 text-center">
                <span className="px-2 bg-white text-xs font-bold text-gray-400 uppercase tracking-wider">OR ENTER MANUALLY</span>
              </div>
            </div>
          )}

          <div>
            <label className="block text-sm font-bold text-gray-700 mb-2 uppercase tracking-wider">Resource Title</label>
            <input
              type="text"
              value={newResTitle}
              onChange={(e) => setNewResTitle(e.target.value)}
              disabled={isCurrentResourceLocked}
              className={`w-full p-4 rounded-xl border border-gray-300 focus:ring-2 focus:ring-gray-900 bg-gray-50 text-lg font-medium ${isCurrentResourceLocked ? 'opacity-70 cursor-not-allowed' : ''}`}
              placeholder={resourceType === 'case_study' ? "E.g., 2024 CrowdStrike Outage / Documentary: The Social Dilemma" : "E.g., Chapter 4: Memory Management"}
            />
          </div>

          <div className="flex-1 flex flex-col min-h-0">
            <label className="block text-sm font-bold text-gray-700 mb-2 uppercase tracking-wider">Raw Text</label>
            <textarea
              value={newResContent}
              onChange={(e) => setNewResContent(e.target.value)}
              disabled={isCurrentResourceLocked}
              className={`flex-1 min-h-[300px] w-full p-4 rounded-xl border border-gray-300 focus:ring-2 focus:ring-gray-900 resize-none font-mono text-sm leading-relaxed ${isCurrentResourceLocked ? 'opacity-70 cursor-not-allowed' : ''}`}
              required
            />
          </div>

          <div className="pt-6 border-t border-gray-100 flex justify-between items-center mt-4">
            <div className="flex items-center gap-4">
              {editingResource !== 'new' && !isCurrentResourceLocked && (
                <>
                  <button
                    type="button"
                    onClick={() => setMergingResource(editingResource)}
                    className="px-6 py-3 text-indigo-600 bg-indigo-50 hover:bg-indigo-100 font-bold rounded-xl transition-colors flex items-center"
                  >
                    <GitMerge className="w-5 h-5 mr-2" />
                    Combine with...
                  </button>
                  <button 
                    type="button"
                    onClick={() => setIsConfirmingDelete(true)}
                    className="px-6 py-3 text-red-500 hover:bg-red-50 font-bold rounded-xl transition-colors flex items-center"
                  >
                    <Trash2 className="w-5 h-5 mr-2" />
                    Delete Resource
                  </button>
                </>
              )}
            </div>
            <div className="flex space-x-4">
              <button 
                type="button" 
                onClick={() => {
                  { setNavDirection('backward'); setEditingResource(null); };
                  setIsConfirmingDelete(false);
                }}
                className="px-6 py-3 text-gray-600 font-bold hover:bg-gray-100 rounded-xl transition-colors"
              >
                {isCurrentResourceLocked ? 'Close' : 'Cancel'}
              </button>
              
              {!isCurrentResourceLocked && (
                <button 
                  type="button"
                  onClick={handleSaveAndCleanup}
                  disabled={isDecluttering || (!newResTitle.trim() && !newResContent.trim())}
                  className="px-8 py-3 bg-gray-900 text-white font-bold rounded-xl hover:bg-black transition-colors flex items-center disabled:opacity-50"
                >
                  {isDecluttering ? (
                    <><Loader2 className="w-5 h-5 mr-2 animate-spin" /> Processing...</>
                  ) : (
                    <><Sparkles className="w-5 h-5 mr-2" /> {resourceType === 'case_study' ? 'Save & Clean Up Article' : 'Save & Clean Up'}</>
                  )}
                </button>
              )}
            </div>
          </div>
        </form>

        {/* Combine Resource Modal */}
        <AnimatePresence>{mergingResource && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-gray-900/50 backdrop-blur-sm z-[60] flex items-center justify-center p-4">
            <div className="bg-white rounded-3xl p-8 max-w-2xl w-full max-h-[80vh] flex flex-col shadow-2xl">
              <div className="flex justify-between items-center mb-6">
                <h3 className="text-xl font-bold text-gray-900 flex items-center">
                  <GitMerge className="w-6 h-6 mr-2 text-indigo-500" />
                  Combine "{mergingResource.title}" with...
                </h3>
                <button onClick={() => setMergingResource(null)} className="p-2 text-gray-400 hover:bg-gray-100 rounded-full transition-colors">
                  <X className="w-5 h-5" />
                </button>
              </div>
              
              <div className="flex-1 overflow-y-auto pr-2">
                <div className="grid grid-cols-1 gap-3">
                  {resources.filter(r => r.id !== mergingResource.id).length === 0 ? (
                    <p className="text-gray-500 text-center py-8">No other resources available to combine with.</p>
                  ) : (
                    resources.filter(r => r.id !== mergingResource.id).map(r => {
                      const isLocked = settings.schedules.some(s => s.isActive && (s.selectedResourceIds || []).includes(r.id));
                      return (
                      <button
                        key={r.id}
                        onClick={() => {
                          if (isLocked) {
                            showError(`Cannot combine with "${r.title}" because it is currently attached to an active schedule.`);
                            return;
                          }
                          onCombineResources(mergingResource.id, r.id);
                          setMergingResource(null);
                          { setNavDirection('backward'); setEditingResource(null); }; // Close the edit window too as they merged
                        }}
                        className={`p-4 rounded-xl flex flex-col text-left transition-colors ${isLocked ? 'bg-gray-100 border border-gray-200 opacity-60 cursor-not-allowed' : 'bg-gray-50 hover:bg-indigo-50 border border-gray-200 hover:border-indigo-200'}`}
                      >
                        <div className="flex justify-between items-center w-full">
                           <span className={`font-bold ${isLocked ? 'text-gray-500' : 'text-gray-900'}`}>{r.title}</span>
                           {isLocked && <span className="text-[10px] font-bold text-red-500 uppercase">Locked</span>}
                        </div>
                        <span className="text-xs text-gray-500 mt-1 line-clamp-1">{r.content}</span>
                      </button>
                    )})
                  )}
                </div>
              </div>
            </div>
          </motion.div>
        )}
        </AnimatePresence>

        {/* Delete Confirmation Modal */}
        <AnimatePresence>{isConfirmingDelete && editingResource !== 'new' && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-gray-900/50 backdrop-blur-sm z-[60] flex items-center justify-center p-4">
            <div className="bg-white rounded-3xl p-8 max-w-sm w-full flex flex-col items-center text-center shadow-2xl">
              <div className="w-16 h-16 bg-red-50 rounded-full flex items-center justify-center mb-4">
                <Trash2 className="w-8 h-8 text-red-500" />
              </div>
              <h3 className="text-xl font-black text-gray-900 mb-2">Delete Resource?</h3>
              <p className="text-gray-500 mb-8 text-sm">Are you sure you want to delete this resource? This action cannot be undone.</p>
              
              <div className="w-full flex space-x-3">
                <button 
                  type="button"
                  onClick={() => setIsConfirmingDelete(false)}
                  className="flex-1 py-3 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold rounded-xl transition-colors"
                >
                  Cancel
                </button>
                <button 
                  type="button"
                  onClick={() => {
                    onRemoveResource(editingResource.id);
                    { setNavDirection('backward'); setEditingResource(null); };
                    setIsConfirmingDelete(false);
                  }}
                  className="flex-1 py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl transition-colors"
                >
                  Delete
                </button>
              </div>
            </div>
          </motion.div>
        )}
        </AnimatePresence>
          </motion.div>
        ) : (
          <motion.div
            key="main-dashboard"
            custom={navDirection}
            variants={pageVariants}
            initial="initial"
            animate="animate"
            exit="exit"
            transition={{ duration: 0.25, ease: 'easeOut' }}
            className="max-w-5xl mx-auto w-full p-6 absolute inset-0 overflow-y-auto"
          >
        <header className="flex flex-col md:flex-row md:items-center justify-between mb-8 gap-4 mt-2">

          <h1 className="text-2xl font-black tracking-tighter text-gray-900">QIEZKA</h1>
          
          <div className="flex items-center space-x-4">
            <div className="flex items-center bg-gray-100 rounded-lg px-3 py-1 border border-gray-200" title="Click to simulate a different time">
              <span className="text-xs font-bold text-gray-500 mr-2 uppercase tracking-wider">Simulated Time:</span>
              <input 
                type="time" 
                step="1"
                value={formatForInput(displayTime)}
                onChange={onTimeOverride}
                className="bg-transparent text-sm font-mono text-gray-900 font-bold focus:outline-none"
              />
              {(timeOffset !== 0 || settings.consequenceActive) && (
                <button 
                  onClick={onResetLockdown || onResetTime}
                  className="ml-2 text-xs font-bold text-red-500 hover:text-red-700 bg-red-100 hover:bg-red-200 px-2 py-0.5 rounded cursor-pointer whitespace-nowrap transition-colors"
                  title="Reset simulated time and clear all active locks & consequences"
                >
                  {settings.consequenceActive ? 'Reset All Locks' : 'Reset Time'}
                </button>
              )}
            </div>
            <button
              onClick={onOpenLogs}
              className="p-2 bg-white border border-gray-200 rounded-lg hover:bg-red-50 text-red-500 transition-colors cursor-pointer"
              title="Failed Homeworks"
            >
              <AlertTriangle className="w-5 h-5" />
            </button>
            <button
              onClick={onOpenSettings}
              className="p-2 bg-white border border-gray-200 rounded-lg hover:bg-gray-50 text-gray-500 transition-colors cursor-pointer"
              title="Settings"
            >
              <Settings className="w-5 h-5" />
            </button>
          </div>
        </header>

        {settings.consequenceActive && (
          (() => {
            const hasFailedHomework = (completedHomeworks || []).some(h => !h.passed);
            if (!hasFailedHomework) {
              return (
                <div className="mb-6 p-4 md:p-5 rounded-2xl bg-amber-50 border-2 border-amber-500 shadow-md flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 animate-in fade-in slide-in-from-top duration-300">
                  <div className="flex items-start gap-3.5">
                    <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-600 flex items-center justify-center shrink-0 mt-0.5 sm:mt-0">
                      <AlertTriangle className="w-6 h-6" />
                    </div>
                    <div>
                      <h3 className="font-black text-amber-950 text-base uppercase tracking-wide">
                        Consequence Active (No Failed Homeworks)
                      </h3>
                      <p className="text-xs text-amber-900 mt-0.5 leading-relaxed font-medium">
                        Device restrictions are active in background, but no failed homework session was found in your records.
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2 self-stretch sm:self-auto shrink-0">
                    <button
                      type="button"
                      onClick={onClearConsequence}
                      className="px-4 py-2.5 bg-amber-600 hover:bg-amber-700 active:bg-amber-800 text-white font-bold text-xs rounded-xl shadow-sm transition-colors flex items-center gap-1.5 justify-center cursor-pointer flex-1 sm:flex-initial whitespace-nowrap"
                    >
                      Clear Orphaned Consequence
                    </button>
                  </div>
                </div>
              );
            }

            return (
              <div className="mb-6 p-4 md:p-5 rounded-2xl bg-red-50 border-2 border-red-500 shadow-md flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 animate-in fade-in slide-in-from-top duration-300">
                <div className="flex items-start gap-3.5">
                  <div className="w-10 h-10 rounded-xl bg-red-100 text-red-600 flex items-center justify-center shrink-0 mt-0.5 sm:mt-0">
                    <AlertTriangle className="w-6 h-6" />
                  </div>
                  <div>
                    <h3 className="font-black text-red-950 text-base uppercase tracking-wide">
                      Consequence Active: Homework Failed
                    </h3>
                    <p className="text-xs text-red-800 mt-0.5 leading-relaxed font-medium">
                      {settings.operatingMode === 'hardcore'
                        ? 'Study session expired without passing. Distracting apps remain restricted around the clock (Hardcore 24/7) until rescheduled and passed.'
                        : 'Study session expired without passing. Distracting apps remain restricted during operating hours (7:00 PM – 3:00 AM) until rescheduled and passed.'}
                    </p>
                  </div>
                </div>
                <button
                  type="button"
                  onClick={onOpenLogs}
                  className="px-5 py-2.5 bg-red-600 hover:bg-red-700 active:bg-red-800 text-white font-bold text-xs rounded-xl shadow-sm transition-colors flex items-center gap-1.5 shrink-0 whitespace-nowrap self-stretch sm:self-auto justify-center cursor-pointer"
                >
                  View Failed Homeworks
                </button>
              </div>
            );
          })()
        )}

      {activeSchedules.length === 0 ? (
        <div className="bg-white rounded-3xl p-8 border border-gray-200 shadow-sm text-center flex flex-col items-center justify-center min-h-[200px]">
          <h2 className="text-xl font-black text-gray-900 mb-2">NO ACTIVE SCHEDULES</h2>
          <p className="text-gray-500 max-w-md mx-auto mb-6 text-sm">
            Nothing has been scheduled yet. Create a schedule to define your homework and when the lockdown protocol should activate.
          </p>
          {(!settings.simpleOcrKey && !settings.formattedOcrKey) && (
            <div className="mb-2 max-w-md mx-auto bg-red-50 border border-red-200 text-red-700 p-4 rounded-xl text-sm font-medium flex items-start text-left">
              <ShieldAlert className="w-5 h-5 flex-shrink-0 mr-3 mt-0.5" />
              <div>
                <strong className="block font-bold mb-1">OCR API Key Required (Online Grading)</strong>
                Provide your free OCR API Key (Simple or Formatted) in Settings to transcribe handwritten homework photos.
              </div>
            </div>
          )}
          {!settings.apiKey && (
            <div className="mb-6 max-w-md mx-auto bg-red-50 border border-red-200 text-red-700 p-4 rounded-xl text-sm font-medium flex items-start text-left">
              <ShieldAlert className="w-5 h-5 flex-shrink-0 mr-3 mt-0.5" />
              <div>
                <strong className="block font-bold mb-1">Gemini API Key Required (BYOK)</strong>
                QIEZKA requires you to provide your own Google Gemini API key for online AI evaluation. Study resources and lockdown operate offline, but online grading requires your key. Tap Settings (gear icon) to configure it.
              </div>
            </div>
          )}
          <button
            disabled={!settings.apiKey || (!settings.simpleOcrKey && !settings.formattedOcrKey)}
            onClick={() => {
              onCreateSchedule();
            }}
            className={`px-6 py-3 font-bold rounded-xl shadow-lg transition-all flex items-center ${
              (settings.apiKey && (settings.simpleOcrKey || settings.formattedOcrKey))
                ? 'bg-red-600 hover:bg-red-700 text-white' 
                : 'bg-gray-300 text-gray-500 cursor-not-allowed'
            }`}
          >
            <Plus className="w-5 h-5 mr-2" />
            Create Schedule
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          
          {/* Left Column: Status */}
          <div className="lg:col-span-1 space-y-6">
            <div className="bg-gray-900 rounded-3xl p-6 text-white shadow-xl relative overflow-hidden">
              <div className="absolute top-0 right-0 p-4 opacity-10 pointer-events-none">
                <Clock className="w-32 h-32" />
              </div>
              <h2 className="text-sm font-bold text-gray-400 uppercase tracking-widest mb-2">Next Activation</h2>
              <div className="text-4xl font-mono font-light text-red-500 mb-2">
                {formatTimeLeft(timeUntilLock)}
              </div>
              <p className="text-gray-400 text-sm mb-6">
                {nextActivationDate 
                  ? `Scheduled for ${nextActivationDate.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}` 
                  : 'Scheduling...'}
              </p>

              <button
                disabled={!settings.apiKey}
                onClick={onCreateSchedule}
                className="w-full py-3 bg-red-600 hover:bg-red-700 text-white font-bold rounded-xl transition-colors flex items-center justify-center text-sm disabled:opacity-50 mt-4"
              >
                <Plus className="w-4 h-4 mr-2" />
                Add Another Schedule
              </button>
            </div>
          </div>

          {/* Right Column: Schedule Details */}
          <div className="lg:col-span-2 space-y-4">
            <h2 className="text-xl font-black text-gray-900 flex items-center mb-4">
              <Clock className="w-6 h-6 mr-2 text-gray-500" />
              Active Schedules
            </h2>
            <div className="space-y-4 max-h-[600px] overflow-y-auto pr-2">
              {activeSchedules.map((schedule) => (
                <div key={schedule.id} className="bg-white rounded-3xl p-6 border border-gray-200 shadow-sm relative group">
                  <div className="absolute top-6 right-6">
                    <button
                      onClick={() => onClearSchedule(schedule.id)}
                      className="p-2 text-gray-400 hover:bg-red-50 hover:text-red-500 rounded-lg transition-colors"
                      title="Cancel Schedule"
                    >
                      <Trash2 className="w-5 h-5" />
                    </button>
                  </div>
                  <div className="flex justify-between items-start mb-4 pr-12">
                    <h3 className="text-lg font-bold text-gray-900 flex items-center">
                      <BookOpen className="w-5 h-5 mr-2 text-gray-500" />
                      {schedule.title || 'Homework Session'}
                    </h3>
                  </div>
                  <div className="flex items-center space-x-4 mb-4 text-sm font-bold text-gray-600">
                    <div className="px-3 py-1 bg-gray-100 rounded-lg flex items-center">
                      <Clock className="w-4 h-4 mr-2" />
                      {format12Hour(schedule.activationTime)}
                    </div>
                    <div className="px-3 py-1 bg-gray-100 rounded-lg flex items-center">
                      <Clock className="w-4 h-4 mr-2" />
                      {schedule.durationMinutes} Min
                    </div>
                  </div>
                  
                  <div className="mb-4">
                    <div className="flex justify-between items-center mb-2">
                      <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider">Homework Content</h4>
                      <span 
                        onClick={() => setViewingHomeworkSchedule(schedule)}
                        className="text-[10px] text-indigo-500 font-semibold cursor-pointer hover:underline"
                      >
                        View Full
                      </span>
                    </div>
                    <div 
                      onClick={() => setViewingHomeworkSchedule(schedule)}
                      className="bg-gray-50 hover:bg-gray-100 p-3 rounded-xl border border-gray-100 hover:border-indigo-200 cursor-pointer transition-all shadow-xs"
                      title="Click to view full homework content"
                    >
                      <p className="text-xs text-gray-600 font-mono whitespace-pre-wrap line-clamp-3">
                        {schedule.homeworkContent}
                      </p>
                    </div>
                  </div>

                  {schedule.selectedResourceIds && schedule.selectedResourceIds.length > 0 && (
                    <div className="mb-4">
                      <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2">Attached Resources</h4>
                      <div className="flex flex-wrap gap-2">
                        {schedule.selectedResourceIds.map(resId => {
                          if (resId === 'ai-general-knowledge') {
                            return (
                              <span key="ai-general-knowledge" className="px-2.5 py-1 bg-indigo-50 border border-indigo-200 rounded-lg text-xs font-bold text-indigo-700 flex items-center shadow-xs">
                                <Sparkles className="w-3.5 h-3.5 mr-1.5 text-indigo-500" />
                                🌐 General AI Knowledge
                              </span>
                            );
                          }
                          const res = resources.find(r => r.id === resId);
                          return res ? (
                            <span key={res.id} className="px-2 py-1 bg-gray-100 border border-gray-200 rounded-lg text-xs font-bold text-gray-600 flex items-center">
                              <FileText className="w-3 h-3 mr-1 text-gray-400" />
                              {res.title}
                            </span>
                          ) : null;
                        })}
                      </div>
                    </div>
                  )}

                  <div>
                    <h4 className="text-xs font-bold text-gray-400 uppercase tracking-wider mb-2 flex items-center">
                      {schedule.rubricMode === 'ai' ? <Sparkles className="w-4 h-4 mr-1 text-red-500" /> : <Pencil className="w-4 h-4 mr-1 text-gray-500" />}
                      Rubric
                    </h4>
                    <div 
                      onClick={() => onViewRubric(schedule)}
                      className="bg-red-50 p-3 rounded-xl border border-red-100 cursor-pointer hover:bg-red-100 transition-colors"
                      title="Click to view or edit full rubric"
                    >
                      <p className="text-xs text-red-900 font-mono leading-relaxed whitespace-pre-wrap line-clamp-3">
                        {schedule.rubricContent}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Persistent Course Resource Library */}
      <div className="mt-8 bg-white rounded-2xl p-6 border border-gray-200 shadow-sm">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h2 className="text-xl font-bold text-gray-900 flex items-center">
              <BookOpen className="w-6 h-6 mr-2 text-gray-500" />
              Saved Course Resources
            </h2>
            <p className="text-xs text-gray-400 mt-0.5">Syllabuses, lecture notes, and textbook excerpts.</p>
          </div>
          <button
            onClick={() => openNewResource('lecture_notes')}
            className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold rounded-xl transition-colors flex items-center text-sm"
          >
            <Plus className="w-4 h-4 mr-2" />
            Add Resource
          </button>
        </div>

        {resources.filter(r => r.type !== 'case_study').length === 0 ? (
          <div className="flex flex-col items-center justify-center text-gray-400 p-8 text-center bg-gray-50 rounded-2xl border border-dashed border-gray-200">
            <BookOpen className="w-12 h-12 mb-4 opacity-20" />
            <p className="font-bold text-gray-500 mb-1">Your course library is empty</p>
            <p className="text-sm">Save lecture notes or textbooks here to use them in schedules.</p>
          </div>
        ) : (
          <div className="flex flex-col space-y-3 max-h-[300px] overflow-y-auto pr-2">
            {resources.filter(r => r.type !== 'case_study').map((res) => {
              const isLocked = settings.schedules.some(s => s.isActive && (s.selectedResourceIds || []).includes(res.id));
              return (
              <div 
                key={res.id} 
                onClick={() => {
                  openEditResource(res);
                }}
                className={`group p-4 bg-white border border-gray-200 rounded-xl transition-all shadow-sm flex items-center justify-between text-left w-full cursor-pointer ${isLocked ? 'hover:border-red-300' : 'hover:border-gray-900'}`}
              >
                <div className="flex items-center">
                  <BookOpen className={`w-5 h-5 mr-3 transition-colors ${isLocked ? 'text-gray-300' : 'text-gray-400 group-hover:text-gray-900'}`} />
                  <h3 className={`font-bold text-base ${isLocked ? 'text-gray-500' : 'text-gray-900'}`}>{res.title}</h3>
                </div>
                {isLocked && <div className="text-[10px] font-bold text-red-500 bg-red-50 px-2 py-1 rounded border border-red-100 uppercase tracking-wider flex items-center"><ShieldAlert className="w-3 h-3 mr-1" /> Locked (View Only)</div>}
              </div>
            )})}
          </div>
        )}
      </div>

      {/* Case Studies & Articles Library */}
      <div className="mt-8 bg-white rounded-2xl p-6 border border-gray-200 shadow-sm">
        <div className="flex justify-between items-center mb-6">
          <div>
            <h2 className="text-xl font-bold text-gray-900 flex items-center">
              <FileText className="w-6 h-6 mr-2 text-indigo-600" />
              Case Studies & Articles
            </h2>
            <p className="text-xs text-gray-400 mt-0.5">News reports, documentaries, movies, and journal articles for milestone analysis.</p>
          </div>
          <button
            onClick={() => openNewResource('case_study')}
            className="px-4 py-2 bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-bold rounded-xl transition-colors flex items-center text-sm cursor-pointer"
          >
            <Plus className="w-4 h-4 mr-2" />
            Add Case Study
          </button>
        </div>

        {resources.filter(r => r.type === 'case_study').length === 0 ? (
          <div className="flex flex-col items-center justify-center text-gray-400 p-8 text-center bg-gray-50 rounded-2xl border border-dashed border-gray-200">
            <FileText className="w-12 h-12 mb-4 opacity-20 text-indigo-600" />
            <p className="font-bold text-gray-500 mb-1">No case studies or articles yet</p>
            <p className="text-sm">Save external articles, documentaries, or news reports here. They will be cleaned using Article Declutter.</p>
          </div>
        ) : (
          <div className="flex flex-col space-y-3 max-h-[300px] overflow-y-auto pr-2">
            {resources.filter(r => r.type === 'case_study').map((res) => {
              const isLocked = settings.schedules.some(s => s.isActive && (s.selectedResourceIds || []).includes(res.id));
              return (
              <div 
                key={res.id} 
                onClick={() => {
                  openEditResource(res);
                }}
                className={`group p-4 bg-white border border-gray-200 rounded-xl transition-all shadow-sm flex items-center justify-between text-left w-full cursor-pointer ${isLocked ? 'hover:border-red-300' : 'hover:border-indigo-600'}`}
              >
                <div className="flex items-center min-w-0 pr-4">
                  <FileText className={`w-5 h-5 flex-shrink-0 mr-3 transition-colors ${isLocked ? 'text-gray-300' : 'text-indigo-400 group-hover:text-indigo-600'}`} />
                  <div className="min-w-0">
                    <h3 className={`font-bold text-base truncate ${isLocked ? 'text-gray-500' : 'text-gray-900'}`}>{res.title}</h3>
                    <p className="text-xs text-gray-400 line-clamp-1 mt-0.5">{res.content.slice(0, 100)}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2 flex-shrink-0">
                  <span className="text-[10px] font-bold text-indigo-600 bg-indigo-50 px-2 py-1 rounded border border-indigo-100 uppercase tracking-wider">Case Study</span>
                  {isLocked && <div className="text-[10px] font-bold text-red-500 bg-red-50 px-2 py-1 rounded border border-red-100 uppercase tracking-wider flex items-center"><ShieldAlert className="w-3 h-3 mr-1" /> Locked</div>}
                </div>
              </div>
            )})}
          </div>
        )}
      </div>

      {/* Allowed Apps Section */}
      <div className="mt-8 bg-white rounded-2xl p-6 border border-gray-200 shadow-sm">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-xl font-bold text-gray-900 flex items-center">
            <LayoutGrid className="w-6 h-6 mr-2 text-gray-500" />
            Allowed Applications
          </h2>
          <button
            onClick={() => setIsAppSelectorOpen(true)}
            className="px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold rounded-xl transition-colors flex items-center"
          >
            <Plus className="w-4 h-4 mr-2" />
            Add App
          </button>
        </div>
        <p className="text-sm text-gray-500 mb-6">These applications will remain accessible when the system is locked.</p>

        {(() => {
          const pool = (installedApps && installedApps.length > 0) ? installedApps : availableApps;
          const hardcodedApps: AllowedApp[] = (pool || []).filter(app => 
            !isAppBlacklisted(app.id) && (app.isHardcoded || app.isLauncher)
          );

          const customApps = (settings.allowedApps || []).filter(app => 
            !isAppBlacklisted(app.id) && 
            !(app.isHardcoded || app.isLauncher) &&
            !hardcodedApps.some(h => h.id === app.id)
          );

          const unifiedAllowedApps: Array<AllowedApp & { isHardcoded: boolean }> = [
            ...hardcodedApps.map(h => ({ ...h, isHardcoded: true })),
            ...customApps.map(c => ({ ...c, isHardcoded: false }))
          ];

          const iconMap: Record<string, React.ElementType> = {
            'Calculator': Calculator,
            'FileText': FileText,
            'Music': Music,
            'Globe': Globe,
            'BookOpen': BookOpen,
            'MessageSquare': MessageSquare,
            'MonitorPlay': MonitorPlay,
            'Camera': Camera,
            'ShieldCheck': ShieldCheck,
            'Sparkles': Sparkles
          };

          const renderAppItem = (app: AllowedApp, isHardcoded: boolean) => {
            let FallbackIcon = iconMap[app.iconName] || LayoutGrid;
            if (app.isLauncher) FallbackIcon = Home;

            return (
              <div 
                key={app.id} 
                className="flex flex-col items-center group relative w-full text-center" 
                title={isHardcoded ? `${app.name} (Always Allowed by System)` : app.name}
              >
                <div className="relative">
                  {isHardcoded ? (
                    <div className="absolute -top-1.5 -right-1.5 bg-emerald-600 text-white rounded-full p-0.5 z-10 shadow-2xs">
                      <ShieldCheck className="w-3 h-3" />
                    </div>
                  ) : (
                    <button
                      onClick={() => {
                        if (onSettingsChange) {
                          onSettingsChange({
                            allowedApps: settings.allowedApps!.filter(a => a.id !== app.id)
                          });
                        }
                      }}
                      className="absolute -top-1.5 -right-1.5 bg-gray-900 text-white rounded-full p-1 opacity-0 group-hover:opacity-100 transition-opacity z-10 hover:bg-red-500 cursor-pointer shadow-xs"
                      title="Remove App"
                    >
                      <X className="w-3 h-3" />
                    </button>
                  )}
                  <div className={`w-14 h-14 sm:w-16 sm:h-16 rounded-2xl flex items-center justify-center shadow-2xs overflow-hidden ${
                    isHardcoded 
                      ? 'bg-emerald-50/80 border border-emerald-200/80 text-emerald-700' 
                      : 'bg-white border border-gray-200 text-gray-700 group-hover:bg-gray-50 transition-colors'
                  }`}>
                    {app.iconBase64 ? (
                      <img src={`data:image/png;base64,${app.iconBase64}`} alt={app.name} className="w-full h-full object-cover p-1.5" />
                    ) : (
                      <FallbackIcon className={`w-7 h-7 ${isHardcoded ? 'text-emerald-700' : 'text-gray-700'}`} />
                    )}
                  </div>
                </div>
                <span className="text-[11px] sm:text-xs font-semibold text-gray-700 w-full text-center truncate px-1 mt-1.5">{app.name}</span>
                {isHardcoded && (
                  <span className="text-[9px] font-bold uppercase tracking-tight text-emerald-600">
                    Always
                  </span>
                )}
              </div>
            );
          };

          return (
            <div 
              className="max-h-80 sm:max-h-96 overflow-y-auto custom-scrollbar pr-1"
              style={{ WebkitOverflowScrolling: 'touch', overscrollBehavior: 'contain' }}
            >
              <div className="flex flex-col gap-y-6 py-2">
                {customApps.length > 0 && (
                  <div className="grid grid-cols-4 gap-y-6 gap-x-3 sm:gap-x-4">
                    {customApps.map(app => renderAppItem(app, false))}
                  </div>
                )}
                <div className="grid grid-cols-4 gap-y-6 gap-x-3 sm:gap-x-4">
                  {hardcodedApps.map(app => renderAppItem(app, true))}
                </div>
              </div>
            </div>
          );
        })()}
      </div>
      
      {/* App Selector Modal */}
      <AnimatePresence>
        {isAppSelectorOpen && (
          <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} exit={{ opacity: 0 }} className="fixed inset-0 bg-gray-900/50 backdrop-blur-sm z-[60] flex items-center justify-center p-4" style={{ paddingBottom: 'calc(1rem + var(--safe-bottom))', paddingTop: 'calc(1rem + var(--safe-top))' }}>
          <motion.div initial={{ scale: 0.95, opacity: 0 }} animate={{ scale: 1, opacity: 1 }} exit={{ scale: 0.95, opacity: 0 }} className="bg-white rounded-3xl p-6 max-w-lg w-full flex flex-col shadow-2xl overflow-hidden max-h-[85vh]">
              <div className="flex justify-between items-center mb-6">
                <div>
                  <h3 className="text-xl font-black text-gray-900">Select Allowed Apps</h3>
                  <p className="text-sm text-gray-500 mt-1">Choose apps to whitelist during system lock</p>
                </div>
                <button onClick={() => setIsAppSelectorOpen(false)} className="p-2 bg-gray-100 hover:bg-gray-200 rounded-full transition-colors">
                  <X className="w-5 h-5 text-gray-600" />
                </button>
              </div>
              
              {isLoadingApps ? (
                <div className="flex flex-col items-center justify-center py-16 gap-3">
                  <Loader2 className="w-8 h-8 animate-spin text-gray-400" />
                  <p className="text-sm text-gray-500 font-medium">Loading installed apps...</p>
                </div>
              ) : (
                <div className="grid grid-cols-4 sm:grid-cols-5 gap-y-6 gap-x-4 overflow-y-auto py-2 flex-1 pr-1">
                  {availableApps
                    .filter((simApp) => 
                      !isAppBlacklisted(simApp.id) && 
                      !(simApp.isHardcoded || simApp.isLauncher)
                    )
                    .map((simApp) => {
                    const iconMap: Record<string, React.ElementType> = {
                      'Calculator': Calculator,
                      'FileText': FileText,
                      'Music': Music,
                      'Globe': Globe,
                      'BookOpen': BookOpen,
                      'MessageSquare': MessageSquare,
                      'MonitorPlay': MonitorPlay,
                      'Camera': Camera,
                      'ShieldCheck': ShieldCheck,
                      'Sparkles': Sparkles
                    };
                    let FallbackIcon = iconMap[simApp.iconName] || LayoutGrid;
                    if (simApp.isLauncher) FallbackIcon = Home;

                    const isSelected = (settings.allowedApps || []).some(a => a.id === simApp.id);
                    
                    return (
                      <div 
                        key={simApp.id}
                        onClick={() => {
                          if (!onSettingsChange) return;
                          if (isAppBlacklisted(simApp.id)) {
                            showError('This application is classified as a distraction and cannot be allowed during lockdown.');
                            return;
                          }
                          const current = (settings.allowedApps || []).filter(a => !isAppBlacklisted(a.id));
                          if (isSelected) {
                            onSettingsChange({ allowedApps: current.filter(a => a.id !== simApp.id) });
                          } else {
                            onSettingsChange({ allowedApps: [...current, simApp] });
                          }
                        }}
                        className="flex flex-col items-center cursor-pointer group"
                      >
                        <div className={`w-14 h-14 rounded-2xl flex items-center justify-center mb-2 transition-all relative overflow-hidden ${isSelected ? 'bg-blue-50 border-2 border-blue-500 text-blue-600 shadow-sm' : 'bg-white border border-gray-200 text-gray-500 hover:border-gray-400 hover:shadow-sm'}`}>
                          {isSelected && (
                            <div className="absolute -top-1 -right-1 bg-blue-500 text-white rounded-full p-0.5 border-2 border-white z-10">
                              <Check className="w-2.5 h-2.5" />
                            </div>
                          )}
                          {simApp.iconBase64 ? (
                            <img src={`data:image/png;base64,${simApp.iconBase64}`} alt={simApp.name} className="w-full h-full object-cover" />
                          ) : (
                            <FallbackIcon className="w-7 h-7" />
                          )}
                        </div>
                        <span className={`text-[10px] font-bold text-center w-full truncate px-1 ${isSelected ? 'text-blue-700' : 'text-gray-600'}`}>{simApp.name}</span>
                      </div>
                    );
                  })}
                </div>
              )}
              
              <div className="mt-6 flex justify-end pt-4 border-t border-gray-100">
                <button 
                  onClick={() => setIsAppSelectorOpen(false)}
                  className="px-6 py-3 bg-gray-900 hover:bg-black text-white font-bold rounded-xl transition-colors"
                >
                  Done
                </button>
              </div>
            </motion.div>
          </motion.div>
        )}
      </AnimatePresence>
          </motion.div>
        )}
      </AnimatePresence>
      {viewingHomeworkSchedule && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white rounded-3xl w-full max-w-2xl max-h-[85vh] shadow-2xl overflow-hidden flex flex-col">
            <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50/80">
              <div className="flex items-center space-x-3">
                <div className="p-2 bg-indigo-50 text-indigo-600 rounded-xl">
                  <FileText className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-gray-900 text-lg">
                    {viewingHomeworkSchedule.title || 'Homework Session'}
                  </h3>
                  <p className="text-xs text-gray-500">
                    Full Homework Content (Read Only)
                  </p>
                </div>
              </div>
              <button
                onClick={() => setViewingHomeworkSchedule(null)}
                className="p-2 bg-gray-100 hover:bg-gray-200 rounded-full text-gray-500 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="flex-1 overflow-y-auto p-6 bg-gray-50">
              <div className="bg-white p-5 rounded-2xl border border-gray-200 shadow-sm">
                <p className="font-mono text-sm text-gray-800 whitespace-pre-wrap leading-relaxed select-text">
                  {viewingHomeworkSchedule.homeworkContent || 'No homework content provided.'}
                </p>
              </div>
            </div>

            <div className="p-4 bg-white border-t border-gray-100 flex justify-end">
              <button
                onClick={() => setViewingHomeworkSchedule(null)}
                className="px-6 py-2.5 bg-gray-900 hover:bg-black text-white font-bold text-sm rounded-xl transition-colors"
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

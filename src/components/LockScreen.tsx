import React, { useState, useEffect, useRef } from 'react';
import { Lock, Upload, Camera, FileWarning, CheckCircle, Sparkles, X, Loader2, RefreshCcw, Calculator, FileText, Music, Globe, MessageSquare, MonitorPlay, BookOpen, LayoutGrid, AlertTriangle, ShieldCheck } from 'lucide-react';
import { motion } from 'motion/react';
import { ScheduleData, AppSettings, SavedResource, AllowedApp } from '../types';
import { parseResource } from '../api/parseResource';
import { generateAnswer } from '../api/generateAnswer';
import { endLockdown } from '../systemBridge';
import { isAppBlacklisted } from '../constants/blacklistedApps';
import { 
  DEFAULT_HARDCODED_APPS, 
  isHardcodedApp, 
  isHiddenSystemExemptApp,
  isBrowserPackage,
  isMusicPackage,
  isCameraPackage,
  isAuthenticatorPackage,
  isAiPackage,
  isNotesPackage,
  isStudentPackage,
  isMessagingPackage
} from '../constants/allowedApps';

interface LockScreenProps {
  schedule: ScheduleData;
  settings: AppSettings;
  resources: SavedResource[];
  lockEndTime: number;
  onSubmitHomework: (file: File, ocrType: 'simple' | 'formatted', transcribedText?: string) => void;
    onTimeout: (skipped?: boolean) => void;
  getCurrentTime: () => number;
  onTimeOverride?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  timeOffset?: number;
  onResetTime?: () => void;
  onSettingsChange?: (updates: Partial<AppSettings>) => void;
  installedApps?: AllowedApp[];
}

export function LockScreen({ schedule, settings, resources, lockEndTime, onSubmitHomework, onTimeout, getCurrentTime, onTimeOverride, timeOffset, onResetTime, onSettingsChange, installedApps = [] }: LockScreenProps) {
  const [timeLeft, setTimeLeft] = useState(() => {
    const maxSec = (schedule.durationMinutes || 25) * 60;
    return Math.min(maxSec, Math.max(0, Math.floor((lockEndTime - getCurrentTime()) / 1000)));
  });
  
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  
  const formatForInput = (date: Date) => {
    return date.toTimeString().split(' ')[0];
  };
  
  const [transcribedText, setTranscribedText] = useState<string | null>(null);
  const [isTranscribing, setIsTranscribing] = useState(false);
  const [ocrError, setOcrError] = useState<string | null>(null);

  const [ocrType, setOcrType] = useState<'simple' | 'formatted'>(settings.defaultOcrType || 'simple');
  const fileInputRef = useRef<HTMLInputElement>(null);

  // Load from local storage on mount
  useEffect(() => {
    const savedData = localStorage.getItem(`lockscreen_data_${schedule.id}`) || localStorage.getItem('lockscreen_last_draft');
    if (savedData) {
      try {
        const parsed = JSON.parse(savedData);
        if (parsed.transcribedText) {
          setTranscribedText(parsed.transcribedText);
        }
        if (parsed.fileData && parsed.fileName && parsed.fileType) {
          fetch(parsed.fileData)
            .then(res => res.blob())
            .then(blob => {
              const file = new File([blob], parsed.fileName, { type: parsed.fileType });
              setSelectedFile(file);
              setPreviewUrl(URL.createObjectURL(file));
            });
        }
      } catch (e) {
        console.error("Failed to restore lockscreen data", e);
      }
    }
  }, [schedule.id]);

  // Save to local storage on change
  useEffect(() => {
    const saveData = async () => {
      let fileData = null;
      let fileName = null;
      let fileType = null;

      if (selectedFile) {
        fileName = selectedFile.name;
        fileType = selectedFile.type;
        const reader = new FileReader();
        fileData = await new Promise((resolve) => {
          reader.onloadend = () => resolve(reader.result);
          reader.readAsDataURL(selectedFile);
        });
      }

      const dataToSave = {
        transcribedText,
        fileData,
        fileName,
        fileType
      };
      
      if (!transcribedText && !selectedFile) {
        localStorage.removeItem(`lockscreen_data_${schedule.id}`);
        localStorage.removeItem('lockscreen_last_draft');
      } else {
        try {
          const json = JSON.stringify(dataToSave);
          localStorage.setItem(`lockscreen_data_${schedule.id}`, json);
          localStorage.setItem('lockscreen_last_draft', json);
        } catch(e) {
          console.warn("Storage quota exceeded, could not save image to local storage.");
        }
      }
    };

    saveData();
  }, [transcribedText, selectedFile, schedule.id]);

  const [isGeneratingAnswer, setIsGeneratingAnswer] = useState(false);
  const [aiAnswer, setAiAnswer] = useState<string | null>(null);
  const [showAnswerPopup, setShowAnswerPopup] = useState(false);
  const [selectedCategory, setSelectedCategory] = useState<string>('auto');

  useEffect(() => {
    const handleBack = (e: Event) => {
      if (showAnswerPopup) {
        e.preventDefault();
        setShowAnswerPopup(false);
      }
    };
    window.addEventListener('qiezka-back-press', handleBack);
    return () => window.removeEventListener('qiezka-back-press', handleBack);
  }, [showAnswerPopup]);

  const handleGetAnswer = async (force = false, categoryOverride?: string) => {
    setShowAnswerPopup(true);
    const effectiveCategory = categoryOverride !== undefined ? categoryOverride : selectedCategory;
    if (aiAnswer && !force && effectiveCategory === selectedCategory) return;

    setIsGeneratingAnswer(true);
    setShowAnswerPopup(true);
    setAiAnswer(null);
    try {
      const hasGeneralKnowledge = (schedule.selectedResourceIds || []).includes('ai-general-knowledge');
      let resourcesText = resources.filter(r => (schedule.selectedResourceIds || []).includes(r.id)).map(r => `--- ${r.title} ---\n${r.content}`).join('\n\n');
      if (hasGeneralKnowledge) {
        resourcesText += '\n\n=== SYSTEM NOTE ===\nThe AI is authorized to browse online knowledge, use live web search, and draw from external general knowledge to complete this task.';
      }

      const answer = await generateAnswer({
        content: schedule.homeworkContent,
        resourcesText,
        rubric: schedule.rubricContent,
        apiKey: settings.apiKey || '',
        apiModel: settings.apiModel || 'gemini-2.0-flash',
        customPrompts: settings.prompts,
        isGeneralKnowledge: hasGeneralKnowledge,
        category: effectiveCategory,
      });

      if (answer) {
        setAiAnswer(answer);
        schedule.aiAnswer = answer;
      } else {
        setAiAnswer('Error: Empty response from AI.');
      }
    } catch (err: any) {
      setAiAnswer(`Error: ${err.message}`);
    } finally {
      setIsGeneratingAnswer(false);
    }
  };

  const hasTimedOutRef = useRef(false);

  useEffect(() => {
    let effectiveEndTime = lockEndTime;
    const initialNow = getCurrentTime();
    const maxAllowedSec = (schedule.durationMinutes || 25) * 60;
    if (effectiveEndTime > initialNow + maxAllowedSec * 1000) {
      effectiveEndTime = initialNow + maxAllowedSec * 1000;
    }

    let timer: any = null;
    const update = () => {
      const now = getCurrentTime();
      const rawRemaining = Math.max(0, Math.floor((effectiveEndTime - now) / 1000));
      const remaining = Math.min(maxAllowedSec, rawRemaining);
      setTimeLeft(prev => (prev !== remaining ? remaining : prev));
      
      if (remaining === 0) {
        if (hasTimedOutRef.current) return;
        hasTimedOutRef.current = true;
        if (timer) clearInterval(timer);
        // Delegate to onTimeout to log failed homework and transition to Consequence Mode
        onTimeout();
      }
    };
    update();
    timer = setInterval(update, 1000);
    return () => {
      if (timer) clearInterval(timer);
    };
  }, [lockEndTime, onTimeout, getCurrentTime, schedule.id, schedule.durationMinutes]);

  const formatTime = (seconds: number) => {
    const h = Math.floor(seconds / 3600);
    const m = Math.floor((seconds % 3600) / 60);
    const s = seconds % 60;
    if (h > 0) {
      return `${h}:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
    }
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  const runOCR = async (file: File) => {
    setIsTranscribing(true);
    setOcrError(null);
    try {
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
      if (data.error) {
        setOcrError(data.error);
      } else if (data.content) {
        setTranscribedText(data.content);
        setOcrError(null);
      } else {
        setOcrError('No text could be extracted from the image. You can manually type your handwritten answer below.');
      }
    } catch (err: any) {
      setOcrError(err.message || 'OCR service timed out or failed. You can manually type your answer below.');
    } finally {
      setIsTranscribing(false);
    }
  };

  const handleFileChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      if (file.size > 5 * 1024 * 1024) {
        alert("This image is too large (over 5MB). Please compress it or take a lower resolution photo before uploading.");
        return;
      }
      setSelectedFile(file);
      const url = URL.createObjectURL(file);
      setPreviewUrl(url);

      runOCR(file);
    }
  };

  const handleSubmit = () => {
    if (!selectedFile) {
      alert("Please attach a photo of your physical homework first.");
      return;
    }
    if (!transcribedText || !transcribedText.trim()) {
      alert("Please enter or transcribe your homework answer before submitting.");
      return;
    }
    // NOTE: We no longer clear localStorage here. It is cleared in App.tsx ONLY if evaluation passes.
    onSubmitHomework(selectedFile, ocrType, transcribedText.trim());
  };


  return (
    <div className="fixed inset-0 bg-gray-950 flex flex-col items-center p-6 text-white z-50 overflow-y-auto" style={{ paddingBottom: 'calc(3rem + var(--safe-bottom))', paddingTop: 'calc(1.5rem + var(--safe-top))' }}>
      <div className="absolute top-0 left-0 w-full h-1 bg-gray-800">
        <motion.div 
          className="h-full bg-red-600"
          initial={{ width: '100%' }}
          animate={{ width: `${(timeLeft / (schedule.durationMinutes * 60)) * 100}%` }}
          transition={{ duration: 1, ease: 'linear' }}
        />
      </div>

      <div className="max-w-3xl w-full flex flex-col items-center relative">
        <div className="absolute top-0 right-0 flex items-center gap-2">
          <select
            value={selectedCategory}
            onChange={(e) => setSelectedCategory(e.target.value)}
            className="bg-gray-900 border border-gray-700 text-indigo-300 text-xs font-semibold rounded-lg px-2.5 py-1.5 focus:outline-none focus:border-indigo-500 cursor-pointer shadow-xs"
            title="Homework Format Category"
          >
            <option value="auto">Auto Category</option>
            <option value="reflection">Reflection / Essay</option>
            <option value="coding">Coding / CS Task</option>
            <option value="math">Math / Physics</option>
            <option value="case_study">Case Study</option>
            <option value="worksheets">Worksheets / Q&A</option>
          </select>
          <button 
            onClick={() => handleGetAnswer(false)}
            className="px-4 py-2 bg-indigo-900 hover:bg-indigo-800 text-xs text-indigo-300 font-bold rounded-lg transition-colors flex items-center shadow-xs"
          >
            <Sparkles className="w-3 h-3 mr-2" />
            Get AI Answer
          </button>
          <button 
            onClick={() => { localStorage.removeItem(`lockscreen_data_${schedule.id}`); onTimeout(true); }}
            className="px-4 py-2 bg-gray-800 hover:bg-gray-700 text-xs text-gray-400 font-bold rounded-lg transition-colors"
          >
            Skip Lock (Test)
          </button>
        </div>

        <Lock className="w-16 h-16 text-red-500 mb-6" />
        
        <h2 className="text-4xl font-black tracking-widest text-white mb-2">SYSTEM LOCKED</h2>
        <div className="text-6xl font-mono text-red-500 mb-2 font-light">
          {formatTime(timeLeft)}
        </div>
        <div className="flex items-center justify-center space-x-2 mb-8 bg-gray-900/50 rounded-lg px-4 py-2 border border-gray-800">
          <span className="text-xs font-bold text-gray-500 uppercase tracking-wider">Simulated Time:</span>
          {onTimeOverride ? (
            <>
              <input 
                type="time" 
                step="1"
                value={formatForInput(new Date(getCurrentTime()))}
                onChange={onTimeOverride}
                className="bg-transparent text-sm font-mono text-gray-300 font-bold focus:outline-none"
              />
              {(timeOffset || 0) !== 0 && (
                <button 
                  onClick={onResetTime}
                  className="ml-2 text-[10px] font-bold text-red-400 hover:text-red-300 bg-red-900/30 px-2 py-0.5 rounded uppercase"
                >
                  Reset
                </button>
              )}
            </>
          ) : (
            <span className="text-sm font-mono text-gray-300 font-bold">{new Date(getCurrentTime()).toLocaleTimeString()}</span>
          )}
        </div>

        <div className="w-full bg-gray-900 border border-red-900/50 rounded-2xl p-6 mb-8 text-left flex flex-col gap-4">
          <div>
            <h3 className="text-red-400 font-bold mb-2 uppercase text-sm tracking-widest flex items-center">
              <FileWarning className="w-4 h-4 mr-2" /> Required to Unlock
            </h3>
            <p className="text-gray-300 font-mono text-sm leading-relaxed">
              Submit a photo of your handwritten homework covering the topic below and satisfying its rubric.
            </p>
          </div>
          
          <div>
            <h4 className="text-gray-500 font-bold uppercase text-xs mb-1">Homework Context</h4>
            <div className="bg-black/40 p-4 rounded-xl border border-gray-800 max-h-32 overflow-y-auto">
              <p className="text-sm text-gray-300 font-mono whitespace-pre-wrap">{schedule.homeworkContent}</p>
            </div>
          </div>

          {schedule.selectedResourceIds && schedule.selectedResourceIds.length > 0 && (
            <div>
              <h4 className="text-gray-500 font-bold uppercase text-xs mb-1">Attached Resources</h4>
              <div className="flex flex-wrap gap-2">
                {schedule.selectedResourceIds.map(resId => {
                  if (resId === 'ai-general-knowledge') {
                    return (
                      <span key="ai-general-knowledge" className="px-2.5 py-1 bg-indigo-950/60 border border-indigo-800 rounded-lg text-xs font-bold text-indigo-300 flex items-center shadow-xs">
                        <Sparkles className="w-3.5 h-3.5 mr-1.5 text-indigo-400" />
                        🌐 General AI Knowledge
                      </span>
                    );
                  }
                  const res = resources.find(r => r.id === resId);
                  return res ? (
                    <span key={res.id} className="px-2 py-1 bg-gray-900 border border-gray-800 rounded-lg text-xs font-bold text-gray-300 flex items-center">
                      <FileText className="w-3 h-3 mr-1 text-gray-500" />
                      {res.title}
                    </span>
                  ) : null;
                })}
              </div>
            </div>
          )}

          <div>
            <h4 className="text-gray-500 font-bold uppercase text-xs mb-1">Grading Rubric</h4>
            <div className="bg-black/40 p-4 rounded-xl border border-red-900/30 max-h-48 overflow-y-auto">
              <p className="text-sm text-red-300 font-mono whitespace-pre-wrap">{schedule.rubricContent}</p>
            </div>
          </div>
        </div>

        <div className="w-full bg-gray-900 border border-gray-800 rounded-2xl p-8 flex flex-col items-center">
          <div className="w-full flex justify-between items-center mb-4">
            <h4 className="text-gray-400 font-medium">Upload Submission</h4>
            <div className="flex gap-2">
              <select
                value={settings.apiModel || 'gemini-3.7-flash'}
                onChange={(e) => onSettingsChange?.({ apiModel: e.target.value })}
                className="bg-gray-800 text-sm text-gray-300 px-3 py-1.5 rounded-lg border border-gray-700 focus:outline-none focus:border-gray-500"
              >
                <option value="gemini-3.7-flash">Gemini 3.7 Flash</option>
                <option value="gemini-3.7-pro">Gemini 3.7 Pro</option>
                <option value="gemini-3.5-flash">Gemini 3.5 Flash</option>
                <option value="gemini-3.5-pro">Gemini 3.5 Pro</option>
                <option value="gemini-2.5-flash">Gemini 2.5 Flash</option>
                <option value="gemini-2.5-pro">Gemini 2.5 Pro</option>
                <option value="gemini-2.0-flash">Gemini 2.0 Flash</option>
                <option value="gemini-2.0-pro">Gemini 2.0 Pro</option>
              </select>
              <select
                value={ocrType}
                onChange={(e) => setOcrType(e.target.value as 'simple' | 'formatted')}
                className="bg-gray-800 text-sm text-gray-300 px-3 py-1.5 rounded-lg border border-gray-700 focus:outline-none focus:border-gray-500"
              >
                <option value="simple">Simple OCR</option>
                <option value="formatted">Formatted OCR (Tables)</option>
              </select>
            </div>
          </div>
          {!previewUrl ? (
            <div className="w-full">
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                className="group w-full p-6 sm:p-7 rounded-2xl border-2 border-dashed border-gray-700 hover:border-indigo-500 bg-gray-950/50 hover:bg-gray-900/80 transition-all flex flex-col items-center justify-center text-center cursor-pointer shadow-sm hover:shadow-indigo-500/10"
              >
                <div className="w-14 h-14 rounded-2xl bg-indigo-500/10 text-indigo-400 group-hover:bg-indigo-500/20 group-hover:scale-105 flex items-center justify-center mb-3 transition-all relative">
                  <Camera className="w-6 h-6 absolute -translate-x-1.5 -translate-y-1 text-indigo-400" />
                  <Upload className="w-5 h-5 absolute translate-x-2 translate-y-1.5 text-indigo-300" />
                </div>
                <span className="text-white font-bold text-base mb-1">Upload Homework</span>
                <span className="text-gray-400 text-xs leading-relaxed max-w-sm">
                  Tap to capture with camera or choose from gallery / files
                </span>
                <div className="mt-2.5 flex items-center gap-1.5 text-[11px] font-semibold text-indigo-400 bg-indigo-950/40 px-2.5 py-0.5 rounded-full border border-indigo-900/50">
                  <span>Camera + Gallery Supported</span>
                </div>
              </button>
            </div>
          ) : (
            <div className="w-full flex flex-col items-center">
              
              <div className="relative w-full aspect-[4/3] rounded-xl overflow-hidden mb-6 border border-gray-700">
                <img src={previewUrl} alt="Homework Preview" className="object-cover w-full h-full" />
                <button 
                  onClick={() => {
                    setSelectedFile(null);
                    setPreviewUrl(null);
                    setTranscribedText(null);
                    if (fileInputRef.current) fileInputRef.current.value = '';
                  }}
                  className="absolute top-4 right-4 bg-gray-900/80 p-2 rounded-full hover:bg-red-600 transition-colors"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <div className="w-full mb-6 flex flex-col">
                {ocrError && (
                  <div className="w-full mb-4 p-3.5 bg-amber-950/60 border border-amber-800/80 rounded-xl text-xs text-amber-200 flex items-start gap-2.5 shadow-sm text-left">
                    <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-bold text-amber-300">OCR Notice: </span>
                      <span>{ocrError}</span>
                      <p className="mt-1 text-amber-200/80 leading-relaxed">
                        You can manually type your answer in the text box below. Your attached physical photo will still be sent as proof.
                      </p>
                    </div>
                  </div>
                )}
                
                <h4 className="text-gray-400 font-medium mb-2 flex items-center justify-between">
                  <span>Transcribed Text</span>
                  <div className="flex items-center space-x-3">
                    {isTranscribing && <span className="text-xs text-indigo-400 flex items-center"><Loader2 className="w-3 h-3 mr-1 animate-spin" /> Extracting...</span>}
                    <button 
                      onClick={() => selectedFile && runOCR(selectedFile)}
                      disabled={isTranscribing || !selectedFile}
                      className="text-xs font-bold text-gray-500 hover:text-white transition-colors flex items-center disabled:opacity-50"
                    >
                      <RefreshCcw className="w-3 h-3 mr-1" /> Retry OCR
                    </button>
                  </div>
                </h4>

                <textarea
                  value={transcribedText || ''}
                  onChange={(e) => setTranscribedText(e.target.value)}
                  disabled={isTranscribing}
                  placeholder={isTranscribing ? "Running OCR..." : "Transcription will appear here (or manually type your answer)..."}
                  className="w-full h-32 bg-gray-950 border border-gray-700 rounded-xl p-3 text-sm text-gray-300 font-mono focus:border-indigo-500 focus:outline-none resize-none disabled:opacity-50"
                />
                <p className="text-xs text-gray-500 mt-2">
                  Photo is attached as physical proof. You can edit the text or manually type your answer before submitting.
                </p>
              </div>

              <button
                onClick={handleSubmit}
                disabled={isTranscribing || !transcribedText?.trim()}
                className="w-full py-4 bg-white text-black hover:bg-gray-200 disabled:bg-gray-700 disabled:text-gray-400 disabled:cursor-not-allowed font-bold rounded-xl transition-colors flex items-center justify-center cursor-pointer shadow-md"
              >
                <Upload className="w-5 h-5 mr-2" />
                Submit for AI Evaluation
              </button>
            </div>
          )}

          <input 
            type="file" 
            accept="image/*" 
            ref={fileInputRef} 
            className="hidden" 
            onChange={handleFileChange}
          />
        </div>
        
        <div className="w-full bg-gray-900 border border-gray-800 rounded-2xl p-6 mt-8">
          <h4 className="text-gray-400 font-bold uppercase text-xs mb-4 flex items-center justify-center">
            <LayoutGrid className="w-4 h-4 mr-2" /> Accessible Applications During Lock
          </h4>

          {(() => {
            const detectedHardcoded = (installedApps || []).filter(app => !isAppBlacklisted(app.id) && isHardcodedApp(app) && !isHiddenSystemExemptApp(app.id, app.name));
            const hardcodedApps: AllowedApp[] = detectedHardcoded.length > 0 ? detectedHardcoded : DEFAULT_HARDCODED_APPS;

            const customApps = (settings.allowedApps || []).filter(app => 
              !isAppBlacklisted(app.id) && 
              !isHardcodedApp(app) &&
              !isHiddenSystemExemptApp(app.id, app.name) &&
              !hardcodedApps.some(h => h.id === app.id)
            );

            const unifiedLockApps: Array<AllowedApp & { isHardcoded: boolean }> = [
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

            const renderLockAppItem = (app: AllowedApp, isHardcoded: boolean) => {
              let FallbackIcon = iconMap[app.iconName] || LayoutGrid;
              if (isBrowserPackage(app.id)) FallbackIcon = Globe;
              else if (isMusicPackage(app.id, app.name)) FallbackIcon = Music;
              else if (isCameraPackage(app.id, app.name)) FallbackIcon = Camera;
              else if (isAuthenticatorPackage(app.id, app.name)) FallbackIcon = ShieldCheck;
              else if (isAiPackage(app.id, app.name)) FallbackIcon = Sparkles;
              else if (isNotesPackage(app.id, app.name)) FallbackIcon = FileText;
              else if (isStudentPackage(app.id, app.name)) FallbackIcon = BookOpen;
              else if (isMessagingPackage(app.id)) FallbackIcon = MessageSquare;

              return (
                <div 
                  key={app.id} 
                  className="flex flex-col items-center group relative w-full text-center" 
                  title={isHardcoded ? `${app.name} (Always Allowed by System)` : app.name}
                >
                  <div className="relative">
                    {isHardcoded && (
                      <div className="absolute -top-1.5 -right-1.5 bg-emerald-600 text-white rounded-full p-0.5 z-10 shadow-2xs">
                        <ShieldCheck className="w-3 h-3" />
                      </div>
                    )}
                    <div className={`w-14 h-14 sm:w-16 sm:h-16 rounded-2xl flex items-center justify-center shadow-md overflow-hidden ${
                      isHardcoded 
                        ? 'bg-emerald-950/60 border border-emerald-500/40 text-emerald-400' 
                        : 'bg-gray-800/80 border border-gray-700 text-gray-300'
                    }`}>
                      {app.iconBase64 ? (
                        <img src={`data:image/png;base64,${app.iconBase64}`} alt={app.name} className="w-full h-full object-cover p-1.5" />
                      ) : (
                        <FallbackIcon className={`w-7 h-7 ${isHardcoded ? 'text-emerald-400' : 'text-gray-300'}`} />
                      )}
                    </div>
                  </div>
                  <span className="text-[11px] sm:text-xs font-medium text-gray-300 w-full text-center truncate px-1 mt-1.5">{app.name}</span>
                  {isHardcoded && (
                    <span className="text-[9px] font-bold uppercase tracking-tight text-emerald-400 mt-0.5">
                      Always
                    </span>
                  )}
                </div>
              );
            };

            return (
              <div 
                className="max-h-72 sm:max-h-88 overflow-y-auto custom-scrollbar pr-1 w-full"
                style={{ WebkitOverflowScrolling: 'touch', overscrollBehavior: 'contain' }}
              >
                <div className="flex flex-col gap-y-6 max-w-sm sm:max-w-md mx-auto w-full py-2">
                  {customApps.length > 0 && (
                    <div className="grid grid-cols-4 gap-y-6 gap-x-3 sm:gap-x-4 w-full">
                      {customApps.map(app => renderLockAppItem(app, false))}
                    </div>
                  )}
                  <div className="grid grid-cols-4 gap-y-6 gap-x-3 sm:gap-x-4 w-full">
                    {hardcodedApps.map(app => renderLockAppItem(app, true))}
                  </div>
                </div>
              </div>
            );
          })()}
        </div>
      </div>

      {showAnswerPopup && (
        <div className="fixed inset-0 bg-black/80 backdrop-blur-md z-50 flex items-center justify-center p-4">
          <div className="bg-gray-900 border border-gray-700 rounded-3xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden flex flex-col">
            <div className="px-6 py-4 border-b border-gray-800 flex justify-between items-center">
              <h2 className="text-xl font-bold text-white flex items-center">
                <Sparkles className="w-5 h-5 mr-2 text-indigo-400" />
                AI Generated Answer
              </h2>
              <button onClick={() => setShowAnswerPopup(false)} className="text-gray-400 hover:text-white transition-colors">
                <X className="w-6 h-6" />
              </button>
            </div>
            <div className="px-6 py-3 bg-gray-950/60 border-b border-gray-800/80 flex flex-wrap items-center justify-between gap-3">
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold text-gray-400 uppercase tracking-wider">Format Category:</span>
                <select
                  value={selectedCategory}
                  onChange={(e) => {
                    const newCat = e.target.value;
                    setSelectedCategory(newCat);
                    handleGetAnswer(true, newCat);
                  }}
                  className="bg-gray-900 text-xs text-indigo-300 font-semibold px-3 py-1.5 rounded-lg border border-gray-700 focus:outline-none focus:border-indigo-500 cursor-pointer"
                >
                  <option value="auto">Auto-Detect (AI Decides)</option>
                  <option value="reflection">Reflection / Personal Essay</option>
                  <option value="coding">Coding / Computer Science Task</option>
                  <option value="math">Math / Physics / Engineering</option>
                  <option value="case_study">Case Study / Technical Analysis</option>
                  <option value="worksheets">Standard Q&A / Worksheets</option>
                </select>
              </div>
              <button 
                onClick={() => handleGetAnswer(true)}
                disabled={isGeneratingAnswer}
                className="px-3 py-1.5 bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-300 text-xs font-bold rounded-lg transition-colors flex items-center gap-1.5 disabled:opacity-50"
              >
                <RefreshCcw className={`w-3 h-3 ${isGeneratingAnswer ? 'animate-spin' : ''}`} />
                Regenerate
              </button>
            </div>
            <div className="p-6 overflow-y-auto font-mono text-sm leading-relaxed text-gray-300">
              {isGeneratingAnswer ? (
                <div className="flex flex-col items-center justify-center py-12 text-indigo-400">
                  <Loader2 className="w-8 h-8 mb-4 animate-spin" />
                  <p>Generating answer...</p>
                </div>
              ) : (
                <div className="whitespace-pre-wrap">{aiAnswer}</div>
              )}
            </div>
            <div className="px-6 py-4 border-t border-gray-800 bg-black/20 flex justify-end space-x-3">
              <button 
                onClick={() => handleGetAnswer(true)}
                disabled={isGeneratingAnswer}
                className="px-6 py-2 bg-indigo-600/20 hover:bg-indigo-600/40 text-indigo-400 font-bold rounded-xl transition-colors disabled:opacity-50"
              >
                Retry
              </button>
              <button 
                onClick={() => setShowAnswerPopup(false)}
                className="px-6 py-2 bg-gray-800 hover:bg-gray-700 text-white font-bold rounded-xl transition-colors"
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

import React, { useState, useRef, useEffect } from 'react';
import { Upload, Loader2, Sparkles, Pencil, ArrowRight, ArrowLeft, X, Clock, AlertTriangle, ShieldAlert, ShieldCheck, FileText, Check, Plus } from 'lucide-react';
import { ScheduleData, SavedResource } from '../types';
import { parseResource } from '../api/parseResource';
import { buildRubric } from '../api/buildRubric';
import { checkSimilarity } from '../api/checkSimilarity';
import { validateHomework } from '../api/validateHomework';

interface CreateScheduleProps {
  role: string;
  resources: SavedResource[];
  apiKey?: string;
  apiModel?: string;
  existingSchedules: ScheduleData[];
  settings: any;
  addLog: (action: string, details?: string) => void;
  onSave: (schedule: ScheduleData) => void;
  onAddResource?: (title: string, content: string, type?: 'lecture_notes' | 'case_study') => SavedResource;
  onCancel: () => void;
  showError: (msg: string) => void;
}

export function CreateSchedule({ role, resources, apiKey, apiModel, existingSchedules, settings, addLog, onSave, onAddResource, onCancel, showError }: CreateScheduleProps) {
  const [step, setStep] = useState<1 | 2 | 3 | 4>(1);
  const [selectedResourceIds, setSelectedResourceIds] = useState<string[]>([]);
  const [scheduleTitle, setScheduleTitle] = useState(() => localStorage.getItem('draft_scheduleTitle') || 'Homework Session');
  const [homeworkContent, setHomeworkContent] = useState(() => localStorage.getItem('draft_homeworkContent') || '');

  useEffect(() => {
    localStorage.setItem('draft_scheduleTitle', scheduleTitle);
  }, [scheduleTitle]);
  
  useEffect(() => {
    localStorage.setItem('draft_homeworkContent', homeworkContent);
  }, [homeworkContent]);
  const [rubricMode, setRubricMode] = useState<'ai' | 'manual'>('manual'); // 'ai' unused in UI
  const [rubricContent, setRubricContent] = useState('');
  const [activationTime, setActivationTime] = useState('19:00');
  const [durationMinutes, setDurationMinutes] = useState(60);
  
  const [isParsing, setIsParsing] = useState(false);
  const [ocrType, setOcrType] = useState<'simple' | 'formatted'>(settings?.defaultOcrType || 'simple');
  const [isCheckingSimilarity, setIsCheckingSimilarity] = useState(false);
  const [similarityError, setSimilarityError] = useState<string | null>(null);
  const [isGeneratingRubric, setIsGeneratingRubric] = useState(false);
  const [isRefiningRubric, setIsRefiningRubric] = useState(false);
  const [isValidating, setIsValidating] = useState(false);
  const [validationError, setValidationError] = useState<string | null>(null);
  const [aiAdvice, setAiAdvice] = useState<{ open: boolean; reason: string } | null>(null);
  const [activePopup, setActivePopup] = useState<'none' | 'user'>('none');
  const [tempRubric, setTempRubric] = useState('');
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [isCaseStudyDetected, setIsCaseStudyDetected] = useState(false);
  const [showAddCaseStudyModal, setShowAddCaseStudyModal] = useState(false);
  const [inlineCaseTitle, setInlineCaseTitle] = useState('');
  const [inlineCaseContent, setInlineCaseContent] = useState('');
  const [isDeclutteringCase, setIsDeclutteringCase] = useState(false);
  const [isParsingCaseFile, setIsParsingCaseFile] = useState(false);
  const caseFileInputRef = useRef<HTMLInputElement>(null);

  const handleCaseFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    if (file.size > 25 * 1024 * 1024) {
      showError('File is too large. Please upload a file smaller than 25MB.');
      if (caseFileInputRef.current) caseFileInputRef.current.value = '';
      return;
    }

    setIsParsingCaseFile(true);
    try {
      const data = await parseResource({
        file,
        type: 'transcription',
        apiKey: apiKey || '',
        apiModel: apiModel || 'gemini-2.0-flash',
        ocrType,
        simpleOcrKey: settings?.simpleOcrKey || '',
        formattedOcrKey: settings?.formattedOcrKey || '',
        customPrompts: settings?.prompts,
      });

      if (data.error) throw new Error(data.error);
      setInlineCaseContent(data.content);
      if (data.title && !inlineCaseTitle.trim()) setInlineCaseTitle(data.title);
    } catch (err: any) {
      showError(`Failed to parse document: ${err.message}`);
    } finally {
      setIsParsingCaseFile(false);
      if (caseFileInputRef.current) caseFileInputRef.current.value = '';
    }
  };

  const handleSaveInlineCaseStudy = async () => {
    if (!inlineCaseTitle.trim() && !inlineCaseContent.trim()) {
      showError('Please provide a title or content for the case study/article.');
      return;
    }

    setIsDeclutteringCase(true);
    let finalTitle = inlineCaseTitle;
    let finalContent = inlineCaseContent;

    try {
      if (apiKey) {
        const { declutterResource } = await import('../api/declutterResource');
        const data = await declutterResource({
          title: inlineCaseTitle,
          content: inlineCaseContent,
          apiKey: apiKey,
          apiModel: apiModel || 'gemini-2.0-flash',
          resourceType: 'case_study',
          customPrompts: settings?.prompts,
        });
        if (!inlineCaseTitle.trim() && data.title) finalTitle = data.title;
        if (data.content) finalContent = data.content;
      }
    } catch (err: any) {
      console.error('Error decluttering case study:', err);
    } finally {
      setIsDeclutteringCase(false);
    }

    if (onAddResource) {
      const savedRes = onAddResource(finalTitle || 'Untitled Case Study', finalContent, 'case_study');
      setSelectedResourceIds(prev => {
        const withoutOtherCases = prev.filter(id => {
          const res = resources.find(x => x.id === id);
          return res?.type !== 'case_study';
        });
        return [...withoutOtherCases, savedRes.id];
      });
      addLog('Saved & Selected Case Study', finalTitle);
    }
    setShowAddCaseStudyModal(false);
    setInlineCaseTitle('');
    setInlineCaseContent('');
    setValidationError(null);
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
      const data = await parseResource({
        file,
        type: 'homework',
        apiKey: apiKey || '',
        apiModel: apiModel || 'gemini-2.0-flash',
        ocrType,
        simpleOcrKey: settings?.simpleOcrKey || '',
        formattedOcrKey: settings?.formattedOcrKey || '',
        customPrompts: settings?.prompts,
      });

      if (data.error) throw new Error(data.error);
      setHomeworkContent(data.content);
      addLog('Uploaded Homework File', file.name);
      setValidationError(null);
      if (data.title) setScheduleTitle(data.title);
    } catch (err: any) {
      showError(`Failed to parse document: ${err.message}`);
      addLog('Error', `Parsing failed: ${err.message}`);
    } finally {
      setIsParsing(false);
      if (fileInputRef.current) fileInputRef.current.value = '';
    }
  };

  const handleNextStep1 = () => {
    if (selectedResourceIds.length === 0) {
      showError('Please select at least one resource for the AI to use.');
      return;
    }
    setStep(2);
  };

  const handleGenerateRubric = async () => {
    setIsGeneratingRubric(true);
    try {
      const selectedResources = resources.filter(r => selectedResourceIds.includes(r.id) && r.id !== 'ai-general-knowledge');
      let resourcesText = selectedResources.map(r => `=== ${r.title} ===\n${r.content}`).join('\n\n');
      if (selectedResourceIds.includes('ai-general-knowledge')) {
        resourcesText += '\n\n=== SYSTEM NOTE ===\nThe AI is authorized to use external general knowledge to complete this task.';
      }

      const rubric = await buildRubric({
        content: homeworkContent,
        resourcesText,
        userDraft: 'None',
        apiKey: apiKey || '',
        apiModel: apiModel || 'gemini-2.0-flash',
        customPrompts: settings?.prompts,
      });
      setRubricContent(rubric);
      addLog('Generated Grading Rubric');
    } catch (err: any) {
      showError(`Failed to generate rubric: ${err.message}`);
      addLog('Error', `Rubric generation failed: ${err.message}`);
    } finally {
      setIsGeneratingRubric(false);
    }
  };

  const handleRefineRubric = async () => {
    if (!tempRubric.trim()) {
      showError('Please enter a base rubric first.');
      return;
    }

    setIsRefiningRubric(true);
    try {
      let resourcesText = resources.filter(r => selectedResourceIds.includes(r.id) && r.id !== 'ai-general-knowledge').map(r => r.content).join('\n\n');
      if (selectedResourceIds.includes('ai-general-knowledge')) {
        resourcesText += '\n\n=== SYSTEM NOTE ===\nThe AI is authorized to use external general knowledge to complete this task.';
      }

      const rubric = await buildRubric({
        content: homeworkContent,
        resourcesText,
        userDraft: tempRubric,
        apiKey: apiKey || '',
        apiModel: apiModel || 'gemini-2.0-flash',
        customPrompts: settings?.prompts,
      });
      setRubricContent(rubric);
      setActivePopup('none');
      setTempRubric('');
    } catch (err: any) {
      showError(`Failed to refine rubric: ${err.message}`);
      addLog('Error', `Rubric refinement failed: ${err.message}`);
    } finally {
      setIsRefiningRubric(false);
    }
  };

  const handleNextStep2 = async () => {
    if (!homeworkContent.trim()) {
      showError('Please provide the homework content first.');
      return;
    }

    const existingHws = existingSchedules.filter(s => s.isActive).map(s => s.homeworkContent);
    if (existingHws.length > 0 && !similarityError) {
      setIsCheckingSimilarity(true);
      try {
        const simData = await checkSimilarity({
          newHomework: homeworkContent,
          existingHomeworks: existingHws,
          apiKey: apiKey || '',
          apiModel: apiModel || 'gemini-2.0-flash',
          customPrompts: settings?.prompts,
        });
        if (simData.similar) {
          setSimilarityError(`Looks like you already scheduled this homework task:\n${simData.reason}\n\nIf you want to schedule it anyway, edit the text slightly and try again.`);
          setIsCheckingSimilarity(false);
          return;
        }
      } catch (err: any) {
        console.error('Similarity check error', err);
      }
      setIsCheckingSimilarity(false);
    }

    setIsValidating(true);

    const hasAiGeneral = selectedResourceIds.includes('ai-general-knowledge');
    const specificResourceIds = selectedResourceIds.filter(id => id !== 'ai-general-knowledge');

    // Case 1: Both AI General Knowledge AND specific course resources are selected
    if (hasAiGeneral && specificResourceIds.length > 0) {
      try {
        const selectedResources = resources.filter(r => specificResourceIds.includes(r.id));
        const resourcesText = selectedResources.map(r => `=== ${r.title} ===\n${r.content}`).join('\n\n');

        const data = await validateHomework({
          content: homeworkContent,
          resourcesText,
          apiKey: apiKey || '',
          apiModel: apiModel || 'gemini-2.0-flash',
          customPrompts: settings?.prompts,
        });

        if (data.valid) {
          // Homework is completely answerable with the user's selected resources alone!
          setAiAdvice({
            open: true,
            reason: data.reason || 'This homework task appears to be fully answerable using your selected course notes without needing external AI knowledge.'
          });
          setIsValidating(false);
          return;
        } else {
          // Course resources alone are NOT sufficient, so AI General Knowledge is legitimately needed. Proceed smoothly!
          setIsValidating(false);
          setStep(3);
          return;
        }
      } catch (err: any) {
        console.error('Validation error with AI General Knowledge', err);
        // Fallback gracefully to Step 3 if validation API fails
        setIsValidating(false);
        setStep(3);
        return;
      }
    }

    // Case 2: Only AI General Knowledge is selected (no other course resources)
    if (hasAiGeneral && specificResourceIds.length === 0) {
      setIsValidating(false);
      setStep(3);
      return;
    }

    // Case 3: Specific course resources selected, but AI General Knowledge is OFF
    if (!hasAiGeneral && specificResourceIds.length > 0) {
      try {
        const selectedResources = resources.filter(r => specificResourceIds.includes(r.id));
        const resourcesText = selectedResources.map(r => `=== ${r.title} ===\n${r.content}`).join('\n\n');

        const data = await validateHomework({
          content: homeworkContent,
          resourcesText,
          apiKey: apiKey || '',
          apiModel: apiModel || 'gemini-2.0-flash',
          customPrompts: settings?.prompts,
        });

        if (data.isCaseStudy) {
          const hasCaseStudy = selectedResourceIds.some(id => {
            const r = resources.find(res => res.id === id);
            return r?.type === 'case_study';
          });
          if (!hasCaseStudy && !hasAiGeneral) {
            setIsCaseStudyDetected(true);
            setValidationError(data.reason || 'Case Study Detected: This assignment requires analyzing a real-world issue, news report, documentary, or article.');
            setIsValidating(false);
            return;
          }
        }

        if (!data.valid) {
          setValidationError(data.reason || 'Wait! Your homework task does not seem to be covered by the selected resources.');
          setIsValidating(false);
          return;
        }
      } catch (err: any) {
        console.error('Validation error', err);
        showError(`AI Validation Error: ${err.message || 'Unknown error. Check quota or API key.'}`);
        addLog('Error', `Validation failed: ${err.message}`);
        setIsValidating(false);
        return;
      }
    }

    setIsValidating(false);
    setStep(3);
  };

  const handleNextStep3 = () => {
    if (!rubricContent.trim()) {
      showError('Please define or generate the grading rubric first.');
      return;
    }
    setStep(4);
  };

  const handleSave = () => {
    const normalizeMinutes = (timeStr: string) => {
      const [h, m] = timeStr.split(':').map(Number);
      return (h < 12 ? h + 24 : h) * 60 + m;
    };

    const formatMinutesTo12Hour = (totalMinutes: number) => {
      let m = totalMinutes % (24 * 60);
      const h24 = Math.floor(m / 60);
      const mins = m % 60;
      const ampm = h24 >= 12 && h24 < 24 ? 'PM' : 'AM';
      const h12 = h24 % 12 || 12;
      return `${h12}:${mins.toString().padStart(2, '0')} ${ampm}`;
    };

    const finalDuration = Math.min(90, Math.max(1, durationMinutes));
    const proposedStart = normalizeMinutes(activationTime);
    const proposedEnd = proposedStart + finalDuration;

    for (const existing of existingSchedules) {
      if (!existing.isActive) continue;

      const exStart = normalizeMinutes(existing.activationTime);
      const exEnd = exStart + existing.durationMinutes;

      // Check if times overlap (including a 30 min buffer)
      if (proposedStart < exEnd + 30 && proposedEnd + 30 > exStart) {
        const overlapMsg = `Schedule overlaps with existing schedule at ${formatMinutesTo12Hour(exStart)} - ${formatMinutesTo12Hour(exEnd + 30)}.`;
        showError(overlapMsg);
        return;
      }
    }

    onSave({
      id: crypto.randomUUID(),
      title: scheduleTitle,
      homeworkContent,
      rubricMode,
      rubricContent,
      selectedResourceIds,
      activationTime,
      durationMinutes: finalDuration,
      isActive: true
    });
    localStorage.removeItem('draft_scheduleTitle');
    localStorage.removeItem('draft_homeworkContent');
  };

  return (
    <div className="max-w-3xl mx-auto w-full p-6">
      <div className="bg-white rounded-3xl p-8 border border-gray-200 shadow-sm min-h-[500px] flex flex-col">
        
        {/* Header */}
        <div className="flex justify-between items-center mb-6">
          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={() => {
                if (step > 1) {
                  setStep(s => (s - 1) as 1 | 2 | 3 | 4);
                } else {
                  onCancel();
                }
              }}
              className="flex items-center px-3 py-1.5 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-sm rounded-xl transition-colors shadow-2xs"
              title={step > 1 ? 'Previous Step' : 'Cancel to Dashboard'}
            >
              <ArrowLeft className="w-4 h-4 mr-1.5 text-gray-600" /> Back
            </button>
            <h2 className="text-xl sm:text-2xl font-black text-gray-900 uppercase">Create Schedule</h2>
          </div>
          <div className="flex items-center gap-3">
            <div className="flex space-x-2">
              {[1, 2, 3, 4].map(s => (
                <div key={s} className={`h-2 w-6 sm:w-8 rounded-full ${step >= s ? 'bg-red-500' : 'bg-gray-200'}`} />
              ))}
            </div>
            <button
              type="button"
              onClick={onCancel}
              className="p-1.5 hover:bg-gray-100 rounded-full text-gray-400 hover:text-gray-700 transition-colors"
              title="Cancel to Dashboard"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Step 1: Select Resources */}
        {step === 1 && (
          <div className="flex-1 flex flex-col">
            <h3 className="text-lg font-bold text-gray-800 mb-4">Step 1: Select Resources</h3>
            <p className="text-gray-500 text-sm mb-6">Which resources should the AI use to evaluate this homework?</p>
            
            <div className="space-y-3 mb-6 flex-1 overflow-y-auto">
              {(
                [
                  { id: 'ai-general-knowledge', title: '🌐 General AI Knowledge (Bypass validation)', content: 'Allows the AI to use its pre-trained general knowledge.' },
                  ...resources
                ].map(r => (
                  <label key={r.id} className="flex items-center p-4 border rounded-xl cursor-pointer hover:bg-gray-50 transition-colors">
                    <input 
                      type="checkbox" 
                      checked={selectedResourceIds.includes(r.id)}
                      onChange={(e) => {
                        let newSelection = [...selectedResourceIds];
                        if (e.target.checked) {
                          newSelection.push(r.id);
                        } else {
                          newSelection = newSelection.filter(id => id !== r.id);
                        }
                        
                        // Enforce the rule: If "ai-general-knowledge" is selected, max 1 other resource allowed (total 2).
                        const hasAI = newSelection.includes('ai-general-knowledge');
                        if (hasAI && newSelection.length > 2) {
                          showError("When using General AI Knowledge, you can only select ONE other resource to append data to.");
                          return;
                        }

                        setSelectedResourceIds(newSelection);
                        setValidationError(null);
                      }}
                      className="w-5 h-5 text-red-600 rounded border-gray-300 focus:ring-red-500"
                    />
                    <span className="ml-3 font-bold text-gray-700 flex items-center gap-2">
                      {r.title}
                      {(r as any).type === 'case_study' && (
                        <span className="text-[10px] font-bold text-indigo-600 bg-indigo-50 px-2 py-0.5 rounded border border-indigo-100 uppercase tracking-wider">
                          Case Study
                        </span>
                      )}
                    </span>
                  </label>
                ))
              )}
            </div>

            <div className="flex justify-end space-x-4">
              <button onClick={onCancel} className="px-6 py-3 font-bold text-gray-500 hover:bg-gray-100 rounded-xl">Cancel</button>
              <button onClick={handleNextStep1} className="px-6 py-3 font-bold text-white bg-gray-900 hover:bg-black rounded-xl flex items-center">
                Next <ArrowRight className="w-4 h-4 ml-2" />
              </button>
            </div>
          </div>
        )}

        {/* Step 2: Define Homework */}
        {step === 2 && (
          <div className="flex-1 flex flex-col">
            <h3 className="text-lg font-bold text-gray-800 mb-4">Step 2: Define the Homework</h3>
            <p className="text-gray-500 text-sm mb-4">What are you supposed to do? Upload your assignment to extract the requirements.</p>


            <div className="mb-4">
              <label className="block text-sm font-bold text-gray-700 mb-2">Session Title</label>
              <input 
                type="text" 
                value={scheduleTitle}
                onChange={(e) => setScheduleTitle(e.target.value)}
                placeholder="e.g., Math Homework, History Essay"
                className="w-full p-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-medium"
              />
            </div>

            
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

            <div className="mb-4">
              <input 
                type="file" 
                ref={fileInputRef} 
                onChange={handleFileUpload} 
                accept="text/plain, image/*, .docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                className="hidden" 
              />
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={isParsing}
                className="w-full py-4 border-2 border-dashed border-gray-300 rounded-xl flex items-center justify-center text-gray-500 hover:border-red-400 hover:text-red-500 transition-colors disabled:opacity-50 disabled:cursor-not-allowed bg-white"
              >
                {isParsing ? (
                  <Loader2 className="w-5 h-5 mr-2 animate-spin" />
                ) : (
                  <Upload className="w-5 h-5 mr-2" />
                )}
                {isParsing ? 'Parsing document...' : 'Upload Image / DOCX / TXT'}
              </button>
            </div>

            
            <div className="flex-1 w-full flex flex-col mb-6">
              <div className="flex justify-between items-center mb-2">
                <span className="text-xs font-bold text-gray-500 uppercase tracking-wider">Homework Content / Requirements:</span>
                <span className="text-xs text-indigo-600 font-medium">Editable</span>
              </div>
              <textarea
                value={homeworkContent}
                onChange={(e) => {
                  setHomeworkContent(e.target.value);
                  setValidationError(null);
                  setSimilarityError(null);
                }}
                placeholder="Upload an image/document above, or type/edit your homework questions and instructions here..."
                className="flex-1 w-full p-4 rounded-xl border border-gray-300 bg-gray-50 focus:bg-white focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono text-sm text-gray-800 leading-relaxed resize-y min-h-[160px]"
              />
            </div>


            {similarityError && (
              <div className="mb-6 p-4 bg-orange-50 border border-orange-200 rounded-xl text-orange-800 text-sm whitespace-pre-wrap flex items-start">
                <AlertTriangle className="w-5 h-5 flex-shrink-0 mr-3 mt-0.5" />
                <div>{similarityError}</div>
              </div>
            )}

            {isCaseStudyDetected && (
              <div className="mb-6 p-5 bg-indigo-50/80 border-2 border-indigo-300 rounded-2xl flex flex-col space-y-4">
                <div className="flex items-start">
                  <FileText className="w-6 h-6 flex-shrink-0 mr-3 mt-0.5 text-indigo-600" />
                  <div className="flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-black text-indigo-950 text-base">Case Study / Real-World Article Required</span>
                      <span className="text-[10px] uppercase tracking-wider bg-indigo-200/80 text-indigo-800 font-bold px-2 py-0.5 rounded-full">Required to Proceed</span>
                    </div>
                    <p className="mt-1 text-indigo-900 text-sm leading-relaxed">
                      {validationError || 'This milestone activity requires evaluating a real-world issue, news clip, documentary, or article. Please select which case study or article to use:'}
                    </p>
                  </div>
                </div>

                {/* List of Case Studies */}
                <div className="space-y-2 max-h-48 overflow-y-auto pr-1">
                  {resources.filter(r => r.type === 'case_study').length === 0 ? (
                    <div className="p-4 bg-white/80 border border-dashed border-indigo-200 rounded-xl text-center">
                      <p className="text-xs font-bold text-indigo-900">No saved case studies found in your library.</p>
                      <p className="text-[11px] text-gray-500 mt-0.5">Click "+ Add Case Study / Article" below to attach one, or let AI use General Knowledge.</p>
                    </div>
                  ) : (
                    resources.filter(r => r.type === 'case_study').map(r => {
                      const isSelected = selectedResourceIds.includes(r.id);
                      return (
                        <div
                          key={r.id}
                          onClick={() => {
                            setSelectedResourceIds(prev => {
                              const withoutOtherCases = prev.filter(id => {
                                const res = resources.find(x => x.id === id);
                                return res?.type !== 'case_study';
                              });
                              return isSelected ? withoutOtherCases : [...withoutOtherCases, r.id];
                            });
                            setValidationError(null);
                          }}
                          className={`p-3 rounded-xl border transition-all cursor-pointer flex items-center justify-between ${
                            isSelected 
                              ? 'bg-indigo-600 text-white border-indigo-600 shadow-xs' 
                              : 'bg-white hover:bg-indigo-50 border-indigo-200 text-gray-800'
                          }`}
                        >
                          <div className="flex items-center min-w-0 pr-2">
                            <FileText className={`w-4 h-4 mr-2.5 flex-shrink-0 ${isSelected ? 'text-white' : 'text-indigo-600'}`} />
                            <div className="min-w-0">
                              <span className="font-bold text-sm block truncate">{r.title}</span>
                              <span className={`text-[11px] block truncate ${isSelected ? 'text-indigo-100' : 'text-gray-500'}`}>
                                {r.content.slice(0, 90)}
                              </span>
                            </div>
                          </div>
                          <div className="flex-shrink-0">
                            {isSelected ? (
                              <Check className="w-5 h-5 text-white" />
                            ) : (
                              <div className="w-5 h-5 rounded-full border-2 border-indigo-300" />
                            )}
                          </div>
                        </div>
                      );
                    })
                  )}
                </div>

                {/* Actions */}
                <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-indigo-200/80">
                  <button
                    type="button"
                    onClick={() => setShowAddCaseStudyModal(true)}
                    className="px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold text-xs rounded-lg transition-colors flex items-center shadow-xs cursor-pointer active:scale-95"
                  >
                    <Plus className="w-3.5 h-3.5 mr-1.5" />
                    + Add Case Study / Article
                  </button>

                  <button
                    type="button"
                    onClick={() => {
                      setSelectedResourceIds(prev => Array.from(new Set([...prev, 'ai-general-knowledge'])));
                      setIsCaseStudyDetected(false);
                      setValidationError(null);
                      setStep(3);
                    }}
                    className="px-3.5 py-2 bg-white hover:bg-indigo-100 text-indigo-700 border border-indigo-300 font-bold text-xs rounded-lg transition-colors flex items-center cursor-pointer active:scale-95"
                  >
                    <Sparkles className="w-3.5 h-3.5 mr-1.5 text-indigo-600" />
                    Let AI Pick Case (General Knowledge)
                  </button>
                </div>
              </div>
            )}

            {validationError && !isCaseStudyDetected && (
              <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-xl text-red-700 text-sm whitespace-pre-wrap flex flex-col space-y-3">
                <div className="flex items-start">
                  <ShieldAlert className="w-5 h-5 flex-shrink-0 mr-3 mt-0.5 text-red-600" />
                  <div className="flex-1">
                    <span className="font-bold text-red-900">Missing Resource Coverage:</span>
                    <p className="mt-1 text-red-800 leading-relaxed">{validationError}</p>
                  </div>
                </div>
                <div className="flex flex-wrap items-center gap-2 pt-2 border-t border-red-200/80">
                  <button
                    type="button"
                    onClick={() => {
                      setSelectedResourceIds(prev => Array.from(new Set([...prev, 'ai-general-knowledge'])));
                      setValidationError(null);
                      setStep(3);
                    }}
                    className="px-3.5 py-2 bg-red-600 hover:bg-red-700 text-white font-bold text-xs rounded-lg transition-colors flex items-center shadow-sm cursor-pointer active:scale-95"
                  >
                    <Sparkles className="w-3.5 h-3.5 mr-1.5" />
                    Enable AI General Knowledge & Proceed
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setValidationError(null);
                      setStep(1);
                    }}
                    className="px-3.5 py-2 bg-white hover:bg-gray-100 text-gray-700 border border-gray-300 font-bold text-xs rounded-lg transition-colors cursor-pointer active:scale-95"
                  >
                    Back to Select Resources
                  </button>
                </div>
              </div>
            )}

            {(() => {
              const hasCaseStudyAttached = selectedResourceIds.some(id => {
                const r = resources.find(res => res.id === id);
                return r?.type === 'case_study';
              }) || selectedResourceIds.includes('ai-general-knowledge');

              const isNextDisabled = isValidating || isCheckingSimilarity || !!similarityError || (isCaseStudyDetected ? !hasCaseStudyAttached : !!validationError);

              return (
                <div className="flex items-center justify-between">
                  <div>
                    {isCaseStudyDetected && !hasCaseStudyAttached && (
                      <span className="text-xs text-indigo-600 font-bold">
                        Please select an article above to proceed
                      </span>
                    )}
                  </div>
                  <div className="flex space-x-4">
                    <button onClick={() => setStep(1)} className="px-6 py-3 font-bold text-gray-500 hover:bg-gray-100 rounded-xl">Back</button>
                    <button 
                      onClick={handleNextStep2} 
                      disabled={isNextDisabled} 
                      className={`px-6 py-3 font-bold rounded-xl flex items-center transition-all ${
                        isNextDisabled
                          ? 'bg-gray-300 text-gray-500 cursor-not-allowed' 
                          : 'text-white bg-gray-900 hover:bg-black'
                      }`}
                    >
                      {isValidating || isCheckingSimilarity ? <Loader2 className="w-5 h-5 mr-2 animate-spin" /> : null}
                      {isValidating ? 'Validating...' : isCheckingSimilarity ? 'Checking...' : 'Next'} <ArrowRight className="w-4 h-4 ml-2" />
                    </button>
                  </div>
                </div>
              );
            })()}
          </div>
        )}

        {/* Step 3: Rubric */}
        {step === 3 && (
          <div className="flex-1 flex flex-col relative">
            <h3 className="text-lg font-bold text-gray-800 mb-4">Step 3: Define Grading Rubric</h3>
            <p className="text-gray-500 text-sm mb-6">How should the AI evaluate your handwritten submission?</p>
            
            <div className="flex flex-col gap-3 mb-6">
              <button
                onClick={handleGenerateRubric}
                disabled={isGeneratingRubric}
                className="w-full p-4 rounded-xl border-2 border-red-200 bg-red-50 hover:bg-red-100 flex items-center justify-center transition-colors text-red-700 font-bold disabled:opacity-50"
              >
                {isGeneratingRubric ? <Loader2 className="w-5 h-5 mr-2 animate-spin" /> : <Sparkles className="w-5 h-5 mr-2" />}
                {isGeneratingRubric ? 'Generating...' : 'Let AI Generate Rubric'}
              </button>
              
              <button
                onClick={() => { setTempRubric(''); setActivePopup('user'); }}
                className="w-full p-4 rounded-xl border-2 border-gray-200 bg-gray-50 hover:bg-gray-100 flex items-center justify-center transition-colors text-gray-700 font-bold"
              >
                <Pencil className="w-5 h-5 mr-2" />
                User defined + AI Generated rubric
              </button>
            </div>
            
            {rubricContent && (
              <div className="mb-6 flex-1 flex flex-col min-h-[200px]">
                <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-2">Current Rubric</label>
                <div className="bg-gray-50 p-4 rounded-xl border border-gray-200 font-mono text-sm leading-relaxed text-gray-700 flex-1 overflow-y-auto whitespace-pre-wrap">
                  {rubricContent}
                </div>
              </div>
            )}

            <div className="flex justify-end space-x-4 mt-auto">
              <button onClick={() => setStep(2)} className="px-6 py-3 font-bold text-gray-500 hover:bg-gray-100 rounded-xl">Back</button>
              <button onClick={handleNextStep3} className="px-6 py-3 font-bold text-white bg-gray-900 hover:bg-black rounded-xl flex items-center">
                Next <ArrowRight className="w-4 h-4 ml-2" />
              </button>
            </div>

            {/* Popup Modal */}
            {activePopup !== 'none' && (
              <div className="absolute inset-0 z-10 bg-white/90 backdrop-blur-sm flex flex-col">
                <h3 className="text-lg font-bold text-gray-800 mb-2 mt-2">
                  Enter Your Rubric (AI will refine)
                </h3>
                <p className="text-gray-500 text-sm mb-4">Type your grading criteria here.</p>
                <textarea
                  className="flex-1 w-full p-4 border border-gray-300 rounded-xl resize-none font-mono text-sm mb-4 focus:ring-2 focus:ring-red-500"
                  placeholder="E.g. Must include 3 references, must be 1 page long..."
                  value={tempRubric}
                  onChange={e => setTempRubric(e.target.value)}
                />
                <div className="flex justify-end space-x-3 mb-2">
                  <button onClick={() => setActivePopup('none')} className="px-4 py-2 font-bold text-gray-500 hover:bg-gray-100 rounded-xl">Cancel</button>
                  <button 
                    onClick={handleRefineRubric}
                    disabled={isRefiningRubric}
                    className="px-6 py-2 font-bold text-white bg-red-600 hover:bg-red-700 rounded-xl flex items-center disabled:opacity-50"
                  >
                    {isRefiningRubric ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : null}
                    {isRefiningRubric ? 'Processing...' : 'Next'}
                  </button>
                </div>
              </div>
            )}
          </div>
        )}

        {/* Step 4: Timing */}
        {step === 4 && (
          <div className="flex-1 flex flex-col">
            <h3 className="text-lg font-bold text-gray-800 mb-4">Step 4: Schedule Lock</h3>
            <p className="text-gray-500 text-sm mb-8">When should the system lock you into study mode?</p>
            
            <div className="space-y-6 max-w-md mx-auto w-full">


              <div>
                <label className="block text-sm font-bold text-gray-700 mb-2 flex items-center">
                  <Clock className="w-4 h-4 mr-2" /> Activation Time
                </label>
                <input
                  type="time"
                  value={activationTime}
                  onChange={(e) => setActivationTime(e.target.value)}
                  className="w-full px-4 py-4 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 text-lg"
                />
                
                {(() => {
                  const isHardcore = settings?.operatingMode === 'hardcore';
                  const [h] = activationTime.split(':').map(Number);
                  const isTimeValid = isHardcore || (h >= 19 || h <= 3);
                  if (!isTimeValid && activationTime !== '') {
                    return (
                      <p className="text-sm text-red-500 mt-2 font-bold flex items-center bg-red-50 p-2 rounded-lg">
                        <AlertTriangle className="w-4 h-4 mr-2 flex-shrink-0" />
                        ERROR: You can only schedule locks between 7:00 PM and 3:00 AM in Safemode.
                      </p>
                    );
                  }
                  if (isHardcore) {
                    return <p className="text-xs text-red-600 font-bold mt-2">🔥 Hardcore 24/7 Mode: Can schedule at any time of day.</p>;
                  }
                  return <p className="text-xs text-gray-500 mt-2">Safemode: Must be between 7:00 PM and 3:00 AM</p>;
                })()}
              </div>

              <div>
                <div className="flex justify-between items-center mb-2">
                  <label className="block text-sm font-bold text-gray-700">Duration (Minutes)</label>
                  <span className="text-xs font-semibold text-gray-500">Max 90 mins</span>
                </div>
                <input
                  type="number"
                  min="1"
                  max="90"
                  value={durationMinutes}
                  onChange={(e) => {
                    const parsed = parseInt(e.target.value);
                    if (isNaN(parsed)) {
                      setDurationMinutes(1);
                    } else {
                      setDurationMinutes(Math.min(90, Math.max(1, parsed)));
                    }
                  }}
                  className="w-full px-4 py-4 rounded-xl border border-gray-300 focus:ring-2 focus:ring-red-500 text-lg"
                />
              </div>
            </div>

            <div className="flex justify-end space-x-4 mt-auto pt-8">
              <button onClick={() => setStep(3)} className="px-6 py-3 font-bold text-gray-500 hover:bg-gray-100 rounded-xl">Back</button>
              
              {(() => {
                const isHardcore = settings?.operatingMode === 'hardcore';
                const [h] = activationTime.split(':').map(Number);
                const isTimeValid = isHardcore || (h >= 19 || h <= 3);
                return (
                  <button 
                    onClick={handleSave} 
                    disabled={!isTimeValid}
                    className="px-8 py-3 font-bold text-white bg-red-600 hover:bg-red-700 rounded-xl shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                  >
                    Save & Activate Schedule
                  </button>
                );
              })()}
            </div>
          </div>
        )}
        {aiAdvice && aiAdvice.open && (
          <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-lg w-full shadow-2xl border border-gray-100 flex flex-col animate-in fade-in zoom-in-95 duration-200">
              <div className="w-12 h-12 bg-amber-100 text-amber-600 rounded-2xl flex items-center justify-center mb-4">
                <Sparkles className="w-6 h-6" />
              </div>
              
              <h3 className="text-xl font-black text-gray-900 mb-2">
                AI General Knowledge May Not Be Needed
              </h3>
              
              <p className="text-sm text-gray-600 mb-4 leading-relaxed">
                {aiAdvice.reason}
              </p>

              <div className="p-3.5 bg-amber-50 rounded-xl border border-amber-200 text-xs text-amber-900 leading-relaxed mb-6 flex items-start">
                <ShieldCheck className="w-4 h-4 text-amber-700 flex-shrink-0 mr-2 mt-0.5" />
                <span>
                  <strong>Tip:</strong> Turning off AI General Knowledge keeps your resource flashcards clean and prevents assignment-specific details (like formulas or weights) from being harvested into your permanent notes.
                </span>
              </div>

              <div className="flex flex-col sm:flex-row gap-3">
                <button
                  type="button"
                  onClick={() => {
                    // Turn off AI General Knowledge
                    setSelectedResourceIds(prev => prev.filter(id => id !== 'ai-general-knowledge'));
                    setAiAdvice(null);
                    setStep(3);
                  }}
                  className="flex-1 py-3 px-4 bg-gray-900 hover:bg-black text-white font-bold text-sm rounded-xl transition-all shadow-md active:scale-95 cursor-pointer text-center"
                >
                  Turn Off AI Knowledge & Continue
                </button>
                <button
                  type="button"
                  onClick={() => {
                    // Keep AI General Knowledge
                    setAiAdvice(null);
                    setStep(3);
                  }}
                  className="py-3 px-4 bg-gray-100 hover:bg-gray-200 text-gray-700 font-bold text-sm rounded-xl transition-all active:scale-95 cursor-pointer text-center"
                >
                  Keep Enabled Anyway
                </button>
              </div>
            </div>
          </div>
        )}

        {/* Add Case Study Modal */}
        {showAddCaseStudyModal && (
          <div className="fixed inset-0 bg-gray-900/60 backdrop-blur-xs z-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-3xl p-6 sm:p-8 max-w-xl w-full shadow-2xl border border-gray-100 flex flex-col max-h-[90vh]">
              <div className="flex justify-between items-center mb-4">
                <div className="flex items-center gap-2">
                  <FileText className="w-6 h-6 text-indigo-600" />
                  <h3 className="text-xl font-black text-gray-900">Add Case Study / Article</h3>
                </div>
                <button
                  type="button"
                  onClick={() => setShowAddCaseStudyModal(false)}
                  className="p-1 text-gray-400 hover:text-gray-700 rounded-full hover:bg-gray-100 cursor-pointer"
                >
                  <X className="w-5 h-5" />
                </button>
              </div>

              <p className="text-xs text-gray-500 mb-4">
                Upload or paste the external article, news clip transcript, documentary summary, or thesis. It will be decluttered using the <strong>Article Declutter</strong> prompt.
              </p>

              <div className="space-y-4 flex-1 overflow-y-auto pr-1">
                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">Article / Case Title</label>
                  <input
                    type="text"
                    value={inlineCaseTitle}
                    onChange={(e) => setInlineCaseTitle(e.target.value)}
                    placeholder="e.g. Documentary: The Social Dilemma / 2024 CrowdStrike Outage"
                    className="w-full p-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 text-sm font-medium"
                  />
                </div>

                <div>
                  <input
                    type="file"
                    ref={caseFileInputRef}
                    onChange={handleCaseFileUpload}
                    accept="text/plain, image/*, .docx, application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                    className="hidden"
                  />
                  <button
                    type="button"
                    onClick={() => caseFileInputRef.current?.click()}
                    disabled={isParsingCaseFile}
                    className="w-full py-3 border-2 border-dashed border-indigo-200 hover:border-indigo-400 rounded-xl flex items-center justify-center text-xs font-bold text-indigo-600 bg-indigo-50/50 hover:bg-indigo-50 transition-colors cursor-pointer"
                  >
                    {isParsingCaseFile ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Upload className="w-4 h-4 mr-2" />}
                    {isParsingCaseFile ? 'Parsing file...' : 'Upload Article File (DOCX, TXT, Image)'}
                  </button>
                </div>

                <div>
                  <label className="block text-xs font-bold text-gray-700 uppercase tracking-wider mb-1">Content / Transcript / Summary</label>
                  <textarea
                    value={inlineCaseContent}
                    onChange={(e) => setInlineCaseContent(e.target.value)}
                    placeholder="Paste the news story, case facts, documentary summary, or research paper details here..."
                    className="w-full min-h-[160px] p-3 rounded-xl border border-gray-300 focus:outline-none focus:ring-2 focus:ring-indigo-500 font-mono text-xs text-gray-800 resize-y"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-gray-100 mt-4">
                <button
                  type="button"
                  onClick={() => setShowAddCaseStudyModal(false)}
                  className="px-4 py-2 text-gray-600 font-bold hover:bg-gray-100 rounded-xl text-sm cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  type="button"
                  onClick={handleSaveInlineCaseStudy}
                  disabled={isDeclutteringCase || (!inlineCaseTitle.trim() && !inlineCaseContent.trim())}
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white font-bold rounded-xl text-sm flex items-center disabled:opacity-50 cursor-pointer active:scale-95"
                >
                  {isDeclutteringCase ? <Loader2 className="w-4 h-4 mr-2 animate-spin" /> : <Sparkles className="w-4 h-4 mr-2" />}
                  {isDeclutteringCase ? 'Decluttering...' : 'Save & Attach Article'}
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

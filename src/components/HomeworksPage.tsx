import React, { useState, useMemo } from 'react';
import { 
  FileText, 
  X, 
  ChevronRight, 
  CheckCircle, 
  XCircle, 
  ArrowLeft, 
  RotateCcw, 
  Clock, 
  AlertTriangle, 
  ArrowRight, 
  Sparkles,
  Check
} from 'lucide-react';
import { CompletedHomework, ScheduleData } from '../types';

export interface HomeworksPageProps {
  homeworks: CompletedHomework[];
  schedules?: ScheduleData[];
  onBack: () => void;
  onClear: () => void;
  onReschedule?: (updatedSchedules: ScheduleData[]) => void;
}

// Normalizes time string ("HH:mm") into continuous minutes with 19:00 (7 PM) as night clock baseline
export const normalizeMinutes = (timeStr: string): number => {
  const [h, m] = (timeStr || '00:00').split(':').map(Number);
  return (h < 12 ? h + 24 : h) * 60 + m;
};

// Converts continuous normalized minutes back to 24-hour "HH:mm"
export const minutesToTimeString = (totalMinutes: number): string => {
  let m = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60);
  const h24 = Math.floor(m / 60);
  const mins = m % 60;
  return `${h24.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}`;
};

// Formats normalized minutes to friendly 12-hour AM/PM string
export const formatMinutesTo12Hour = (totalMinutes: number): string => {
  let m = ((totalMinutes % (24 * 60)) + (24 * 60)) % (24 * 60);
  const h24 = Math.floor(m / 60);
  const mins = m % 60;
  const ampm = h24 >= 12 && h24 < 24 ? 'PM' : 'AM';
  const h12 = h24 % 12 || 12;
  return `${h12}:${mins.toString().padStart(2, '0')} ${ampm}`;
};

export interface CascadedItem {
  schedule: ScheduleData;
  start: number;
  duration: number;
  end: number;
  isEmergency: boolean;
  isShifted: boolean;
  originalStart?: number;
}

export interface CascadeResult {
  chain: CascadedItem[];
  valid: boolean;
  error?: string;
  finalSchedules: ScheduleData[];
}

export function calculateCascadeSchedules(
  existingActiveSchedules: ScheduleData[],
  emergencyItem: {
    title: string;
    homeworkContent: string;
    rubricContent: string;
    selectedResourceIds?: string[];
    activationTime: string; // "HH:mm"
    durationMinutes: number;
  }
): CascadeResult {
  const targetStart = normalizeMinutes(emergencyItem.activationTime);
  const targetDuration = Math.min(90, Math.max(1, emergencyItem.durationMinutes));

  // Sort existing active schedules by normalized start time
  const sorted = [...existingActiveSchedules].sort(
    (a, b) => normalizeMinutes(a.activationTime) - normalizeMinutes(b.activationTime)
  );

  const emergencySchedule: ScheduleData = {
    id: crypto.randomUUID(),
    title: emergencyItem.title,
    homeworkContent: emergencyItem.homeworkContent,
    rubricMode: 'points',
    rubricContent: emergencyItem.rubricContent,
    selectedResourceIds: emergencyItem.selectedResourceIds || [],
    activationTime: emergencyItem.activationTime,
    durationMinutes: targetDuration,
    isActive: true,
  };

  // Check if targetStart falls inside an existing schedule
  let conflictIndex = -1;
  for (let i = 0; i < sorted.length; i++) {
    const sStart = normalizeMinutes(sorted[i].activationTime);
    const sEnd = sStart + sorted[i].durationMinutes;
    if (targetStart >= sStart && targetStart < sEnd) {
      conflictIndex = i;
      break;
    }
  }

  let rawChain: Array<{
    schedule: ScheduleData;
    start: number;
    duration: number;
    isEmergency: boolean;
    originalStart: number;
  }> = [];

  if (conflictIndex !== -1) {
    const conflicting = sorted[conflictIndex];
    const cStart = normalizeMinutes(conflicting.activationTime);
    const cDuration = conflicting.durationMinutes;
    const cMidpoint = cStart + Math.floor(cDuration / 2);

    if (targetStart < cMidpoint) {
      // Below median (< 50%): Emergency preempts conflicting schedule!
      for (let i = 0; i < conflictIndex; i++) {
        const start = normalizeMinutes(sorted[i].activationTime);
        rawChain.push({
          schedule: sorted[i],
          start,
          duration: sorted[i].durationMinutes,
          isEmergency: false,
          originalStart: start
        });
      }

      // Emergency takes requested targetStart
      rawChain.push({
        schedule: emergencySchedule,
        start: targetStart,
        duration: targetDuration,
        isEmergency: true,
        originalStart: targetStart
      });

      // Conflicting schedule is moved to start after emergency + 30m buffer
      const confNewStart = targetStart + targetDuration + 30;
      rawChain.push({
        schedule: conflicting,
        start: confNewStart,
        duration: cDuration,
        isEmergency: false,
        originalStart: cStart
      });

      // Subsequent schedules
      for (let i = conflictIndex + 1; i < sorted.length; i++) {
        const start = normalizeMinutes(sorted[i].activationTime);
        rawChain.push({
          schedule: sorted[i],
          start,
          duration: sorted[i].durationMinutes,
          isEmergency: false,
          originalStart: start
        });
      }
    } else {
      // At or above median (>= 50%): Conflicting schedule finishes first!
      for (let i = 0; i <= conflictIndex; i++) {
        const start = normalizeMinutes(sorted[i].activationTime);
        rawChain.push({
          schedule: sorted[i],
          start,
          duration: sorted[i].durationMinutes,
          isEmergency: false,
          originalStart: start
        });
      }

      // Emergency placed immediately after conflicting schedule + 30m buffer
      const confEnd = cStart + cDuration;
      const emNewStart = confEnd + 30;
      rawChain.push({
        schedule: emergencySchedule,
        start: emNewStart,
        duration: targetDuration,
        isEmergency: true,
        originalStart: targetStart
      });

      // Subsequent schedules
      for (let i = conflictIndex + 1; i < sorted.length; i++) {
        const start = normalizeMinutes(sorted[i].activationTime);
        rawChain.push({
          schedule: sorted[i],
          start,
          duration: sorted[i].durationMinutes,
          isEmergency: false,
          originalStart: start
        });
      }
    }
  } else {
    // No direct conflict with interior of an active schedule.
    // Insert emergency into sorted list based on targetStart:
    let inserted = false;
    for (let i = 0; i < sorted.length; i++) {
      const sStart = normalizeMinutes(sorted[i].activationTime);
      if (!inserted && targetStart < sStart) {
        rawChain.push({
          schedule: emergencySchedule,
          start: targetStart,
          duration: targetDuration,
          isEmergency: true,
          originalStart: targetStart
        });
        inserted = true;
      }
      rawChain.push({
        schedule: sorted[i],
        start: sStart,
        duration: sorted[i].durationMinutes,
        isEmergency: false,
        originalStart: sStart
      });
    }
    if (!inserted) {
      rawChain.push({
        schedule: emergencySchedule,
        start: targetStart,
        duration: targetDuration,
        isEmergency: true,
        originalStart: targetStart
      });
    }
  }

  // Ripple Pass: Enforce 30-minute buffer interval between every consecutive schedule
  for (let i = 1; i < rawChain.length; i++) {
    const prev = rawChain[i - 1];
    const minStart = prev.start + prev.duration + 30;
    if (rawChain[i].start < minStart) {
      rawChain[i].start = minStart;
    }
  }

  // Build final cascaded items
  const chain: CascadedItem[] = rawChain.map(item => ({
    schedule: {
      ...item.schedule,
      activationTime: minutesToTimeString(item.start),
      durationMinutes: item.duration,
      isActive: true
    },
    start: item.start,
    duration: item.duration,
    end: item.start + item.duration,
    isEmergency: item.isEmergency,
    isShifted: !item.isEmergency && item.start !== item.originalStart,
    originalStart: item.originalStart
  }));

  const finalSchedules: ScheduleData[] = chain.map(c => c.schedule);

  // Check 3:00 AM Curfew (1620 continuous normalized minutes)
  // Rule: No 30m buffer required after the last schedule
  const lastItem = chain[chain.length - 1];
  const lastEnd = lastItem ? lastItem.end : 0;

  if (lastEnd > 1620) {
    return {
      chain,
      valid: false,
      error: `Too many schedules / exceeds 3:00 AM curfew (ends at ${formatMinutesTo12Hour(lastEnd)})`,
      finalSchedules
    };
  }

  return {
    chain,
    valid: true,
    finalSchedules
  };
}

export function HomeworksPage({ homeworks, schedules = [], onBack, onClear, onReschedule }: HomeworksPageProps) {
  const [selectedHomework, setSelectedHomework] = useState<CompletedHomework | null>(null);
  const [rescheduleTarget, setRescheduleTarget] = useState<CompletedHomework | null>(null);
  const [rescheduleTime, setRescheduleTime] = useState<string>('19:10');
  const [rescheduleDuration, setRescheduleDuration] = useState<number>(25);
  const [rescheduleTitle, setRescheduleTitle] = useState<string>('');
  const [successToast, setSuccessToast] = useState<string | null>(null);

  React.useEffect(() => {
    const handleBack = (e: Event) => {
      if (rescheduleTarget) {
        e.preventDefault();
        setRescheduleTarget(null);
        return;
      }
      if (selectedHomework) {
        e.preventDefault();
        setSelectedHomework(null);
        return;
      }
    };
    window.addEventListener('qiezka-back-press', handleBack);
    return () => window.removeEventListener('qiezka-back-press', handleBack);
  }, [rescheduleTarget, selectedHomework]);

  const activeExistingSchedules = useMemo(() => {
    return (schedules || []).filter(s => s.isActive);
  }, [schedules]);

  const cascadeSimulation = useMemo(() => {
    if (!rescheduleTarget) return null;
    return calculateCascadeSchedules(activeExistingSchedules, {
      title: rescheduleTitle || rescheduleTarget.title,
      homeworkContent: rescheduleTarget.homeworkContent,
      rubricContent: rescheduleTarget.rubricContent,
      selectedResourceIds: rescheduleTarget.selectedResourceIds || [],
      activationTime: rescheduleTime,
      durationMinutes: rescheduleDuration,
    });
  }, [rescheduleTarget, rescheduleTime, rescheduleDuration, rescheduleTitle, activeExistingSchedules]);

  const handleOpenReschedule = (hw: CompletedHomework) => {
    // Determine a smart initial start time (e.g. 19:10 or next 10-min slot)
    const now = new Date();
    const currentH = now.getHours();
    const currentM = Math.ceil(now.getMinutes() / 5) * 5;
    const smartTime = `${currentH.toString().padStart(2, '0')}:${currentM.toString().padStart(2, '0')}`;

    setRescheduleTarget(hw);
    setRescheduleTitle(`${hw.title} (Retry)`);
    setRescheduleTime(currentH >= 19 || currentH < 3 ? smartTime : '19:10');
    setRescheduleDuration(hw.durationMinutes || 25);
  };

  const handleConfirmReschedule = () => {
    if (!cascadeSimulation || !cascadeSimulation.valid || !onReschedule) return;

    onReschedule(cascadeSimulation.finalSchedules);
    const targetTitle = rescheduleTitle || rescheduleTarget?.title;
    setRescheduleTarget(null);
    setSuccessToast(`Successfully cascaded ${cascadeSimulation.finalSchedules.length} schedules with "${targetTitle}"`);
    setTimeout(() => setSuccessToast(null), 4000);
  };

  return (
    <div className="flex-1 flex items-center justify-center p-4 h-full relative">
      {successToast && (
        <div className="fixed top-6 left-1/2 -translate-x-1/2 z-[100] bg-emerald-700 text-white px-5 py-3 rounded-2xl shadow-2xl flex items-center gap-2.5 font-bold text-sm border border-emerald-500 animate-in fade-in slide-in-from-top duration-200">
          <Check className="w-5 h-5 text-emerald-200" />
          <span>{successToast}</span>
        </div>
      )}

      <div className="bg-white rounded-3xl w-full max-w-4xl max-h-[90vh] shadow-2xl overflow-hidden flex flex-col">
        <div className="p-6 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
          <h2 className="text-xl font-bold flex items-center text-gray-800">
            {selectedHomework ? (
              <button 
                onClick={() => setSelectedHomework(null)}
                className="flex items-center text-gray-500 hover:text-gray-800 transition-colors mr-3"
              >
                <ArrowLeft className="w-5 h-5 mr-1" /> Back
              </button>
            ) : (
              <><FileText className="w-6 h-6 mr-2 text-indigo-500" /> Homeworks Log</>
            )}
          </h2>
          <div className="flex gap-2">
            {!selectedHomework && homeworks.length > 0 && (
              <button onClick={onClear} className="px-4 py-2 text-sm font-bold text-red-600 hover:bg-red-50 rounded-xl transition-colors flex items-center border border-red-100"> 
                 Clear All
              </button>
            )}
            <button onClick={onBack} className="p-2 bg-gray-100 hover:bg-gray-200 rounded-full transition-colors text-gray-500">
              <X className="w-6 h-6" />
            </button>
          </div>
        </div>

        <div className="flex-1 overflow-y-auto bg-gray-50 p-6">
          {!selectedHomework ? (
            <div className="space-y-3">
              {homeworks.length === 0 ? (
                <div className="text-center text-gray-500 py-12 flex flex-col items-center">
                  <FileText className="w-12 h-12 text-gray-300 mb-3" />
                  <p>No homework logs yet.</p>
                </div>
              ) : (
                homeworks.map(hw => (
                  <div 
                    key={hw.id}
                    onClick={() => setSelectedHomework(hw)}
                    className="w-full flex items-center justify-between p-4 border border-gray-200 rounded-xl bg-white hover:border-indigo-300 hover:shadow-sm transition-all text-left group cursor-pointer"
                  >
                    <div className="flex items-center min-w-0 pr-4">
                      {hw.passed ? (
                        <CheckCircle className="w-5 h-5 text-emerald-500 mr-3 shrink-0" />
                      ) : (
                        <XCircle className="w-5 h-5 text-red-500 mr-3 shrink-0" />
                      )}
                      <div className="min-w-0">
                        <div className="flex items-center gap-2 flex-wrap">
                          <h3 className="font-bold text-gray-900 truncate">{hw.title}</h3>
                          {!hw.passed && (
                            <span className="text-[10px] uppercase font-bold bg-red-100 text-red-700 px-2 py-0.5 rounded-md shrink-0">
                              Failed / Expired
                            </span>
                          )}
                        </div>
                        <p className="text-xs text-gray-400 mt-1">
                          {new Date(hw.timestamp).toLocaleDateString()} at {new Date(hw.timestamp).toLocaleTimeString()}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 shrink-0">
                      {!hw.passed && (
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            handleOpenReschedule(hw);
                          }}
                          className="px-3 py-1.5 bg-red-50 hover:bg-red-100 text-red-700 font-bold text-xs rounded-lg border border-red-200 transition-colors flex items-center gap-1 shadow-2xs"
                        >
                          <RotateCcw className="w-3.5 h-3.5" />
                          Add Again
                        </button>
                      )}
                      <ChevronRight className="w-5 h-5 text-gray-300 group-hover:text-indigo-500 transition-colors" />
                    </div>
                  </div>
                ))
              )}
            </div>
          ) : (
            <div className="space-y-6 max-w-3xl mx-auto">
              <div className="bg-white p-6 rounded-2xl border border-gray-200 shadow-sm">
                <div className="flex items-center justify-between mb-4 flex-wrap gap-2">
                  <h3 className="font-black text-xl text-gray-900">{selectedHomework.title}</h3>
                  <div className="flex items-center gap-2">
                    <span className={`px-3 py-1 text-xs font-bold rounded-lg ${selectedHomework.passed ? 'bg-emerald-100 text-emerald-700' : 'bg-red-100 text-red-700'}`}>
                      {selectedHomework.passed ? 'PASSED' : 'FAILED / EXPIRED'}
                    </span>
                    {!selectedHomework.passed && (
                      <button
                        onClick={() => handleOpenReschedule(selectedHomework)}
                        className="px-3.5 py-1.5 bg-red-600 hover:bg-red-500 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center gap-1.5"
                      >
                        <RotateCcw className="w-3.5 h-3.5" />
                        Add Again (Reschedule)
                      </button>
                    )}
                  </div>
                </div>
                
                <div className="mb-6">
                  <h4 className="text-sm font-bold text-gray-500 uppercase tracking-wider mb-2">Original Assignment</h4>
                  <div className="p-4 bg-gray-50 rounded-xl border border-gray-100 whitespace-pre-wrap text-gray-700 font-serif text-sm">
                    {selectedHomework.homeworkContent}
                  </div>
                </div>

                <div className="mb-6">
                  <h4 className="text-sm font-bold text-gray-500 uppercase tracking-wider mb-2">Rubric Used</h4>
                  <div className="p-4 bg-gray-50 rounded-xl border border-gray-100 whitespace-pre-wrap text-gray-700 text-sm">
                    {selectedHomework.rubricContent}
                  </div>
                </div>

                <div className="mb-6">
                  <h4 className="text-sm font-bold text-gray-500 uppercase tracking-wider mb-2">Submitted Work (Transcript)</h4>
                  <div className="p-4 bg-blue-50 rounded-xl border border-blue-100 whitespace-pre-wrap text-gray-800 font-serif leading-relaxed">
                    {selectedHomework.transcribedText}
                  </div>
                </div>

                <div className="mb-6">
                  <h4 className="text-sm font-bold text-gray-500 uppercase tracking-wider mb-2">AI Evaluation Feedback</h4>
                  <div className={`p-4 rounded-xl border whitespace-pre-wrap text-sm ${selectedHomework.passed ? 'bg-emerald-50 border-emerald-100 text-emerald-900' : 'bg-red-50 border-red-100 text-red-900'}`}>
                    {selectedHomework.feedback}
                  </div>
                </div>

                {!selectedHomework.passed && (
                  <div className="p-4 bg-red-50/80 border border-red-200 rounded-xl flex items-center justify-between flex-wrap gap-3">
                    <div>
                      <h4 className="text-sm font-bold text-red-900">Need another try at this assignment?</h4>
                      <p className="text-xs text-red-700 mt-0.5">
                        Click "Add Again" to schedule an emergency retake session. Other schedules will automatically cascade forward.
                      </p>
                    </div>
                    <button
                      onClick={() => handleOpenReschedule(selectedHomework)}
                      className="px-4 py-2 bg-red-600 hover:bg-red-500 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center gap-1.5 shrink-0"
                    >
                      <RotateCcw className="w-4 h-4" />
                      Add Again (Reschedule)
                    </button>
                  </div>
                )}
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Emergency Cascade Reschedule Modal */}
      {rescheduleTarget && cascadeSimulation && (
        <div className="fixed inset-0 bg-black/75 backdrop-blur-sm z-[999] flex items-center justify-center p-4 overflow-y-auto">
          <div className="bg-white rounded-3xl shadow-2xl w-full max-w-2xl overflow-hidden border border-gray-100 flex flex-col my-auto max-h-[92vh]">
            <div className="p-5 border-b border-gray-100 flex justify-between items-center bg-gray-50/80">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-xl bg-red-100 text-red-600 flex items-center justify-center">
                  <RotateCcw className="w-5 h-5" />
                </div>
                <div>
                  <h3 className="font-bold text-gray-900 text-base">Add Again: Cascade Reschedule</h3>
                  <p className="text-xs text-gray-500">Non-overlapping ripple scheduling with 30-min break buffers</p>
                </div>
              </div>
              <button 
                onClick={() => setRescheduleTarget(null)}
                className="p-2 hover:bg-gray-200 rounded-full text-gray-400 hover:text-gray-700 transition-colors"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6 overflow-y-auto space-y-5">
              {/* Session Configuration */}
              <div className="bg-gray-50 p-4 rounded-2xl border border-gray-200/80 space-y-4">
                <div>
                  <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-1">
                    Schedule Title
                  </label>
                  <input 
                    type="text"
                    value={rescheduleTitle}
                    onChange={(e) => setRescheduleTitle(e.target.value)}
                    className="w-full px-3.5 py-2 bg-white border border-gray-300 rounded-xl text-sm font-semibold text-gray-800 focus:outline-none focus:border-red-500"
                    placeholder="Enter session title"
                  />
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider mb-1 flex items-center gap-1.5">
                      <Clock className="w-3.5 h-3.5 text-red-500" />
                      Activation Start Time
                    </label>
                    <input 
                      type="time"
                      value={rescheduleTime}
                      onChange={(e) => setRescheduleTime(e.target.value)}
                      className="w-full px-3.5 py-2 bg-white border border-gray-300 rounded-xl text-sm font-mono font-bold text-gray-800 focus:outline-none focus:border-red-500"
                    />
                  </div>

                  <div>
                    <div className="flex justify-between items-center mb-1">
                      <label className="block text-xs font-bold text-gray-500 uppercase tracking-wider">
                        Duration: {rescheduleDuration} min
                      </label>
                      <span className="text-[11px] text-gray-400 font-mono">(1 - 90 min)</span>
                    </div>
                    <input 
                      type="range"
                      min="1"
                      max="90"
                      value={rescheduleDuration}
                      onChange={(e) => setRescheduleDuration(Number(e.target.value))}
                      className="w-full accent-red-600 cursor-pointer"
                    />
                    <div className="flex gap-1.5 mt-2">
                      {[15, 25, 40, 60, 90].map(mins => (
                        <button
                          key={mins}
                          type="button"
                          onClick={() => setRescheduleDuration(mins)}
                          className={`text-[11px] px-2 py-0.5 font-bold rounded-md border transition-colors ${
                            rescheduleDuration === mins 
                              ? 'bg-red-600 text-white border-red-600' 
                              : 'bg-white text-gray-600 border-gray-200 hover:bg-gray-100'
                          }`}
                        >
                          {mins}m
                        </button>
                      ))}
                    </div>
                  </div>
                </div>
              </div>

              {/* Cascade Shift Simulation Preview */}
              <div>
                <div className="flex items-center justify-between mb-2">
                  <h4 className="text-xs font-bold text-gray-700 uppercase tracking-wider flex items-center gap-1.5">
                    <Sparkles className="w-3.5 h-3.5 text-indigo-500" />
                    Cascaded Schedule Ripple Preview
                  </h4>
                  <span className="text-[11px] font-semibold text-gray-500">
                    {cascadeSimulation.chain.length} Active Session{cascadeSimulation.chain.length !== 1 ? 's' : ''}
                  </span>
                </div>

                <div className="space-y-2 border border-gray-200 rounded-2xl p-3 bg-gray-50/50">
                  {cascadeSimulation.chain.map((item, index) => {
                    const isLast = index === cascadeSimulation.chain.length - 1;
                    return (
                      <div key={item.schedule.id + index} className="space-y-2">
                        <div className={`p-3 rounded-xl border transition-all ${
                          item.isEmergency 
                            ? 'bg-red-50/90 border-red-300 shadow-xs' 
                            : item.isShifted
                              ? 'bg-amber-50/80 border-amber-300'
                              : 'bg-white border-gray-200'
                        }`}>
                          <div className="flex items-center justify-between flex-wrap gap-2">
                            <div className="flex items-center gap-2 min-w-0">
                              <span className="w-5 h-5 rounded-full bg-gray-200 text-gray-700 text-[11px] font-bold flex items-center justify-center shrink-0">
                                {index + 1}
                              </span>
                              <span className="font-bold text-sm text-gray-900 truncate">
                                {item.schedule.title || 'Untitled Session'}
                              </span>
                              {item.isEmergency && (
                                <span className="text-[10px] font-black uppercase px-2 py-0.5 rounded-md bg-red-600 text-white tracking-wide">
                                  Emergency Retry
                                </span>
                              )}
                              {item.isShifted && (
                                <span className="text-[10px] font-bold uppercase px-2 py-0.5 rounded-md bg-amber-100 text-amber-800 border border-amber-300">
                                  Shifted Forward
                                </span>
                              )}
                            </div>

                            <div className="text-right shrink-0">
                              <div className="font-mono text-xs font-bold text-gray-800">
                                {formatMinutesTo12Hour(item.start)} – {formatMinutesTo12Hour(item.end)}
                              </div>
                              <div className="text-[11px] text-gray-500">
                                {item.duration} min
                                {item.isShifted && item.originalStart !== undefined && (
                                  <span className="text-amber-700 ml-1">
                                    (Was {formatMinutesTo12Hour(item.originalStart)})
                                  </span>
                                )}
                              </div>
                            </div>
                          </div>
                        </div>

                        {!isLast && (
                          <div className="flex items-center justify-center gap-2 py-0.5 text-gray-400 text-[11px] font-medium">
                            <div className="h-px w-8 bg-gray-300" />
                            <span className="px-2 py-0.5 bg-gray-200/80 rounded-full font-mono text-[10px] text-gray-600 font-bold">
                              + 30m break buffer
                            </span>
                            <div className="h-px w-8 bg-gray-300" />
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>
              </div>

              {/* Curfew Status Banner */}
              {!cascadeSimulation.valid ? (
                <div className="p-4 bg-red-950/90 border-2 border-red-600 rounded-2xl text-red-100 flex items-start gap-3 shadow-md">
                  <AlertTriangle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
                  <div>
                    <h5 className="font-bold text-sm text-white uppercase tracking-wide">Curfew Error</h5>
                    <p className="text-xs text-red-200 mt-0.5 leading-relaxed">
                      {cascadeSimulation.error || 'Too many schedules / exceeds 3:00 AM curfew'}
                    </p>
                    <p className="text-[11px] text-red-300/80 mt-1">
                      Lockdown curfew strictly ends at 3:00 AM. Please reduce duration or remove unneeded active schedules to proceed.
                    </p>
                  </div>
                </div>
              ) : (
                <div className="p-3.5 bg-emerald-50 border border-emerald-200 rounded-2xl text-emerald-800 flex items-center gap-2.5 text-xs font-semibold">
                  <CheckCircle className="w-4 h-4 text-emerald-600 shrink-0" />
                  <span>Cascade Shift Valid — All schedules safely finish before the 3:00 AM curfew.</span>
                </div>
              )}
            </div>

            <div className="p-5 border-t border-gray-100 bg-gray-50 flex justify-end items-center gap-3">
              <button
                type="button"
                onClick={() => setRescheduleTarget(null)}
                className="px-5 py-2.5 bg-white border border-gray-300 hover:bg-gray-100 text-gray-700 text-xs font-bold rounded-xl transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                disabled={!cascadeSimulation.valid}
                onClick={handleConfirmReschedule}
                className="px-6 py-2.5 bg-red-600 hover:bg-red-500 text-white text-xs font-bold rounded-xl transition-colors disabled:opacity-40 disabled:cursor-not-allowed shadow-sm flex items-center gap-2"
              >
                <RotateCcw className="w-4 h-4" />
                Confirm & Cascade Schedule
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export type AppState = 'onboarding' | 'dashboard' | 'create_schedule' | 'locked' | 'evaluating' | 'result' | 'settings' | 'edit_rubric' | 'permission_walkthrough' | 'logs';

export interface SavedResource {
  id: string;
  title: string;
  content: string;
  createdAt: number;
  type?: 'lecture_notes' | 'case_study';
}

export interface ScheduleData {
  id: string;
  title?: string;
  homeworkContent: string;
  rubricMode: 'ai' | 'manual';
  rubricContent: string;
  selectedResourceIds?: string[];
  activationTime: string; // HH:mm
  durationMinutes: number;
  isActive: boolean;
  userDraft?: string;
  aiAnswer?: string;
}

export interface AllowedApp {
  id: string;
  name: string;
  iconName: string;
  iconBase64?: string;
  isHardcoded?: boolean;
  isBrowser?: boolean;
  isMusic?: boolean;
  isCamera?: boolean;
  isAuthenticator?: boolean;
  isNotes?: boolean;
  isStudentApp?: boolean;
  isAi?: boolean;
}

export interface AppSettings {
  onboardingComplete: boolean;
  role: 'student' | 'teacher' | 'just a guy';
  schedules: ScheduleData[];
  apiKey?: string;
  apiModel?: string;
  prompts?: Record<string, string>;
  simpleOcrKey?: string;
  formattedOcrKey?: string;
  defaultOcrType?: 'simple' | 'formatted';
  uiScale?: number;
  uiWidth?: string;
  allowedApps?: AllowedApp[];
  allowedAppsInitialized?: boolean;
}

export interface EvaluationResult {
  wordCount?: number;
  sentenceCount?: number;
  passed: boolean;
  feedback: string;
  transcribedText: string;
}

export interface LogEntry {
  id: string;
  timestamp: number;
  action: string;
  details?: string;
}

export interface CompletedHomework {
  id: string;
  title: string;
  homeworkContent: string;
  rubricContent: string;
  transcribedText: string;
  feedback: string;
  passed: boolean;
  timestamp: number;
  durationMinutes?: number;
  selectedResourceIds?: string[];
}

declare global {
  interface Window {
    systemLockAPI?: {
      getInstalledApps: () => Promise<any[]>;
      startLockdown: (allowedAppIds: string[]) => void;
      endLockdown: () => void;
    };
  }
}

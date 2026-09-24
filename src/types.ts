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
  rubricMode: 'ai' | 'manual' | 'points';
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
  isAutoAllowed?: boolean;
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
  consequenceActive?: boolean;
  consequenceScheduleId?: string;
  operatingMode?: 'safemode' | 'hardcore';
  webProtectionMode?: 'accessibility' | 'dns_vpn' | 'dual_hybrid';
  allowYoutube?: boolean;
  blockWebGames?: boolean;
  dnsFilterProfile?: 'cleanbrowsing' | 'cloudflare_family' | 'adguard_family' | 'standard';
  enforceSafeSearch?: boolean;
  activeServices?: string[];
}

export interface UnifiedServiceDefinition {
  id: string;
  name: string;
  description: string;
  badge: string;
  packages: string[];
  domains: string[];
}

export const UNIFIED_SERVICES: UnifiedServiceDefinition[] = [
  {
    id: 'youtube',
    name: 'YouTube',
    description: 'Allows YouTube app and domains (youtube.com, youtu.be, m.youtube.com).',
    badge: 'App + Web',
    packages: ['com.google.android.youtube', 'com.google.android.youtube.tv', 'app.revanced.android.youtube', 'org.schabi.newpipe', 'app.libre_tube'],
    domains: ['youtube.com', 'youtu.be', 'm.youtube.com']
  },
  {
    id: 'gemini',
    name: 'Google Gemini',
    description: 'Allows Gemini app and web domains (gemini.google.com, bard.google.com).',
    badge: 'App + Web',
    packages: ['com.google.android.apps.gemini', 'com.google.android.apps.bard'],
    domains: ['gemini.google.com', 'bard.google.com']
  },
  {
    id: 'openai',
    name: 'ChatGPT / OpenAI',
    description: 'Allows ChatGPT app and web domains (chatgpt.com, openai.com).',
    badge: 'App + Web',
    packages: ['com.openai.chatgpt'],
    domains: ['chatgpt.com', 'chat.openai.com', 'openai.com']
  },
  {
    id: 'claude',
    name: 'Claude AI',
    description: 'Allows Claude app and web domains (claude.ai, anthropic.com).',
    badge: 'App + Web',
    packages: ['com.anthropic.claude'],
    domains: ['claude.ai', 'anthropic.com']
  }
];

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
  activationTime?: string;
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

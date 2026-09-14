import { getGeminiClient } from './geminiClient';
import { getPrompt } from './promptUtils';

interface GenerateAnswerOptions {
  content: string;
  resourcesText: string;
  rubric?: string;
  apiKey: string;
  apiModel: string;
  customPrompts?: Record<string, string>;
  isGeneralKnowledge?: boolean;
  category?: string;
}

export async function generateAnswer(opts: GenerateAnswerOptions): Promise<string> {
  const { content, resourcesText, rubric, apiKey, apiModel, customPrompts, isGeneralKnowledge, category } = opts;

  if (!apiKey || !apiKey.trim()) {
    throw new Error('Missing Gemini API Key. Please add it in Settings.');
  }

  const categoryMap: Record<string, string> = {
    reflection: 'Reflection / Personal Essay',
    coding: 'Coding / Computer Science Task',
    math: 'Math / Physics / Engineering Problem',
    case_study: 'Case Study / Technical Analysis',
    worksheets: 'Standard Q&A / Worksheets',
  };

  const categoryName = category && category !== 'auto' ? (categoryMap[category] || category) : '';
  const categorySection = categoryName
    ? `=== CATEGORY OVERRIDE ===\nThe user has explicitly classified this assignment as: "${categoryName}".\nYou MUST strictly follow the exact tone, style, and formatting rules defined for "${categoryName}" in the FORMATTING MATRIX above. Do NOT use any other category's format.\n`
    : '';

  const ai = getGeminiClient(apiKey);
  let prompt = getPrompt('generateAnswer', {
    CONTENT: content,
    RESOURCES_TEXT: resourcesText,
    RUBRIC_SECTION: rubric ? `=== RUBRIC ===\n${rubric}\n` : '',
    CATEGORY_SECTION: categorySection,
  }, customPrompts);

  if (categorySection && !prompt.includes(categorySection)) {
    prompt = `${categorySection}\n${prompt}`;
  }

  const hasGeneralKnowledge = Boolean(
    isGeneralKnowledge || 
    resourcesText.includes('SYSTEM NOTE') || 
    resourcesText.includes('external general knowledge')
  );

  const preferredModel = apiModel || 'gemini-2.0-flash';
  const candidateModels = Array.from(new Set([
    preferredModel,
    'gemini-2.5-flash',
    'gemini-2.0-flash',
    'gemini-1.5-flash',
    'gemini-2.5-pro',
  ]));

  let lastError: any = null;
  for (const model of candidateModels) {
    const config: Record<string, any> = { temperature: 0.3 };
    if (hasGeneralKnowledge) {
      config.tools = [{ googleSearch: {} }];
    }

    try {
      try {
        const response = await ai.models.generateContent({
          model,
          contents: prompt,
          config,
        });
        return response.text ?? '';
      } catch (err: any) {
        // If the chosen model or API key does not support the googleSearch grounding tool, gracefully fallback without tools
        if (config.tools) {
          delete config.tools;
          const response = await ai.models.generateContent({
            model,
            contents: prompt,
            config,
          });
          return response.text ?? '';
        }
        throw err;
      }
    } catch (err: any) {
      lastError = err;
      const errMsg = (err?.message || '').toLowerCase();
      const isRateLimit = errMsg.includes('429') || errMsg.includes('resource_exhausted') || errMsg.includes('quota');
      if (isRateLimit || errMsg.includes('not found') || errMsg.includes('404')) {
        console.warn(`generateAnswer: Model ${model} hit error (${err?.message}). Trying next candidate model...`);
        continue;
      }
      throw err;
    }
  }

  throw new Error(`Rate limit exceeded on all available Gemini models. ${lastError?.message || 'Please wait a moment or switch models.'}`);
}

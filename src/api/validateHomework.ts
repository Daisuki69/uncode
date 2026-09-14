import { getGeminiClient } from './geminiClient';
import { getPrompt } from './promptUtils';

interface ValidateHomeworkOptions {
  content: string;
  resourcesText: string;
  apiKey: string;
  apiModel: string;
  customPrompts?: Record<string, string>;
}

export async function validateHomework(opts: ValidateHomeworkOptions): Promise<{ valid: boolean; reason: string; isCaseStudy?: boolean }> {
  const { content, resourcesText, apiKey, apiModel, customPrompts } = opts;

  if (!apiKey || !apiKey.trim()) {
    throw new Error('Missing Gemini API Key. Please add it in Settings.');
  }

  const ai = getGeminiClient(apiKey);
  const prompt = getPrompt('validateHomework', {
    CONTENT: content,
    RESOURCES_TEXT: resourcesText,
  }, customPrompts);

  const response = await ai.models.generateContent({
    model: apiModel || 'gemini-2.0-flash',
    contents: prompt,
    config: { responseMimeType: 'application/json', temperature: 0.1 },
  });

  const cleaned = (response.text ?? '{}').replace(/```json/g, '').replace(/```/g, '').trim();
  return JSON.parse(cleaned);
}

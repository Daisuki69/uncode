import { Filesystem, Directory, Encoding } from '@capacitor/filesystem';

/**
 * Saves JSON data to the native app data directory, bypassing WebView storage limits.
 */
export async function saveData(filename: string, data: any): Promise<void> {
  const jsonString = JSON.stringify(data);
  try {
    localStorage.setItem(filename, jsonString);
  } catch {}

  try {
    await Filesystem.writeFile({
      path: filename,
      data: jsonString,
      directory: Directory.Data,
      encoding: Encoding.UTF8,
    });
  } catch (error) {
    console.error(`Failed to save ${filename} natively:`, error);
  }
}

/**
 * Instant synchronous storage read from hot localStorage cache.
 */
export function loadDataSync<T>(filename: string, defaultValue: T): T {
  try {
    const raw = localStorage.getItem(filename);
    if (raw) {
      return JSON.parse(raw) as T;
    }
  } catch {}
  return defaultValue;
}

/**
 * Loads JSON data from the native app data directory.
 * If the file is not found (e.g. first run after migration), it will attempt
 * to load from localStorage as a fallback/migration path.
 */
export async function loadData<T>(filename: string, defaultValue: T): Promise<T> {
  const fallback = localStorage.getItem(filename);
  let cachedValue: T | null = null;
  if (fallback) {
    try {
      cachedValue = JSON.parse(fallback) as T;
    } catch {}
  }

  try {
    const contents = await Filesystem.readFile({
      path: filename,
      directory: Directory.Data,
      encoding: Encoding.UTF8,
    });
    const parsed = JSON.parse(contents.data as string) as T;
    try {
      localStorage.setItem(filename, contents.data as string);
    } catch {}
    return parsed;
  } catch (error) {
    if (cachedValue !== null) {
      return cachedValue;
    }
    return defaultValue;
  }
}

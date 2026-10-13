import re

def extract_methods(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Extract plugin methods
    plugin_methods = re.findall(r'@PluginMethod\s+(?:public\s+)?(?:override\s+)?(?:void\s+|fun\s+)(\w+)', content)
    # Extract activity callbacks
    activity_callbacks = re.findall(r'@ActivityCallback\s+(?:private\s+)?(?:void\s+|fun\s+)(\w+)', content)
    # Extract permission callbacks
    perm_callbacks = re.findall(r'@PermissionCallback\s+(?:private\s+)?(?:void\s+|fun\s+)(\w+)', content)
    # Extract prefs keys with optional safe call
    prefs_get = set(re.findall(r'(?:prefs|sharedPrefs)\??\.get\w+\(["\']([^"\']+)["\']', content))
    prefs_put = set(re.findall(r'(?:editor|sharedPrefs\??\.edit\(\))\??\.(?:put|remove)\w*\(["\']([^"\']+)["\']', content))
    
    # Also find all string literals inside get/put calls
    all_prefs = set(re.findall(r'\.(?:get|put|remove)[A-Za-z]*\(["\']([a-zA-Z0-9_]+)["\']', content))

    return {
        'plugin_methods': plugin_methods,
        'activity_callbacks': activity_callbacks,
        'perm_callbacks': perm_callbacks,
        'prefs_get': prefs_get,
        'prefs_put': prefs_put,
        'all_prefs': all_prefs
    }

java_info = extract_methods(r'c:\Users\CxAdmin\Desktop\qiezka\uncode\scratch\backup_phase5\LockPlugin.java')
kt_info = extract_methods(r'c:\Users\CxAdmin\Desktop\qiezka\uncode\android\app\src\main\java\com\uncode\app\LockPlugin.kt')

print("--- ALL PREFS ACCESS ---")
diff_prefs = java_info['all_prefs'] - kt_info['all_prefs']
print(f"Java all prefs keys ({len(java_info['all_prefs'])}): {sorted(java_info['all_prefs'])}")
print(f"Kotlin all prefs keys ({len(kt_info['all_prefs'])}): {sorted(kt_info['all_prefs'])}")
print(f"Missing prefs in Kotlin: {diff_prefs}")


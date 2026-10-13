import xml.etree.ElementTree as ET

tree = ET.parse('scratch/role_dump.xml')
root = tree.getroot()

print("1. Root className:", root.find('node').get('class'))

# Check 2: find by id com.android.permissioncontroller:id/title
title_nodes = [n for n in root.iter('node') if n.get('resource-id') == 'com.android.permissioncontroller:id/title']
print(f"2. Found {len(title_nodes)} title nodes:")
for t in title_nodes:
    txt = t.get('text', '')
    lower = txt.strip().lower()
    match = ('default home' in lower or 'home app' in lower or ('default' in lower and 'home' in lower) or lower == 'home launcher')
    print(f"   text='{txt}' -> matches English keywords? {match}")

# Check 3: list and button1
list_nodes = [n for n in root.iter('node') if n.get('resource-id') == 'com.android.permissioncontroller:id/list']
btn1_nodes = [n for n in root.iter('node') if n.get('resource-id') == 'android:id/button1']
print(f"3. list_nodes count: {len(list_nodes)}, btn1_nodes count: {len(btn1_nodes)}")
for b in btn1_nodes:
    bt = b.get('text', '')
    print(f"   btn1 text='{bt}' -> contains 'default'? {'default' in bt.lower()}")

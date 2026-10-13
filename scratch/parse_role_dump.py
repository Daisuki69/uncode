import xml.etree.ElementTree as ET

tree = ET.parse('scratch/role_dump.xml')
root = tree.getroot()

def dump_node(node, depth=0):
    indent = "  " * depth
    cls = node.get('class', '')
    res_id = node.get('resource-id', '')
    pkg = node.get('package', '')
    text = node.get('text', '')
    desc = node.get('content-desc', '')
    checkable = node.get('checkable', '')
    checked = node.get('checked', '')
    clickable = node.get('clickable', '')
    bounds = node.get('bounds', '')
    print(f"{indent}<{cls} id='{res_id}' text='{text}' desc='{desc}' checkable='{checkable}' checked='{checked}' bounds='{bounds}'>")
    for child in node.findall('node'):
        dump_node(child, depth + 1)

for child in root.findall('node'):
    dump_node(child, 0)

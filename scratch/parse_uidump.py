import xml.etree.ElementTree as ET

tree = ET.parse('scratch/uidump.xml')
for node in tree.iter('node'):
    text = node.attrib.get('text', '')
    res_id = node.attrib.get('resource-id', '')
    cls = node.attrib.get('class', '')
    pkg = node.attrib.get('package', '')
    if text:
        print(f'{pkg} | {cls} | {res_id} -> text: "{text}"')

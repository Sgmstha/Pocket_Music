"""Convert the pinned ISC-licensed Lucide SVG primitives to Android VectorDrawable.
Original SVGs and upstream revision are retained beside this script. No tracing.
"""
from pathlib import Path
import xml.etree.ElementTree as ET
from xml.sax.saxutils import quoteattr

root = Path(__file__).resolve().parents[2]
out = root / 'app/src/main/res/drawable'
for file in (Path(__file__).parent / 'svg').glob('*.svg'):
    svg = ET.parse(file).getroot()
    paths = []
    for item in svg:
        tag = item.tag.split('}')[-1]
        a = item.attrib
        if tag == 'path':
            d = a['d']
        elif tag == 'line':
            d = f"M{a['x1']},{a['y1']}L{a['x2']},{a['y2']}"
        elif tag in ('polyline', 'polygon'):
            points = a['points'].strip().split()
            d = 'M' + 'L'.join(points) + ('Z' if tag == 'polygon' else '')
        elif tag == 'circle':
            x, y, r = (float(a[k]) for k in ('cx', 'cy', 'r'))
            d = f'M{x-r:g},{y:g}a{r:g},{r:g} 0 1,0 {2*r:g},0a{r:g},{r:g} 0 1,0 {-2*r:g},0'
        elif tag == 'rect':
            x,y,w,h = (float(a.get(k,0)) for k in ('x','y','width','height'))
            r = min(float(a.get('rx',0)), w/2, h/2)
            d = f'M{x+r:g},{y:g}H{x+w-r:g}Q{x+w:g},{y:g} {x+w:g},{y+r:g}V{y+h-r:g}Q{x+w:g},{y+h:g} {x+w-r:g},{y+h:g}H{x+r:g}Q{x:g},{y+h:g} {x:g},{y+h-r:g}V{y+r:g}Q{x:g},{y:g} {x+r:g},{y:g}Z'
        else:
            raise ValueError(f'Unsupported primitive {tag} in {file}')
        paths.append(f'    <path android:pathData={quoteattr(d)} android:fillColor="@android:color/transparent" android:strokeColor="#FFFFFFFF" android:strokeWidth="2" android:strokeLineCap="round" android:strokeLineJoin="round"/>')
    text = '<!-- Adapted from Lucide (ISC); see assets/licenses/lucide.txt and third_party/lucide/UPSTREAM.txt. -->\n<vector xmlns:android="http://schemas.android.com/apk/res/android" android:width="24dp" android:height="24dp" android:viewportWidth="24" android:viewportHeight="24">\n'+'\n'.join(paths)+'\n</vector>\n'
    (out / ('ic_lucide_'+file.stem.replace('-','_')+'.xml')).write_text(text, encoding='utf-8')
print('Converted pinned Lucide icons')

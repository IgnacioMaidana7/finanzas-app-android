"""Render the TP3 logical architecture as a standalone PNG. Requires Pillow."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import math

ROOT = Path(__file__).resolve().parents[1]
S = 2
im = Image.new('RGB', (1600*S, 1040*S), '#f5f7fb')
d = ImageDraw.Draw(im)
FONT = Path('C:/Windows/Fonts/segoeui.ttf')
BOLD = Path('C:/Windows/Fonts/segoeuib.ttf')
if not FONT.exists():
    FONT = Path('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf')
    BOLD = Path('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf')

def text(x, y, label, size=23, color='#21304a', bold=False, anchor=None):
    d.text((x*S, y*S), label, font=ImageFont.truetype(str(BOLD if bold else FONT), size*S), fill=color, anchor=anchor)

def box(bounds, fill='#ffffff', outline='#c4cfdf', radius=18):
    d.rounded_rectangle(tuple(int(v*S) for v in bounds), radius=radius*S, fill=fill, outline=outline, width=2*S)

def arrow(points, color='#536680', width=3):
    pts = [(int(x*S), int(y*S)) for x,y in points]
    d.line(pts, fill=color, width=width*S, joint='curve')
    x,y = pts[-1]
    angle = math.atan2(y-pts[-2][1], x-pts[-2][0])
    length = 12*S
    d.polygon([(x,y), (x-length*math.cos(angle-.45), y-length*math.sin(angle-.45)),
               (x-length*math.cos(angle+.45), y-length*math.sin(angle+.45))], fill=color)

text(65, 35, 'Finanzas App · Arquitectura de escalabilidad', 39, bold=True)
text(65, 91, 'TP3 · Arquitectura objetivo · 22 de octubre de 2026', 24)
box((1090, 45, 1535, 110), fill='#fff0cc', outline='#dbb76c')
text(1110, 62, 'Cloud Run: despliegue pendiente', 23, bold=True)

box((440, 205, 1110, 850), fill='#eaf2ff', outline='#7597ce', radius=24)
text(470, 222, 'Cloud Run propuesto · Santiago', 26, bold=True)
box((490, 285, 1060, 370))
text(775, 300, 'Entrada HTTPS y reparto administrado', 24, bold=True, anchor='mt')
text(775, 337, 'Firebase Auth en la API · sin afinidad de sesión', 21, anchor='mt')
arrow([(775,370),(775,405)])
box((490, 405, 1060, 475), fill='#d6e6ff', outline='#7597ce')
text(775, 425, 'Autoescalado · mínimo 0 / máximo 2', 25, bold=True, anchor='mt')
arrow([(775,475),(775,492),(620,492),(620,520)])
arrow([(775,492),(920,492),(920,520)])
for x, label in [(490,'Instancia 1'), (795,'Instancia 2 bajo demanda')]:
    box((x,520,x+265,610))
    text(x+132, 535, label, 21, bold=True, anchor='mt')
    text(x+132, 570, 'API Node · sin estado', 21, anchor='mt')
arrow([(620,610),(620,640)])
arrow([(920,610),(920,640)])
box((490,640,1060,755), fill='#ffffff')
text(775, 652, 'Dentro de cada instancia', 23, bold=True, anchor='mt')
text(775, 687, 'Verificar token → UID → consulta → BigInt', 22, anchor='mt')
text(775, 722, '1 mes UTC · hasta 1.001 documentos leídos', 21, anchor='mt')
text(470, 783, '1 vCPU / 256 MiB · concurrencia 16 · sin claves', 23)
text(470, 818, 'Logs y métricas operativas · sin UID ni importes', 21)

box((65,285,370,555))
text(217, 310, 'Android · TP2 + TP3', 25, bold=True, anchor='mt')
for y, label in [(360,'Kotlin / Compose'),(400,'ViewModels / repositorios'),(440,'Sesión Firebase'),(480,'Resumen mensual')]:
    text(217, y, label, 22, anchor='mt')
text(217, 524, 'Consulta manual', 20, anchor='mt')
arrow([(370,325),(490,325)], '#346db4')
text(426, 275, 'Token + mes', 18, color='#346db4', anchor='mt')

box((1180,210,1540,365), fill='#fff7e7', outline='#d6ae65')
text(1360, 233, 'Firebase Authentication', 25, bold=True, anchor='mt')
text(1360, 281, 'ID token · proyecto existente', 22, anchor='mt')
text(1360, 320, 'Estado y revocación de sesión', 21, anchor='mt')
arrow([(217,285),(217,166),(1360,166),(1360,210)], '#bd8c38')
text(590, 133, 'Inicio de sesión · obtiene ID token Firebase', 22, color='#88651f')
arrow([(1060,673),(1140,673),(1140,288),(1180,288)], '#bd8c38')
text(1125, 389, 'Auth', 18, color='#88651f')

box((1180,535,1540,760), fill='#e8f4ed', outline='#77ab8e')
text(1360, 555, 'Firestore · existente', 27, bold=True, anchor='mt')
text(1360, 603, 'users / UID / transactions', 23, anchor='mt')
text(1360, 643, 'Consulta por UID autenticado', 21, anchor='mt')
text(1360, 685, 'Cuenta runtime · IAM lectura', 21, anchor='mt')
text(1360, 722, 'southamerica-west1 · Santiago', 20, anchor='mt')
arrow([(1060,720),(1133,720),(1133,735),(1180,735)], '#3a865a')
text(1115, 765, 'ADC + IAM', 18, color='#3a865a')
arrow([(1360,760),(1360,910),(217,910),(217,555)], '#3a865a')
text(500, 922, 'Acceso directo TP2 · Security Rules · sólo datos del propietario', 24, color='#326d4b')
text(65, 982, 'Terraform + imagen por digest · pruebas locales sintéticas · no hay evidencia cloud todavía', 23)

target = ROOT / 'docs/tp3/arquitectura.png'
im.save(target)
print(target)

"""Generate the editable TP7 diagram. Symbols illustrate technologies, not official logos."""
from pathlib import Path
from html import escape

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / 'docs' / 'tp7' / 'arquitectura.svg'
OUTPUT.parent.mkdir(parents=True, exist_ok=True)
svg = ['<svg xmlns="http://www.w3.org/2000/svg" width="1800" height="1380" viewBox="0 0 1800 1380">',
       '<defs><marker id="arrow" markerWidth="10" markerHeight="10" refX="8" refY="4" orient="auto"><path d="M0,0 L8,4 L0,8" fill="#486478"/></marker></defs>',
       '<rect width="1800" height="1380" fill="white"/>']

def text(x, y, value, size=30, bold=False, color='#193547', anchor='middle'):
    svg.append(f'<text x="{x}" y="{y}" font-family="Arial,sans-serif" font-size="{size}" font-weight="{700 if bold else 400}" fill="{color}" text-anchor="{anchor}">{escape(value)}</text>')

def icon(kind, x, y, color='#2A739B', scale=1):
    shapes = {
        'android': '<path d="M8 25 A30 28 0 0 1 68 25 Z"/><rect x="8" y="28" width="60" height="39" rx="5"/><path d="M14 8 L6 -6 M62 8 L70 -6" fill="none" stroke="currentColor" stroke-width="5"/><circle cx="24" cy="14" r="3" fill="white"/><circle cx="52" cy="14" r="3" fill="white"/><rect x="-3" y="28" width="8" height="32" rx="4"/><rect x="71" y="28" width="8" height="32" rx="4"/><rect x="18" y="62" width="10" height="18" rx="4"/><rect x="48" y="62" width="10" height="18" rx="4"/>',
        'phone': '<rect x="8" y="0" width="54" height="78" rx="8" fill="none" stroke="currentColor" stroke-width="5"/><rect x="17" y="13" width="36" height="44" rx="3" opacity=".2"/><path d="M24 25h20M24 35h20M24 45h13" fill="none" stroke="currentColor" stroke-width="4"/><circle cx="35" cy="67" r="3"/>',
        'layers': '<path d="M0 18L36 0 72 18 36 36Z"/><path d="M0 35L36 53 72 35M0 52L36 70 72 52" fill="none" stroke="currentColor" stroke-width="7"/>',
        'database': '<ellipse cx="36" cy="12" rx="32" ry="12"/><path d="M4 12V62C4 78 68 78 68 62V12" fill="none" stroke="currentColor" stroke-width="5"/><path d="M4 33C4 49 68 49 68 33M4 52C4 68 68 68 68 52" fill="none" stroke="currentColor" stroke-width="4"/>',
        'chart': '<path d="M3 3V68H74M13 49L30 34 46 42 69 14M55 14H69V28" fill="none" stroke="currentColor" stroke-width="6" stroke-linecap="round" stroke-linejoin="round"/>',
        'receipt': '<path d="M10 0H65V77L56 69 47 77 38 69 29 77 20 69 10 77Z" fill="none" stroke="currentColor" stroke-width="4"/><path d="M22 19H53M22 32H53M22 45H43M22 58H53" fill="none" stroke="currentColor" stroke-width="4"/>',
        'chip': '<rect x="9" y="9" width="54" height="54" rx="8" fill="none" stroke="currentColor" stroke-width="5"/><path d="M20 0V9M36 0V9M52 0V9M20 63V74M36 63V74M52 63V74M0 20H9M0 36H9M0 52H9M63 20H74M63 36H74M63 52H74" stroke="currentColor" stroke-width="4"/><text x="36" y="43" text-anchor="middle" font-family="Arial" font-size="24" font-weight="bold" fill="currentColor">ML</text>',
        'flame': '<path d="M8 64L20 7 34 29 43 0 68 65 37 82Z" fill="#FFB300"/><path d="M8 64L34 29 45 48 68 22 68 65 37 82Z" fill="#F57C00"/>',
        'dollar': '<circle cx="36" cy="36" r="33" fill="none" stroke="currentColor" stroke-width="5"/><text x="36" y="53" font-family="Arial" font-size="49" font-weight="bold" text-anchor="middle" fill="currentColor">$</text>',
        'kotlin': '<path d="M0 0H72L35 36 72 72H0Z" fill="#7955B5"/><path d="M0 0H72L0 72Z" fill="#D17AB8"/>',
    }
    svg.append(f'<g transform="translate({x},{y}) scale({scale})" color="{color}" fill="{color}">{shapes[kind]}</g>')

def node(x,y,w,h,lines,kind,fill='#FFFFFF',color='#2A739B',size=30):
    svg.append(f'<rect x="{x}" y="{y}" width="{w}" height="{h}" rx="18" fill="{fill}" stroke="#B7CAD6" stroke-width="2"/>')
    if w <= 400:
        icon(kind,x+w/2-18,y+14,color,.48)
        for i,line in enumerate(lines): text(x+w/2,y+85+i*size*1.28,line,size,i==0)
        return
    s=.8
    icon(kind,x+20,y+(h-78*s)/2,color,s)
    mid=x+(w+75*s)/2
    start=y+h/2-(len(lines)-1)*size*.64+size*.32
    for i,line in enumerate(lines): text(mid,start+i*size*1.28,line,size,i==0)

def arrow(points):
    svg.append(f'<polyline points="{points}" fill="none" stroke="#486478" stroke-width="4" marker-end="url(#arrow)"/>')

svg.append('<rect x="25" y="20" width="1750" height="975" rx="28" fill="#F4F8FB" stroke="#AAC1D0" stroke-width="3"/>')
icon('android',75,55,'#3C9A60',.8)
text(175,92,'Finanzas App en Android',36,True,anchor='start')
icon('kotlin',1300,55,scale=.65);text(1385,91,'Kotlin',28,True,anchor='start')
icon('layers',1535,55,scale=.65);text(1620,91,'Compose',28,True,anchor='start')
node(80,155,1640,135,['Jetpack Compose','Movimientos · Cuentas · Presupuestos · Metas · Indicadores · Tickets'],'phone',size=32)
arrow('900,290 900,350')
node(80,350,1640,115,['ViewModels y lógica de aplicación','Estado · Cálculos · Validación · Confirmación del usuario'],'layers',size=31)
for x in (330,880,1450): arrow(f'{x},465 {x},535')
node(80,535,500,140,['Repositorios del TP2','Sesión y datos financieros'],'database',size=29)
node(620,535,520,140,['Repositorios de indicadores','Cotizaciones e inflación'],'chart',fill='#EAF4FA',size=28)
node(1180,535,540,140,['Adaptador de tickets','Reconocimiento y borrador'],'receipt',fill='#FFF6E8',color='#A96E14',size=28)
arrow('880,675 880,765')
node(620,765,520,135,['Caché local','Datos públicos y fechas'],'database',size=29)
arrow('1450,675 1450,765')
node(1180,765,540,135,['Google ML Kit','OCR en el dispositivo'],'chip',fill='#FFF6E8',color='#A96E14',size=29)
arrow('330,675 330,1090')
arrow('620,605 600,605 600,1020 755,1020 755,1090')
arrow('1140,605 1160,605 1160,1020 1030,1020 1030,1090')
text(80,1040,'Servicios remotos',27,True,anchor='start')
node(80,1090,500,180,['Firebase Auth','Cloud Firestore','Datos privados por UID'],'flame',fill='#FFF8EE',size=28)
node(620,1090,250,180,['DolarAPI','HTTPS y JSON','Solo lectura'],'dollar',fill='#EFF8F2',color='#2E8154',size=25)
node(900,1090,300,180,['ArgentinaDatos','HTTPS y JSON','Solo lectura'],'chart',fill='#EAF4FA',size=25)
icon('receipt',1325,1110,'#A96E14',.8)
text(1460,1220,'Revisar antes de guardar',26,True)
text(900,1330,'El ticket vuelve al formulario como borrador y solo se guarda con confirmación.',29)
svg.append('</svg>')
OUTPUT.write_text('\n'.join(svg),encoding='utf-8')
print(OUTPUT)

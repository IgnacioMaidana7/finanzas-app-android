"""Render only measured Azure replica observations; never synthesize samples."""
from pathlib import Path
from datetime import datetime, timezone
import json
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt
import matplotlib.dates as mdates

root = Path(__file__).resolve().parents[1]
data = json.loads((root / 'docs/tp3/evidence/azure-scale-observed.json').read_text(encoding='utf-8-sig'))
samples = data['samples']
times = [datetime.fromisoformat(row['atUtc']) for row in samples]
fig, ax = plt.subplots(figsize=(13, 4.8), facecolor='#f5f7fb')
ax.set_facecolor('#ffffff')
ax.step(times, [row['replicas'] for row in samples], where='post', marker='o', markersize=3,
        color='#346db4', linewidth=2, label='Réplicas asignadas')
ax.step(times, [row['ready'] for row in samples], where='post', marker='.', markersize=4,
        color='#3a865a', linewidth=1.6, linestyle='--', label='Réplicas listas')
ax.set(title='TP3 · Autoescalado real en Azure Container Apps', xlabel='Hora UTC · 23/10/2026',
       ylabel='Réplicas observadas', ylim=(-.12, 2.3), yticks=[0, 1, 2])
ax.xaxis.set_major_formatter(mdates.DateFormatter('%H:%M:%S', tz=timezone.utc))
ax.xaxis.set_major_locator(mdates.MinuteLocator(interval=1))
ax.grid(axis='y', color='#dfe5ee')
ax.spines[['top', 'right']].set_visible(False)
ax.legend(loc='upper left', frameon=False)
fig.text(.075, .02, f'Fuente: API de réplicas Azure · {len(samples)} muestras reales · carga autenticada sintética: 1.000 consultas, 0 errores',
         fontsize=9, color='#536680')
fig.tight_layout(rect=[0, .05, 1, 1])
output = root / 'docs/tp3/azure/autoescalado-observado.png'
fig.savefig(output, dpi=180)
print(output)

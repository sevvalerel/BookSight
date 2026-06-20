import json
import sys
from pathlib import Path
from collections import defaultdict

try:
    import matplotlib.pyplot as plt
    import matplotlib.patches as mpatches
    import numpy as np
except ImportError:
    print("matplotlib ve numpy kuruluyor...")
    import subprocess
    subprocess.check_call([sys.executable, "-m", "pip", "install", "matplotlib", "numpy"])
    import matplotlib.pyplot as plt
    import matplotlib.patches as mpatches
    import numpy as np

# ── Veri yükle ────────────────────────────────────────────────────────────────
results_file = Path("k6-results.json")
if not results_file.exists():
    print("HATA: k6-results.json bulunamadı.")
    print("Önce şunu çalıştır:")
    print("  k6 run --out json=k6-results.json k6-performance-test.js")
    sys.exit(1)

TRACKED = {
    "login_duration", "books_duration", "book_detail_duration",
    "leaderboard_duration", "reading_status_duration", "recommend_duration",
}
series  = defaultdict(list)  # metric_name -> [ms values]

with open(results_file, encoding="utf-8") as f:
    for line in f:
        line = line.strip()
        if not line:
            continue
        try:
            obj = json.loads(line)
        except json.JSONDecodeError:
            continue
        if obj.get("type") != "Point":
            continue
        name = obj["metric"]
        if name in TRACKED:
            series[name].append(obj["data"]["value"])

if not series:
    print("HATA: JSON dosyasında veri bulunamadı. k6 doğru çalıştı mı?")
    sys.exit(1)

# ── Renkler ve etiketler ──────────────────────────────────────────────────────
CONFIG = {
    "login_duration":         {"label": "Login",           "color": "#E74C3C", "threshold": 3000},
    "books_duration":         {"label": "Kitap Listesi",   "color": "#2ECC71", "threshold": 2000},
    "book_detail_duration":   {"label": "Kitap Detayı",    "color": "#27AE60", "threshold": 2000},
    "leaderboard_duration":   {"label": "Liderlik Tablosu","color": "#F39C12", "threshold": 2000},
    "reading_status_duration":{"label": "Okuma Durumu",    "color": "#9B59B6", "threshold": 2000},
    "recommend_duration":     {"label": "Öneri",           "color": "#3498DB", "threshold": 5000},
}

ORDER = [
    "login_duration", "books_duration", "book_detail_duration",
    "leaderboard_duration", "reading_status_duration", "recommend_duration",
]
available = [m for m in ORDER if m in series]

cols = 3
rows = (len(available) + cols - 1) // cols
fig, axes = plt.subplots(rows, cols, figsize=(18, 5 * rows))
axes = np.array(axes).flatten()
fig.suptitle("BookSight — K6 Performans Testi Sonuçları", fontsize=15, fontweight="bold", y=1.01)

for ax, metric in zip(axes, available):
    cfg    = CONFIG[metric]
    data   = np.array(series[metric]) / 1000  # ms → s
    thresh = cfg["threshold"] / 1000

    # Zaman serisi çizgisi
    ax.plot(data, color=cfg["color"], linewidth=1.2, alpha=0.7, label="Yanıt süresi")

    # p95 ve ortalama yatay çizgiler
    p95  = np.percentile(data, 95)
    mean = np.mean(data)
    ax.axhline(p95,    color=cfg["color"], linestyle="--", linewidth=1.5, label=f"p(95) = {p95:.2f}s")
    ax.axhline(mean,   color="gray",       linestyle=":",  linewidth=1.2, label=f"Ort = {mean:.2f}s")
    ax.axhline(thresh, color="black",      linestyle="-",  linewidth=1.8, label=f"Eşik = {thresh:.0f}s")

    # Eşik aşımı rengi
    status_color = "#2ECC71" if p95 <= thresh else "#E74C3C"
    status_text  = "PASS ✓" if p95 <= thresh else "FAIL ✗"
    ax.set_facecolor("#F9F9F9")
    ax.set_title(f"{cfg['label']}\n[{status_text}]", fontsize=12, color=status_color, fontweight="bold")
    ax.set_xlabel("İstek #", fontsize=9)
    ax.set_ylabel("Süre (saniye)", fontsize=9)
    ax.legend(fontsize=8, loc="upper right")
    ax.grid(True, alpha=0.3)

# Kullanılmayan eksen varsa gizle
for ax in axes[len(available):]:
    ax.set_visible(False)

plt.tight_layout()
out_path = Path("k6-performance-report.png")
plt.savefig(out_path, dpi=150, bbox_inches="tight")
print(f"Grafik kaydedildi: {out_path.resolve()}")
plt.show()

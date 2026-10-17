"""Sinh guide.html đa ngôn ngữ (mặc định tiếng Anh) từ các file i18n_*.py.
Dùng: python3 tools/guide/build_guide.py <thư mục trang web>"""
import importlib.util, json, os, re, sys

here = os.path.dirname(os.path.abspath(__file__))
def load(name):
    spec = importlib.util.spec_from_file_location(name, os.path.join(here, name + ".py"))
    m = importlib.util.module_from_spec(spec); spec.loader.exec_module(m); return m

base = load("i18n_en_vi")
KEYS = base.KEYS
LANGS = [("en", "English", base.EN), ("vi", "Tiếng Việt", base.VI)]
names = {"ES": ("es", "Español"), "PT": ("pt", "Português"), "FR": ("fr", "Français"), "DE": ("de", "Deutsch"),
         "ID": ("id", "Bahasa Indonesia"), "TH": ("th", "ไทย"), "JA": ("ja", "日本語"), "KO": ("ko", "한국어"),
         "ZH": ("zh", "简体中文"), "HI": ("hi", "हिन्दी"), "RU": ("ru", "Русский")}
for f in [x for x in ("i18n_1", "i18n_2", "i18n_3") if os.path.exists(os.path.join(here, x + ".py"))]:
    m = load(f)
    for var, (code, label) in names.items():
        if hasattr(m, var):
            LANGS.append((code, label, getattr(m, var)))
order = ["en", "vi", "es", "pt", "fr", "de", "id", "th", "ja", "ko", "zh", "hi", "ru"]
LANGS.sort(key=lambda x: order.index(x[0]))

data = {}
for code, label, d in LANGS:
    missing = [k for k in KEYS if k not in d]
    if missing: raise SystemExit(f"{code} missing {missing}")
    data[code] = {k: re.sub(r"<k>(.*?)</k>", r"<kbd>\1</kbd>", d[k]) for k in KEYS}

style = open(os.path.join(here, "style.css.txt")).read()
E = data["en"]
def t(k, tag="span", cls=""):
    c = f' class="{cls}"' if cls else ""
    return f'<{tag}{c} data-k="{k}">{E[k]}</{tag}>'
def steps(*ks):
    return '<ol class="steps">' + "".join(f'<li data-k="{k}">{E[k]}</li>' for k in ks) + "</ol>"
def phone(img, alt):
    return f'<div class="phone"><img src="img/guide/en/{img}.png" data-shot="{img}" alt="{alt}" loading="lazy"></div>'
def feature(id_, shots, h, p, st, extra=None, two=False):
    sh = f'<div class="shots{" two" if two else ""}">' + "".join(phone(i, a) for i, a in shots) + "</div>"
    ex = f'<p data-k="{extra}">{E[extra]}</p>' if extra else ""
    return f'''<section class="feature" id="{id_}">{sh}<div><h2 data-k="{h}">{E[h]}</h2><p data-k="{p}">{E[p]}</p>{steps(*st)}{ex}</div></section>'''

options = "".join(f'<option value="{c}">{l}</option>' for c, l, _ in LANGS)
new_items = "".join(f'<li><span class="ico">{ico}</span>{t(f"n{i}t", "b")}{t(f"n{i}d", "small")}</li>'
                    for i, ico in enumerate(["⏰", "🏷️", "🔔", "📷", "🧮", "#️⃣"], 1))
html = f'''<!doctype html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>cyanote++ 1.1 Guide</title>
<meta name="description" content="cyanote++ 1.1: appointments &amp; call reminders with sound, tag appointments in notes, photos and voice notes, Notepad++-style line numbers. Guide in 13 languages.">
<link rel="icon" href="logo.png">
{style}
</head>
<body>
<nav class="lang" aria-label="Language">
  <div class="wrap">
    <a class="brand" href="index.html"><img src="logo.png" alt="">cyanote<span>++</span></a>
    <select id="lang" aria-label="Language">{options}</select>
  </div>
</nav>
<main class="wrap">
  <section class="hero">
    <div>
      <span class="badge">VERSION 1.1 · 2026-10-17</span>
      <h1 data-k="title">{E["title"]}</h1>
      <p class="lead" data-k="lead">{E["lead"]}</p>
      <div class="cta">
        <a class="btn primary" href="download/cyanote-android.apk" data-k="dl">{E["dl"]}</a>
        <a class="btn" href="#install" data-k="install">{E["install"]}</a>
      </div>
    </div>
    {phone("01-main", "cyanote++")}
  </section>
  <ul class="new">{new_items}</ul>
  {feature("appointments", [("02-appointments", "Appointments")], "a_h", "a_p", ["a_1", "a_2", "a_3"])}
  {feature("tag", [("04-tag-appointment", "Tag appointment")], "t_h", "t_p", ["t_1", "t_2", "t_3"])}
  {feature("notify", [("05-notification", "Notification")], "o_h", "o_p", ["o_1", "o_2", "o_3"], extra="o_4")}
  {feature("media", [("01-main", "Strip"), ("06-attachments", "Photos & recordings")], "m_h", "m_p", ["m_1", "m_2", "m_3"], extra="m_4", two=True)}
  {feature("dock", [("03-calculator", "Calculator"), ("08-panel-hidden", "Panel hidden")], "d_h", "d_p", ["d_1", "d_2"], two=True)}
  {feature("lines", [("07-no-wrap", "Line numbers")], "l_h", "l_p", ["l_1", "l_2"])}
  <section class="feature" id="tablet" style="grid-template-columns:1fr">
    <div><h2 data-k="w_h">{E["w_h"]}</h2><p data-k="w_p">{E["w_p"]}</p></div>
    <div class="shots two" style="align-items:flex-start">
      <img class="tablet" style="width:62%" src="img/guide/en/09-tablet.png" data-shot="09-tablet" alt="Tablet" loading="lazy">
      <div class="phone" style="width:30%"><img src="img/guide/en/10-dark.png" data-shot="10-dark" alt="Dark mode" loading="lazy"></div>
    </div>
  </section>
  <section id="install">
    <div class="box"><h3 data-k="i_h">{E["i_h"]}</h3>{steps("i_1", "i_2", "i_3", "i_4")}</div>
    <div class="box"><h3 data-k="f_h">{E["f_h"]}</h3>
      <details><summary data-k="f_q1">{E["f_q1"]}</summary><p data-k="f_a1">{E["f_a1"]}</p></details>
      <details><summary data-k="f_q2">{E["f_q2"]}</summary><p data-k="f_a2">{E["f_a2"]}</p></details>
      <details><summary data-k="f_q3">{E["f_q3"]}</summary><p data-k="f_a3">{E["f_a3"]}</p></details>
    </div>
  </section>
</main>
<footer class="wrap"><a href="index.html">cyanote++</a> · <a href="privacy.html" data-k="privacy">{E["privacy"]}</a> · <a href="mailto:cyan881990@gmail.com">cyan881990@gmail.com</a></footer>
<script>
const T = {json.dumps(data, ensure_ascii=False)};
const sel = document.getElementById('lang');
function apply(l) {{
  if (!T[l]) l = 'en';
  document.documentElement.lang = l;
  sel.value = l;
  document.querySelectorAll('[data-k]').forEach(el => {{ el.innerHTML = T[l][el.dataset.k]; }});
  // Ảnh chụp app: bản tiếng Việt cho "vi", tiếng Anh cho các ngôn ngữ khác
  const set = l === 'vi' ? 'vi' : 'en';
  document.querySelectorAll('img[data-shot]').forEach(img => {{ img.src = 'img/guide/' + set + '/' + img.dataset.shot + '.png'; }});
}}
// Mặc định tiếng Anh; ?lang=vi hoặc lựa chọn đã lưu sẽ được ưu tiên
let l = new URLSearchParams(location.search).get('lang');
if (!l) {{ try {{ l = localStorage.getItem('guide_lang'); }} catch (e) {{}} }}
apply(l || 'en');
sel.addEventListener('change', () => {{
  apply(sel.value);
  try {{ localStorage.setItem('guide_lang', sel.value); }} catch (e) {{}}
  const u = new URL(location); u.searchParams.set('lang', sel.value); history.replaceState(null, '', u);
}});
</script>
</body>
</html>
'''
out = os.path.join(sys.argv[1], "guide.html")
open(out, "w").write(html)
print("wrote", out, len(html), "bytes,", len(LANGS), "languages")

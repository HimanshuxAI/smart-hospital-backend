# -*- coding: utf-8 -*-
"""Smart Hospital — 17-slide editorial (Swiss print) theme."""
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN, MSO_ANCHOR
from pptx.enum.shapes import MSO_SHAPE
from pptx.enum.chart import XL_CHART_TYPE
from pptx.chart.data import CategoryChartData
from pptx.oxml.ns import qn
from pptx.oxml.xmlchemy import OxmlElement
from pathlib import Path
from PIL import Image

PAPER = RGBColor(0xF7, 0xF3, 0xEA)
INK = RGBColor(0x12, 0x18, 0x1F)
TEAL = RGBColor(0x0F, 0x52, 0x57)
CORAL = RGBColor(0xE4, 0x57, 0x2E)
GOLD = RGBColor(0xE9, 0xC4, 0x6A)
CRITICAL = RGBColor(0xE5, 0x48, 0x4D)
HIGH = RGBColor(0xF9, 0x73, 0x16)
MEDIUM = RGBColor(0xF5, 0xB8, 0x3D)
NORMAL = RGBColor(0x22, 0xA0, 0x6B)
SOFT = RGBColor(0xE3, 0xDD, 0xCE)
MUTE = RGBColor(0x55, 0x52, 0x48)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
SERIF = "Georgia"
SANS = "Calibri"

prs = Presentation()
prs.slide_width = Inches(13.333)
prs.slide_height = Inches(7.5)
blank = prs.slide_layouts[6]
TOTAL = 17
ROOT = Path("/Users/himanshu/Documents/hospital")
ASSETS = ROOT / "assets"


def solid(sh, color):
    sh.fill.solid()
    sh.fill.fore_color.rgb = color
    sh.line.fill.background()


def rule(s, x, y, w, h=0.014, color=INK, alpha=1.0):
    sh = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(x), Inches(y), Inches(w), Inches(h))
    sh.fill.solid()
    sh.fill.fore_color.rgb = color
    if alpha < 1.0:
        spPr = sh._element.spPr
        sr = spPr.find(qn("a:solidFill")).find(qn("a:srgbClr"))
        a = OxmlElement("a:alpha")
        a.set("val", str(int(alpha * 100000)))
        sr.append(a)
    sh.line.fill.background()
    return sh


def text(s, x, y, w, h, content, size=14, color=INK, bold=False, italic=False,
         align=PP_ALIGN.LEFT, anchor=MSO_ANCHOR.TOP, font=SANS):
    tb = s.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = tb.text_frame
    tf.word_wrap = True
    tf.vertical_anchor = anchor
    tf.margin_left = Inches(0.02)
    tf.margin_right = Inches(0.02)
    tf.margin_top = Inches(0.01)
    tf.margin_bottom = Inches(0.01)
    for i, line in enumerate(content.split("\n")):
        p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
        p.alignment = align
        r = p.add_run()
        r.text = line
        r.font.size = Pt(size)
        r.font.bold = bold
        r.font.italic = italic
        r.font.name = font
        r.font.color.rgb = color
    return tb


def dot_grid(s, start_x=0.35, start_y=0.95, end_x=13.1, end_y=7.05, step=0.7):
    x = start_x
    while x < end_x:
        y = start_y
        while y < end_y:
            d = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x), Inches(y), Inches(0.02), Inches(0.02))
            solid(d, RGBColor(0xD8, 0xD2, 0xC2))
            y += step
        x += step


def page_mark(s, num, accent=TEAL):
    dot_grid(s)
    rule(s, 0.5, 0.66, 12.33, 0.016, INK)
    text(s, 0.5, 0.28, 9.5, 0.32, "SMART HOSPITAL — PATIENT QUEUE & APPOINTMENT SYSTEM", size=12, color=accent, bold=True)
    text(s, 12.0, 0.2, 0.83, 0.45, f"{num:02d}", size=22, color=accent, bold=True, align=PP_ALIGN.RIGHT, font=SERIF)
    rule(s, 0.5, 7.12, 12.33, 0.012, INK)
    text(s, 0.5, 7.17, 12.33, 0.25, "SMART HOSPITAL  ·  SY2207  ·  MIT SCHOOL OF COMPUTING", size=9, color=MUTE, bold=True, align=PP_ALIGN.CENTER)


def bg(s, paper=True):
    f = s.background.fill
    f.solid()
    f.fore_color.rgb = PAPER if paper else INK


def title_block(s, eyebrow, title, accent=TEAL, title_size=38, navy=False):
    c = WHITE if navy else INK
    text(s, 0.5, 0.85, 6.0, 0.35, eyebrow.upper(), size=13, color=accent, bold=True)
    text(s, 0.5, 1.1, 12.4, 0.6, title, size=title_size, color=c, bold=True, font=SERIF)
    rule(s, 0.5, 1.95, 2.4, 0.04, accent)


def footer(s, navy=False):
    col = RGBColor(0x94, 0xA3, 0xB8) if navy else MUTE
    text(s, 0.5, 6.85, 12.33, 0.3, "SMART HOSPITAL  ·  SY2207  ·  MIT SCHOOL OF COMPUTING", size=10, color=col, align=PP_ALIGN.CENTER)


def node(s, x, y, d, fill, label=None, tcol=PAPER, size=13):
    sh = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x), Inches(y), Inches(d), Inches(d))
    solid(sh, fill)
    if label:
        text(s, x, y, d, d, label, size=size, color=tcol, bold=True, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)


def rounded_card(s, x, y, w, h, fill=PAPER, accent=None, radius=0.06):
    sh = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(x), Inches(y), Inches(w), Inches(h))
    sh.fill.solid()
    sh.fill.fore_color.rgb = fill
    sh.line.color.rgb = INK
    sh.line.width = Pt(1.3)
    try:
        sh.adjustments[0] = radius
    except Exception:
        pass
    if accent:
        rule(s, x, y, w, 0.06, accent)
    return sh


def arrow_r(s, x, y, w=0.4):
    rule(s, x, y, w, 0.02, INK)
    tri = s.shapes.add_shape(MSO_SHAPE.ISOSCELES_TRIANGLE, Inches(x + w - 0.12), Inches(y - 0.055), Inches(0.14), Inches(0.12))
    tri.rotation = 90
    solid(tri, INK)


def cross_mark(s, x, y, size=0.22, color=CORAL):
    bar1 = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(x), Inches(y + size * 0.38), Inches(size), Inches(size * 0.24))
    solid(bar1, color)
    bar2 = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(x + size * 0.38), Inches(y), Inches(size * 0.24), Inches(size))
    solid(bar2, color)


def notes(s, who, body):
    try:
        s.notes_slide.notes_text_frame.text = f"Presenter: {who}\n\n{body}"
    except Exception:
        pass


def add_fade(s):
    try:
        trans = OxmlElement("p:transition")
        trans.set("spd", "med")
        fade = OxmlElement("p:fade")
        trans.append(fade)
        s._element.append(trans)
    except Exception:
        pass


def fit_size(path, box_w, box_h):
    with Image.open(path) as im:
        w, h = im.size
    r = min(box_w / w, box_h / h)
    return w * r, h * r


# ==========================================================
# 1 — TITLE (ink)
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s, paper=True)
dot_grid(s, end_x=13.1)
# top band
logo = ASSETS / "logo.png"
if logo.is_file():
    lw, lh = fit_size(logo, 1.25, 1.15)
    s.shapes.add_picture(str(logo), Inches(0.6), Inches(0.45), width=Inches(lw), height=Inches(lh))
text(s, 2.0, 0.72, 9.0, 0.4, "MIT SCHOOL OF COMPUTING   ·   SY2207", size=13, color=TEAL, bold=True)
cross_mark(s, 12.25, 0.62, 0.34, CORAL)
rule(s, 0.6, 1.75, 12.13, 0.02, INK)
# left column
text(s, 0.6, 2.05, 7.4, 1.9, "Smart\nHospital", size=74, color=INK, bold=True, font=SERIF)
text(s, 0.62, 4.0, 7.0, 0.9, "Patient Queue &\nAppointment Management", size=24, color=TEAL, bold=True, font=SERIF)
rule(s, 0.65, 5.05, 5.8, 0.024, INK)
text(s, 0.65, 5.18, 7.0, 0.4, "Book  ·  Token  ·  Track  ·  Triage  ·  Predict", size=16, color=INK, bold=True)
text(s, 0.65, 5.85, 7.0, 0.5, "Uday Mhaske · Sarthak Kothawale · Aryan Bhorde · Atharv Shewale", size=14, color=MUTE)
# right column
cross_mark(s, 9.85, 2.25, 0.55, CORAL)
text(s, 8.4, 3.0, 4.2, 1.0, "A hospital should not be\na waiting room.", size=22, color=INK, bold=True, italic=True, font=SERIF, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
text(s, 8.4, 4.05, 4.2, 0.4, "A clinic operating system, from booking to consultation.", size=13, color=TEAL, align=PP_ALIGN.CENTER)
rule(s, 9.2, 4.6, 2.8, 0.014, CORAL, alpha=0.7)
text(s, 8.5, 4.75, 3.7, 0.3, "GUIDE  ·  Dr. P. D. Patil", size=12, color=MUTE, align=PP_ALIGN.CENTER)
text(s, 8.5, 5.1, 3.7, 0.3, "A.Y. 2026–27  ·  Mini Project / Capstone", size=12, color=MUTE, align=PP_ALIGN.CENTER)
# bottom folio
rule(s, 0.6, 6.8, 12.13, 0.012, INK)
text(s, 0.6, 6.86, 9.0, 0.3, "SMART HOSPITAL  ·  PATIENT QUEUE & APPOINTMENT SYSTEM", size=10, color=MUTE)
text(s, 11.3, 6.86, 1.43, 0.3, "01 / 17", size=12, color=CORAL, bold=True, align=PP_ALIGN.RIGHT)
notes(s, "Uday", "Welcome the jury. State the tagline and the one-line promise: patients book, get a token, track their queue, and know their wait — while emergencies are triaged first.")
add_fade(s)


# ==========================================================
# 2 — PROBLEM HERO
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 2, CORAL)
title_block(s, "THE PROBLEM", "Patients wait 98.6 minutes to see a doctor", CORAL, 36)
text(s, 0.5, 2.0, 6.0, 1.2, "98.6 min", size=80, color=CORAL, bold=True, font=SERIF, anchor=MSO_ANCHOR.MIDDLE)
text(s, 0.5, 3.2, 6.0, 0.4, "average OPD waiting time", size=16, color=INK)
text(s, 0.5, 3.6, 6.0, 0.7, "6.5 min with the doctor", size=20, color=TEAL, bold=True)
text(s, 0.5, 4.3, 6.0, 0.3, "WAITING vs CONSULTING", size=11, color=TEAL, bold=True)
bar_y = 4.65
rule(s, 0.5, bar_y, 5.6, 0.35, TEAL)
text(s, 0.6, bar_y + 0.02, 5.0, 0.3, "Waiting time", size=12, color=WHITE, bold=True)
rule(s, 0.5, bar_y + 0.48, 0.36, 0.35, INK)
text(s, 0.95, bar_y + 0.5, 5.0, 0.3, "Consultation time", size=12, color=MUTE, bold=True)
text(s, 0.5, 5.7, 5.8, 0.3, "Datta M. Indian J Community Med. 2024;49(Suppl 1):S61 · tertiary hospital, Kolkata · n = 432", size=10, color=MUTE, italic=True)
x0 = 6.9
text(s, x0, 2.0, 5.8, 0.35, "THE COST OF NOT KNOWING", size=11, color=TEAL, bold=True)
pains = ["Long, uncertain waits — no position, no estimate",
         "Overcrowded rooms — no live visibility for anyone",
         "Manual tracking — paper tokens get lost or skipped",
         "Emergencies not triaged — critical patients wait behind routine cases"]
for i, p in enumerate(pains):
    text(s, x0, 2.4 + i * 0.62, 5.8, 0.55, f"·  {p}", size=14, color=INK)
rounded_card(s, x0, 5.0, 5.8, 1.5, accent=TEAL)
text(s, x0 + 0.2, 5.15, 5.4, 0.35, "WHAT WE REPLACE", size=11, color=TEAL, bold=True)
for i, (a, b) in enumerate([("Paper tokens", "Digital tokens"), ("Verbal calls", "Live queue board"), ("Guesswork", "ML wait estimate")]):
    text(s, x0 + 0.2, 5.55 + i * 0.32, 5.4, 0.3, f"{a}   →   {b}", size=13, color=INK)
footer(s, navy=False)
notes(s, "Uday", "Open with the Kolkata figure: patients wait 98.6 minutes but see the doctor for only 6.5. Emphasise that the wait is the problem, not the consultation, and that emergencies are mixed into the same line.")
add_fade(s)


# ==========================================================
# 3 — LITERATURE SURVEY
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 3, TEAL)
title_block(s, "LITERATURE SURVEY", "No open system does all four", TEAL, 36)
text(s, 0.5, 2.0, 12.33, 0.4, "The gap: no open, student-buildable stack combines booking, live queueing, emergency triage, and ML wait estimation.", size=14, color=MUTE, italic=True)
cols = ["Capability", "Paper token / OPD slip", "Standalone booking app", "Hospital HIS module", "Smart Hospital"]
rows = [
    ["Appointment booking", "—", "✓", "✓", "✓"],
    ["Digital token", "Manual slip", "—", "Partial", "✓"],
    ["Live queue position", "—", "—", "Partial", "✓"],
    ["Emergency triage", "Ad-hoc", "—", "✓", "✓ — 4 tiers"],
    ["Waiting-time estimate", "—", "—", "—", "✓ — ML + fallback"],
    ["Open, runnable locally", "—", "—", "—", "✓"],
]
widths = [3.0, 2.35, 2.35, 2.35, 2.28]
x_start = 0.5
y = 2.5
rule(s, x_start, y, 12.33, 0.016, INK)
x = x_start
for i, c in enumerate(cols):
    text(s, x + 0.08, y + 0.1, widths[i] - 0.16, 0.4, c.upper(), size=12, color=(TEAL if i in (0, 4) else MUTE), bold=True)
    x += widths[i]
rule(s, x_start, y + 0.55, 12.33, 0.01, INK)
for r, row in enumerate(rows):
    yy = y + 0.7 + r * 0.52
    x = x_start
    for i, cell in enumerate(row):
        color = INK if i == 0 else (TEAL if (i == 4 and cell.startswith("✓")) else MUTE)
        text(s, x + 0.08, yy, widths[i] - 0.16, 0.42, cell, size=14, color=color, bold=(i == 0 or i == 4))
        x += widths[i]
    rule(s, x_start, yy + 0.44, 12.33, 0.008, SOFT)
rounded_card(s, 0.5, 6.25, 12.33, 0.7, accent=TEAL)
text(s, 0.7, 6.4, 11.9, 0.4, "Conclusion: existing tools cover pieces; Smart Hospital is the first open stack that closes the whole loop.", size=14, color=INK, bold=True)
footer(s)
notes(s, "Uday", "Walk the table row by row and land on the last row: nothing else is open and runnable by a student. That is the contribution.")
add_fade(s)


# ==========================================================
# 4 — OBJECTIVES
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 4, TEAL)
title_block(s, "OBJECTIVES", "Six goals, each with a target", TEAL, 38)
rule(s, 0.5, 2.0, 12.33, 0.014, INK)
objs = [
    ("1", "Online booking", "Target: book in under 60 s; zero double-bookings", TEAL),
    ("2", "Digital tokens", "Target: one unique token per doctor, per day", TEAL),
    ("3", "Live queue", "Target: position and triage always visible", TEAL),
    ("4", "Emergency priority", "Target: CRITICAL served before every NORMAL case", CORAL),
    ("5", "Wait estimation", "Target: MAE ≤ 10 min on the synthetic set", GOLD),
    ("6", "Role dashboards", "Target: patient, doctor, admin from one login", TEAL),
]
for i, (n, t, d, c) in enumerate(objs):
    col = i % 3
    row = i // 3
    x = 0.5 + col * 4.24
    y = 2.25 + row * 2.25
    rounded_card(s, x, y, 3.95, 1.95, accent=c)
    text(s, x + 0.25, y + 0.25, 0.55, 0.55, n, size=28, color=c, bold=True, font=SERIF, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    text(s, x + 0.9, y + 0.28, 2.9, 0.45, t, size=16, color=INK, bold=True)
    text(s, x + 0.9, y + 0.72, 2.9, 0.9, d, size=13, color=MUTE)
footer(s)
notes(s, "Uday", "Each goal is measurable. Point out that every target maps to a test in the suite, so completion is verifiable, not aspirational.")
add_fade(s)


# ==========================================================
# 5 — SOLUTION
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 5, TEAL)
title_block(s, "SOLUTION", "Book, track, triage, predict — in one flow", TEAL, 36)
text(s, 0.5, 2.0, 12.33, 0.5, "A clinic operating system, from booking to consultation.", size=18, color=TEAL, bold=True, font=SERIF, italic=True)
pillars = [
    ("Book", TEAL, "Conflict-safe slots with a unique database index — the database has the final word."),
    ("Track", INK, "Live token, position and triage on the patient's own screen."),
    ("Triage + Predict", CORAL, "CRITICAL first; ML wait estimate with a deterministic Java fallback."),
]
for i, (t, c, d) in enumerate(pillars):
    x = 0.5 + i * 4.24
    rounded_card(s, x, 2.7, 3.95, 3.2, accent=c)
    text(s, x + 0.25, 2.9, 3.45, 0.5, t, size=22, color=c, bold=True, font=SERIF)
    text(s, x + 0.25, 3.45, 3.45, 2.0, d, size=14, color=INK)
footer(s)
notes(s, "Sarthak", "One sentence per pillar. Emphasise that the system is one flow, not three separate apps.")
add_fade(s)


# ==========================================================
# 6 — WORKFLOW
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 6, TEAL)
title_block(s, "WORKFLOW", "Who does what, in order", TEAL, 36)
lanes = [
    ("PATIENT", TEAL, ["Register", "Book slot", "Check in", "Track queue"]),
    ("SYSTEM", INK, ["Block slot", "Issue token", "Order queue", "Predict wait"]),
    ("DOCTOR", CORAL, ["Classify triage", "Call next", "Consult", "Complete"]),
]
lane_label_w, chips_x, chip_w, gap = 1.45, 2.1, 2.42, 0.22
for li, (lane, color, steps) in enumerate(lanes):
    y = 2.25 + li * 1.32
    rule(s, 0.5, y - 0.16, 12.33, 0.01, SOFT)
    text(s, 0.5, y + 0.24, lane_label_w, 0.36, lane, size=14, color=color, bold=True, anchor=MSO_ANCHOR.MIDDLE)
    for si, st in enumerate(steps):
        x = chips_x + si * (chip_w + gap)
        rounded_card(s, x, y, chip_w, 0.78, accent=color)
        text(s, x + 0.08, y + 0.04, chip_w - 0.16, 0.7, st, size=13, color=INK, bold=True, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
        if si < 3:
            arrow_r(s, x + chip_w + 0.02, y + 0.38, gap - 0.06)
rule(s, 0.5, 6.55, 12.33, 0.014, INK)
text(s, 0.5, 6.5, 12.33, 0.3, "Every lane shares one token — the patient always sees the same queue the doctor does.", size=15, color=TEAL, bold=True, italic=True, font=SERIF, align=PP_ALIGN.CENTER)
footer(s)
notes(s, "Sarthak", "Walk the lanes left-to-right. Stress that the system lane is invisible to the patient but is where the token and prediction happen.")
add_fade(s)


# ==========================================================
# 7 — ARCHITECTURE
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 7, TEAL)
title_block(s, "ARCHITECTURE", "One browser, one API, two data services", TEAL, 36)
rounded_card(s, 0.5, 2.3, 3.4, 2.2, accent=TEAL)
text(s, 0.7, 2.5, 3.0, 0.4, "Browser · React SPA", size=16, color=INK, bold=True, font=SERIF)
text(s, 0.7, 2.95, 3.0, 1.4, "Renders patient, doctor, and admin workspaces.\nTalks only to /api/v1.", size=13, color=MUTE)
arrow_r(s, 3.95, 3.4, 0.75)
text(s, 3.8, 3.62, 0.9, 0.3, "HTTPS + JWT", size=9, color=MUTE, align=PP_ALIGN.CENTER, bold=True)
rounded_card(s, 4.75, 2.3, 3.4, 2.2, accent=INK)
text(s, 4.95, 2.5, 3.0, 0.4, "API · Spring Boot", size=16, color=INK, bold=True, font=SERIF)
text(s, 4.95, 2.95, 3.0, 1.4, "Owns all state and rules:\nidentity, scheduling, queue,\ntriage, dashboards.", size=13, color=MUTE)
arrow_r(s, 8.2, 2.75, 0.85)
text(s, 7.95, 2.45, 1.05, 0.3, "JDBC", size=11, color=MUTE, align=PP_ALIGN.CENTER, bold=True)
arrow_r(s, 8.2, 3.95, 0.85)
text(s, 7.85, 4.15, 1.15, 0.3, "REST · 750 ms", size=10, color=MUTE, align=PP_ALIGN.CENTER, bold=True)
rounded_card(s, 9.05, 2.3, 3.7, 1.05, accent=CORAL)
text(s, 9.25, 2.52, 3.3, 0.4, "MySQL 8", size=16, color=INK, bold=True, font=SERIF)
text(s, 9.25, 2.95, 3.3, 0.3, "The authoritative store.", size=12, color=MUTE)
rounded_card(s, 9.05, 3.45, 3.7, 1.05, accent=GOLD)
text(s, 9.25, 3.68, 3.3, 0.4, "FastAPI · ML", size=16, color=INK, bold=True, font=SERIF)
text(s, 9.25, 4.1, 3.3, 0.3, "Predicts waiting time.", size=12, color=MUTE)
rule(s, 0.5, 5.25, 12.33, 0.014, INK)
text(s, 0.5, 5.38, 12.33, 0.4, "If the ML service is down or slow, Java falls back to a deterministic estimate — the API never blocks.", size=14, color=INK, align=PP_ALIGN.CENTER)
text(s, 0.5, 5.88, 12.33, 0.35, "Runs with Docker Compose: mysql · ml-service · backend · frontend.  Deployed on Render with ML_SERVICE_BASE_URL.", size=11, color=MUTE, italic=True, align=PP_ALIGN.CENTER)
footer(s)
notes(s, "Sarthak", "Name each box and its port. Explain that Java owns all state and Python only predicts — that is the key design choice.")
add_fade(s)


# ==========================================================
# 8 — DATA MODEL
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 8, CORAL)
title_block(s, "DATA MODEL", "Three data clusters, one clean schema", CORAL, 36)
clusters = [
    ("IDENTITY", TEAL, 0.5, "users\npatient_profiles\nrefresh_tokens"),
    ("SCHEDULING", INK, 4.72, "departments\ndoctor_profiles\navailability_templates\nschedule_slots\nappointments"),
    ("QUEUE + ML", CORAL, 9.14, "queue_entries\nqueue_audits\nwaiting_time_samples"),
]
for name, color, x, fields in clusters:
    rounded_card(s, x, 1.75, 3.8, 4.9, accent=color)
    text(s, x + 0.25, 1.95, 3.3, 0.35, name, size=13, color=color, bold=True)
    text(s, x + 0.25, 2.35, 3.3, 3.8, fields, size=14, color=INK)
text(s, 0.5, 6.55, 12.33, 0.3, "Flyway V1–V3 · forward-only · validated on a clean MySQL start · unique constraints prevent double-booking and duplicate tokens.", size=11, color=MUTE, italic=True)
footer(s)
notes(s, "Sarthak", "Group entities by concern, not by table. Point out the two unique constraints that protect booking and token allocation.")
add_fade(s)


# ==========================================================
# 9 — TECHNOLOGY
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 9, TEAL)
title_block(s, "TECHNOLOGY", "Boring, reliable, pinned versions", TEAL, 36)
stack = [
    ("LANGUAGE", "Java 21 · TypeScript 7.0.2\nPython 3.11", TEAL, "{ }"),
    ("BACKEND", "Spring Boot 3\nSpring Security · JWT", INK, "⚙"),
    ("DATA", "MySQL 8 · Flyway V1–V3\nJPA / Hibernate", CORAL, "DB"),
    ("ML", "FastAPI · scikit-learn", GOLD, "ML"),
    ("FRONTEND", "React 19.3.1 · Vite 8.3.8\nTailwind 4.3.5", TEAL, "⚛"),
    ("QUALITY", "JUnit 5 · 245 tests · pytest\nVitest 5.1.0 · Swagger UI · WCAG AA · <200 KB gzip", INK, "✓"),
]
for i, (cat, tech, c, glyph) in enumerate(stack):
    col = i % 3
    row = i // 3
    x = 0.5 + col * 4.24
    y = 1.75 + row * 2.45
    rounded_card(s, x, y, 3.95, 2.15, accent=c)
    text(s, x + 0.25, y + 0.22, 0.55, 0.55, glyph, size=22, color=c, bold=True, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE, font=SERIF)
    text(s, x + 0.9, y + 0.25, 2.8, 0.35, cat, size=12, color=c, bold=True)
    text(s, x + 0.25, y + 0.7, 3.45, 1.2, tech, size=14, color=INK, bold=True)
text(s, 0.5, 6.6, 12.33, 0.3, "Versions checked against package.json, Maven, and the Python environment.", size=11, color=MUTE, italic=True)
footer(s)
notes(s, "Aryan", "Highlight that every version is pinned and verified, and that the frontend ships under a 200 KB gzip budget.")
add_fade(s)


# ==========================================================
# 10 — THREE SEATS
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 10, TEAL)
title_block(s, "THE THREE SEATS", "Same data, three points of view", TEAL, 38)
roles = [
    ("P", "PATIENT", "BOOK. TRACK. ARRIVE.", TEAL, [
        "Browse departments, doctors, and slots",
        "Book or cancel in one tap",
        "Check in and receive a digital token",
        "Track position, triage, and estimated wait",
    ]),
    ("D", "DOCTOR", "CALL. TRIAGE. TREAT.", INK, [
        "See today's schedule at a glance",
        "Work the live queue in priority order",
        "Call, serve, complete, or skip",
        "Classify emergency with an audited override",
    ]),
    ("A", "ADMIN", "OPERATE. MEASURE. CONTROL.", CORAL, [
        "Manage users, doctors, departments, slots",
        "Oversee every appointment and queue",
        "Activate or deactivate accounts",
        "Read aggregate dashboards and audits",
    ]),
]
for i, (letter, name, tag, c, items) in enumerate(roles):
    x = 0.5 + i * 4.24
    rounded_card(s, x, 1.75, 3.95, 4.9, accent=c)
    text(s, x + 0.25, 1.95, 1.3, 0.9, letter, size=72, color=c, bold=True, font=SERIF, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    text(s, x + 1.5, 2.05, 2.3, 0.4, name, size=16, color=INK, bold=True)
    text(s, x + 1.5, 2.45, 2.3, 0.3, tag, size=11, color=c, bold=True)
    for j, it in enumerate(items):
        text(s, x + 0.25, 3.0 + j * 0.5, 3.45, 0.45, f"·  {it}", size=13, color=MUTE)
footer(s)
notes(s, "Aryan", "One sentence per role. Emphasise that all three see the same token and the same queue, just filtered by permission.")
add_fade(s)


# ==========================================================
# 11 — BOOKING TIMELINE
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 11, CORAL)
title_block(s, "BOOKING", "One slot, one token, no double-booking", CORAL, 36)
steps = [
    ("Browse", "Patient picks a doctor and an open slot."),
    ("Book", "Slot re-checked in a transaction; the unique index rejects a second active appointment."),
    ("Check in", "Valid ticket and inside the check-in window."),
    ("Token", "Allocated per doctor, per day — transactional."),
    ("Queue entry", "Position and triage returned to the patient."),
]
rule(s, 0.95, 2.55, 11.35, 0.04, TEAL)
for i, (t, d) in enumerate(steps):
    x = 0.6 + i * 2.45
    sh = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x + 0.55), Inches(2.2), Inches(0.7), Inches(0.7))
    solid(sh, TEAL)
    text(s, x + 0.55, 2.2, 0.7, 0.7, str(i + 1), size=22, color=WHITE, bold=True, font=SERIF, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    text(s, x, 3.05, 2.3, 0.4, t, size=16, color=INK, bold=True, align=PP_ALIGN.CENTER)
    text(s, x, 3.45, 2.3, 1.0, d, size=12, color=MUTE, align=PP_ALIGN.CENTER)
rounded_card(s, 0.5, 5.6, 12.33, 0.9, accent=TEAL)
text(s, 0.7, 5.8, 11.9, 0.5, "The slot is truth. The token is proof. The index is the guard.", size=15, color=INK, bold=True, italic=True, font=SERIF, align=PP_ALIGN.CENTER)
footer(s)
notes(s, "Aryan", "Emphasise the unique index: the database, not the application code, is the final guard against double-booking.")
add_fade(s)


# ==========================================================
# 12 — TRIAGE + ML
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 12, CORAL)
title_block(s, "TRIAGE & PREDICTION", "The sickest go first; waits are predicted", CORAL, 34)
text(s, 0.5, 1.95, 5.6, 0.3, "PRIORITY LADDER", size=11, color=TEAL, bold=True)
tiers = [("CRITICAL", 4.6, CORAL, WHITE, "seen first"), ("HIGH", 4.1, HIGH, WHITE, "elevated"),
         ("MEDIUM", 3.6, GOLD, INK, "elevated"), ("NORMAL", 3.1, NORMAL, WHITE, "oldest first")]
for i, (name, w, c, tc, note) in enumerate(tiers):
    y = 2.3 + i * 0.72
    sh = s.shapes.add_shape(MSO_SHAPE.ROUNDED_RECTANGLE, Inches(0.5), Inches(y), Inches(w), Inches(0.58))
    solid(sh, c)
    sh.line.fill.background()
    try:
        sh.adjustments[0] = 0.12
    except Exception:
        pass
    text(s, 0.68, y + 0.12, 2.4, 0.34, name, size=16, color=tc, bold=True, font=SERIF)
    text(s, 5.25, y + 0.14, 1.5, 0.34, note, size=12, color=MUTE)
text(s, 0.5, 5.3, 5.6, 0.35, "Within a tier, the oldest arrival is served first.", size=13, color=INK, bold=True)
text(s, 0.5, 5.65, 5.6, 0.35, "Any manual override requires a reason and writes an audit row.", size=13, color=MUTE, italic=True)
rounded_card(s, 6.5, 1.75, 3.0, 1.7, accent=GOLD)
text(s, 6.7, 1.9, 2.6, 0.35, "MAE", size=12, color=MUTE, bold=True)
text(s, 6.7, 2.25, 2.6, 1.0, "9.77", size=60, color=INK, bold=True, font=SERIF, anchor=MSO_ANCHOR.MIDDLE)
text(s, 6.7, 3.1, 2.6, 0.3, "min", size=12, color=MUTE, align=PP_ALIGN.CENTER)
rounded_card(s, 9.7, 1.75, 3.0, 1.7, accent=GOLD)
text(s, 9.9, 1.9, 2.6, 0.35, "R²", size=12, color=MUTE, bold=True)
text(s, 9.9, 2.25, 2.6, 1.0, "0.998", size=60, color=INK, bold=True, font=SERIF, anchor=MSO_ANCHOR.MIDDLE)
text(s, 0.5, 6.05, 5.6, 0.3, "Fallback formula alone: MAE 257.4 min — a safety net, not a predictor.", size=11, color=MUTE)
cd = CategoryChartData()
cd.categories = ["Avg consult mins", "Queue position", "People waiting", "Minutes from start", "Emergencies", "Hour of day"]
cd.add_series("Importance", (0.3396, 0.3352, 0.3181, 0.0018, 0.0017, 0.0017))
ch = s.shapes.add_chart(XL_CHART_TYPE.BAR_CLUSTERED, Inches(6.5), Inches(4.25), Inches(6.2), Inches(2.35), cd).chart
ch.has_legend = False
ch.value_axis.visible = False
ch.category_axis.tick_labels.font.size = Pt(11)
ch.category_axis.tick_labels.font.color.rgb = MUTE
ch.series[0].format.fill.solid()
ch.series[0].format.fill.fore_color.rgb = TEAL
text(s, 6.5, 6.62, 6.2, 0.28, "Feature importance read from model.joblib (RandomForestRegressor) · synthetic set, 1,200 rows.", size=10, color=MUTE, italic=True)
footer(s)
notes(s, "Aryan", "Point out that consult time, position and people waiting dominate, and that triage barely matters to the model — an honest finding worth discussing.")
add_fade(s)


# ==========================================================
# 13 — LIVE DEMO (ink)
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s, paper=False)
cross_mark(s, 0.6, 0.32, 0.24, CORAL)
text(s, 1.0, 0.34, 8.0, 0.3, "LIVE DEMO", size=11, color=CORAL, bold=True)
text(s, 0.6, 0.62, 12.1, 0.55, "Three screens, one flow", size=30, color=WHITE, bold=True, font=SERIF)
rule(s, 0.6, 1.28, 2.4, 0.05, CORAL)
frames = [("PATIENT", "Token, position, wait"), ("DOCTOR", "Live queue board"), ("ADMIN", "Operations dashboard")]
for i, (name, cap) in enumerate(frames):
    x = 0.6 + i * 4.24
    sh = s.shapes.add_shape(MSO_SHAPE.RECTANGLE, Inches(x), Inches(1.9), Inches(3.95), Inches(3.9))
    sh.fill.solid()
    sh.fill.fore_color.rgb = PAPER
    sh.line.color.rgb = INK
    sh.line.width = Pt(1.2)
    rule(s, x, 1.9, 3.95, 0.06, CORAL)
    shot = ASSETS / f"{name.lower()}.png"
    if shot.is_file():
        sw, shh = fit_size(shot, 3.7, 3.2)
        s.shapes.add_picture(str(shot), Inches(x + (3.95 - sw) / 2), Inches(2.1), width=Inches(sw), height=Inches(shh))
    else:
        text(s, x, 3.5, 3.95, 0.4, "[ screenshot ]", size=13, color=CORAL, bold=True, align=PP_ALIGN.CENTER)
    text(s, x, 5.3, 3.95, 0.34, name, size=16, color=INK, bold=True, align=PP_ALIGN.CENTER)
    text(s, x, 5.65, 3.95, 0.34, cap, size=13, color=MUTE, align=PP_ALIGN.CENTER)
text(s, 0.6, 6.3, 12.1, 0.4, "All data shown is the seeded demo dataset served by the real API.", size=12, color=RGBColor(0x94, 0xA3, 0xB8), italic=True, align=PP_ALIGN.CENTER)
notes(s, "Atharv", "Demo the patient booking and token, the doctor calling the next patient, and the admin dashboard in that order.")
add_fade(s)


# ==========================================================
# 14 — TESTING
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 14, TEAL)
title_block(s, "TESTING", "What we actually ran", TEAL, 38)
stats = [("245", "JUnit tests", TEAL), ("44", "Java test classes", INK), ("8", "frontend test files", CORAL), ("2", "pytest files", GOLD)]
for i, (v, k, c) in enumerate(stats):
    x = 0.5 + i * 3.12
    text(s, x, 1.75, 2.95, 1.2, v, size=60, color=c, bold=True, font=SERIF, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    rule(s, x, 3.05, 2.5, 0.03, c)
    text(s, x, 3.18, 2.95, 0.4, k, size=13, color=MUTE, align=PP_ALIGN.CENTER)
text(s, 0.5, 3.85, 6.0, 0.3, "WHAT THE TESTS COVER", size=11, color=TEAL, bold=True)
covers = ["Booking concurrency and slot uniqueness", "Token uniqueness per doctor and day",
          "Queue ordering and emergency tiers", "Auth, refresh rotation, rate limiting",
          "Flyway migrations on a clean database", "ML fallback when the sidecar is down"]
for i, c in enumerate(covers):
    text(s, 0.5, 4.2 + i * 0.4, 6.0, 0.38, f"✓  {c}", size=13, color=INK)
text(s, 7.0, 3.85, 5.7, 0.3, "VERIFICATION COMMANDS", size=11, color=CORAL, bold=True)
cmds = ["mvn verify  (backend)", "pytest  (ML service)", "npm test -- --run  (frontend)",
        "npm run typecheck && npm run build", "npm run check:bundle-size"]
for i, c in enumerate(cmds):
    text(s, 7.0, 4.2 + i * 0.4, 5.7, 0.38, c, size=13, color=INK)
text(s, 0.5, 6.6, 12.33, 0.3, "A deterministic demo sequence covers register → login → book → check in → token → triage → queue progression → wait estimate.", size=11, color=MUTE, italic=True, align=PP_ALIGN.CENTER)
footer(s)
notes(s, "Atharv", "List the commands and the coverage areas. Mention that the bundle budget is enforced in CI.")
add_fade(s)


# ==========================================================
# 15 — OUTCOMES & ROADMAP
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 15, TEAL)
title_block(s, "OUTCOMES & ROADMAP", "What ships today, what comes next", TEAL, 36)
stats = [("1", "token per check-in", TEAL), ("4", "emergency tiers", CORAL), ("2", "prediction sources", GOLD), ("0", "double-bookings possible", INK)]
for i, (v, k, c) in enumerate(stats):
    x = 0.5 + i * 3.12
    text(s, x, 1.75, 2.95, 0.9, v, size=48, color=c, bold=True, font=SERIF, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    rule(s, x, 2.7, 2.5, 0.03, c)
    text(s, x, 2.85, 2.95, 0.5, k, size=12, color=MUTE, align=PP_ALIGN.CENTER)
text(s, 0.5, 3.6, 5.8, 0.3, "SHIPPED AND VERIFIED", size=11, color=TEAL, bold=True)
ver = ["RFC 9457 problem details", "Single serialized token refresh", "Route guards and accessible 403/404",
       "WCAG AA contrast and focus states", "<200 KB gzip initial bundle"]
for i, v in enumerate(ver):
    text(s, 0.5, 3.95 + i * 0.42, 5.8, 0.38, f"✓  {v}", size=13, color=INK)
text(s, 7.0, 3.6, 5.7, 0.3, "ROADMAP", size=11, color=CORAL, bold=True)
rule(s, 7.15, 4.0, 0.04, 2.5, TEAL)
road = ["Mobile app and SMS / push alerts", "Multi-hospital and multi-department tenancy",
        "Cloud-hosted database with backups", "Online consultation, digital records, voice announcements"]
for i, f in enumerate(road):
    sh = s.shapes.add_shape(MSO_SHAPE.OVAL, Inches(7.0), Inches(4.0 + i * 0.62), Inches(0.32), Inches(0.32))
    solid(sh, TEAL)
    text(s, 7.0, 4.0 + i * 0.62, 0.32, 0.32, str(i + 1), size=12, color=WHITE, bold=True, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE)
    text(s, 7.45, 4.0 + i * 0.62, 5.2, 0.55, f, size=13, color=INK)
footer(s)
notes(s, "Atharv", "Left column is done; right column is the next two semesters. Be clear about what is not yet built.")
add_fade(s)


# ==========================================================
# 16 — LIMITATIONS + CONCLUSION (ink)
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s, paper=False)
cross_mark(s, 0.6, 0.32, 0.24, CORAL)
text(s, 1.0, 0.34, 8.0, 0.3, "LIMITATIONS & CONCLUSION", size=11, color=CORAL, bold=True)
text(s, 0.6, 0.62, 12.1, 0.55, "Honest limits, clear conclusion", size=30, color=WHITE, bold=True, font=SERIF)
rule(s, 0.6, 1.28, 2.4, 0.05, CORAL)
text(s, 0.6, 1.5, 5.9, 0.3, "LIMITATIONS", size=12, color=CORAL, bold=True)
limits = ["The ML model trains on synthetic, seeded data — not real patients",
          "Reported MAE and R² are on the training set, with no holdout split",
          "Single-hospital scope; no multi-tenant isolation",
          "Queue updates are fetched, not pushed in real time",
          "No payments, medical records, or notifications yet"]
for i, l in enumerate(limits):
    text(s, 0.6, 1.9 + i * 0.62, 5.9, 0.55, f"—  {l}", size=13, color=RGBColor(0xCB, 0xD5, 0xE1))
rule(s, 6.95, 1.5, 0.015, 4.5, RGBColor(0x3A, 0x2A, 0x24))
text(s, 7.2, 1.5, 5.5, 0.3, "CONCLUSION", size=12, color=TEAL, bold=True)
text(s, 7.2, 1.9, 5.5, 1.05, "Booking, live queueing, emergency triage, and ML wait estimation now run as one open, locally reproducible stack.", size=14, color=WHITE)
bullets = ["Concurrency handled with transactions and unique constraints",
           "Prediction degrades gracefully to a deterministic fallback",
           "Every role sees the same token from a different lens"]
for i, b in enumerate(bullets):
    text(s, 7.2, 3.05 + i * 0.58, 5.5, 0.55, f"·  {b}", size=13, color=RGBColor(0xCB, 0xD5, 0xE1))
text(s, 7.2, 5.0, 5.5, 0.5, "Next: replace synthetic history with real OPD data and add a holdout evaluation.", size=13, color=TEAL, bold=True, italic=True, font=SERIF)
rule(s, 0.6, 5.85, 12.33, 0.015, RGBColor(0x3A, 0x2A, 0x24))
text(s, 0.6, 6.0, 12.33, 0.5, "“Care that respects the patient’s time.”", size=24, color=GOLD, bold=True, italic=True, font=SERIF, align=PP_ALIGN.CENTER)
text(s, 0.6, 6.65, 12.33, 0.4, "Thank You / Questions?", size=20, color=WHITE, bold=True, align=PP_ALIGN.CENTER)
notes(s, "Atharv", "Be candid about limits, then land the conclusion and the closing quote. Invite questions.")
add_fade(s)


# ==========================================================
# 17 — REFERENCES (paper)
# ==========================================================
s = prs.slides.add_slide(blank)
bg(s)
page_mark(s, 17, INK)
title_block(s, "REFERENCES", "Sources and specifications", INK, 38)
refs = [
    "Datta M. Assessment of outdoor patients waiting time and consultation time in a tertiary care hospital of Kolkata. Indian J Community Med. 2024;49(Suppl 1):S61. doi:10.4103/ijcm.ijcm_abstract210",
    "Mohebbifar R, et al. Outpatient waiting time in health services and teaching hospitals: a case study in Iran. Glob J Health Sci. 2013;5(6):96–104. PMC4825353",
    "Oche MO, Adamu H. Determinants of patient waiting time in the general outpatient department of a tertiary health institution in north-western Nigeria. Ann Med Health Sci Res. 2013;3(4):588–592. PMC3868129",
    "The Spring Boot reference documentation. https://docs.spring.io/spring-boot/",
    "FastAPI documentation. https://fastapi.tiangolo.com/",
    "scikit-learn user guide. https://scikit-learn.org/stable/user_guide.html",
    "MySQL 8.4 reference manual. https://dev.mysql.com/doc/",
    "RFC 9457 — Problem Details for HTTP APIs. https://www.rfc-editor.org/rfc/rfc9457.html",
]
for i, r in enumerate(refs):
    y = 2.0 + i * 0.58
    text(s, 0.5, y, 0.5, 0.4, f"{i + 1:02d}", size=14, color=CORAL, bold=True, font=SERIF)
    text(s, 1.05, y, 11.7, 0.55, r, size=12.5, color=INK)
footer(s)
notes(s, "Atharv", "Keep this slide brief; offer the full paper list on request.")
add_fade(s)


out = ROOT / "Smart_Hospital_Presentation.pptx"
prs.save(str(out))
print("saved", out)
print("slides", len(prs.slides))

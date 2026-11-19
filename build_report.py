#!/usr/bin/env python3
"""Build the Smart Hospital B.Tech project report in Annexure 6 format.

The layout follows the Annexure 6 template: A4 page, 1" top/bottom and
1.25" left / 1" right margins, Times New Roman 12 pt justified body with
1.5 line spacing, 18 pt bold centred chapter titles, 12 pt bold section
headings, IEEE references and the MITSOC footer from the Introduction.
"""

from __future__ import annotations

import argparse
import json
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont
from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import (WD_ALIGN_PARAGRAPH, WD_TAB_ALIGNMENT,
                            WD_TAB_LEADER)
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Inches, Pt

ROOT = Path(__file__).resolve().parent
ASSETS = ROOT / "assets"
OUTPUT = ROOT / "Smart_Hospital_Project_Report_Formatted.docx"
FIG_DIR = ROOT / ".report_figures"

TITLE = "SMART HOSPITAL PATIENT QUEUE AND APPOINTMENT MANAGEMENT SYSTEM"
TEAM = [
    ("Uday Mhaske", "ADT24SOCB1735"),
    ("Aditya Sundalam", "MITU22BTCS0044"),
    ("Atharv Shewale", "ADT25SOCB1715"),
    ("Saiprasad Bendre", "ADT25SOCB1651"),
]
GUIDE = "Dr. P. D. Patil"
YEAR = "2026-27"

# A4 as used by the Annexure 6 template.
PAGE_W, PAGE_H = 8.27, 11.69
M_TOP = M_BOTTOM = 1.0
M_LEFT, M_RIGHT = 1.25, 1.0
TEXT_W = PAGE_W - M_LEFT - M_RIGHT  # 6.02 in

FRONT = "MITSOC, Department of Computer Science and Engineering, " + YEAR


def _font(run, size=12, bold=False, italic=False, name="Times New Roman"):
    run.font.name = name
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    rpr = run._element.get_or_add_rPr()
    rfonts = rpr.find(qn("w:rFonts"))
    if rfonts is None:
        rfonts = OxmlElement("w:rFonts")
        rpr.append(rfonts)
    for attr in ("ascii", "hAnsi", "eastAsia", "cs"):
        rfonts.set(qn(f"w:{attr}"), name)


def _para(doc, text="", *, align=WD_ALIGN_PARAGRAPH.JUSTIFY, indent=True,
          size=12, bold=False, italic=False, before=0, after=0, spacing=1.5,
          keep=False, left=0.0):
    p = doc.add_paragraph()
    p.alignment = align
    pf = p.paragraph_format
    pf.space_before = Pt(before)
    pf.space_after = Pt(after)
    pf.line_spacing = spacing
    pf.keep_with_next = keep
    pf.left_indent = Inches(left)
    pf.first_line_indent = Inches(0.5) if indent else Inches(0)
    if text:
        _font(p.add_run(text), size, bold, italic)
    return p


def _body(doc, text):
    """Annexure body paragraph: justified, 1.5 spacing, 2-space indent."""
    return _para(doc, text, before=10, after=0, spacing=1.5)


def _chapter(doc, text, before=18):
    p = _para(doc, text.upper(), align=WD_ALIGN_PARAGRAPH.CENTER, indent=False,
              size=18, bold=True, before=before, after=12, spacing=1.0,
              keep=True)
    for run in p.runs:
        _font(run, 18, bold=True)
    return p


def _section(doc, text):
    p = _para(doc, text, align=WD_ALIGN_PARAGRAPH.LEFT, indent=False,
              size=12, bold=True, before=12, after=6, spacing=1.0, keep=True)
    for run in p.runs:
        _font(run, 12, bold=True)
    return p


def _sub(doc, text):
    p = _para(doc, text, align=WD_ALIGN_PARAGRAPH.LEFT, indent=False,
              size=12, bold=True, italic=True, before=10, after=4,
              spacing=1.0, keep=True)
    for run in p.runs:
        _font(run, 12, bold=True, italic=True)
    return p


def _caption(doc, text, keep_next=False):
    p = _para(doc, text, align=WD_ALIGN_PARAGRAPH.CENTER, indent=False,
              size=10, bold=True, before=4, after=10, spacing=1.0,
              keep=keep_next)
    return p


def _no_borders(table):
    tbl_pr = table._tbl.tblPr
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = OxmlElement(f"w:{edge}")
        el.set(qn("w:val"), "nil")
        el.set(qn("w:sz"), "0")
        el.set(qn("w:space"), "0")
        borders.append(el)
    tbl_pr.append(borders)


def _set_widths(table, widths):
    tbl = table._tbl
    grid = tbl.tblGrid
    for i, width in enumerate(widths):
        if i < len(grid.gridCol_lst):
            grid.gridCol_lst[i].set(qn("w:w"), str(int(width * 1440)))
    tbl_pr = tbl.tblPr
    tbl_w = tbl_pr.find(qn("w:tblW"))
    if tbl_w is None:
        tbl_w = OxmlElement("w:tblW")
        tbl_pr.append(tbl_w)
    tbl_w.set(qn("w:w"), str(int(sum(widths) * 1440)))
    tbl_w.set(qn("w:type"), "dxa")


def _grid_table(doc, header, rows, widths, *, size=10):
    t = doc.add_table(rows=1, cols=len(header))
    t.style = "Table Grid"
    t.autofit = False
    for i, value in enumerate(header):
        cell = t.rows[0].cells[i]
        cell.text = ""
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_before = Pt(2)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.line_spacing = 1.0
        _font(p.add_run(str(value)), size, bold=True)
    for row in rows:
        cells = t.add_row().cells
        for i, value in enumerate(row):
            cells[i].text = ""
            p = cells[i].paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.LEFT
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            p.paragraph_format.line_spacing = 1.0
            _font(p.add_run(str(value)), size)
    for r in t.rows:
        for i, w in enumerate(widths):
            r.cells[i].width = Inches(w)
    _set_widths(t, widths)
    # Keep short tables on one page.
    for row in t.rows:
        tr_pr = row._tr.get_or_add_trPr()
        tr_pr.append(OxmlElement("w:cantSplit"))
        for cell in row.cells:
            for p in cell.paragraphs:
                p.paragraph_format.keep_with_next = True
    for cell in t.rows[-1].cells:
        for p in cell.paragraphs:
            p.paragraph_format.keep_with_next = False
    return t


def _plain_table(doc, header, rows, widths, *, size=12, header_bold=True):
    """Borderless two-column block: the template's plain name/PRN lines."""
    t = doc.add_table(rows=1, cols=len(header))
    t.alignment = WD_TABLE_ALIGNMENT.CENTER
    t.autofit = False
    _no_borders(t)
    for i, value in enumerate(header):
        cell = t.rows[0].cells[i]
        cell.text = ""
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.line_spacing = 1.0
        _font(p.add_run(str(value)), size, bold=header_bold)
    for row in rows:
        cells = t.add_row().cells
        for i, value in enumerate(row):
            cells[i].text = ""
            p = cells[i].paragraphs[0]
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER
            p.paragraph_format.space_before = Pt(0)
            p.paragraph_format.space_after = Pt(2)
            p.paragraph_format.line_spacing = 1.0
            _font(p.add_run(str(value)), size)
    for r in t.rows:
        for i, w in enumerate(widths):
            r.cells[i].width = Inches(w)
    _set_widths(t, widths)
    return t


def _figure(doc, path, caption, width=6.0):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    pf = p.paragraph_format
    pf.space_before = Pt(0)
    pf.space_after = Pt(2)
    pf.first_line_indent = Inches(0)
    pf.left_indent = Inches(0)
    pf.right_indent = Inches(0)
    pf.keep_with_next = True
    p.add_run().add_picture(str(path), width=Inches(width))
    _caption(doc, caption)
    return p


def _page_break(doc):
    doc.add_page_break()


def _field(paragraph, instruction, size=10):
    run = paragraph.add_run()
    begin = OxmlElement("w:fldChar")
    begin.set(qn("w:fldCharType"), "begin")
    instr = OxmlElement("w:instrText")
    instr.set(qn("xml:space"), "preserve")
    instr.text = f" {instruction} "
    end = OxmlElement("w:fldChar")
    end.set(qn("w:fldCharType"), "end")
    run._r.append(begin)
    run._r.append(instr)
    run._r.append(end)
    _font(run, size)


def _page_numbering(section, fmt, start=None):
    sect_pr = section._sectPr
    pg = sect_pr.find(qn("w:pgNumType"))
    if pg is None:
        pg = OxmlElement("w:pgNumType")
        sect_pr.append(pg)
    pg.set(qn("w:fmt"), fmt)
    if start is not None:
        pg.set(qn("w:start"), str(start))


def _footer(section, *, text=None, with_page=True):
    """Centred 10 pt footer, as required by the Annexure instructions."""
    footer = section.footer
    footer.is_linked_to_previous = False
    p = footer.paragraphs[0]
    p.text = ""
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    pf = p.paragraph_format
    pf.space_before = Pt(0)
    pf.space_after = Pt(0)
    pf.line_spacing = 1.0
    if text:
        _font(p.add_run(text), 10)
    if with_page:
        number = footer.add_paragraph()
        number.alignment = WD_ALIGN_PARAGRAPH.CENTER
        number.paragraph_format.space_before = Pt(0)
        number.paragraph_format.space_after = Pt(0)
        number.paragraph_format.line_spacing = 1.0
        _field(number, "PAGE")


def _setup_section(section):
    section.page_width = Inches(PAGE_W)
    section.page_height = Inches(PAGE_H)
    section.top_margin = Inches(M_TOP)
    section.bottom_margin = Inches(M_BOTTOM)
    section.left_margin = Inches(M_LEFT)
    section.right_margin = Inches(M_RIGHT)


def _logo(doc, name, width):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(8)
    p.add_run().add_picture(str(ASSETS / name), width=Inches(width))
    return p


def _institution_block(doc, after=0):
    for line in [
        "MIT SCHOOL OF COMPUTING",
        "DEPARTMENT OF COMPUTER SCIENCE AND ENGINEERING",
        "MIT ART, DESIGN AND TECHNOLOGY UNIVERSITY",
        "RAJBAUG CAMPUS, LONI-KALBHOR, PUNE 412201",
    ]:
        _para(doc, line, align=WD_ALIGN_PARAGRAPH.CENTER, indent=False,
              size=12, bold=True, after=4, spacing=1.0)
    if after:
        _para(doc, "", indent=False, after=after, spacing=1.0)


def _signatures(doc, left=("", ""), right=("", "")):
    t = doc.add_table(rows=2, cols=2)
    t.autofit = False
    _no_borders(t)
    cells = {(0, 0): left[0], (1, 0): left[1],
             (0, 1): right[0], (1, 1): right[1]}
    for (row_idx, col_idx), text in cells.items():
        cell = t.rows[row_idx].cells[col_idx]
        cell.text = ""
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_before = Pt(0)
        p.paragraph_format.space_after = Pt(2)
        p.paragraph_format.line_spacing = 1.0
        _font(p.add_run(text), 12, bold=(row_idx == 0))
    for r in t.rows:
        r.cells[0].width = Inches(3.0)
        r.cells[1].width = Inches(3.0)
    return t


def _dots(doc, title, page, *, level=0, bold=False, size=11):
    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.left_indent = Inches(0.28 * level)
    pf.first_line_indent = Inches(0)
    pf.space_before = Pt(0)
    pf.space_after = Pt(0)
    pf.line_spacing = 1.15
    pf.tab_stops.add_tab_stop(Inches(TEXT_W), WD_TAB_ALIGNMENT.RIGHT,
                              WD_TAB_LEADER.DOTS)
    _font(p.add_run(f"{title.strip()}\t{page}"), size, bold=bold)
    return p


def _diagrams():
    FIG_DIR.mkdir(exist_ok=True)
    serif = "/System/Library/Fonts/Times.ttc"
    bold = ImageFont.truetype(serif, 30)
    regular = ImageFont.truetype(serif, 24)
    small = ImageFont.truetype(serif, 20)
    navy = (22, 39, 87)
    orange = (214, 78, 42)
    cream = (244, 238, 225)
    ink = (20, 20, 20)
    grey = (120, 120, 120)

    # Figure 2.1 - system architecture
    im = Image.new("RGB", (1400, 760), "white")
    d = ImageDraw.Draw(im)
    blocks = [
        (40, 250, 330, 500, "CLIENT", "React + TypeScript", cream, ink),
        (400, 250, 870, 500, "CORE SERVICE", "Spring Boot API", navy, "white"),
        (940, 250, 1360, 500, "INTELLIGENCE", "FastAPI + scikit-learn", cream, ink),
    ]
    for x1, y1, x2, y2, head, body, fill, fg in blocks:
        d.rectangle((x1, y1, x2, y2), fill=fill, outline=ink, width=3)
        d.text((x1 + 24, y1 + 28), head, font=small,
               fill=orange if fill == cream else (216, 243, 105))
        d.text((x1 + 24, y1 + 78), body, font=bold, fill=fg)
    d.text((470, 370), "AUTH  /  APPOINTMENTS\nQUEUE  /  AUDIT",
           font=regular, fill="white", spacing=12)
    d.text((425, 545), "PATIENT     DOCTOR     ADMIN", font=small, fill=grey)
    d.text((965, 545), "ML ESTIMATE  /  FALLBACK", font=small, fill=grey)
    d.rectangle((400, 620, 870, 700), fill=navy)
    d.text((430, 642), "MYSQL 8.4   /   FLYWAY", font=regular, fill="white")
    d.line((635, 500, 635, 620), fill=ink, width=3)
    d.polygon([(345, 360), (390, 360), (390, 350), (410, 375),
               (390, 400), (390, 390), (345, 390)], fill=orange)
    d.polygon([(885, 360), (930, 360), (930, 350), (950, 375),
               (930, 400), (930, 390), (885, 390)], fill=orange)
    arch = FIG_DIR / "architecture.png"
    im.save(arch)

    # Figure 2.2 - queue ordering
    im = Image.new("RGB", (1400, 560), "white")
    d = ImageDraw.Draw(im)
    tiers = [("CRITICAL", orange), ("HIGH", (35, 60, 120)),
             ("MEDIUM", (46, 91, 155)), ("NORMAL", grey)]
    for i, (label, color) in enumerate(tiers):
        y = 130 + i * 95
        d.rectangle((60, y, 420, y + 70), fill=color)
        d.text((90, y + 18), label, font=regular, fill="white")
        d.line((440, y + 35, 640, y + 35), fill=color, width=4)
        d.text((660, y + 12), f"rank {i + 1}", font=regular, fill=ink)
    d.text((900, 160), "SORT BY", font=small, fill=orange)
    d.text((900, 205), "priority rank", font=bold, fill=ink)
    d.text((900, 270), "THEN", font=small, fill=orange)
    d.text((900, 315), "check-in time", font=bold, fill=ink)
    d.text((60, 510), "FIFO applies inside each priority tier.",
           font=regular, fill=grey)
    flow = FIG_DIR / "queue_flow.png"
    im.save(flow)

    # Figure 3.1 - verified waiting-time comparison
    im = Image.new("RGB", (1400, 700), "white")
    d = ImageDraw.Draw(im)
    bars = [("Kolkata", 98.6, orange), ("Iran", 161, (35, 60, 120)),
            ("Nigeria", 168.4, (46, 91, 155))]
    base = 600
    for i, (label, value, color) in enumerate(bars):
        x = 120 + i * 420
        height = int(value / 180 * 430)
        d.rectangle((x, base - height, x + 220, base), fill=color)
        d.text((x + 40, base - height - 55), f"{value:g}", font=bold, fill=ink)
        d.text((x + 45, base + 18), label, font=regular, fill=ink)
        d.text((x + 60, base + 55), f"n = {[432, 160, 96][i]}",
               font=small, fill=grey)
    d.line((80, base, 1340, base), fill=ink, width=2)
    d.text((40, 660), "Values are from three separate studies; see Table 3.1.",
           font=small, fill=grey)
    bars_path = FIG_DIR / "waiting_bars.png"
    im.save(bars_path)
    return arch, flow, bars_path


def _front_matter(doc, pages):
    # --- Cover ---------------------------------------------------------
    _para(doc, "A PROJECT REPORT ON", align=WD_ALIGN_PARAGRAPH.CENTER,
          indent=False, size=14, bold=True, after=10, spacing=1.0)
    _para(doc, TITLE, align=WD_ALIGN_PARAGRAPH.CENTER, indent=False,
          size=18, bold=True, after=14, spacing=1.0)
    _para(doc, "SUBMITTED TO MIT SCHOOL OF COMPUTING, LONI, PUNE IN PARTIAL "
          "FULFILLMENT OF THE REQUIREMENTS FOR THE AWARD OF THE DEGREE",
          align=WD_ALIGN_PARAGRAPH.CENTER, indent=False, size=12,
          after=12, spacing=1.0)
    _para(doc, "BACHELOR OF TECHNOLOGY", align=WD_ALIGN_PARAGRAPH.CENTER,
          indent=False, size=14, bold=True, after=4, spacing=1.0)
    _para(doc, "(COMPUTER SCIENCE AND ENGINEERING / INFORMATION TECHNOLOGY)",
          align=WD_ALIGN_PARAGRAPH.CENTER, indent=False, size=12,
          after=12, spacing=1.0)
    _para(doc, "BY", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False,
          size=14, bold=True, after=8, spacing=1.0)
    _plain_table(doc, ["Candidate Name", "PRN / Enrollment No"], TEAM,
                 [3.1, 2.9])
    _para(doc, "UNDER THE GUIDANCE OF", align=WD_ALIGN_PARAGRAPH.CENTER,
          indent=False, size=14, bold=True, before=14, after=6, spacing=1.0)
    _para(doc, GUIDE.upper(), align=WD_ALIGN_PARAGRAPH.CENTER, indent=False,
          size=14, bold=True, after=10, spacing=1.0)
    _logo(doc, "mit_seal.png", 1.5)
    for line in [
        "DEPARTMENT OF COMPUTER SCIENCE AND ENGINEERING",
        "MIT SCHOOL OF COMPUTING",
        "MIT ART, DESIGN AND TECHNOLOGY UNIVERSITY",
        "RAJBAUG CAMPUS, LONI-KALBHOR, PUNE 412201",
        YEAR,
    ]:
        _para(doc, line, align=WD_ALIGN_PARAGRAPH.CENTER, indent=False,
              size=12, after=4, spacing=1.0)

    # --- Front matter: Roman page numbers, no footer text --------------
    sec = doc.add_section(WD_SECTION.NEW_PAGE)
    _setup_section(sec)
    sec.different_first_page_header_footer = False
    _page_numbering(sec, "lowerRoman", 1)
    _footer(sec, text=None, with_page=True)

    _logo(doc, "mit_adt_logo.png", 2.2)
    _institution_block(doc, after=8)
    _para(doc, "CERTIFICATE", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False,
          size=16, bold=True, after=10, spacing=1.0)
    _para(doc, "This is to certify that the project report entitled",
          align=WD_ALIGN_PARAGRAPH.CENTER, indent=False, after=4, spacing=1.0)
    _para(doc, f"\u201c{TITLE}\u201d", align=WD_ALIGN_PARAGRAPH.CENTER,
          indent=False, bold=True, after=6, spacing=1.0)
    _para(doc, "submitted by the candidates listed below is a bonafide work "
          "carried out under the supervision of Dr. P. D. Patil and is "
          "submitted towards the partial fulfillment of the requirement of "
          "MIT Art, Design and Technology University, Pune, for the award of "
          "the degree of Bachelor of Technology (Computer Science and "
          "Engineering).", spacing=1.15)
    _para(doc, "", indent=False, after=2, spacing=1.0)
    _plain_table(doc, ["Candidate Name", "PRN / Enrollment No"], TEAM,
                 [3.1, 2.9])
    _para(doc, "", indent=False, after=8, spacing=1.0)
    _signatures(doc, ("Dr. P. D. Patil", "Guide"),
                ("____________________", "Head of Department"))
    _para(doc, "", indent=False, after=8, spacing=1.0)
    _signatures(doc, ("____________________", "Chief Coordinator-PBL"),
                ("____________________", "Dean"))
    seal = _para(doc, "", indent=False, align=WD_ALIGN_PARAGRAPH.LEFT,
                 after=0, spacing=1.0)
    seal.paragraph_format.tab_stops.add_tab_stop(Inches(2.3))
    seal.paragraph_format.tab_stops.add_tab_stop(Inches(4.3))
    _font(seal.add_run("Seal/Stamp of the College\tPlace: Pune\tDate:"), 12)

    # --- Company certificate -------------------------------------------
    _page_break(doc)
    _para(doc, "CERTIFICATE (FROM COMPANY IF ANY)",
          align=WD_ALIGN_PARAGRAPH.CENTER, indent=False, size=16, bold=True,
          after=18, spacing=1.0)
    _para(doc, "This is to certify that the project report entitled",
          align=WD_ALIGN_PARAGRAPH.CENTER, indent=False, after=8, spacing=1.5)
    _para(doc, f"\u201c{TITLE}\u201d", align=WD_ALIGN_PARAGRAPH.CENTER,
          indent=False, bold=True, after=10, spacing=1.5)
    _para(doc, "submitted by the team listed below was carried out under the "
          "supervision of Dr. P. D. Patil and has been completed "
          "successfully.")
    _para(doc, "", indent=False, after=6, spacing=1.0)
    _plain_table(doc, ["Candidate Name", "Enrollment No"], TEAM, [3.1, 2.9])
    _para(doc, "", indent=False, after=18, spacing=1.0)
    _signatures(doc, ("____________________", "External Guide"),
                ("", "Seal/Stamp of the Organisation"))
    _para(doc, "Place:                                            Date:",
          indent=False, align=WD_ALIGN_PARAGRAPH.LEFT, spacing=1.0)

    # --- Declaration ----------------------------------------------------
    _page_break(doc)
    _para(doc, "DECLARATION", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False,
          size=16, bold=True, after=18, spacing=1.0)
    _para(doc, "We, the team members whose names and enrollment numbers appear "
          "below, hereby declare that the project work incorporated in the "
          f"present project entitled \u201c{TITLE}\u201d is original work. "
          "This work, in part or in full, has not been submitted to any "
          "university for the award of a degree or diploma. We have properly "
          "acknowledged material collected from secondary sources wherever "
          "required, and we solely own the responsibility for the originality "
          "of the entire content.")
    _para(doc, "", indent=False, after=4, spacing=1.0)
    _plain_table(doc, ["Name", "Enrollment No"], TEAM, [3.1, 2.9])
    _para(doc, "", indent=False, after=16, spacing=1.0)
    _para(doc, "Date: 19/11/2026", indent=False,
          align=WD_ALIGN_PARAGRAPH.LEFT, after=12, spacing=1.0)
    _para(doc, "Name and Signature of the Team Members", indent=False,
          align=WD_ALIGN_PARAGRAPH.LEFT, bold=True, after=8, spacing=1.0)
    t = doc.add_table(rows=1, cols=4)
    _no_borders(t)
    labels = ["Member 1", "Member 2", "Member 3", "Member 4"]
    for i, label in enumerate(labels):
        cell = t.rows[0].cells[i]
        cell.text = ""
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_after = Pt(2)
        _font(p.add_run(f"{label}: __________"), 12)
        cell.width = Inches(1.5)
    _para(doc, "Name and Signature of Guide                     "
          "Seal/Stamp of the College", indent=False,
          align=WD_ALIGN_PARAGRAPH.LEFT, before=24, after=12, spacing=1.0)
    _para(doc, "Place: Pune                                     Date: 19/11/2026",
          indent=False, align=WD_ALIGN_PARAGRAPH.LEFT, spacing=1.0)

    # --- Examiner's approval -------------------------------------------
    _page_break(doc)
    _logo(doc, "mit_adt_logo.png", 2.4)
    _institution_block(doc, after=14)
    _para(doc, "EXAMINER\u2019S APPROVAL CERTIFICATE",
          align=WD_ALIGN_PARAGRAPH.CENTER, indent=False, size=16, bold=True,
          after=14, spacing=1.0)
    _para(doc, "The project report entitled \u201c" + TITLE + "\u201d submitted "
          "by the team listed below, in partial fulfillment for the award of "
          "the degree of Bachelor of Technology (Computer Science and "
          f"Engineering) during the academic year {YEAR}, of MIT Art, Design "
          "and Technology University, Pune, is hereby approved.")
    _para(doc, "", indent=False, after=6, spacing=1.0)
    _plain_table(doc, ["Name", "Enrollment No"], TEAM, [3.1, 2.9])
    _para(doc, "", indent=False, after=16, spacing=1.0)
    _para(doc, "Examiners:", indent=False, bold=True, after=10, spacing=1.0)
    _para(doc, "1. ______________________________", indent=False,
          align=WD_ALIGN_PARAGRAPH.LEFT, after=14, spacing=1.0)
    _para(doc, "2. ______________________________", indent=False,
          align=WD_ALIGN_PARAGRAPH.LEFT, spacing=1.0)

    # --- Acknowledgement ------------------------------------------------
    _page_break(doc)
    _para(doc, "ACKNOWLEDGEMENT", align=WD_ALIGN_PARAGRAPH.CENTER,
          indent=False, size=16, bold=True, after=18, spacing=1.0)
    _para(doc, "We thank our guide, Dr. P. D. Patil, Department of Computer "
          "Science and Engineering, MIT School of Computing, for his "
          "continuous support, guidance and encouragement throughout this "
          "project.")
    _para(doc, "We are grateful to the faculty and laboratory staff of MIT "
          "School of Computing for providing the resources and environment "
          "needed to build the Smart Hospital Patient Queue and Appointment "
          "Management System. We also thank our parents and friends for "
          "their constant support and understanding.")
    _para(doc, "", indent=False, after=14, spacing=1.0)
    _para(doc, "Name, Enrollment No.", indent=False, bold=True,
          align=WD_ALIGN_PARAGRAPH.RIGHT, after=4, spacing=1.0)
    _plain_table(doc, ["Name", "Enrollment No"], TEAM, [3.1, 2.9])

    # --- Abstract --------------------------------------------------------
    _page_break(doc)
    _para(doc, "ABSTRACT", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False,
          size=16, bold=True, after=18, spacing=1.0)
    _para(doc, "Outpatient departments often manage appointments and physical "
          "queues as separate processes. A paper token records a place in "
          "line but carries no status, no expected wait and no priority rule. "
          "Published studies from Kolkata, Iran and Nigeria report mean "
          "waiting times between 98.6 and 168.4 minutes in their respective "
          "samples [1]\u2013[3], but those studies measure waiting rather than "
          "test an integrated workflow.")
    _para(doc, "This project designs and implements a patient queue and "
          "appointment management system that brings booking, check-in, "
          "priority ordering, live tracking and consultation into one shared "
          "state. A React and TypeScript client serves patient, doctor and "
          "administrator roles; a Spring Boot API owns the appointment and "
          "queue state in MySQL; and a FastAPI sidecar provides a Random "
          "Forest wait-time estimate that is labelled ML or FALLBACK. Queue "
          "order is priority rank followed by check-in time, with FIFO inside "
          "each tier.")
    _para(doc, "The implementation was verified with 247 backend tests, 19 "
          "Python tests, 41 frontend tests and a successful production build. "
          "The model is trained on 1,200 synthetic demonstration rows, and "
          "priority tiers are clinician-assigned software categories. The "
          "project therefore reports software behaviour and invariants, not "
          "clinical outcomes or validated triage performance.")

    # --- Contents --------------------------------------------------------
    _page_break(doc)
    _para(doc, "CONTENTS", align=WD_ALIGN_PARAGRAPH.CENTER, indent=False,
          size=16, bold=True, after=14, spacing=1.0)
    for title, key in [
        ("Certificate", "certificate"),
        ("Certificate (From Company If Any)", "company_certificate"),
        ("Declaration", "declaration"),
        ("Examiner\u2019s Approval Certificate", "examiner"),
        ("Acknowledgement", "acknowledgement"),
        ("Abstract", "abstract"),
        ("List of Figures", "figures"),
        ("List of Tables", "tables"),
    ]:
        _dots(doc, title, pages[key])
    for number, title, key in TOC:
        _dots(doc, f"{number}  {title}", pages[key], bold=True, size=11)
        for sub_number, sub_title in SECTIONS[number]:
            _dots(doc, f"{sub_number}  {sub_title}",
                  pages.get(sub_number, ""), level=1, size=11)

    # --- List of figures --------------------------------------------------
    _page_break(doc)
    _para(doc, "LIST OF FIGURES", align=WD_ALIGN_PARAGRAPH.CENTER,
          indent=False, size=16, bold=True, after=14, spacing=1.0)
    for title, number in FIGURES:
        _dots(doc, title, pages.get(f"fig{number}", ""))

    # --- List of tables ---------------------------------------------------
    _page_break(doc)
    _para(doc, "LIST OF TABLES", align=WD_ALIGN_PARAGRAPH.CENTER,
          indent=False, size=16, bold=True, after=14, spacing=1.0)
    for title, number in TABLES:
        _dots(doc, title, pages.get(f"tbl{number}", ""))


FIGURES = [
    ("Figure 2.1: System architecture", "2.1"),
    ("Figure 2.2: Queue ordering rule", "2.2"),
    ("Figure 3.1: Reported mean outpatient wait", "3.1"),
    ("Figure 6.1: Patient portal", "6.1"),
    ("Figure 6.2: Doctor workspace", "6.2"),
    ("Figure 6.3: Administrator dashboard", "6.3"),
]

TABLES = [
    ("Table 3.1: Waiting-time evidence from outpatient studies", "3.1"),
    ("Table 4.1: Project schedule", "4.1"),
    ("Table 5.1: Functional requirements", "5.1"),
    ("Table 6.1: Verification results", "6.1"),
    ("Table 7.1: Test inventory", "7.1"),
    ("Table A.1: Model metadata", "A.1"),
]

TOC = [
    ("1.", "INTRODUCTION", "ch1"),
 ("2.", "CONCEPTS AND METHODS", "ch2"),
    ("3.", "LITERATURE SURVEY", "ch3"),
    ("4.", "PROJECT PLAN", "ch4"),
    ("5.", "SOFTWARE REQUIREMENT SPECIFICATION", "ch5"),
    ("6.", "IMPLEMENTATION AND RESULTS", "ch6"),
    ("7.", "SOFTWARE TESTING", "ch7"),
    ("8.", "CONCLUSION AND FUTURE WORK", "ch8"),
    ("", "REFERENCES", "refs"),
    ("", "ANNEXURE A: MODEL METADATA", "annexure"),
]

SECTIONS = {
    "1.": [("1.1", "Overview"), ("1.2", "Motivation"),
           ("1.3", "Problem Statement"), ("1.4", "Objectives"),
           ("1.5", "Scope")],
    "2.": [("2.1", "Definitions"), ("2.2", "System Architecture"),
           ("2.3", "Queue Ordering"), ("2.4", "Wait-Time Estimator"),
           ("2.5", "Method Summary")],
    "3.": [("3.1", "Waiting-Time Evidence"), ("3.2", "Gap"),
           ("3.3", "Standards and Technologies")],
    "4.": [("4.1", "Development Model"), ("4.2", "Task Schedule"),
           ("4.3", "Risk Management")],
    "5.": [("5.1", "Functional Requirements"),
           ("5.2", "User Classes and Characteristics"),
           ("5.3", "Non-Functional Requirements"),
           ("5.4", "Assumptions and Constraints")],
    "6.": [("6.1", "Technology Stack"), ("6.2", "Implemented Role Views"),
           ("6.3", "Verification Results")],
    "7.": [("7.1", "Test Strategy"), ("7.2", "Test Inventory"),
           ("7.3", "Invariants and Limitations")],
    "8.": [("8.1", "Conclusions"), ("8.2", "Future Work")],
    "": [],
}

# Placeholder page numbers for the first pass. The second pass overwrites them
# with the numbers read back from the rendered PDF (see sync_pages.py).
DEFAULT_PAGES = {
    "certificate": "i", "company_certificate": "ii", "declaration": "iii",
    "examiner": "iv", "acknowledgement": "v", "abstract": "vi",
    "figures": "viii", "tables": "ix",
    "ch1": "1", "ch2": "2", "ch3": "4", "ch4": "5", "ch5": "6",
    "ch6": "7", "ch7": "9", "ch8": "10", "refs": "11", "annexure": "12",
    "1.1": "1", "1.2": "1", "1.3": "1", "1.4": "1", "1.5": "2",
    "2.1": "2", "2.2": "2", "2.3": "3", "2.4": "3", "2.5": "3",
    "3.1": "4", "3.2": "4", "3.3": "4",
    "4.1": "5", "4.2": "5", "4.3": "5",
    "5.1": "6", "5.2": "6", "5.3": "6", "5.4": "7",
    "6.1": "7", "6.2": "7", "6.3": "9",
    "7.1": "9", "7.2": "10", "7.3": "10",
    "8.1": "10", "8.2": "10",
    "fig2.1": "2", "fig2.2": "3", "fig3.1": "4",
    "fig6.1": "8", "fig6.2": "8", "fig6.3": "9",
    "tbl3.1": "4", "tbl4.1": "5", "tbl5.1": "6",
    "tbl6.1": "9", "tbl7.1": "10", "tblA.1": "12",
}


def _main(doc, figs, pages):
    arch, flow, bars = figs
    sec = doc.add_section(WD_SECTION.NEW_PAGE)
    _setup_section(sec)
    sec.different_first_page_header_footer = False
    _page_numbering(sec, "decimal", 1)
    _footer(sec, text=FRONT, with_page=True)
    doc.sections[0].different_first_page_header_footer = False

    # Chapter 1
    _chapter(doc, "1.  INTRODUCTION", before=0)
    _section(doc, "1.1  Overview")
    _body(doc, "Smart Hospital is a patient queue and appointment management "
          "system for an outpatient department. It replaces the split between "
          "an appointment book and a paper token queue with one shared state "
          "that patients can see, doctors can move and administrators can "
          "audit. The prototype covers three roles: patient, doctor and "
          "administrator.")
    _section(doc, "1.2  Motivation")
    _body(doc, "Long outpatient waits are a documented problem. Datta reported "
          "a mean registration-to-chamber wait of 98.6 minutes with a "
          "6.5-minute consultation in a Kolkata tertiary hospital sample of "
          "432 outpatients [1]. Mohebbifar et al. reported a mean of 161 "
          "minutes across 160 outpatients in Iran [2], while Oche and Adamu "
          "reported 168.4 minutes from 96 responses in Nigeria [3]. These "
          "figures come from different health systems and samples; they "
          "establish that waiting is measurable, not that this project "
          "reduces it.")
    _section(doc, "1.3  Problem Statement")
    _body(doc, "Manual queues do not encode priority, do not give patients a "
          "visible position or expected wait, and leave a weak audit trail "
          "when staff reorder the line. Paper tokens communicate a number but "
          "not the state of the queue. The project addresses this software "
          "gap rather than the clinical question of how patients should be "
          "triaged.")
    _section(doc, "1.4  Objectives")
    objectives = [
        "To provide one shared appointment and queue state for all three roles.",
        "To issue a digital token at check-in and display position and "
        "estimated wait.",
        "To order the queue by clinician-assigned priority tier and check-in "
        "time.",
        "To record doctor actions, including overrides, in an audit trail.",
        "To provide a labelled wait-time estimate with a deterministic "
        "fallback.",
    ]
    for i, objective in enumerate(objectives, 1):
        _para(doc, f"{i}.  {objective}", before=6, after=0, left=0.25,
              indent=False)
    _section(doc, "1.5  Scope")
    _body(doc, "The scope includes registration and role-based access, "
          "department and doctor discovery, conflict-safe slot booking, "
          "digital check-in, live queue tracking, doctor queue actions and "
          "administrative management. It excludes clinical triage validation, "
          "electronic health record integration, payments, real patient "
          "records and hospital-scale load testing.")

    # Chapter 2
    _chapter(doc, "2.  CONCEPTS AND METHODS")
    _section(doc, "2.1  Definitions")
    _body(doc, "An appointment is a reserved slot with a doctor and time. A "
          "token is the digital queue identifier issued at check-in. A "
          "priority tier is a clinician-assigned software category: CRITICAL, "
          "HIGH, MEDIUM or NORMAL. Check-in time is the timestamp used to "
          "break ties inside a tier. An estimate is a predicted wait in "
          "minutes, labelled ML when produced by the model and FALLBACK when "
          "produced by the deterministic rule.")
    _section(doc, "2.2  System Architecture")
    _body(doc, "The client is a React 19 and TypeScript single-page "
          "application with three role-based views. It communicates over REST "
          "with a Java 21 / Spring Boot 3.5 API that owns authentication, "
          "appointments, queue state and the audit log. MySQL 8.4 stores the "
          "data, Flyway manages migrations, and a Python FastAPI sidecar "
          "exposes the wait-time model. Docker Compose runs the complete stack "
          "locally. Figure 2.1 shows the arrangement.")
    _figure(doc, arch, "Figure 2.1: System architecture", width=5.2)
    _section(doc, "2.3  Queue Ordering")
    _body(doc, "The queue is ordered by priority rank and then by check-in "
          "time. CRITICAL has rank one, HIGH rank two, MEDIUM rank three and "
          "NORMAL rank four. Inside a tier the order is first-in-first-out. A "
          "doctor may serve, skip, complete or override a token; an override "
          "is recorded with an audit note and every waiting token receives a "
          "recalculated position and estimate.")
    _figure(doc, flow, "Figure 2.2: Queue ordering rule", width=5.2)
    _section(doc, "2.4  Wait-Time Estimator")
    _body(doc, "The sidecar uses a RandomForestRegressor with eight features: "
          "queue position, people waiting, average consultation minutes, "
          "active emergency count, triage tier, hour of day, day of week and "
          "minutes from scheduled start. The model version is "
          "waiting-time-rf-v1, trained with seed 41 on 1,200 synthetic "
          "demonstration rows. It is a decision aid only. If the sidecar is "
          "unavailable, the API returns a deterministic estimate labelled "
          "FALLBACK.")
    _section(doc, "2.5  Method Summary")
    _body(doc, "The contribution is a working workflow and a set of tested "
          "software invariants. No clinical outcome, time saving or triage "
          "accuracy is claimed, because the system has not been evaluated "
          "with real patient data.")

    # Chapter 3
    _chapter(doc, "3.  LITERATURE SURVEY")
    _section(doc, "3.1  Waiting-Time Evidence")
    _body(doc, "Three outpatient studies provide the evidence base for the "
          "problem statement. Their sample sizes, settings and reported means "
          "differ, so they are presented as separate observations rather than "
          "pooled.")
    _caption(doc, "Table 3.1: Waiting-time evidence from outpatient studies", keep_next=True)
    _grid_table(doc,
                ["Study", "Setting and sample", "Reported result"],
                [
                    ["Datta, 2024 [1]",
                     "Kolkata tertiary hospital; 432 outpatients",
                     "Mean wait 98.6 min; consultation 6.5 min"],
                    ["Mohebbifar et al., 2013 [2]",
                     "Iran; 160 outpatients",
                     "Mean wait 161 min; clinic-level variation"],
                    ["Oche and Adamu, 2013 [3]",
                     "Nigeria; 96 responses",
                     "Mean wait 168.4 min; consultation 7.1 min"],
                ], [2.2, 1.8, 2.0])
    _para(doc, "", indent=False, after=0, spacing=1.0)
    _figure(doc, bars, "Figure 3.1: Reported mean outpatient wait", width=5.4)
    _section(doc, "3.2  Gap")
    _body(doc, "The cited studies quantify waiting. They do not evaluate a "
          "live booking, token, priority and forecast workflow in one system. "
          "Smart Hospital addresses that software gap and evaluates software "
          "behaviour only.")
    _section(doc, "3.3  Standards and Technologies")
    _body(doc, "The implementation follows the official documentation of "
          "Spring Boot [4], FastAPI [5], scikit-learn [6] and MySQL [7]. "
          "Problem responses follow RFC 9457 for HTTP APIs [8]. These sources "
          "define interfaces and behavior; they do not provide clinical "
          "evidence.")

    # Chapter 4
    _chapter(doc, "4.  PROJECT PLAN")
    _section(doc, "4.1  Development Model")
    _body(doc, "The work followed a waterfall structure with iterative "
          "testing inside each phase. Requirements were fixed first, followed "
          "by architecture, implementation, verification and documentation. "
          "Test suites were run after each major change rather than only at "
          "the end.")
    _section(doc, "4.2  Task Schedule")
    _caption(doc, "Table 4.1: Project schedule", keep_next=True)
    _grid_table(doc,
                ["Phase", "Deliverable", "Duration"],
                [
                    ["Requirements", "Scope, objectives and SRS", "2 weeks"],
                    ["Architecture", "Service boundaries and data model",
                     "2 weeks"],
                    ["Implementation", "Client, API, database and ML sidecar",
                     "6 weeks"],
                    ["Verification", "JUnit, pytest, Vitest and build",
                     "3 weeks"],
                    ["Documentation", "Report, slides and references",
                     "2 weeks"],
                ], [1.5, 3.3, 1.2])
    _para(doc, "", indent=False, after=0, spacing=1.0)
    _section(doc, "4.3  Risk Management")
    _body(doc, "The main technical risk was model unavailability. It is "
          "handled by a deterministic fallback that labels its source. The "
          "main scope risk was overstating clinical value; it is handled by "
          "limiting claims to software behaviour and by labelling training "
          "data as synthetic.")

    # Chapter 5
    _chapter(doc, "5.  SOFTWARE REQUIREMENT SPECIFICATION")
    _section(doc, "5.1  Functional Requirements")
    _caption(doc, "Table 5.1: Functional requirements", keep_next=True)
    _grid_table(doc,
                ["ID", "Requirement"],
                [
                    ["FR-01", "A user can register with a role and "
                     "authenticate with JWT."],
                    ["FR-02", "A patient can browse departments and doctors."],
                    ["FR-03", "A patient can book a conflict-safe appointment "
                     "slot."],
                    ["FR-04", "A patient can check in and receive a digital "
                     "token."],
                    ["FR-05", "A patient can view token position and "
                     "estimated wait."],
                    ["FR-06", "A doctor can call the next token and assign a "
                     "priority tier."],
                    ["FR-07", "A doctor can serve, skip, complete or override "
                     "a token."],
                    ["FR-08", "Overrides are stored with an audit note."],
                    ["FR-09", "An administrator can manage users, doctors and "
                     "schedules."],
                    ["FR-10", "Every estimate is labelled ML or FALLBACK."],
                ], [1.1, 4.9])
    _para(doc, "", indent=False, after=0, spacing=1.0)
    _section(doc, "5.2  User Classes and Characteristics")
    _sub(doc, "5.2.1  Patient")
    _body(doc, "The patient books a slot, checks in, carries a digital token "
          "and tracks position and estimated wait. The patient cannot change "
          "queue order.")
    _sub(doc, "5.2.2  Doctor")
    _body(doc, "The doctor views the live queue, calls the next patient, "
          "assigns priority within hospital policy, and records serves, "
          "skips, completions and overrides.")
    _sub(doc, "5.2.3  Administrator")
    _body(doc, "The administrator manages users, doctors, schedules and the "
          "operational view, and can inspect the same queue state that the "
          "other roles see.")
    _section(doc, "5.3  Non-Functional Requirements")
    for item in [
        "Access control is enforced by role-based JWT checks on protected "
        "routes.",
        "Concurrent check-in must not create duplicate tokens.",
        "Queue changes must produce a new position and estimate for waiting "
        "tokens.",
        "Model absence must not break the queue; the fallback must be "
        "labelled.",
        "The prototype uses synthetic demonstration data and no real patient "
        "records.",
    ]:
        _para(doc, item, before=6, after=0, left=0.25, indent=False)
    _section(doc, "5.4  Assumptions and Constraints")
    _body(doc, "The prototype assumes a single hospital and a single outpatient "
          "department per deployment. Priority tiers are entered by clinical "
          "staff, so the software does not decide medical urgency. The "
          "wait-time model is a decision aid trained on synthetic data, and "
          "the system requires a network connection between the client, the "
          "API and the model sidecar. Real patient records, payments and "
          "insurance workflows are outside the current scope.")

    # Chapter 6
    _chapter(doc, "6.  IMPLEMENTATION AND RESULTS")
    _section(doc, "6.1  Technology Stack")
    _body(doc, "The experience layer uses React 19, TypeScript, Vite, Tailwind "
          "CSS and TanStack Query. The service layer uses Java 21, Spring Boot "
          "3.5, Spring Security, JWT and Flyway. MySQL 8.4 is the data store. "
          "The intelligence layer uses Python, FastAPI and scikit-learn. JUnit "
          "5, pytest, Vitest and Docker Compose support verification and local "
          "runtime.")
    _section(doc, "6.2  Implemented Role Views")
    _body(doc, "The patient portal shows the active token, position and "
          "estimate. The doctor workspace shows today's appointments, waiting "
          "count and the operating protocol. The administrator dashboard "
          "shows staffing, department load and infrastructure telemetry. The "
          "figures below are screenshots from the working build and use "
          "demonstration data.")
    _figure(doc, ASSETS / "patient.png", "Figure 6.1: Patient portal",
            width=5.9)
    _figure(doc, ASSETS / "doctor.png", "Figure 6.2: Doctor workspace",
            width=5.9)
    _figure(doc, ASSETS / "admin.png", "Figure 6.3: Administrator dashboard",
            width=5.9)
    _section(doc, "6.3  Verification Results")
    _caption(doc, "Table 6.1: Verification results", keep_next=True)
    _grid_table(doc,
                ["Check", "Result", "Notes"],
                [
                    ["Backend JUnit 5 suite", "247 tests",
                     "0 failures, 0 errors"],
                    ["ML pytest suite", "19 passed", "FastAPI and model tests"],
                    ["Frontend Vitest suite", "41 passed",
                     "Role views and queue tests"],
                    ["Production build", "Passed",
                     "TypeScript and Vite build"],
                ], [2.0, 1.2, 2.8])
    _para(doc, "", indent=False, after=0, spacing=1.0)
    _body(doc, "All suites were run on 19 November 2026. These results verify "
          "software behaviour. They are not evidence of clinical "
          "effectiveness.")

    # Chapter 7
    _chapter(doc, "7.  SOFTWARE TESTING")
    _section(doc, "7.1  Test Strategy")
    _body(doc, "Testing covers units, services, role isolation, concurrency "
          "and the patient journey. Backend tests use JUnit 5 with Spring "
          "Boot test support; the ML service uses pytest; the frontend uses "
          "Vitest with Testing Library. The production build is treated as a "
          "release check.")
    _section(doc, "7.2  Test Inventory")
    _caption(doc, "Table 7.1: Test inventory", keep_next=True)
    _grid_table(doc,
                ["Layer", "Tool", "Focus"],
                [
                    ["Backend", "JUnit 5",
                     "Queue service, roles, appointments, audit"],
                    ["Concurrency", "JUnit 5",
                     "Unique token under simultaneous check-in"],
                    ["End-to-end", "JUnit 5",
                     "Patient journey from booking to consultation"],
                    ["ML service", "pytest",
                     "Model contract and fallback labelling"],
                    ["Frontend", "Vitest",
                     "Role views and queue interactions"],
                ], [1.4, 1.2, 3.3])
    _para(doc, "", indent=False, after=0, spacing=1.0)
    _section(doc, "7.3  Invariants and Limitations")
    _body(doc, "The critical invariants are unique token creation, priority "
          "order with FIFO inside a tier, role isolation on protected routes, "
          "and explicit ML or FALLBACK labelling. The evaluation does not "
          "include real patient data, clinical validation, hospital-scale "
          "load testing or a measured time saving.")

    # Chapter 8
    _chapter(doc, "8.  CONCLUSION AND FUTURE WORK")
    _section(doc, "8.1  Conclusions")
    _body(doc, "Smart Hospital connects booked appointments to a visible "
          "consultation queue. Patients receive a token and an estimate, "
          "doctors run the live queue and record overrides, and "
          "administrators manage the operational data behind both views. The "
          "implementation passes its test suites and production build, and "
          "the model degrades to a labelled fallback when unavailable.")
    _section(doc, "8.2  Future Work")
    _body(doc, "The next step is a supervised pilot with real operational "
          "data. Future work includes SMS and push notifications, mobile "
          "access, multi-hospital support, monitored models trained on real "
          "data, and an evaluation that measures waiting-time change and "
          "staff workflow. Until that work is done, the system should be "
          "described as a working prototype, not a validated clinical "
          "service.")

    # References
    _page_break(doc)
    _chapter(doc, "REFERENCES", before=0)
    refs = [
        "[1] M. Datta, \u201cAssessment of outdoor patients waiting time and "
        "consultation time in a tertiary care hospital of Kolkata,\u201d "
        "Indian J. Community Med., vol. 49, no. Suppl 1, p. S61, 2024, "
        "doi: 10.4103/ijcm.ijcm_abstract210.",
        "[2] R. Mohebbifar, E. Hasanpoor, M. Mohseni, et al., \u201cOutpatient "
        "waiting time in health services and teaching hospitals: a case study "
        "in Iran,\u201d Glob. J. Health Sci., vol. 6, no. 1, pp. 172\u2013180, "
        "2013, doi: 10.5539/gjhs.v6n1p172.",
        "[3] M. O. Oche and H. Adamu, \u201cDeterminants of patient waiting "
        "time in the general outpatient department of a tertiary health "
        "institution in north-western Nigeria,\u201d Ann. Med. Health Sci. "
        "Res., vol. 3, no. 4, pp. 588\u2013592, 2013, "
        "doi: 10.4103/2141-9248.122123.",
        "[4] Broadcom, Spring Boot 3.5.16 Reference Documentation. [Online]. "
        "Available: https://docs.spring.io/spring-boot/3.5.16/reference/"
        "index.html. Accessed: 19 Nov. 2026.",
        "[5] FastAPI, FastAPI Documentation. [Online]. Available: "
        "https://fastapi.tiangolo.com/. Accessed: 19 Nov. 2026.",
        "[6] scikit-learn, scikit-learn User Guide. [Online]. Available: "
        "https://scikit-learn.org/stable/user_guide.html. Accessed: 19 Nov. "
        "2026.",
        "[7] Oracle, MySQL 8.4 Reference Manual. [Online]. Available: "
        "https://dev.mysql.com/doc/refman/8.4/en/. Accessed: 19 Nov. 2026.",
        "[8] M. Nottingham, E. Wilde, and S. Dalal, RFC 9457: Problem Details "
        "for HTTP APIs. IETF, July 2023. [Online]. Available: "
        "https://www.rfc-editor.org/rfc/rfc9457.html.",
    ]
    for ref in refs:
        p = _para(doc, ref, indent=False, align=WD_ALIGN_PARAGRAPH.JUSTIFY,
                  after=8, spacing=1.0, left=0.35)
        p.paragraph_format.first_line_indent = Inches(-0.35)

    # Annexure
    _page_break(doc)
    _chapter(doc, "ANNEXURE A: MODEL METADATA", before=0)
    _body(doc, "The wait-time estimator is described by the following metadata "
          "recorded with the trained artefact. It is included to make the "
          "provenance of the model explicit and to support the limitation "
          "stated in Chapter 2.")
    _caption(doc, "Table A.1: Model metadata", keep_next=True)
    _grid_table(doc,
                ["Field", "Value"],
                [
                    ["Model type", "RandomForestRegressor"],
                    ["Model version", "waiting-time-rf-v1"],
                    ["Training rows", "1,200"],
                    ["Training data", "synthetic-demo-data"],
                    ["Seed", "41"],
                    ["Feature schema version", "1"],
                    ["Features", "queuePosition, peopleWaiting, "
                     "averageConsultationMinutes, activeEmergencyCount, "
                     "triageTier, hourOfDay, dayOfWeek, "
                     "minutesFromScheduledStart"],
                ], [2.1, 3.9])
    _section(doc, "A.2  Feature Definitions")
    features = [
        ("queuePosition", "position of the token in the live queue."),
        ("peopleWaiting", "number of patients waiting at request time."),
        ("averageConsultationMinutes", "mean consultation length used as the "
         "service-rate input."),
        ("activeEmergencyCount", "number of active emergency cases."),
        ("triageTier", "NORMAL, MEDIUM, HIGH or CRITICAL, mapped to an "
         "ordinal value before prediction."),
        ("hourOfDay", "request hour on a 24-hour clock."),
        ("dayOfWeek", "day of the week from 1 to 7."),
        ("minutesFromScheduledStart", "minutes elapsed since the appointment "
         "slot start."),
    ]
    for name, description in features:
        _para(doc, f"{name}: {description}", before=2, after=0, left=0.25,
              indent=False, spacing=1.15)
    _section(doc, "A.3  Response Contract")
    _body(doc, "The service returns estimatedWaitMinutes, confidence, "
          "modelVersion, featureSchemaVersion and trainingDataProvenance. "
          "Confidence is an agreement score from the spread of the forest's "
          "trees, not a calibrated probability; out-of-range inputs are "
          "rejected before prediction.")


def build(pages: dict[str, str], output: Path = OUTPUT) -> Path:
    figs = _diagrams()
    doc = Document()
    normal = doc.styles["Normal"]
    normal.font.name = "Times New Roman"
    normal.font.size = Pt(12)
    normal.paragraph_format.space_before = Pt(10)
    normal.paragraph_format.space_after = Pt(0)
    normal.paragraph_format.line_spacing = 1.5
    normal.paragraph_format.first_line_indent = Inches(0.5)
    normal.paragraph_format.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY

    cover = doc.sections[0]
    _setup_section(cover)
    cover.different_first_page_header_footer = False
    _footer(cover, text=None, with_page=False)

    _front_matter(doc, pages)
    _main(doc, figs, pages)
    output.parent.mkdir(parents=True, exist_ok=True)
    doc.save(str(output))
    return output


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--pages", type=Path,
                        help="JSON map of TOC keys to page numbers")
    parser.add_argument("--output", type=Path, default=OUTPUT)
    args = parser.parse_args()
    pages = dict(DEFAULT_PAGES)
    if args.pages:
        pages.update(json.loads(args.pages.read_text()))
    print(build(pages, args.output))


if __name__ == "__main__":
    main()

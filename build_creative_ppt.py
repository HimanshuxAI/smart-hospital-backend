#!/usr/bin/env python3
"""Build the editorial Smart Hospital deck on the supplied MITSoC template."""

from __future__ import annotations

import argparse
import hashlib
import io
from pathlib import Path
from typing import Sequence

from PIL import Image
from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_CONNECTOR, MSO_SHAPE, MSO_SHAPE_TYPE
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN
from pptx.util import Inches, Pt

ROOT = Path(__file__).resolve().parent
TEMPLATE = ROOT / "assets" / "Final_Project_Presentation_format.pptx"
OUTPUT = ROOT / "Smart_Hospital_Presentation_Final.pptx"

# The UI screenshots use cream, orange, lime, and black. The deck borrows those
# colours so the product shots read as part of the presentation, not pasted on.
INK = RGBColor(0x10, 0x12, 0x14)
BLACK = RGBColor(0x08, 0x09, 0x0A)
NAVY = RGBColor(0x16, 0x27, 0x57)
ORANGE = RGBColor(0xFF, 0x4E, 0x2A)
LIME = RGBColor(0xD8, 0xF3, 0x69)
CREAM = RGBColor(0xF3, 0xEC, 0xDC)
PAPER = RGBColor(0xFA, 0xF7, 0xF0)
MUTED = RGBColor(0x6C, 0x68, 0x61)
RULE = RGBColor(0xD7, 0xD2, 0xC7)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
RED = RGBColor(0xD7, 0x1F, 0x2D)
BLUE = RGBColor(0x2E, 0x5B, 0x9B)

DISPLAY = "Bodoni 72"
BODY = "Avenir Next"
CONDENSED = "Avenir Next Condensed"
MONO = "Menlo"

MARGIN = 0.52
FOOTER_TEXT = "Department of Computer Science & Engineering"

# One approved type scale. Sizes are snapped to these steps so no slide has
# stray 6.5 pt captions next to 138 pt numbers.
TYPE_SCALE = (7.5, 9, 10, 11, 12, 13, 18, 26, 30, 36, 48, 54)


def _snap_size(size: float) -> float:
    for step in TYPE_SCALE:
        if size <= step + 0.01:
            return step
    return TYPE_SCALE[-1]


def _snap_type(prs: Presentation) -> None:
    for slide in prs.slides:
        for shape in slide.shapes:
            if not getattr(shape, "has_text_frame", False):
                continue
            for paragraph in shape.text_frame.paragraphs:
                for run in paragraph.runs:
                    if run.font.size is not None:
                        run.font.size = Pt(_snap_size(run.font.size.pt))


def _normalize_footer(prs: Presentation) -> None:
    for slide in prs.slides:
        for shape in slide.shapes:
            if not getattr(shape, "has_text_frame", False) or shape.top is None:
                continue
            if shape.top < Inches(6.15) or FOOTER_TEXT not in shape.text:
                continue
            for paragraph in shape.text_frame.paragraphs:
                for run in paragraph.runs:
                    run.text = run.text.replace("MITSoE", "MITSOC").replace("MITSoC", "MITSOC")


def _name(shape, name: str):
    shape.name = name
    return shape


def _rect(
    slide,
    x: float,
    y: float,
    w: float,
    h: float,
    fill: RGBColor | None = WHITE,
    line: RGBColor | None = None,
    line_width: float = 0.75,
    name: str = "content_shape",
):
    shape = slide.shapes.add_shape(
        MSO_SHAPE.RECTANGLE, Inches(x), Inches(y), Inches(w), Inches(h)
    )
    if fill is None:
        shape.fill.background()
    else:
        shape.fill.solid()
        shape.fill.fore_color.rgb = fill
    if line is None:
        shape.line.fill.background()
    else:
        shape.line.color.rgb = line
        shape.line.width = Pt(line_width)
    shape.shadow.inherit = False
    return _name(shape, name)


def _circle(
    slide,
    x: float,
    y: float,
    d: float,
    fill: RGBColor | None = None,
    line: RGBColor | None = BLACK,
    line_width: float = 1.0,
    name: str = "content_circle",
):
    shape = slide.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x), Inches(y), Inches(d), Inches(d))
    if fill is None:
        shape.fill.background()
    else:
        shape.fill.solid()
        shape.fill.fore_color.rgb = fill
    if line is None:
        shape.line.fill.background()
    else:
        shape.line.color.rgb = line
        shape.line.width = Pt(line_width)
    shape.shadow.inherit = False
    return _name(shape, name)


def _line(
    slide,
    x1: float,
    y1: float,
    x2: float,
    y2: float,
    color: RGBColor = BLACK,
    width: float = 1.0,
    name: str = "content_line",
):
    shape = slide.shapes.add_connector(
        MSO_CONNECTOR.STRAIGHT, Inches(x1), Inches(y1), Inches(x2), Inches(y2)
    )
    shape.line.color.rgb = color
    shape.line.width = Pt(width)
    return _name(shape, name)


def _arrow(
    slide,
    x: float,
    y: float,
    w: float = 0.42,
    h: float = 0.24,
    color: RGBColor = ORANGE,
    name: str = "content_arrow",
):
    shape = slide.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW, Inches(x), Inches(y), Inches(w), Inches(h))
    shape.fill.solid()
    shape.fill.fore_color.rgb = color
    shape.line.fill.background()
    shape.shadow.inherit = False
    return _name(shape, name)


def _char_spacing(run, points: float) -> None:
    run.font._element.set("spc", str(int(points * 100)))


def _text(
    slide,
    x: float,
    y: float,
    w: float,
    h: float,
    text: str,
    size: float,
    color: RGBColor = INK,
    font: str = BODY,
    bold: bool = False,
    italic: bool = False,
    align=PP_ALIGN.LEFT,
    anchor=MSO_ANCHOR.TOP,
    line_spacing: float = 1.0,
    spacing: float = 0,
    name: str = "content_text",
):
    box = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.clear()
    tf.word_wrap = True
    tf.margin_left = 0
    tf.margin_right = 0
    tf.margin_top = 0
    tf.margin_bottom = 0
    tf.vertical_anchor = anchor
    p = tf.paragraphs[0]
    p.alignment = align
    p.line_spacing = line_spacing
    p.space_before = Pt(0)
    p.space_after = Pt(0)
    run = p.add_run()
    run.text = text
    run.font.name = font
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    run.font.color.rgb = color
    if spacing:
        _char_spacing(run, spacing)
    return _name(box, name)


def _rich(
    slide,
    x: float,
    y: float,
    w: float,
    h: float,
    segments: Sequence[tuple[str, dict]],
    size: float = 12,
    align=PP_ALIGN.LEFT,
    anchor=MSO_ANCHOR.TOP,
    line_spacing: float = 1.0,
    name: str = "content_text",
):
    box = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.clear()
    tf.word_wrap = True
    tf.margin_left = 0
    tf.margin_right = 0
    tf.margin_top = 0
    tf.margin_bottom = 0
    tf.vertical_anchor = anchor
    p = tf.paragraphs[0]
    p.alignment = align
    p.line_spacing = line_spacing
    p.space_before = Pt(0)
    p.space_after = Pt(0)
    for text, overrides in segments:
        run = p.add_run()
        run.text = text
        run.font.name = overrides.get("font", BODY)
        run.font.size = Pt(overrides.get("size", size))
        run.font.bold = overrides.get("bold", False)
        run.font.italic = overrides.get("italic", False)
        run.font.color.rgb = overrides.get("color", INK)
        if overrides.get("spacing"):
            _char_spacing(run, overrides["spacing"])
    return _name(box, name)


def _label(
    slide,
    x: float,
    y: float,
    w: float,
    text: str,
    color: RGBColor = ORANGE,
    size: float = 7.5,
    name: str = "content_label",
):
    return _text(
        slide,
        x,
        y,
        w,
        0.18,
        text.upper(),
        size,
        color,
        CONDENSED,
        bold=True,
        spacing=1.4,
        name=name,
    )


def _image(path: Path, focus: tuple[int, int, int, int] | None, ratio: float) -> io.BytesIO:
    with Image.open(path) as image:
        image = image.convert("RGB")
        if focus:
            image = image.crop(focus)
        width, height = image.size
        if width / height > ratio:
            new_width = int(height * ratio)
            left = (width - new_width) // 2
            image = image.crop((left, 0, left + new_width, height))
        else:
            new_height = int(width / ratio)
            top = (height - new_height) // 2
            image = image.crop((0, top, width, top + new_height))
        buffer = io.BytesIO()
        image.save(buffer, format="PNG")
        buffer.seek(0)
        return buffer


def _screenshot(
    slide,
    key: str,
    x: float,
    y: float,
    w: float,
    h: float,
    focus: tuple[int, int, int, int] | None = None,
    shadow: bool = True,
    name: str = "content_shot",
):
    paths = {
        "patient": ROOT / "assets" / "patient.png",
        "doctor": ROOT / "assets" / "doctor.png",
        "admin": ROOT / "assets" / "admin.png",
    }
    if shadow:
        _rect(slide, x + 0.09, y + 0.09, w, h, BLACK, None, name=f"{name}_shadow")
    picture = slide.shapes.add_picture(
        _image(paths[key], focus, w / h), Inches(x), Inches(y), Inches(w), Inches(h)
    )
    _name(picture, name)
    _rect(slide, x, y, w, h, None, BLACK, 0.9, name=f"{name}_frame")
    return picture


def _footer_signature(slide) -> tuple[str, str | None]:
    footer_text = ""
    logo_hash = None
    for shape in slide.shapes:
        text = shape.text.strip() if getattr(shape, "has_text_frame", False) else ""
        in_footer = shape.top is not None and shape.top >= Inches(6.15)
        if in_footer and FOOTER_TEXT in text:
            footer_text = text
        if in_footer and shape.shape_type == MSO_SHAPE_TYPE.PICTURE:
            logo_hash = hashlib.sha256(shape.image.blob).hexdigest()
    return footer_text, logo_hash


def _footer_baseline(prs: Presentation) -> list[tuple[str, str | None]]:
    return [_footer_signature(slide) for slide in prs.slides]


def _validate_footer(signature: list[tuple[str, str | None]]) -> None:
    for slide_number, (text, logo_hash) in enumerate(signature, 1):
        if not text or not logo_hash:
            raise RuntimeError(f"Footer or logo missing on slide {slide_number}")


def strip_body(slide) -> None:
    for shape in list(slide.shapes):
        if shape.top is not None and shape.top >= Inches(6.15):
            continue
        shape._element.getparent().remove(shape._element)


def _page(slide, number: str, section: str, dark: bool = False):
    _text(
        slide,
        0.55,
        0.34,
        1.2,
        0.2,
        number,
        8.5,
        ORANGE,
        CONDENSED,
        bold=True,
        spacing=1.8,
        name="content_page",
    )
    _text(
        slide,
        5.55,
        0.34,
        4.0,
        0.2,
        section.upper(),
        7.2,
        WHITE if dark else MUTED,
        CONDENSED,
        bold=True,
        align=PP_ALIGN.RIGHT,
        spacing=1.5,
        name="content_section",
    )


def _headline(slide, text: str, x: float, y: float, w: float, h: float, size: float = 38):
    return _text(
        slide,
        x,
        y,
        w,
        h,
        text,
        size,
        INK,
        DISPLAY,
        bold=False,
        line_spacing=0.94,
        name="content_headline",
    )


def _rule(slide, x: float, y: float, w: float, color: RGBColor = BLACK, thickness: float = 0.025):
    return _rect(slide, x, y, w, thickness, color, None, name="content_rule")


def _body(slide, text: str, x: float, y: float, w: float, h: float, size: float = 10.5):
    return _text(
        slide, x, y, w, h, text, size, MUTED, BODY, line_spacing=1.2, name="content_body"
    )


def _source(slide, text: str, y: float = 5.9):
    _text(slide, 0.55, y, 8.9, 0.18, text, 6.8, MUTED, BODY, italic=True, name="content_source")


def _deck(slide, text, y=1.48, w=8.9, size=11):
    return _text(slide, 0.55, y, w, 0.32, text, size, MUTED, BODY, name="content_intro")


def _card(slide, x, y, w, h, fill=WHITE, line=RULE):
    return _rect(slide, x, y, w, h, fill, line, 0.8, name="content_card")


def _takeaway(slide, text, y=5.02, h=0.68, size=12):
    _rect(slide, 0.55, y, 8.9, h, BLACK, None, name="content_strip")
    _text(slide, 0.75, y + 0.2, 8.5, 0.3, text, size, WHITE, BODY, bold=True, name="content_strip_text")


def slide_cover(slide):
    _label(slide, 0.55, 0.42, 5.2, "SY2207  /  Project presentation  /  2026–27", INK, 7.5)
    _text(slide, 0.5, 0.92, 5.2, 0.72, "SMART", 48, BLACK, DISPLAY, line_spacing=0.85, name="content_title")
    _text(slide, 0.5, 1.6, 5.2, 0.78, "HOSPITAL", 48, ORANGE, DISPLAY, italic=True, line_spacing=0.85, name="content_title")
    _text(slide, 0.52, 2.52, 5.1, 0.55, "Patient queue & appointment management system", 12, MUTED, BODY, line_spacing=1.18, name="content_subtitle")
    _rule(slide, 0.52, 3.2, 0.8, ORANGE, 0.05)

    people = [
        ("Uday Mhaske", "ADT24SOCB1735"),
        ("Aditya Sundalam", "MITU22BTCS0044"),
        ("Atharv Shewale", "ADT25SOCB1715"),
        ("Saiprasad Bendre", "ADT25SOCB1651"),
    ]
    for i, (name, roll) in enumerate(people):
        x = 0.52 + (i % 2) * 2.5
        y = 3.42 + (i // 2) * 0.62
        _text(slide, x, y, 2.3, 0.22, name, 10, INK, BODY, bold=True, name="content_team")
        _text(slide, x, y + 0.24, 2.3, 0.18, roll, 8, MUTED, CONDENSED, spacing=0.8, name="content_roll")

    _text(slide, 0.52, 4.78, 1.0, 0.18, "GUIDED BY", 7.5, ORANGE, CONDENSED, bold=True, spacing=1.4, name="content_label")
    _text(slide, 0.52, 5.0, 4.6, 0.26, "Dr. P. D. Patil", 12, NAVY, DISPLAY, name="content_guide")
    _text(slide, 0.52, 5.32, 5.1, 0.22, "MIT Art, Design and Technology University  ·  Loni-Kalbhor", 8.5, MUTED, BODY, name="content_university")

    _screenshot(slide, "patient", 5.9, 0.82, 3.55, 2.22, name="content_shot_patient")
    _rect(slide, 5.9, 3.16, 3.55, 1.12, BLACK, None, name="content_token_panel")
    _text(slide, 6.1, 3.28, 1.1, 0.18, "LIVE TOKEN", 7.5, LIME, CONDENSED, bold=True, spacing=1.3, name="content_label")
    _text(slide, 6.08, 3.46, 1.1, 0.6, "14", 38, WHITE, DISPLAY, line_spacing=0.8, name="content_token")
    _text(slide, 7.05, 3.36, 2.2, 0.18, "POSITION 3", 8, WHITE, CONDENSED, bold=True, spacing=1.1, name="content_label")
    _text(slide, 7.05, 3.6, 2.2, 0.36, "≈ 18 MIN", 20, LIME, DISPLAY, name="content_eta")
    _text(slide, 7.05, 4.0, 2.2, 0.16, "CARDIOLOGY / WAITING", 7, WHITE, CONDENSED, spacing=1.0, name="content_status")
    _text(slide, 5.9, 4.5, 3.55, 0.55, "One queue. Three points of view.", 11, BLACK, BODY, italic=True, name="content_caption")


def slide_thesis(slide):
    _page(slide, "01", "The argument")
    _headline(slide, "The waiting room should know\nwhat happens next.", 0.55, 0.68, 8.9, 1.15, 30)
    _deck(slide, "This deck follows the wait, the system that removes the guesswork, and the tests that prove the queue rules.", 1.86)

    stages = [
        ("01", "THE WAIT", "Why the queue feels opaque"),
        ("02", "THE SYSTEM", "How one live state replaces paper"),
        ("03", "THE PROOF", "What the tests actually verify"),
    ]
    for i, (number, title, body) in enumerate(stages):
        x = 0.55 + i * 3.05
        _card(slide, x, 2.35, 2.85, 2.35)
        _text(slide, x + 0.2, 2.55, 0.9, 0.5, number, 26, ORANGE if i == 0 else BLACK, DISPLAY, name="content_stage_num")
        _rule(slide, x + 0.2, 3.18, 0.62, ORANGE, 0.04)
        _text(slide, x + 0.2, 3.36, 2.4, 0.22, title, 9, BLACK, CONDENSED, bold=True, spacing=1.3, name="content_stage_label")
        _text(slide, x + 0.2, 3.7, 2.4, 0.75, body, 10, MUTED, BODY, line_spacing=1.2, name="content_stage_body")

    _takeaway(slide, "From a paper token to a live queue state.")
    _text(slide, 0.55, 5.86, 8.8, 0.18, "15 slides  ·  one patient journey  ·  three role-based views", 7.5, MUTED, BODY, name="content_note")


def slide_problem(slide):
    _page(slide, "01", "The wait")
    _headline(slide, "Waiting dwarfs the time in care.", 0.55, 0.68, 8.9, 0.72, 30)
    _deck(slide, "One Kolkata outpatient sample: a 98.6-minute wait for a 6.5-minute consultation.", 1.48)

    _text(slide, 0.55, 2.02, 3.2, 0.95, "98.6", 54, ORANGE, DISPLAY, line_spacing=0.86, name="content_big_number")
    _text(slide, 0.57, 2.98, 2.7, 0.22, "MINUTES WAITING", 8, ORANGE, CONDENSED, bold=True, spacing=1.4, name="content_label")
    _text(slide, 0.57, 3.32, 3.0, 0.8, "Mean registration-to-chamber time. n = 432 outpatients.", 10, MUTED, BODY, line_spacing=1.2, name="content_caption")

    _text(slide, 6.2, 2.04, 1.2, 0.2, "WAIT", 8, ORANGE, CONDENSED, bold=True, spacing=1.4, name="content_label")
    _line(slide, 6.2, 2.32, 9.1, 2.32, NAVY, 5.2, name="content_wait_bar")
    _text(slide, 9.15, 2.22, 0.45, 0.2, "98.6", 9, NAVY, BODY, bold=True, name="content_wait_value")
    _text(slide, 6.2, 2.66, 2.0, 0.2, "CONSULTATION", 8, ORANGE, CONDENSED, bold=True, spacing=1.4, name="content_label")
    _line(slide, 6.2, 2.94, 6.42, 2.94, ORANGE, 5.2, name="content_consult_bar")
    _text(slide, 6.5, 2.84, 0.5, 0.2, "6.5", 9, ORANGE, BODY, bold=True, name="content_consult_value")
    _text(slide, 8.66, 2.66, 0.9, 0.45, "15×", 26, ORANGE, DISPLAY, name="content_ratio")

    _rect(slide, 6.2, 3.55, 3.2, 1.35, CREAM, BLACK, 0.9, name="content_ticket")
    _text(slide, 6.42, 3.7, 1.4, 0.18, "TOKEN", 7.5, MUTED, CONDENSED, bold=True, spacing=1.4, name="content_ticket_label")
    _text(slide, 6.42, 3.9, 1.4, 0.45, "17", 26, BLACK, DISPLAY, name="content_ticket_number")
    _text(slide, 7.95, 3.72, 1.25, 0.7, "NO ETA\nNO STATUS", 10, MUTED, CONDENSED, bold=True, line_spacing=1.2, name="content_ticket_text")
    _rect(slide, 6.42, 4.42, 1.4, 0.3, RED, None, name="content_stamp")
    _text(slide, 6.5, 4.49, 1.24, 0.16, "WAIT UNKNOWN", 7.5, WHITE, CONDENSED, bold=True, spacing=1.2, align=PP_ALIGN.CENTER, name="content_stamp_text")

    _takeaway(slide, "A paper token carries a number, not a status.")
    _source(slide, "[1] Datta M. Indian J Community Med. 2024;49(Suppl 1):S61. Mean wait 98.6 min (SD 45.4); consultation 6.5 min (SD 13.7).", 5.88)


def slide_triptych(slide):
    _page(slide, "02", "The system")
    _headline(slide, "One queue. Three points of view.", 0.55, 0.68, 8.9, 0.72, 30)
    _deck(slide, "The same state, filtered by role and permission.", 1.48)

    cards = [
        ("patient", "01", "PATIENT", "Token, position, estimate"),
        ("doctor", "02", "DOCTOR", "Queue, triage, one action"),
        ("admin", "03", "ADMIN", "People, schedules, metrics"),
    ]
    for key, number, role, caption in cards:
        x = {"patient": 0.55, "doctor": 3.55, "admin": 6.55}[key]
        _screenshot(slide, key, x, 1.9, 2.85, 1.78, name=f"content_shot_{key}")
        _text(slide, x, 3.8, 0.42, 0.2, number, 8, ORANGE, CONDENSED, bold=True, spacing=1.2, name="content_card_num")
        _text(slide, x + 0.45, 3.8, 1.6, 0.2, role, 9, BLACK, CONDENSED, bold=True, spacing=1.2, name="content_card_role")
        _text(slide, x, 4.05, 2.7, 0.32, caption, 10, MUTED, BODY, name="content_card_caption")
        _line(slide, x + 1.42, 4.5, x + 1.42, 4.86, RULE, 1.0, name="content_state_line")

    _line(slide, 1.97, 4.86, 7.97, 4.86, RULE, 1.0, name="content_state_spine")
    _rect(slide, 2.85, 5.05, 3.3, 0.54, BLACK, None, name="content_state_panel")
    _text(slide, 3.05, 5.21, 2.9, 0.2, "ONE QUEUE STATE", 9, LIME, CONDENSED, bold=True, spacing=1.4, align=PP_ALIGN.CENTER, name="content_state_label")
    _text(slide, 6.35, 5.19, 3.05, 0.26, "Appointments, tokens, roles, audit events.", 9.5, MUTED, BODY, name="content_state_note")


def slide_journey(slide):
    _page(slide, "03", "The flow")
    _headline(slide, "Six steps. One shared record.", 0.55, 0.68, 8.9, 0.72, 30)
    _deck(slide, "Booked or walk-in, every patient enters the same queue.", 1.48)

    steps = [
        ("01", "REGISTER", "Create identity and role"),
        ("02", "DISCOVER", "Choose doctor and department"),
        ("03", "BOOK", "Reserve a conflict-safe slot"),
        ("04", "CHECK IN", "Receive a digital token"),
        ("05", "TRACK", "See position and estimate"),
        ("06", "CONSULT", "Doctor updates the queue"),
    ]
    for i, (number, label, body) in enumerate(steps):
        col, row = i % 3, i // 3
        x = 0.55 + col * 3.05
        y = 1.98 + row * 1.55
        _card(slide, x, y, 2.85, 1.35)
        _rect(slide, x + 0.18, y + 0.18, 0.44, 0.44, BLACK, None, name="content_step_badge")
        _text(slide, x + 0.18, y + 0.27, 0.44, 0.2, number, 8, WHITE, CONDENSED, bold=True, align=PP_ALIGN.CENTER, name="content_step_num")
        _text(slide, x + 0.75, y + 0.19, 1.9, 0.22, label, 10, ORANGE if i == 3 else BLACK, CONDENSED, bold=True, spacing=1.1, name="content_step_label")
        _text(slide, x + 0.75, y + 0.55, 1.9, 0.6, body, 10, MUTED, BODY, line_spacing=1.18, name="content_step_body")
        if col < 2:
            _arrow(slide, x + 2.88, y + 0.58, 0.14, 0.18, ORANGE, name="content_step_arrow")

    _takeaway(slide, "Every doctor action recalculates every waiting token.", 5.12, 0.66, 12)
    _text(slide, 0.55, 5.9, 8.8, 0.18, "One patient record. Every screen sees the same state.", 7.8, MUTED, BODY, italic=True, name="content_note")


def slide_queue(slide):
    _page(slide, "04", "The rule")
    _headline(slide, "Urgency first. Arrival time breaks the tie.", 0.55, 0.68, 8.9, 0.72, 30)
    _deck(slide, "Priority rank sets the order; check-in time settles ties inside a tier.", 1.48)

    tiers = [
        ("CRITICAL", "01", ORANGE),
        ("HIGH", "02", NAVY),
        ("MEDIUM", "03", BLUE),
        ("NORMAL", "04", MUTED),
    ]
    for i, (word, rank, color) in enumerate(tiers):
        y = 2.02 + i * 0.65
        _text(slide, 0.55, y + 0.05, 0.42, 0.2, rank, 8, color, CONDENSED, bold=True, spacing=1.2, name="content_rank")
        _text(slide, 1.02, y - 0.12, 3.0, 0.6, word, 24, color, DISPLAY, name="content_tier")
        _line(slide, 1.02, y + 0.48, 5.55, y + 0.48, color, 0.8, name="content_tier_rule")

    _screenshot(slide, "doctor", 6.2, 1.95, 3.2, 2.0, name="content_shot_queue")
    _rect(slide, 6.2, 4.1, 3.2, 1.18, WHITE, RULE, 0.8, name="content_board")
    _text(slide, 6.36, 4.22, 2.85, 0.18, "LIVE QUEUE  /  CARDIOLOGY", 7.5, ORANGE, CONDENSED, bold=True, spacing=1.2, name="content_board_label")
    board = [("#12", "CRITICAL", ORANGE), ("#14", "HIGH", NAVY), ("#15", "MEDIUM", BLUE), ("#17", "NORMAL", MUTED)]
    for i, (token, tier, color) in enumerate(board):
        y = 4.48 + i * 0.2
        _text(slide, 6.36, y, 0.5, 0.17, token, 9, BLACK, CONDENSED, bold=True, name="content_board_token")
        _circle(slide, 6.98, y + 0.04, 0.08, color, None, name="content_board_dot")
        _text(slide, 7.16, y, 1.7, 0.17, tier, 8.5, color, CONDENSED, bold=True, spacing=1.0, name="content_board_tier")

    _rect(slide, 0.55, 5.38, 8.9, 0.48, BLACK, None, name="content_formula_panel")
    _text(slide, 0.72, 5.51, 8.5, 0.2, "ORDER BY  priority_rank ASC,  checked_in_at ASC", 9, WHITE, MONO, name="content_formula")
    _text(slide, 0.55, 5.94, 8.8, 0.16, "Tier labels are software categories; the hospital's approved triage policy governs care.", 7.5, MUTED, BODY, italic=True, name="content_disclaimer")


def slide_evidence(slide):
    _page(slide, "05", "The evidence")
    _headline(slide, "Studies measure the wait.\nThey do not test the whole workflow.", 0.55, 0.68, 8.9, 1.15, 30)
    _deck(slide, "Three outpatient studies. Three different health systems.", 1.86)

    studies = [
        ("98.6", "KOLKATA", "n = 432", "mean wait / minutes", ORANGE),
        ("161", "IRAN", "n = 160", "mean wait / minutes", NAVY),
        ("168.4", "NIGERIA", "n = 96", "mean wait / minutes", BLUE),
    ]
    for i, (number, place, sample, label, color) in enumerate(studies):
        x = 0.55 + i * 3.05
        _card(slide, x, 2.3, 2.85, 1.62)
        _text(slide, x + 0.18, 2.45, 2.5, 0.72, number, 40, color, DISPLAY, line_spacing=0.9, name="content_study_num")
        _text(slide, x + 0.18, 3.2, 2.2, 0.2, place, 9, BLACK, CONDENSED, bold=True, spacing=1.3, name="content_study_place")
        _text(slide, x + 0.18, 3.44, 2.2, 0.18, sample, 8.5, MUTED, CONDENSED, name="content_study_sample")
        _text(slide, x + 0.18, 3.64, 2.3, 0.18, label, 8.5, MUTED, BODY, name="content_study_label")

    _rect(slide, 0.55, 4.18, 8.9, 0.86, BLACK, None, name="content_gap_panel")
    _text(slide, 0.75, 4.4, 8.5, 0.42, "The gap: no cited study tests booking, token, priority, and forecast as one workflow.", 14, WHITE, DISPLAY, line_spacing=1.0, name="content_gap")
    _text(slide, 0.55, 5.25, 8.8, 0.32, "This project builds the workflow and evaluates software behaviour. It does not claim clinical outcomes.", 10, MUTED, BODY, italic=True, name="content_limitation")
    _source(slide, "[1] Datta 2024. [2] Mohebbifar et al. Glob J Health Sci. 2013;6(1):172–180. [3] Oche & Adamu. Ann Med Health Sci Res. 2013;3(4):588–592.", 5.86)


def slide_architecture(slide):
    _page(slide, "06", "The engine")
    _headline(slide, "Fast path first. Intelligence stays optional.", 0.55, 0.68, 8.9, 0.72, 29)
    _deck(slide, "The API owns state. The model only advises.", 1.48)

    _rect(slide, 0.55, 1.95, 2.5, 2.35, CREAM, BLACK, 0.8, name="content_client")
    _text(slide, 0.75, 2.12, 2.0, 0.18, "CLIENT", 7.5, ORANGE, CONDENSED, bold=True, spacing=1.4, name="content_label")
    _text(slide, 0.75, 2.42, 2.1, 0.85, "React\nTypeScript", 18, BLACK, DISPLAY, line_spacing=1.0, name="content_block_title")
    _text(slide, 0.75, 3.45, 2.0, 0.62, "Patient\nDoctor\nAdmin", 10, MUTED, BODY, line_spacing=1.3, name="content_block_list")

    _rect(slide, 3.35, 1.95, 3.2, 2.35, BLACK, None, name="content_api")
    _text(slide, 3.55, 2.12, 2.6, 0.18, "CORE SERVICE", 7.5, LIME, CONDENSED, bold=True, spacing=1.4, name="content_label")
    _text(slide, 3.55, 2.42, 2.7, 0.5, "Spring Boot", 20, WHITE, DISPLAY, name="content_block_title")
    _text(slide, 3.55, 3.05, 2.7, 0.62, "AUTH  /  APPOINTMENTS\nQUEUE  /  AUDIT", 9, WHITE, MONO, line_spacing=1.4, name="content_block_list")
    _text(slide, 3.55, 3.85, 2.7, 0.2, "JWT  •  FLYWAY  •  JAVA 21", 7.5, LIME, CONDENSED, bold=True, spacing=1.0, name="content_block_meta")

    _rect(slide, 6.9, 1.95, 2.55, 2.35, CREAM, BLACK, 0.8, name="content_ml")
    _text(slide, 7.1, 2.12, 2.1, 0.18, "INTELLIGENCE", 7.5, ORANGE, CONDENSED, bold=True, spacing=1.4, name="content_label")
    _text(slide, 7.1, 2.42, 2.1, 0.5, "FastAPI", 20, BLACK, DISPLAY, name="content_block_title")
    _text(slide, 7.1, 3.05, 2.1, 0.62, "Random Forest\nwait estimate", 10, MUTED, BODY, line_spacing=1.2, name="content_block_list")
    _text(slide, 7.1, 3.85, 2.1, 0.2, "1,200 SYNTHETIC ROWS", 7.5, ORANGE, CONDENSED, bold=True, spacing=1.0, name="content_block_meta")

    _arrow(slide, 3.08, 3.0, 0.22, 0.18, ORANGE, name="content_arrow")
    _arrow(slide, 6.61, 3.0, 0.22, 0.18, ORANGE, name="content_arrow")
    _text(slide, 3.02, 3.24, 0.36, 0.16, "REST", 6.5, MUTED, CONDENSED, bold=True, spacing=1.0, align=PP_ALIGN.CENTER, name="content_arrow_label")
    _text(slide, 6.55, 3.24, 0.36, 0.16, "REST", 6.5, MUTED, CONDENSED, bold=True, spacing=1.0, align=PP_ALIGN.CENTER, name="content_arrow_label")

    _rect(slide, 3.35, 4.62, 3.2, 0.52, NAVY, None, name="content_db")
    _text(slide, 3.55, 4.77, 2.8, 0.2, "MYSQL 8  /  JPA", 8, WHITE, CONDENSED, bold=True, spacing=1.2, name="content_db_text")
    _line(slide, 4.95, 4.3, 4.95, 4.62, BLACK, 1.2, name="content_db_line")

    _rect(slide, 6.9, 4.52, 2.55, 0.72, CREAM, ORANGE, 0.8, name="content_fallback_panel")
    _text(slide, 7.1, 4.65, 2.15, 0.45, "MODEL DOWN?\nFALLBACK ANSWERS.", 9, ORANGE, CONDENSED, bold=True, line_spacing=1.15, name="content_fallback")

    _rect(slide, 0.55, 5.42, 8.9, 0.44, PAPER, None, name="content_runtime")
    _text(slide, 0.72, 5.54, 8.5, 0.2, "DOCKER COMPOSE  /  FRONTEND  +  BACKEND  +  MYSQL  +  ML SIDECAR", 8, NAVY, CONDENSED, bold=True, spacing=0.9, name="content_runtime_text")


def slide_tools(slide):
    _page(slide, "07", "The tools")
    _headline(slide, "The stack is ordinary. The flow is not.", 0.55, 0.68, 8.9, 0.72, 30)
    _deck(slide, "Four focused layers. One local runtime. Clear ownership.", 1.48)

    groups = [
        ("JAVA 21", "BACKEND", 0.55, 1.95, BLACK, "Spring Boot 3.5  ·  Security  ·  JWT  ·  Flyway"),
        ("REACT 19", "INTERFACE", 5.1, 1.95, ORANGE, "TypeScript  ·  Vite  ·  Tailwind  ·  TanStack Query"),
        ("MYSQL 8", "DATA", 0.55, 3.65, NAVY, "JPA / Hibernate  ·  Flyway migrations"),
        ("PYTHON", "INTELLIGENCE", 5.1, 3.65, BLACK, "FastAPI  ·  scikit-learn  ·  Random Forest"),
    ]
    for title, category, x, y, color, details in groups:
        _card(slide, x, y, 4.35, 1.5)
        _text(slide, x + 0.2, y + 0.18, 3.9, 0.18, category, 7.5, ORANGE, CONDENSED, bold=True, spacing=1.4, name="content_label")
        _text(slide, x + 0.2, y + 0.44, 3.9, 0.58, title, 26, color, DISPLAY, line_spacing=0.9, name="content_tool")
        _text(slide, x + 0.2, y + 1.08, 3.9, 0.32, details, 10, MUTED, BODY, line_spacing=1.15, name="content_tool_detail")

    _rect(slide, 0.55, 5.42, 8.9, 0.52, LIME, None, name="content_quality_panel")
    _text(slide, 0.72, 5.58, 8.5, 0.2, "JUNIT 5   /   PYTEST   /   VITEST   /   DOCKER COMPOSE", 9, BLACK, CONDENSED, bold=True, spacing=1.0, name="content_quality")
    _text(slide, 0.55, 6.0, 8.8, 0.14, "API contract: REST + OpenAPI. Access control: role-based JWT.", 7.5, MUTED, BODY, italic=True, name="content_note")


def slide_product(slide):
    _page(slide, "08", "The product")
    _headline(slide, "The clinic, on one screen.", 0.55, 0.68, 8.9, 0.72, 30)
    _deck(slide, "Doctor workspace: today's list, live queue, one action.", 1.48)

    _screenshot(slide, "doctor", 0.55, 1.98, 6.3, 3.94, name="content_shot_hero")
    metrics = [("TODAY", "12", "appointments", BLACK), ("WAITING", "5", "prioritized tokens", ORANGE), ("IN ROOM", "1", "consultation", BLACK)]
    for i, (label, number, caption, color) in enumerate(metrics):
        y = 2.0 + i * 1.3
        _text(slide, 7.25, y, 2.0, 0.18, label, 7.5, ORANGE, CONDENSED, bold=True, spacing=1.4, name="content_label")
        _text(slide, 7.22, y + 0.22, 2.0, 0.6, number, 32, color, DISPLAY, name="content_metric")
        _text(slide, 7.25, y + 0.82, 2.0, 0.22, caption, 9.5, MUTED, BODY, name="content_metric_label")
        if i < 2:
            _line(slide, 7.25, y + 1.15, 9.4, y + 1.15, RULE, 0.65, name="content_metric_rule")
    _text(slide, 0.55, 6.0, 8.8, 0.14, "Screenshot from the working build. Demo data only.", 7.5, MUTED, BODY, italic=True, name="content_caption")


def slide_results(slide):
    _page(slide, "09", "The proof")
    _headline(slide, "The rules survive the test suite.", 0.55, 0.68, 8.9, 0.72, 30)
    _deck(slide, "Fresh runs of the backend, Python, and frontend suites.", 1.48)

    metrics = [("247", "BACKEND TESTS", ORANGE), ("19", "PYTHON TESTS", BLACK), ("41", "FRONTEND TESTS", BLACK), ("0", "FAILED TESTS", BLACK)]
    for i, (number, label, color) in enumerate(metrics):
        x = 0.55 + i * 2.28
        _text(slide, x, 2.06, 2.1, 0.72, number, 36, color, DISPLAY, name="content_metric")
        _text(slide, x, 2.9, 2.1, 0.22, label, 9, ORANGE, CONDENSED, bold=True, spacing=1.2, name="content_metric_label")
        _line(slide, x, 3.22, x + 1.9, 3.22, RULE, 0.7, name="content_metric_rule")

    _text(slide, 0.55, 3.5, 8.8, 0.32, "JUnit 5: 0 failures, 0 errors.  Production build passes.  API contract documented with OpenAPI.", 10, MUTED, BODY, name="content_build_note")

    _rect(slide, 0.55, 4.28, 8.9, 0.98, BLACK, None, name="content_invariants")
    invariants = [("01", "UNIQUE TOKEN"), ("02", "PRIORITY ORDER"), ("03", "ROLE ISOLATION"), ("04", "ML FALLBACK")]
    for i, (num, label) in enumerate(invariants):
        x = 0.78 + i * 2.18
        _text(slide, x, 4.46, 0.4, 0.18, num, 8, LIME, CONDENSED, bold=True, spacing=1.0, name="content_invariant_num")
        _text(slide, x, 4.72, 1.9, 0.22, label, 10, WHITE, CONDENSED, bold=True, spacing=1.0, name="content_invariant")
    _text(slide, 0.55, 5.62, 8.8, 0.32, "Every estimate is labelled ML or FALLBACK; concurrent check-in cannot duplicate a token.", 10, MUTED, BODY, name="content_detail")
    _text(slide, 0.55, 6.0, 8.8, 0.14, "Software invariants, not clinical outcomes.", 7.5, MUTED, BODY, italic=True, name="content_note")


def slide_conclusion(slide):
    _page(slide, "10", "The next step")
    _headline(slide, "From a working queue to\na trusted service.", 0.55, 0.68, 5.6, 1.15, 30)
    _deck(slide, "The prototype closes the loop between booking and consultation.", 1.86, 5.4, 10.5)

    outcomes = [
        ("01", "Patients", "Book, check in, receive a token, and see position and estimate."),
        ("02", "Doctors", "Run the live queue, assign priority, and record overrides."),
        ("03", "Administrators", "Manage users, doctors, schedules, and the operational view."),
    ]
    for i, (num, head, body) in enumerate(outcomes):
        y = 2.35 + i * 1.0
        _text(slide, 0.55, y, 0.5, 0.4, num, 18, ORANGE, DISPLAY, name="content_outcome_num")
        _text(slide, 1.1, y, 4.6, 0.22, head, 11, BLACK, CONDENSED, bold=True, name="content_outcome_head")
        _text(slide, 1.1, y + 0.3, 4.6, 0.5, body, 10, MUTED, BODY, line_spacing=1.2, name="content_outcome_body")

    _rect(slide, 6.3, 2.2, 3.15, 3.25, CREAM, BLACK, 0.8, name="content_next_panel")
    _text(slide, 6.5, 2.4, 2.7, 0.2, "NEXT HORIZON", 8, ORANGE, CONDENSED, bold=True, spacing=1.4, name="content_label")
    future = [("01", "Reach", "SMS, push, mobile"), ("02", "Scale", "Multi-hospital tenancy"), ("03", "Evidence", "Real data, monitored models")]
    for i, (num, head, body) in enumerate(future):
        y = 2.82 + i * 0.82
        _text(slide, 6.5, y, 0.42, 0.3, num, 18, ORANGE, DISPLAY, name="content_future_num")
        _text(slide, 7.02, y, 2.2, 0.2, head, 10, BLACK, CONDENSED, bold=True, name="content_future_head")
        _text(slide, 7.02, y + 0.24, 2.2, 0.4, body, 9.5, MUTED, BODY, name="content_future_body")
        if i < 2:
            _line(slide, 6.5, y + 0.68, 9.25, y + 0.68, RULE, 0.65, name="content_future_rule")
    _text(slide, 0.55, 5.9, 8.8, 0.18, "Clinical outcomes and real-hospital time savings remain unmeasured.", 8, MUTED, BODY, italic=True, name="content_note")


def slide_references(slide):
    _page(slide, "11", "Sources")
    _headline(slide, "Sources.", 0.55, 0.68, 8.9, 0.72, 30)
    _deck(slide, "Research, standards, and official documentation used in this deck.", 1.48)

    left = [
        ("01", "Datta M. Assessment of outdoor patients waiting time and consultation time in a tertiary care hospital of Kolkata. Indian J Community Med. 2024;49(Suppl 1):S61. doi:10.4103/ijcm.ijcm_abstract210"),
        ("02", "Mohebbifar R, Hasanpoor E, Mohseni M, et al. Outpatient waiting time in health services and teaching hospitals: a case study in Iran. Glob J Health Sci. 2013;6(1):172–180. doi:10.5539/gjhs.v6n1p172"),
        ("03", "Oche MO, Adamu H. Determinants of patient waiting time in the general outpatient department of a tertiary health institution in north-western Nigeria. Ann Med Health Sci Res. 2013;3(4):588–592. doi:10.4103/2141-9248.122123"),
        ("04", "Spring Boot 3.5.16 Reference Documentation. Broadcom. https://docs.spring.io/spring-boot/3.5.16/reference/index.html"),
    ]
    right = [
        ("05", "FastAPI Documentation. https://fastapi.tiangolo.com/"),
        ("06", "scikit-learn User Guide. https://scikit-learn.org/stable/user_guide.html"),
        ("07", "MySQL 8.4 Reference Manual. Oracle. https://dev.mysql.com/doc/refman/8.4/en/"),
        ("08", "Nottingham M, Wilde E, Dalal S. RFC 9457: Problem Details for HTTP APIs. IETF, July 2023. https://www.rfc-editor.org/rfc/rfc9457.html"),
    ]
    for column, entries in enumerate([left, right]):
        x = 0.55 if column == 0 else 5.08
        for i, (num, text) in enumerate(entries):
            y = 2.0 + i * 0.92
            _text(slide, x, y, 0.42, 0.2, num, 8, ORANGE, CONDENSED, bold=True, spacing=1.2, name="content_ref_num")
            _text(slide, x + 0.45, y - 0.02, 3.85, 0.82, text, 9.5, INK, BODY, line_spacing=1.15, name="content_ref")
    _text(slide, 0.55, 5.86, 8.8, 0.18, "URLs accessed 19 November 2026. The ML model uses synthetic demonstration data.", 7.5, MUTED, BODY, italic=True, name="content_ref_note")


def slide_thanks(slide):
    _page(slide, "12", "Close")
    _text(slide, 0.55, 1.2, 6.0, 1.0, "Thank you.", 54, BLACK, DISPLAY, line_spacing=0.85, name="content_title")
    _text(slide, 0.58, 2.35, 5.4, 0.32, "Smart Hospital: from booking to consultation.", 13, MUTED, BODY, italic=True, name="content_subtitle")

    _text(slide, 0.55, 3.75, 3.9, 0.2, "TEAM SY2207", 8, ORANGE, CONDENSED, bold=True, spacing=1.5, name="content_label")
    names = [
        "Uday Mhaske  ·  ADT24SOCB1735",
        "Aditya Sundalam  ·  MITU22BTCS0044",
        "Atharv Shewale  ·  ADT25SOCB1715",
        "Saiprasad Bendre  ·  ADT25SOCB1651",
    ]
    for i, name in enumerate(names):
        _text(slide, 0.55, 4.08 + i * 0.3, 5.6, 0.22, name, 10, INK, BODY, name="content_team")
    _text(slide, 0.55, 5.55, 4.8, 0.22, "Guided by Dr. P. D. Patil", 10, MUTED, BODY, name="content_guide")

    _rect(slide, 6.6, 1.5, 2.3, 3.55, CREAM, BLACK, 0.9, name="content_thanks_ticket")
    _text(slide, 6.8, 1.72, 1.9, 0.2, "LIVE TOKEN", 8, ORANGE, CONDENSED, bold=True, spacing=1.4, name="content_ticket_label")
    _text(slide, 6.72, 2.15, 2.0, 1.5, "14", 60, BLACK, DISPLAY, line_spacing=0.75, name="content_thanks_number")
    _text(slide, 6.8, 4.3, 1.9, 0.42, "POSITION 3  /  ≈18 MIN", 9, MUTED, CONDENSED, bold=True, spacing=0.9, align=PP_ALIGN.CENTER, name="content_thanks_meta")
    _rect(slide, 9.12, 1.5, 0.23, 3.55, ORANGE, None, name="content_thanks_bar")


def slide_qanda(slide):
    _page(slide, "13", "Questions")
    _text(slide, 0.5, 1.0, 8.9, 1.3, "Q&A.", 54, ORANGE, DISPLAY, line_spacing=0.85, name="content_title")
    _text(slide, 0.55, 2.45, 8.6, 0.32, "Ask about the queue rules, the ML fallback, or the next step.", 13, MUTED, BODY, name="content_subtitle")

    prompts = [
        ("01", "QUEUE", "Priority, FIFO, overrides"),
        ("02", "MODEL", "Synthetic data, fallback"),
        ("03", "PILOT", "What to measure next"),
    ]
    for i, (num, head, body) in enumerate(prompts):
        x = 0.55 + i * 3.05
        _card(slide, x, 3.4, 2.85, 1.55)
        _text(slide, x + 0.2, 3.6, 0.5, 0.2, num, 8, ORANGE, CONDENSED, bold=True, spacing=1.2, name="content_prompt_num")
        _rule(slide, x + 0.2, 3.9, 0.6, ORANGE, 0.04)
        _text(slide, x + 0.2, 4.08, 2.4, 0.22, head, 10, BLACK, CONDENSED, bold=True, spacing=1.3, name="content_prompt_head")
        _text(slide, x + 0.2, 4.4, 2.4, 0.35, body, 10, MUTED, BODY, name="content_prompt_body")
    _text(slide, 0.55, 5.86, 8.8, 0.18, "Team SY2207  ·  Department of Computer Science & Engineering", 8, MUTED, BODY, name="content_note")


BUILDERS = [
    slide_cover,
    slide_thesis,
    slide_problem,
    slide_triptych,
    slide_journey,
    slide_queue,
    slide_evidence,
    slide_architecture,
    slide_tools,
    slide_product,
    slide_results,
    slide_conclusion,
    slide_references,
    slide_thanks,
    slide_qanda,
]


def _validate_assets() -> None:
    for name in ("patient", "doctor", "admin"):
        path = ROOT / "assets" / f"{name}.png"
        if not path.exists():
            raise FileNotFoundError(path)
        with Image.open(path) as image:
            image.verify()


def _count(value: str) -> str:
    try:
        number = int(value)
    except ValueError as error:
        raise argparse.ArgumentTypeError("test count must be an integer") from error
    if number < 0:
        raise argparse.ArgumentTypeError("test count cannot be negative")
    return str(number)


def build(template: Path, output: Path, backend_tests="247", python_tests="19", frontend_tests="41"):
    if not template.exists():
        raise FileNotFoundError(template)
    _validate_assets()
    prs = Presentation(str(template))
    if len(prs.slides) != len(BUILDERS):
        raise ValueError(f"Expected {len(BUILDERS)} slides, found {len(prs.slides)}")

    _normalize_footer(prs)
    prs.core_properties.title = "Smart Hospital: Patient Queue & Appointment Management System"
    prs.core_properties.subject = "Project Presentation, 2026–27"
    prs.core_properties.author = "Team SY2207"

    footer_before = _footer_baseline(prs)
    _validate_footer(footer_before)

    for slide, builder in zip(prs.slides, BUILDERS):
        strip_body(slide)
        builder(slide)

    # The results slide is deliberately data-driven from the CLI.
    results_slide = prs.slides[10]
    # Replace the hard-coded values in the builder with the supplied counts.
    for shape in results_slide.shapes:
        if getattr(shape, "has_text_frame", False):
            if shape.text.strip() == "247":
                shape.text_frame.paragraphs[0].runs[0].text = backend_tests
            elif shape.text.strip() == "19":
                shape.text_frame.paragraphs[0].runs[0].text = python_tests
            elif shape.text.strip() == "41":
                shape.text_frame.paragraphs[0].runs[0].text = frontend_tests

    _snap_type(prs)

    footer_after = _footer_baseline(prs)
    _validate_footer(footer_after)
    if footer_before != footer_after:
        raise RuntimeError("Footer furniture changed")

    for i, slide in enumerate(prs.slides, 1):
        has_page = any(
            getattr(shape, "has_text_frame", False) and shape.text.strip() == "‹#›"
            for shape in slide.shapes
        )
        if has_page is not (i != 1):
            raise RuntimeError(f"Page number state changed on slide {i}")

    output.parent.mkdir(parents=True, exist_ok=True)
    prs.save(str(output))
    return prs


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--template", type=Path, default=TEMPLATE)
    parser.add_argument("--output", type=Path, default=OUTPUT)
    parser.add_argument("--backend-tests", type=_count, default="247")
    parser.add_argument("--python-tests", type=_count, default="19")
    parser.add_argument("--frontend-tests", type=_count, default="41")
    args = parser.parse_args()
    prs = build(
        args.template,
        args.output,
        args.backend_tests,
        args.python_tests,
        args.frontend_tests,
    )
    print(f"Wrote {args.output} with {len(prs.slides)} slides")


if __name__ == "__main__":
    main()

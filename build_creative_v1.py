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


def slide_cover(slide):
    _label(slide, 0.55, 0.52, 5.4, "SY2207  /  Project presentation  /  2026–27", INK, 7.2)
    _text(slide, 0.46, 1.02, 5.15, 0.78, "SMART", 63, BLACK, DISPLAY, line_spacing=0.8, name="content_title")
    _text(
        slide,
        0.46,
        1.72,
        5.15,
        0.85,
        "HOSPITAL",
        63,
        ORANGE,
        DISPLAY,
        italic=True,
        line_spacing=0.8,
        name="content_title",
    )
    _text(
        slide,
        0.5,
        2.7,
        5.25,
        0.52,
        "Patient queue & appointment management system",
        12.5,
        MUTED,
        BODY,
        line_spacing=1.15,
        name="content_subtitle",
    )
    _rule(slide, 0.5, 3.42, 0.78, ORANGE, 0.055)

    people = [
        ("Uday Mhaske", "ADT24SOCB1735"),
        ("Aditya Sundalam", "MITU22BTCS0044"),
        ("Atharv Shewale", "ADT25SOCB1715"),
        ("Saiprasad Bendre", "ADT25SOCB1651"),
    ]
    for i, (name, roll) in enumerate(people):
        x = 0.5 + (i % 2) * 2.36
        y = 3.74 + (i // 2) * 0.62
        _text(slide, x, y, 2.1, 0.2, name, 8.8, INK, BODY, bold=True, name="content_team")
        _text(slide, x, y + 0.2, 2.1, 0.17, roll, 6.5, MUTED, CONDENSED, spacing=0.8, name="content_roll")

    _text(slide, 0.5, 5.02, 0.72, 0.17, "GUIDE", 6.5, ORANGE, CONDENSED, bold=True, spacing=1.5, name="content_label")
    _text(slide, 0.5, 5.2, 4.5, 0.26, "Dr. P. D. Patil", 12, NAVY, DISPLAY, name="content_guide")
    _text(
        slide,
        0.5,
        5.48,
        5.4,
        0.22,
        "MIT Art, Design and Technology University  ·  Loni-Kalbhor",
        7.2,
        MUTED,
        BODY,
        name="content_university",
    )

    # Product artefact, not a decorative mock-up: the live patient screen is the cover image.
    _screenshot(slide, "patient", 5.9, 0.46, 3.55, 2.22, name="content_shot_patient")
    _rect(slide, 5.9, 2.86, 3.55, 1.16, BLACK, None, name="content_token_panel")
    _text(slide, 6.09, 2.98, 1.0, 0.18, "LIVE TOKEN", 6.5, LIME, CONDENSED, bold=True, spacing=1.4, name="content_label")
    _text(slide, 6.07, 3.16, 1.1, 0.64, "14", 40, WHITE, DISPLAY, line_spacing=0.8, name="content_token")
    _text(slide, 7.05, 3.05, 2.2, 0.18, "POSITION 3", 7, WHITE, CONDENSED, bold=True, spacing=1.2, name="content_label")
    _text(slide, 7.05, 3.3, 2.2, 0.4, "≈ 18 MIN", 21, LIME, DISPLAY, name="content_eta")
    _text(slide, 7.05, 3.72, 2.2, 0.16, "CARDIOLOGY / WAITING", 6.2, WHITE, CONDENSED, spacing=1.1, name="content_status")

    _rect(slide, 5.9, 4.22, 3.55, 0.55, CREAM, None, name="content_caption_panel")
    _text(slide, 6.07, 4.36, 3.2, 0.2, "One queue. Three points of view.", 8.2, BLACK, BODY, italic=True, name="content_caption")


def slide_thesis(slide):
    _page(slide, "01", "The argument")
    _headline(slide, "The waiting room should know what happens next.", 0.55, 0.9, 4.85, 2.2, 40)
    _rule(slide, 0.55, 3.42, 2.2, ORANGE, 0.05)
    _body(
        slide,
        "This deck follows the wait, the system that removes the guesswork, and the tests that prove the queue rules.",
        0.55,
        3.65,
        4.6,
        0.82,
        11.2,
    )

    stages = [
        ("01", "THE WAIT", "Why the queue feels opaque"),
        ("02", "THE SYSTEM", "How one live state replaces paper"),
        ("03", "THE PROOF", "What the test suite verifies"),
    ]
    for i, (number, title, body) in enumerate(stages):
        x = 5.42 + i * 1.42
        _text(slide, x, 1.08, 0.9, 0.55, number, 40, ORANGE if i == 0 else BLACK, DISPLAY, name="content_stage_num")
        _text(slide, x, 1.75, 1.2, 0.18, title, 6.5, BLACK, CONDENSED, bold=True, spacing=1.1, name="content_stage_label")
        _text(slide, x, 2.05, 1.15, 0.65, body, 8.1, MUTED, BODY, line_spacing=1.18, name="content_stage_body")
        _line(slide, x, 2.88, x + 1.15, 2.88, RULE, 0.65, name="content_stage_rule")

    _rect(slide, 0.55, 5.05, 8.9, 0.54, BLACK, None, name="content_strip")
    _text(slide, 0.73, 5.19, 8.4, 0.22, "From a paper token to a live queue state.", 12, WHITE, BODY, bold=True, name="content_strip_text")


def slide_problem(slide):
    _page(slide, "01", "The wait")
    _headline(slide, "98.6 minutes of waiting.\n6.5 minutes of care.", 0.55, 0.7, 7.6, 1.3, 32)
    _text(slide, 0.5, 2.02, 0.8, 0.22, "MEAN", 7, ORANGE, CONDENSED, bold=True, spacing=1.7, name="content_label")
    _rich(
        slide,
        0.42,
        2.2,
        4.3,
        1.7,
        [
            ("98.6", {"size": 132, "font": DISPLAY, "color": BLACK}),
            (" min", {"size": 15, "bold": True, "color": MUTED}),
        ],
        name="content_big_number",
    )
    _text(slide, 0.52, 3.78, 3.4, 0.45, "registration to chamber entry\nn = 432 outpatients, Kolkata", 7.6, MUTED, BODY, line_spacing=1.2, name="content_caption")

    # A physical scale comparison: waiting dwarfs the consultation.
    _text(slide, 6.15, 1.72, 1.1, 0.18, "WAIT", 7, ORANGE, CONDENSED, bold=True, spacing=1.5, name="content_label")
    _line(slide, 6.15, 1.95, 9.18, 1.95, NAVY, 5.2, name="content_wait_bar")
    _text(slide, 9.2, 1.84, 0.35, 0.2, "98.6", 7.2, NAVY, BODY, bold=True, name="content_wait_value")
    _text(slide, 6.15, 2.35, 1.35, 0.18, "CONSULTATION", 7, ORANGE, CONDENSED, bold=True, spacing=1.5, name="content_label")
    _line(slide, 6.15, 2.58, 6.38, 2.58, ORANGE, 5.2, name="content_consult_bar")
    _text(slide, 6.45, 2.47, 0.5, 0.2, "6.5", 7.2, ORANGE, BODY, bold=True, name="content_consult_value")
    _text(slide, 8.72, 2.3, 0.85, 0.55, "≈15×", 27, ORANGE, DISPLAY, name="content_ratio")
    _text(slide, 8.72, 2.84, 0.7, 0.16, "LONGER", 6.2, ORANGE, CONDENSED, bold=True, spacing=1.2, name="content_ratio_label")

    # Paper token with a red stamp.
    _rect(slide, 6.4, 3.55, 2.75, 1.42, CREAM, BLACK, 0.9, name="content_ticket")
    _text(slide, 6.55, 3.65, 1.0, 0.18, "TOKEN", 6.2, MUTED, CONDENSED, bold=True, spacing=1.5, name="content_ticket_label")
    _text(slide, 6.52, 3.9, 0.9, 0.75, "17", 50, BLACK, DISPLAY, name="content_ticket_number")
    _text(slide, 7.35, 3.78, 1.65, 0.7, "NO ETA\nNO STATUS", 10.5, MUTED, CONDENSED, bold=True, line_spacing=1.15, name="content_ticket_text")
    _rect(slide, 7.2, 4.44, 1.1, 0.26, RED, None, name="content_stamp")
    _text(slide, 7.26, 4.49, 0.98, 0.15, "NO ETA", 6.2, WHITE, CONDENSED, bold=True, spacing=1.5, align=PP_ALIGN.CENTER, name="content_stamp_text")
    _text(slide, 8.55, 4.65, 0.9, 0.16, "WAIT UNKNOWN", 5.8, RED, CONDENSED, bold=True, spacing=1.0, name="content_ticket_note")

    _rect(slide, 0.55, 5.28, 8.9, 0.44, BLACK, None, name="content_strip")
    _text(slide, 0.73, 5.4, 8.45, 0.18, "The patient carries a number, not a status.", 10.2, WHITE, BODY, bold=True, name="content_strip_text")
    _source(
        slide,
        "[1] Datta M. Indian J Community Med. 2024;49(Suppl 1):S61. Mean wait 98.6 min (SD 45.4); consultation 6.5 min (SD 13.7).",
        5.88,
    )


def slide_triptych(slide):
    _page(slide, "02", "The system")
    _headline(slide, "One queue. Three points of view.", 0.55, 0.68, 8.7, 0.75, 40)
    _text(slide, 0.55, 1.34, 8.6, 0.22, "The same state, filtered by role and permission.", 9.4, MUTED, BODY, name="content_intro")

    cards = [
        ("patient", "01", "PATIENT", "Token, position, estimate"),
        ("doctor", "02", "DOCTOR", "Queue, triage, one action"),
        ("admin", "03", "ADMIN", "People, schedules, metrics"),
    ]
    for key, number, role, caption in cards:
        x = {"patient": 0.55, "doctor": 3.55, "admin": 6.55}[key]
        _screenshot(slide, key, x, 1.66, 2.85, 1.78, name=f"content_shot_{key}")
        _text(slide, x, 3.62, 0.42, 0.18, number, 7.2, ORANGE, CONDENSED, bold=True, spacing=1.3, name="content_card_num")
        _text(slide, x + 0.45, 3.62, 1.55, 0.18, role, 7.2, BLACK, CONDENSED, bold=True, spacing=1.2, name="content_card_role")
        _text(slide, x, 3.87, 2.7, 0.34, caption, 8.6, MUTED, BODY, name="content_card_caption")
        _line(slide, x + 1.42, 4.35, x + 1.42, 4.78, RULE, 1.0, name="content_state_line")

    _line(slide, 1.98, 4.78, 7.98, 4.78, RULE, 1.0, name="content_state_spine")
    _rect(slide, 3.05, 4.99, 3.9, 0.56, BLACK, None, name="content_state_panel")
    _text(slide, 3.24, 5.16, 3.5, 0.2, "ONE QUEUE STATE", 9, LIME, CONDENSED, bold=True, spacing=1.5, align=PP_ALIGN.CENTER, name="content_state_label")
    _text(slide, 0.55, 5.75, 8.8, 0.18, "Appointments, tokens, roles, and audit events live in the same backend.", 7.4, MUTED, BODY, italic=True, name="content_note")


def slide_journey(slide):
    _page(slide, "03", "The flow")
    _headline(slide, "Six steps. One shared record.", 0.55, 0.68, 8.7, 0.75, 40)
    _text(slide, 0.55, 1.34, 8.6, 0.22, "Booked or walk-in, the patient enters the same queue.", 9.4, MUTED, BODY, name="content_intro")

    points = [
        ("01", "REGISTER", "identity + role", 0.9, 4.55),
        ("02", "DISCOVER", "doctor + department", 2.45, 4.25),
        ("03", "BOOK", "conflict-safe slot", 4.0, 3.78),
        ("04", "CHECK IN", "digital token", 5.55, 3.3),
        ("05", "TRACK", "position + estimate", 7.1, 2.85),
        ("06", "CONSULT", "doctor moves queue", 8.65, 2.38),
    ]

    # Orthogonal route: the vertical risers make the journey feel built, not floated.
    route = [
        (1.1, 4.75, 2.65, 4.75),
        (2.65, 4.75, 2.65, 4.45),
        (2.65, 4.45, 4.2, 4.45),
        (4.2, 4.45, 4.2, 3.98),
        (4.2, 3.98, 5.75, 3.98),
        (5.75, 3.98, 5.75, 3.5),
        (5.75, 3.5, 7.3, 3.5),
        (7.3, 3.5, 7.3, 3.05),
        (7.3, 3.05, 8.85, 3.05),
        (8.85, 3.05, 8.85, 2.58),
    ]
    for x1, y1, x2, y2 in route:
        _line(slide, x1, y1, x2, y2, RULE, 2.2, name="content_route")
    _arrow(slide, 8.86, 2.45, 0.35, 0.22, ORANGE, name="content_route_arrow")

    for number, label, body, x, y in points:
        color = ORANGE if number == "04" else BLACK
        _circle(slide, x, y, 0.42, BLACK, BLACK, 0.8, name="content_step_dot")
        _text(slide, x, y + 0.08, 0.42, 0.2, number, 7.2, WHITE, CONDENSED, bold=True, align=PP_ALIGN.CENTER, spacing=1.0, name="content_step_num")
        if number == "06":
            _text(slide, x - 1.55, y - 0.02, 1.35, 0.2, label, 6.8, color, CONDENSED, bold=True, align=PP_ALIGN.RIGHT, spacing=1.0, name="content_step_label")
            _text(slide, x - 1.55, y + 0.19, 1.4, 0.3, body, 7.8, MUTED, BODY, align=PP_ALIGN.RIGHT, line_spacing=1.12, name="content_step_body")
        else:
            _text(slide, x + 0.53, y - 0.02, 1.25, 0.2, label, 6.8, color, CONDENSED, bold=True, spacing=1.0, name="content_step_label")
            _text(slide, x + 0.53, y + 0.19, 1.3, 0.3, body, 7.8, MUTED, BODY, line_spacing=1.12, name="content_step_body")

    _text(slide, 5.78, 4.58, 3.62, 0.18, "DOCTOR ACTIONS → NEW POSITIONS + ESTIMATES", 6.3, ORANGE, CONDENSED, bold=True, spacing=1.0, name="content_feedback")
    _line(slide, 0.55, 5.65, 9.45, 5.65, RULE, 0.65, name="content_rule")
    _text(slide, 0.55, 5.77, 8.7, 0.18, "One patient record. Every screen sees the same state.", 7.6, MUTED, BODY, italic=True, name="content_note")


def slide_queue(slide):
    _page(slide, "04", "The rule")
    _headline(slide, "Urgency first. Arrival time breaks the tie.", 0.55, 0.68, 8.8, 0.8, 38)

    tiers = [
        ("CRITICAL", "01", ORANGE, 0.0),
        ("HIGH", "02", NAVY, 0.22),
        ("MEDIUM", "03", BLUE, 0.44),
        ("NORMAL", "04", MUTED, 0.66),
    ]
    for word, rank, color, indent in tiers:
        y = 1.68 + (int(rank) - 1) * 0.82
        _text(slide, 0.55 + indent, y, 0.4, 0.2, rank, 7.2, color, CONDENSED, bold=True, spacing=1.2, name="content_rank")
        _text(slide, 0.97 + indent, y - 0.1, 4.25, 0.68, word, 38, color, DISPLAY, line_spacing=0.86, name="content_tier")
        _line(slide, 0.97 + indent, y + 0.57, 5.95, y + 0.57, color, 0.8, name="content_tier_rule")

    _text(slide, 0.55, 5.03, 4.8, 0.4, "Clinician-assigned tier, then FIFO within the tier.", 8.4, MUTED, BODY, italic=True, name="content_note")

    _screenshot(slide, "doctor", 6.2, 1.58, 3.2, 2.0, name="content_shot_queue")
    _rect(slide, 6.2, 3.78, 3.2, 1.35, WHITE, RULE, 0.8, name="content_board")
    _text(slide, 6.36, 3.9, 2.85, 0.18, "LIVE QUEUE  /  CARDIOLOGY", 6.2, ORANGE, CONDENSED, bold=True, spacing=1.2, name="content_board_label")
    board = [
        ("#12", "CRITICAL", ORANGE),
        ("#14", "HIGH", NAVY),
        ("#15", "MEDIUM", BLUE),
        ("#17", "NORMAL", MUTED),
    ]
    for i, (token, tier, color) in enumerate(board):
        y = 4.16 + i * 0.23
        _text(slide, 6.36, y, 0.55, 0.17, token, 7.4, BLACK, CONDENSED, bold=True, name="content_board_token")
        _circle(slide, 7.02, y + 0.04, 0.08, color, None, name="content_board_dot")
        _text(slide, 7.2, y, 1.6, 0.17, tier, 6.6, color, CONDENSED, bold=True, spacing=1.0, name="content_board_tier")

    _rect(slide, 0.55, 5.55, 8.9, 0.38, BLACK, None, name="content_formula_panel")
    _text(
        slide,
        0.72,
        5.64,
        8.55,
        0.18,
        "ORDER BY  priority_rank ASC,  checked_in_at ASC",
        8.2,
        WHITE,
        MONO,
        name="content_formula",
    )
    _text(slide, 0.55, 5.96, 8.9, 0.16, "Tier labels describe software priority; the hospital's approved triage policy governs care.", 5.9, MUTED, BODY, italic=True, name="content_disclaimer")


def slide_evidence(slide):
    _page(slide, "05", "The evidence")
    _headline(slide, "The evidence measures waiting, not the full workflow.", 0.55, 0.68, 8.8, 1.35, 36)
    _text(slide, 0.55, 2.0, 8.4, 0.2, "Three outpatient studies. Three different health systems.", 9.2, MUTED, BODY, name="content_intro")

    studies = [
        ("98.6", "KOLKATA", "n = 432", "mean wait / minutes", ORANGE),
        ("161", "IRAN", "n = 160", "mean wait / minutes", NAVY),
        ("168.4", "NIGERIA", "n = 96", "mean wait / minutes", BLUE),
    ]
    for i, (number, place, sample, label, color) in enumerate(studies):
        x = 0.55 + i * 3.05
        _text(slide, x, 2.26, 2.8, 0.95, number, 66, color, DISPLAY, line_spacing=0.9, name="content_study_num")
        _text(slide, x, 3.24, 2.5, 0.18, place, 7.2, BLACK, CONDENSED, bold=True, spacing=1.4, name="content_study_place")
        _text(slide, x, 3.45, 2.55, 0.18, sample, 6.6, MUTED, CONDENSED, spacing=0.8, name="content_study_sample")
        _text(slide, x, 3.66, 2.3, 0.18, label, 7.2, MUTED, BODY, name="content_study_label")
        if i < 2:
            _line(slide, x + 2.82, 2.32, x + 2.82, 3.88, RULE, 0.65, name="content_study_rule")

    _rect(slide, 0.55, 4.12, 8.9, 0.82, BLACK, None, name="content_gap_panel")
    _text(slide, 0.75, 4.29, 8.45, 0.46, "The gap: no cited study tests booking, token, priority, and forecast as one workflow.", 15.5, WHITE, DISPLAY, line_spacing=0.96, name="content_gap")
    _text(slide, 0.55, 5.2, 8.7, 0.34, "This project builds the workflow and evaluates software behaviour. It does not claim clinical outcomes.", 8.2, MUTED, BODY, italic=True, name="content_limitation")
    _source(
        slide,
        "[1] Datta 2024. [2] Mohebbifar et al. Glob J Health Sci. 2013;6(1):172–180. [3] Oche & Adamu. Ann Med Health Sci Res. 2013;3(4):588–592.",
        5.76,
    )


def slide_architecture(slide):
    _page(slide, "06", "The engine")
    _headline(slide, "Fast path first. Intelligence stays optional.", 0.55, 0.68, 8.8, 0.8, 38)
    _text(slide, 0.55, 1.35, 8.5, 0.2, "The API owns state. The model only advises.", 9.2, MUTED, BODY, name="content_intro")

    _rect(slide, 0.55, 1.78, 2.25, 2.65, CREAM, BLACK, 0.8, name="content_client")
    _text(slide, 0.75, 1.95, 1.75, 0.2, "CLIENT", 6.5, ORANGE, CONDENSED, bold=True, spacing=1.4, name="content_label")
    _text(slide, 0.75, 2.18, 1.75, 0.5, "React\nTypeScript", 17, BLACK, DISPLAY, line_spacing=0.95, name="content_block_title")
    _text(slide, 0.75, 2.9, 1.7, 0.62, "Patient\nDoctor\nAdmin", 8.2, MUTED, BODY, line_spacing=1.42, name="content_block_list")

    _rect(slide, 3.25, 1.78, 3.38, 2.65, BLACK, None, name="content_api")
    _text(slide, 3.48, 1.96, 2.6, 0.2, "CORE SERVICE", 6.5, LIME, CONDENSED, bold=True, spacing=1.4, name="content_label")
    _text(slide, 3.48, 2.18, 2.75, 0.46, "Spring Boot", 22, WHITE, DISPLAY, name="content_block_title")
    _text(slide, 3.48, 2.65, 2.8, 0.7, "AUTH  /  APPOINTMENTS\nQUEUE  /  AUDIT", 7.8, WHITE, MONO, line_spacing=1.45, name="content_block_list")
    _text(slide, 3.48, 3.52, 2.75, 0.18, "JWT  •  FLYWAY  •  JAVA 21", 6.1, LIME, CONDENSED, bold=True, spacing=1.0, name="content_block_meta")

    _rect(slide, 7.08, 1.78, 2.35, 2.18, CREAM, BLACK, 0.8, name="content_ml")
    _text(slide, 7.28, 1.95, 1.9, 0.2, "INTELLIGENCE", 6.5, ORANGE, CONDENSED, bold=True, spacing=1.4, name="content_label")
    _text(slide, 7.28, 2.16, 1.95, 0.46, "FastAPI", 20, BLACK, DISPLAY, name="content_block_title")
    _text(slide, 7.28, 2.63, 1.95, 0.55, "Random Forest\nwait estimate", 7.6, MUTED, BODY, line_spacing=1.18, name="content_block_list")
    _text(slide, 7.28, 3.35, 1.95, 0.2, "1,200 SYNTHETIC ROWS", 5.8, ORANGE, CONDENSED, bold=True, spacing=0.8, name="content_block_meta")

    _arrow(slide, 2.9, 2.95, 0.25, 0.18, ORANGE, name="content_arrow")
    _arrow(slide, 6.72, 2.95, 0.25, 0.18, ORANGE, name="content_arrow")
    _text(slide, 2.84, 3.2, 0.38, 0.16, "REST", 5.2, MUTED, CONDENSED, bold=True, spacing=1.0, align=PP_ALIGN.CENTER, name="content_arrow_label")
    _text(slide, 6.66, 3.2, 0.38, 0.16, "REST", 5.2, MUTED, CONDENSED, bold=True, spacing=1.0, align=PP_ALIGN.CENTER, name="content_arrow_label")

    _rect(slide, 3.6, 4.83, 2.7, 0.54, NAVY, None, name="content_db")
    _text(slide, 3.76, 4.98, 2.35, 0.2, "MYSQL 8  /  JPA", 7.4, WHITE, CONDENSED, bold=True, spacing=1.1, name="content_db_text")
    _line(slide, 4.95, 4.43, 4.95, 4.83, BLACK, 1.4, name="content_db_line")

    _text(slide, 7.08, 4.15, 2.4, 0.45, "MODEL DOWN?\nFALLBACK ANSWERS.", 7.8, ORANGE, CONDENSED, bold=True, line_spacing=1.2, name="content_fallback")
    _line(slide, 7.08, 4.58, 6.4, 4.58, ORANGE, 1.0, name="content_fallback_line")
    _line(slide, 6.4, 4.58, 6.4, 4.1, ORANGE, 1.0, name="content_fallback_line")

    _rect(slide, 0.55, 5.55, 8.88, 0.36, PAPER, None, name="content_runtime")
    _text(slide, 0.72, 5.65, 8.5, 0.17, "DOCKER COMPOSE  /  FRONTEND  +  BACKEND  +  MYSQL  +  ML SIDECAR", 5.9, NAVY, CONDENSED, bold=True, spacing=0.8, name="content_runtime_text")


def slide_tools(slide):
    _page(slide, "07", "The tools")
    _headline(slide, "The stack is ordinary. The flow is not.", 0.55, 0.68, 8.7, 0.75, 42)

    groups = [
        ("JAVA 21", "BACKEND", 0.55, 1.45, BLACK, "Spring Boot 3.5  ·  Security  ·  JWT  ·  Flyway"),
        ("REACT 19", "INTERFACE", 5.15, 1.45, ORANGE, "TypeScript  ·  Vite  ·  Tailwind  ·  TanStack Query"),
        ("MYSQL 8", "DATA", 0.55, 3.45, NAVY, "JPA / Hibernate  ·  Flyway migrations"),
        ("PYTHON", "INTELLIGENCE", 5.15, 3.45, BLACK, "FastAPI  ·  scikit-learn  ·  Random Forest"),
    ]
    for title, category, x, y, color, details in groups:
        _text(slide, x, y, 1.0, 0.18, category, 6.5, ORANGE, CONDENSED, bold=True, spacing=1.5, name="content_label")
        _text(slide, x, y + 0.18, 3.8, 0.82, title, 55, color, DISPLAY, line_spacing=0.88, name="content_tool")
        _line(slide, x, y + 1.08, x + 3.55, y + 1.08, RULE, 0.65, name="content_tool_rule")
        _text(slide, x, y + 1.2, 3.7, 0.4, details, 7.6, MUTED, BODY, line_spacing=1.2, name="content_tool_detail")

    _rect(slide, 0.55, 5.55, 8.9, 0.36, LIME, None, name="content_quality_panel")
    _text(slide, 0.72, 5.64, 8.45, 0.18, "JUNIT 5   /   PYTEST   /   VITEST   /   DOCKER COMPOSE", 6.2, BLACK, CONDENSED, bold=True, spacing=1.0, name="content_quality")


def slide_product(slide):
    _page(slide, "08", "The product")
    _headline(slide, "The clinic, on one screen.", 0.55, 0.66, 6.4, 0.7, 36)
    _text(slide, 0.55, 1.3, 6.2, 0.2, "Doctor workspace: today's list, live queue, one action.", 8.8, MUTED, BODY, name="content_intro")

    _screenshot(slide, "doctor", 0.55, 1.58, 7.0, 4.28, name="content_shot_hero")
    _text(slide, 7.9, 1.62, 0.55, 0.18, "TODAY", 6.4, ORANGE, CONDENSED, bold=True, spacing=1.3, name="content_label")
    _text(slide, 7.88, 1.8, 1.55, 0.7, "12", 44, BLACK, DISPLAY, name="content_metric")
    _text(slide, 7.9, 2.45, 1.4, 0.18, "appointments", 7.2, MUTED, BODY, name="content_metric_label")
    _line(slide, 7.9, 2.78, 9.42, 2.78, RULE, 0.65, name="content_metric_rule")

    _text(slide, 7.9, 2.96, 0.55, 0.18, "NOW", 6.4, ORANGE, CONDENSED, bold=True, spacing=1.3, name="content_label")
    _text(slide, 7.88, 3.14, 1.55, 0.7, "5", 44, ORANGE, DISPLAY, name="content_metric")
    _text(slide, 7.9, 3.79, 1.4, 0.18, "waiting", 7.2, MUTED, BODY, name="content_metric_label")
    _line(slide, 7.9, 4.12, 9.42, 4.12, RULE, 0.65, name="content_metric_rule")

    _text(slide, 7.9, 4.3, 1.0, 0.18, "IN ROOM", 6.4, ORANGE, CONDENSED, bold=True, spacing=1.3, name="content_label")
    _text(slide, 7.88, 4.48, 1.55, 0.7, "1", 44, BLACK, DISPLAY, name="content_metric")
    _text(slide, 7.9, 5.13, 1.45, 0.18, "consultation", 7.2, MUTED, BODY, name="content_metric_label")

    _text(slide, 0.55, 5.94, 8.6, 0.16, "Screenshot from the working build. Demo data only.", 6.1, MUTED, BODY, italic=True, name="content_caption")


def slide_results(slide):
    _page(slide, "09", "The proof")
    _headline(slide, "The rules survive the test suite.", 0.55, 0.68, 8.8, 0.75, 40)

    _text(slide, 0.42, 1.55, 3.55, 1.9, "247", 138, ORANGE, DISPLAY, line_spacing=0.72, name="content_big_test")
    _text(slide, 0.55, 3.48, 2.8, 0.42, "backend tests\n0 failures, 0 errors", 8.2, BLACK, BODY, bold=True, line_spacing=1.2, name="content_test_caption")
    _line(slide, 4.15, 1.72, 4.15, 4.45, RULE, 0.8, name="content_metric_rule")

    metrics = [
        ("19", "PYTHON TESTS", 4.55, 1.72),
        ("41", "FRONTEND TESTS", 6.7, 1.72),
        ("0", "FAILED TESTS", 8.58, 1.72),
    ]
    for number, label, x, y in metrics:
        _text(slide, x, y, 1.45, 0.8, number, 44, BLACK, DISPLAY, name="content_metric")
        _text(slide, x, y + 0.72, 1.55, 0.18, label, 5.9, ORANGE, CONDENSED, bold=True, spacing=1.0, name="content_metric_label")
        _line(slide, x, y + 1.04, x + 1.4, y + 1.04, RULE, 0.65, name="content_metric_rule")

    _text(slide, 4.55, 3.08, 4.6, 0.65, "The production build also passes, and the API contract is documented.", 8.4, MUTED, BODY, line_spacing=1.2, name="content_build_note")

    _rect(slide, 0.55, 4.84, 8.9, 0.82, BLACK, None, name="content_invariants")
    invariants = [("01", "UNIQUE TOKEN"), ("02", "PRIORITY ORDER"), ("03", "ROLE ISOLATION"), ("04", "ML FALLBACK")]
    for i, (num, label) in enumerate(invariants):
        x = 0.76 + i * 2.16
        _text(slide, x, 5.03, 0.3, 0.16, num, 5.8, LIME, CONDENSED, bold=True, spacing=1.0, name="content_invariant_num")
        _text(slide, x, 5.25, 1.8, 0.16, label, 6.6, WHITE, CONDENSED, bold=True, spacing=1.0, name="content_invariant")


def slide_conclusion(slide):
    _page(slide, "10", "The next step")
    _headline(slide, "From a working queue to a trusted service.", 0.55, 0.72, 4.95, 1.5, 38)
    _body(
        slide,
        "The prototype closes the loop between booking and consultation. The next work is reach, evidence, and operational hardening.",
        0.55,
        2.4,
        4.9,
        1.0,
        10.2,
    )

    future = [
        ("01", "REACH", "SMS, push, mobile"),
        ("02", "SCALE", "Multi-hospital tenancy"),
        ("03", "EVIDENCE", "Real data, monitored models"),
    ]
    for i, (num, head, body) in enumerate(future):
        y = 1.45 + i * 1.18
        _text(slide, 5.95, y, 0.82, 0.5, num, 26, ORANGE, DISPLAY, name="content_future_num")
        _text(slide, 6.85, y - 0.02, 2.5, 0.2, head, 7, BLACK, CONDENSED, bold=True, spacing=1.3, name="content_future_head")
        _text(slide, 6.85, y + 0.2, 2.55, 0.48, body, 9, MUTED, BODY, name="content_future_body")
        _line(slide, 6.85, y + 0.78, 9.42, y + 0.78, RULE, 0.65, name="content_future_rule")

    _text(slide, 0.55, 5.38, 4.8, 0.48, "Clinical outcomes and real-hospital time savings remain unmeasured.", 7.6, MUTED, BODY, italic=True, line_spacing=1.2, name="content_limitation")


def slide_references(slide):
    _page(slide, "11", "Sources")
    _headline(slide, "Sources.", 0.55, 0.67, 3.2, 0.7, 42)
    _text(slide, 0.55, 1.35, 8.5, 0.2, "Research, standards, and official documentation used in this deck.", 8.8, MUTED, BODY, name="content_intro")

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
            y = 1.72 + i * 1.02
            _text(slide, x, y, 0.42, 0.2, num, 7, ORANGE, CONDENSED, bold=True, spacing=1.2, name="content_ref_num")
            _text(slide, x + 0.45, y - 0.02, 3.75, 0.88, text, 7.1, INK, BODY, line_spacing=1.15, name="content_ref")
    _text(slide, 0.55, 5.85, 8.8, 0.18, "URLs accessed 19 November 2026. The ML model uses synthetic demonstration data.", 6.2, MUTED, BODY, italic=True, name="content_ref_note")


def slide_thanks(slide):
    _page(slide, "12", "Close")
    _text(slide, 0.55, 1.4, 6.2, 1.4, "Thank you.", 82, BLACK, DISPLAY, line_spacing=0.82, name="content_title")
    _text(slide, 0.58, 2.92, 5.4, 0.35, "Smart Hospital: from booking to consultation.", 13, MUTED, BODY, italic=True, name="content_subtitle")

    _text(slide, 0.55, 4.25, 3.9, 0.2, "TEAM SY2207", 6.5, ORANGE, CONDENSED, bold=True, spacing=1.5, name="content_label")
    names = [
        "Uday Mhaske  ·  ADT24SOCB1735",
        "Aditya Sundalam  ·  MITU22BTCS0044",
        "Atharv Shewale  ·  ADT25SOCB1715",
        "Saiprasad Bendre  ·  ADT25SOCB1651",
    ]
    for i, name in enumerate(names):
        _text(slide, 0.55, 4.55 + i * 0.3, 5.5, 0.2, name, 7.8, INK, BODY, name="content_team")
    _text(slide, 0.55, 5.84, 4.8, 0.18, "Guided by Dr. P. D. Patil", 7.2, MUTED, BODY, name="content_guide")

    _rect(slide, 6.65, 1.55, 2.55, 3.45, CREAM, BLACK, 0.9, name="content_thanks_ticket")
    _text(slide, 6.88, 1.78, 2.0, 0.2, "LIVE TOKEN", 6.8, ORANGE, CONDENSED, bold=True, spacing=1.5, name="content_ticket_label")
    _text(slide, 6.78, 2.2, 2.1, 1.55, "14", 120, BLACK, DISPLAY, line_spacing=0.7, name="content_thanks_number")
    _text(slide, 6.9, 4.2, 1.9, 0.42, "POSITION 3  /  ≈18 MIN", 7.2, MUTED, CONDENSED, bold=True, spacing=1.0, name="content_thanks_meta")
    _rect(slide, 9.2, 1.55, 0.26, 3.45, ORANGE, None, name="content_thanks_bar")


def slide_qanda(slide):
    _page(slide, "13", "Questions")
    _rich(
        slide,
        0.48,
        1.15,
        8.9,
        1.55,
        [
            ("Q", {"size": 128, "font": DISPLAY, "color": ORANGE}),
            ("&", {"size": 92, "font": DISPLAY, "color": BLACK}),
            ("A.", {"size": 128, "font": DISPLAY, "color": BLACK}),
        ],
        line_spacing=0.8,
        name="content_title",
    )
    _text(slide, 0.55, 3.0, 7.2, 0.35, "Ask about the queue rules, the ML fallback, or the next step.", 13, MUTED, BODY, name="content_subtitle")

    prompts = [
        ("01", "QUEUE", "priority, FIFO, overrides"),
        ("02", "MODEL", "synthetic data, fallback"),
        ("03", "PILOT", "what to measure next"),
    ]
    for i, (num, head, body) in enumerate(prompts):
        x = 0.55 + i * 2.95
        _text(slide, x, 4.5, 0.42, 0.18, num, 6.8, ORANGE, CONDENSED, bold=True, spacing=1.2, name="content_prompt_num")
        _line(slide, x, 4.72, x + 2.55, 4.72, RULE, 0.7, name="content_prompt_rule")
        _text(slide, x, 4.88, 2.5, 0.2, head, 6.8, BLACK, CONDENSED, bold=True, spacing=1.3, name="content_prompt_head")
        _text(slide, x, 5.12, 2.55, 0.25, body, 8.2, MUTED, BODY, name="content_prompt_body")


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

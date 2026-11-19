#!/usr/bin/env python3
"""Build the redesigned Smart Hospital deck on the supplied MITSoC template."""

from __future__ import annotations

import argparse
import hashlib
from pathlib import Path
from typing import Sequence

from PIL import Image
from pptx import Presentation
from pptx.dml.color import RGBColor
from pptx.enum.shapes import MSO_CONNECTOR, MSO_SHAPE, MSO_SHAPE_TYPE
from pptx.enum.text import MSO_ANCHOR, PP_ALIGN
from pptx.util import Inches, Pt

ROOT = Path(__file__).resolve().parent
LOCAL_TEMPLATE = ROOT / "assets" / "Final_Project_Presentation_format.pptx"
DOWNLOADED_TEMPLATE = Path("/Users/himanshu/Downloads/Final_Project_Presentation_format.pptx")
DEFAULT_TEMPLATE = LOCAL_TEMPLATE if LOCAL_TEMPLATE.exists() else DOWNLOADED_TEMPLATE
DEFAULT_OUTPUT = ROOT / "Smart_Hospital_Redesigned_Presentation.pptx"

# Palette sampled from the fixed brand furniture.
NAVY = RGBColor(0x18, 0x2B, 0x5C)
NAVY_LIGHT = RGBColor(0xE9, 0xEF, 0xF7)
BLUE = RGBColor(0x2F, 0x5D, 0x9E)
RED = RGBColor(0xD7, 0x1F, 0x2D)
RED_LIGHT = RGBColor(0xFB, 0xED, 0xEE)
INK = RGBColor(0x20, 0x24, 0x2B)
MUTED = RGBColor(0x67, 0x6E, 0x7A)
RULE = RGBColor(0xD8, 0xDD, 0xE5)
PALE = RGBColor(0xF4, 0xF6, 0xF9)
WHITE = RGBColor(0xFF, 0xFF, 0xFF)
ORANGE = RGBColor(0xD9, 0x72, 0x2B)
GREEN = RGBColor(0x3A, 0x7A, 0x5B)

FONT_DISPLAY = "Century Schoolbook"
FONT_BODY = "Poppins"

MARGIN = 0.52

FOOTER_TEXT = "Department of Computer Science & Engineering"


def _name(shape, name: str):
    shape.name = name
    return shape


def _no_line(shape) -> None:
    shape.line.fill.background()


def _rect(
    slide,
    x: float,
    y: float,
    w: float,
    h: float,
    fill: RGBColor | None = WHITE,
    line: RGBColor | None = None,
    line_width: float = 0.8,
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
        _no_line(shape)
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
    color: RGBColor = RULE,
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
    w: float = 0.18,
    h: float = 0.14,
    color: RGBColor = RED,
    name: str = "content_arrow",
):
    shape = slide.shapes.add_shape(MSO_SHAPE.RIGHT_ARROW, Inches(x), Inches(y), Inches(w), Inches(h))
    shape.fill.solid()
    shape.fill.fore_color.rgb = color
    _no_line(shape)
    shape.shadow.inherit = False
    return _name(shape, name)


def _circle(
    slide,
    x: float,
    y: float,
    d: float,
    fill: RGBColor | None = WHITE,
    line: RGBColor | None = NAVY,
    line_width: float = 1.0,
    name: str = "content_dot",
):
    shape = slide.shapes.add_shape(MSO_SHAPE.OVAL, Inches(x), Inches(y), Inches(d), Inches(d))
    if fill is None:
        shape.fill.background()
    else:
        shape.fill.solid()
        shape.fill.fore_color.rgb = fill
    if line is None:
        _no_line(shape)
    else:
        shape.line.color.rgb = line
        shape.line.width = Pt(line_width)
    shape.shadow.inherit = False
    return _name(shape, name)


def _text(
    slide,
    x: float,
    y: float,
    w: float,
    h: float,
    text: str,
    size: float,
    color: RGBColor = INK,
    font: str = FONT_BODY,
    bold: bool = False,
    italic: bool = False,
    align=PP_ALIGN.LEFT,
    anchor=MSO_ANCHOR.TOP,
    line_spacing: float = 1.0,
    name: str = "content_text",
    margins: tuple[float, float, float, float] = (0, 0, 0, 0),
):
    box = slide.shapes.add_textbox(Inches(x), Inches(y), Inches(w), Inches(h))
    tf = box.text_frame
    tf.clear()
    tf.word_wrap = True
    tf.margin_left = Inches(margins[0])
    tf.margin_right = Inches(margins[1])
    tf.margin_top = Inches(margins[2])
    tf.margin_bottom = Inches(margins[3])
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
    return _name(box, name)


def _rich_text(
    slide,
    x: float,
    y: float,
    w: float,
    h: float,
    segments: Sequence[tuple[str, dict]],
    size: float = 12,
    color: RGBColor = INK,
    font: str = FONT_BODY,
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
        run.font.name = overrides.get("font", font)
        run.font.size = Pt(overrides.get("size", size))
        run.font.bold = overrides.get("bold", False)
        run.font.italic = overrides.get("italic", False)
        run.font.color.rgb = overrides.get("color", color)
    return _name(box, name)


def _image_contain(slide, path: Path, x: float, y: float, w: float, h: float, name: str):
    with Image.open(path) as im:
        iw, ih = im.size
    scale = min(w / iw, h / ih)
    draw_w, draw_h = iw * scale, ih * scale
    draw_x = x + (w - draw_w) / 2
    draw_y = y + (h - draw_h) / 2
    pic = slide.shapes.add_picture(
        str(path), Inches(draw_x), Inches(draw_y), Inches(draw_w), Inches(draw_h)
    )
    return _name(pic, name)


def _label(
    slide,
    x: float,
    y: float,
    w: float,
    text: str,
    color: RGBColor = RED,
    size: float = 7.5,
    align=PP_ALIGN.LEFT,
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
        FONT_BODY,
        bold=True,
        align=align,
        name=name,
    )


def _section_title(slide, index: str, section: str, title: str, size: float = 22):
    _label(slide, MARGIN, 0.28, 2.8, f"{index} / {section}", RED, 7.5)
    _text(
        slide,
        MARGIN,
        0.46,
        8.95,
        0.92,
        title,
        size,
        NAVY,
        FONT_DISPLAY,
        bold=True,
        line_spacing=0.95,
        name="content_title",
    )
    _rect(slide, MARGIN, 1.39, 0.52, 0.045, RED, None, name="content_rule")


def _citation(slide, text: str, y: float = 5.73):
    _text(slide, MARGIN, y, 8.9, 0.3, text, 7.5, MUTED, FONT_BODY, italic=True, name="content_citation")


def _footer_signature(slide) -> tuple[str, str | None]:
    footer_text = ""
    logo_hash = None
    for shape in slide.shapes:
        text = shape.text.strip() if getattr(shape, "has_text_frame", False) else ""
        is_footer_zone = shape.top is not None and shape.top >= Inches(6.15)
        if is_footer_zone and FOOTER_TEXT in text:
            footer_text = text
        if shape.shape_type == MSO_SHAPE_TYPE.PICTURE and is_footer_zone:
            logo_hash = hashlib.sha256(shape.image.blob).hexdigest()
    return footer_text, logo_hash


def _validate_footer_signature(signature: list[tuple[str, str | None]]) -> None:
    for slide_number, (text, logo_hash) in enumerate(signature, 1):
        if not text or not logo_hash:
            raise RuntimeError(
                f"Footer or logo could not be identified on slide {slide_number}"
            )


def strip_content(slide) -> None:
    """Remove body shapes and keep the supplied footer furniture in place."""
    for shape in list(slide.shapes):
        if shape.top is not None and shape.top >= Inches(6.15):
            continue
        shape._element.getparent().remove(shape._element)


def _keep_footer_baseline(prs: Presentation):
    return [(_footer_signature(s)) for s in prs.slides]


def _screenshots():
    return {
        "patient": ROOT / "assets" / "patient.png",
        "doctor": ROOT / "assets" / "doctor.png",
        "admin": ROOT / "assets" / "admin.png",
    }


def build_cover(slide):
    _label(slide, 0.6, 0.5, 8.8, "PROJECT PRESENTATION  •  2026–27", INK, 8, PP_ALIGN.CENTER)
    _text(
        slide,
        0.55,
        1.02,
        8.9,
        0.55,
        "SMART HOSPITAL",
        30,
        NAVY,
        FONT_DISPLAY,
        bold=True,
        align=PP_ALIGN.CENTER,
        name="content_title",
    )
    _text(
        slide,
        0.65,
        1.58,
        8.7,
        0.7,
        "Patient Queue & Appointment Management System",
        19,
        NAVY,
        FONT_DISPLAY,
        bold=True,
        align=PP_ALIGN.CENTER,
        name="content_subtitle",
    )
    _rect(slide, 4.55, 2.38, 0.9, 0.05, RED, None, name="content_rule")
    _text(
        slide,
        1.1,
        2.62,
        7.8,
        0.35,
        "One clinic flow, from booking to consultation.",
        12.5,
        MUTED,
        FONT_BODY,
        italic=True,
        align=PP_ALIGN.CENTER,
        name="content_kicker",
    )

    names = [
        ("Uday Mhaske", "ADT24SOCB1735"),
        ("Aditya Sundalam", "MITU22BTCS0044"),
        ("Atharv Shewale", "ADT25SOCB1715"),
        ("Saiprasad Bendre", "ADT25SOCB1651"),
    ]
    x_positions = [1.05, 5.25]
    y_positions = [3.38, 4.18]
    for i, (name, roll) in enumerate(names):
        x = x_positions[i % 2]
        y = y_positions[i // 2]
        _text(slide, x, y, 3.5, 0.27, name, 11, INK, FONT_BODY, bold=True, name="content_team")
        _text(slide, x, y + 0.25, 3.5, 0.22, roll, 7.5, MUTED, FONT_BODY, name="content_roll")

    _line(slide, 1.05, 4.72, 8.95, 4.72, RULE, 0.8, name="content_rule")
    _text(slide, 1.05, 4.9, 1.1, 0.25, "GUIDED BY", 7, RED, FONT_BODY, bold=True, name="content_label")
    _text(
        slide,
        1.05,
        5.12,
        7.4,
        0.3,
        "Dr. P. D. Patil",
        13,
        NAVY,
        FONT_DISPLAY,
        bold=True,
        name="content_guide",
    )
    _text(
        slide,
        1.05,
        5.42,
        7.6,
        0.55,
        "MIT Art, Design and Technology University  •  Loni-Kalbhor, Pune",
        9.5,
        MUTED,
        FONT_BODY,
        name="content_department",
    )


def build_roadmap(slide):
    _section_title(slide, "00", "ROADMAP", "The presentation follows one patient’s path through the clinic.")
    _text(
        slide,
        0.55,
        1.62,
        8.8,
        0.3,
        "Five moves: diagnose the wait, define the rules, connect the services, test the behavior, and plan the next step.",
        10,
        MUTED,
        FONT_BODY,
    )

    xs = [0.95, 2.94, 4.93, 6.92, 8.91]
    _line(slide, 1.05, 3.18, 9.02, 3.18, RULE, 1.2, name="content_timeline")
    stages = [
        ("01", "DIAGNOSE", "Expose the wait"),
        ("02", "DEFINE", "Set queue rules"),
        ("03", "DESIGN", "Connect services"),
        ("04", "PROVE", "Test invariants"),
        ("05", "EXTEND", "Plan the rollout"),
    ]
    for i, (num, label, desc) in enumerate(stages):
        x = xs[i]
        color = RED if i == 0 else NAVY
        _circle(slide, x, 2.98, 0.4, WHITE, color, 1.4, name="content_node")
        _text(
            slide,
            x,
            3.0,
            0.4,
            0.3,
            num,
            7,
            color,
            FONT_BODY,
            bold=True,
            align=PP_ALIGN.CENTER,
            anchor=MSO_ANCHOR.MIDDLE,
            name="content_node_text",
        )
        _text(
            slide,
            x - 0.55,
            3.58,
            1.5,
            0.2,
            label,
            7,
            color,
            FONT_BODY,
            bold=True,
            align=PP_ALIGN.CENTER,
            name="content_stage_label",
        )
        _text(
            slide,
            x - 0.68,
            3.85,
            1.76,
            0.55,
            desc,
            11,
            INK,
            FONT_DISPLAY,
            bold=True,
            align=PP_ALIGN.CENTER,
            name="content_stage",
        )

    _rect(slide, 0.55, 4.83, 8.9, 0.78, PALE, None, name="content_panel")
    _text(slide, 0.75, 4.99, 1.1, 0.2, "THE ARC", 7, RED, FONT_BODY, bold=True, name="content_label")
    _text(
        slide,
        0.75,
        5.21,
        8.4,
        0.24,
        "Problem  →  System  →  Evidence  →  Next step",
        11,
        NAVY,
        FONT_BODY,
        bold=True,
        name="content_arc",
    )


def build_problem(slide):
    _section_title(slide, "01", "THE PROBLEM", "Manual queues make waiting opaque, long, and hard to prioritize.")

    _text(slide, 0.55, 1.61, 1.5, 0.2, "REPORTED WAIT", 7, RED, FONT_BODY, bold=True, name="content_label")
    _rich_text(
        slide,
        0.55,
        1.82,
        3.2,
        0.85,
        [
            ("98.6", {"size": 53, "font": FONT_DISPLAY, "bold": True, "color": NAVY}),
            (" min", {"size": 15, "color": MUTED, "bold": True}),
        ],
        name="content_stat",
    )
    _text(
        slide,
        0.58,
        2.68,
        3.0,
        0.48,
        "mean registration to chamber entry\nin one Kolkata tertiary hospital (n = 432)",
        8.7,
        MUTED,
        FONT_BODY,
        line_spacing=1.2,
        name="content_caption",
    )

    _text(slide, 4.35, 1.61, 1.5, 0.2, "CONSULTATION", 7, BLUE, FONT_BODY, bold=True, name="content_label")
    _rich_text(
        slide,
        4.35,
        1.82,
        2.1,
        0.62,
        [
            ("6.5", {"size": 31, "font": FONT_DISPLAY, "bold": True, "color": BLUE}),
            (" min", {"size": 11, "color": MUTED, "bold": True}),
        ],
        name="content_stat",
    )
    _text(
        slide,
        5.63,
        1.93,
        1.45,
        0.4,
        "≈15× shorter\nthan the wait",
        9,
        BLUE,
        FONT_BODY,
        bold=True,
        line_spacing=1.15,
        name="content_ratio",
    )

    _text(slide, 7.25, 1.61, 2.2, 0.2, "THE EXPERIENCE", 7, RED, FONT_BODY, bold=True, name="content_label")
    rows = [
        ("Opaque", "No visible position or expected wait."),
        ("Unordered", "Urgent cases can sit behind routine ones."),
        ("Unrecorded", "Manual calls leave a weak audit trail."),
    ]
    for i, (head, body) in enumerate(rows):
        y = 1.9 + i * 0.76
        _rect(slide, 7.25, y, 0.035, 0.53, RED if i == 0 else NAVY, None, name="content_rule")
        _text(slide, 7.42, y - 0.01, 1.95, 0.22, head, 10.5, INK, FONT_BODY, bold=True, name="content_row_head")
        _text(slide, 7.42, y + 0.23, 2.0, 0.32, body, 8.2, MUTED, FONT_BODY, name="content_row_body")

    _line(slide, 0.55, 4.28, 9.45, 4.28, RULE, 0.8, name="content_rule")
    _text(slide, 0.55, 4.48, 1.6, 0.2, "WHY IT PERSISTS", 7, RED, FONT_BODY, bold=True, name="content_label")
    _text(
        slide,
        0.55,
        4.72,
        8.75,
        0.65,
        "Paper tokens encode a place in line but not the state of the line. Staff carry the missing information in memory and on the wall.",
        11.2,
        INK,
        FONT_DISPLAY,
        line_spacing=1.15,
        name="content_insight",
    )
    _citation(
        slide,
        "[1] Datta M. Indian J Community Med. 2024;49(Suppl 1):S61. Mean wait 98.6 min (SD 45.4); consultation 6.5 min (SD 13.7).",
        5.64,
    )


def build_objective(slide):
    _section_title(slide, "02", "THE DESIGN PROMISE", "The project turns one fragmented OPD journey into one auditable flow.")
    _label(slide, 0.55, 1.58, 3.3, "DESIGN TARGET", RED, 7)
    _text(
        slide,
        0.55,
        1.82,
        3.45,
        1.25,
        "One flow.\nThree roles.\nOne live queue.",
        28,
        NAVY,
        FONT_DISPLAY,
        bold=True,
        line_spacing=0.94,
        name="content_big",
    )
    _text(
        slide,
        0.58,
        3.4,
        3.35,
        0.95,
        "Replace paper tokens with a shared state that patients can see, doctors can move, and administrators can audit.",
        10.2,
        INK,
        FONT_BODY,
        line_spacing=1.2,
        name="content_paragraph",
    )
    _rect(slide, 0.55, 4.72, 3.35, 0.75, NAVY_LIGHT, None, name="content_panel")
    _text(slide, 0.72, 4.86, 2.95, 0.45, "Measure uncertainty first.\nMeasure clinical impact later.", 8.8, NAVY, FONT_BODY, bold=True, line_spacing=1.15, name="content_note")

    _line(slide, 4.9, 1.72, 4.9, 5.42, RULE, 1.0, name="content_spine")
    roles = [
        ("PATIENT", "Book a slot. Check in. Carry a token. See the position and estimate.", RED),
        ("DOCTOR", "Call the next patient. Assign priority. Serve, skip, complete, or override with an audit note.", NAVY),
        ("ADMIN", "Manage users, doctors, schedules, appointments, and the operational queue view.", BLUE),
    ]
    for i, (role, body, color) in enumerate(roles):
        y = 1.72 + i * 1.27
        _circle(slide, 4.75, y + 0.03, 0.28, WHITE, color, 1.5, name="content_role_dot")
        _text(slide, 4.81, y + 0.065, 0.16, 0.15, str(i + 1), 6.5, color, FONT_BODY, bold=True, align=PP_ALIGN.CENTER, name="content_role_num")
        _text(slide, 5.18, y - 0.02, 1.15, 0.24, role, 8, color, FONT_BODY, bold=True, name="content_role")
        _text(slide, 6.22, y - 0.03, 3.15, 0.9, body, 9.5, INK, FONT_BODY, line_spacing=1.16, name="content_role_body")

    _line(slide, 0.55, 5.48, 9.45, 5.48, RULE, 0.8, name="content_rule")
    _text(slide, 0.55, 5.61, 8.9, 0.22, "The queue rules are software rules. Clinical priority remains a hospital policy decision.", 7.8, MUTED, FONT_BODY, italic=True, name="content_disclaimer")


def build_journey(slide):
    _section_title(slide, "03", "PATIENT JOURNEY", "Every patient follows the same six-step digital journey.")
    _text(slide, 0.55, 1.58, 8.8, 0.3, "Each step writes to one shared state. The patient sees a token; the doctor sees the queue.", 10.2, MUTED, FONT_BODY, name="content_intro")

    steps = [
        ("01", "REGISTER", "Create identity\nand role"),
        ("02", "DISCOVER", "Choose department\nand doctor"),
        ("03", "BOOK", "Reserve a\nconflict-safe slot"),
        ("04", "CHECK IN", "Receive a digital\ntoken"),
        ("05", "TRACK", "See position and\nestimated wait"),
        ("06", "CONSULT", "Doctor updates\nthe queue"),
    ]
    xs = [0.75, 2.28, 3.81, 5.34, 6.87, 8.4]
    _line(slide, 0.88, 3.08, 9.12, 3.08, RULE, 1.3, name="content_journey_line")
    for i, (num, label, body) in enumerate(steps):
        x = xs[i]
        color = RED if i in (3, 4) else NAVY
        _circle(slide, x, 2.85, 0.45, WHITE, color, 1.3, name="content_step_dot")
        _text(slide, x, 2.92, 0.45, 0.25, num, 6.8, color, FONT_BODY, bold=True, align=PP_ALIGN.CENTER, anchor=MSO_ANCHOR.MIDDLE, name="content_step_num")
        _text(slide, x - 0.06, 2.4, 1.28, 0.2, label, 7, color, FONT_BODY, bold=True, align=PP_ALIGN.CENTER, name="content_step_label")
        _text(slide, x - 0.12, 3.48, 1.42, 0.55, body, 8.6, INK, FONT_BODY, align=PP_ALIGN.CENTER, line_spacing=1.16, name="content_step_body")

    _rect(slide, 0.55, 4.35, 8.9, 0.72, NAVY_LIGHT, None, name="content_panel")
    _text(slide, 0.75, 4.49, 2.0, 0.2, "FEEDBACK LOOP", 7, NAVY, FONT_BODY, bold=True, name="content_label")
    _text(
        slide,
        0.75,
        4.7,
        8.45,
        0.24,
        "When the doctor serves or skips a patient, every waiting token gets a new position and estimate.",
        9.4,
        NAVY,
        FONT_BODY,
        name="content_loop",
    )

    _line(slide, 0.75, 5.45, 9.25, 5.45, RULE, 0.8, name="content_rule")
    _text(slide, 0.75, 5.58, 8.6, 0.23, "The same flow supports walk-in check-in and booked appointments, without two parallel queues.", 7.8, MUTED, FONT_BODY, italic=True, name="content_note")


def build_queue(slide):
    _section_title(slide, "04", "QUEUE INTELLIGENCE", "Urgency determines priority; arrival time settles ties.")
    _text(slide, 0.55, 1.57, 4.4, 0.24, "ONE ORDERING RULE", 7, RED, FONT_BODY, bold=True, name="content_label")
    _rect(slide, 0.55, 1.81, 4.25, 0.5, PALE, RULE, 0.7, name="content_formula_box")
    _text(slide, 0.7, 1.93, 3.95, 0.24, "sort(priority rank, check-in time)", 10, NAVY, FONT_BODY, bold=True, name="content_formula")

    rows = [
        ("CRITICAL", "rank 1", "Seen before all other tiers", RED),
        ("HIGH", "rank 2", "Seen before medium and normal", ORANGE),
        ("MEDIUM", "rank 3", "Seen before normal", BLUE),
        ("NORMAL", "rank 4", "FIFO within the tier", MUTED),
    ]
    for i, (tier, rank, desc, color) in enumerate(rows):
        y = 2.55 + i * 0.66
        _rect(slide, 0.55, y, 0.11, 0.48, color, None, name="content_tier_bar")
        _text(slide, 0.8, y - 0.01, 1.25, 0.22, tier, 9.5, color, FONT_BODY, bold=True, name="content_tier")
        _text(slide, 2.05, y + 0.01, 0.7, 0.18, rank, 7, MUTED, FONT_BODY, name="content_rank")
        _text(slide, 2.72, y + 0.01, 1.95, 0.25, desc, 7.8, INK, FONT_BODY, name="content_tier_desc")

    _rect(slide, 5.2, 1.55, 4.25, 3.72, WHITE, RULE, 0.8, name="content_board")
    _rect(slide, 5.2, 1.55, 4.25, 0.36, NAVY, None, name="content_board_head")
    _text(slide, 5.34, 1.64, 3.8, 0.2, "LIVE QUEUE  •  CARDIOLOGY", 6.8, WHITE, FONT_BODY, bold=True, name="content_board_title")

    queue_rows = [
        ("#12", "CRITICAL", "NOW", RED),
        ("#14", "HIGH", "NEXT", ORANGE),
        ("#15", "MEDIUM", "WAIT", BLUE),
        ("#17", "NORMAL", "WAIT", MUTED),
    ]
    for i, (token, tier, state, color) in enumerate(queue_rows):
        y = 2.12 + i * 0.62
        _text(slide, 5.38, y, 0.55, 0.24, token, 11, INK, FONT_BODY, bold=True, name="content_queue_token")
        _text(slide, 6.04, y + 0.01, 1.1, 0.2, tier, 6.8, color, FONT_BODY, bold=True, name="content_queue_tier")
        _text(slide, 7.25, y + 0.01, 0.8, 0.2, state, 6.8, color if state == "NOW" else MUTED, FONT_BODY, bold=True, name="content_queue_state")
        if i < 3:
            _line(slide, 5.36, y + 0.39, 9.28, y + 0.39, RULE, 0.55, name="content_queue_rule")

    _rect(slide, 5.4, 4.55, 3.86, 0.48, RED_LIGHT, None, name="content_token_panel")
    _text(slide, 5.55, 4.67, 3.6, 0.22, "#14  •  POSITION 3  •  ESTIMATE ≈ 18 MIN", 7.8, RED, FONT_BODY, bold=True, name="content_token_detail")

    _text(
        slide,
        0.55,
        5.25,
        8.9,
        0.28,
        "Priority tiers are clinician-assigned software categories. The hospital’s approved triage policy governs their use.",
        7.8,
        MUTED,
        FONT_BODY,
        italic=True,
        name="content_disclaimer",
    )
    _citation(slide, "Queue order: priority rank, then check-in time (FIFO within tier). Machine-learning estimate shown only as a decision aid.", 5.6)


def build_literature(slide):
    _section_title(slide, "05", "EVIDENCE & GAP", "The evidence measures waiting; the project operationalizes the journey.")
    _text(slide, 0.55, 1.57, 5.4, 0.24, "WHAT THE STUDIES SHOW", 7, RED, FONT_BODY, bold=True, name="content_label")

    studies = [
        ("01", "Kolkata, India", "n = 432 outpatients", "98.6 min mean wait\n6.5 min consultation", "[1]"),
        ("02", "Iran", "n = 160 outpatients", "161 min mean wait\nvariation by clinic", "[2]"),
        ("03", "Nigeria", "n = 96 responses", "168.4 min mean wait\n7.1 min consultation", "[3]"),
    ]
    for i, (num, place, sample, finding, ref) in enumerate(studies):
        y = 1.84 + i * 1.15
        _text(slide, 0.55, y, 0.42, 0.24, num, 15, RED if i == 0 else NAVY, FONT_DISPLAY, bold=True, name="content_study_num")
        _text(slide, 1.02, y, 1.35, 0.22, place, 9.4, INK, FONT_BODY, bold=True, name="content_study_place")
        _text(slide, 1.02, y + 0.25, 1.4, 0.2, sample, 7.2, MUTED, FONT_BODY, name="content_study_sample")
        _text(slide, 2.5, y - 0.02, 2.25, 0.55, finding, 9.2, NAVY, FONT_DISPLAY, bold=True, line_spacing=1.1, name="content_study_finding")
        _text(slide, 4.78, y, 0.33, 0.2, ref, 7.2, BLUE, FONT_BODY, bold=True, name="content_study_ref")
        if i < 2:
            _line(slide, 0.55, y + 0.95, 5.12, y + 0.95, RULE, 0.55, name="content_study_rule")

    _rect(slide, 5.6, 1.75, 3.82, 3.72, NAVY_LIGHT, None, name="content_gap_panel")
    _text(slide, 5.84, 1.96, 3.2, 0.2, "THE GAP", 7.5, RED, FONT_BODY, bold=True, name="content_label")
    _text(
        slide,
        5.84,
        2.2,
        3.25,
        1.18,
        "The studies quantify the wait. None tests a live booking, token, priority, and forecast workflow in one system.",
        12.4,
        NAVY,
        FONT_DISPLAY,
        bold=True,
        line_spacing=1.08,
        name="content_gap",
    )
    _line(slide, 5.84, 3.59, 9.12, 3.59, RGBColor(0xC7, 0xD2, 0xE2), 0.8, name="content_rule")
    _text(
        slide,
        5.84,
        3.75,
        3.18,
        0.86,
        "This project builds that workflow and evaluates software behavior. It does not claim clinical outcomes.",
        9,
        INK,
        FONT_BODY,
        line_spacing=1.2,
        name="content_gap_body",
    )
    _text(
        slide,
        5.84,
        4.86,
        3.05,
        0.34,
        "System contribution, not a medical trial.",
        8,
        RED,
        FONT_BODY,
        bold=True,
        name="content_gap_tag",
    )
    _citation(
        slide,
        "[1] Datta 2024. [2] Mohebbifar et al. Glob J Health Sci. 2013;6(1):172–180. [3] Oche & Adamu. Ann Med Health Sci Res. 2013;3(4):588–592.",
        5.64,
    )


def build_architecture(slide):
    _section_title(slide, "06", "ARCHITECTURE", "A modular backend keeps clinic flow fast while ML stays replaceable.")
    _label(slide, 0.55, 1.57, 2.4, "CLIENT", BLUE, 7)
    _rect(slide, 0.55, 1.78, 2.55, 2.45, WHITE, RULE, 0.8, name="content_client")
    _text(slide, 0.72, 1.94, 2.2, 0.24, "React + TypeScript", 9.2, NAVY, FONT_BODY, bold=True, name="content_client_stack")
    _text(slide, 0.72, 2.2, 2.1, 0.42, "One interface,\nthree role-based views", 7.5, MUTED, FONT_BODY, line_spacing=1.15, name="content_client_body")
    for i, role in enumerate(["PATIENT", "DOCTOR", "ADMIN"]):
        _rect(slide, 0.72, 2.82 + i * 0.4, 1.9, 0.28, NAVY_LIGHT, None, name="content_role_chip")
        _text(slide, 0.82, 2.88 + i * 0.4, 1.7, 0.16, role, 6.6, NAVY, FONT_BODY, bold=True, name="content_role_chip_text")

    _label(slide, 3.6, 1.57, 3.2, "CORE SERVICE", RED, 7)
    _rect(slide, 3.6, 1.78, 2.95, 3.15, NAVY, None, name="content_api")
    _text(slide, 3.8, 1.95, 2.5, 0.25, "Spring Boot API", 12, WHITE, FONT_BODY, bold=True, name="content_api_title")
    _text(slide, 3.8, 2.23, 2.45, 0.45, "Java 21  •  JWT  •  role checks", 7.2, RGBColor(0xCC, 0xD7, 0xE9), FONT_BODY, name="content_api_stack")
    for i, line in enumerate(["Appointments", "Queue state", "Audit log"]):
        _text(slide, 3.8, 2.85 + i * 0.42, 1.8, 0.2, line, 8.6, WHITE, FONT_BODY, name="content_api_item")
        _line(slide, 3.8, 3.12 + i * 0.42, 6.18, 3.12 + i * 0.42, RGBColor(0x4C, 0x62, 0x8D), 0.55, name="content_api_rule")
    _text(slide, 3.8, 4.42, 2.25, 0.22, "Flyway  •  MySQL", 7.2, RGBColor(0xCC, 0xD7, 0xE9), FONT_BODY, name="content_api_footer")

    _label(slide, 7.15, 1.57, 2.4, "INTELLIGENCE", ORANGE, 7)
    _rect(slide, 7.15, 1.78, 2.3, 2.45, RED_LIGHT, None, name="content_ml")
    _text(slide, 7.32, 1.95, 1.9, 0.25, "FastAPI sidecar", 9.2, NAVY, FONT_BODY, bold=True, name="content_ml_title")
    _text(slide, 7.32, 2.25, 1.9, 0.55, "Random Forest\nwait-time model", 7.5, MUTED, FONT_BODY, line_spacing=1.15, name="content_ml_body")
    _text(slide, 7.32, 2.98, 1.9, 0.55, "1,200 synthetic rows\nseed 41", 6.8, RED, FONT_BODY, bold=True, line_spacing=1.15, name="content_ml_provenance")
    _text(slide, 7.32, 3.68, 1.9, 0.2, "ML  or  FALLBACK", 6.8, NAVY, FONT_BODY, bold=True, name="content_ml_tag")

    _arrow(slide, 3.14, 2.43, 0.28, 0.14, RED, "content_arrow")
    _text(slide, 3.12, 2.62, 0.45, 0.15, "REST", 6, MUTED, FONT_BODY, bold=True, align=PP_ALIGN.CENTER, name="content_arrow_label")
    _arrow(slide, 6.58, 2.43, 0.28, 0.14, RED, "content_arrow")
    _text(slide, 6.55, 2.62, 0.38, 0.15, "REST", 6, MUTED, FONT_BODY, bold=True, align=PP_ALIGN.CENTER, name="content_arrow_label")

    _rect(slide, 0.55, 4.93, 8.9, 0.48, PALE, None, name="content_runtime")
    _text(slide, 0.72, 5.05, 8.55, 0.2, "DOCKER COMPOSE  /  FRONTEND  +  BACKEND  +  MYSQL  +  ML SIDECAR  /  LOCAL OR DEPLOYED", 6.2, NAVY, FONT_BODY, bold=True, name="content_runtime_text")

    _text(slide, 3.6, 5.6, 5.85, 0.25, "If the sidecar is unavailable, the API returns a deterministic fallback and labels the source.", 7.6, MUTED, FONT_BODY, italic=True, name="content_fallback_note")


def build_stack(slide):
    _section_title(slide, "07", "TECHNOLOGY", "Each layer uses focused, tested tools.")
    _text(slide, 0.55, 1.58, 8.8, 0.24, "The stack is deliberately ordinary: fewer moving parts, clear ownership, one local runtime.", 10.2, MUTED, FONT_BODY, name="content_intro")
    layers = [
        ("EXPERIENCE", ["React 19", "TypeScript", "Vite", "Tailwind CSS", "TanStack Query"], BLUE),
        ("SERVICE", ["Java 21", "Spring Boot 3.5", "Spring Security", "JWT", "Flyway"], NAVY),
        ("DATA", ["MySQL 8", "JPA / Hibernate"], RGBColor(0x4D, 0x65, 0x8A)),
        ("INTELLIGENCE", ["Python", "FastAPI", "scikit-learn"], ORANGE),
        ("QUALITY", ["JUnit 5", "pytest", "Vitest", "Docker Compose"], GREEN),
    ]
    for i, (label, tools, color) in enumerate(layers):
        y = 1.92 + i * 0.74
        _rect(slide, 0.55, y, 8.9, 0.61, WHITE, RULE, 0.65, name="content_stack_row")
        _rect(slide, 0.55, y, 1.52, 0.61, color, None, name="content_stack_tag")
        _text(slide, 0.7, y + 0.19, 1.25, 0.18, label, 6.5, WHITE, FONT_BODY, bold=True, name="content_stack_label")
        x = 2.3
        widths = []
        for tool in tools:
            width = max(0.62, 0.12 + len(tool) * 0.071)
            widths.append(width)
        total = sum(widths) + 0.1 * (len(tools) - 1)
        if total > 6.7:
            scale = 6.7 / total
            widths = [w * scale for w in widths]
        for j, tool in enumerate(tools):
            width = widths[j]
            _text(slide, x, y + 0.18, width, 0.22, tool, 8.2, INK, FONT_BODY, bold=(j == 0), name="content_tool")
            x += width + 0.1

    _line(slide, 0.55, 5.62, 9.45, 5.62, RULE, 0.7, name="content_rule")
    _text(slide, 0.55, 5.76, 8.8, 0.22, "Local runtime: Docker Compose. API contract: REST + OpenAPI. Access control: role-based JWT.", 7.6, MUTED, FONT_BODY, italic=True, name="content_runtime_note")


def build_implementation(slide):
    _section_title(slide, "08", "IMPLEMENTATION", "The prototype implements the patient, doctor, and admin journeys end to end.")
    shots = _screenshots()
    cards = [
        ("PATIENT", "Token, position, estimate", shots["patient"], 0.55),
        ("DOCTOR", "Call next, triage, serve", shots["doctor"], 3.5),
        ("ADMIN", "Users, schedules, metrics", shots["admin"], 6.45),
    ]
    for role, description, path, x in cards:
        _rect(slide, x, 1.55, 2.95, 1.98, WHITE, RULE, 0.8, name="content_shot_frame")
        _image_contain(slide, path, x + 0.08, 1.62, 2.79, 1.84, f"content_shot_{role.lower()}")
        _text(slide, x, 3.68, 2.95, 0.2, role, 8, RED, FONT_BODY, bold=True, name="content_shot_label")
        _text(slide, x, 3.92, 2.95, 0.3, description, 8.8, NAVY, FONT_BODY, bold=True, name="content_shot_desc")

    _line(slide, 0.55, 4.55, 9.45, 4.55, RULE, 0.8, name="content_rule")
    _text(slide, 0.55, 4.73, 1.7, 0.2, "ONE SOURCE OF TRUTH", 7, RED, FONT_BODY, bold=True, name="content_label")
    _text(
        slide,
        0.55,
        4.95,
        8.55,
        0.52,
        "Appointments, queue entries, roles, and audit events live in one backend, so every screen reads the same clinic state.",
        10.4,
        INK,
        FONT_DISPLAY,
        line_spacing=1.1,
        name="content_insight",
    )
    _text(
        slide,
        0.55,
        5.58,
        8.9,
        0.2,
        "Demo data is local. No real patient records are used in this prototype.",
        7.4,
        MUTED,
        FONT_BODY,
        italic=True,
        name="content_disclaimer",
    )


def build_results(
    slide,
    backend_tests: str = "247",
    python_tests: str = "19",
    frontend_tests: str = "41",
):
    _section_title(slide, "09", "EVIDENCE", "The implemented system covers the critical queue invariants.")
    _label(slide, 0.55, 1.56, 2.4, "BACKEND VERIFICATION", RED, 7)
    _text(slide, 0.55, 1.8, 2.3, 1.15, backend_tests, 72, NAVY, FONT_DISPLAY, bold=True, name="content_test_count")
    _text(slide, 0.58, 2.83, 2.45, 0.48, "JUnit 5 tests\n0 failures, 0 errors", 9, INK, FONT_BODY, bold=True, line_spacing=1.2, name="content_test_caption")
    _text(slide, 0.58, 3.5, 2.5, 0.48, f"Python: {python_tests} passed\nFrontend: {frontend_tests} passed", 7.2, MUTED, FONT_BODY, line_spacing=1.15, name="content_test_note")

    checks = [
        ("UNIQUE TOKEN", "Concurrent check-in cannot create the same token twice.", RED),
        ("PRIORITY ORDER", "Critical and high tiers precede routine tiers; FIFO inside each.", ORANGE),
        ("ROLE ISOLATION", "Patient, doctor, and admin routes enforce JWT roles.", NAVY),
        ("FORECAST FALLBACK", "Every estimate is labelled ML or FALLBACK.", BLUE),
    ]
    for i, (head, body, color) in enumerate(checks):
        y = 1.58 + i * 0.97
        _circle(slide, 3.65, y + 0.04, 0.3, WHITE, color, 1.2, name="content_check_dot")
        _text(slide, 3.7, y + 0.07, 0.2, 0.14, "✓", 8, color, FONT_BODY, bold=True, align=PP_ALIGN.CENTER, name="content_check")
        _text(slide, 4.1, y, 1.55, 0.22, head, 7, color, FONT_BODY, bold=True, name="content_check_head")
        _text(slide, 5.45, y - 0.01, 3.95, 0.58, body, 8.8, INK, FONT_BODY, line_spacing=1.18, name="content_check_body")

    _rect(slide, 0.55, 5.22, 8.9, 0.45, NAVY_LIGHT, None, name="content_result_strip")
    _text(slide, 0.75, 5.34, 8.45, 0.2, "Production build passes  •  0 failed tests  •  OpenAPI contract documented  •  Demo data only", 7.2, NAVY, FONT_BODY, bold=True, name="content_result_strip_text")


def build_conclusion(slide):
    _section_title(slide, "10", "CONCLUSION", "The system closes the loop between a booked appointment and a visible consultation queue.")
    _label(slide, 0.55, 1.58, 3.2, "WHAT SHIPPED", RED, 7)
    rows = [
        ("01", "Patients", "Book a slot, check in, receive a token, and see their position and estimate."),
        ("02", "Doctors", "Run the live queue, assign priority, serve patients, and record overrides."),
        ("03", "Administrators", "Manage users, doctors, schedules, and the operational view."),
    ]
    for i, (num, head, body) in enumerate(rows):
        y = 1.9 + i * 1.02
        _text(slide, 0.55, y, 0.38, 0.25, num, 12, RED if i == 0 else NAVY, FONT_DISPLAY, bold=True, name="content_outcome_num")
        _text(slide, 1.02, y - 0.01, 1.35, 0.22, head, 9.2, NAVY, FONT_BODY, bold=True, name="content_outcome_head")
        _text(slide, 2.3, y - 0.02, 3.55, 0.65, body, 8.8, INK, FONT_BODY, line_spacing=1.18, name="content_outcome_body")
    _text(
        slide,
        0.55,
        5.1,
        5.5,
        0.45,
        "Real-world time savings and clinical outcomes remain outside this evaluation.",
        8,
        MUTED,
        FONT_BODY,
        italic=True,
        name="content_limitation",
    )

    _rect(slide, 6.35, 1.55, 3.1, 4.0, NAVY_LIGHT, None, name="content_next_panel")
    _text(slide, 6.58, 1.76, 2.5, 0.2, "NEXT HORIZON", 7, RED, FONT_BODY, bold=True, name="content_label")
    future = [
        ("01", "Notifications", "SMS and push updates"),
        ("02", "Reach", "Mobile and multi-hospital"),
        ("03", "Evidence", "Real data and richer models"),
    ]
    for i, (num, head, body) in enumerate(future):
        y = 2.12 + i * 1.05
        _text(slide, 6.58, y, 0.35, 0.23, num, 10, NAVY, FONT_BODY, bold=True, name="content_future_num")
        _text(slide, 7.02, y - 0.02, 2.1, 0.22, head, 9, NAVY, FONT_BODY, bold=True, name="content_future_head")
        _text(slide, 7.02, y + 0.2, 2.1, 0.24, body, 7.4, MUTED, FONT_BODY, name="content_future_body")

    _text(slide, 6.58, 5.0, 2.5, 0.26, "From a working flow to a validated service.", 8, RED, FONT_BODY, bold=True, name="content_future_tag")


def build_references(slide):
    _section_title(slide, "11", "REFERENCES", "References")
    refs_left = [
        ("1", "Datta M. Assessment of outdoor patients waiting time and consultation time in a tertiary care hospital of Kolkata. Indian J Community Med. 2024;49(Suppl 1):S61. doi:10.4103/ijcm.ijcm_abstract210"),
        ("2", "Mohebbifar R, Hasanpoor E, Mohseni M, et al. Outpatient waiting time in health services and teaching hospitals: a case study in Iran. Glob J Health Sci. 2013;6(1):172–180. doi:10.5539/gjhs.v6n1p172"),
        ("3", "Oche MO, Adamu H. Determinants of patient waiting time in the general outpatient department of a tertiary health institution in north-western Nigeria. Ann Med Health Sci Res. 2013;3(4):588–592. doi:10.4103/2141-9248.122123"),
        ("4", "Spring Boot 3.5.16 Reference Documentation. Broadcom. https://docs.spring.io/spring-boot/3.5.16/reference/index.html"),
    ]
    refs_right = [
        ("5", "FastAPI Documentation. https://fastapi.tiangolo.com/"),
        ("6", "scikit-learn User Guide. https://scikit-learn.org/stable/user_guide.html"),
        ("7", "MySQL 8.4 Reference Manual. Oracle. https://dev.mysql.com/doc/refman/8.4/en/"),
        ("8", "Nottingham M, Wilde E, Dalal S. RFC 9457: Problem Details for HTTP APIs. IETF, July 2023. https://www.rfc-editor.org/rfc/rfc9457.html"),
    ]
    for col, refs in enumerate([refs_left, refs_right]):
        x = 0.55 if col == 0 else 5.08
        for i, (num, text_value) in enumerate(refs):
            y = 1.56 + i * 1.08
            _text(slide, x, y, 0.26, 0.2, num, 10, RED, FONT_BODY, bold=True, name="content_ref_num")
            _text(slide, x + 0.32, y - 0.01, 3.72, 0.96, text_value, 7.7, INK, FONT_BODY, line_spacing=1.17, name="content_ref")
    _text(slide, 0.55, 5.83, 8.9, 0.2, "All URLs accessed 19 November 2026. The ML model uses synthetic demonstration data, not patient records.", 6.8, MUTED, FONT_BODY, italic=True, name="content_ref_note")


def build_thanks(slide):
    _label(slide, 0.7, 0.62, 8.6, "CLOSING", RED, 8, PP_ALIGN.CENTER)
    _text(slide, 0.8, 2.1, 8.4, 0.9, "Thank you.", 48, NAVY, FONT_DISPLAY, bold=True, align=PP_ALIGN.CENTER, name="content_title")
    _rect(slide, 4.55, 3.16, 0.9, 0.05, RED, None, name="content_rule")
    _text(
        slide,
        1.2,
        3.45,
        7.6,
        0.5,
        "Smart Hospital: from booking to consultation.",
        14,
        MUTED,
        FONT_BODY,
        italic=True,
        align=PP_ALIGN.CENTER,
        name="content_subtitle",
    )
    _text(
        slide,
        1.2,
        4.55,
        7.6,
        0.4,
        "Team SY2207  •  Department of Computer Science & Engineering",
        8.2,
        INK,
        FONT_BODY,
        align=PP_ALIGN.CENTER,
        name="content_team",
    )


def build_questions(slide):
    _label(slide, 0.7, 0.62, 8.6, "Q&A", RED, 8, PP_ALIGN.CENTER)
    _text(slide, 0.8, 2.02, 8.4, 0.9, "Questions?", 48, RED, FONT_DISPLAY, bold=True, align=PP_ALIGN.CENTER, name="content_title")
    _text(
        slide,
        1.3,
        3.06,
        7.4,
        0.45,
        "Ask about queue rules, the ML fallback, or the implementation.",
        13,
        NAVY,
        FONT_BODY,
        align=PP_ALIGN.CENTER,
        name="content_subtitle",
    )
    prompts = [("QUEUE", "priority and FIFO"), ("ML", "limits and fallback"), ("NEXT", "pilot and evidence")]
    for i, (head, body) in enumerate(prompts):
        x = 1.35 + i * 2.65
        _line(slide, x, 4.2, x + 1.9, 4.2, RULE, 0.8, name="content_rule")
        _text(slide, x, 4.36, 1.85, 0.2, head, 7, RED, FONT_BODY, bold=True, name="content_prompt_head")
        _text(slide, x, 4.6, 1.9, 0.24, body, 8.2, MUTED, FONT_BODY, name="content_prompt_body")


SLIDE_BUILDERS = [
    build_cover,
    build_roadmap,
    build_problem,
    build_objective,
    build_journey,
    build_queue,
    build_literature,
    build_architecture,
    build_stack,
    build_implementation,
    build_results,
    build_conclusion,
    build_references,
    build_thanks,
    build_questions,
]


def _validate_screenshots() -> None:
    for name, path in _screenshots().items():
        if not path.exists():
            raise FileNotFoundError(f"Missing {name} screenshot: {path}")
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


def build_deck(
    template: Path,
    output: Path,
    backend_tests: str = "247",
    python_tests: str = "19",
    frontend_tests: str = "41",
) -> Presentation:
    if not template.exists():
        raise FileNotFoundError(f"Template not found: {template}")
    _validate_screenshots()
    prs = Presentation(str(template))
    prs.core_properties.title = "Smart Hospital: Patient Queue & Appointment Management System"
    prs.core_properties.subject = "Project Presentation, 2026–27"
    prs.core_properties.author = "Team SY2207"
    if len(prs.slides) != len(SLIDE_BUILDERS):
        raise ValueError(f"Template has {len(prs.slides)} slides, expected {len(SLIDE_BUILDERS)}")

    footer_before = _keep_footer_baseline(prs)
    _validate_footer_signature(footer_before)
    for slide, builder in zip(prs.slides, SLIDE_BUILDERS):
        strip_content(slide)
        if builder is build_results:
            builder(
                slide,
                backend_tests=backend_tests,
                python_tests=python_tests,
                frontend_tests=frontend_tests,
            )
        else:
            builder(slide)

    footer_after = _keep_footer_baseline(prs)
    _validate_footer_signature(footer_after)
    if footer_before != footer_after:
        raise RuntimeError("Footer signature changed; refusing to save the deck")

    for i, slide in enumerate(prs.slides, 1):
        has_page_number = any(
            getattr(shape, "has_text_frame", False) and shape.text.strip() == "‹#›"
            for shape in slide.shapes
        )
        if has_page_number is not (i != 1):
            raise RuntimeError(f"Unexpected page-number placeholder state on slide {i}")

    output.parent.mkdir(parents=True, exist_ok=True)
    prs.save(str(output))
    return prs


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--template", type=Path, default=DEFAULT_TEMPLATE)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--backend-tests", type=_count, default="247")
    parser.add_argument("--python-tests", type=_count, default="19")
    parser.add_argument("--frontend-tests", type=_count, default="41")
    args = parser.parse_args()
    prs = build_deck(
        args.template,
        args.output,
        args.backend_tests,
        args.python_tests,
        args.frontend_tests,
    )
    print(f"Wrote {args.output} with {len(prs.slides)} slides")


if __name__ == "__main__":
    main()

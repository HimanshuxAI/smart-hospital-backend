import hashlib
from pathlib import Path

from pptx import Presentation
from pptx.enum.dml import MSO_THEME_COLOR
from pptx.util import Inches

from build_redesigned_ppt import (
    DEFAULT_TEMPLATE,
    _footer_signature,
    _keep_footer_baseline,
    build_deck,
)

FOOTER_TOP = Inches(6.15)
LEGACY_WORDS = (
    "UCAV",
    "loitering",
    "munition",
    "TRL 1",
    "Mini Shopee",
    "e-commerce",
    "rainfall",
    "intrusion detection",
)


def _build(tmp_path: Path) -> tuple[Presentation, Presentation]:
    template = Presentation(str(DEFAULT_TEMPLATE))
    output_path = tmp_path / "deck.pptx"
    output = build_deck(DEFAULT_TEMPLATE, output_path)
    return template, output


def _all_text(prs: Presentation) -> str:
    lines = []
    for slide in prs.slides:
        for shape in slide.shapes:
            if getattr(shape, "has_text_frame", False):
                lines.append(shape.text)
    return "\n".join(lines)


def test_structure_footer_and_white_master(tmp_path):
    template, prs = _build(tmp_path)

    assert len(prs.slides) == 15
    assert prs.slide_width == Inches(10)
    assert prs.slide_height == Inches(7.5)

    master_fill = prs.slide_master.background.fill
    assert master_fill.type == 1
    assert master_fill.fore_color.theme_color == MSO_THEME_COLOR.LIGHT_1

    expected = _keep_footer_baseline(template)
    actual = [_footer_signature(slide) for slide in prs.slides]
    assert actual == expected

    for index, slide in enumerate(prs.slides, start=1):
        has_page_number = any(
            getattr(shape, "has_text_frame", False) and shape.text.strip() == "‹#›"
            for shape in slide.shapes
        )
        assert has_page_number is (index != 1)
        assert actual[index - 1][0]
        assert actual[index - 1][1]


def test_content_stays_above_the_footer(tmp_path):
    _, prs = _build(tmp_path)

    for slide_number, slide in enumerate(prs.slides, start=1):
        for shape in slide.shapes:
            if not shape.name.startswith("content_"):
                continue
            assert shape.top is not None and shape.height is not None
            bottom = shape.top + shape.height
            assert bottom <= FOOTER_TOP, (
                f"content on slide {slide_number} crosses the footer: "
                f"{shape.name} ends at {bottom / 914400:.2f} in"
            )


def test_no_legacy_or_unrelated_content(tmp_path):
    _, prs = _build(tmp_path)
    text = _all_text(prs)
    for word in LEGACY_WORDS:
        assert word.lower() not in text.lower()


def test_required_story_and_evidence_are_present(tmp_path):
    _, prs = _build(tmp_path)
    text = _all_text(prs)

    required = [
        "SMART HOSPITAL",
        "The presentation follows one patient",
        "98.6",
        "6.5",
        "One flow.",
        "Every patient follows the same six-step digital journey.",
        "Urgency determines priority",
        "The evidence measures waiting",
        "A modular backend",
        "Each layer uses focused, tested tools.",
        "The prototype implements",
        "247",
        "Python: 19 passed",
        "Frontend: 41 passed",
        "The system closes the loop",
        "Thank you.",
        "Questions?",
        "doi:10.4103/ijcm.ijcm_abstract210",
        "doi:10.5539/gjhs.v6n1p172",
        "doi:10.4103/2141-9248.122123",
        "https://docs.spring.io/spring-boot/3.5.16/reference/index.html",
        "https://www.rfc-editor.org/rfc/rfc9457.html",
        "1,200 synthetic rows",
        "seed 41",
    ]
    for phrase in required:
        assert phrase in text, phrase

    screenshot_shapes = [
        shape
        for shape in prs.slides[9].shapes
        if shape.shape_type == 13 and shape.name.startswith("content_shot_")
    ]
    image_hashes = {
        hashlib.sha256(shape.image.blob).hexdigest() for shape in screenshot_shapes
    }
    assert len(screenshot_shapes) == 3
    assert len(image_hashes) == 3

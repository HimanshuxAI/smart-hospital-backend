import hashlib
from pathlib import Path

from pptx import Presentation
from pptx.enum.dml import MSO_THEME_COLOR
from pptx.util import Inches

from build_creative_ppt import (
    TEMPLATE,
    TYPE_SCALE,
    _footer_baseline,
    _normalize_footer,
    build,
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


def _deck(tmp_path: Path) -> Presentation:
    return build(TEMPLATE, tmp_path / "deck.pptx")


def _text(prs: Presentation) -> str:
    return "\n".join(
        shape.text
        for slide in prs.slides
        for shape in slide.shapes
        if getattr(shape, "has_text_frame", False)
    )


def test_structure_and_footer(tmp_path):
    template = Presentation(str(TEMPLATE))
    _normalize_footer(template)
    prs = _deck(tmp_path)

    assert len(prs.slides) == 15
    assert prs.slide_width == Inches(10)
    assert prs.slide_height == Inches(7.5)

    fill = prs.slide_master.background.fill
    assert fill.type == 1
    assert fill.fore_color.theme_color == MSO_THEME_COLOR.LIGHT_1
    assert all(slide.background.fill.type == 5 for slide in prs.slides)

    assert _footer_baseline(prs) == _footer_baseline(template)
    assert all("MITSOC" in sig[0] for sig in _footer_baseline(prs))
    assert all("MITSoE" not in sig[0] and "MITSoC" not in sig[0] for sig in _footer_baseline(prs))

    for i, slide in enumerate(prs.slides, 1):
        page = any(
            getattr(shape, "has_text_frame", False) and shape.text.strip() == "‹#›"
            for shape in slide.shapes
        )
        assert page is (i != 1)


def test_content_stays_above_footer(tmp_path):
    prs = _deck(tmp_path)
    for i, slide in enumerate(prs.slides, 1):
        for shape in slide.shapes:
            if not shape.name.startswith("content_"):
                continue
            assert shape.top is not None and shape.height is not None
            bottom = shape.top + shape.height
            assert bottom <= FOOTER_TOP, f"slide {i}: {shape.name} ends at {bottom / 914400:.2f} in"


def test_no_legacy_content(tmp_path):
    text = _text(_deck(tmp_path)).lower()
    for word in LEGACY_WORDS:
        assert word not in text


def test_story_and_citations(tmp_path):
    prs = _deck(tmp_path)
    text = _text(prs)
    required = [
        "SMART",
        "HOSPITAL",
        "One queue. Three points of view.",
        "Six steps. One shared record.",
        "Urgency first. Arrival time breaks the tie.",
        "Studies measure the wait.",
        "Fast path first. Intelligence stays optional.",
        "The rules survive the test suite.",
        "Thank you.",
        "Q&A.",
        "doi:10.4103/ijcm.ijcm_abstract210",
        "doi:10.5539/gjhs.v6n1p172",
        "doi:10.4103/2141-9248.122123",
        "https://docs.spring.io/spring-boot/3.5.16/reference/index.html",
        "https://www.rfc-editor.org/rfc/rfc9457.html",
        "1,200 SYNTHETIC ROWS",
        "synthetic demonstration data",
        "247",
        "19",
        "41",
    ]
    for phrase in required:
        assert phrase in text, phrase

    hashes = {
        hashlib.sha256(shape.image.blob).hexdigest()
        for shape in prs.slides[3].shapes
        if shape.shape_type == 13 and shape.name.startswith("content_shot_")
    }
    assert len(hashes) == 3


def test_evidence_intro_clears_headline(tmp_path):
    prs = _deck(tmp_path)
    slide = prs.slides[6]
    shapes = {shape.name: shape for shape in slide.shapes}
    headline = shapes["content_headline"]
    intro = shapes["content_intro"]
    assert intro.top >= Inches(1.8)
    assert headline.top + headline.height <= Inches(1.9)


def test_font_sizes_use_one_scale(tmp_path):
    prs = _deck(tmp_path)
    used = set()
    for slide in prs.slides:
        for shape in slide.shapes:
            if not getattr(shape, "has_text_frame", False):
                continue
            for paragraph in shape.text_frame.paragraphs:
                for run in paragraph.runs:
                    if run.text.strip() and run.font.size is not None:
                        used.add(run.font.size.pt)
    assert used <= set(TYPE_SCALE), f"unexpected sizes: {sorted(used - set(TYPE_SCALE))}"

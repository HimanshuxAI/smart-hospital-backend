from pathlib import Path

from docx import Document
from docx.enum.text import (WD_ALIGN_PARAGRAPH, WD_TAB_ALIGNMENT,
                            WD_TAB_LEADER)
from docx.oxml.ns import qn

from build_report import DEFAULT_PAGES, FIGURES, TABLES, build

DOCX = Path("Smart_Hospital_Project_Report_Formatted.docx")


def _document(tmp_path: Path):
    output = tmp_path / "report.docx"
    build(DEFAULT_PAGES, output)
    return Document(str(output))


def test_annexure_page_setup(tmp_path):
    doc = _document(tmp_path)
    assert len(doc.sections) == 3
    for section in doc.sections:
        # A4, as used by the Annexure 6 template.
        assert round(section.page_width.inches, 2) == 8.27
        assert round(section.page_height.inches, 2) == 11.69
        assert round(section.top_margin.inches, 2) == 1
        assert round(section.bottom_margin.inches, 2) == 1
        assert round(section.left_margin.inches, 2) == 1.25
        assert round(section.right_margin.inches, 2) == 1


def test_footer_matches_template(tmp_path):
    doc = _document(tmp_path)
    # Cover: no footer. Front matter: page number only.
    assert not " ".join(p.text for p in doc.sections[0].footer.paragraphs).strip()
    assert not " ".join(p.text for p in doc.sections[1].footer.paragraphs).strip()
    # Main body: the template footer text, from the Introduction onward.
    expected = "MITSOC, Department of Computer Science and Engineering, 2026-27"
    assert expected in " ".join(p.text for p in doc.sections[2].footer.paragraphs)
    # Cover seal + two letterhead logos + six figures.
    assert len(doc.inline_shapes) == 9


def test_page_numbering_and_typography(tmp_path):
    doc = _document(tmp_path)
    assert doc.sections[0]._sectPr.find(qn("w:pgNumType")) is None
    front = doc.sections[1]._sectPr.find(qn("w:pgNumType"))
    assert front.get(qn("w:fmt")) == "lowerRoman"
    assert front.get(qn("w:start")) == "1"
    main = doc.sections[2]._sectPr.find(qn("w:pgNumType"))
    assert main.get(qn("w:fmt")) == "decimal"
    assert main.get(qn("w:start")) == "1"

    chapters = [
        p for p in doc.paragraphs
        if p.text.strip() in {
            "1.  INTRODUCTION", "2.  CONCEPTS AND METHODS", "3.  LITERATURE SURVEY",
            "4.  PROJECT PLAN", "5.  SOFTWARE REQUIREMENT SPECIFICATION",
            "6.  IMPLEMENTATION AND RESULTS", "7.  SOFTWARE TESTING",
            "8.  CONCLUSION AND FUTURE WORK", "REFERENCES", "ANNEXURE A: MODEL METADATA",
        }
    ]
    assert len(chapters) == 10
    for chapter in chapters:
        assert chapter.alignment == WD_ALIGN_PARAGRAPH.CENTER
        run = next(r for r in chapter.runs if r.text.strip())
        assert run.font.size.pt == 18
        assert run.font.bold
        assert run.font.name == "Times New Roman"


def test_contents_uses_dot_leaders(tmp_path):
    doc = _document(tmp_path)
    contents = [
        p for p in doc.paragraphs
        if p.text.startswith("1.  INTRODUCTION") and len(p.paragraph_format.tab_stops)
    ]
    assert contents
    tab = contents[0].paragraph_format.tab_stops[0]
    assert tab.leader == WD_TAB_LEADER.DOTS
    assert tab.alignment == WD_TAB_ALIGNMENT.RIGHT


def test_lists_use_caption_page_numbers(tmp_path):
    doc = _document(tmp_path)
    lines = [p.text for p in doc.paragraphs if "\t" in p.text]
    for title, number in FIGURES:
        line = next(text for text in lines if text.startswith(title))
        assert line.split("\t")[1] == DEFAULT_PAGES[f"fig{number}"]
    for title, number in TABLES:
        line = next(text for text in lines if text.startswith(title))
        assert line.split("\t")[1] == DEFAULT_PAGES[f"tbl{number}"]


def test_report_evidence_and_figures(tmp_path):
    doc = _document(tmp_path)
    text = "\n".join(p.text for p in doc.paragraphs)
    text += "\n" + "\n".join(
        cell.text for table in doc.tables for row in table.rows for cell in row.cells
    )
    for phrase in [
        "98.6", "161", "168.4", "RandomForestRegressor", "synthetic-demo-data",
        "seed 41", "247", "19", "41", "FALLBACK", "clinical outcomes",
        "10.4103/ijcm.ijcm_abstract210", "10.5539/gjhs.v6n1p172",
        "10.4103/2141-9248.122123", "rfc9457",
    ]:
        assert phrase in text, phrase
    assert len(doc.inline_shapes) == 9

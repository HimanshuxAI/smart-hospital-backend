#!/usr/bin/env python3
"""Read the rendered PDF and record the printed page number of every heading."""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

import pymupdf

from build_report import FIGURES, TABLES

PDF = Path("Smart_Hospital_Project_Report_Formatted.pdf")
OUT = Path(".report_pages.json")

HEADINGS = {
    "certificate": "This is to certify that the project report entitled",
    "company_certificate": "CERTIFICATE (FROM COMPANY IF ANY)",
    "declaration": "DECLARATION",
    "examiner": "APPROVAL CERTIFICATE",
    "acknowledgement": "ACKNOWLEDGEMENT",
    "abstract": "ABSTRACT",
    "figures": "LIST OF FIGURES",
    "tables": "LIST OF TABLES",
    "ch1": "1. INTRODUCTION",
    "ch2": "2. CONCEPTS AND METHODS",
    "ch3": "3. LITERATURE SURVEY",
    "ch4": "4. PROJECT PLAN",
    "ch5": "5. SOFTWARE REQUIREMENT SPECIFICATION",
    "ch6": "6. IMPLEMENTATION AND RESULTS",
    "ch7": "7. SOFTWARE TESTING",
    "ch8": "8. CONCLUSION AND FUTURE WORK",
    "refs": "REFERENCES",
    "annexure": "ANNEXURE A: MODEL METADATA",
}
SECTIONS = {
    "1.1": "1.1 Overview", "1.2": "1.2 Motivation",
    "1.3": "1.3 Problem Statement", "1.4": "1.4 Objectives",
    "1.5": "1.5 Scope",
    "2.1": "2.1 Definitions", "2.2": "2.2 System Architecture",
    "2.3": "2.3 Queue Ordering", "2.4": "2.4 Wait-Time Estimator",
    "2.5": "2.5 Method Summary",
    "3.1": "3.1 Waiting-Time Evidence", "3.2": "3.2 Gap",
    "3.3": "3.3 Standards and Technologies",
    "4.1": "4.1 Development Model", "4.2": "4.2 Task Schedule",
    "4.3": "4.3 Risk Management",
    "5.1": "5.1 Functional Requirements",
    "5.2": "5.2 User Classes and Characteristics",
    "5.3": "5.3 Non-Functional Requirements",
    "5.4": "5.4 Assumptions and Constraints",
    "6.1": "6.1 Technology Stack", "6.2": "6.2 Implemented Role Views",
    "6.3": "6.3 Verification Results",
    "7.1": "7.1 Test Strategy", "7.2": "7.2 Test Inventory",
    "7.3": "7.3 Invariants and Limitations",
    "8.1": "8.1 Conclusions", "8.2": "8.2 Future Work",
}


def _norm(text: str) -> str:
    return re.sub(r"\s+", " ", text).strip()


def _printed(page) -> str:
    lines = [line.strip() for line in page.get_text().splitlines() if line.strip()]
    return lines[-1] if lines else ""


def main() -> int:
    doc = pymupdf.open(PDF)
    texts = [_norm(page.get_text()) for page in doc]
    printed = [_printed(page) for page in doc]

    body = next(i for i, text in enumerate(texts)
                if printed[i] == "1" and "1. INTRODUCTION" in text)
    pages: dict[str, str] = {}

    missing = []
    for key, needle in HEADINGS.items():
        start = 0 if key in {"certificate", "company_certificate",
                             "declaration", "examiner", "acknowledgement",
                             "abstract", "figures", "tables"} else body
        needle = _norm(needle)
        for i in range(start, len(texts)):
            if needle in texts[i]:
                pages[key] = printed[i]
                break
        else:
            missing.append(key)

    for title, number in FIGURES:
        needle = _norm(title)
        for i in range(body, len(texts)):
            if needle in texts[i]:
                pages[f"fig{number}"] = printed[i]
                break
        else:
            missing.append(f"fig{number}")

    for title, number in TABLES:
        needle = _norm(title)
        for i in range(body, len(texts)):
            if needle in texts[i]:
                pages[f"tbl{number}"] = printed[i]
                break
        else:
            missing.append(f"tbl{number}")

    for key, needle in SECTIONS.items():
        needle = _norm(needle)
        for i in range(body, len(texts)):
            if needle in texts[i]:
                pages[key] = printed[i]
                break
        else:
            missing.append(key)

    if missing:
        print("missing headings:", ", ".join(missing), file=sys.stderr)
        return 1

    OUT.write_text(json.dumps(pages, indent=2, ensure_ascii=False) + "\n")
    print(json.dumps(pages, indent=2, ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

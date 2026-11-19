# -*- coding: utf-8 -*-
"""Fill Smart Hospital content into the attached format template,
preserving footer, logo, slide size, positions, and fonts."""
from pptx import Presentation
from pptx.util import Inches, Pt
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN

TEMPLATE = "/Users/himanshu/Downloads/Final_Project_Presentation_format.pptx"
OUT = "/Users/himanshu/Documents/hospital/Smart_Hospital_Filled_Presentation.pptx"

prs = Presentation(TEMPLATE)


from pptx.enum.dml import MSO_COLOR_TYPE


def capture_font(tf):
    """Return font props from the first run of the first non-empty paragraph."""
    for p in tf.paragraphs:
        if p.runs:
            r = p.runs[0]
            f = r.font
            color = f.color
            ctype = None
            crgb = None
            if color is not None and color.type is not None:
                ctype = color.type
                if ctype == MSO_COLOR_TYPE.RGB:
                    crgb = color.rgb
            return {
                "size": f.size,
                "bold": f.bold,
                "italic": f.italic,
                "ctype": ctype,
                "crgb": crgb,
                "name": f.name,
            }
            break
    return {}


def set_text(tf, new_text, align=None):
    """Replace text in a text frame, keeping the first paragraph's font style
    and cloning it to new paragraphs. Preserves paragraph alignment."""
    lines = new_text.split("\n")
    font = capture_font(tf)
    # ensure enough paragraphs
    while len(tf.paragraphs) < len(lines):
        tf.add_paragraph()
    # remove extra paragraphs beyond line count
    while len(tf.paragraphs) > len(lines):
        p = tf.paragraphs[-1]
        p._element.getparent().remove(p._element)
    for i, line in enumerate(lines):
        p = tf.paragraphs[i]
        runs = list(p.runs)
        if not runs:
            r = p.add_run()
        else:
            r = runs[0]
        r.text = line
        # blank out any extra runs in this paragraph
        for extra in runs[1:]:
            extra.text = ""
        f = r.font
        if font.get("size") is not None:
            f.size = font["size"]
        if font.get("bold") is not None:
            f.bold = font["bold"]
        if font.get("italic") is not None:
            f.italic = font["italic"]
        if font.get("ctype") is not None and font.get("ctype") == MSO_COLOR_TYPE.RGB and font.get("crgb") is not None:
            f.color.rgb = font["crgb"]
        if font.get("name") is not None:
            f.name = font["name"]
        if align is not None:
            p.alignment = align


def shape_by_id(slide, shape_id):
    for sh in slide.shapes:
        if sh.shape_id == shape_id:
            return sh
    return None


# ---------------------------------------------------------------------------
# Slide 1 — Title
# ---------------------------------------------------------------------------
s = prs.slides[0]
set_text(shape_by_id(s, 48).text_frame,
         "SMART HOSPITAL - PATIENT QUEUE & APPOINTMENT MANAGEMENT SYSTEM")
set_text(shape_by_id(s, 50).text_frame,
         "A Clinic Operating System - From Booking to Consultation")
set_text(shape_by_id(s, 49).text_frame,
         "Uday Mhaske (ADT24SOCB1735)\nAditya Sundalam (MITU22BTCS0044)\nAtharv Shewale (ADT25SOCB1715)\nSaiprasad Bendre (ADT25SOCB1651)\n\nGuided by\nDr. P. D. Patil\nDepartment of Computer Science & Engineering\nMIT School of Computing\nMIT Art, Design and Technology University, Loni-Kalbhor, Pune\n2026 - 27")

# ---------------------------------------------------------------------------
# Slide 2 — Outline
# ---------------------------------------------------------------------------
s = prs.slides[1]
set_text(shape_by_id(s, 60).text_frame,
         "1. Introduction\n2. Problem Statement\n3. Objectives\n4. Concepts & Methods\n5. Literature Survey\n6. Identification of Gaps & Scope of Work\n7. Process and Architecture\n8. Tools and Languages\n9. Implementation\n10. Results\n11. Conclusion and Future Work\n12. References")

# ---------------------------------------------------------------------------
# Slide 3 — Introduction
# ---------------------------------------------------------------------------
s = prs.slides[2]
set_text(shape_by_id(s, 69).text_frame,
         "Hospitals and clinics typically serve a large number of patients who need to consult doctors on a given day. "
         "When queue management is handled manually, the result is often long waiting times, overcrowded waiting areas, "
         "and confusion among patients and staff. Patients frequently do not know their exact position in the queue or "
         "how long they will have to wait before seeing the doctor. Emergency cases also need to be identified quickly "
         "and given priority so that critical patients receive treatment without delay.\n\nThere is a clear need for a "
         "smart system that manages appointments and queues efficiently while giving patients reliable information about "
         "their queue position and expected waiting time. The Smart Hospital Patient Queue and Appointment Management "
         "System addresses these problems by digitizing the entire patient journey, from registration and appointment "
         "booking to token generation, queue tracking, and waiting-time estimation.")

# ---------------------------------------------------------------------------
# Slide 4 — Problem Statement + Objectives
# ---------------------------------------------------------------------------
s = prs.slides[3]
set_text(shape_by_id(s, 79).text_frame,
         "Traditional hospital OPDs rely on paper tokens and manual tracking. Patients face long, uncertain waits with "
         "no visibility into their queue position or expected wait time. Waiting rooms become overcrowded, paper tokens "
         "get lost or skipped, and emergency patients are not reliably triaged - critical cases can wait behind routine "
         "ones. There is no live queue board and no data-driven waiting-time estimate.\n\nReported average OPD waiting "
         "time is around 98.6 minutes, while actual consultation time is only about 6.5 minutes.")
set_text(shape_by_id(s, 80).text_frame,
         "1. Online patient appointment booking with conflict-safe slots\n2. Digital token generation (one per doctor per day)\n"
         "3. Live queue position and triage visibility for patients\n4. Emergency priority handling (CRITICAL first)\n"
         "5. Waiting-time estimation using ML with a deterministic fallback\n6. Role-based dashboards for patient, doctor, and admin\n"
         "7. Reduce waiting time and overcrowding; provide a simple, user-friendly system")

# ---------------------------------------------------------------------------
# Slide 5 — Concepts and Methods (three boxes)
# ---------------------------------------------------------------------------
s = prs.slides[4]
set_text(shape_by_id(s, 90).text_frame,
         "Smart Hospital: A hospital that uses information technology and interconnected systems to manage patient flow, "
         "appointments, and clinical resources efficiently.\nPatient Queue: An ordered list of patients waiting to consult "
         "a doctor, arranged by check-in time and emergency priority tier.\nDigital Token: A unique per-doctor, per-day "
         "number issued at check-in, indicating the patient's turn in the queue.")
set_text(shape_by_id(s, 91).text_frame,
         "Emergency Triage: Classification into one of four priority tiers - NORMAL, MEDIUM, HIGH, or CRITICAL - so urgent "
         "cases are called before routine ones.\nWaiting-Time Prediction: Estimation (in minutes) of how long a patient "
         "will wait before consultation, based on queue position, historical consultation duration, emergency load, and "
         "time-of-day context.\nSpring Boot: Java-based framework for the REST API backend.")
set_text(shape_by_id(s, 92).text_frame,
         "MySQL: Relational database storing users, appointments, queue entries, and hospital data.\nFastAPI: Lightweight "
         "Python framework for the ML sidecar that predicts waiting time.\nscikit-learn: ML library for the waiting-time "
         "regression model.\nReact + TypeScript: Responsive frontend UI.\nJWT: Stateless authentication and role-based "
         "authorization.\nFlyway: Database-migration tool for reproducible schema changes.")

# ---------------------------------------------------------------------------
# Slide 6 — Literature Survey (table)
# ---------------------------------------------------------------------------
s = prs.slides[5]
tbl_sh = None
for sh in s.shapes:
    if sh.has_table:
        tbl_sh = sh
        break
tbl = tbl_sh.table
entries = [
    ("1.", "Patient waiting time and consultation time in a tertiary care hospital of Kolkata",
     "Datta M.", "Average OPD waiting ~98.6 min vs ~6.5 min consultation; long waits are the core problem."),
    ("2.", "Outpatient waiting time in health services and teaching hospitals: a case study in Iran",
     "Mohebbifar R. et al.", "Waiting time varies by department and time-of-day; supports time-based prediction features."),
    ("3.", "Determinants of patient waiting time in the general outpatient department of a tertiary health institution",
     "Oche M.O., Adamu H.", "Queue discipline and emergency prioritization materially reduce waits for urgent cases."),
]
for row_idx, (sr, title, author, finding) in enumerate(entries, start=1):
    cells = tbl.rows[row_idx].cells
    set_text(cells[0].text_frame, sr)
    set_text(cells[1].text_frame, title)
    set_text(cells[2].text_frame, author)
    set_text(cells[3].text_frame, finding)

# ---------------------------------------------------------------------------
# Slide 7 — Identification of gaps & scope of work
# ---------------------------------------------------------------------------
s = prs.slides[6]
set_text(shape_by_id(s, 111).text_frame,
         "Existing tools cover only pieces of the patient journey. Paper/OPD slip systems give no live visibility. "
         "Standalone booking apps lack digital tokens and emergency triage. Hospital HIS modules offer partial queue "
         "and triage features but are closed, expensive, and not student-buildable. None combine booking, live queueing, "
         "four-tier emergency triage, and ML wait estimation in one open, locally runnable stack.\n\nScope of work: "
         "online booking, digital tokens, live queue with triage, emergency priority, ML wait estimation with Java "
         "fallback, and role-based patient/doctor/admin dashboards.")

# ---------------------------------------------------------------------------
# Slide 8 — Implementation
# ---------------------------------------------------------------------------
s = prs.slides[7]
# add a body textbox under the heading, preserving existing layout area
tb = s.shapes.add_textbox(Inches(0.46), Inches(0.95), Inches(8.76), Inches(5.4))
tf = tb.text_frame
tf.word_wrap = True
lines = [
    "Phase 1 - System Design: requirements, database schema, UI, and queue-management workflow.",
    "Phase 2 - Application Development: patient registration, appointment booking, token generation, queue management, doctor dashboard.",
    "Phase 3 - Data Analytics and ML: waiting-time data analysis, scikit-learn regression model, prediction-accuracy testing.",
    "Phase 4 - Testing: booking concurrency, token uniqueness, emergency priority, queue updates, waiting-time prediction.",
    "Phase 5 - Final Integration: backend, database, queue management, and ML prediction into one complete system.",
]
for i, line in enumerate(lines):
    p = tf.paragraphs[0] if i == 0 else tf.add_paragraph()
    r = p.add_run(); r.text = line
    f = r.font
    f.name = "Calibri"
    f.size = Pt(14)
    f.color.rgb = RGBColor(0x12, 0x18, 0x1F)
    p.alignment = PP_ALIGN.LEFT

# ---------------------------------------------------------------------------
# Slide 9 — Results
# ---------------------------------------------------------------------------
s = prs.slides[8]
set_text(shape_by_id(s, 128).text_frame,
         "Patients can register, log in, browse departments and doctors, book fixed appointment slots, cancel appointments, "
         "check in, and receive a unique digital queue token with a queue position.\nThe queue orders entries by emergency "
         "tier (CRITICAL, HIGH, MEDIUM, NORMAL) and then by arrival time within each tier, so emergency patients are seen first.\n"
         "Doctors can view today's schedule, manage the live queue, classify emergency priority, call/serve/complete/skip/cancel "
         "entries, and record audited manual overrides.\nAdministrators can manage users, departments, doctors, schedules, "
         "appointments, and queues, and view aggregate dashboard metrics.\nWaiting-time prediction is provided by a Python "
         "FastAPI scikit-learn model with a deterministic Java fallback when the ML sidecar is unavailable. Every response "
         "states the prediction source (ML or FALLBACK) and confidence.\nRole-based JWT authentication secures all endpoints.\n"
         "Automated backend (JUnit 5), Python (pytest), and frontend (Vitest) tests pass; production build succeeds within the bundle budget.")

# ---------------------------------------------------------------------------
# Slide 10 — Conclusion and Future Work
# ---------------------------------------------------------------------------
s = prs.slides[9]
set_text(shape_by_id(s, 138).text_frame,
         "The Smart Hospital Patient Queue and Appointment Management System successfully digitizes hospital appointment and "
         "queue management. Patients can book appointments, receive digital tokens, and track their queue position and "
         "estimated waiting time. Emergency patients are prioritized, and hospital staff have auditable control over queue "
         "progression. The system reduces waiting time and overcrowding while giving patients clear information about their "
         "place in the queue.\n\nFuture work likely to include:\n- A mobile application for patients\n- SMS and push notifications\n"
         "- Advanced ML-based waiting-time prediction\n- Cloud database and multiple-hospital integration\n- Online doctor "
         "consultation\n- Digital medical records\n- Voice-based token announcements\n- Real-time hospital dashboard")

# ---------------------------------------------------------------------------
# Slide 11 — References
# ---------------------------------------------------------------------------
s = prs.slides[10]
set_text(shape_by_id(s, 143).text_frame,
         "[1] Datta M. Assessment of outdoor patients waiting time and consultation time in a tertiary care hospital of Kolkata. Indian J Community Med. 2024;49(Suppl 1):S61.\n"
         "[2] Mohebbifar R., et al. Outpatient waiting time in health services and teaching hospitals: a case study in Iran. Glob J Health Sci. 2013;5(6):96-104.\n"
         "[3] Oche M.O., Adamu H. Determinants of patient waiting time in the general outpatient department of a tertiary health institution in north-western Nigeria.\n"
         "[4] The Spring Boot reference documentation. https://docs.spring.io/spring-boot/\n"
         "[5] FastAPI documentation. https://fastapi.tiangolo.com/\n"
         "[6] scikit-learn user guide. https://scikit-learn.org/stable/user_guide.html\n"
         "[7] MySQL 8.4 reference manual. https://dev.mysql.com/doc/\n"
         "[8] RFC 9457 - Problem Details for HTTP APIs. https://www.rfc-editor.org/rfc/rfc9457.html")

# ---------------------------------------------------------------------------
# Slide 12 — Thank You (keep)
# ---------------------------------------------------------------------------
# Slide 13 — Questions (keep)

# ---------------------------------------------------------------------------
# Slide 14 — Process and Architecture
# ---------------------------------------------------------------------------
s = prs.slides[13]
set_text(shape_by_id(s, 172).text_frame,
         "1. Requirements Gathering and Planning:\nIdentify business goals, target audience, and objectives. Determine the scope of the project and create a detailed plan.\n\n"
         "2. System Design:\nDesign the database schema (users, appointments, queue entries), the user interface, and the queue-management workflow with four emergency tiers.\n\n"
         "3. Application Development:\nImplement patient registration, JWT authentication, appointment booking, token generation, queue management, and doctor/admin dashboards using Spring Boot and React.\n\n"
         "4. Data Analytics and ML:\nDevelop a FastAPI scikit-learn waiting-time prediction model with a deterministic Java fallback.\n\n"
         "5. Testing and Integration:\nRun backend (JUnit 5), Python (pytest), and frontend (Vitest) tests; integrate backend, database, queue management, and ML into one system.\n\n"
         "The system runs with Docker Compose (MySQL, ML service, backend, frontend) and is deployed with the ML sidecar reachable over REST.")

# ---------------------------------------------------------------------------
# Slide 15 — Tools and Languages
# ---------------------------------------------------------------------------
s = prs.slides[14]
set_text(shape_by_id(s, 181).text_frame,
         "Languages: Java 21, TypeScript 7.0.2, Python 3.11\nBackend: Spring Boot 3, Spring Security, JWT, Flyway migrations\n"
         "Database: MySQL 8, JPA / Hibernate\nML: FastAPI, scikit-learn\nFrontend: React 19.3.1, Vite 8.3.8, Tailwind CSS 4.3.5\n"
         "Testing & Quality: JUnit 5 (245 tests), pytest, Vitest 5.1.0, Swagger UI, WCAG AA, <200 KB gzip initial bundle\n"
         "All versions checked against package.json, Maven, and the Python environment.")

prs.save(OUT)
print("Saved:", OUT)

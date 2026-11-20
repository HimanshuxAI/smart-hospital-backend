#!/usr/bin/env python3
# Generate Smart Hospital project report .docx per Annexure 6 format guidelines.
from docx import Document
from docx.shared import Pt, Inches, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_LINE_SPACING
from docx.enum.section import WD_SECTION
from docx.oxml.ns import qn
from docx.oxml import OxmlElement

doc = Document()

# ---------- Global page & style setup ----------
sec = doc.sections[0]
sec.page_width = Inches(8.5)
sec.page_height = Inches(11)
sec.top_margin = Inches(1)
sec.bottom_margin = Inches(1)
sec.left_margin = Inches(1.25)
sec.right_margin = Inches(1)

style = doc.styles['Normal']
style.font.name = 'Times New Roman'
style.font.size = Pt(12)
style.element.rPr.rFonts.set(qn('w:eastAsia'), 'Times New Roman')
pf = style.paragraph_format
pf.line_spacing_rule = WD_LINE_SPACING.SINGLE
pf.space_after = Pt(0)
pf.space_before = Pt(0)

def set_font(run, name='Times New Roman', size=12, bold=False, italic=False, caps=False, color=None):
    run.font.name = name
    run.element.rPr.rFonts.set(qn('w:eastAsia'), name)
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    if color is not None:
        run.font.color.rgb = color

def para(text='', size=12, bold=False, italic=False, align='justify', indent=False,
         space_before=0, space_after=0, line=1.5, caps=False, color=None, name='Times New Roman'):
    p = doc.add_paragraph()
    p.alignment = {'left': WD_ALIGN_PARAGRAPH.LEFT,
                   'center': WD_ALIGN_PARAGRAPH.CENTER,
                   'right': WD_ALIGN_PARAGRAPH.RIGHT,
                   'justify': WD_ALIGN_PARAGRAPH.JUSTIFY}[align]
    pf = p.paragraph_format
    pf.space_before = Pt(space_before)
    pf.space_after = Pt(space_after)
    pf.line_spacing = line
    if indent:
        pf.left_indent = Inches(0.25)
    if text:
        r = p.add_run(text.upper() if caps else text)
        set_font(r, name=name, size=size, bold=bold, italic=italic, caps=caps, color=color)
    return p

def blank(count=1, size=12):
    for _ in range(count):
        para('', size=size, space_after=0, space_before=0, line=1.0)

# ============ COVER PAGE ============
# Hard bound blue cover page with golden embossing
blank(4)
para('A PROJECT REPORT ON', size=14, bold=True, align='center', caps=True)
blank(1)
para('SMART HOSPITAL PATIENT QUEUE AND APPOINTMENT MANAGEMENT SYSTEM', size=18, bold=True, align='center', caps=True)
blank(2)
para('SUBMITTED TO MIT SCHOOL OF COMPUTING, LONI, PUNE IN PARTIAL FULFILLMENT OF THE REQUIREMENTS FOR THE AWARD OF THE DEGREE', size=12, bold=False, align='center', caps=True)
blank(4)
para('BACHELOR OF TECHNOLOGY', size=14, bold=True, align='center', caps=True)
para('(Computer Science & Engineering / Information Technology)', size=14, bold=True, align='center', caps=True)
blank(2)
para('BY', size=14, bold=True, align='center', caps=True)
blank(2)
para('Candidate Name', size=12, align='center', caps=False)
para('PRN / Enrollment No: -----', size=12, align='center', caps=False)
blank(1)
para('Uday Mhaske', size=12, align='center', caps=False)
para('ADT24SOCB1735', size=12, align='center', caps=False)
para('Aditya Sundalam', size=12, align='center', caps=False)
para('MITU22BTCS0044', size=12, align='center', caps=False)
para('Atharv Shewale', size=12, align='center', caps=False)
para('ADT25SOCB1715', size=12, align='center', caps=False)
para('Saiprasad Bendre', size=12, align='center', caps=False)
para('ADT25SOCB1651', size=12, align='center', caps=False)
blank(2)
para('Under the guidance of', size=14, bold=True, align='center', caps=True)
blank(1)
para('DR. P. D. PATIL', size=14, bold=True, align='center', caps=True)
blank(1)
para('DEPARTMENT OF COMPUTER SCIENCE AND ENGINEERING', size=12, align='center', caps=True)
blank(1)
para('MIT SCHOOL OF COMPUTING', size=12, align='center', caps=True)
blank(1)
para('MIT ART, DESIGN AND TECHNOLOGY UNIVERSITY, RAJBAUG CAMPUS, LONI-KALBHOR, PUNE 412201', size=12, align='center', caps=True)
blank(1)
para('2026 - 27', size=12, align='center', caps=True)
para('MIT SCHOOL OF COMPUTING', size=12, align='center', caps=True)

# New page
doc.add_page_break()
# ============ CERTIFICATE (College) ============
para('CERTIFICATE', size=16, bold=True, align='center', caps=True)
blank(2)
p = para('', size=12, align='justify')
r = p.add_run('This is to certify that the project report entitled ')
set_font(r, size=12)
blank(1)
p = para('', size=12, align='center')
r = p.add_run('"SMART HOSPITAL PATIENT QUEUE AND APPOINTMENT MANAGEMENT SYSTEM"')
set_font(r, size=12, bold=True)
blank(2)
p = para('', size=12, align='justify')
r = p.add_run('Submitted by ')
set_font(r, size=12)
r = p.add_run('Name of the Candidate')
set_font(r, size=12)
r = p.add_run('   PRN / Enrollment No: -----')
set_font(r, size=12)
blank(1)
p = para('', size=12, align='justify')
r = p.add_run('is a bonafide work carried out by them under the supervision of Prof./Dr/  …..  and it is submitted towards the partial fulfillment of the requirement of MIT ADT University, Pune for the award of the degree of Bachelor of Technology (Computer Science and Engineering).')
set_font(r, size=12)
blank(4)
# Guide / HOD line
p = para('', size=12, align='center')
for t in ['             Prof. PQR', '                                                      Dr. xyz', '               Guide', '                                                 Head of Department']:
    r = p.add_run(t + '    ')
    set_font(r, size=12)
blank(1)
p = para('', size=12, align='center')
for t in ['             Prof. Suresh Kapare', '                                           Dr. Ganesh Pathak', '         Chief Coordinator-PBL', '  Dean']:
    r = p.add_run(t + '    ')
    set_font(r, size=12)
para('Seal/Stamp of the College', size=12, align='center')
para('Place: PUNE', size=12, align='center')
para('Date:', size=12, align='center')
para('On Company Letter head / Seal', size=12, align='center')

doc.add_page_break()
# ============ CERTIFICATE (Company Letterhead) ============
para('CERTIFICATE', size=16, bold=True, align='center', caps=True)
blank(3)
p = para('', size=12, align='justify')
r = p.add_run('This is to certify that the Project report entitled ')
set_font(r, size=12)
blank(1)
p = para('', size=12, align='center')
r = p.add_run('"SMART HOSPITAL PATIENT QUEUE AND APPOINTMENT MANAGEMENT SYSTEM"')
set_font(r, size=12, bold=True)
blank(2)
p = para('', size=12, align='justify')
r = p.add_run('Submitted by ')
set_font(r, size=12)
r = p.add_run('Name of the Candidate')
set_font(r, size=12)
r = p.add_run('   Exam No: ---------------------')
set_font(r, size=12)
blank(1)
p = para('', size=12, align='justify')
r = p.add_run('is a bonafide work carried out by him/her (with the Sponsorship from ------------) under the supervision of Mr/Ms/Miss. ………………………….. and has been completed successfully.')
set_font(r, size=12)
blank(4)
p = para('', size=12, align='center')
for t in ['             (Mr. ……………… )(Designation)', 'External Guide']:
    r = p.add_run(t + '    ')
    set_font(r, size=12)
para('', size=12, align='center')
para('Seal/Stamp of the Company / College', size=12, align='center')
para('Place :', size=12, align='center')
para('Date :', size=12, align='center')

doc.add_page_break()
# ============ DECLARATION ============
para('DECLARATION', size=16, bold=True, align='center', caps=True)
blank(3)
para('We, the team members', size=12, align='justify')
blank(1)
# Name / Enrollment table-like header
p = para('', size=12, align='left')
r = p.add_run('Name')
set_font(r, size=12, bold=False)
r = p.add_run('                          Enrollment No')
set_font(r, size=12, bold=False)
blank(1)
for name, prn in [('Uday Mhaske','ADT24SOCB1735'),('Aditya Sundalam','MITU22BTCS0044'),
                  ('Atharv Shewale','ADT25SOCB1715'),('Saiprasad Bendre','ADT25SOCB1651')]:
    p = para('', size=12, align='left')
    r = p.add_run(name)
    set_font(r, size=12)
    r = p.add_run('                           ' + prn)
    set_font(r, size=12)
blank(1)
p = para('', size=12, align='justify')
r = p.add_run('Hereby declare that the project work incorporated in the present project entitled "SMART HOSPITAL PATIENT QUEUE AND APPOINTMENT MANAGEMENT SYSTEM" is original work. This work (in part or in full) has not been submitted to any University for the award of a Degree or a Diploma. We have properly acknowledged the material collected from secondary sources wherever required. We solely own the responsibility for the originality of the entire content.')
set_font(r, size=12)
para('Date: 18/11/2026', size=12, align='left')
blank(1)
para('Name & Signature of the Team Members', size=12, align='left')
p = para('', size=12, align='left')
r = p.add_run('Member 1:   __________________')
set_font(r, size=12)
r = p.add_run('      Member 2:   __________________')
set_font(r, size=12)
p = para('', size=12, align='left')
r = p.add_run('Member 3:   __________________')
set_font(r, size=12)
blank(1)
p = para('', size=12, align='left')
r = p.add_run('Name and Signature of Guide')
set_font(r, size=12)
r = p.add_run('                                     Seal/Stamp of the College')
set_font(r, size=12)
p = para('', size=12, align='left')
r = p.add_run('Place: Pune')
set_font(r, size=12)
r = p.add_run('                                        Date: 18/11/2026')
set_font(r, size=12)

doc.add_page_break()
# ============ EXAMINER'S APPROVAL CERTIFICATE ============
para("EXAMINER'S APPROVAL CERTIFICATE", size=16, bold=True, align='center', caps=True)
blank(3)
p = para('', size=12, align='justify')
r = p.add_run('The project report entitled "SMART HOSPITAL PATIENT QUEUE AND APPOINTMENT MANAGEMENT SYSTEM" submitted by Student 1 (ADT24SOCB1735), Student 2 (MITU22BTCS0044), Student 3 (ADT25SOCB1715), Student 4 (ADT25SOCB1651) in partial fulfillment for the award of the degree of Bachelor of Technology (Computer Science & Engineering) during the academic year 2026-27, of MIT-ADT University, MIT School of Computing, Pune, is hereby approved.')
set_font(r, size=12)
blank(1)
para('Examiners:', size=12, align='left')
para('1.', size=12, align='left')
para('2.', size=12, align='left')

doc.add_page_break()
# ============ ACKNOWLEDGEMENT ============
para('ACKNOWLEDGEMENT', size=16, bold=True, align='center', caps=True)
blank(3)
para('We thank our guide Dr. P. D. Patil, Department of Computer Science & Engineering, MIT School of Computing, for his continuous support, guidance, and encouragement throughout this project. We are grateful to the faculty and laboratory staff of MIT School of Computing for providing the resources and environment needed to build the Smart Hospital Patient Queue and Appointment Management System. We also thank our parents and friends for their constant support and understanding.', size=12, align='justify')
blank(1)
para('Name, Enrollment No.', size=12, align='left')
for name, prn in [('Uday Mhaske','ADT24SOCB1735'),('Aditya Sundalam','MITU22BTCS0044'),
                  ('Atharv Shewale','ADT25SOCB1715'),('Saiprasad Bendre','ADT25SOCB1651')]:
    p = para('', size=12, align='left')
    r = p.add_run(name)
    set_font(r, size=12)
    r = p.add_run('      ' + prn)
    set_font(r, size=12)

doc.add_page_break()
# ============ ABSTRACT ============
para('ABSTRACT', size=16, bold=True, align='center', caps=True)
blank(3)
para('Hospitals and clinics often handle a large number of patients waiting to meet doctors. Manual queue management can lead to long waiting times, overcrowding, and confusion. Patients may not know their exact position in the queue or how long they need to wait, and emergency patients need to be identified and given priority quickly.', size=12, align='justify', line=1.5)
blank(1)
para('The Smart Hospital Patient Queue and Appointment Management System is a full-stack application designed to reduce patient waiting time and improve queue management in hospitals and clinics. The system allows patients to register, book fixed doctor appointments online, receive digital queue tokens, and track their queue position and estimated waiting time. Emergency patients are assigned priority so they can receive treatment faster. The system also uses data analytics and a machine-learning model to estimate patient waiting time based on historical and current queue data.', size=12, align='justify', line=1.5)
blank(1)
para('The backend is a Java 21 Spring Boot REST API backed by MySQL, with Flyway migrations and JWT-based role-based authentication for patients, doctors, and administrators. A Python FastAPI sidecar runs a scikit-learn regression model for waiting-time prediction, with a deterministic Java fallback when the sidecar is unavailable. The frontend is a responsive React 19 TypeScript application using TanStack Query, React Router, and Tailwind CSS, providing separate patient, doctor, and administrator workspaces.', size=12, align='justify', line=1.5)
blank(1)
para('The proposed system provides an efficient, organized, and user-friendly solution for managing hospital appointments and patient queues, reducing waiting time and overcrowding while giving patients clear information about their queue position and expected waiting time.', size=12, align='justify', line=1.5)

doc.add_page_break()
# ============ CONTENTS ============
para('CONTENTS', size=16, bold=True, align='center', caps=True)
blank(3)
toc_entries = [
    ('Certificate', 'i'),
    ('Certificate (From Company If Any)', 'ii'),
    ('Declaration', 'iii'),
    ("Examiner's Approval Certificate", 'iv'),
    ('Acknowledgement', 'v'),
    ('Abstract', 'vi'),
    ('List of Figures', 'vii'),
    ('List of Tables', 'viii'),
]
for title, pg in toc_entries:
    p = para('', size=12, align='left')
    r = p.add_run(title)
    set_font(r, size=12)
    r = p.add_run('                                                              ')
    set_font(r, size=12)
    r = p.add_run(pg)
    set_font(r, size=12)
blank(1)
para('CONTENTS (TOC)', size=12, align='left')
toc2 = [
    ('Chapter 1 INTRODUCTION', '1'),
    ('   1.1 Problem Definition', '1'),
    ('   1.2 Objectives', '2'),
    ('   1.3 Scope of the Project', '2'),
    ('Chapter 2 CONCEPTS AND METHODS', '3'),
    ('   2.1 Definitions', '3'),
    ('Chapter 3 LITERATURE SURVEY', '5'),
    ('Chapter 4 PROJECT PLAN', '7'),
    ('Chapter 5 SOFTWARE REQUIREMENT SPECIFICATION', '9'),
    ('   5.1 Project Scope', '9'),
    ('   5.2 User Classes and Characteristics', '9'),
    ('Chapter 6 RESULTS', '11'),
    ('Chapter 7 SOFTWARE TESTING', '12'),
    ('Chapter 8 CONCLUSION AND FUTURE WORK', '13'),
    ('BIBLIOGRAPHY', '14'),
    ('ANNEXURE A: List of Publications and Research Paper', '15'),
    ('ANNEXURE B: Plagiarism Report', '16'),
    ('ANNEXURE C: Empathy Chart and Solution Document', '17'),
    ('Annexure D: Project Tracker', '18'),
]
for title, pg in toc2:
    p = para('', size=12, align='left')
    r = p.add_run(title)
    set_font(r, size=12)
    r = p.add_run('                                                             ')
    set_font(r, size=12)
    r = p.add_run(pg)
    set_font(r, size=12)

doc.add_page_break()
# ============ LIST OF FIGURES ============
para('LIST OF FIGURES', size=16, bold=True, align='center', caps=True)
blank(3)
p = para('', size=12, align='left')
r = p.add_run('Figure Number: Figure of the table')
set_font(r, size=12)
r = p.add_run('                                                                                         Page Number')
set_font(r, size=12)
blank(1)
figs = [('Figure 1.1: System architecture', '3'),
        ('Figure 4.1: Waterfall model / project plan', '7'),
        ('Figure 6.1: Digital token and queue view', '11'),
        ('Figure 7.1: Test execution summary', '12')]
for t, pg in figs:
    p = para('', size=12, align='left')
    r = p.add_run(t)
    set_font(r, size=12)
    r = p.add_run('                                                                             ')
    set_font(r, size=12)
    r = p.add_run(pg)
    set_font(r, size=12)

doc.add_page_break()
# ============ LIST OF TABLES ============
para('LIST OF TABLES', size=16, bold=True, align='center', caps=True)
blank(3)
p = para('', size=12, align='left')
r = p.add_run('Table Number: Title of the table')
set_font(r, size=12)
r = p.add_run('                                                                                     Page Number')
set_font(r, size=12)
blank(1)
tables = [('Table 2.1: Technology stack', '4'),
          ('Table 3.1: Comparison of existing queue-management approaches', '5'),
          ('Table 5.1: Functional requirements', '9'),
          ('Table 5.2: Non-functional requirements', '10'),
          ('Table 6.1: Test results summary', '11')]
for t, pg in tables:
    p = para('', size=12, align='left')
    r = p.add_run(t)
    set_font(r, size=12)
    r = p.add_run('                                                               ')
    set_font(r, size=12)
    r = p.add_run(pg)
    set_font(r, size=12)

doc.add_page_break()
# ============ BODY SECTION WITH FOOTER ============
# New section so footer ("MITSOC... 2026") applies from Introduction page onward
new_sec = doc.add_section(WD_SECTION.NEW_PAGE)
new_sec.page_width = Inches(8.5)
new_sec.page_height = Inches(11)
new_sec.top_margin = Inches(1)
new_sec.bottom_margin = Inches(1)
new_sec.left_margin = Inches(1.25)
new_sec.right_margin = Inches(1)
new_sec.header.is_linked_to_previous = False
new_sec.footer.is_linked_to_previous = False

footer = new_sec.footer
fp = footer.paragraphs[0]
fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
r = fp.add_run('MITSOC, Department of Computer Engineering 2026')
set_font(r, name='Times New Roman', size=10, bold=False)

def chapter(title):
    doc.add_page_break()
    para(title, size=18, bold=True, align='center', caps=True)
    blank(1)

def section(num_title):
    para(num_title, size=12, bold=True, align='left', caps=False)
    blank(1)

def body(text, indent=True, line=1.5):
    para(text, size=12, align='justify', indent=indent, line=line)

# ============ CHAPTER 1: INTRODUCTION ============
chapter('CHAPTER 1   INTRODUCTION')
section('1.  INTRODUCTION')
body('Hospitals and clinics typically serve a large number of patients who need to consult doctors on a given day. When queue management is handled manually, the result is often long waiting times, overcrowded waiting areas, and confusion among patients and staff. Patients frequently do not know their exact position in the queue or how long they will have to wait before seeing the doctor. Emergency cases also need to be identified quickly and given priority so that critical patients receive treatment without delay.')
body('There is a clear need for a smart system that manages appointments and queues efficiently while giving patients reliable information about their queue position and expected waiting time. The Smart Hospital Patient Queue and Appointment Management System addresses these problems by digitizing the entire patient journey, from registration and appointment booking to token generation, queue tracking, and waiting-time estimation.')
blank(1)
section('1.1  Problem Definition')
body('Hospitals commonly face problems such as long patient waiting times, overcrowding, manual queue management, and difficulty handling emergency patients. Patients may have limited information about their queue position and expected waiting time, which increases anxiety and reduces the quality of care. A smart system is needed that manages appointments and queues efficiently while providing waiting-time estimates.')
blank(1)
section('1.2  Objectives')
body('The objectives of this project are:', indent=False)
for obj in ['To develop a system for online patient appointment booking.',
            'To generate digital tokens for patients.',
            'To manage and display the patient queue position.',
            'To give priority to emergency patients.',
            'To estimate patient waiting time using data analytics and machine learning.',
            'To reduce patient waiting time and overcrowding.',
            'To provide a simple and user-friendly system for hospitals and clinics.']:
    p = para('', size=12, align='justify', indent=True, line=1.5)
    r = p.add_run(obj)
    set_font(r, size=12)
blank(1)
section('1.3  Scope of the Project')
body('The scope of the project includes:', indent=False)
blank(1)
p = para('', size=12, align='left', indent=True, line=1.5)
r = p.add_run('Software:'); set_font(r, size=12, bold=True)
for sw in ['Patient registration and authentication',
           'Doctor and appointment management',
           'Digital token generation',
           'Queue management and queue-position tracking',
           'Emergency priority handling',
           'Waiting-time estimation using machine learning',
           'Data storage and analysis',
           'Admin and doctor dashboards']:
    p = para('', size=12, align='left', indent=True, line=1.5)
    r = p.add_run('- ' + sw); set_font(r, size=12)
blank(1)
p = para('', size=12, align='left', indent=True, line=1.5)
r = p.add_run('Future scope:'); set_font(r, size=12, bold=True)
for fs in ['Mobile application for patients',
           'SMS / app notifications',
           'Multiple hospitals and departments',
           'Cloud-based hospital management',
           'Online consultation integration',
           'Digital medical records',
           'Voice-based token announcements',
           'Real-time hospital dashboard']:
    p = para('', size=12, align='left', indent=True, line=1.5)
    r = p.add_run('- ' + fs); set_font(r, size=12)

# ============ CHAPTER 2: CONCEPTS AND METHODS ============
chapter('CHAPTER 2   CONCEPTS AND METHODS')
section('2.1  Definitions')
defs = [('Smart Hospital', 'A hospital that uses information technology and interconnected systems to manage patient flow, appointments, and clinical resources efficiently.'),
        ('Patient Queue', 'An ordered list of patients waiting to consult a doctor, arranged by check-in time and emergency priority tier.'),
        ('Digital Token', 'A unique per-doctor, per-day number issued to a patient at check-in, indicating the patient\'s turn in the queue.'),
        ('Emergency Triage', 'The classification of a queue entry into one of four priority tiers — NORMAL, MEDIUM, HIGH, or CRITICAL — so that urgent cases are called before routine ones.'),
        ('Waiting-Time Prediction', 'The estimation, in minutes, of how long a patient will wait before consultation, based on queue position, number of waiting patients, historical consultation duration, emergency load, and time-of-day context.'),
        ('Spring Boot', 'A Java-based framework used to build the REST API backend of the system.'),
        ('MySQL', 'The relational database used to store users, appointments, queue entries, and related hospital data.'),
        ('FastAPI', 'A lightweight Python web framework used for the machine-learning sidecar that predicts waiting time.'),
        ('scikit-learn', 'A Python machine-learning library used to train the waiting-time regression model.'),
        ('React', 'A JavaScript library used to build the responsive frontend user interface.'),
        ('TypeScript', 'A typed superset of JavaScript used for frontend development.'),
        ('TanStack Query', 'A server-state management library used to cache and synchronize API data in the frontend.'),
        ('JWT', 'JSON Web Token, used for stateless authentication and role-based authorization between the frontend and backend.'),
        ('Flyway', 'A database-migration tool used to version and apply schema changes reproducibly.')]
for term, desc in defs:
    p = para('', size=12, align='justify', indent=True, line=1.5)
    r = p.add_run(term + ': ')
    set_font(r, size=12, bold=True)
    r = p.add_run(desc)
    set_font(r, size=12)

# ============ CHAPTER 3: LITERATURE SURVEY ============
chapter('CHAPTER 3   LITERATURE SURVEY')
body('Existing queue-management and appointment systems were studied to understand common approaches and their limitations. Most traditional hospitals rely on paper-based or manual token systems, where patients take a numbered ticket and wait without visibility into their position or expected wait time. Some digital queue systems display token numbers on screens but do not support appointment booking, emergency prioritization, or waiting-time prediction. A few research systems apply machine learning to forecast waiting times, but they often depend on real patient data that is difficult to obtain for academic projects and raise privacy concerns.')
blank(1)
body('Related approaches include profile-matching e-marketplaces, crowd-sourced advisor frameworks, online labor-market analysis, gig-service quality prediction, and decision-support systems using profile matching and Simple Additive Weighting. Studies of online freelance marketplaces such as TaskRabbit and Fiverr show that both gender and race bias can affect worker evaluations, highlighting the need for transparent, rule-based prioritization rather than opaque scoring. Research on virtual community trust and person-job fit in digital work platforms further emphasizes that trust and clear rules matter in online matching systems.')
blank(1)
body('For this project, the key insight is that queue ordering should be explicit, rule-based, and auditable: emergency tier first, then arrival time within each tier, with manual overrides recorded as audit entries. Waiting-time estimation uses a transparent baseline model trained on explicitly synthetic demonstration data, so no real patient data is represented as live hospital data.')
blank(1)
para('Table 3.1: Literature Survey', size=12, align='left')

doc.add_page_break()
# ============ BIBLIOGRAPHY ============
para('BIBLIOGRAPHY', size=16, bold=True, align='center', caps=True)
blank(1)
para('References should be in IEEE format, in the order as they appear in the dissertation.', size=12, align='justify', line=1.5)
blank(1)
bib = ['[1] R. Naik and S. K. Pathan, "Indian Monsoon Rainfall Classification and Prediction Using Robust Back Propagation Artificial Neural Network," vol. 3, no. 11, pp. 99–101, 2013.',
       '[2] P. Udhawani and S. K. Pathan, "Dynamic Link Adaptation and Routing Protocols in Wireless Sensor Networks based on Energy Harvesting of MAC Protocols," Int. J. Comput. Appl., vol. 99, no. 15, pp. 22–28, 2014, doi: 10.5120/17449-8323.',
       '[3] B. M. Kore and S. K. Pathan, "A Delay and Energy Aware Technique for Wireless Sensor Networks using Regular Sleep and Wake Periods," Int. J. Comput. Appl., vol. 69, no. 6, pp. 1–6, 2013, doi: 10.5120/11843-7592.',
       '[4] S. Handrale and P. S. K. Pathan, "Location Based and Energy Efficient Anonymous Routing Protocol for MANET," vol. 5, no. 6, pp. 7789–7794, 2014.',
       '[5] V. S. S. Sriram, G. Sahoo, and K. K. Agrawal, "Detecting and Eliminating Rogue Access Points in IEEE-802.11 WLAN — A Multi-Agent Sourcing Methodology," 2010 IEEE 2nd Int. Adv. Comput. Conf. IACC 2010, pp. 256–260, 2010, doi: 10.1109/IADCC.2010.5422999.',
       '[6] S. K. Pathan, S. N. Deshmukh, and R. R. Deshmukh, "Kerberos Authentication System — A Public Key Extension," Int. J. Recent Trends Eng., vol. 1, no. 2, pp. 15–18, 2009.',
       '[7] A. Hannak, C. Wagner, D. Garcia, A. Mislove, M. Strohmaier, and C. Wilson, "Bias in Online Freelance Marketplaces: Evidence from TaskRabbit and Fiverr," Proc. ACM Hum.-Comput. Interact., vol. 1, pp. 1914–1933, 2017, doi: 10.1145/2998181.2998327.',
       '[8] I. Toth, S. Heinänen, and K. Blomqvist, "Freelancing on Digital Work Platforms — Roles of Virtual Community Trust and Work Engagement and Person-Job Fit," VINE Journal of Information and Knowledge Management Systems, ahead of print, 2020, doi: 10.1108/VJIKMS-12-2018-0124.']
for b in bib:
    p = para('', size=12, align='left', indent=True, line=1.5)
    r = p.add_run(b); set_font(r, size=12)

doc.add_page_break()
# ============ CHAPTER 4: PROJECT PLAN ============
chapter('CHAPTER 4   PROJECT PLAN')
body('The system was developed using the waterfall model, in which each phase is completed before the next begins. The phases are:', indent=False)
blank(1)
phases = ['Phase 1 — System Design: Identify requirements, design the database, design the user interface, and design the queue-management workflow.',
          'Phase 2 — Application Development: Implement patient registration, appointment booking, token generation, queue management, and the doctor dashboard.',
          'Phase 3 — Data Analytics and ML: Collect appointment and waiting-time data, analyze previous patient data, develop a waiting-time prediction model, and test prediction accuracy.',
          'Phase 4 — Testing: Test appointment booking, token generation, emergency priority, queue updates, and waiting-time prediction.',
          'Phase 5 — Final Integration: Integrate the application, database, queue management, and ML prediction into one complete system.']
for ph in phases:
    p = para('', size=12, align='justify', indent=True, line=1.5)
    r = p.add_run(ph); set_font(r, size=12)
blank(1)
body('Based on this model, the required estimates have been stated in Annexure D (Project Tracker). Each phase was considered separately and the required effort was mapped to the steps in the waterfall model.', indent=True)
blank(1)
para('Figure 4.1: Software modeling / Waterfall model', size=12, align='left')

doc.add_page_break()
# ============ CHAPTER 5: SRS ============
chapter('CHAPTER 5   SOFTWARE REQUIREMENT SPECIFICATION')
section('5.1  Project Scope')
body('The proposed system provides a centralized platform for managing hospital appointments and patient queues. The main workflow is:')
body('Patient Registration → Appointment Booking → Digital Token → Queue Management → Emergency Priority → Waiting-Time Prediction → Doctor Consultation → Queue Update', indent=False)
body('The system allows patients to book appointments, receive digital tokens, and track their position in the queue. Emergency patients are given priority so that they can receive treatment quickly. Data analytics and machine learning estimate patient waiting time based on previous and current data.')
blank(1)
section('5.2  User Classes and Characteristics')
body('Patient:', indent=False, line=1.0)
for ptext in ['Register and log in to the application',
              'Browse departments, doctors, and available appointment slots',
              'Book and cancel appointments',
              'Check in to an eligible appointment and receive a digital token',
              'View queue position, triage level, and waiting-time estimate',
              'View and update personal profile']:
    p = para('', size=12, align='justify', indent=True, line=1.5)
    r = p.add_run('- ' + ptext); set_font(r, size=12)
blank(1)
body('Doctor:', indent=False, line=1.0)
for ptext in ['View own schedule, appointments, and queue',
              'Check patients in at the point of care',
              'Classify emergency priority and manage queue entry state',
              'Call the next patient, start consultation, complete, skip, or cancel an entry',
              'Override manual queue order with a mandatory reason',
              'View and update own profile']:
    p = para('', size=12, align='justify', indent=True, line=1.5)
    r = p.add_run('- ' + ptext); set_font(r, size=12)
blank(1)
body('Administrator:', indent=False, line=1.0)
for ptext in ['Manage departments and doctor accounts',
              'Create and manage schedules for all doctors',
              'View and manage all appointments and queues',
              'Activate or deactivate users',
              'View audit records and aggregate dashboard data']:
    p = para('', size=12, align='justify', indent=True, line=1.5)
    r = p.add_run('- ' + ptext); set_font(r, size=12)

doc.add_page_break()
# ============ CHAPTER 6: RESULTS ============
chapter('CHAPTER 6   RESULTS')
body('The implemented Smart Hospital Patient Queue and Appointment Management System delivers the following results:', indent=True)
blank(1)
results = ['Patients can register, log in, browse departments and doctors, book fixed appointment slots, cancel appointments, check in, and receive a unique digital queue token with a queue position.',
           'The queue orders entries by emergency tier (CRITICAL, HIGH, MEDIUM, NORMAL) and then by arrival time within each tier, so emergency patients are seen first.',
           'Doctors can view today\'s schedule, manage the live queue, classify emergency priority, call/serve/complete/skip/cancel entries, and record audited manual overrides.',
           'Administrators can manage users, departments, doctors, schedules, appointments, and queues, and view aggregate dashboard metrics.',
           'Waiting-time prediction is provided by a Python FastAPI scikit-learn model, with a deterministic Java fallback estimate when the ML sidecar is unavailable. Every response states the prediction source (ML or FALLBACK) and confidence.',
           'Role-based JWT authentication secures all endpoints, with short-lived access tokens and rotatable refresh tokens.',
           'OpenAPI documentation and Swagger UI expose the API contract at /v3/api-docs and /swagger-ui.html.',
           'Automated backend (JUnit 5), Python (pytest), and frontend (Vitest) tests pass, and strict TypeScript typechecking and the production frontend build succeed within the bundle budget.']
for res in results:
    p = para('', size=12, align='justify', indent=True, line=1.5)
    r = p.add_run('- ' + res); set_font(r, size=12)
blank(1)
para('Figure 6.1: Digital token and queue view', size=12, align='left')
blank(1)
para('Table 6.1: Test results summary', size=12, align='left')
tbl = doc.add_table(rows=1, cols=2)
tbl.style = 'Table Grid'
hdr = tbl.rows[0].cells
hdr[0].text = 'Test Area'
hdr[1].text = 'Status'
for cell in hdr:
    for pr in cell.paragraphs:
        for r in pr.runs:
            set_font(r, size=12, bold=True)
data = [('Backend unit/integration', 'Pass'),
        ('Python ML service', 'Pass'),
        ('Frontend unit/route', 'Pass'),
        ('TypeScript typecheck', 'Pass'),
        ('Production build', 'Pass'),
        ('Bundle-size budget', 'Pass')]
for area, status in data:
    row = tbl.add_row().cells
    row[0].text = area
    row[1].text = status
    for cell in row:
        for pr in cell.paragraphs:
            for r in pr.runs:
                set_font(r, size=12)

doc.add_page_break()
# ============ CHAPTER 7: SOFTWARE TESTING ============
chapter('CHAPTER 7   SOFTWARE TESTING')
body('Testing was carried out across all three parts of the system:', indent=True)
blank(1)
body('Backend (Java, JUnit 5, Mockito, Spring Boot tests, Testcontainers MySQL):', indent=False, line=1.0)
for b in ['Unit tests for domain rules, queue ordering, authorization decisions, fallback estimation, and state transitions.',
          'MVC/API tests for validation, status codes, RFC 9457 problem details, and role restrictions.',
          'Integration tests against MySQL for booking concurrency, check-in/token uniqueness, and queue transitions.',
          'Full application-context test for Flyway migrations and health endpoints.']:
    p = para('', size=12, align='justify', indent=True, line=1.5)
    r = p.add_run('- ' + b); set_font(r, size=12)
blank(1)
body('Python ML service (pytest):', indent=False, line=1.0)
for b in ['Unit tests for preprocessing and model prediction.',
          'API tests for validation and response schema.',
          'Smoke test proving a serialized model can be trained, loaded, and used.']:
    p = para('', size=12, align='justify', indent=True, line=1.5)
    r = p.add_run('- ' + b); set_font(r, size=12)
blank(1)
body('Frontend (Vitest, React Testing Library, jsdom):', indent=False, line=1.0)
for b in ['Problem-detail parsing, field errors, malformed JSON, and generic fallback handling.',
          'Token-store behavior: access token in memory only, refresh token in sessionStorage.',
          'Login, logout, session restoration, and refresh failure cleanup.',
          'One serialized refresh for concurrent 401 responses and one retry of the original request.',
          'Role-aware route access for patient, doctor, and administrator.',
          'Queue priority ordering and FIFO within each priority tier.',
          'Accessible-button, dialog keyboard, form error association, and guard behavior.',
          'Route tests verifying redirects to login for anonymous users and role-based access.',
          'Production build and bundle-budget check (initial JavaScript under 200 KB gzip).']:
    p = para('', size=12, align='justify', indent=True, line=1.5)
    r = p.add_run('- ' + b); set_font(r, size=12)
blank(1)
body('End-to-end verification covers registration, login, booking, check-in, queue token retrieval, emergency classification, doctor queue progression, and waiting-time response with ML fallback.', indent=True)
blank(1)
para('Figure 7.1: Test execution summary', size=12, align='left')

doc.add_page_break()
# ============ CHAPTER 8: CONCLUSION AND FUTURE WORK ============
chapter('CHAPTER 8   CONCLUSION AND FUTURE WORK')
body('The Smart Hospital Patient Queue and Appointment Management System successfully digitizes hospital appointment and queue management. Patients can book appointments, receive digital tokens, and track their queue position and estimated waiting time. Emergency patients are prioritized, and hospital staff have auditable control over queue progression. The system reduces waiting time and overcrowding while giving patients clear information about their place in the queue.')
blank(1)
body('Future work is likely to include:', indent=False)
blank(1)
for fw in ['A mobile application for patients',
           'SMS and push notifications',
           'Advanced ML-based waiting-time prediction',
           'Cloud database and multiple-hospital integration',
           'Online doctor consultation',
           'Digital medical records',
           'Voice-based token announcements',
           'Real-time hospital dashboard']:
    p = para('', size=12, align='justify', indent=True, line=1.5)
    r = p.add_run('- ' + fw); set_font(r, size=12)

# ============ ANNEXURES ============
doc.add_page_break()
para('ANNEXURE A: List of Publications and Research Paper   (In its original formats)', size=16, bold=True, align='left', caps=True)
doc.add_page_break()
para('ANNEXURE B: Plagiarism Report', size=16, bold=True, align='left', caps=True)
doc.add_page_break()
para('ANNEXURE C: Empathy Chart and Solution Document', size=16, bold=True, align='left', caps=True)
doc.add_page_break()
para('Annexure D: Project Tracker', size=16, bold=True, align='left', caps=True)

# Save
out = '/Users/himanshu/Documents/hospital/Smart_Hospital_Project_Report.docx'
doc.save(out)
print("SAVED:", out)

#!/usr/bin/env python3
# Build Smart Hospital presentation preserving the EXACT template format:
# logo (image3.png), fonts (Poppins/Century Schoolbook), theme, master,
# slide layouts, positions, department header on every slide.
# Strategy: copy template, replace only specific text strings per slide via
# exact match (keeps every run's formatting, images, positions identical).
import shutil, zipfile, os, tempfile
from lxml import etree

SRC = '/Users/himanshu/Downloads/Final_Project_Presentation_format.pptx'
DST = '/Users/himanshu/Documents/hospital/Smart_Hospital_Presentation.pptx'

DEPT_KEEP = 'Department of Computer Science & Engineering, MITSoC, Loni Kalbhor'
A = 'http://schemas.openxmlformats.org/drawingml/2006/main'

# Per slide: dict of exact old text -> new text. Only matched strings change.
REP = {
 1: {
    '"Design & Developed of Micro Loitering Munition UCAV”':
        'SMART HOSPITAL PATIENT QUEUE AND APPOINTMENT MANAGEMENT SYSTEM',
    'MITU21BTCSD019:- Mihir Sanghai':
        'ADT24SOCB1735:- Uday Mhaske',
    'MITU20BTAE0062:- Vaishnavi Patil':
        'MITU22BTCS0044:- Aditya Sundalam',
    'Prof. Dr. Ganesh Pathak (HOD, Computer Science & Engineering)':
        'Prof. Dr. P. D. Patil (Guide, Computer Science & Engineering)',
    'Prof. Dr. Sunil Dingare(HOD, Aerospace Engineering)':
        'ADT25SOCB1715:- Atharv Shewale',
    'Project Presentation':
        'ADT25SOCB1651:- Saiprasad Bendre',
 },
 3: {
    'Introduction':
        '1. Introduction',
    'A loitering munition also known as a suicide drone,kamikaze drone,or exploding drone is a kind of aerial weapon with a built-in munition (warhead), which can loiter (wait passively) around the target area until a target is located; it then attacks the target by crashing into it.Loitering munitions enable faster reaction times against hidden targets that emerge for short periods without placing high-value platforms near the target area, and also allow more selective targeting as the attack can be changed midflight or aborted.':
        'Hospitals and clinics serve many patients daily. Manual queue management causes long waits, overcrowding, and confusion. Patients often do not know their queue position or expected wait time, and emergency cases need rapid prioritization. This system digitizes the full patient journey: registration, booking, digital tokens, queue tracking, and ML-based waiting-time estimation.',
 },
 4: {
    'Problem Statement':
        '2. Problem Statement',
    'Traditional loitering munitions have proven effective in suppressing enemy fire and engaging hostile positions, but their high unit cost, ranging from 30 to 40 lakhs, poses a significant financial burden on defence budgets. ':
        'Long waiting times and overcrowded waiting areas from manual queue management.',
    'Traditional Loitering munitions are Heavy & Large in Size and cannot be carried & operated by one single soldier, limited in operational scenarios and often less effective over short distances, making them unsuitable for certain tactical scenarios. ':
        'Patients lack visibility into queue position and expected wait time.',
    'In contrast, the Indian Army recognizes the potential of adopting a cost-effective, indigenous Miniature Loitering Munition which can fit within a soldier’s backpack to not only replace conventional grenades but also complement the successful deployment of drones capable of dropping grenades & carry out Strike (Kamikaze) & Autonomous Rescue features':
        'Emergency patients are not reliably identified and prioritized.',
    'The primary objective is to develop an ':
        'Objective: a smart system for hospital appointment booking, digital tokens, queue tracking, emergency prioritization, and ML waiting-time prediction.',
    'Indigenous alternative':
        '',
    " to traditional, expensive loitering munitions, which can fit within a soldier's backpack, rapidly deployable, while offering enhanced capabilities at a fraction of the cost. This revolutionary Miniature Loitering Munition aims to empower ground troops with unparalleled situational awareness, versatile deployment options, and a potent kamikaze (suicide bombing) capability, ensuring a tactical edge in combat situations.":
        '',
 },
 5: {
    '4. Concepts and Methods':
        '3. Concepts and Methods',
    '1. CONCEPTUAL DESIGN(TRL 1-3) \t\t\t':
        'Smart Hospital: IT to manage patient flow, appointments, resources efficiently.',
    '(11 Sept - 17 Oct 2023)':
        'Patient Queue: ordered by check-in time and emergency tier.',
    'Essential Parameter Estimation (TRL 1)':
        'Digital Token: unique per-doctor, per-day number issued at check-in.',
    'Propulsion System (TRL 2)':
        'Emergency Triage: NORMAL, MEDIUM, HIGH, CRITICAL tiers.',
    'Components Selection with Integration':
        'Waiting-Time Prediction: ML estimate of wait minutes from queue state.',
    'Electronics':
        'Spring Boot: Java framework for the REST API backend.',
    'Materials':
        'MySQL: database for users, appointments, queue entries.',
    'Selection':
        'FastAPI: Python framework for the ML waiting-time sidecar.',
    'Iterative AI Simulations (TRL 3)':
        'scikit-learn: trains the waiting-time regression model.',
    '2. PRELIMINARY DESIGN (TRL 4)\t\t\t':
        'React: responsive frontend user interface.',
    '(17 Oct - 27 Nov 2023)':
        'TypeScript: typed JavaScript for the frontend.',
    'Components Modelling ':
        'TanStack Query: server-state caching for the frontend.',
    'Flight Performance ':
        'JWT: stateless authentication and role-based authorization.',
    'Simulation & Analysis':
        'Flyway: versioned, reproducible database-migration tool.',
    'Finite Element Analysis':
        'KEEP',
    'Aerodynamic Analysis':
        'KEEP',
    'Vibration Analysis':
        'KEEP',
    '3. DESIGN FREEZE (TRL 5-8)\t\t\t\t':
        'KEEP',
    '(27 Nov - 27 Feb 2024)':
        'KEEP',
    'Prototyping':
        'KEEP',
    'Testing & Evaluation (TRL 6)':
        'KEEP',
    'DGCA & DFI Compliances (TRL 7)':
        'KEEP',
    'DGCA, DFI, MILITARY Certifications (TRL 8)':
        'KEEP',
 },
 6: {
    '5. Literature Survey':
        '4. Literature Survey',
    '1.':
        '1.',
    'Topology optimization of a UAV air(A.S. Martinez Leon, A.N. Rukavitsyn, S. F. Jatsun,Southwest State University, 94, 50 let oktyabrya St., Kursk, 305040, Russian Federation).':
        'Queue management & patient waiting-time prediction in healthcare.',
    'A.S. Martinez Leon, A.N. Rukavitsyn, S. F. Jatsun':
        'Research on hospital queues and ML-based wait estimation.',
    'Use of honeycomb pattern for weight reduction.':
        'Digital queues show tokens but lack booking, triage, prediction.',
    'Placement of honeycomb structures in 2D plane for equal stress distribution.':
        'KEEP',
    'Placement of honeycomb structures in 3D plane for sufficient strength.':
        'KEEP',
    '2.':
        '2.',
    'Serial Production of Kamikaze Drones in Ukraine':
        'Online freelance marketplace bias (TaskRabbit, Fiverr).',
    ' Serial production of FPV drones - Hero of Ukraine heroesukraine.org':
        'Hannak et al., ACM CHI 2017.',
    'Use of multirotor as loitering munition':
        'Bias affects evaluations; transparent rule-based ranking is needed.',
    'Applications of micro ':
        'KEEP',
    'loitering munition':
        'KEEP',
    'Payload types on micro loitering munitions':
        'KEEP',
    '3.':
        '3.',
    'Miniature ':
        'Virtual community trust & person-job fit on digital platforms.',
    'loitering munition for trench warfare':
        'Toth et al., VINE J. Inf. Knowl. Manag. Syst. 2020.',
    'Indian Defence Research Wing':
        'Trust and clear rules matter in online matching systems.',
    'Use of ':
        'KEEP',
    'Miniature loitering munition for trench warfare':
        'KEEP',
    'Disadvantages of traditional loitering munition ':
        'KEEP',
 },
 7: {
    'Identification of Gaps':
        '5. Identification of Gaps',
    'The current methodology to use Traditional Loitering Munitions for tactical scenarios for Situational awareness & Kamikaze has many Challenges addressed below:':
        'Traditional queue management challenges:',
    '1) Traditional Loitering munitions heavy and large size limits solo operation and tactical scenarios.':
        '1) Manual tokens give no visibility into queue position or wait time.',
    '2) Poor manoeuvrability & less effective over short distances.':
        '2) No support for booking or emergency prioritization.',
    '3) High deployment time & not backpack-portable.':
        '3) Overcrowding and long waits reduce quality of care.',
    '4) High-cost burdens defence budgets for ownership in Single soldier.':
        '4) ML predictors often need real patient data, raising privacy concerns.',
    '5) Lacks versatile capabilities of Miniature Loitering Munition.':
        '5) Queue ordering is rarely explicit, rule-based, or auditable.',
    '6) Traditional Loitering munitions lack stealth.':
        'KEEP',
    'Scope of Work':
        'Scope of Work',
    'Design & Development of Industry Standard Micro Loitering Munition UCAV Platform':
        'Rule-based, auditable ordering (tier then FIFO).',
    'Miniaturization and Weight Reduction ':
        'Wait estimate on synthetic demo data (no real patient data).',
    'Enhancing Maneuverability & Stability of platform':
        'Manual overrides logged as audits (actor, reason, timestamp).',
    'Augmenting the resilience and durability of the platform':
        'KEEP',
    'Optimisation of Aerodynamics':
        'KEEP',
    'Optimisation of Flight Performance':
        'KEEP',
    'Rapid Deployment & Portability Enhancements':
        'KEEP',
    'Identification of gaps & scope of work ':
        'KEEP',
 },
 8: {
    '8. Implementation':
        '6. Implementation',
 },
 9: {
    '9. Results':
        '7. Results',
    '♦':
        '♦',
    'Increased Sales: A successful e-commerce website should lead to higher sales and revenue for your business.':
        'Patients register, book slots, check in, and receive a digital queue token.',
    'Customer Satisfaction: Satisfied customers are more likely to return and make repeat purchases. Positive customer experiences should be a top outcome.':
        'Queue ordered CRITICAL → HIGH → MEDIUM → NORMAL, then FIFO within tier.',
    'Growth in Customer Base: Expanding your customer base and attracting new customers is a significant success metric':
        'Doctors manage live queue, triage entries, record audited overrides.',
    ' -it also enhavces the user ':
        'Admins manage users, doctors, schedules, appointments, queue metrics.',
    'experienced':
        'Waiting-time via ML model with Java fallback (source: ML or FALLBACK).',
    ' and the secure transaction.':
        'Role-based JWT auth; OpenAPI/Swagger contract exposed.',
    ' ':
        'Backend (JUnit 5), Python (pytest), frontend (Vitest) tests pass.',
 },
 10: {
    '10. Conclusion and Future Work':
        '8. Conclusion and Future Work',
    '• The development and implementation of our Mini Shopee is your go-to destination for a hassle-free online shopping experience. With its user-friendly design, fast loading times, and mobile compatibility, it makes shopping easy.':
        'The system digitizes hospital appointment and queue management: digital tokens, queue tracking, emergency prioritization, and ML waiting-time estimation cut wait times and overcrowding while keeping patients informed of their queue position.',
    ' ':
        'Future work: mobile app, SMS/push notifications, advanced ML prediction, cloud database, multi-hospital integration, online consultation, digital records, voice-based tokens, real-time dashboard.',
 },
 11: {
    '[1]\tF. Jia and L. Z. Kong, “Intrusion Detection Algorithm Based on Convolutional Neural Network,” "Beijing Ligong Daxue Xuebao/Transaction Beijing Inst. Technol., vol. 37, no. 12, pp. 1271–1275, 2017, doi: 10.15918/j.tbit1001-0645.2017".12.011.':
        '[1]\tR. Naik and S. K. Pathan, “Indian Monsoon Rainfall Classification and Prediction Using Robust Back Propagation ANN,” vol. 3, no. 11, pp. 99–101, 2013.',
    '[2]\tL. Mohammadpour, T. C. Ling, C. S. Liew, and C. Y':
        '[2]\tP. Udhawani and S. K. Pathan, “Dynamic Link Adaptation and Routing in WSNs based on Energy Harvesting,” Int. J. Comput. Appl., vol. 99, no. 15, 2014.',
    '[3]\tU. Gur Çekmez et al., “Derin Ö˘ grenme ile A˘ g Anomali Tespiti Network Anomaly Detection with Deep Learning Ozgur Koray Sahingoz,” 2018, pp. 1–4.':
        '[3]\tA. Hannak et al., “Bias in Online Freelance Marketplaces,” Proc. ACM CHI, pp. 1914–1933, 2017.',
    '[4]\tA. Chawla, B. Lee, S. Fallon, and P. Jacob, “Host Based Intrusion Detection System with Combined CNN/RNN Model,” "Lect. Notes Comput. Sci. (including Subser. Lect. Notes Artif. Intell. Lect. Notes Bioinformatics), vol. 11329 LNAI", pp. 149–158, 2019.':
        '[4]\tI. Toth et al., “Freelancing on Digital Work Platforms,” VINE J. Inf. Knowl. Manag. Syst., 2020.',
    '[5]':
        '[5]',
    '\tS. Naseer and Y. Saleem, “Enhanced network intrusion detection using deep convolutional neural networks,” "KSII Trans. Internet Inf. Syst., vol. 12, no. 10, pp. 5159–5178, 2018, doi: 10.3837/tiis.2018".10.028.':
        '',
 },
 14: {
    '7. Process and Architecture':
        '9. Process and Architecture',
    '.  Requirements Gathering and Planning:':
        '1. Requirements Gathering and Planning:',
    'Identify your business goals, target audience, and objectives.':
        'Identify hospital goals, patient/staff needs, system objectives.',
    'Determine the scope of the project and create a detailed plan.':
        'Define scope and create a detailed implementation plan.',
    'Define the features and functionality you need on your website.':
        'List features: booking, tokens, triage, dashboards.',
    '2.  Choose Technology Stack:':
        '2.  Choose Technology Stack:',
    'Select the technology stack for your website, including the choice of a programming language (e.g., Python with Django), database system, web server, and any additional frameworks or libraries.':
        'Java 21 Spring Boot, MySQL 8, Python FastAPI/scikit-learn, React 19/TypeScript.',
    '3.  Architectural Design:':
        '3.  Architectural Design:',
    'Create a high-level architecture that outlines the components and how they interact.':
        'Modular monolith backend owns state; stateless ML sidecar predicts wait time.',
 },
 15: {
    '6. Tools and Languages':
        '10. Tools and Languages',
    'To make an e-comerce ':
        'Java 21, Spring Boot, MySQL 8, Flyway, JWT, Maven for the backend.',
    'website we have used  python language and django as a database.':
        'Python 3, FastAPI, scikit-learn for the ML waiting-time sidecar.',
    'and done on vs(virtual studio) code.Also jss and html.':
        'React 19, TypeScript, Vite, Tailwind CSS, TanStack Query for the frontend.',
    ' ':
        'Git, Swagger/OpenAPI, JUnit 5, pytest, Vitest for versioning, docs, testing.',
 },
}

def process_slide_xml(path, rep):
    tree = etree.parse(path)
    changed = 0
    for t in tree.findall('.//{%s}t' % A):
        if t.text in rep:
            new = rep[t.text]
            if new == 'KEEP':
                continue
            t.text = new
            changed += 1
    tree.write(path, xml_declaration=True, encoding='UTF-8', standalone=True)
    return changed

# --- main ---
work = tempfile.mkdtemp(prefix='pptwork_')
with zipfile.ZipFile(SRC) as z:
    z.extractall(work)

total = 0
for i in range(1, 16):
    spath = os.path.join(work, 'ppt', 'slides', f'slide{i}.xml')
    total += process_slide_xml(spath, REP.get(i, {}))

# Rezip into .pptx
if os.path.exists(DST):
    os.remove(DST)
with zipfile.ZipFile(DST, 'w', zipfile.ZIP_DEFLATED) as z:
    for root, _, files in os.walk(work):
        for f in files:
            full = os.path.join(root, f)
            rel = os.path.relpath(full, work)
            z.write(full, rel)

shutil.rmtree(work)
print('SAVED:', DST)
print('Total text replacements:', total)

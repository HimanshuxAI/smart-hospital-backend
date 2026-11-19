================================================================================
SMART HOSPITAL - B.TECH PROJECT REPORT
Format preserved exactly per: Annexure 6_Project report format - guidelines_SKP.docx
MIT SCHOOL OF COMPUTING, MIT ADT UNIVERSITY, LONI KALBHOR, PUNE
================================================================================

FORM: Hard bound blue cover page with golden embossing

--------------------------------------------------------------
(Four blank spaces)
A PROJECT REPORT ON          [14 / bold / upper case]

(One blank space)
SMART HOSPITAL PATIENT QUEUE AND APPOINTMENT MANAGEMENT SYSTEM   [18 / bold / upper case]

(Two blank spaces)
SUBMITTED TO MIT SCHOOL OF COMPUTING, LONI, PUNE IN PARTIAL FULFILLMENT OF THE REQUIREMENTS FOR THE AWARD OF THE DEGREE   [12 / upper case]

(Four blank spaces)
BACHELOR OF TECHNOLOGY   (Computer Science & Engineering / Information Technology)   [14 / bold / upper case]

(Two blank spaces)
BY   [14 / bold / upper case]

(Two blank spaces)
Candidate Name                      PRN / Enrollment No: -----

Uday Mhaske                              ADT24SOCB1735
Aditya Sundalam                          MITU22BTCS0044
Atharv Shewale                           ADT25SOCB1715
Saiprasad Bendre                         ADT25SOCB1651

(Two blank spaces)
Under the guidance of   [One blank space]   [14 / bold / upper case]

DR. P. D. PATIL   [one blank space] [one blank space]

DEPARTMENT OF COMPUTER SCIENCE AND ENGINEERING   [Two blank spaces]

MIT SCHOOL OF COMPUTING   [One blank space]

MIT ART, DESIGN AND TECHNOLOGY UNIVERSITY, RAJBAUG CAMPUS, LONI-KALBHOR, PUNE 412201   [One blank space]

2026 - 27   MIT SCHOOL OF COMPUTING

================================================================================

FORM: CERTIFICATE   [16 / bold / upper case]

(Two blank spaces)
This is to certify that the project report entitled   [12 / sentence case]

(One blank space)
"SMART HOSPITAL PATIENT QUEUE AND APPOINTMENT MANAGEMENT SYSTEM"   [12 / bold / upper case]

(Two blank spaces)
Submitted by   [One blank space]
Name of the Candidate   PRN / Enrollment No: -----   [12 / Title case]

(One blank space)
is a bonafide work carried out by them under the supervision of Prof./Dr/  …..  and it is submitted towards the partial fulfillment of the requirement of MIT ADT University, Pune for the award of the degree of Bachelor of Technology (Computer Science and Engineering)   [12 / Sentence case]

(Four blank spaces)
             Prof. PQR                                                      Dr. xyz               Guide                                                 Head of Department
             Prof. Suresh Kapare                                           Dr. Ganesh Pathak         Chief Coordinator-PBL  Dean
             Seal/Stamp of the College         Place: PUNE         Date:

On Company Letter head / Seal

================================================================================

FORM: CERTIFICATE   [16 / bold / upper case]

(Three blank spaces)
This is to certify that the Project report entitled   [12 / sentence case]

(One blank space)
"SMART HOSPITAL PATIENT QUEUE AND APPOINTMENT MANAGEMENT SYSTEM"   [12 / bold / upper case]

(Two blank spaces)
Submitted by   [One blank space]
Name of the Candidate   Exam No: ---------------------   [12 / Title case]

(One blank space)
is a bonafide work carried out by him/her (with the Sponsorship from ------------) under the supervision of Mr/Ms/Miss. ………………………….. and has been completed successfully.

(Four blank spaces)
             (Mr. ……………… )(Designation)   External Guide   [12 / Sentence case]
                                         Seal/Stamp of the Company / College
                                         Place :         Date :

================================================================================

FORM: DECLARATION   [16 / bold / upper case]

(Three blank spaces)
We, the team members

Name                                  Enrollment No
Uday Mhaske                           ADT24SOCB1735
Aditya Sundalam                       MITU22BTCS0044
Atharv Shewale                        ADT25SOCB1715
Saiprasad Bendre                      ADT25SOCB1651

Hereby declare that the project work incorporated in the present project entitled "SMART HOSPITAL PATIENT QUEUE AND APPOINTMENT MANAGEMENT SYSTEM" is original work. This work (in part or in full) has not been submitted to any University for the award of a Degree or a Diploma. We have properly acknowledged the material collected from secondary sources wherever required. We solely own the responsibility for the originality of the entire content.

Date: 18/11/2026

Name & Signature of the Team Members
Member 1:   __________________      Member 2:   __________________
Member 3:   __________________

Name and Signature of Guide            Seal/Stamp of the College
Place: Pune                            Date: 18/11/2026

================================================================================

FORM: EXAMINER'S APPROVAL CERTIFICATE   [16 / bold / upper case]

(Three blank spaces)
The project report entitled "SMART HOSPITAL PATIENT QUEUE AND APPOINTMENT MANAGEMENT SYSTEM" submitted by Student 1 (ADT24SOCB1735), Student 2 (MITU22BTCS0044), Student 3 (ADT25SOCB1715), Student 4 (ADT25SOCB1651) in partial fulfillment for the award of the degree of Bachelor of Technology (Computer Science & Engineering) during the academic year 2026-27, of MIT-ADT University, MIT School of Computing, Pune, is hereby approved.

Examiners:
1.
2.

================================================================================

FORM: ACKNOWLEDGEMENT   [16 / bold / upper case]

(Three blank spaces)
We thank our guide Dr. P. D. Patil, Department of Computer Science & Engineering, MIT School of Computing, for his continuous support, guidance, and encouragement throughout this project. We are grateful to the faculty and laboratory staff of MIT School of Computing for providing the resources and environment needed to build the Smart Hospital Patient Queue and Appointment Management System. We also thank our parents and friends for their constant support and understanding.

Name, Enrollment No.
Uday Mhaske      ADT24SOCB1735
Aditya Sundalam  MITU22BTCS0044
Atharv Shewale   ADT25SOCB1715
Saiprasad Bendre ADT25SOCB1651

================================================================================

FORM: ABSTRACT   [16 / bold / upper case]

(Three blank spaces)
Hospitals and clinics often handle a large number of patients waiting to meet doctors. Manual queue management can lead to long waiting times, overcrowding, and confusion. Patients may not know their exact position in the queue or how long they need to wait, and emergency patients need to be identified and given priority quickly.

The Smart Hospital Patient Queue and Appointment Management System is a full-stack application designed to reduce patient waiting time and improve queue management in hospitals and clinics. The system allows patients to register, book fixed doctor appointments online, receive digital queue tokens, and track their queue position and estimated waiting time. Emergency patients are assigned priority so they can receive treatment faster. The system also uses data analytics and a machine-learning model to estimate patient waiting time based on historical and current queue data.

The backend is a Java 21 Spring Boot REST API backed by MySQL, with Flyway migrations and JWT-based role-based authentication for patients, doctors, and administrators. A Python FastAPI sidecar runs a scikit-learn regression model for waiting-time prediction, with a deterministic Java fallback when the sidecar is unavailable. The frontend is a responsive React 19 TypeScript application using TanStack Query, React Router, and Tailwind CSS, providing separate patient, doctor, and administrator workspaces.

The proposed system provides an efficient, organized, and user-friendly solution for managing hospital appointments and patient queues, reducing waiting time and overcrowding while giving patients clear information about their queue position and expected waiting time.

================================================================================

FORM: CONTENTS   [16 / bold / upper case]

(Three blank spaces)
Certificate           ……………………………………… i
Certificate (From Company If Any)     …………………… ii
Declaration       ………………………………………… iii
Examiner's Approval Certificate      …………………… iv
Acknowledgement     ……………………………………… v
Abstract    ……………………………………………… vi
List of Figures    ……………………………………… vii
List of Tables   ………………………………………… viii

CONTENTS (TOC)
Chapter 1 INTRODUCTION .................................................... 1
   1.1 Problem Definition ................................................ 1
   1.2 Objectives ........................................................ 2
   1.3 Scope of the Project .............................................. 2
Chapter 2 CONCEPTS AND METHODS ........................................ 3
   2.1 Definitions ....................................................... 3
Chapter 3 LITERATURE SURVEY ........................................... 5
Chapter 4 PROJECT PLAN .................................................. 7
Chapter 5 SOFTWARE REQUIREMENT SPECIFICATION ........................ 9
   5.1 Project Scope ..................................................... 9
   5.2 User Classes and Characteristics ................................ 9
Chapter 6 RESULTS ....................................................... 11
Chapter 7 SOFTWARE TESTING ............................................ 12
Chapter 8 CONCLUSION AND FUTURE WORK ................................ 13
BIBLIOGRAPHY ............................................................ 14
ANNEXURE A: List of Publications and Research Paper ................. 15
ANNEXURE B: Plagiarism Report ......................................... 16
ANNEXURE C: Empathy Chart and Solution Document ..................... 17
Annexure D: Project Tracker ........................................... 18

================================================================================

FORM: LIST OF FIGURES   [16 / bold / upper case]

(Three blank spaces)
Figure Number: Figure of the table                                                                 Page Number
Figure 1.1: System architecture ....................................... 3
Figure 4.1: Waterfall model / project plan ............................ 7
Figure 6.1: Digital token and queue view .............................. 11
Figure 7.1: Test execution summary .................................... 12

================================================================================

FORM: LIST OF TABLES   [16 / bold / upper case]

(Three blank spaces)
Table Number: Title of the table                                                                      Page Number
Table 2.1: Technology stack ............................................. 4
Table 3.1: Comparison of existing queue-management approaches ......... 5
Table 5.1: Functional requirements ...................................... 9
Table 5.2: Non-functional requirements .................................. 10
Table 6.1: Test results summary ......................................... 11

================================================================================

FORM: Footer — "MITSOC, Department of Computer Engineering 2026" [10, Times Roman, Center justified] from Introduction page onward

================================================================================

CHAPTER 1   INTRODUCTION   [18 / bold / all capitals / center aligned]

(One blank space)

1. INTRODUCTION   [12 / bold / capitalized / left aligned]

(One blank space)
  Hospitals and clinics typically serve a large number of patients who need to consult doctors on a given day. When queue management is handled manually, the result is often long waiting times, overcrowded waiting areas, and confusion among patients and staff. Patients frequently do not know their exact position in the queue or how long they will have to wait before seeing the doctor. Emergency cases also need to be identified quickly and given priority so that critical patients receive treatment without delay.

  There is a clear need for a smart system that manages appointments and queues efficiently while giving patients reliable information about their queue position and expected waiting time. The Smart Hospital Patient Queue and Appointment Management System addresses these problems by digitizing the entire patient journey, from registration and appointment booking to token generation, queue tracking, and waiting-time estimation.

1.1 Problem Definition   [12 / bold / capitalized / left aligned]

(One blank space)
  Hospitals commonly face problems such as long patient waiting times, overcrowding, manual queue management, and difficulty handling emergency patients. Patients may have limited information about their queue position and expected waiting time, which increases anxiety and reduces the quality of care. A smart system is needed that manages appointments and queues efficiently while providing waiting-time estimates.

1.2 Objectives   [12 / bold / capitalized / left aligned]

(One blank space)
  The objectives of this project are:

  1. To develop a system for online patient appointment booking.
  2. To generate digital tokens for patients.
  3. To manage and display the patient queue position.
  4. To give priority to emergency patients.
  5. To estimate patient waiting time using data analytics and machine learning.
  6. To reduce patient waiting time and overcrowding.
  7. To provide a simple and user-friendly system for hospitals and clinics.

1.3 Scope of the Project   [12 / bold / capitalized / left aligned]

(One blank space)
  The scope of the project includes:

  Software:
  - Patient registration and authentication
  - Doctor and appointment management
  - Digital token generation
  - Queue management and queue-position tracking
  - Emergency priority handling
  - Waiting-time estimation using machine learning
  - Data storage and analysis
  - Admin and doctor dashboards

  Future scope:
  - Mobile application for patients
  - SMS / app notifications
  - Multiple hospitals and departments
  - Cloud-based hospital management
  - Online consultation integration
  - Digital medical records
  - Voice-based token announcements
  - Real-time hospital dashboard

================================================================================

CHAPTER 2   CONCEPTS AND METHODS   [18 / bold / all capitals / center aligned]

(One blank space)

2.1 Definitions   [12 / bold / capitalized / left aligned]

(One blank space)
  Smart Hospital: A hospital that uses information technology and interconnected systems to manage patient flow, appointments, and clinical resources efficiently.

  Patient Queue: An ordered list of patients waiting to consult a doctor, arranged by check-in time and emergency priority tier.

  Digital Token: A unique per-doctor, per-day number issued to a patient at check-in, indicating the patient's turn in the queue.

  Emergency Triage: The classification of a queue entry into one of four priority tiers — NORMAL, MEDIUM, HIGH, or CRITICAL — so that urgent cases are called before routine ones.

  Waiting-Time Prediction: The estimation, in minutes, of how long a patient will wait before consultation, based on queue position, number of waiting patients, historical consultation duration, emergency load, and time-of-day context.

  Spring Boot: A Java-based framework used to build the REST API backend of the system.

  MySQL: The relational database used to store users, appointments, queue entries, and related hospital data.

  FastAPI: A lightweight Python web framework used for the machine-learning sidecar that predicts waiting time.

  scikit-learn: A Python machine-learning library used to train the waiting-time regression model.

  React: A JavaScript library used to build the responsive frontend user interface.

  TypeScript: A typed superset of JavaScript used for frontend development.

  TanStack Query: A server-state management library used to cache and synchronize API data in the frontend.

  JWT: JSON Web Token, used for stateless authentication and role-based authorization between the frontend and backend.

  Flyway: A database-migration tool used to version and apply schema changes reproducibly.

================================================================================

CHAPTER 3   LITERATURE SURVEY   [18 / bold / all capitals / center aligned]

(One blank space)

(One blank space)
  Existing queue-management and appointment systems were studied to understand common approaches and their limitations. Most traditional hospitals rely on paper-based or manual token systems, where patients take a numbered ticket and wait without visibility into their position or expected wait time. Some digital queue systems display token numbers on screens but do not support appointment booking, emergency prioritization, or waiting-time prediction. A few research systems apply machine learning to forecast waiting times, but they often depend on real patient data that is difficult to obtain for academic projects and raise privacy concerns.

  Related approaches include profile-matching e-marketplaces, crowd-sourced advisor frameworks, online labor-market analysis, gig-service quality prediction, and decision-support systems using profile matching and Simple Additive Weighting. Studies of online freelance marketplaces such as TaskRabbit and Fiverr show that both gender and race bias can affect worker evaluations, highlighting the need for transparent, rule-based prioritization rather than opaque scoring. Research on virtual community trust and person-job fit in digital work platforms further emphasizes that trust and clear rules matter in online matching systems.

  For this project, the key insight is that queue ordering should be explicit, rule-based, and auditable: emergency tier first, then arrival time within each tier, with manual overrides recorded as audit entries. Waiting-time estimation uses a transparent baseline model trained on explicitly synthetic demonstration data, so no real patient data is represented as live hospital data.

Table 3.1: Literature Survey   [Table at beginning or end of page]

================================================================================

BIBLIOGRAPHY   [References in IEEE format, in the order they appear]

(One blank space)
[1] R. Naik and S. K. Pathan, "Indian Monsoon Rainfall Classification and Prediction Using Robust Back Propagation Artificial Neural Network," vol. 3, no. 11, pp. 99–101, 2013.

[2] P. Udhawani and S. K. Pathan, "Dynamic Link Adaptation and Routing Protocols in Wireless Sensor Networks based on Energy Harvesting of MAC Protocols," Int. J. Comput. Appl., vol. 99, no. 15, pp. 22–28, 2014, doi: 10.5120/17449-8323.

[3] B. M. Kore and S. K. Pathan, "A Delay and Energy Aware Technique for Wireless Sensor Networks using Regular Sleep and Wake Periods," Int. J. Comput. Appl., vol. 69, no. 6, pp. 1–6, 2013, doi: 10.5120/11843-7592.

[4] S. Handrale and P. S. K. Pathan, "Location Based and Energy Efficient Anonymous Routing Protocol for MANET," vol. 5, no. 6, pp. 7789–7794, 2014.

[5] V. S. S. Sriram, G. Sahoo, and K. K. Agrawal, "Detecting and Eliminating Rogue Access Points in IEEE-802.11 WLAN — A Multi-Agent Sourcing Methodology," 2010 IEEE 2nd Int. Adv. Comput. Conf. IACC 2010, pp. 256–260, 2010, doi: 10.1109/IADCC.2010.5422999.

[6] S. K. Pathan, S. N. Deshmukh, and R. R. Deshmukh, "Kerberos Authentication System — A Public Key Extension," Int. J. Recent Trends Eng., vol. 1, no. 2, pp. 15–18, 2009.

[7] A. Hannak, C. Wagner, D. Garcia, A. Mislove, M. Strohmaier, and C. Wilson, "Bias in Online Freelance Marketplaces: Evidence from TaskRabbit and Fiverr," Proc. ACM Hum.-Comput. Interact., vol. 1, pp. 1914–1933, 2017, doi: 10.1145/2998181.2998327.

[8] I. Toth, S. Heinänen, and K. Blomqvist, "Freelancing on Digital Work Platforms — Roles of Virtual Community Trust and Work Engagement and Person-Job Fit," VINE Journal of Information and Knowledge Management Systems, ahead of print, 2020, doi: 10.1108/VJIKMS-12-2018-0124.

================================================================================

CHAPTER 4   PROJECT PLAN   [18 / bold / all capitals / center aligned]

(One blank space)

(One blank space)
  The system was developed using the waterfall model, in which each phase is completed before the next begins. The phases are:

  Phase 1 — System Design: Identify requirements, design the database, design the user interface, and design the queue-management workflow.

  Phase 2 — Application Development: Implement patient registration, appointment booking, token generation, queue management, and the doctor dashboard.

  Phase 3 — Data Analytics and ML: Collect appointment and waiting-time data, analyze previous patient data, develop a waiting-time prediction model, and test prediction accuracy.

  Phase 4 — Testing: Test appointment booking, token generation, emergency priority, queue updates, and waiting-time prediction.

  Phase 5 — Final Integration: Integrate the application, database, queue management, and ML prediction into one complete system.

  Figure 4.1: Software modeling / Waterfall model   [Figure at beginning or end of page]

  Based on this model, the required estimates have been stated in Annexure D (Project Tracker). Each phase was considered separately and the required effort was mapped to the steps in the waterfall model.

================================================================================

CHAPTER 5   SOFTWARE REQUIREMENT SPECIFICATION   [18 / bold / all capitals / center aligned]

(One blank space)

5.1 Project Scope   [12 / bold / capitalized / left aligned]

(One blank space)
  The proposed system provides a centralized platform for managing hospital appointments and patient queues. The main workflow is:

  Patient Registration → Appointment Booking → Digital Token → Queue Management → Emergency Priority → Waiting-Time Prediction → Doctor Consultation → Queue Update

  The system allows patients to book appointments, receive digital tokens, and track their position in the queue. Emergency patients are given priority so that they can receive treatment quickly. Data analytics and machine learning estimate patient waiting time based on previous and current data.

5.2 User Classes and Characteristics   [12 / bold / capitalized / left aligned]

(One blank space)
  Patient:
  - Register and log in to the application
  - Browse departments, doctors, and available appointment slots
  - Book and cancel appointments
  - Check in to an eligible appointment and receive a digital token
  - View queue position, triage level, and waiting-time estimate
  - View and update personal profile

  Doctor:
  - View own schedule, appointments, and queue
  - Check patients in at the point of care
  - Classify emergency priority and manage queue entry state
  - Call the next patient, start consultation, complete, skip, or cancel an entry
  - Override manual queue order with a mandatory reason
  - View and update own profile

  Administrator:
  - Manage departments and doctor accounts
  - Create and manage schedules for all doctors
  - View and manage all appointments and queues
  - Activate or deactivate users
  - View audit records and aggregate dashboard data

================================================================================

CHAPTER 6   RESULTS   [18 / bold / all capitals / center aligned]

(One blank space)

(One blank space)
  The implemented Smart Hospital Patient Queue and Appointment Management System delivers the following results:

  - Patients can register, log in, browse departments and doctors, book fixed appointment slots, cancel appointments, check in, and receive a unique digital queue token with a queue position.
  - The queue orders entries by emergency tier (CRITICAL, HIGH, MEDIUM, NORMAL) and then by arrival time within each tier, so emergency patients are seen first.
  - Doctors can view today's schedule, manage the live queue, classify emergency priority, call/serve/complete/skip/cancel entries, and record audited manual overrides.
  - Administrators can manage users, departments, doctors, schedules, appointments, and queues, and view aggregate dashboard metrics.
  - Waiting-time prediction is provided by a Python FastAPI scikit-learn model, with a deterministic Java fallback estimate when the ML sidecar is unavailable. Every response states the prediction source (ML or FALLBACK) and confidence.
  - Role-based JWT authentication secures all endpoints, with short-lived access tokens and rotatable refresh tokens.
  - OpenAPI documentation and Swagger UI expose the API contract at /v3/api-docs and /swagger-ui.html.
  - Automated backend (JUnit 5), Python (pytest), and frontend (Vitest) tests pass, and strict TypeScript typechecking and the production frontend build succeed within the bundle budget.

  Figure 6.1: Digital token and queue view   [Figure at beginning or end of page]

  Table 6.1: Test results summary

  | Test Area               | Status |
  |-------------------------|--------|
  | Backend unit/integration| Pass   |
  | Python ML service       | Pass   |
  | Frontend unit/route     | Pass   |
  | TypeScript typecheck    | Pass   |
  | Production build        | Pass   |
  | Bundle-size budget      | Pass   |

================================================================================

CHAPTER 7   SOFTWARE TESTING   [18 / bold / all capitals / center aligned]

(One blank space)

(One blank space)
  Testing was carried out across all three parts of the system:

  Backend (Java, JUnit 5, Mockito, Spring Boot tests, Testcontainers MySQL):
  - Unit tests for domain rules, queue ordering, authorization decisions, fallback estimation, and state transitions.
  - MVC/API tests for validation, status codes, RFC 9457 problem details, and role restrictions.
  - Integration tests against MySQL for booking concurrency, check-in/token uniqueness, and queue transitions.
  - Full application-context test for Flyway migrations and health endpoints.

  Python ML service (pytest):
  - Unit tests for preprocessing and model prediction.
  - API tests for validation and response schema.
  - Smoke test proving a serialized model can be trained, loaded, and used.

  Frontend (Vitest, React Testing Library, jsdom):
  - Problem-detail parsing, field errors, malformed JSON, and generic fallback handling.
  - Token-store behavior: access token in memory only, refresh token in sessionStorage.
  - Login, logout, session restoration, and refresh failure cleanup.
  - One serialized refresh for concurrent 401 responses and one retry of the original request.
  - Role-aware route access for patient, doctor, and administrator.
  - Queue priority ordering and FIFO within each priority tier.
  - Accessible-button, dialog keyboard, form error association, and guard behavior.
  - Route tests verifying redirects to login for anonymous users and role-based access.
  - Production build and bundle-budget check (initial JavaScript under 200 KB gzip).

  End-to-end verification covers registration, login, booking, check-in, queue token retrieval, emergency classification, doctor queue progression, and waiting-time response with ML fallback.

  Figure 7.1: Test execution summary   [Figure at beginning or end of page]

================================================================================

CHAPTER 8   CONCLUSION AND FUTURE WORK   [18 / bold / all capitals / center aligned]

(One blank space)

(One blank space)
  The Smart Hospital Patient Queue and Appointment Management System successfully digitizes hospital appointment and queue management. Patients can book appointments, receive digital tokens, and track their queue position and estimated waiting time. Emergency patients are prioritized, and hospital staff have auditable control over queue progression. The system reduces waiting time and overcrowding while giving patients clear information about their place in the queue.

  Future work is likely to include:
  - A mobile application for patients
  - SMS and push notifications
  - Advanced ML-based waiting-time prediction
  - Cloud database and multiple-hospital integration
  - Online doctor consultation
  - Digital medical records
  - Voice-based token announcements
  - Real-time hospital dashboard

================================================================================

ANNEXURE A: List of Publications and Research Paper   (In its original formats)

================================================================================

ANNEXURE B: Plagiarism Report

================================================================================

ANNEXURE C: Empathy Chart and Solution Document

================================================================================

Annexure D: Project Tracker

================================================================================



# Smart Hospital Frontend Design

**Date:** 2026-11-15
**Status:** Approved for implementation by project owner
**Scope:** Lightweight, responsive patient, doctor, and administrator MVP
**Foundation delivery boundary:** This implementation unit establishes the application, design system, authentication/API boundaries, role guards, dashboard route shells, deterministic patient demo, and verification gates. Complete booking, directory, queue-management, profile, schedule, and administrator workflows remain separate feature work after this foundation is accepted.

## 1. Purpose and principles

The frontend is the browser-facing application for the Smart Hospital appointment and queue system. It gives patients a clear path from discovery to booking, check-in, and live queue status; gives doctors a focused workspace for today's schedule and queue progression; and gives administrators a compact management overview.

The design follows four principles:

1. **Clinical clarity over decoration.** Information hierarchy, restrained color, readable typography, and explicit status language take precedence over visual effects.
2. **Server state stays on the server.** TanStack Query owns remote data and cache invalidation. Local React state owns only ephemeral UI concerns such as an open menu or a form draft.
3. **Security is explicit.** Access tokens live in memory, refresh tokens live in `sessionStorage`, protected data is never rendered before authorization succeeds, and tokens or patient details are never logged.
4. **Every state is designed.** Loading, empty, error, permission, stale-data, success, and destructive-confirmation states have deliberate UI treatments rather than generic spinners or blank panels.

The application uses only browser-provided assets: a system font stack, local CSS, and bundled code. It does not fetch fonts, icons, or scripts from a CDN.

## 2. Technology and build architecture

| Concern | Decision |
|---|---|
| UI runtime | React 19.3.1 |
| Language | TypeScript 7.0.2 in strict mode |
| Build and dev server | Vite 8.3.8 |
| Styling | Tailwind CSS 4.3.5 plus CSS custom-property design tokens |
| Routing | React Router 7.18.5 with route-level `lazy()` modules |
| Server state | TanStack Query 5.107.1 |
| Icons | Lucide React 1.73.0, bundled locally |
| Tests | Vitest 5.1.0, React Testing Library, `user-event`, jsdom |
| Package management | npm with a committed `package-lock.json` |
| Real API base | `VITE_API_BASE_URL`, default `/api/v1` |
| API mode | `VITE_API_MODE=demo` opts into the deterministic adapter; all other values use real HTTP |

There is no Redux store, chart library, component framework, external font, or utility framework. `package.json` pins exact production and test dependency versions.

### 2.1 Source layout

```text
frontend/src/
├── app/                    App shell, providers, router, error boundary
├── api/                    HTTP client, RFC 9457 parsing, endpoint contracts
├── auth/                   Session provider, guards, role-aware redirects
├── components/
│   ├── ui/                 Accessible primitives
│   └── layout/             Responsive navigation and page framing
├── features/               Role and workflow feature modules
├── lib/                    Formatting, validation, constants, query keys
├── mocks/                  Deterministic demo adapter and fictional data
├── test/                   Shared test setup and render helpers
├── main.tsx
└── styles.css
```

Feature modules own their pages, hooks, and feature-specific components. Shared screens reuse primitives from `components/ui` and framing from `components/layout`; they do not import another role's private components.

### 2.2 Runtime layers

1. `main.tsx` mounts `AppProviders` into `#root`.
2. `AppProviders` creates the QueryClient, auth session provider, toast provider, and browser router.
3. The router loads route modules lazily. Public, patient, doctor, and administrator trees are split into separate chunks.
4. `RequireAuth` resolves session restoration before rendering a protected route.
5. `RequireRole` checks the restored role and renders the route, redirects to the user's own dashboard, or renders the accessible 403 page.
6. Feature hooks call typed endpoint functions. The endpoint boundary delegates to either the real HTTP client or the explicitly selected demo adapter.

Vite `manualChunks` may place React, React Router, and TanStack Query into stable vendor chunks. The route chunks themselves remain independently lazy-loadable. The bundle script sums the entry's preloaded JavaScript rather than all lazy route chunks.

## 3. Information architecture and route map

### 3.1 Public routes

| Route | Experience |
|---|---|
| `/login` | Email/password sign-in, inline validation, demo-mode disclosure |
| `/register` | Patient self-registration, password guidance, validation summary |
| `/forbidden` | Route-level 403 with a safe return action |
| `*` | Route-level 404 with primary navigation links |
| `/` | Redirect to `/login` when anonymous or to the role dashboard when authenticated |

### 3.2 Patient routes

| Route | Experience |
|---|---|
| `/patient` | Upcoming appointments, active queue token, next appointment summary |
| `/patient/departments` | Searchable department directory |
| `/patient/doctors` | Doctor directory with department and specialty filters |
| `/patient/doctors/:doctorId` | Doctor details, specialty, consultation length, and slots |
| `/patient/doctors/:doctorId/book` | Slot picker and booking form |
| `/patient/appointments` | Upcoming and past appointment views |
| `/patient/appointments/:appointmentId` | Appointment details, cancel action, and check-in action |
| `/patient/queue/:entryId` | Digital token, queue position, triage, and wait estimate |
| `/patient/profile` | Personal and contact profile forms |

### 3.3 Doctor routes

| Route | Experience |
|---|---|
| `/doctor` | Today summary, current consultation, and queue alerts |
| `/doctor/schedule` | Today's and upcoming schedule by time |
| `/doctor/appointments` | Assigned appointment list with date filter |
| `/doctor/queue` | Live queue board with priority ordering and transitions |
| `/doctor/profile` | Doctor-owned profile and availability summary |

### 3.4 Administrator routes

| Route | Experience |
|---|---|
| `/admin` | Operational overview, trends as simple CSS metrics, and attention items |
| `/admin/users` | User table and activation controls |
| `/admin/doctors` | Doctor directory and profile management |
| `/admin/doctors/new` | Doctor creation form |
| `/admin/departments` | Department management |
| `/admin/schedules` | Doctor schedule and slot management |
| `/admin/appointments` | Appointment oversight and filters |
| `/admin/queues` | Queue oversight and priority inspection |

The administrator area uses an indigo accent within the same navy/teal token system. The patient area emphasizes teal actions, the doctor area emphasizes queue-safe amber and teal actions, and the administrator area uses dense data tables without becoming visually heavy. Each workspace has its own icon and concise role label so switching context is obvious.

### 3.5 Route behavior

- Unknown routes render the 404 page without requesting protected data.
- An anonymous user requesting a protected route is redirected to `/login` with a sanitized `from` location.
- An authenticated user requesting another role's route is redirected to their own dashboard.
- A signed-in user opening `/login` or `/register` is redirected to their own dashboard.
- A session restoration failure clears local tokens and returns to `/login` with a non-sensitive status message.
- Browser titles use the pattern `Page | Smart Hospital`.

## 4. UX design system

### 4.1 Visual language

The interface uses a polished light clinical palette:

- **Ink:** deep navy `#10233f` for headings and primary text.
- **Primary:** clinical teal `#0f766e` with a darker `#115e59` hover state.
- **Secondary surface:** cool slate `#f1f5f9` and white `#ffffff`.
- **Medium priority:** amber `#b45309` on `#fffbeb`.
- **Critical priority:** rose `#be123c` on `#fff1f2`.
- **Success:** green `#15803d` on `#f0fdf4`.
- **Borders:** `#dbe3ec`.

Text and controls use at least 4.5:1 contrast for normal text and 3:1 for large text and meaningful graphical controls. Teal is never the only status signal; every priority also has text and an icon or shape treatment.

### 4.2 Tokens

CSS variables in `styles.css` define:

```text
--color-ink, --color-muted, --color-primary, --color-primary-strong
--color-surface, --color-surface-subtle, --color-border
--color-success, --color-warning, --color-danger, --color-info
--space-1 through --space-12 based on an 8px rhythm
--radius-sm, --radius-md, --radius-lg, --radius-full
--shadow-card, --shadow-overlay
--font-sans, --duration-fast, --duration-normal
```

The spacing rhythm is primarily 8px, with 4px allowed for compact icon gaps and 24px/32px/48px for section rhythm. Shadows are limited to card elevation and overlays. Gradients are not used as a dominant background treatment.

### 4.3 Typography

The font stack is:

```text
ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI",
sans-serif
```

Page titles use 24–30px and 700 weight. Section titles use 18–20px and 650–700 weight. Body text is 15–16px with at least 1.5 line height. Labels use 13–14px and 600 weight. Numeric queue and token values use tabular numerals.

### 4.4 Core primitives

`components/ui` contains the following accessible, reusable components:

- **Button:** native button semantics, visible variants, loading state that preserves its width, minimum 44px touch target, and optional icon.
- **Input, Select, Textarea:** labels, descriptions, required/optional text, error IDs, `aria-invalid`, and `aria-describedby` association.
- **Card:** semantic article/region option, optional heading and actions, consistent padding.
- **Badge:** neutral, success, warning, critical, and info variants with text labels.
- **Dialog:** focus containment, Escape close, labeled title/description, and focus restoration.
- **Drawer:** mobile navigation overlay with dialog semantics and focus restoration.
- **DropdownMenu:** keyboard navigation, Escape close, outside-click close, and menu roles.
- **Toast:** polite live region for success, error, and progress feedback.
- **Skeleton:** non-announced loading block that reserves final layout dimensions.
- **EmptyState:** icon, concise explanation, and one relevant action.
- **ErrorState:** safe error summary and retry action.
- **Pagination:** labelled previous/next controls and page status.
- **Tabs:** tablist semantics with arrow-key navigation and associated panels.
- **Avatar:** initials fallback with an accessible user label.

### 4.5 Responsive behavior

- At widths of 1024px and above, the application uses a fixed 248px desktop sidebar, a top bar, and a constrained content column.
- Below 1024px, the sidebar becomes a left drawer opened by a labelled top-bar button; a compact bottom navigation exposes the four most-used current-role destinations.
- Tables that exceed the available width scroll inside a labelled region. At narrow widths, primary record views switch to stacked cards where semantic table markup would impair comprehension.
- Forms use one column below 640px and one or two columns above that breakpoint.
- Touch targets are at least 44 by 44 pixels. Modals use the viewport with 16px edge padding.

## 5. Authentication and token lifecycle

### 5.1 Session model

```text
type AuthUser = {
  id: number;
  email: string;
  fullName: string;
  role: 'PATIENT' | 'DOCTOR' | 'ADMIN';
  active: boolean;
  patientProfile?: PatientProfileSummary;
  doctorProfile?: DoctorProfileSummary;
};
```

The provider owns the current user and an in-memory access token. The refresh token is stored under the versioned key `smarthospital.refresh-token` in `sessionStorage`, so it is removed when the browser tab session ends.

### 5.2 Login and registration

`POST /api/v1/auth/login` and `POST /api/v1/auth/register` return the backend's token bundle:

```json
{
  "accessToken": "…",
  "refreshToken": "…",
  "tokenType": "Bearer",
  "expiresIn": 900,
  "user": {"id": 1, "email": "patient@example.com", "fullName": "A Patient", "role": "PATIENT"}
}
```

The provider stores the access token only in memory, stores the refresh token in `sessionStorage`, and exposes the user through context. Registration follows the same lifecycle and routes a patient to `/patient`.

### 5.3 Session restoration

On startup, if a refresh token exists, the provider performs exactly one `POST /api/v1/auth/refresh` request. A successful response restores the in-memory access token and user. Failure removes the refresh token and resolves to an anonymous session. The provider never exposes a partially restored session to protected routes.

### 5.4 Refresh and retry

For an authenticated HTTP request receiving 401:

1. The client asks the session coordinator to refresh.
2. Concurrent 401 responses share one in-flight refresh promise.
3. The refresh request uses the refresh token directly and bypasses the authenticated request path.
4. If refresh succeeds, the original request is retried exactly once with the new access token.
5. If refresh fails, both tokens are cleared and the user is sent to login.
6. A retried request that receives another 401 is returned as an error and is never refreshed again.

No token, authorization header, form value, patient detail, or response body containing personal data is written to `console`, local storage, analytics, or error-reporting payloads.

### 5.5 Logout

`POST /api/v1/auth/logout` sends the refresh token, clears the in-memory access token and user on completion or failure, removes `sessionStorage`, invalidates protected queries, and redirects to `/login`.

## 6. API boundary

### 6.1 Real HTTP mode

All requests use the `/api/v1` base. The browser client:

- sends JSON `Content-Type` only for bodies;
- sends `Accept: application/json, application/problem+json`;
- sends `Authorization: Bearer <access token>` only when a token exists;
- uses `credentials: 'omit'` because authentication is bearer-based;
- applies an AbortSignal supplied by TanStack Query;
- parses successful JSON and handles an empty 204 response;
- never mutates request behavior for demo mode.

### 6.2 RFC 9457 errors

`ApiProblem` contains:

```text
status: number
code: string
title: string
detail: string
path?: string
timestamp?: string
fieldErrors: Record<string, string>
retryAfterSeconds?: number
```

The parser accepts `application/problem+json`, also tolerates a JSON problem body served with a generic content type, and falls back to a stable code such as `HTTP_500` for malformed responses. Field errors are normalized to a map so forms can associate messages with their controls.

Status handling:

| Status | Client behavior |
|---|---|
| 400 | Show field or request-level validation error |
| 401 | Perform one serialized refresh and retry once |
| 403 | Render permission state without leaking hidden data |
| 404 | Render route or resource not-found state |
| 409 | Explain the state conflict and invalidate affected queries |
| 429 | Show a retry message and honor `Retry-After` when present |
| 5xx/network | Show a safe generic error with retry action |

### 6.3 Defined backend routes

The client uses the defined `/api/v1` routes:

```text
POST   /auth/register
POST   /auth/login
POST   /auth/refresh
POST   /auth/logout
GET    /auth/me
GET    /departments
GET    /doctors
GET    /doctors/{doctorId}
GET    /doctors/{doctorId}/slots?date=YYYY-MM-DD
GET    /appointments
GET    /appointments/{id}
POST   /appointments
POST   /appointments/{id}/cancel
POST   /appointments/{id}/check-in
GET    /queue
GET    /queue/{entryId}
GET    /queue/{entryId}/waiting-time
POST   /queue/{entryId}/classify
POST   /queue/{entryId}/call
POST   /queue/{entryId}/serve
POST   /queue/{entryId}/complete
POST   /queue/{entryId}/skip
POST   /queue/{entryId}/cancel
POST   /queue/{entryId}/override
GET    /dashboard/patient
GET    /dashboard/doctor
GET    /dashboard/admin
```

The currently implemented backend exposes `/api/v1/doctors/profile` and `/api/v1/doctors/{doctorId}/profile`; the public directory contract remains `GET /api/v1/doctors/{doctorId}` as defined in the approved backend design. This integration difference is called out in the final handoff.

### 6.4 Administrator contracts

The approved backend design names administrator capabilities but does not define every administrator URL. The typed client therefore uses explicit provisional contracts:

```text
GET    /admin/users
PATCH  /admin/users/{id}/activation
POST   /admin/doctors
GET    /admin/doctors/{doctorId}/profile
PUT    /admin/doctors/{doctorId}/profile
GET    /admin/departments
POST   /admin/departments
PUT    /admin/departments/{departmentId}
DELETE /admin/departments/{departmentId}
GET    /admin/schedules
POST   /admin/schedules
DELETE /admin/schedules/{slotId}
GET    /admin/appointments
GET    /admin/queues
```

`frontend/src/api/endpoints.ts` marks this group `PROVISIONAL_ADMIN_CONTRACT` with the expected shapes. These paths are not presented as finalized backend behavior. The demo adapter implements them; the real adapter calls the typed route unchanged so integration can align the server without weakening client validation.

### 6.5 Endpoint organization

- `api/types.ts` contains response and request domain types.
- `api/problem.ts` parses and normalizes failures.
- `api/client.ts` owns HTTP mechanics, token injection, refresh coordination, and retry.
- `api/endpoints.ts` groups typed endpoint functions by domain.
- `api/adapter.ts` selects real HTTP or demo adapter at module initialization.
- `mocks/adapter.ts` implements the same endpoint contract with deterministic fictional data.
- `mocks/data.ts` contains stable in-memory records and fixed dates relative to the current day.

## 7. Demo mode

Demo mode is opt-in through `VITE_API_MODE=demo`. Real HTTP is used when the variable is absent, empty, or set to `http`; a production build therefore defaults to HTTP mode.

When demo mode is active:

- a persistent, visibly labeled `Demo data` badge appears in the top bar;
- login accepts the fictional patient, doctor, and administrator accounts shown on the login screen;
- mutations update deterministic in-memory data for the current browser tab;
- no request is sent to the backend;
- the UI does not describe demo records as live hospital data.

The mode boundary is explicit in `api/adapter.ts`; feature code never branches on demo mode.

## 8. Feature behavior

### 8.1 Patient

- Dashboard cards show the next appointment, current queue position, and a concise action to reach the digital token.
- Department and doctor directory searches are client-side over loaded server data and expose result counts.
- Booking requires a selected future slot and nonblank visit reason. Slot submission is disabled during mutation. A 409 explains that the slot was taken and refreshes availability.
- Appointment cancellation and check-in open a confirmation dialog before sending the request.
- The digital token view presents token number prominently, position in plain language, triage badge, wait estimate, and last-updated time.
- Profile forms display field-level server errors and save a success toast.

### 8.2 Doctor

- Dashboard shows today's appointment count, active queue count, and the entry currently in consultation.
- Queue rows are ordered `CRITICAL`, `HIGH`, `MEDIUM`, `NORMAL`, then oldest arrival first.
- Queue actions are constrained by status: call a waiting entry, serve a called entry, complete a served entry, and skip or cancel with confirmation.
- A queue mutation updates the row optimistically with a pending state, rolls back on failure, and invalidates the queue and dashboard queries.
- Triage classification uses a labeled select. A manual override requires a reason and presents the audited action clearly.
- Emergency action uses rose styling and text; no destructive or priority action relies on color alone.

### 8.3 Administrator

- Dashboard uses compact metric cards rather than a chart dependency.
- User activation uses a confirmation dialog for deactivation.
- Doctor creation and profile editing use associated field errors.
- Department and schedule tables use responsive table-to-card behavior.
- Appointment and queue overviews support date and status filters, with explicit empty results.

## 9. Loading, empty, error, and permission states

- **Initial page loading:** a route-level skeleton matching the final page layout, marked `aria-hidden` while a textual status is available to assistive technology.
- **Background refresh:** retain existing data and show a small busy indicator near the affected section.
- **Mutation:** disable only the affected action, preserve its label, and announce progress in a polite live region.
- **Empty collection:** explain why it is empty and offer the one action that can change it.
- **Load error:** show a safe summary, a retry button, and no raw response text.
- **Permission denied:** show a 403 state explaining that the signed-in role cannot open that area.
- **Not found:** distinguish a missing route from a missing record and provide a relevant dashboard link.
- **Offline/network:** say the request could not reach the hospital service and preserve user-entered form values.
- **Unauthorized after refresh:** clear sensitive session state and return to login.

## 10. Accessibility

The application targets WCAG 2.2 AA:

- one `header`, one `nav`, one `main`, and landmark-labeled complementary regions;
- skip link to the main content;
- logical heading hierarchy;
- keyboard access for all actions, menus, tabs, and dialogs;
- visible focus with at least 2px outline and adequate offset;
- form labels always visible, errors programmatically associated, and invalid fields marked with `aria-invalid`;
- live regions for toast, mutation, and session status;
- status communicated by text and icon in addition to color;
- touch targets at least 44 by 44 pixels;
- reduced-motion media query removing nonessential transitions;
- dialogs trapping focus, closing with Escape where safe, and restoring focus to the trigger;
- tables with captions or accessible names and header associations;
- no reliance on hover for essential information;
- images and decorative icons given correct alternative or hidden semantics.

Automated Testing Library checks cover accessible names, keyboard operation, error association, and guard behavior. Browser inspection covers responsive layout at 1440px and 390px widths.

## 11. Testing strategy

### 11.1 Unit and service tests

- Problem-detail parsing for defined fields, field errors, malformed JSON, and generic fallback.
- Token storage: access token absent from `sessionStorage` and `localStorage`; refresh token present only in `sessionStorage`.
- Login, logout, startup restoration, and refresh failure cleanup.
- One refresh for concurrent 401 responses and one retry of the original request.
- No second retry after a retried request receives 401.
- Role-aware route access for patient, doctor, and administrator.
- Queue comparator order for all priorities and FIFO inside a priority.
- Formatting and validation helpers.

### 11.2 Component tests

- Accessible Button loading and disabled behavior.
- Input error association and invalid state.
- Dialog keyboard close and focus restoration.
- Queue priority badges and permitted action labels.
- Patient, doctor, and administrator dashboard access.
- Confirmation before cancellation, activation change, and destructive queue action.
- Mobile navigation opens and closes with accessible controls.

### 11.3 Route tests

The test router renders the actual route configuration with mocked session state. It verifies:

- anonymous access to `/patient`, `/doctor`, and `/admin` redirects to login;
- a patient cannot open doctor or administrator routes;
- a doctor cannot open patient or administrator routes;
- an administrator cannot open patient or doctor routes;
- each role reaches its own dashboard;
- unknown routes render 404.

### 11.4 Commands

```bash
cd frontend
npm test -- --run
npm run typecheck
npm run build
npm run check:bundle-size
```

## 12. Performance budgets

| Metric | Budget |
|---|---|
| Initial route JavaScript, gzip | Less than 200 KB |
| Largest lazy route JavaScript chunk, gzip | Less than 120 KB |
| Production CSS, gzip | Less than 60 KB |
| Web font requests | 0 |
| Runtime chart dependency | 0 |
| Route-level lazy chunks | Public, patient, doctor, and administrator trees |

`frontend/scripts/check-bundle-size.mjs` parses Vite's `manifest.json`, identifies the entry chunk and its static imports, sums their gzip byte lengths, and exits nonzero when the initial JavaScript exceeds 200 KB. It also reports the largest lazy route chunk and fails above 120 KB. The check runs after `vite build` as a standalone local verification step.

Performance tactics include route-level lazy loading, no external assets, system fonts, CSS instead of charting, stable query caching, abortable requests, pagination for administration tables, and narrow TanStack Query cache defaults.

## 13. Release acceptance

The frontend is complete when:

- the documented routes exist and role guards protect them;
- the token lifecycle meets the memory/session-storage and single-refresh requirements;
- RFC 9457 errors reach fields and page states without exposing raw data;
- patient, doctor, and administrator workflows include complete loading, empty, error, success, and permission states;
- destructive and priority-changing actions are confirmed where appropriate;
- critical interactions pass accessible-name and keyboard tests;
- strict TypeScript, all Vitest tests, the production build, and the bundle script pass;
- initial JavaScript is below 200 KB gzip;
- desktop and mobile layouts have been inspected;
- no deployment automation, hosted workflow definitions, external fonts, tokens, or patient data are committed;
- `git diff --check` is clean.

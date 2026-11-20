# Smart Hospital Frontend Foundation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a responsive, accessible React foundation with secure token handling, typed API boundaries, role-aware route shells, authentication screens, and a deterministic patient dashboard preview.

**Architecture:** A Vite application composes independent route-level feature modules around providers for React Router, TanStack Query, authentication, and toast feedback. A typed API client owns RFC 9457 failures and a single serialized refresh/retry path; the real HTTP client is the default, while an explicitly selected demo adapter implements the same endpoint contract. CSS custom properties and Tailwind utilities provide the clinical design system without external assets or a component framework.

**Tech Stack:** React 19.3.1, TypeScript 7.0.2, Vite 8.3.8, Tailwind CSS 4.3.5, React Router 7.18.5, TanStack Query 5.107.1, Lucide React 1.73.0, Vitest 5.1.0, React Testing Library 16.3.3, jsdom 31.0.1.

---

## Scope boundary

This plan delivers the frontend foundation and dashboard shells. It intentionally does not implement the complete booking, directory, queue-management, profile, schedule, or administrator workflows listed in the design specification. Those remain route-ready feature areas for subsequent plans.

The foundation still includes the complete shared architecture required by those workflows:

- role-aware navigation and guards for `PATIENT`, `DOCTOR`, and `ADMIN`;
- secure access/refresh token lifecycle;
- typed HTTP and endpoint boundaries;
- accessible reusable UI primitives;
- loading, empty, error, forbidden, and not-found states;
- opt-in deterministic demo adapter with visible labeling;
- route-level lazy loading and a 200 KB initial-JavaScript gzip budget.

## File map

### Project configuration

```text
frontend/package.json
frontend/package-lock.json
frontend/index.html
frontend/tsconfig.json
frontend/vite.config.ts
frontend/vitest.config.ts
frontend/scripts/check-bundle-size.mjs
frontend/.env.example
```

### Application and API boundary

```text
frontend/src/main.tsx
frontend/src/styles.css
frontend/src/app/App.tsx
frontend/src/app/providers.tsx
frontend/src/app/router.tsx
frontend/src/app/error-boundary.tsx
frontend/src/api/types.ts
frontend/src/api/problem.ts
frontend/src/api/problem.test.ts
frontend/src/api/token-store.ts
frontend/src/api/token-store.test.ts
frontend/src/api/client.ts
frontend/src/api/client.test.ts
frontend/src/api/endpoints.ts
frontend/src/api/adapter.ts
```

### Authentication and routing

```text
frontend/src/auth/AuthProvider.tsx
frontend/src/auth/AuthProvider.test.tsx
frontend/src/auth/auth-context.ts
frontend/src/auth/guards.tsx
frontend/src/auth/guards.test.tsx
frontend/src/auth/session-coordinator.ts
frontend/src/app/router-smoke.test.tsx
frontend/src/app/bundle-budget.test.ts
```

### UI and layout

```text
frontend/src/components/ui/button.tsx
frontend/src/components/ui/field.tsx
frontend/src/components/ui/card.tsx
frontend/src/components/ui/badge.tsx
frontend/src/components/ui/dialog.tsx
frontend/src/components/ui/drawer.tsx
frontend/src/components/ui/menu.tsx
frontend/src/components/ui/toast.tsx
frontend/src/components/ui/feedback.tsx
frontend/src/components/ui/pagination.tsx
frontend/src/components/ui/tabs.tsx
frontend/src/components/ui/avatar.tsx
frontend/src/components/ui/index.ts
frontend/src/components/ui/ui.test.tsx
frontend/src/components/layout/AppShell.tsx
frontend/src/components/layout/Sidebar.tsx
frontend/src/components/layout/TopBar.tsx
frontend/src/components/layout/MobileNavigation.tsx
frontend/src/components/layout/PageHeader.tsx
```

### Features, helpers, mocks, and test support

```text
frontend/src/features/auth/LoginPage.tsx
frontend/src/features/auth/RegisterPage.tsx
frontend/src/features/auth/auth-forms.test.tsx
frontend/src/features/patient/PatientDashboardPage.tsx
frontend/src/features/doctor/DoctorDashboardPage.tsx
frontend/src/features/admin/AdminDashboardPage.tsx
frontend/src/features/queue/PriorityBadge.tsx
frontend/src/features/queue/PriorityBadge.test.tsx
frontend/src/features/shared/ForbiddenPage.tsx
frontend/src/features/shared/NotFoundPage.tsx
frontend/src/lib/constants.ts
frontend/src/lib/format.ts
frontend/src/lib/query-keys.ts
frontend/src/lib/validation.ts
frontend/src/mocks/adapter.ts
frontend/src/mocks/data.ts
frontend/src/test/setup.ts
frontend/src/test/render.tsx
```

## Task 1: Scaffold the pinned frontend toolchain

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/package-lock.json`
- Create: `frontend/index.html`
- Create: `frontend/tsconfig.json`
- Create: `frontend/vite.config.ts`
- Create: `frontend/vitest.config.ts`
- Create: `frontend/.env.example`
- Create: `frontend/src/test/setup.ts`
- Create: `frontend/src/main.tsx`
- Create: `frontend/src/styles.css`

- [ ] **Step 1: Add the exact dependency manifest**

Create `frontend/package.json` with no range operators:

```json
{
  "name": "smart-hospital-frontend",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc --noEmit && vite build",
    "preview": "vite preview",
    "typecheck": "tsc --noEmit",
    "test": "vitest",
    "check:bundle-size": "node scripts/check-bundle-size.mjs"
  },
  "dependencies": {
    "@tanstack/react-query": "5.107.1",
    "lucide-react": "1.73.0",
    "react": "19.3.1",
    "react-dom": "19.3.1",
    "react-router-dom": "7.18.5"
  },
  "devDependencies": {
    "@tailwindcss/vite": "4.3.5",
    "@testing-library/jest-dom": "7.0.1",
    "@testing-library/react": "16.3.3",
    "@testing-library/user-event": "14.6.10",
    "@types/react": "19.3.0",
    "@types/react-dom": "19.3.0",
    "@vitejs/plugin-react": "6.3.0",
    "jsdom": "31.0.1",
    "tailwindcss": "4.3.5",
    "typescript": "7.0.2",
    "vite": "8.3.8",
    "vitest": "5.1.0"
  }
}
```

- [ ] **Step 2: Configure strict TypeScript, Vite, and Vitest**

`frontend/tsconfig.json` must enable `strict`, `noUncheckedIndexedAccess`, `exactOptionalPropertyTypes`, `noFallthroughCasesInSwitch`, `noEmit`, bundler module resolution, and DOM libraries.

`frontend/vite.config.ts` must use `react()` and `tailwindcss()` from Vite plugins and set `build.manifest: true`.

`frontend/vitest.config.ts` must use the React plugin, `environment: 'jsdom'`, `setupFiles: ['./src/test/setup.ts']`, and CSS processing enabled.

- [ ] **Step 3: Install and capture the lockfile**

Run:

```bash
cd frontend
npm install
```

Expected: `package-lock.json` is created, `npm ls --depth=0` reports no invalid dependency, and only the pinned versions above are installed.

- [ ] **Step 4: Add the base clinical tokens and test setup**

`frontend/src/styles.css` must begin with:

```css
@import "tailwindcss";

:root {
  --color-ink: #10233f;
  --color-muted: #52657a;
  --color-primary: #0f766e;
  --color-primary-strong: #115e59;
  --color-surface: #ffffff;
  --color-surface-subtle: #f1f5f9;
  --color-border: #dbe3ec;
  --color-success: #15803d;
  --color-warning: #b45309;
  --color-danger: #be123c;
  --space-1: 0.25rem;
  --space-2: 0.5rem;
  --space-3: 0.75rem;
  --space-4: 1rem;
  --space-5: 1.25rem;
  --space-6: 1.5rem;
  --space-8: 2rem;
  --space-10: 2.5rem;
  --space-12: 3rem;
  --radius-sm: 0.375rem;
  --radius-md: 0.625rem;
  --radius-lg: 1rem;
  --radius-full: 9999px;
  --shadow-card: 0 8px 24px rgb(15 35 63 / 0.07);
  --shadow-overlay: 0 20px 50px rgb(15 35 63 / 0.18);
  --font-sans: ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
  --duration-fast: 120ms;
  --duration-normal: 200ms;
}
```

`frontend/src/test/setup.ts` must import `@testing-library/jest-dom/vitest` and clean up after each test.

- [ ] **Step 5: Commit the scaffold**

```bash
git add frontend/package.json frontend/package-lock.json frontend/index.html frontend/tsconfig.json frontend/vite.config.ts frontend/vitest.config.ts frontend/.env.example frontend/src/test/setup.ts frontend/src/main.tsx frontend/src/styles.css
git commit -m "chore: scaffold frontend foundation"
```

## Task 2: Parse RFC 9457 problem details

**Files:**
- Create: `frontend/src/api/types.ts`
- Create: `frontend/src/api/problem.ts`
- Create: `frontend/src/api/problem.test.ts`

- [ ] **Step 1: Write the failing parser tests**

Create tests that require this public API:

```ts
export type ApiProblem = {
  status: number;
  code: string;
  title: string;
  detail: string;
  path?: string;
  timestamp?: string;
  fieldErrors: Record<string, string>;
  retryAfterSeconds?: number;
};

export function parseProblem(status: number, body: unknown, headers?: Headers): ApiProblem;
```

The tests must cover a complete problem body, the backend-compatible `message` alias, field errors, malformed JSON represented by an `unknown` body, and `Retry-After`.

- [ ] **Step 2: Run the focused test and verify RED**

Run:

```bash
cd frontend
npm test -- --run src/api/problem.test.ts
```

Expected: FAIL because `src/api/problem.ts` does not exist.

- [ ] **Step 3: Implement the minimal parser**

Implement `parseProblem` with these rules:

- use `code`, or `HTTP_<status>` when absent;
- use `title`, then a stable status title, then `Request failed`;
- use `detail`, then `message`, then the title;
- copy only string-valued `fieldErrors` and `errors` entries;
- parse integer `Retry-After` seconds;
- never include raw body text in the returned detail.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run:

```bash
cd frontend
npm test -- --run src/api/problem.test.ts
```

Expected: all problem parser tests pass.

- [ ] **Step 5: Commit the API problem boundary**

```bash
git add frontend/src/api/types.ts frontend/src/api/problem.ts frontend/src/api/problem.test.ts
git commit -m "feat: add typed api problem handling"
```

## Task 3: Implement the secure token lifecycle and serialized refresh retry

**Files:**
- Create: `frontend/src/api/token-store.ts`
- Create: `frontend/src/api/token-store.test.ts`
- Create: `frontend/src/api/client.ts`
- Create: `frontend/src/api/client.test.ts`

- [ ] **Step 1: Write failing token-store tests**

Require this API:

```ts
export const REFRESH_TOKEN_KEY = "smarthospital.refresh-token";

export interface TokenStore {
  getAccessToken(): string | null;
  setAccessToken(token: string): void;
  getRefreshToken(): string | null;
  setRefreshToken(token: string): void;
  clear(): void;
}

export function createTokenStore(storage: Storage = sessionStorage): TokenStore;
```

The test must prove that the access token is absent from both `sessionStorage` and `localStorage`, while the refresh token is present only under `REFRESH_TOKEN_KEY` in `sessionStorage`.

- [ ] **Step 2: Run the token-store test and verify RED**

Run:

```bash
cd frontend
npm test -- --run src/api/token-store.test.ts
```

Expected: FAIL because the token store module does not exist.

- [ ] **Step 3: Implement the token store and verify GREEN**

Run:

```bash
cd frontend
npm test -- --run src/api/token-store.test.ts
```

Expected: all token-store tests pass.

- [ ] **Step 4: Write failing refresh/retry tests**

Use an injected `fetch` and `TokenStore` to require:

```ts
export interface TokenCoordinator {
  getAccessToken(): string | null;
  refresh(): Promise<string>;
  clear(): void;
}

export function createApiClient(options: {
  baseUrl: string;
  fetch?: typeof fetch;
  tokens: TokenStore;
  refresh: () => Promise<TokenBundleResponse>;
  onSessionExpired?: () => void;
}): ApiClient;
```

The tests must prove:

1. a 401 triggers one refresh and one retried request with the new bearer token;
2. concurrent 401 responses trigger one shared refresh request;
3. a second 401 after retry is not refreshed again;
4. refresh failure clears both tokens and calls `onSessionExpired` once.

- [ ] **Step 5: Run the client test and verify RED**

Run:

```bash
cd frontend
npm test -- --run src/api/client.test.ts
```

Expected: FAIL because `src/api/client.ts` does not exist.

- [ ] **Step 6: Implement the minimal client and verify GREEN**

The client must parse success JSON, return `undefined` for 204, parse failures through `parseProblem`, retain the original request body only when it can be replayed, and track a private `_retried` flag on the cloned `Request`.

Run:

```bash
cd frontend
npm test -- --run src/api/client.test.ts
```

Expected: all client tests pass with one serialized refresh and no second retry.

- [ ] **Step 7: Commit the secure client**

```bash
git add frontend/src/api/token-store.ts frontend/src/api/token-store.test.ts frontend/src/api/client.ts frontend/src/api/client.test.ts
git commit -m "feat: add secure token refresh lifecycle"
```

## Task 4: Add authentication context, restoration, and typed endpoints

**Files:**
- Create: `frontend/src/auth/auth-context.ts`
- Create: `frontend/src/auth/session-coordinator.ts`
- Create: `frontend/src/auth/AuthProvider.tsx`
- Create: `frontend/src/auth/AuthProvider.test.tsx`
- Create: `frontend/src/api/endpoints.ts`
- Create: `frontend/src/api/adapter.ts`
- Modify: `frontend/src/api/types.ts`

- [ ] **Step 1: Write failing provider tests**

Require the provider to expose:

```ts
type AuthContextValue = {
  user: AuthUser | null;
  status: "restoring" | "authenticated" | "anonymous";
  signIn(credentials: LoginInput): Promise<AuthUser>;
  register(input: RegisterInput): Promise<AuthUser>;
  signOut(): Promise<void>;
};
```

Tests must cover successful login, restoration from the session refresh token, failed restoration clearing the refresh token, and logout clearing in-memory state.

- [ ] **Step 2: Run the provider test and verify RED**

Run:

```bash
cd frontend
npm test -- --run src/auth/AuthProvider.test.tsx
```

Expected: FAIL because the provider does not exist.

- [ ] **Step 3: Implement the provider and verify GREEN**

The provider must keep the access token only in `TokenStore` memory, keep the refresh token only in `sessionStorage`, expose `restoring` until one restoration attempt completes, and never log request data or personal details.

Run:

```bash
cd frontend
npm test -- --run src/auth/AuthProvider.test.tsx
```

Expected: all provider tests pass.

- [ ] **Step 4: Add endpoint and adapter contracts**

`frontend/src/api/endpoints.ts` must expose typed `login`, `register`, `refresh`, `logout`, `getPatientDashboard`, `getDoctorDashboard`, and `getAdminDashboard` functions. `frontend/src/api/adapter.ts` must select the real client unless `import.meta.env.VITE_API_MODE === 'demo'`. The demo adapter must implement the same functions and return deterministic fictional data.

- [ ] **Step 5: Commit authentication and endpoint contracts**

```bash
git add frontend/src/auth frontend/src/api/endpoints.ts frontend/src/api/adapter.ts frontend/src/api/types.ts
git commit -m "feat: add authentication session provider"
```

## Task 5: Enforce role guards and lazy route configuration

**Files:**
- Create: `frontend/src/auth/guards.tsx`
- Create: `frontend/src/auth/guards.test.tsx`
- Create: `frontend/src/app/router.tsx`
- Create: `frontend/src/app/router-smoke.test.tsx`
- Create: `frontend/src/features/shared/ForbiddenPage.tsx`
- Create: `frontend/src/features/shared/NotFoundPage.tsx`

- [ ] **Step 1: Write failing guard tests**

Cover these cases:

- anonymous users are redirected from each protected role route to `/login`;
- a patient cannot open `/doctor` or `/admin`;
- a doctor cannot open `/patient` or `/admin`;
- an administrator cannot open `/patient` or `/doctor`;
- each role reaches its own dashboard;
- unknown routes render the 404 page.

- [ ] **Step 2: Run the guard tests and verify RED**

Run:

```bash
cd frontend
npm test -- --run src/auth/guards.test.tsx frontend/src/app/router-smoke.test.tsx
```

Expected: FAIL because the guard and route modules do not exist.

- [ ] **Step 3: Implement guards and the lazy router**

Use `React.lazy` for public, patient, doctor, and administrator route modules. `RequireAuth` must wait for restoration and redirect with a sanitized `from` location. `RequireRole` must redirect an authenticated user to their own dashboard rather than rendering protected content.

- [ ] **Step 4: Run guard and smoke tests and verify GREEN**

Run:

```bash
cd frontend
npm test -- --run src/auth/guards.test.tsx frontend/src/app/router-smoke.test.tsx
```

Expected: all guard and route smoke tests pass.

- [ ] **Step 5: Commit role-aware routing**

```bash
git add frontend/src/auth frontend/src/app frontend/src/features/shared
git commit -m "feat: add role-aware routing and guards"
```

## Task 6: Build the accessible clinical design system

**Files:**
- Create: `frontend/src/components/ui/button.tsx`
- Create: `frontend/src/components/ui/field.tsx`
- Create: `frontend/src/components/ui/card.tsx`
- Create: `frontend/src/components/ui/badge.tsx`
- Create: `frontend/src/components/ui/dialog.tsx`
- Create: `frontend/src/components/ui/drawer.tsx`
- Create: `frontend/src/components/ui/menu.tsx`
- Create: `frontend/src/components/ui/toast.tsx`
- Create: `frontend/src/components/ui/feedback.tsx`
- Create: `frontend/src/components/ui/pagination.tsx`
- Create: `frontend/src/components/ui/tabs.tsx`
- Create: `frontend/src/components/ui/avatar.tsx`
- Create: `frontend/src/components/ui/index.ts`
- Create: `frontend/src/components/ui/ui.test.tsx`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Write failing primitive tests**

Require tests for:

- Button native semantics, loading label, and disabled state;
- Input visible label, `aria-invalid`, and error association;
- Dialog `role="dialog"`, labelled title, Escape close, and focus restoration;
- Tabs arrow-key navigation and selected-tab state;
- Avatar accessible initials fallback;
- Toast polite live-region semantics.

- [ ] **Step 2: Run the primitive tests and verify RED**

Run:

```bash
cd frontend
npm test -- --run src/components/ui/ui.test.tsx
```

Expected: FAIL because the UI modules do not exist.

- [ ] **Step 3: Implement the primitives and verify GREEN**

All buttons and interactive controls must have at least 44px touch targets. Dialog and Drawer must contain focus, close with Escape where safe, and restore focus. Menu must support ArrowUp/ArrowDown/Home/End/Escape. Tabs must support ArrowLeft/ArrowRight/Home/End.

Run:

```bash
cd frontend
npm test -- --run src/components/ui/ui.test.tsx
```

Expected: all primitive tests pass.

- [ ] **Step 4: Commit the design system**

```bash
git add frontend/src/components/ui frontend/src/styles.css
git commit -m "feat: add frontend foundation and design system"
```

## Task 7: Add responsive role navigation and page framing

**Files:**
- Create: `frontend/src/components/layout/AppShell.tsx`
- Create: `frontend/src/components/layout/Sidebar.tsx`
- Create: `frontend/src/components/layout/TopBar.tsx`
- Create: `frontend/src/components/layout/MobileNavigation.tsx`
- Create: `frontend/src/components/layout/PageHeader.tsx`
- Create: `frontend/src/components/layout/layout.test.tsx`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Write failing layout tests**

Test that patient, doctor, and administrator navigation labels are distinct, the current page has `aria-current="page"`, the mobile navigation button has an accessible name, and the demo badge is visible only in demo mode.

- [ ] **Step 2: Run the layout test and verify RED**

Run:

```bash
cd frontend
npm test -- --run src/components/layout/layout.test.tsx
```

Expected: FAIL because the layout modules do not exist.

- [ ] **Step 3: Implement the responsive shell and verify GREEN**

The shell must provide a skip link, `header`, role-specific `nav`, `main`, and `aside` landmarks. The desktop sidebar is 248px wide; below 1024px it becomes a drawer and the current role's four most-used destinations appear in compact bottom navigation.

Run:

```bash
cd frontend
npm test -- --run src/components/layout/layout.test.tsx
```

Expected: all layout tests pass.

- [ ] **Step 4: Commit the responsive shell**

```bash
git add frontend/src/components/layout frontend/src/styles.css
git commit -m "feat: add responsive clinical application shell"
```

## Task 8: Implement login and registration screens

**Files:**
- Create: `frontend/src/features/auth/LoginPage.tsx`
- Create: `frontend/src/features/auth/RegisterPage.tsx`
- Create: `frontend/src/features/auth/auth-forms.test.tsx`
- Create: `frontend/src/lib/validation.ts`
- Create: `frontend/src/lib/constants.ts`
- Modify: `frontend/src/auth/AuthProvider.tsx`
- Modify: `frontend/src/app/router.tsx`

- [ ] **Step 1: Write failing form tests**

Require:

- a blank login form shows a focused validation summary and field errors;
- invalid email and short password are rejected before a request;
- a successful login routes to the user's role dashboard;
- registration validates full name, email, password, phone, and date of birth;
- server field errors are associated with matching controls;
- demo credentials are visible only in demo mode.

- [ ] **Step 2: Run the form tests and verify RED**

Run:

```bash
cd frontend
npm test -- --run src/features/auth/auth-forms.test.tsx
```

Expected: FAIL because the screens and validation helpers do not exist.

- [ ] **Step 3: Implement the screens and verify GREEN**

Use visible labels, associated descriptions and errors, `noValidate`, inline error text, disabled mutation controls with stable labels, and safe generic request failures. Do not display raw response bodies.

Run:

```bash
cd frontend
npm test -- --run src/features/auth/auth-forms.test.tsx
```

Expected: all form tests pass.

- [ ] **Step 4: Commit authentication screens**

```bash
git add frontend/src/features/auth frontend/src/lib frontend/src/auth/AuthProvider.tsx frontend/src/app/router.tsx
git commit -m "feat: add accessible login and registration screens"
```

## Task 9: Add deterministic patient dashboard and priority rendering

**Files:**
- Create: `frontend/src/mocks/data.ts`
- Create: `frontend/src/mocks/adapter.ts`
- Create: `frontend/src/features/patient/PatientDashboardPage.tsx`
- Create: `frontend/src/features/doctor/DoctorDashboardPage.tsx`
- Create: `frontend/src/features/admin/AdminDashboardPage.tsx`
- Create: `frontend/src/features/queue/PriorityBadge.tsx`
- Create: `frontend/src/features/queue/PriorityBadge.test.tsx`
- Create: `frontend/src/lib/format.ts`
- Create: `frontend/src/lib/query-keys.ts`

- [ ] **Step 1: Write failing priority-rendering test**

Require:

```tsx
<PriorityBadge priority="HIGH" />
```

to render the visible text `High priority`, an accessible name that does not rely on color, and a warning variant. Test every `NORMAL`, `MEDIUM`, `HIGH`, and `CRITICAL` value.

- [ ] **Step 2: Run the priority test and verify RED**

Run:

```bash
cd frontend
npm test -- --run src/features/queue/PriorityBadge.test.tsx
```

Expected: FAIL because `PriorityBadge.tsx` does not exist.

- [ ] **Step 3: Implement priority rendering and verify GREEN**

Run:

```bash
cd frontend
npm test -- --run src/features/queue/PriorityBadge.test.tsx
```

Expected: all priority tests pass.

- [ ] **Step 4: Add the deterministic patient dashboard fixture**

`frontend/src/mocks/data.ts` must define stable fictional records containing:

- upcoming appointment date, time, doctor, department, and status;
- queue token `A-014`;
- queue position `3`;
- wait estimate `18 minutes`;
- priority `HIGH`.

The demo adapter must return this same result for every patient dashboard request in the same browser session. The page must visibly label the preview as demo data and must not describe the record as live hospital data.

- [ ] **Step 5: Add distinct doctor and administrator shell content**

The doctor shell must show today's appointments, active queue count, and current consultation placeholders. The administrator shell must show users, departments, doctors, appointments, and queues metric placeholders. Neither shell may claim to complete the full role workflow.

- [ ] **Step 6: Commit dashboard shells**

```bash
git add frontend/src/mocks frontend/src/features/patient frontend/src/features/doctor frontend/src/features/admin frontend/src/features/queue frontend/src/lib
git commit -m "feat: add role dashboard shells and demo patient queue"
```

## Task 10: Add providers, error handling, and application entrypoint

**Files:**
- Create: `frontend/src/app/providers.tsx`
- Create: `frontend/src/app/error-boundary.tsx`
- Create: `frontend/src/app/App.tsx`
- Modify: `frontend/src/main.tsx`
- Modify: `frontend/src/styles.css`

- [ ] **Step 1: Implement provider composition**

`AppProviders` must create one `QueryClient`, mount `AuthProvider`, mount the toast provider, and expose the browser router through route configuration. Query defaults must use a narrow stale time and one retry, except 4xx responses.

- [ ] **Step 2: Implement the route error boundary**

The boundary must render a safe recovery panel with a reload action. It must not display raw stack traces, response bodies, tokens, or personal details.

- [ ] **Step 3: Verify application compilation**

Run:

```bash
cd frontend
npm run typecheck
```

Expected: TypeScript exits with code 0.

- [ ] **Step 4: Commit application composition**

```bash
git add frontend/src/app frontend/src/main.tsx frontend/src/styles.css
git commit -m "feat: compose frontend providers and error boundary"
```

## Task 11: Enforce route and bundle budgets

**Files:**
- Create: `frontend/scripts/check-bundle-size.mjs`
- Modify: `frontend/package.json`
- Modify: `frontend/vite.config.ts`
- Create: `frontend/src/app/bundle-budget.test.ts`

- [ ] **Step 1: Write the failing budget test**

The test must run the size-check script against a temporary manifest containing an entry and static imports, assert that the script exits nonzero above 200,000 gzip bytes, and exits zero below the threshold.

- [ ] **Step 2: Run the budget test and verify RED**

Run:

```bash
cd frontend
npm test -- --run src/app/bundle-budget.test.ts
```

Expected: FAIL because `frontend/scripts/check-bundle-size.mjs` does not exist.

- [ ] **Step 3: Implement the budget script and verify GREEN**

The script must parse `dist/.vite/manifest.json`, find the entry chunk, recursively sum static imports only, gzip each JavaScript file, and fail when the total is at least 200,000 bytes. It must report the largest lazy route chunk and fail that chunk at or above 120,000 gzip bytes.

Run:

```bash
cd frontend
npm test -- --run src/app/bundle-budget.test.ts
```

Expected: all budget tests pass.

- [ ] **Step 4: Build and measure the real bundle**

Run:

```bash
cd frontend
npm run build
npm run check:bundle-size
```

Expected: Vite exits 0, the initial JavaScript is below 200 KB gzip, and the largest lazy route chunk is below 120 KB gzip. If the budget fails, split the offending route or dependency; do not increase the budget.

- [ ] **Step 5: Commit budget enforcement**

```bash
git add frontend/scripts/check-bundle-size.mjs frontend/package.json frontend/vite.config.ts frontend/src/app/bundle-budget.test.ts
git commit -m "test: enforce frontend bundle budget"
```

## Task 12: Run complete verification and publish

**Files:**
- Modify only files required by findings from the verification commands.

- [ ] **Step 1: Run all frontend tests**

```bash
cd frontend
npm test -- --run
```

Expected: every Vitest file passes with zero failed tests and no unhandled errors.

- [ ] **Step 2: Run strict typechecking**

```bash
cd frontend
npm run typecheck
```

Expected: TypeScript exits 0 with no diagnostics.

- [ ] **Step 3: Run the production build and bundle checks**

```bash
cd frontend
npm run build
npm run check:bundle-size
```

Expected: the production build exits 0, initial JavaScript remains below 200 KB gzip, and each lazy route chunk remains below 120 KB gzip.

- [ ] **Step 4: Check repository hygiene**

```bash
git diff --check
git status --short
git ls-files | grep -E '(^|/)(dist|coverage|node_modules)/' || true
git grep -nE '(gh[opsu]_[A-Za-z0-9]{20,}|BEGIN (RSA |EC )?PRIVATE KEY)' -- frontend || true
```

Expected: `git diff --check` is clean; no generated build, coverage, dependency, token, or private-key artifact is tracked; only intentional source and documentation files remain changed.

- [ ] **Step 5: Push the verified branch**

```bash
git push -u origin feature/frontend-mvp
```

Expected: the remote branch points to the verified local commit and reports no rejected push.

# Frontend Audit — Figma Make Output

> Generated: 2026-05-08
> Scope: `frontend/` directory only
> Source: Figma Make (docs 23/24 prompts)

---

## What Figma Make Generated Well

### Architecture & Structure
- **Feature-based directory layout** — `src/features/`, `src/components/`, `src/layouts/`, `src/routes/`, `src/auth/`, `src/api/` matches docs 20/22 architecture spec
- **Separate public and app layouts** — `PublicLayout` (landing/login/register) vs `AppLayout` (sidebar+topbar+outlet)
- **Clean route configuration** — `createBrowserRouter` with public, protected, and admin route groups
- **Centralized route paths** — `routePaths.ts` constants object

### Authentication
- **Real JWT auth — no mocks** — all auth calls hit real backend endpoints (`/auth/login`, `/auth/register`, `/auth/me`, `/auth/refresh`, `/auth/logout`)
- **Token service** — localStorage-based `tokenService` with get/set/clear/hasTokens
- **Auth context with useReducer** — proper state machine (SET_USER, LOGOUT, SET_LOADING)
- **Session bootstrap** — on mount, checks tokens → calls `/auth/me` → populates user state
- **Token refresh interceptor** — 401 response triggers `/auth/refresh` with mutex to prevent race conditions, retries original request
- **Role/permission checks** — `hasRole()` and `hasPermission()` helpers exposed via context

### Route Protection
- **ProtectedRoute** — redirects unauthenticated users to `/login`
- **AdminRoute** — checks both authentication AND `ADMIN` role, redirects to `/dashboard` if not admin
- **Loading screen** — shown during auth bootstrap to prevent flash of wrong content

### Role-Aware Navigation
- **Sidebar** — dynamically shows admin section only for users with ADMIN role
- **Topbar** — displays current user info and logout button
- **Responsive** — mobile drawer sidebar with backdrop overlay, collapses on desktop

### API Layer
- **Axios client** — configured with `VITE_API_BASE_URL` env var, fallback to `http://localhost:8080/api/v1`
- **Request interceptor** — automatically attaches Bearer token
- **Response interceptor** — handles 401 refresh with subscriber queue pattern
- **Error handling** — `parseApiError` utility maps backend `ErrorResponse` format correctly

### Generated Types
- **`api/generated/types.ts`** — TypeScript interfaces matching backend contract: `CurrentUserResponse`, `TokenResponse`, `UserResponse`, `PagedResponse<T>`, `ErrorResponse`, `RoleResponse`, `PermissionResponse`, status enums, request DTOs
- **Correct pagination contract** — `PagedResponse` with `content`, `page`, `size`, `totalElements`, `totalPages`, `first`, `last`, `sort`

### Forms & Validation
- **React Hook Form + Zod** — used in Login and Register pages
- **Backend field error mapping** — Register page maps `fieldErrors` to inline form errors via `setError()`
- **Login error handling** — uses `parseApiError` for toast notifications

### Admin Pages
- **Users list** — paginated DataTable with sorting, row click navigation to detail
- **User detail** — view user info, change status via modal with confirmation
- **Roles page** — fetches from `/roles` endpoint, card list
- **Permissions page** — fetches from `/permissions` endpoint, card list
- **System page** — health check via `/ping` endpoint with auto-refresh option
- **Admin dashboard** — navigation cards to all admin sections

### UI Components
- **Custom component library** — Button, Card, Badge, StatusBadge, Modal, Spinner, Skeleton, EmptyState, Input
- **DataTable** — generic typed table with sort indicators
- **Pagination** — page navigation component
- **FormInput** — React Hook Form integrated input wrapper
- **Tailwind-based** — all components use Tailwind utility classes with `cn()` helper

### Data Fetching
- **TanStack Query** — used consistently across admin pages with proper query keys, loading states, error handling
- **Query invalidation** — UserDetailPage invalidates both user and users queries after status update

### Styling
- **Tailwind CSS v4** — via `@tailwindcss/vite` plugin
- **CSS variable theme system** — light/dark mode variables defined in `theme.css`
- **shadcn-compatible theme** — `@theme inline` block maps CSS vars to Tailwind tokens
- **Inter font** — loaded from Google Fonts
- **Base typography** — default sizing for headings, labels, buttons, inputs

---

## What Is Missing

### vs Doc 20 (Frontend Architecture)
- ❌ **No OpenAPI code generation** — `api/generated/types.ts` is hand-written, not generated from OpenAPI spec. Doc 20 says "All API models should come from generated OpenAPI TypeScript client"
- ❌ **No `types/` directory** — planned in doc 20 structure but not created
- ❌ **No `pages/` directory** — planned in doc 20 but pages live inside `features/` (acceptable deviation)

### vs Doc 21 (Product UI Spec)
- ❌ **No About page** — listed under Public Area
- ❌ **No Terms/Privacy page** — listed under Public Area
- ❌ **No public marketplace browsing** — landing links to `/marketplace` but it's behind auth
- ❌ **No MODERATOR navigation** — doc 21 specifies reports, moderation queue, user review tools for moderators
- ❌ **No notifications** — doc 21 lists it for authenticated users, no notification bell or area
- ❌ **No favorites/watchlist** — doc 21 lists it for authenticated users
- ❌ **No settings page** — doc 21 lists it for authenticated users
- ❌ **User dashboard has no widgets** — doc 21 specifies recent activity, recent offers, notifications, saved listings, recommendations; current version only has navigation cards
- ❌ **Admin dashboard has no stats** — doc 21 specifies total users, active users, banned users, reports count, system activity; current version only has navigation cards
- ❌ **No search bar in topbar** — doc 21 specifies search bar in main layout
- ❌ **No breadcrumbs** — doc 21 specifies breadcrumbs in layout
- ❌ **No user/account dropdown** — doc 21 specifies user dropdown; current topbar shows username inline
- ❌ **No dark mode toggle** — theme CSS has dark mode vars but no toggle UI

### vs Doc 22 (Technical Spec)
- ❌ **No `api/client/` subdirectory** — doc 22 specifies `api/generated/`, `api/client/`, `api/interceptors/`
- ❌ **No `api/interceptors/` subdirectory** — interceptors are inline in `axios.ts`
- ❌ **No `useAuthGuard` / `useRoleGuard` hooks** — doc 22 mentions these; auth guards are component-based instead (acceptable)
- ❌ **No filtering/search in admin tables** — doc 22 specifies filtering and search for admin tables
- ❌ **No unauthorized / 404 page** — fallback is `Navigate to="/"`, doc 22 says "unauthorized fallback pages"
- ❌ **Feature directories don't contain hooks/services/validation sub-modules** — doc 22 specifies each feature should have pages, hooks, components, services, validation, route definitions

### vs Docs 23/24 (Figma Make Prompts)
- ❌ **No `.env.example`** — prompt specified it (now added, see fixes below)
- ❌ **No `FormSelect` component** — doc 24 lists it alongside `FormInput`
- ❌ **No `SortHeader` component** — doc 24 lists it in data components
- ❌ **No `MobileDrawer` component** — sidebar handles mobile but no dedicated drawer
- ❌ **No `vite-env.d.ts`** — prompt specified it for env type safety

---

## What Is Risky

### Yarn / Package Manager
- ⚠️ **`pnpm-workspace.yaml` still exists** — not harmful to Yarn but signals pnpm setup; can be removed later
- ⚠️ **Bundle size is 519 KB** — chunk size warning from Vite; many unused dependencies (MUI, Emotion, Radix extras, react-dnd, canvas-confetti, react-slick, recharts, etc.) inflate the bundle significantly

### Dead Code
- ⚠️ **`src/app/components/` tree** — Figma Make generated ~50 shadcn/ui component files under `src/app/components/ui/` plus a `figma/` folder and a duplicate `utils.ts`. **None are imported by the actual app** — the app uses `src/components/ui/` instead. This is pure dead code
- ⚠️ **`src/app/components/ui/sonner.tsx`** imports `next-themes` (`useTheme`) — a Next.js-only package. Will crash if ever imported from a Vite app. Dead code for now
- ⚠️ **`globals.css` is empty** — imported nowhere, serves no purpose

### Unused Dependencies in `package.json`
- ⚠️ `@emotion/react`, `@emotion/styled`, `@mui/material`, `@mui/icons-material` — MUI is not used by any app component
- ⚠️ `canvas-confetti`, `react-dnd`, `react-dnd-html5-backend`, `react-slick`, `react-responsive-masonry`, `recharts`, `react-popper`, `@popperjs/core`, `embla-carousel-react`, `input-otp`, `cmdk`, `react-resizable-panels`, `vaul`, `react-day-picker`, `motion` — none currently imported by app code
- ⚠️ `next-themes` — Next.js specific, only imported by dead code in `src/app/components/ui/sonner.tsx`
- ⚠️ Many Radix UI packages — only `@radix-ui/react-slot` is potentially used; the rest are from the dead shadcn tree

### Auth / Security
- ⚠️ **Tokens in localStorage** — acceptable per doc 22 ("localStorage initially, future migration possible") but XSS risk
- ⚠️ **`window.location.href` redirect on refresh failure** — works but loses React Router state; caused by interceptor running outside Router context

---

## Backend Integration Readiness

### Real Backend Endpoints Used
| Endpoint | Method | Used In |
|---|---|---|
| `/auth/login` | POST | AuthContext.login |
| `/auth/register` | POST | AuthContext.register |
| `/auth/me` | GET | AuthContext.initAuth, AuthContext.login |
| `/auth/refresh` | POST | axios interceptor |
| `/auth/logout` | POST | AuthContext.logout |
| `/users` | GET | UsersListPage |
| `/users/:uuid` | GET | UserDetailPage |
| `/users/:uuid/status` | PATCH | UserDetailPage |
| `/roles` | GET | RolesPage |
| `/permissions` | GET | PermissionsPage |
| `/ping` | GET | SystemPage |

### Mock / Local Data
- **None** — zero mock data, zero hardcoded responses, zero demo/fake auth. All data flows through `apiClient` to real backend

### Integration Status
- ✅ Auth flow is fully wired to backend
- ✅ Admin CRUD operations use real endpoints
- ✅ Error handling matches backend `ErrorResponse` contract
- ✅ Pagination uses backend contract (`page`, `size`, `sort`)
- ✅ Token refresh with automatic retry is implemented
- ⚠️ No `.env` file — will use fallback `http://localhost:8080/api/v1` (`.env.example` now provided)

---

## What Must Be Fixed Before First Run

These were the startup blockers — **all are now resolved** (see section below).

1. **`react` and `react-dom` were only in `peerDependencies`** — Yarn does not auto-install peer deps; app would fail to resolve React
2. **`pnpm.overrides` in `package.json`** — Yarn ignores this field; needed conversion to `resolutions`
3. **Package name `@figma/my-make-file`** — incorrect project identity
4. **Missing `.env.example`** — no documentation of required environment variables
5. **Missing `@types/react` and `@types/react-dom`** — TypeScript compilation needs React type definitions
6. **Missing `typescript`** — needed as devDependency for `tsc` checks

---

## What Can Be Fixed Later

These are non-blocking improvements for future tasks:

1. **Remove dead `src/app/components/` tree** (~50 unused shadcn/figma files)
2. **Prune unused dependencies** — MUI, Emotion, canvas-confetti, react-dnd, react-slick, etc.
3. **Set up OpenAPI code generation** — replace hand-written `types.ts` with generated client
4. **Add missing pages** — About, Terms, 404/Unauthorized, Settings
5. **Add MODERATOR role navigation** — reports, moderation queue
6. **Add real dashboard widgets** — stats, activity feeds, charts
7. **Add dark mode toggle** — theme CSS ready, just needs UI toggle
8. **Add search bar and breadcrumbs** — in topbar/layout
9. **Add notification system** — bell icon, notification area
10. **Add `vite-env.d.ts`** — env variable type safety
11. **Restructure features** — add hooks/services/validation sub-modules per doc 22
12. **Code-split** — dynamic imports to reduce bundle size below 500 KB
13. **Delete `pnpm-workspace.yaml`** — no longer needed
14. **Remove `globals.css`** — empty file

---

## Startup Blocker Fixes Applied

The following minimal changes were made to enable `yarn install` and `yarn build` to succeed. No refactoring, no cleanup, no architecture changes.

### 1. `package.json` — Moved `react` and `react-dom` to `dependencies`
**Why:** They were listed only under `peerDependencies` with `optional: true`. Yarn (classic) does not automatically install peer dependencies. Without React as a real dependency, every import of `react` would fail at runtime and during build.

**Change:** Added `"react": "18.3.1"` and `"react-dom": "18.3.1"` to `dependencies`. Removed the `peerDependencies` and `peerDependenciesMeta` sections entirely (no longer needed since they're direct deps).

### 2. `package.json` — Replaced `pnpm.overrides` with `resolutions`
**Why:** The `pnpm` key with `overrides` is a pnpm-specific feature that Yarn ignores completely. Yarn uses the `resolutions` field for the same purpose.

**Change:** Replaced `"pnpm": { "overrides": { "vite": "6.3.5" } }` with `"resolutions": { "vite": "6.3.5" }`.

### 3. `package.json` — Renamed package
**Why:** `@figma/my-make-file` is the Figma Make default name, not meaningful for the project.

**Change:** Renamed to `@barter-platform/frontend`.

### 4. `package.json` — Added `@types/react`, `@types/react-dom`, `typescript` to `devDependencies`
**Why:** TypeScript compilation requires React type definitions. Without them, all `.tsx` files would fail type checking. The `typescript` package was also missing as an explicit devDependency.

**Change:** Added `"@types/react": "^18.3.0"`, `"@types/react-dom": "^18.3.0"`, and `"typescript": "^5.5.0"` to `devDependencies`.

### 5. Created `frontend/.env.example`
**Why:** The API client uses `import.meta.env.VITE_API_BASE_URL` with a fallback. A `.env.example` documents this for developers.

**Change:** Created `.env.example` with `VITE_API_BASE_URL=http://localhost:8080/api/v1`.

### Build Results
```
$ yarn install → success (lockfile generated)
$ yarn build  → success (8.31s, dist/ output)
  - dist/index.html          0.52 kB
  - dist/assets/index.css   101.82 kB
  - dist/assets/index.js    519.32 kB (chunk warning — non-blocking, fix later with dep pruning)
```


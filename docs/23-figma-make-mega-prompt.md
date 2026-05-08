# Figma Make — Barter Platform Frontend Mega Prompt

> **Usage:** Copy the entire prompt block below and paste it directly into Figma Make.
> It will generate the complete frontend application foundation for the Barter Platform.

---

````text
You are a senior frontend architect. Generate a complete, production-grade React frontend application for the **Barter Platform** — a modern item-exchange / barter marketplace with a full admin control panel.

The backend already exists, is fully functional, and uses an **OpenAPI-first** design. The frontend must integrate with it through a **generated TypeScript client** — never manually duplicate backend DTOs.

---

## 1 — TECH STACK (mandatory, do not substitute)

| Layer            | Technology                               |
|------------------|------------------------------------------|
| Framework        | React 18+                                |
| Language         | TypeScript (strict mode)                 |
| Build tool       | Vite                                     |
| Styling          | Tailwind CSS 3+                          |
| Routing          | React Router v6 (data-router)            |
| Data fetching    | TanStack Query v5 (React Query)          |
| Forms            | React Hook Form + Zod                    |
| API layer        | Generated OpenAPI TypeScript client      |
| Icons            | Lucide React                             |
| Toasts           | Sonner                                   |
| State (auth)     | React Context + useReducer              |

---

## 2 — PROJECT STRUCTURE (feature-based architecture)

Generate exactly this directory layout:

```
frontend/
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
├── tailwind.config.ts
├── postcss.config.js
├── .env.example            # VITE_API_BASE_URL=http://localhost:8080/api/v1
├── src/
│   ├── main.tsx
│   ├── App.tsx
│   ├── vite-env.d.ts
│   │
│   ├── api/
│   │   ├── generated/              # placeholder — openapi-generator output goes here
│   │   │   └── .gitkeep
│   │   ├── client.ts               # axios instance, base URL, interceptors
│   │   └── interceptors/
│   │       ├── auth.interceptor.ts  # attach Bearer token
│   │       └── error.interceptor.ts # global error → toast mapping
│   │
│   ├── auth/
│   │   ├── AuthContext.tsx          # auth state provider
│   │   ├── AuthProvider.tsx         # bootstrap: load tokens → call /auth/me
│   │   ├── useAuth.ts              # hook exposing user, roles, permissions, login, logout
│   │   ├── useAuthGuard.ts         # redirect if unauthenticated
│   │   ├── useRoleGuard.ts         # redirect if role missing
│   │   └── token.service.ts        # localStorage get/set/clear tokens
│   │
│   ├── components/
│   │   ├── ui/
│   │   │   ├── Button.tsx
│   │   │   ├── Input.tsx
│   │   │   ├── Select.tsx
│   │   │   ├── Badge.tsx
│   │   │   ├── Card.tsx
│   │   │   ├── Avatar.tsx
│   │   │   ├── Skeleton.tsx
│   │   │   ├── Spinner.tsx
│   │   │   ├── Modal.tsx
│   │   │   ├── DropdownMenu.tsx
│   │   │   ├── Tooltip.tsx
│   │   │   ├── EmptyState.tsx
│   │   │   └── StatusBadge.tsx      # maps UserStatus enum to colored badges
│   │   ├── data/
│   │   │   ├── DataTable.tsx        # generic paginated/sortable table
│   │   │   ├── Pagination.tsx       # page/size controls matching backend contract
│   │   │   └── SortHeader.tsx       # clickable column header with sort indicator
│   │   ├── feedback/
│   │   │   ├── LoadingScreen.tsx
│   │   │   ├── ErrorCard.tsx
│   │   │   └── FormFieldError.tsx   # inline field error from fieldErrors[]
│   │   └── forms/
│   │       ├── FormInput.tsx         # React Hook Form + Tailwind input wrapper
│   │       ├── FormSelect.tsx
│   │       └── FormGroup.tsx
│   │
│   ├── layouts/
│   │   ├── PublicLayout.tsx          # minimal header + centered content
│   │   ├── AppLayout.tsx            # top bar + collapsible sidebar + content area
│   │   ├── Sidebar.tsx              # role-aware navigation sections
│   │   ├── TopBar.tsx               # search, notifications bell, user dropdown
│   │   ├── UserDropdown.tsx         # avatar, name, role badges, logout
│   │   └── MobileDrawer.tsx         # responsive sidebar drawer
│   │
│   ├── features/
│   │   ├── landing/
│   │   │   └── LandingPage.tsx
│   │   ├── auth/
│   │   │   ├── LoginPage.tsx
│   │   │   ├── RegisterPage.tsx
│   │   │   ├── hooks/
│   │   │   │   ├── useLogin.ts      # TanStack mutation → POST /auth/login
│   │   │   │   └── useRegister.ts   # TanStack mutation → POST /auth/register
│   │   │   └── components/
│   │   │       ├── LoginForm.tsx
│   │   │       └── RegisterForm.tsx
│   │   ├── dashboard/
│   │   │   ├── DashboardPage.tsx     # role-aware: user widgets vs admin stats
│   │   │   └── components/
│   │   │       ├── WelcomeCard.tsx
│   │   │       ├── QuickActions.tsx
│   │   │       └── RecentActivity.tsx
│   │   ├── profile/
│   │   │   ├── ProfilePage.tsx
│   │   │   └── components/
│   │   │       └── ProfileCard.tsx
│   │   ├── admin/
│   │   │   ├── AdminDashboardPage.tsx
│   │   │   ├── users/
│   │   │   │   ├── UsersListPage.tsx    # GET /users with pagination
│   │   │   │   ├── UserDetailPage.tsx   # GET /users/{userUuid}
│   │   │   │   ├── hooks/
│   │   │   │   │   ├── useUsers.ts      # TanStack query → GET /users
│   │   │   │   │   ├── useUser.ts       # TanStack query → GET /users/{uuid}
│   │   │   │   │   └── useUpdateUserStatus.ts  # TanStack mutation → PATCH /users/{uuid}/status
│   │   │   │   └── components/
│   │   │   │       ├── UsersTable.tsx
│   │   │   │       ├── UserStatusBadge.tsx
│   │   │   │       └── UpdateStatusModal.tsx
│   │   │   ├── roles/
│   │   │   │   ├── RolesListPage.tsx    # GET /roles
│   │   │   │   ├── hooks/
│   │   │   │   │   └── useRoles.ts
│   │   │   │   └── components/
│   │   │   │       └── RolesTable.tsx
│   │   │   ├── permissions/
│   │   │   │   ├── PermissionsListPage.tsx  # GET /permissions
│   │   │   │   ├── hooks/
│   │   │   │   │   └── usePermissions.ts
│   │   │   │   └── components/
│   │   │   │       └── PermissionsTable.tsx
│   │   │   └── system/
│   │   │       └── SystemOverviewPage.tsx   # GET /ping + system stats
│   │   ├── marketplace/
│   │   │   └── MarketplacePlaceholder.tsx   # future module placeholder
│   │   ├── offers/
│   │   │   └── OffersPlaceholder.tsx        # future module placeholder
│   │   └── messages/
│   │       └── MessagesPlaceholder.tsx      # future module placeholder
│   │
│   ├── routes/
│   │   ├── index.tsx                # createBrowserRouter definition
│   │   ├── ProtectedRoute.tsx       # auth guard wrapper
│   │   ├── AdminRoute.tsx           # role guard wrapper (ADMIN)
│   │   └── routePaths.ts            # centralized path constants
│   │
│   ├── styles/
│   │   └── globals.css              # Tailwind directives + CSS custom properties
│   │
│   ├── types/
│   │   ├── auth.types.ts            # AuthState, AuthAction
│   │   └── pagination.types.ts      # PageParams, SortParams
│   │
│   └── utils/
│       ├── cn.ts                    # clsx + tailwind-merge utility
│       ├── date.ts                  # date formatting helpers
│       └── error.ts                 # parseApiError → map ErrorResponse to displayable
```

---

## 3 — BACKEND API CONTRACT (source of truth — do NOT invent new endpoints)

Base URL: `http://localhost:8080/api/v1`

### 3.1 Auth Endpoints

| Method | Path               | Auth | Request Body              | Success Response            |
|--------|--------------------|------|---------------------------|-----------------------------|
| POST   | `/auth/register`   | No   | `RegisterUserRequest`     | `201` → `CurrentUserResponse` |
| POST   | `/auth/login`      | No   | `LoginRequest`            | `200` → `TokenResponse`    |
| GET    | `/auth/me`         | Yes  | —                         | `200` → `CurrentUserResponse` |
| POST   | `/auth/refresh`    | No   | `RefreshTokenRequest`     | `200` → `TokenResponse`    |
| POST   | `/auth/logout`     | No   | `RefreshTokenRequest`     | `204` — no body            |

### 3.2 User Management Endpoints

| Method | Path                          | Auth | Query Params            | Response                   |
|--------|-------------------------------|------|-------------------------|----------------------------|
| GET    | `/users`                      | Yes  | `page`, `size`, `sort`  | `200` → `UserPagedResponse` |
| GET    | `/users/{userUuid}`           | Yes  | —                       | `200` → `UserResponse`    |
| PATCH  | `/users/{userUuid}/status`    | Yes  | —                       | `200` → `UserResponse`    |

### 3.3 Roles & Permissions Endpoints

| Method | Path                  | Auth | Response                      |
|--------|-----------------------|------|-------------------------------|
| GET    | `/roles`              | Yes  | `200` → `RoleResponse[]`     |
| GET    | `/roles/{code}`       | Yes  | `200` → `RoleResponse`       |
| GET    | `/permissions`        | Yes  | `200` → `PermissionResponse[]` |

### 3.4 System

| Method | Path    | Auth | Response              |
|--------|---------|------|-----------------------|
| GET    | `/ping` | No   | `200` → `PingResponse` |

---

## 4 — BACKEND DTO SCHEMAS (exact shapes — use as reference, import from generated client)

### ErrorResponse
```json
{
  "timestamp": "2025-01-01T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "Validation failed",
  "path": "/api/v1/auth/register",
  "fieldErrors": [
    { "field": "email", "message": "must be a valid email" }
  ]
}
```

### TokenResponse
```json
{
  "accessToken": "eyJ...",
  "refreshToken": "abc123..."
}
```

### CurrentUserResponse
```json
{
  "uuid": "550e8400-e29b-41d4-a716-446655440000",
  "username": "johndoe",
  "email": "john@example.com",
  "status": "ACTIVE",
  "emailVerified": true,
  "mfaEnabled": false,
  "lastLoginAt": "2025-01-01T12:00:00Z",
  "roles": [{ "code": "USER", "name": "User", "description": "..." }],
  "permissions": [{ "code": "ITEM_CREATE", "name": "Create Item", "description": "..." }],
  "oauthAccounts": [],
  "createdAt": "2025-01-01T00:00:00Z",
  "updatedAt": null
}
```

### UserSummaryResponse (inside UserPagedResponse.content[])
```json
{
  "uuid": "...",
  "username": "...",
  "email": "...",
  "status": "ACTIVE",
  "emailVerified": true,
  "mfaEnabled": false,
  "lastLoginAt": null,
  "createdAt": "...",
  "updatedAt": null
}
```

### UserPagedResponse
```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8,
  "first": true,
  "last": false,
  "sort": "username,asc"
}
```

### UserStatus enum values
`PENDING_VERIFICATION` | `ACTIVE` | `SUSPENDED` | `BANNED` | `DELETED`

### RegisterUserRequest
```json
{ "username": "...", "email": "...", "password": "..." }
```

### LoginRequest
```json
{ "identifier": "...", "password": "..." }
```
`identifier` accepts either username or email.

### RefreshTokenRequest
```json
{ "refreshToken": "..." }
```

### UpdateUserStatusRequest
```json
{ "status": "SUSPENDED" }
```

---

## 5 — AUTHENTICATION ARCHITECTURE

### 5.1 Token Storage
- Store `accessToken` and `refreshToken` in `localStorage`.
- On every API request, attach `Authorization: Bearer {accessToken}`.

### 5.2 Session Bootstrap (on app load)
1. Check localStorage for existing tokens.
2. If tokens exist → call `GET /auth/me`.
3. If `/auth/me` succeeds → populate AuthContext with user, roles, permissions.
4. If `/auth/me` fails (401) → attempt silent refresh via `POST /auth/refresh`.
5. If refresh fails → clear tokens, redirect to `/login`.
6. Show a full-screen `<LoadingScreen>` spinner during bootstrap.

### 5.3 Token Refresh Interceptor
- Axios response interceptor: on `401` response, attempt exactly one refresh.
- If refresh succeeds → update stored tokens, retry the original request.
- If refresh fails → clear auth state, redirect to `/login`.
- Use a mutex/queue to prevent multiple concurrent refresh calls.

### 5.4 Login Flow
1. `POST /auth/login` with `{ identifier, password }`.
2. Store `accessToken` and `refreshToken`.
3. Call `GET /auth/me` to populate user state.
4. Redirect to `/dashboard`.

### 5.5 Register Flow
1. `POST /auth/register` with `{ username, email, password }`.
2. On `201` → show success message, redirect to `/login`.
3. On `409` (conflict) → show "username or email already exists".
4. On `400` → show inline field errors from `fieldErrors[]`.

### 5.6 Logout Flow
1. Call `POST /auth/logout` with `{ refreshToken }`.
2. Clear localStorage tokens.
3. Reset AuthContext.
4. Redirect to `/`.

---

## 6 — ROLE-BASED ACCESS CONTROL

### 6.1 Roles
The backend defines three roles: `USER`, `MODERATOR`, `ADMIN`.

### 6.2 AuthContext Shape
```ts
interface AuthState {
  user: CurrentUserResponse | null;
  isAuthenticated: boolean;
  isLoading: boolean;        // true during bootstrap
  roles: string[];           // e.g. ["USER", "ADMIN"]
  permissions: string[];     // e.g. ["ITEM_CREATE", "USER_BAN"]
}
```

### 6.3 Helper Hooks
- `useAuth()` — returns auth state + `login()`, `logout()`, `register()`.
- `hasRole(role: string): boolean`
- `hasPermission(permission: string): boolean`
- `isAdmin(): boolean` — shorthand for `hasRole("ADMIN")`

### 6.4 Route Guards
- `<ProtectedRoute>` — redirects to `/login` if not authenticated.
- `<AdminRoute>` — redirects to `/dashboard` if user lacks `ADMIN` role.

---

## 7 — ROUTING

```tsx
const router = createBrowserRouter([
  // --- Public routes (PublicLayout) ---
  { path: "/",         element: <LandingPage /> },
  { path: "/login",    element: <LoginPage /> },
  { path: "/register", element: <RegisterPage /> },

  // --- Protected routes (AppLayout, requires auth) ---
  {
    element: <ProtectedRoute><AppLayout /></ProtectedRoute>,
    children: [
      { path: "/dashboard",  element: <DashboardPage /> },
      { path: "/profile",    element: <ProfilePage /> },
      { path: "/marketplace", element: <MarketplacePlaceholder /> },
      { path: "/offers",     element: <OffersPlaceholder /> },
      { path: "/messages",   element: <MessagesPlaceholder /> },

      // --- Admin routes (requires ADMIN role) ---
      {
        element: <AdminRoute />,
        children: [
          { path: "/admin",              element: <AdminDashboardPage /> },
          { path: "/admin/users",        element: <UsersListPage /> },
          { path: "/admin/users/:uuid",  element: <UserDetailPage /> },
          { path: "/admin/roles",        element: <RolesListPage /> },
          { path: "/admin/permissions",  element: <PermissionsListPage /> },
          { path: "/admin/system",       element: <SystemOverviewPage /> },
        ],
      },
    ],
  },

  // --- Fallback ---
  { path: "*", element: <NotFoundPage /> },
]);
```

---

## 8 — LAYOUT SYSTEM

### 8.1 PublicLayout
- Clean centered layout.
- Minimal header with logo, "Login" and "Register" links.
- Footer with copyright.
- Used by landing, login, and register pages.

### 8.2 AppLayout (authenticated shell)
- **Top bar:** logo, global search input (placeholder), notification bell icon, user dropdown (avatar + username + role badges + logout).
- **Left sidebar:** collapsible, role-aware navigation.
- **Content area:** scrollable, uses `<Outlet />`.
- **Mobile:** sidebar becomes a slide-in drawer triggered by hamburger icon.

### 8.3 Sidebar Navigation (role-aware)

**All authenticated users see:**
- Dashboard (`/dashboard`) — icon: LayoutDashboard
- Marketplace (`/marketplace`) — icon: Store
- My Offers (`/offers`) — icon: ArrowLeftRight
- Messages (`/messages`) — icon: MessageSquare
- Profile (`/profile`) — icon: User

**MODERATOR users additionally see:**
- Section header: "Moderation"
- Reports (placeholder) — icon: Flag
- Review Queue (placeholder) — icon: ClipboardList

**ADMIN users additionally see:**
- Section header: "Administration"
- Admin Dashboard (`/admin`) — icon: BarChart3
- Users (`/admin/users`) — icon: Users
- Roles (`/admin/roles`) — icon: Shield
- Permissions (`/admin/permissions`) — icon: Key
- System (`/admin/system`) — icon: Server

Use a visual separator between navigation sections.
Active route should be highlighted with a colored background and bold text.

---

## 9 — PAGE SPECIFICATIONS

### 9.1 Landing Page (`/`)
- Hero section with headline "Trade What You Have for What You Need".
- Subtext about the platform's mission.
- CTA buttons: "Get Started" → `/register`, "Browse Marketplace" → `/marketplace`.
- Feature highlights grid (3–4 cards): Easy Trading, Secure Platform, Community Driven, No Money Needed.
- Modern gradient background or subtle pattern.

### 9.2 Login Page (`/login`)
- Centered card on PublicLayout.
- Fields: Identifier (username or email), Password.
- "Login" button (primary, full-width).
- "Don't have an account? Register" link → `/register`.
- Inline field validation errors from backend `fieldErrors[]`.
- Loading spinner on button during submission.
- On success → redirect to `/dashboard`.

### 9.3 Register Page (`/register`)
- Centered card on PublicLayout.
- Fields: Username, Email, Password, Confirm Password.
- Client-side validation with Zod: username 3–80 chars, valid email, password min 8 chars with complexity, passwords match.
- Map backend `fieldErrors` inline under each field.
- On 409 → toast "Username or email already taken".
- On success → toast "Account created! Please log in." → redirect to `/login`.

### 9.4 Dashboard Page (`/dashboard`)
- Welcome card: "Welcome back, {username}!" with user avatar placeholder.
- Quick actions row: "Create Listing" (placeholder), "Browse Marketplace" (placeholder), "View Offers" (placeholder).
- Recent activity section (placeholder — shows empty state for now).
- If user is ADMIN → also show a card linking to Admin Dashboard.

### 9.5 Profile Page (`/profile`)
- Display current user info from AuthContext (no extra API call needed).
- Card with: avatar placeholder, username, email, status badge, email verified badge, MFA status, account created date.
- Roles list with badges.
- "Edit Profile" button (disabled/placeholder for now).

### 9.6 Admin Dashboard (`/admin`)
- Stats cards row: Total Users, Active Users, Suspended Users, Banned Users (all placeholders with static/mock counts — real stats endpoint is not built yet; display "—" or 0).
- Quick links: "Manage Users", "View Roles", "System Status".
- Recent admin activity (empty state placeholder).

### 9.7 Users List Page (`/admin/users`)
- Title: "User Management".
- `<DataTable>` displaying user data from `GET /users`.
- Columns: Username, Email, Status (badge), Email Verified (icon), MFA (icon), Created At, Actions.
- Pagination controls below table: page selector, page size dropdown (10/20/50), total count display.
- Sort by clicking column headers (sends `sort=field,dir` to API).
- Loading skeleton rows while fetching.
- Empty state component when no users found.
- Row click → navigate to `/admin/users/{uuid}`.
- Status column uses colored `<StatusBadge>`:
  - ACTIVE → green
  - PENDING_VERIFICATION → yellow
  - SUSPENDED → orange
  - BANNED → red
  - DELETED → gray

### 9.8 User Detail Page (`/admin/users/:uuid`)
- Fetch user via `GET /users/{userUuid}`.
- Full user profile card: avatar placeholder, username, email, UUID, status, created/updated dates.
- Roles section listing assigned roles with badges.
- "Change Status" button → opens `<UpdateStatusModal>` with a select for new status and confirm button.
- Status change calls `PATCH /users/{userUuid}/status` with `{ status: "NEW_STATUS" }`.
- Success → toast "User status updated" + invalidate TanStack Query cache.
- Back button → `/admin/users`.

### 9.9 Roles List Page (`/admin/roles`)
- Fetch roles via `GET /roles`.
- Table: Code, Name, Description.
- Read-only view — no inline editing.

### 9.10 Permissions List Page (`/admin/permissions`)
- Fetch permissions via `GET /permissions`.
- Table: Code, Name, Description.
- Read-only view.

### 9.11 System Overview Page (`/admin/system`)
- Call `GET /ping` to verify backend is reachable.
- Display: API status (green/red indicator), backend base URL, timestamp of last check.
- "Refresh" button to re-ping.

### 9.12 Placeholder Pages (marketplace, offers, messages)
- Each shows an `<EmptyState>` component with:
  - Illustration icon.
  - "Coming Soon" title.
  - Description text explaining the feature is under development.

### 9.13 Not Found Page (`*`)
- Large "404" heading.
- "Page not found" message.
- "Go Home" button → `/`.

---

## 10 — REUSABLE COMPONENT SPECIFICATIONS

### 10.1 DataTable
```tsx
interface DataTableProps<T> {
  data: T[];
  columns: ColumnDef<T>[];
  isLoading: boolean;
  pagination: {
    page: number;
    size: number;
    totalElements: number;
    totalPages: number;
    first: boolean;
    last: boolean;
  };
  sort: string;                    // e.g. "username,asc"
  onPageChange: (page: number) => void;
  onSizeChange: (size: number) => void;
  onSortChange: (sort: string) => void;
  onRowClick?: (item: T) => void;
  emptyMessage?: string;
}
```
- Renders skeleton rows when `isLoading` is true.
- Renders `<EmptyState>` when `data` is empty and not loading.
- Pagination component below the table.

### 10.2 Pagination
- Shows: "Showing X–Y of Z results".
- Page navigation buttons: First, Previous, Page numbers, Next, Last.
- Page size selector: dropdown with 10, 20, 50 options.

### 10.3 StatusBadge
- Maps `UserStatus` enum to colored pill badges.
- Uses Tailwind color classes.

### 10.4 EmptyState
- Centered icon + title + description + optional action button.

### 10.5 FormInput / FormSelect
- Wraps React Hook Form `Controller`.
- Displays label, input, and error message.
- `error` prop accepts a string (sourced from Zod or from backend `fieldErrors`).

### 10.6 Modal
- Overlay + centered card.
- Title, body (children), footer with cancel/confirm buttons.
- Closes on overlay click and Escape key.

### 10.7 Button
- Variants: `primary`, `secondary`, `danger`, `ghost`.
- Sizes: `sm`, `md`, `lg`.
- Loading state: replaces text with spinner.
- Disabled state.

### 10.8 Card
- Rounded corners, subtle shadow, padding.
- Optional `title` and `description` header.

---

## 11 — API INTEGRATION LAYER

### 11.1 Axios Client (`src/api/client.ts`)
- Create an axios instance with `baseURL` from `import.meta.env.VITE_API_BASE_URL`.
- Default headers: `Content-Type: application/json`.
- Request interceptor: attach `Authorization: Bearer {token}` from localStorage.
- Response interceptor: on 401, attempt silent token refresh (one retry), then reject.

### 11.2 Error Handling Utility (`src/utils/error.ts`)
```ts
interface ParsedApiError {
  code: string;
  message: string;
  fieldErrors: { field: string; message: string }[];
  status: number;
}
function parseApiError(error: unknown): ParsedApiError;
```
- Extracts `ErrorResponse` from axios error response body.
- Falls back to generic message if body is missing.

### 11.3 TanStack Query Setup
- Configure `QueryClient` with sensible defaults:
  - `staleTime: 5 * 60 * 1000` (5 minutes)
  - `retry: 1`
  - `refetchOnWindowFocus: false`
- Global `onError` → show toast via Sonner.
- Wrap app in `<QueryClientProvider>`.

### 11.4 Query Key Convention
Use arrays: `["users"]`, `["users", uuid]`, `["roles"]`, `["permissions"]`, `["auth", "me"]`.

### 11.5 OpenAPI Client Generation Script
Add to `package.json` scripts:
```json
{
  "generate:api": "openapi-generator-cli generate -i ../backend/barter-api/src/main/resources/openapi/openapi.yaml -g typescript-axios -o src/api/generated --additional-properties=supportsES6=true,withInterfaces=true"
}
```

Until generation is wired, create manual TypeScript interfaces in `src/api/generated/` that match the schemas in Section 4 exactly. Mark them with `// AUTO-GENERATED — will be replaced by openapi-generator output` comments.

---

## 12 — ERROR HANDLING & FEEDBACK UX

### 12.1 Form Validation Errors
- Client-side: Zod schemas validate before submission.
- Server-side: if API returns `400` with `fieldErrors[]`, map each `{ field, message }` to the corresponding form field inline error.
- Use `<FormFieldError>` component under each input.

### 12.2 Toast Notifications (Sonner)
- **Success:** green toast — "Logged in successfully", "User status updated", "Account created".
- **Error:** red toast — API error `message` or fallback "Something went wrong".
- **Info:** blue toast — "Session expired, please log in again".
- Position: bottom-right.
- Auto-dismiss: 5 seconds.

### 12.3 Loading States
- Full-screen spinner during auth bootstrap.
- Skeleton rows in data tables.
- Button spinner during form submission (disable button).
- Content area skeleton for page-level data fetching.

### 12.4 Empty States
- Friendly illustration + message when no data exists.
- Example: "No users found" in admin users table, "No activity yet" on dashboard.

---

## 13 — STYLING & DESIGN SYSTEM

### 13.1 Tailwind Configuration
- Dark-mode-ready: use `class` strategy (`darkMode: "class"`).
- Custom color palette:
  - Primary: indigo-600 / indigo-500 (dark mode).
  - Accent: violet-500.
  - Success: emerald-500.
  - Warning: amber-500.
  - Danger: red-500.
  - Neutral: slate scale for backgrounds and text.
- Default font: Inter (Google Fonts import) or system font stack.
- Border radius default: `0.5rem`.

### 13.2 Design Direction
- Inspired by: Linear, Notion, GitHub, modern SaaS admin dashboards.
- Rounded corners on cards, inputs, buttons.
- Subtle box shadows (`shadow-sm`, `shadow`).
- Clean sans-serif typography with clear hierarchy.
- Spacious layout with consistent padding (e.g., `p-6` for cards, `gap-6` for grids).
- Subtle hover/focus transitions (150ms ease).
- Sidebar: dark background (slate-900) with light text; active item uses primary color background.
- Top bar: white/light background with bottom border.

### 13.3 Responsive Breakpoints
- Mobile-first approach.
- `sm` (640px): stacked layouts.
- `md` (768px): sidebar visible, tables scroll horizontally.
- `lg` (1024px): full sidebar + content.
- `xl` (1280px): max-width container for content.

### 13.4 Dark Mode Readiness
- All components must use Tailwind's `dark:` variant classes.
- Do NOT implement the dark mode toggle yet — just ensure all styles include `dark:` counterparts.
- Example: `bg-white dark:bg-slate-900`, `text-slate-900 dark:text-slate-100`.

### 13.5 Unified Design System Consistency
- All pages and components must follow a single, unified design system.
- Spacing, typography, card styles, button variants, table formatting, layout grids, and navigation must remain visually consistent across the entire application.
- Prefer reusable UI patterns and shared component primitives over one-off or page-specific styling.
- Do not mix design languages — every screen should feel like it belongs to the same product.

### 13.6 Responsive Admin Tables
- Admin data tables must remain usable on tablet and mobile viewports.
- Use responsive horizontal overflow (`overflow-x-auto`), adaptive column hiding, stacked card layouts on small screens, or mobile-friendly row rendering.
- Critical columns (e.g., username, status, actions) must remain visible at all breakpoints.

---

## 14 — CRITICAL RULES

1. **Do NOT invent or mock backend API endpoints** that are not listed in Section 3. Where data doesn't exist yet (e.g., admin stats counts), display placeholder values ("—" or 0) with a comment indicating the real endpoint is pending.

2. **Do NOT create mock API services.** Wire all data fetching hooks to real API calls using the axios client. Components that depend on future endpoints should render empty/placeholder states.

3. **All TypeScript types for API models must be imported from `src/api/generated/`.** Provide temporary manual type definitions that exactly match Section 4 schemas until openapi-generator is configured.

4. **Never expose internal numeric database IDs.** Always use `uuid` as the public identifier.

5. **Pagination parameters must match the backend contract exactly:** `page` (0-indexed), `size` (default 20, max 100), `sort` (format: `field,direction`).

6. **The `ErrorResponse` shape is a backend contract.** Always parse it through `parseApiError()`. Never assume a different error format.

7. **Role checks must use the `roles[]` array from `CurrentUserResponse`**, checking `role.code` values (`USER`, `MODERATOR`, `ADMIN`).

8. **The admin panel is NOT a separate app** — it is additional navigation sections and routes within the same shell, visible only to ADMIN users.

9. **Use feature-based file organization.** Each feature is self-contained with its own pages, hooks, components, and services.

10. **Write clean, production-grade code.** No `any` types. No `console.log` left in code. Proper error boundaries. Accessible HTML semantics. ARIA labels where needed.

11. **The generated frontend must feel like a real production SaaS platform.** Avoid generic demo-dashboard UI patterns. Do not render fake enterprise charts, widgets, or analytics disconnected from real backend functionality. Every visible element should serve a real purpose or be an explicit placeholder for a planned feature.

12. **When backend functionality is not yet implemented, use realistic empty states and placeholders.** Show "Coming Soon" states, "No data yet" messages, or disabled controls — never fake/mock business data unless explicitly required by the specification.

13. **Authentication must use the real backend JWT endpoints** (`/auth/login`, `/auth/register`, `/auth/me`, `/auth/refresh`, `/auth/logout`). Do not implement fake auth, hardcoded sessions, demo-mode logins, or mocked authentication providers.

---

## 15 — PACKAGE.JSON DEPENDENCIES

```json
{
  "dependencies": {
    "react": "^18.3.0",
    "react-dom": "^18.3.0",
    "react-router-dom": "^6.28.0",
    "@tanstack/react-query": "^5.60.0",
    "react-hook-form": "^7.54.0",
    "@hookform/resolvers": "^3.9.0",
    "zod": "^3.23.0",
    "axios": "^1.7.0",
    "lucide-react": "^0.460.0",
    "sonner": "^1.7.0",
    "clsx": "^2.1.0",
    "tailwind-merge": "^2.6.0"
  },
  "devDependencies": {
    "@types/react": "^18.3.0",
    "@types/react-dom": "^18.3.0",
    "@vitejs/plugin-react": "^4.3.0",
    "autoprefixer": "^10.4.0",
    "postcss": "^8.4.0",
    "tailwindcss": "^3.4.0",
    "typescript": "^5.6.0",
    "vite": "^6.0.0"
  }
}
```

---

## 16 — SUMMARY OF WHAT TO GENERATE

Generate every file listed in the project structure (Section 2). Every component, hook, page, layout, route, utility, and configuration file must be fully implemented with real logic, real API integration, and production-quality TypeScript code. The output should be a complete, runnable frontend application that connects to the existing backend out of the box.

Prioritize fully implemented foundational flows and reusable architecture over placeholder-only code. Do not skip files. Do not add features or endpoints beyond what is specified. Follow every specification precisely.

---

## 17 — GENERATION PRIORITY ORDER

When generating, prioritize in this order:

1. **Application architecture correctness** — project structure, configuration, dependency wiring.
2. **Authentication flow correctness** — login, register, token management, session bootstrap, logout.
3. **API integration structure** — axios client, interceptors, TanStack Query setup, error handling.
4. **Reusable layout and component system** — layouts, sidebar, UI primitives, data table, forms.
5. **Role-aware routing and navigation** — route guards, admin routes, dynamic sidebar sections.
6. **Responsive UX** — mobile layouts, adaptive tables, drawer navigation.
7. **Visual polish** — design consistency, transitions, dark-mode-ready classes, typography.
````


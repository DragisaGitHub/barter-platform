You are a senior frontend architect. Generate a production-grade React frontend for the Barter Platform — a modern barter marketplace with admin control panel in the same shell. Backend exists, OpenAPI-first. Integrate via generated TypeScript client — never duplicate DTOs. All components must be reusable and part of a scalable design system.

TECH STACK (mandatory)
React 18+ | TypeScript strict | Vite | Tailwind 3+ | React Router v6 | TanStack Query v5 | React Hook Form+Zod | Axios | Lucide React | Sonner | Context+useReducer auth

ARCHITECTURE (feature-based, scalable)
- src/api/ — axios (env base URL), auth+error interceptors, generated/ for OpenAPI types
- src/auth/ — AuthContext/Provider, useAuth, token.service, useAuthGuard, useRoleGuard
- src/components/ — ui/ (Button, Input, Badge, Card, Modal, Spinner, StatusBadge, EmptyState, Skeleton), data/ (DataTable paginated+sortable, Pagination, SortHeader), forms/ (FormInput, FormSelect via RHF)
- src/layouts/ — PublicLayout, AppLayout (topbar+sidebar+Outlet), role-aware Sidebar, MobileDrawer
- src/features/ — landing, auth, dashboard, profile, admin/{users,roles,permissions,system}, placeholders
- src/routes/ — createBrowserRouter, ProtectedRoute, AdminRoute, routePaths
- src/utils/ — cn, parseApiError

API ENDPOINTS (base: /api/v1 — do NOT invent others)
POST /auth/register {username,email,password} → 201 CurrentUserResponse
POST /auth/login {identifier,password} → 200 TokenResponse {accessToken,refreshToken}
GET /auth/me (Bearer) → 200 CurrentUserResponse {uuid,username,email,status,roles[{code,name}],permissions[{code,name}]}
POST /auth/refresh {refreshToken} → 200 TokenResponse
POST /auth/logout {refreshToken} → 204
GET /users?page&size&sort (Bearer) → UserPagedResponse {content[],page,size,totalElements,totalPages,first,last,sort}
GET /users/{uuid} (Bearer) → UserResponse
PATCH /users/{uuid}/status {status} (Bearer) → UserResponse
GET /roles → RoleResponse[] | GET /permissions → PermissionResponse[] | GET /ping → PingResponse

ErrorResponse: {timestamp,status,error,code,message,path,fieldErrors[{field,message}]}
UserStatus: PENDING_VERIFICATION | ACTIVE | SUSPENDED | BANNED | DELETED
Roles: USER | MODERATOR | ADMIN

AUTH FLOW (real JWT only — no fake/mock/demo auth)
- Login: POST /auth/login → store tokens → GET /auth/me → populate context → redirect /dashboard
- Bootstrap: localStorage → /auth/me → 401 try /auth/refresh → fail = clear+redirect /login. LoadingScreen during.
- Interceptor: on 401 one silent refresh with mutex, retry or redirect
- Register: POST /auth/register → 201 redirect /login, 400 fieldErrors inline, 409 toast
- Logout: POST /auth/logout → clear tokens → reset context → redirect /

ROUTING
Public (PublicLayout): /, /login, /register
Protected (AppLayout+ProtectedRoute): /dashboard, /profile, /marketplace, /offers, /messages
Admin (AdminRoute, ADMIN): /admin, /admin/users, /admin/users/:uuid, /admin/roles, /admin/permissions, /admin/system
Fallback: 404

SIDEBAR (role-aware, same shell)
All users: Dashboard, Marketplace, My Offers, Messages, Profile
ADMIN adds "Administration": Admin Dashboard, Users, Roles, Permissions, System
Active highlighted. Collapsible desktop, drawer mobile.

KEY PAGES
- Landing: hero "Trade What You Have for What You Need", CTAs register/marketplace, feature cards
- Login/Register: centered cards, Zod validation, fieldErrors inline, loading states
- Dashboard: welcome card, quick actions (placeholders), empty state, admin link if ADMIN
- Admin Users: DataTable pagination+sort, StatusBadge (green ACTIVE, yellow PENDING, orange SUSPENDED, red BANNED, gray DELETED), row→detail
- User Detail: profile card, roles badges, "Change Status" Modal → PATCH → invalidate cache → toast
- Roles/Permissions: read-only tables | System: /ping indicator+refresh
- Placeholders: EmptyState "Coming Soon"

DESIGN SYSTEM
Primary indigo-600, accent violet-500, success emerald, warning amber, danger red, slate neutrals. Linear/Notion/GitHub — rounded, subtle shadows, Inter font. Dark-mode ready (dark: variants, no toggle). Mobile-first. Sidebar slate-900, topbar white. Consistent spacing, 150ms transitions. Unified language across all pages.

CRITICAL RULES
1. NO mock/fake API — all hooks wire to real axios calls
2. NO fake auth — real JWT endpoints only
3. Types from src/api/generated/ (manual interfaces until codegen)
4. Pagination: page 0-indexed, size 20, sort "field,direction"
5. Errors via parseApiError() per ErrorResponse contract
6. Admin is NOT separate — role-gated routes+sidebar same shell
7. UUIDs only, never numeric IDs
8. No console.log, no any, semantic HTML, ARIA
9. Missing data → empty states, never fake data/charts
10. Every element: real purpose or explicit placeholder

GENERATION PRIORITY
1. Architecture+config → 2. Auth flow → 3. API layer → 4. Layouts+components → 5. Role-aware routing → 6. Responsive polish
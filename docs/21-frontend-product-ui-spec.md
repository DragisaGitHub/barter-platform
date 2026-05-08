# Frontend Product & UI Specification

## Product Vision

The platform is a modern item exchange/barter marketplace.

Users can:
- register
- authenticate
- manage profiles
- create listings
- browse listings
- negotiate trades
- communicate
- manage offers

The application must support both:
- public/user-facing experience
- administrative control panel

The UI must feel modern, responsive, fast, and enterprise-grade.

---

# Main Application Areas

## 1. Public Area

Accessible without authentication.

Pages:
- Landing page
- Login
- Register
- Public marketplace browsing
- About
- Terms/Privacy

Goals:
- modern product presentation
- onboarding
- conversion to registration

---

## 2. Authenticated User Area

Accessible to authenticated users.

Core features:
- Dashboard
- Profile management
- My listings
- Favorites/watchlist
- Messages
- Trade offers
- Notifications
- Settings

The authenticated area should feel similar to:
- modern SaaS dashboards
- Discord
- Notion
- Linear
- GitHub
- modern marketplace applications

---

## 3. Administrative Area

Visible only to users with ADMIN role.

ADMIN users must:
- see normal user UI
- also see an Admin Control Panel

The admin experience should feel inspired by:
- Liferay Control Panel
- enterprise administration portals
- modern operations dashboards

Admin UI should NOT be separated into another application.

Instead:
- a shared shell/layout should exist
- ADMIN users receive additional navigation sections
- role-aware navigation must dynamically appear

---

# Role-Aware Navigation

## USER

Can see:
- dashboard
- profile
- marketplace
- offers
- messages
- notifications

## MODERATOR

Additional sections:
- reports
- moderation queue
- user review tools

## ADMIN

Additional sections:
- admin dashboard
- user management
- role management
- permission overview
- system overview
- audit/logs
- platform settings

---

# Application Layout

## Main Layout

Structure:

- Top navigation bar
- Left sidebar navigation
- Main content area
- Responsive/mobile support
- Notification area
- User/account dropdown

The layout must support:
- collapsing sidebar
- responsive mobile drawer
- breadcrumbs
- search bar
- dark/light mode later

---

# Dashboard

## User Dashboard

Widgets:
- recent activity
- recent offers
- notifications
- saved/favorite listings
- recommendations

## Admin Dashboard

Widgets:
- total users
- active users
- banned users
- reports count
- moderation queue
- system activity
- audit events
- platform statistics

---

# Authentication UX

Flows:
- register
- login
- token refresh
- logout
- protected routes

Behavior:
- automatic redirect after login
- automatic logout on invalid token
- persistent session
- role-aware route visibility

---

# Error Handling UX

Frontend must support backend ErrorResponse structure:

- code
- message
- fieldErrors

Validation errors should:
- display inline
- highlight fields
- show backend validation messages cleanly

Global errors:
- toast/snackbar notifications
- friendly error cards where appropriate

---

# Pagination & Tables

The frontend must support backend pagination contract:
- page
- size
- sort

Admin tables should support:
- sorting
- pagination
- filtering
- search
- empty states
- loading states

---

# Visual Style

The application should feel:
- modern
- premium
- responsive
- clean
- minimal but powerful

Inspirations:
- Linear
- Notion
- Discord
- GitHub
- modern SaaS admin dashboards

Preferred style:
- rounded corners
- soft shadows
- clean typography
- spacious layout
- subtle animations
- modern cards/tables

---

# Technical Frontend Direction

Stack:
- React
- TypeScript
- Vite
- Tailwind CSS
- TanStack Query
- React Router
- OpenAPI generated TypeScript client

Architecture:
- feature-based organization
- API-driven
- reusable UI components
- centralized auth/session handling
- centralized API layer

---

# First Frontend Milestone

## Authentication Foundation

Pages:
- Landing
- Login
- Register
- Dashboard shell

Features:
- login
- register
- JWT handling
- auth/me bootstrap
- protected routes
- role-aware navigation

---

# Future Features

Planned:
- item marketplace
- barter/trade system
- chat/messages
- notifications
- moderation tools
- audit logs
- MFA
- OAuth login
- email verification
- media/file uploads
- advanced search
- recommendations
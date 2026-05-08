# Frontend Architecture

## Goal

Create a modern React frontend that follows the backend OpenAPI contract.

Frontend must be:
- functional
- clean
- scalable
- API-driven
- role-aware
- mobile-friendly

## Stack

- React
- TypeScript
- Vite
- Tailwind CSS
- React Router
- TanStack Query
- OpenAPI generated TypeScript client

## Main Rule

Do not manually duplicate backend DTO models.

All API models should come from generated OpenAPI TypeScript client.

## Planned Structure

frontend/
src/
app/
api/
auth/
components/
features/
layouts/
pages/
routes/
styles/
types/
utils/

## Auth Flow

- register
- login
- store access token
- store refresh token
- load current user through /auth/me
- logout
- protected routes

## Role-Based UI

Frontend should support:
- USER
- MODERATOR
- ADMIN

## First Pages

- Landing page
- Register page
- Login page
- Dashboard
- My profile
- Users admin page

## Error Handling

Use backend ErrorResponse format:
- code
- message
- fieldErrors

## Pagination

Use backend pagination contract:
- page
- size
- sort

## Implementation Order

1. Create Vite React skeleton
2. Configure Tailwind
3. Configure OpenAPI TypeScript client generation
4. Configure API client
5. Implement auth store/context
6. Implement routes
7. Implement first pages
8. Add modern UI polish
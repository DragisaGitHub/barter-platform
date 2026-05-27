# Barter Platform Frontend

A modern, production-ready React frontend for a barter marketplace with integrated admin control panel.

## Tech Stack

- **React 18+** with TypeScript (strict mode)
- **Vite** for blazing-fast development
- **Tailwind CSS 4** for styling
- **React Router v6** for navigation
- **TanStack Query v5** for server state management
- **React Hook Form + Zod** for form validation
- **Axios** for API communication
- **Lucide React** for icons
- **Sonner** for toast notifications

## Architecture

The project follows a feature-based architecture:

```
src/
├── api/              # API client & generated types
├── auth/             # Authentication context & services
├── components/       # Reusable UI components
│   ├── ui/          # Base UI components (Button, Input, Card, etc.)
│   ├── data/        # Data display components (DataTable, Pagination)
│   └── forms/       # Form components
├── features/         # Feature modules
│   ├── landing/     # Landing page
│   ├── auth/        # Login & Register
│   ├── dashboard/   # User dashboard
│   ├── admin/       # Admin pages
│   └── placeholder/ # Placeholder pages
├── layouts/          # Layout components
├── routes/           # Routing configuration
└── utils/            # Utility functions
```

## Getting Started

### Prerequisites

- Node.js 18+ and pnpm
- Backend API running (see backend repository)

### Installation

1. Clone the repository
2. Install dependencies:
   ```bash
   pnpm install
   ```

3. Create `.env` file (copy from `.env.example`):
   ```bash
   cp .env.example .env
   ```

4. Configure environment variables:
   ```
   VITE_API_BASE_URL=http://localhost:8080/api/v1
   ```

### Development

```bash
# The dev server is already running in Figma Make
# Access the preview in the Make interface
```

## API Integration

The frontend integrates with the backend via REST API using OpenAPI-generated TypeScript types:

- **Base URL**: `/api/v1`
- **Authentication**: JWT Bearer tokens
- **Token Refresh**: Automatic with retry on 401

### Key Endpoints

- `POST /auth/register` - User registration
- `POST /auth/login` - User login
- `GET /auth/me` - Get current user
- `POST /auth/refresh` - Refresh access token
- `GET /users` - List users (paginated)
- `PATCH /users/{uuid}/status` - Update user status

## Authentication Flow

1. **Login**: POST /auth/login → store tokens → GET /auth/me → populate context
2. **Bootstrap**: localStorage → /auth/me → on 401 try /auth/refresh → fail = redirect /login
3. **Auto-refresh**: On 401 responses, automatically try token refresh once
4. **Logout**: POST /auth/logout → clear tokens → redirect /login

### Current token-storage posture

- **Current implementation**: access token + refresh token are persisted in `localStorage`.
- **Why it remains for now**: this preserves the current SPA login/bootstrap UX and avoids a broader auth redesign during the production-hardening pass.
- **Current mitigation**: short-lived access tokens, server-side refresh-token hashing + revocation, logout revocation, and stricter backend security headers.
- **Known limitation**: `localStorage` tokens are still readable by injected JavaScript if XSS occurs.
- **Deferred roadmap option**: move the refresh token to a secure `httpOnly` cookie and keep the access token short-lived, ideally in memory.

## Features

### Public Pages
- Landing page with feature showcase
- Login and registration with validation
- Responsive design

### Protected Pages
- Dashboard with quick actions
- Marketplace (placeholder)
- My Offers (placeholder)
- Messages (placeholder)
- Profile (placeholder)

### Admin Panel (ADMIN role required)
- User management with pagination & sorting
- User detail view with status management
- Roles and permissions viewing
- System health monitoring

## Design System

- **Primary**: Indigo 600
- **Accent**: Violet 500
- **Success**: Emerald
- **Warning**: Amber
- **Danger**: Red
- **Neutrals**: Slate
- **Font**: Inter
- **Dark mode**: Ready (dark: variants)

## Code Quality

- TypeScript strict mode
- No `any` types
- Semantic HTML with ARIA attributes
- Mobile-first responsive design
- 150ms transitions for smooth UX
- Proper error handling with user-friendly messages

## Security

- All API calls authenticated with JWT
- XSS protection via React's escaping
- CSRF protection via token-based auth
- Input validation with Zod schemas
- Token persistence currently uses `localStorage` for pragmatic SPA UX; this is a documented tradeoff, not the ideal end-state
- Refresh tokens are rotated and revoked server-side; revoked or expired refresh attempts return `401`

## Contributing

1. Follow the existing architecture patterns
2. Use TypeScript strict mode
3. Write semantic, accessible HTML
4. Test on mobile and desktop viewports
5. No console.log in production code

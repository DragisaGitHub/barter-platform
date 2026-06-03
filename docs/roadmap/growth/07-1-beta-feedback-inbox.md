# Growth 07.1 — Beta Feedback Inbox

> Source: Growth 07 follow-up
> Priority: P1 beta testing / admin visibility
> Execution mode: one controlled Agent-mode implementation unit.

# Goal

- Make beta feedback visible and actionable for admins.
- Persist submitted beta feedback instead of logging it only.
- Allow admins to review and resolve submitted feedback.
- Protect the feedback endpoint from spam using the existing rate-limiting system.
- Preserve the existing OpenAPI-first architecture.

# Why It Matters

Growth 07 added a beta feedback entry point.

However, feedback currently only gets logged by the backend.

That is not enough for closed beta testing.

During beta, feedback must be easy to collect, review, and process.

Admins need a simple inbox where they can see:

- who submitted feedback
- what category it belongs to
- what page it came from
- what the user wrote
- whether it is new, reviewed, or resolved

# Current State

The app currently has:

- POST /api/v1/feedback/beta
- frontend feedback page
- OpenAPI-generated feedback request DTO
- backend feedback controller implementing the generated API
- backend service that logs feedback
- existing rate-limiting infrastructure

The current feedback service does not persist feedback.

There is no admin feedback page.

There is no feedback status workflow.

# Risks

## Product Risks

- Making feedback review too complex
- Mixing beta feedback with abuse reports
- Treating feedback as a full ticketing system too early

## Technical Risks

- Creating ad-hoc backend request/response classes outside OpenAPI
- Creating frontend API calls outside generated API conventions
- Duplicating existing rate-limiting logic

## Rollout Risks

- Admin users need to clearly distinguish feedback from reports
- Existing feedback submission behavior must remain stable

# Proposed Solution

Introduce a lightweight beta feedback inbox.

Workflow:

1. User submits beta feedback.
2. Backend validates the request.
3. Existing rate limiting protects the endpoint.
4. Feedback is stored in the database with status NEW.
5. Admin opens the Beta Feedback page.
6. Admin marks feedback as REVIEWED or RESOLVED.

# Feedback Statuses

Use a simple status enum:

- NEW
- REVIEWED
- RESOLVED

# Backend Changes

Follow the existing OpenAPI-first architecture.

Add or update OpenAPI contract for:

## User Endpoint

POST /api/v1/feedback/beta

The existing endpoint remains.

It should persist feedback and return MessageResponse.

## Admin Endpoints

GET /api/v1/admin/feedback/beta

PATCH /api/v1/admin/feedback/beta/{feedbackUuid}/status

Admin list endpoint should support pagination and optional status filtering if consistent with existing admin conventions.

Status update endpoint should allow admins to mark feedback as:

- REVIEWED
- RESOLVED

Add generated DTOs/interfaces through OpenAPI.

Do not create ad-hoc web-layer request records.

# Database Changes

Add Flyway migration for beta feedback storage.

Suggested table:

beta_feedback

Suggested columns:

- id
- uuid
- user_uuid
- username
- email
- category
- message
- source_page
- status
- created_at
- reviewed_at
- resolved_at

Notes:

- user_uuid should reference the submitting user if consistent with existing schema conventions.
- email can be nullable.
- category should use the existing generated feedback category enum.
- status should default to NEW.
- message should have a reasonable length limit.
- source_page should be nullable.
- created_at should be set when feedback is submitted.
- reviewed_at should be set when status becomes REVIEWED.
- resolved_at should be set when status becomes RESOLVED.

# Rate Limiting

Use the existing rate-limiting infrastructure.

Do not create a second rate-limiting system.

Add a feedback-specific policy for:

POST /api/v1/feedback/beta

Recommended beta limit:

5 submissions per authenticated user/IP per hour.

Add or update tests for the rate limit.

# Application Layer

Add feedback persistence through normal project layering.

Expected structure should follow existing backend conventions:

- domain/entity
- infrastructure/repository
- application/service
- application/service/impl
- mapper if needed
- web/controller implementing generated API

The service should:

- resolve current user details
- validate category/message/source page
- persist feedback as NEW
- return MessageResponse

Admin service methods should:

- list feedback
- update status
- set reviewed_at/resolved_at timestamps when appropriate

# Frontend Changes

Regenerate frontend API/types from OpenAPI.

Update beta feedback submission to continue using generated request/response types.

Add admin UI page:

Admin -> Beta Feedback

The admin page should show:

- status
- category
- message preview
- username
- source page
- created at
- actions

Actions:

- Mark as reviewed
- Mark as resolved

Use existing admin page/layout conventions.

Only admins should see this page.

# Testing Strategy

## Backend

- feedback submission persists NEW feedback
- feedback submission returns MessageResponse
- admin can list feedback
- admin can update status to REVIEWED
- admin can update status to RESOLVED
- non-admin users cannot access admin feedback endpoints
- rate limit applies to POST /api/v1/feedback/beta

## Frontend

- feedback page still submits successfully
- admin feedback page loads list
- admin can change status
- non-admin navigation does not expose admin feedback page
- EN/SR translations exist

## Manual

- submit feedback as normal user
- login as admin
- verify feedback appears in admin page
- mark as reviewed
- mark as resolved
- verify rate limit behavior

# Rollout Plan

## Phase 1

Add database table and backend persistence.

## Phase 2

Add admin OpenAPI endpoints.

## Phase 3

Add rate limiting policy.

## Phase 4

Add admin frontend page.

## Phase 5

Manual verification on DEV.

# Future Improvements

- feedback priority
- admin comments
- assignment to admin
- screenshot upload
- feedback export
- email digest for admins
- notification to admin users
- feedback analytics

# Explicitly Deferred

Not part of this unit:

- email notifications
- in-app admin notifications
- ticketing workflow
- abuse/report workflow merge
- file attachments
- screenshots
- user-visible feedback status
- public feedback board
- voting
- labels/tags beyond category/status
- SLA tracking
- external integrations
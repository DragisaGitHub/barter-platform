# Pagination & Search Strategy

## Goal

Define a standard approach for search, pagination, sorting and future filtering.

All list/search endpoints must use a consistent contract.

---

# API Rules

List endpoints must support:

- page
- size
- sort

Default values:

- page = 0
- size = 20

Maximum size:

- size <= 100

---

# Sorting

Sort format:

    field,direction

Examples:

    createdAt,desc
    username,asc

Multiple sort values may be supported later.

---

# Filtering

Filtering should be endpoint-specific.

Avoid one generic filtering model too early.

Examples:

Users search may later support:

- username
- email
- status
- role

Items search may later support:

- category
- tags
- city
- status
- condition

---

# Response Shape

Paged responses should contain:

- content
- page
- size
- totalElements
- totalPages
- first
- last
- sort

---

# Backend Rules

Controllers should not build pageable logic manually.

Application layer should use shared pagination helpers.

Repositories should rely on Spring Data Pageable where possible.

---

# Implementation Order

1. Define reusable OpenAPI pagination parameters
2. Define paged response schemas
3. Update list endpoints gradually
4. Add common backend page mapping helper
5. Refactor existing listUsers first
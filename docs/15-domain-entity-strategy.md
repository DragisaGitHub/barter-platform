# Domain Entity Strategy

## Goal

Define entity modeling strategy for the backend domain layer.

The system prioritizes:
- scalability
- predictable persistence
- low Hibernate complexity
- explicit query behavior
- maintainability

---

# Main Principle

Prefer lean entities over large object graphs.

Avoid deep bidirectional relationship trees.

---

# Preferred Modeling Style

Prefer:

- explicit foreign keys
- join entities
- query-driven loading
- service-layer orchestration

Avoid:

- excessive cascading
- large eager object graphs
- deep bidirectional mappings

---

# Relationship Strategy

## Many-To-Many

Avoid direct @ManyToMany mappings.

Preferred:
- explicit join entities

Example:

Instead of:

    UserEntity <-> RoleEntity

Use:

    UserRoleEntity

---

# Collection Strategy

Avoid large collections inside entities.

Example:

Avoid:

    Set<MessageEntity>
    Set<TradeOfferEntity>

inside UserEntity.

Reason:
- memory overhead
- accidental eager loading
- serialization problems
- Hibernate complexity

---

# Fetch Strategy

Default:
- LAZY loading

Avoid:
- EAGER loading

---

# Cascade Strategy

Use cascading carefully.

Avoid:
- CascadeType.ALL by default

Prefer:
- explicit persistence orchestration

---

# Entity Responsibility

Entities represent:
- persistence state
- domain state

Entities should not:
- orchestrate workflows
- call services
- contain HTTP logic

---

# Query Strategy

Complex reads should later use:
- projections
- DTO queries
- query services
- dedicated read models where needed

---

# Soft Delete Strategy

Soft delete should be supported for:
- users
- items
- messages
- trade offers
- reports

---

# Serialization Rule

Entities must never be exposed directly through REST APIs.

Generated DTOs are used externally.

---

# Future Scalability

This strategy prepares the system for:
- high traffic
- pagination-heavy APIs
- search integrations
- eventual microservice extraction if needed
-- Initial RBAC seed data
-- Source of truth: docs/11-identity-access-seed-v1.md

INSERT INTO roles (uuid, code, name, description, created_at)
VALUES
    ('a3f1c2d4-5e6f-4a7b-8c9d-1e2f3a4b5c6d', 'USER', 'User', 'Standard registered platform user', CURRENT_TIMESTAMP),
    ('b7c8d9e0-1f23-4a56-9bcd-2e3f4a5b6c7d', 'MODERATOR', 'Moderator', 'User responsible for reviewing reports and moderating content', CURRENT_TIMESTAMP),
    ('c9d0e1f2-3456-4b78-8abc-3d4e5f607182', 'ADMIN', 'Administrator', 'Full platform administrator', CURRENT_TIMESTAMP);

INSERT INTO permissions (uuid, code, name, description, created_at)
VALUES
    ('3d9f1a2b-4c5e-4f60-8a7b-9c0d1e2f3a4b', 'ITEM_VIEW', 'View items', 'Allows viewing active item listings', CURRENT_TIMESTAMP),
    ('4e1c2d3f-5a6b-4c7d-8e90-1f2a3b4c5d6e', 'ITEM_CREATE', 'Create items', 'Allows creating item listings', CURRENT_TIMESTAMP),
    ('5f2d3e4a-6b7c-4d8e-8b01-2c3d4e5f6071', 'ITEM_UPDATE', 'Update items', 'Allows updating owned item listings', CURRENT_TIMESTAMP),
    ('6a3e4f5b-7c8d-4e9f-8c12-3d4e5f607182', 'ITEM_DELETE', 'Delete items', 'Allows deleting or archiving owned item listings', CURRENT_TIMESTAMP),
    ('7b4f5a6c-8d9e-4f10-8d23-4e5f60718293', 'MESSAGE_SEND', 'Send messages', 'Allows sending messages to other users', CURRENT_TIMESTAMP),
    ('8c5a6b7d-9e0f-4a21-8e34-5f60718293a4', 'TRADE_OFFER_CREATE', 'Create trade offers', 'Allows creating trade offers', CURRENT_TIMESTAMP),
    ('9d6b7c8e-0f1a-4b32-8f45-60718293a4b5', 'TRADE_OFFER_RESPOND', 'Respond to trade offers', 'Allows accepting or rejecting received trade offers', CURRENT_TIMESTAMP),
    ('ae7c8d9f-1a2b-4c43-8a56-718293a4b5c6', 'PROFILE_UPDATE', 'Update profile', 'Allows updating own profile', CURRENT_TIMESTAMP),
    ('bf8d9e0a-2b3c-4d54-8b67-8293a4b5c6d7', 'REPORT_CREATE', 'Create reports', 'Allows reporting users, items or messages', CURRENT_TIMESTAMP),
    ('c09e0a1b-3c4d-4e65-8c78-93a4b5c6d7e8', 'REPORT_REVIEW', 'Review reports', 'Allows reviewing submitted reports', CURRENT_TIMESTAMP),
    ('d1af1b2c-4d5e-4f76-8d89-a4b5c6d7e8f9', 'MODERATION_ACTION_CREATE', 'Create moderation actions', 'Allows performing moderation actions', CURRENT_TIMESTAMP),
    ('e2b01c3d-5e6f-4087-8e9a-b5c6d7e8f9a0', 'USER_VIEW', 'View users', 'Allows viewing user accounts for administration/moderation', CURRENT_TIMESTAMP),
    ('f3c12d4e-6f70-4198-8fab-c6d7e8f9a0b1', 'USER_SUSPEND', 'Suspend users', 'Allows suspending user accounts', CURRENT_TIMESTAMP),
    ('a4d23e5f-7081-4aa9-8abc-d7e8f9a0b1c2', 'USER_BAN', 'Ban users', 'Allows banning user accounts', CURRENT_TIMESTAMP),
    ('b5e34f60-8192-4bba-8bcd-e8f9a0b1c2d3', 'ADMIN_ACCESS', 'Admin access', 'Allows access to administrator features', CURRENT_TIMESTAMP);

WITH role_permission_codes (role_code, permission_code) AS (
    VALUES
        ('USER', 'ITEM_VIEW'),
        ('USER', 'ITEM_CREATE'),
        ('USER', 'ITEM_UPDATE'),
        ('USER', 'ITEM_DELETE'),
        ('USER', 'MESSAGE_SEND'),
        ('USER', 'TRADE_OFFER_CREATE'),
        ('USER', 'TRADE_OFFER_RESPOND'),
        ('USER', 'PROFILE_UPDATE'),
        ('USER', 'REPORT_CREATE'),
        ('MODERATOR', 'ITEM_VIEW'),
        ('MODERATOR', 'ITEM_CREATE'),
        ('MODERATOR', 'ITEM_UPDATE'),
        ('MODERATOR', 'ITEM_DELETE'),
        ('MODERATOR', 'MESSAGE_SEND'),
        ('MODERATOR', 'TRADE_OFFER_CREATE'),
        ('MODERATOR', 'TRADE_OFFER_RESPOND'),
        ('MODERATOR', 'PROFILE_UPDATE'),
        ('MODERATOR', 'REPORT_CREATE'),
        ('MODERATOR', 'REPORT_REVIEW'),
        ('MODERATOR', 'MODERATION_ACTION_CREATE'),
        ('MODERATOR', 'USER_VIEW'),
        ('MODERATOR', 'USER_SUSPEND'),
        ('ADMIN', 'ITEM_VIEW'),
        ('ADMIN', 'ITEM_CREATE'),
        ('ADMIN', 'ITEM_UPDATE'),
        ('ADMIN', 'ITEM_DELETE'),
        ('ADMIN', 'MESSAGE_SEND'),
        ('ADMIN', 'TRADE_OFFER_CREATE'),
        ('ADMIN', 'TRADE_OFFER_RESPOND'),
        ('ADMIN', 'PROFILE_UPDATE'),
        ('ADMIN', 'REPORT_CREATE'),
        ('ADMIN', 'REPORT_REVIEW'),
        ('ADMIN', 'MODERATION_ACTION_CREATE'),
        ('ADMIN', 'USER_VIEW'),
        ('ADMIN', 'USER_SUSPEND'),
        ('ADMIN', 'USER_BAN'),
        ('ADMIN', 'ADMIN_ACCESS')
)
INSERT INTO role_permissions (role_id, permission_id, assigned_at)
SELECT r.id, p.id, CURRENT_TIMESTAMP
FROM role_permission_codes rpc
JOIN roles r ON r.code = rpc.role_code
JOIN permissions p ON p.code = rpc.permission_code;

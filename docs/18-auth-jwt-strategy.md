# Auth & JWT Strategy

## Goal

Implement stateless authentication using JWT access tokens and persisted refresh tokens.

## Token Model

Access token:
- short-lived
- JWT
- sent as Bearer token

Refresh token:
- longer-lived
- stored only as hash in database
- can be revoked
- used to issue new access tokens

## Login Flow

1. User submits email/username and password
2. Backend validates credentials
3. Backend checks user status
4. Backend returns access token and refresh token

## Security Rules

- Never store raw refresh tokens
- Never expose password hash
- Access token is stateless
- Refresh token can be revoked
- Banned/suspended users cannot login

## Future

- MFA challenge
- OAuth2 login
- device/session tracking
src-old is read-only reference.
New src is the active frontend.
Use OpenAPI-generated types/client.
Preserve auth, token refresh, i18n, routing guards, query patterns and API logic from src-old.
Do not copy old UI blindly.
Rebuild feature by feature.
Validate with:
yarn generate:api
npx tsc --noEmit
yarn build
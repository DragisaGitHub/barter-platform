# syntax=docker/dockerfile:1.7

FROM node:26-alpine AS build
WORKDIR /app

ARG VITE_API_BASE_URL=/api/v1
ARG VITE_SENTRY_DSN
ARG VITE_SENTRY_ENVIRONMENT
ARG VITE_COMMIT_SHA

ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}
ENV VITE_SENTRY_DSN=${VITE_SENTRY_DSN}
ENV VITE_SENTRY_ENVIRONMENT=${VITE_SENTRY_ENVIRONMENT}
ENV VITE_COMMIT_SHA=${VITE_COMMIT_SHA}

RUN corepack enable && corepack prepare yarn@1.22.22 --activate

COPY frontend/package.json frontend/yarn.lock ./
RUN yarn install --frozen-lockfile --non-interactive

COPY frontend/ ./
RUN yarn build

FROM nginx:1.31-alpine AS runtime
COPY deployment/docker/frontend/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://localhost/ || exit 1

CMD ["nginx", "-g", "daemon off;"]


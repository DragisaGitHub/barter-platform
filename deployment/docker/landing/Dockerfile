# syntax=docker/dockerfile:1.7

FROM node:22-alpine AS build
WORKDIR /app

RUN corepack enable && corepack prepare yarn@1.22.22 --activate

COPY landing/package.json landing/yarn.lock ./
RUN yarn install --frozen-lockfile --non-interactive

COPY landing/ ./
RUN yarn build

FROM nginx:1.27-alpine AS runtime
COPY deployment/docker/landing/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --quiet --tries=1 --spider http://127.0.0.1/health || exit 1

CMD ["nginx", "-g", "daemon off;"]


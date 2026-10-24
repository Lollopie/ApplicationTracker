FROM node:24-alpine AS builder
WORKDIR /builder

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# Runs as a non-root user and listens on 8080.
FROM nginxinc/nginx-unprivileged:alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY security-headers.conf /etc/nginx/security-headers.conf
COPY --from=builder /builder/dist /usr/share/nginx/html

EXPOSE 8080

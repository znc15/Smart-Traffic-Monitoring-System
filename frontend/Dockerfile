ARG NODE_VERSION=20.19.0
ARG PNPM_VERSION=9.14.4
ARG VITE_BASE_PATH=/
ARG NODE_BASE_IMAGE=docker.m.daocloud.io/library/node:${NODE_VERSION}-alpine
ARG NGINX_BASE_IMAGE=docker.m.daocloud.io/library/nginx:1.27-alpine

FROM ${NODE_BASE_IMAGE} AS builder
ARG VITE_BASE_PATH=/

RUN npm config set registry https://registry.npmmirror.com \
    && npm install -g pnpm@${PNPM_VERSION}

WORKDIR /frontend

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
ENV VITE_BASE_PATH=${VITE_BASE_PATH}
RUN pnpm build

FROM ${NGINX_BASE_IMAGE}

COPY --from=builder /frontend/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

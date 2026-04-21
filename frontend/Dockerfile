ARG NODE_VERSION=20.19.0
ARG PNPM_VERSION=9.14.4
ARG NODE_BASE_IMAGE=docker.m.daocloud.io/library/node:${NODE_VERSION}-alpine
ARG NGINX_BASE_IMAGE=docker.m.daocloud.io/library/nginx:1.27-alpine
ARG VITE_API_HTTP_BASE
ARG VITE_API_WS_BASE
ARG VITE_AMAP_KEY
ARG VITE_AMAP_SECURITY_JS_CODE
ARG VITE_AMAP_SERVICE_HOST

FROM ${NODE_BASE_IMAGE} AS builder

ARG VITE_API_HTTP_BASE
ARG VITE_API_WS_BASE
ARG VITE_AMAP_KEY
ARG VITE_AMAP_SECURITY_JS_CODE
ARG VITE_AMAP_SERVICE_HOST

ENV VITE_API_HTTP_BASE=${VITE_API_HTTP_BASE}
ENV VITE_API_WS_BASE=${VITE_API_WS_BASE}
ENV VITE_AMAP_KEY=${VITE_AMAP_KEY}
ENV VITE_AMAP_SECURITY_JS_CODE=${VITE_AMAP_SECURITY_JS_CODE}
ENV VITE_AMAP_SERVICE_HOST=${VITE_AMAP_SERVICE_HOST}

RUN npm config set registry https://registry.npmmirror.com \
    && npm install -g pnpm@${PNPM_VERSION}

WORKDIR /frontend

COPY package.json pnpm-lock.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
RUN pnpm build

FROM ${NGINX_BASE_IMAGE}
COPY --from=builder /frontend/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

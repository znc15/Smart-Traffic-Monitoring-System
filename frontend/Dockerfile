ARG NODE_VERSION=20.17.0
ARG PNPM_VERSION=9.14.4
ARG NODE_BASE_IMAGE=docker.m.daocloud.io/library/node:${NODE_VERSION}-alpine

FROM ${NODE_BASE_IMAGE}

ENV NODE_ENV=development

RUN npm install -g pnpm@${PNPM_VERSION}

WORKDIR /frontend

COPY package.json ./

RUN pnpm install --no-frozen-lockfile

COPY . .

EXPOSE 5173

CMD ["pnpm", "dev", "--host", "0.0.0.0"]

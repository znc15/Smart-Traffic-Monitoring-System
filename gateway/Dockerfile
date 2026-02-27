ARG NGINX_BASE_IMAGE=docker.m.daocloud.io/library/nginx:1.27-alpine
FROM ${NGINX_BASE_IMAGE}
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]

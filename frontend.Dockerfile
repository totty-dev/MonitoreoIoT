FROM nginx:alpine
RUN apk add --no-cache gettext

COPY web/ /usr/share/nginx/html
COPY docker/frontend/config.js.template /usr/share/nginx/html/js/config.js.template
COPY docker/frontend/generate-config.sh /docker-entrypoint.d/40-generate-config.sh
RUN chmod +x /docker-entrypoint.d/40-generate-config.sh

EXPOSE 80
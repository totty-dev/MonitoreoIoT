#!/bin/sh
set -eu

envsubst '${ESP32_IP} ${FRONTEND_IP} ${BACKEND_PORT} ${BACKEND_CONTEXT_PATH}' \
  < /usr/share/nginx/html/js/config.js.template \
  > /usr/share/nginx/html/js/config.js
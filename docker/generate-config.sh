#!/bin/sh
set -eu

envsubst '${ESP32_IP} ${BACKEND_IP} ${BACKEND_PORT}' \
  < /usr/share/nginx/html/js/config.js.template \
  > /usr/share/nginx/html/js/config.js
FROM nginx:alpine
COPY web/ /usr/share/nginx/html
EXPOSE 8081
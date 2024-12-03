# Sử dụng image chính thức của NGINX
FROM nginx:latest
COPY ./nginx.conf /etc/nginx/nginx.conf

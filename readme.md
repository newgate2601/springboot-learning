https://chatgpt.com/c/6714e6d9-98f0-8010-86bb-aae2b4330ee1
Load balancer: https://chatgpt.com/c/6715dbe7-7438-8010-96b7-483479bb9b2f

# How run a simple Web Server with NGINX docker
* run docker: docker run --name nginx-server-test -p 8080:8080 nginx
* go to server NGINX: http://localhost/ (default)
* get file config of NGINX server to local: docker cp nginx-server-test:/etc/nginx/nginx.conf C:\Users\Lenovo\Downloads\nginxfiletest
* put file config of local to NGINX server: docker cp C:\Users\Lenovo\Downloads\nginxfiletest\nginx.conf nginx-server-test:/etc/nginx/nginx.conf
* reload NGINX: docker exec nginx-server-test nginx -s reload
* add a file from local to NGINX docker: docker cp C:\Users\Lenovo\SpringBoot_Backend\untitled\index.html nginx-server-test:/usr/share/nginx/html/ok.html
* generate a new image (nginx-server-test-v2) from old image (nginx-server-test): docker commit nginx-server-test nginx-server-test-v2

# How run NGINX in local
* download NGINX in window: https://nginx.org/download/nginx-1.27.2.zip
* run NGINX nằm trong file C:\Users\Lenovo\Downloads\nginx-1.27.2
* kill NGINX process in local: taskkill /F /IM nginx.exe

# http{} https://chatgpt.com/c/6714de46-d4ac-8010-9b2e-b0466041efcf
+ http{} sử dụng để config tất cả mọi thứ liên quan tới HTTP request, nằm trong nginx.conf file

### http{include}
+ Include sử dụng để nhúng file config khác vào
+ Vd như sử dụng để nhúng mime.types file vào file config


    http {
        include mime.types
    }    

### http{types} https://chatgpt.com/c/6713c6dd-1244-8010-a705-e0bce8f22afa
+ Mime types (Multipurpose Internet Mail Extensions) để xác định rõ ràng kiểu dữ liệu của file, kiểu định dạng file được gửi qua HTTP
+ Mime types gồm 2 phần chính là: 
    + Loại nội dung: text, image, audio, application
    + Chỉ định định dạng cụ thể: html, png, json -> text/html dùng cho HTML, image/png dùng cho hình ảnh, application/json dùng cho JSON
+ Ví dụ như tôi rõ ràng trả về CSS file nhưng nó không được apply với Browser, lý do là Content-Type nó trả về là dạng 
text/plain chứ không phải dạng text/css
=> Trong https {}, ta có thể define types{} để khai báo + ngoài ra ta có thể dùng include mime.types (mine.types chứa danh 
sách các file extension và mime type tương ứng của nó) để apply toàn bộ Mine type 
mỗi khi NGINX response lại cho 1 HTTP request -> nó sẽ đính kèm Mine type vào Header (Content-Type) của response để Browser có thể xử lý nội dung response

    
    # apply all mime type
    http {
        include mime.types
    }    
    # hoặc tự define
    http {
        types {
            text/html  html;
            image/jpeg jpg jpeg;
            image/png  png;
            text/css   css;
            application/javascript js;
        }
    }

### http{default_type} 
+ default_type sử dụng để define kiểu mặc định cho các file không xác định

# http{server{}}
+ server{} sử dụng để
+ server{proxy_set_header}

## server{location{}} https://chatgpt.com/c/67151e21-ad88-8010-bbd5-a9869bf628a8 
+ Location sử dụng để xác định cách xử lý requests đến Web server dựa trên URI của request đó
+ Location gồm:
  + Định tuyến request tới các dịch vụ khác
  + Trả về static files
  + Forward request tới các Application server backend khác
  + Root thì url sẽ ghép cả phần location + uri làm path file
  + Alias thì không ghép phần location vào làm path file


    server {
        location / {    
            root   /usr/share/nginx/html;  # Thư mục chứa file HTML
            index  index.html;              # File HTML chính
            try_files $uri $uri/ =404;     # Trả về 404 nếu file không tồn tại
        }

        location /ok {  # path file = /usr/share/nginx/html/ok.html chứ không add thêm /ok location vào thêm làm path file
            alias   /usr/share/nginx/html;  # Thư mục chứa file HTML
            index  ok.html;              # File HTML chính
            try_files $uri $uri/ =404;     # Trả về 404 nếu file không tồn tại
        }

        location /good {
          alias  /usr/share/nginx/html;
          index good.html;
          try_files $uri $uri/ =404; 
        }
    }

### location{proxy_pass} https://docs.nginx.com/nginx/admin-guide/web-server/reverse-proxy/
+ sử dụng NGINX như 1 reverse proxy server để define cách mà NGINX sẽ forward các request từ Client tới 1 HTTP Application server khác
+ VD bên dưới là sử dụng NGINX như 1 proxy server, khi call api đến /oke nó sẽ convert /ok/... -> /uaa/...)


    server {
	listen       8081;
        server_name  localhost;	
        location /oke {
            proxy_pass http://localhost:8086/uaa;
        }
    }	

## location{proxy_set_header} 
+ sử dụng để define lại Header của Client request trước khi send request tới Application server


    location /some/path/ {
      proxy_set_header Accept-Encoding "";
    }

## location{proxy_buffers; proxy_buffer_size; proxy_buffering; proxy_bind}

https://docs.nginx.com/nginx/admin-guide/load-balancer/http-load-balancer/

# Custom config: https://chatgpt.com/c/67127717-d0a8-8010-bb14-8e82919072c2
### Dưới đây là config NGINX khi gõ vào localhost:8080 sẽ trả về Client file HTTP ở path /usr/share/nginx/html của NGINX


    worker_processes auto;
    
    events {
      worker_connections 1024;
    }
    
    http {
      include       mime.types;
      default_type  application/octet-stream;
  
      upstream uaa {
          server localhost:8086;  # Backend server 1
          server localhost:8087;  # Backend server 2
          # Bạn có thể thêm nhiều server khác nếu cần
      }
  
      server {
          listen       8081;
          server_name  localhost;	
          location /oke {
              rewrite ^/oke/(.*) /uaa/$1 break;  # thay thế oke -> uaa (http://localhost:8081/oke/api/v1/user/list -> http://localhost:8086/uaa/api/v1/user/list)
              proxy_pass http://uaa;    # sử dụng để define proxy server với khối upstream uaa ở bên trên
          }
      }	
      
      server {
          listen       8080;
          server_name  localhost;
  
          location / {
              root   C:\Users\Lenovo\SpringBoot_Backend\untitled;  # Thư mục chứa file HTML
              index  index.html;              # File HTML chính
              try_files $uri $uri/ =404;     # Trả về 404 nếu file không tồn tại
          }
  
          location /ok {
              alias   C:\Users\Lenovo\SpringBoot_Backend\untitled;  # Thư mục chứa file HTML
              index  ok.html;              # File HTML chính
              try_files $uri $uri/ =404;     # Trả về 404 nếu file không tồn tại
          }
  
          location /good {
              alias  C:\Users\Lenovo\SpringBoot_Backend\untitled;
              index good.html;
              try_files $uri $uri/ =404; 
          }
      }
    }
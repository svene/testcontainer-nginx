package org.svenehrke.nginxtest;

public class NginxConfig {
	public static final String NGINX_CONTAINER_CONFIG_FILE_TEMPLATE = """
		user  nginx;
		worker_processes  auto;
		
		error_log  /var/log/nginx/error.log notice;
		pid        /var/run/nginx.pid;
		
		events {
			worker_connections  1024;
		}
		
		http {
			include       /etc/nginx/mime.types;
			default_type  application/octet-stream;
		
			log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
							  '$status $body_bytes_sent "$http_referer" '
							  '"$http_user_agent" "$http_x_forwarded_for"';
		
			access_log  /var/log/nginx/access.log  main;
		
			sendfile        on;
			#tcp_nopush     on;
		
			keepalive_timeout  65;
		
			#gzip  on;
		
			server {
				listen       80;
				listen  [::]:80;
				server_name  localhost;
		
				access_log  /var/log/nginx/host.access.log  main;
		
				location / {
					root   /usr/share/nginx/html;
					index  index.html index.htm;
				}
				location /server2 {
					proxy_pass http://localhost:8090/;
				}
			}
			server {
				listen       8090;
				server_name  server2;
				access_log  /var/log/nginx/host2.access.log  main;
				location / {
					root   /usr/share/nginx/html2;
					index  index.html;
				}
			}
		}
		""";
}

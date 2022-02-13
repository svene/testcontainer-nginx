package org.svenehrke.nginxtest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class NginxTest {

	private static final DockerImageName NGINX_IMAGE = DockerImageName.parse("nginx:1.21.6");
	public static final String CONTAINER_NAME = "nginx-tc";

	static File htmlFolder;
	static File configFolder;
	static NginxContainer<?> container;

	private static final String NGINX_CONTAINER_HTML_FOLDER = "/usr/share/nginx/html/";
	private static final String NGINX_CONTAINER_CONFIG_FILE = "/etc/nginx/nginx.conf";
	private static final String NGINX_CONTAINER_CONFIG_DIR = "/etc/nginx/nginx.conf/conf.d";

	private static final String NGINX_CONTAINER_CONFIG_FILE_TEMPLATE = """
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
		
			include /etc/nginx/conf.d/*.conf;
		}
		""";
	private static final String INDEX_HTML = """
  			<html><body>Hello World!</body></html>
		""";

	@BeforeAll
	static void setUp() throws IOException {
		htmlFolder = TestUtil.createTempFolder("testcontainer_nginx_html_folder");
		Files.writeString(TestUtil.newTempFile(htmlFolder, "index.html").toPath(), INDEX_HTML);

		configFolder = TestUtil.createTempFolder("testcontainer_nginx_config_folder");
		File configFile = TestUtil.newTempFile(configFolder, "default.conf");
		Files.writeString(configFile.toPath(), NGINX_CONTAINER_CONFIG_FILE_TEMPLATE);

		container = new NginxContainer<>(NGINX_IMAGE)
			.withCreateContainerCmdModifier(cmd -> cmd.withName(CONTAINER_NAME)) // use this line just for development! (can cause name clashes)
			.withCopyFileToContainer(MountableFile.forHostPath(htmlFolder.toPath()), NGINX_CONTAINER_HTML_FOLDER)
			.withCopyFileToContainer(MountableFile.forHostPath(configFile.toPath()), NGINX_CONTAINER_CONFIG_FILE)
			.waitingFor(new HttpWaitStrategy())
		;
		container.start();
	}

	@Test
	void test1() throws IOException {
		URL url = new URL("http", "localhost", container.getFirstMappedPort(), "/index.html");
		String actual = responseFromNginx(url);

		assertThat(actual).contains("Hello World!");
	}

	private static String responseFromNginx(URL baseUrl) throws IOException {
		URLConnection urlConnection = baseUrl.openConnection();
		return new String(urlConnection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
	}
}

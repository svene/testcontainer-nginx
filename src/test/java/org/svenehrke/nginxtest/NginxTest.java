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

import static org.assertj.core.api.Assertions.assertThat;

public class NginxTest {

	private static final DockerImageName NGINX_IMAGE = DockerImageName.parse("nginx:1.21.6");

	static File htmlFolder;
	static NginxContainer<?> container;

	private static final String NGINX_CONTAINER_HTML_FOLDER = "/usr/share/nginx/html/";

	private static final String NGINX_CONFIG_TEMPLATE = """
		""";
	private static final String INDEX_HTML = """
  			<html><body>Hello World!</body></html>
		""";

	@BeforeAll
	static void setUp() throws IOException {
		htmlFolder = TestUtil.createTempFolder("testcontainer_nginx_html_folder");
		File indexHtmlFile = TestUtil.newTempFile(htmlFolder, "index.html");
		Files.writeString(indexHtmlFile.toPath(), INDEX_HTML);

		container = new NginxContainer<>(NGINX_IMAGE)
			.withCopyFileToContainer(MountableFile.forHostPath(htmlFolder.toPath()), NGINX_CONTAINER_HTML_FOLDER)
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

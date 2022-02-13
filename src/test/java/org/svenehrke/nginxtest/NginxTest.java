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
	public static final String CONTAINER_NAME = "nginx-tc";

	static File htmlFolder;
	static File configFolder;
	static NginxContainer<?> container;


	private static final String NGINX_CONTAINER_HTML_FOLDER = "/usr/share/nginx/html/";
	private static final String NGINX_CONTAINER_CONFIG_FILE = "/etc/nginx/nginx.conf";
	private static final String NGINX_CONTAINER_CONFIG_DIR = "/etc/nginx/nginx.conf/conf.d";

	static File html2Folder;
	private static final String NGINX_CONTAINER_HTML2_FOLDER = "/usr/share/nginx/html2/";

	private static final String INDEX_HTML = """
  			<html><body>Hello World!</body></html>
		""";
	private static final String INDEX2_HTML = """
  			<html><body>SERVER TWO</body></html>
		""";

	@BeforeAll
	static void setUp() throws IOException {
		configFolder = TestUtil.createTempFolder("testcontainer_nginx_config_folder");
		File configFile = TestUtil.newTempFile(configFolder, "default.conf");
		Files.writeString(configFile.toPath(), NginxConfig.NGINX_CONTAINER_CONFIG_FILE_TEMPLATE);

		htmlFolder = TestUtil.createTempFolder("testcontainer_nginx_html_folder");
		Files.writeString(TestUtil.newTempFile(htmlFolder, "index.html").toPath(), INDEX_HTML);

		html2Folder = TestUtil.createTempFolder("testcontainer_nginx_html2_folder");
		Files.writeString(TestUtil.newTempFile(html2Folder, "index.html").toPath(), INDEX2_HTML);

		container = new NginxContainer<>(NGINX_IMAGE)
			.withCreateContainerCmdModifier(cmd -> cmd.withName(CONTAINER_NAME)) // use this line just for development! (can cause name clashes)
			.withCopyFileToContainer(MountableFile.forHostPath(configFile.toPath()), NGINX_CONTAINER_CONFIG_FILE)
			.withCopyFileToContainer(MountableFile.forHostPath(htmlFolder.toPath()), NGINX_CONTAINER_HTML_FOLDER)
			.withCopyFileToContainer(MountableFile.forHostPath(html2Folder.toPath()), NGINX_CONTAINER_HTML2_FOLDER)
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
	@Test
	void test2() throws IOException {
		URL url = new URL("http", "localhost", container.getFirstMappedPort(), "/server2/index.html");
		String actual = null;
		try {
			actual = responseFromNginx(url);
		} catch (IOException e) {
			System.out.println();
		}
		assertThat(actual).contains("SERVER TWO");
	}

	private static String responseFromNginx(URL baseUrl) throws IOException {
		URLConnection urlConnection = baseUrl.openConnection();
		return new String(urlConnection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
	}
}

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
	private static final int PROXY_PORT = 9201;

	static File tmpDir;
	static NginxContainer<?> container;

	@BeforeAll
	static void setUp() throws IOException {
		tmpDir = Files.createTempDirectory("tmpDirPrefix").toFile();
		tmpDir.setExecutable(true, false);
		tmpDir.setReadable(true, false);
		tmpDir.deleteOnExit();

		File indexFile = new File(tmpDir, "index.html");
		indexFile.setReadable(true, false);
		indexFile.deleteOnExit();

		String s = "<html><body>Hello World!</body></html>";
		Files.write(indexFile.toPath(), s.getBytes(StandardCharsets.UTF_8));

		container = new NginxContainer<>(NGINX_IMAGE)
//			.withCustomContent(tmpDir.getPath())
//			.withFileSystemBind(docPath + "/", "/usr/share/nginx/html/", BindMode.READ_ONLY)
			.withCopyFileToContainer(MountableFile.forHostPath(tmpDir.toPath()), "/usr/share/nginx/html/")
			.waitingFor(new HttpWaitStrategy())
		;
		container.addExposedPort(PROXY_PORT);
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

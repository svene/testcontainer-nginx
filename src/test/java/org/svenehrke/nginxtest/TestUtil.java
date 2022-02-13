package org.svenehrke.nginxtest;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestUtil {
	@NotNull
	public static File createTempFolder(String folderName) throws IOException {
		File result = Files.createTempDirectory(folderName).toFile();
		result.setExecutable(true, false);
		result.setReadable(true, false);
		result.deleteOnExit();
		return result;
	}

	@NotNull
	public static File newTempFile(File parentFolder, String filename) {
		File result = new File(parentFolder, filename);
		result.setReadable(true, false);
		result.deleteOnExit();
		return result;
	}

}

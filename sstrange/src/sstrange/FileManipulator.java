package sstrange;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class FileManipulator {
	// three methods below are for copying directories
	// copied from https://www.baeldung.com/java-copy-directory
	public static void copyDirectory(File sourceDirectory, File destinationDirectory) throws IOException {
		if (!destinationDirectory.exists()) {
			destinationDirectory.mkdir();
		}
		for (String f : sourceDirectory.list()) {
			copyDirectoryCompatibityMode(new File(sourceDirectory, f), new File(destinationDirectory, f));
		}
	}

	private static void copyDirectoryCompatibityMode(File source, File destination) throws IOException {
		if (source.isDirectory()) {
			copyDirectory(source, destination);
		} else {
			copyFile(source, destination);
		}
	}

	private static void copyFile(File sourceFile, File destinationFile) throws IOException {
		try (InputStream in = new FileInputStream(sourceFile);
				OutputStream out = new FileOutputStream(destinationFile)) {
			byte[] buf = new byte[1024];
			int length;
			while ((length = in.read(buf)) > 0) {
				out.write(buf, 0, length);
			}
		}
	}

	public static void deleteAllTemporaryFiles(File f) {
		// recursively delete all files
		if (f.isDirectory()) {
			File[] children = f.listFiles();
			for (File c : children) {
				deleteAllTemporaryFiles(c);
			}
		}
		f.delete();
	}
}

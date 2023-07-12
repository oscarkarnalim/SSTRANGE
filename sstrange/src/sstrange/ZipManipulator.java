package sstrange;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Enumeration;
import java.util.Stack;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipManipulator {
	public static  String extractAllZips(String assignmentParentDirPath, String assignmentName) {
		// set the output directory
		String outputPath = assignmentParentDirPath + File.separator + "[unzipped] " + assignmentName;

		// create that directory
		new File(outputPath).mkdir();

		File rootDir = new File(assignmentParentDirPath + File.separator + assignmentName);
		File[] submissions = rootDir.listFiles();
		for (File s : submissions) {
			// get the zip
			File zip = getZip(s);

			// if no zip, skip the process
			if (zip == null) {
				continue;
			}

			// create output dir for given submission
			File unzipDir = new File(outputPath + File.separator + s.getName());
			unzipDir.mkdir();

			// unzip
			unzip(zip.getParentFile().getAbsolutePath(), zip.getName(), unzipDir.getAbsolutePath());
		}

		return outputPath;
	}

	private static File getZip(File root) {
		// search a zip in the directory. Return null if not found.
		Stack<File> stack = new Stack<File>();
		stack.push(root);
		while (stack.isEmpty() == false) {
			File t = stack.pop();
			if (t.isDirectory()) {
				File[] tt = t.listFiles();
				for (File ttt : tt) {
					stack.push(ttt);
				}
			} else {
				if (t.getName().endsWith(".zip"))
					return t;
			}
		}

		return null;
	}

	private static void unzip(String zipFileDir, String zipFileName, String unzipDir) {

		// JOptionPane.showMessageDialog(null, zip.getAbsolutePath());

		// copied and modified from
		// http://tutorials.jenkov.com/java-zip/zipfile.html#unzipping-all-entries-in-zipfile
		/*
		 * extract given zipfile to unzipdir
		 */
		String zipFilePath = zipFileDir + File.separator + zipFileName;
		try {
			Charset CP437 = Charset.forName("CP437");
			ZipFile zipFile = new ZipFile(zipFilePath, CP437);

			Enumeration<? extends ZipEntry> entries = zipFile.entries();

			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory()) {
					String destPath = unzipDir + File.separator + entry.getName();
					File file = new File(destPath);
					file.mkdirs();
				} else {
					// this is mac files, ignore
					if (entry.getName().contains("__MACOSX"))
						continue;

					// if the entry is placed under several non-existing subdirectories
					if (entry.getName().contains("/") || entry.getName().contains("\\")) {

						// set the separator. This cannot be based on File.separator given that it
						// depends on how the zip has been created
						String separator = "/";
						if (entry.getName().contains("\\"))
							separator = "\\";

						// get remaining file path whose subdirectories need to be created
						String remainingDirPath = entry.getName();
						// mark what subdirectories that have been created
						String createdDirPath = unzipDir;

						// while remainingDirPath still needs some subdirectories created
						while (remainingDirPath.contains(separator)) {

							// update the marker
							createdDirPath = createdDirPath + File.separator
									+ remainingDirPath.substring(0, remainingDirPath.indexOf(separator));

							// create the subdirectory
							File file = new File(createdDirPath);
							file.mkdirs();

							// shorten the remaining file path
							remainingDirPath = remainingDirPath.substring(remainingDirPath.indexOf(separator) + 1);
						}
					}

					String destPath = unzipDir + File.separator + entry.getName();

					InputStream inputStream = zipFile.getInputStream(entry);
					FileOutputStream outputStream = new FileOutputStream(destPath);
					int data = inputStream.read();
					while (data != -1) {
						outputStream.write(data);
						data = inputStream.read();
					}
					outputStream.close();
				}
			}
			zipFile.close();
		} catch (IOException e) {
			System.out.println(zipFilePath);
			e.printStackTrace();
		}
	}
}

package sstrange;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;

public class CodeMerger {

	public static void mergeCode(String rootDirFilepath, String ext, String outputRootDirFilepath) {
		// this method merges all programs in each submission directory to one file

		// create output directory
		new File(outputRootDirFilepath).mkdir();

		// for each student dir, take the code files and merge them as a file
		File rootDir = new File(rootDirFilepath);
		File[] studentDir = rootDir.listFiles();
		for (File sdir : studentDir) {
			if (sdir.isDirectory()) {
				File outputDir = new File(outputRootDirFilepath + File.separator + sdir.getName());
				outputDir.mkdir();
				File outputFile = new File(
						outputDir.getAbsolutePath() + File.separator + "[merged] " + sdir.getName() + "." + ext);
				// System.out.println(outputFile.getAbsolutePath());
				try {
					FileWriter ffw = new FileWriter(outputFile);
					BufferedWriter fw = new BufferedWriter(ffw);
					mergeSourceCodeFiles(sdir, ext, fw, sdir.getAbsolutePath().length());
					fw.close();
					ffw.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

				// if the extension is html
				if (ext.equalsIgnoreCase("html")) {
					// merge also javascript and css files

					// javascript
					outputFile = new File(
							outputDir.getAbsolutePath() + File.separator + "[merged] " + sdir.getName() + ".js");
					try {
						FileWriter ffw = new FileWriter(outputFile);
						BufferedWriter fw = new BufferedWriter(ffw);
						mergeSourceCodeFiles(sdir, "js", fw, sdir.getAbsolutePath().length());
						fw.close();
						ffw.close();
					} catch (Exception e) {
						e.printStackTrace();
					}

					// css
					outputFile = new File(
							outputDir.getAbsolutePath() + File.separator + "[merged] " + sdir.getName() + ".css");
					try {
						FileWriter ffw = new FileWriter(outputFile);
						BufferedWriter fw = new BufferedWriter(ffw);
						mergeSourceCodeFiles(sdir, "css", fw, sdir.getAbsolutePath().length());
						fw.close();
						ffw.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void mergeSourceCodeFiles(File sfile, String ext, BufferedWriter fw, int studentDirPathLength) {
		if (sfile.isDirectory()) {
			File[] schildren = sfile.listFiles();
			for (File sc : schildren) {
				mergeSourceCodeFiles(sc, ext, fw, studentDirPathLength);
			}
		} else {
			String name = sfile.getName();
			// if the file does not end with the extension, ignore
			if (name.toLowerCase().endsWith(ext) == false)
				return;

			// read the file and write it in filewriter
			try {
				// write the path of the file as a comment
				String path = "Filepath: '" + sfile.getAbsolutePath().substring(studentDirPathLength + 1) + "'";

				boolean doWrite = true;

				// begin a comment
				if (ext.endsWith("java")) {
					String pattern = "/* ";
					for (int i = 0; i < path.length(); i++)
						pattern += "=";
					pattern += " */" + System.lineSeparator();

					fw.write(pattern);
					fw.write("/* " + path + " */" + System.lineSeparator());
					fw.write(pattern);
				} else if (ext.endsWith("py")) {
					String pattern = "# ";
					for (int i = 0; i < path.length(); i++)
						pattern += "=";
					pattern += " #" + System.lineSeparator();

					fw.write(pattern);
					fw.write("# " + path + " #" + System.lineSeparator());
					fw.write(pattern);
				} else if (ext.endsWith("html")) {
					String pattern = "<!-- ";
					for (int i = 0; i < path.length(); i++)
						pattern += "=";
					pattern += " -->" + System.lineSeparator();

					fw.write(pattern);
					fw.write("<!-- " + path + " -->" + System.lineSeparator());
					fw.write(pattern);
				} else if (ext.endsWith("js")) {
					String pattern = "/* ";
					for (int i = 0; i < path.length(); i++)
						pattern += "=";
					pattern += " */" + System.lineSeparator();

					fw.write(pattern);
					fw.write("/* " + path + " */" + System.lineSeparator());
					fw.write(pattern);
				} else if (ext.endsWith("css")) {
					String pattern = "/* ";
					for (int i = 0; i < path.length(); i++)
						pattern += "=";
					pattern += " */" + System.lineSeparator();

					fw.write(pattern);
					fw.write("/* " + path + " */" + System.lineSeparator());
					fw.write(pattern);
				} else if (ext.endsWith("cs")) {
					if (sfile.getAbsolutePath().endsWith("Designer.cs") == false) {
						// only merge if it is not file for UI
						String pattern = "/* ";
						for (int i = 0; i < path.length(); i++)
							pattern += "=";
						pattern += " */" + System.lineSeparator();

						fw.write(pattern);
						fw.write("/* " + path + " */" + System.lineSeparator());
						fw.write(pattern);
					} else {
						// do not write the content
						doWrite = false;
					}
				}

				if (doWrite) {
					FileReader bfr = new FileReader(sfile);
					BufferedReader bbr = new BufferedReader(bfr);
					int i;
					while ((i = bbr.read()) != -1) {
						fw.write((char) i);
					}
					fw.write(System.lineSeparator());
					bbr.close();
					bfr.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// Currently not used
//	public static boolean isGeneratedFromCodeMerging(String s, String extension) {
//		if (extension.toLowerCase().endsWith("java")) {
//			if (s.matches("/\\* =+ \\*/"))
//				return true;
//			else if (s.matches("/\\* Filepath: '.+' \\*/"))
//				return true;
//			else
//				return false;
//		} else if (extension.toLowerCase().endsWith("py")) {
//			// python
//			if (s.matches("# =+ #"))
//				return true;
//			else if (s.matches("# Filepath: '.+' #"))
//				return true;
//			else
//				return false;
//		} else if (extension.toLowerCase().endsWith("html")) {
//			// python
//			if (s.matches("<!-- =+ -->"))
//				return true;
//			else if (s.matches("<!-- Filepath: '.+' -->"))
//				return true;
//			else
//				return false;
//		} else if (extension.toLowerCase().endsWith("cs")) {
//			if (s.matches("/\\* =+ \\*/"))
//				return true;
//			else if (s.matches("/\\* Filepath: '.+' \\*/"))
//				return true;
//			else
//				return false;
//		}
//
//		else
//			return false;
//	}
}

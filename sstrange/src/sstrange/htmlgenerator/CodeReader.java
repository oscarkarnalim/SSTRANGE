package sstrange.htmlgenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.Stack;

public class CodeReader {
	public static File getCode(File assignment, String extension) {
		/*
		 * get the first code with the same extension from given directory. It searches
		 * on all sub directories and returns the first file with such an extension.
		 */
		Stack<File> s = new Stack<>();
		s.push(assignment); // add the main directory
		while (s.isEmpty() == false) {
			File cur = s.pop();
			if (cur.isDirectory()) {
				/*
				 * if it is a directory, add all the children.
				 */
				File[] curchildren = cur.listFiles();
				for (File c : curchildren)
					s.push(c);
			} else if (cur.getName().toLowerCase().endsWith(extension)) {
				// if it is the file, return it as the result.
				return cur;
			}
		}
		return null;
	}

	public static File getCodeButNotThese(File assignment, String extension, ArrayList<String> excludedFileNames) {
		/*
		 * get the first code with the same extension from given directory. It searches
		 * on all sub directories and returns the first file with such an extension. 
		 * However, the filename should not be the ones that are excluded.
		 */
		Stack<File> s = new Stack<>();
		s.push(assignment); // add the main directory
		while (s.isEmpty() == false) {
			File cur = s.pop();
			if (cur.isDirectory()) {
				/*
				 * if it is a directory, add all the children.
				 */
				File[] curchildren = cur.listFiles();
				for (File c : curchildren)
					s.push(c);
			} else if (cur.getName().toLowerCase().endsWith(extension)
					&& excludedFileNames.contains(cur.getName()) == false) {
				// if it is the file and not from the list of excluded names, return it as the result.
				return cur;
			}
		}
		return null;
	}
}

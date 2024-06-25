package sstrange;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Stack;
import java.util.Map.Entry;

import commonfragmentremoval.CodeResultGenerator;
import commonfragmentremoval.CommonSegmentGenerator;
import commonfragmentremoval.CommonSegmentRemoval;
import commonfragmentremoval.CommonSegmentTuple;
import support.LibTuple;

public class JavaPyCommonTemplateRemover {
	public static String removeCommonAndTemplateCodeJavaPython(String assignmentPath, int minMatchingLength,
			String templateDirPath, boolean isCommonCodeAllowed, String progLang, String assignmentParentDirPath,
			String assignmentName, ArrayList<File> filesToBeDeleted) {
		/*
		 * this method remove common and template code. It then returns the new
		 * assignment path as the output
		 */
		

		String codeToBeRemovedFilePath = assignmentParentDirPath + File.separator + "[code to be removed] "
				+ assignmentName + ".out";

		// dealing with common code if that is not allowed
		if (isCommonCodeAllowed == false) {
			// if common code is not allowed, remove
			try {

				ArrayList<CommonSegmentTuple> ccresult = CommonSegmentGenerator.execute(assignmentPath, null, progLang,
						0.5, minMatchingLength, minMatchingLength + 40);
				generateCommonCodeFile(ccresult, progLang, codeToBeRemovedFilePath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// base code / template code
		if (templateDirPath.length() > 0 && !templateDirPath.equals("none")) {
			// embed the template code
			File codeToBeRemovedFile = new File(codeToBeRemovedFilePath);

			try {
				// create the file writer
				BufferedWriter bw = new BufferedWriter(new FileWriter(codeToBeRemovedFile, true));

				// for each file in template code dir
				Stack<File> stack = new Stack<File>();
				stack.push(new File(templateDirPath));
				while (stack.isEmpty() == false) {
					File f = stack.pop();
					if (f.isDirectory()) {
						// if it is a directory, push all the children
						File[] children = f.listFiles();
						for (File c : children)
							stack.push(c);
					} else {
						// if it is a file and ends with proglang
						if (f.getName().endsWith("." + progLang)) {
							BufferedReader br = new BufferedReader(new FileReader(f));
							String text = "";
							while ((text = br.readLine()) != null) {
								bw.write(text);
								bw.newLine();
							}
							br.close();
						}
					}
				}

				// close the file writer
				bw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		// remove both common and template code
		File codeToBeRemovedFile = new File(codeToBeRemovedFilePath);
		if ((templateDirPath.length() > 0  && !templateDirPath.equals("none")) || isCommonCodeAllowed == false) {
			if (codeToBeRemovedFile.exists()) {
				String newAssignmentPath = assignmentParentDirPath + File.separator + "[templateremoved] "
						+ assignmentName;
				// if "code to be removed" exist, remove the code from student submissions
				CommonSegmentRemoval.removeTemplateCode(assignmentPath, progLang, codeToBeRemovedFilePath, null,
						newAssignmentPath, true, minMatchingLength);
				// update the assignment path
				assignmentPath = newAssignmentPath;

				// mark the new assignment path and code to be removed so that both will be
				// removed later
				filesToBeDeleted.add(new File(assignmentPath));
				filesToBeDeleted.add(new File(codeToBeRemovedFilePath));
			}
		}

		return assignmentPath;
	}
	
	// copied from c2s2 https://github.com/oscarkarnalim/c2s2
	private static void generateCommonCodeFile(ArrayList<CommonSegmentTuple> result, String extension,
			String outputFilepath) {
		HashMap<String, ArrayList<CommonSegmentTuple>> sourcePathAndTheSegments = new HashMap<>();
		// get the source paths
		for (int i = 0; i < result.size(); i++) {
			CommonSegmentTuple c = result.get(i);

			// check whether the path exists
			ArrayList<CommonSegmentTuple> segments = sourcePathAndTheSegments.get(c.getFirstSourcePath());
			if (segments == null) {
				// if not, create a new list and attach it in the hashmap with given path
				segments = new ArrayList<CommonSegmentTuple>();
				sourcePathAndTheSegments.put(c.getFirstSourcePath(), segments);
			}

			// add the common segment to given list
			segments.add(c);
		}

		try {
			FileWriter fw = new FileWriter(outputFilepath);
			int segmentCounter = 0;

			// per source path, search the actual form of the common segments and write them
			// as the result
			Iterator<Entry<String, ArrayList<CommonSegmentTuple>>> it = sourcePathAndTheSegments.entrySet().iterator();
			while (it.hasNext()) {
				Entry<String, ArrayList<CommonSegmentTuple>> cur = it.next();
				String curpath = cur.getKey();
				ArrayList<CommonSegmentTuple> cursegments = cur.getValue();

				// generate the tokens
				ArrayList<LibTuple> tokens = CodeResultGenerator.getTokenStringWithCommentWhitespace(new File(curpath),
						extension);

				ArrayList<String> results = CodeResultGenerator.getActualCodeFromCommonSegments(tokens, cursegments);

				for (String r : results) {
					// update the segment counter
					segmentCounter++;

					// write comment header
					if (extension.endsWith("java")) {
						fw.write("//////////////////////////////" + System.lineSeparator());
						fw.write("// Segment #" + segmentCounter + System.lineSeparator());
						fw.write("//////////////////////////////" + System.lineSeparator());
					} else if (extension.endsWith("py")) {
						fw.write("##############################" + System.lineSeparator());
						fw.write("# Segment #" + segmentCounter + System.lineSeparator());
						fw.write("##############################" + System.lineSeparator());
					}
					fw.write(r + System.lineSeparator() + System.lineSeparator());
				}
			}

			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

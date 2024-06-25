package sstrange.anomaly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;

public class AICodeManipulator {

	public static String moveAISubToMainAssignmentFolder(String aiSubPath, String assignmentPath,ArrayList<File> filesToBeDeleted) {
		// create unique name for AI
		boolean repeat = false;
		String aiSubDirName = "";
		do {
			aiSubDirName = "AI-" + System.nanoTime();
			File[] assignments = new File(assignmentPath).listFiles();
			for (File f : assignments) {
				if (aiSubDirName.equals(f.getName())) {
					repeat = true;
				}
			}
		} while (repeat);
		// copy files from aiSubPath to the designated directory
		// create the dir for AI submission
		File targetDir = new File(assignmentPath + File.separator + aiSubDirName);
		if (targetDir.exists() == false)
			targetDir.mkdir();
		// mark to be deleted later
		filesToBeDeleted.add(targetDir);
		// copy files inside the AI submission
		try {
			FileUtils.copyDirectory(new File(aiSubPath), targetDir);
			return aiSubDirName;
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
	}
}

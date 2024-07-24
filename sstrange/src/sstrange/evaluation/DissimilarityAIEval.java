package sstrange.evaluation;

import java.io.File;
import java.util.ArrayList;

import sstrange.FastComparer;
import sstrange.FileManipulator;
import sstrange.anomaly.AnomalyTuple;

public class DissimilarityAIEval {
	public static void main(String[] args) {
		String rootpath = "C:\\Users\\oscar\\OneDrive\\Desktop\\eval\\chatgpt python\\real";
		rootpath = "C:\\Users\\oscar\\OneDrive\\Desktop\\eval\\chatgpt python\\semi-real";
		rootpath = "C:\\Users\\oscar\\OneDrive\\Desktop\\eval\\chatgpt python\\simulation";
		rootpath = "C:\\Users\\oscar\\OneDrive\\Desktop\\eval\\chatgpt python\\semi-real java";

		String simMeasurement = "Cosine";
		simMeasurement = "Jaccard";
		// simMeasurement = "MinHash";
		// simMeasurement = "Super-Bit";
		// simMeasurement = "RKRGST";
		// System.out.println(simMeasurement);
		long before = System.nanoTime();
		File[] assessments = new File(rootpath).listFiles();
		for (File a : assessments) {
			if (a.isDirectory() && a.getName().endsWith(".ini") == false) {
				process(a.getAbsolutePath(), simMeasurement);
			}
		}
		long executionTime = System.nanoTime() - before;
		System.out.println(executionTime);
	}

	public static ArrayList<AnomalyTuple> result;

	private static void process(String dirPath, String simMeasurement) {
		int minMatchLength = 10 * 40;
		boolean isSensitive = true;
		int simThreshold = 0; // not used
		int dissimThreshold = 0; // not used

		String aiTemplatePath = ""; // not used for dissimilarity

		File assignmentFile = new File(dirPath);
		File[] subs = assignmentFile.listFiles();

		ArrayList<File> filesToBeDeleted = new ArrayList<File>();

		String assignmentParentDirPath = assignmentFile.getParentFile().getAbsolutePath();
		String assignmentName = assignmentFile.getName();

		String progLang = "py";
		progLang = "java";

		int maxPairs = subs.length * subs.length;
		boolean isMultipleFiles = true;
		boolean isCommonCodeAllowed = true;
		String humanLang = "en";
		int numStages = 1;
		int numClusters = 2;

		// do the comparison
		FastComparer.doSyntacticComparison(dirPath, progLang, humanLang, simThreshold, dissimThreshold, minMatchLength,
				maxPairs, simMeasurement, assignmentFile, assignmentParentDirPath, assignmentName, isMultipleFiles,
				aiTemplatePath, filesToBeDeleted, numClusters, numStages, isSensitive);

		filesToBeDeleted.add(new File(assignmentParentDirPath + "\\[out] " + assignmentName));

		// start deleting all files
		for (File f : filesToBeDeleted)
			FileManipulator.deleteAllTemporaryFiles(f);

		// count MAP
		double map = 0;
		int relevant = 0;
		for (int i = 0; i < result.size(); i++) {
			// System.out.println(result.get(i).getAssignmentName());
			if (result.get(i).getAssignmentName().startsWith("C_")) {
				relevant++;
				map += (relevant * 1.0 / (i + 1));
				// System.out.println(result.get(i).getAssignmentName() + " " + (relevant * 1.0
				// / (i + 1)));
			} else {
				// System.out.println(result.get(i).getAssignmentName());
			}

		}
		map = map / relevant;

		// System.out.println(assignmentName + "\t" + map);
		System.out.println(map);
	}
}

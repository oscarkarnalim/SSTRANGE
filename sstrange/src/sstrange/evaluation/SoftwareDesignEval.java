package sstrange.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import sstrange.FastComparer;
import sstrange.FileManipulator;
import sstrange.matchgenerator.ComparisonPairTuple;

public class SoftwareDesignEval {
	public static void main(String[] args) throws Exception {

		String assessmentRooPath = "C:\\Users\\oscar\\OneDrive\\Desktop\\PDPL Data";
		String goldPath = "C:\\Users\\oscar\\OneDrive\\Desktop\\gold_standard_PDPL.txt";

		String simMeasurement = "";
		simMeasurement = "Cosine";

		// store the gold standard result (key is the assessment name while values are
		// list of string)
		HashMap<String, ArrayList<String>> gold = new HashMap<>();

		// read the gold standard file
		Scanner sc = new Scanner(new File(goldPath));
		String key = sc.nextLine();
		ArrayList<String> val = new ArrayList<>();
		while (sc.hasNextLine()) {
			String l = sc.nextLine();
			if (l.contains("-")) {
				// end of entry, add to hash map and reset
				gold.put(key, val);
				if (sc.hasNextLine()) {
					key = sc.nextLine();
					val = new ArrayList<>();
				}
			} else {
				val.add(l);
			}
		}
		sc.close();

		File[] assessments = new File(assessmentRooPath).listFiles();
		for (File a : assessments) {
			extract(a, gold.get(a.getName()), simMeasurement);
		}
	}

	private static void extract(File assignmentFile, ArrayList<String> gold, String simMeasurement) {

		String progLang = "java";
		int simThreshold = 0;
		int minMatchLength = 10;
		boolean isMultipleFiles = true;
		boolean isCommonCodeAllowed = true;
		String humanLang = "en";
		boolean isSensitive = true;

		int numStages = 1;
		int numClusters = 2;

		// do the process
		try {
			ArrayList<File> filesToBeDeleted = new ArrayList<File>();

			String assignmentParentDirPath = assignmentFile.getParentFile().getAbsolutePath();
			String assignmentName = assignmentFile.getName();

			int maxPairs = 0;
			// update max pairs
			for (String s : gold) {
				int commaPerLine = 0;
				for (int i = 0; i < s.length(); i++) {
					if (s.charAt(i) == ',')
						commaPerLine++;
				}

				// +1 since the copied programs will be more than one
				commaPerLine++;

				maxPairs += (commaPerLine * (commaPerLine - 1) / 2);
			}

			long before = System.nanoTime();

			// do the comparison
			ArrayList<ComparisonPairTuple> result = FastComparer.doSyntacticComparison(assignmentFile.getAbsolutePath(),
					progLang, humanLang, simThreshold, 15, minMatchLength, maxPairs, simMeasurement, assignmentFile,
					assignmentParentDirPath, assignmentName, isMultipleFiles, "", filesToBeDeleted, numClusters,
					numStages, isSensitive);

			long executionTime = System.nanoTime() - before;

			filesToBeDeleted.add(new File(assignmentParentDirPath + "\\[out] " + assignmentName));

			// start deleting all files
			for (File f : filesToBeDeleted)
				FileManipulator.deleteAllTemporaryFiles(f);

			// calculate the effectiveness
			int copiedAndSuggested = 0;

			for (int i = 0; i < result.size(); i++) {
				ComparisonPairTuple r = result.get(i);

				if (r.getAssignmentName1().toLowerCase().contains("ref_")
						&& r.getAssignmentName2().toLowerCase().contains("ref_")) {
					// search the first and second nrp
					String[] temp = r.getAssignmentName1().split("_");
					String nrp1 = temp[temp.length - 1];
					temp = r.getAssignmentName2().split("_");
					String nrp2 = temp[temp.length - 1];

					// check whether they are in the same cluster
					for (String s : gold) {
						if (s.contains(nrp1) && s.contains(nrp2)) {
							copiedAndSuggested++;
							break;
						}
					}
				}
			}

			System.out.println(assignmentName + "\t" + simMeasurement + "\t" + copiedAndSuggested + "\t" + maxPairs
					+ "\t" + executionTime);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

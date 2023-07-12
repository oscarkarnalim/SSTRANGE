package sstrange.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import sstrange.FastComparer;
import sstrange.FileManipulator;
import sstrange.MainFrame;
import sstrange.matchgenerator.ComparisonPairTuple;

public class SOCOEval {

	private static int numStages = 1;
	private static int numClusters = 2;

	public static void main(String[] args) throws Exception {
		String assessmentRooPath = "C:\\Users\\oscar\\Desktop\\soco_f\\formatted";

		// read the gold standard data
		Scanner rf = new Scanner(new File("C:\\Users\\oscar\\Desktop\\soco_f\\soco_train_clones_fixed.txt"));
		ArrayList<String> goldResult = new ArrayList<String>();
		while (rf.hasNextLine()) {
			goldResult.add(rf.nextLine());
		}

		String simMeasurement = "";
		
		simMeasurement = "Super-Bit";
		extract(assessmentRooPath, goldResult, simMeasurement, 60);
		
//		for (int i = 11; i <= 20; i++) {
//			simMeasurement = "RKRGST";
//			extract(assessmentRooPath, goldResult, simMeasurement, i*10);
//		}

//		while(numStages <= 10) {
//			simMeasurement = "MinHash";
//			extract(assessmentRooPath, goldResult, simMeasurement, 20);
//			simMeasurement = "Super-Bit";
//			extract(assessmentRooPath, goldResult, simMeasurement, 60);
//			numStages++;
//		}
//		simMeasurement = "Jaccard";
//		extract(assessmentRooPath, goldResult, simMeasurement, 30);
//		simMeasurement = "Cosine";
//		extract(assessmentRooPath, goldResult, simMeasurement, 130);
//		simMeasurement = "RKRGST";
// 		extract(assessmentRooPath, goldResult, simMeasurement, 30);
	}

	private static void extract(String assessmentPath, ArrayList<String> goldResult, String simMeasurement,
			int minMatchLength) {

		String progLang = "java";

		int simThreshold = 50;
		int maxPairs = 100; // the closest yet larger value to 97, the number of copied programs
		boolean isMultipleFiles = false;
		boolean isCommonCodeAllowed = true;
		String humanLang = "en";

		// do the process
		try {
			ArrayList<File> filesToBeDeleted = new ArrayList<File>();

			File assignmentFile = new File(assessmentPath);
			String assignmentParentDirPath = assignmentFile.getParentFile().getAbsolutePath();
			String assignmentName = assignmentFile.getName();
			String additionalKeywordsPath = null;

			// if the programming language is either java or python, set it to STRANGE's
			// format
			if (progLang.equals("Java")) {
				progLang = "java";
				additionalKeywordsPath = "java_keywords.txt";
			} else if (progLang.equals("Python")) {
				progLang = "py";
				additionalKeywordsPath = "python_keywords.txt";
			}

			long before = System.nanoTime();

			// do the comparison
			ArrayList<ComparisonPairTuple> result = FastComparer.doSyntacticComparison(assessmentPath, progLang,
					humanLang, simThreshold, minMatchLength, maxPairs, "", simMeasurement, assignmentFile,
					assignmentParentDirPath, assignmentName, isMultipleFiles, isCommonCodeAllowed,
					filesToBeDeleted, numClusters, numStages);

			long executionTime = System.nanoTime() - before;

			// start deleting all files
			for (File f : filesToBeDeleted)
				FileManipulator.deleteAllTemporaryFiles(f);

			// calculate the effectiveness
			int copiedAndSuggested = 0;
			int suggested = result.size();
			for (int i = 0; i < result.size(); i++) {
				ComparisonPairTuple r = result.get(i);
				if (goldResult.contains(r.getAssignmentName1() + " " + r.getAssignmentName2()))
					copiedAndSuggested++;
			}

			// System.out.println(copiedAndSuggested + " " + suggested + " " +
			// goldResult.size());

			double precision = copiedAndSuggested * 1.0 / suggested;
			double recall = copiedAndSuggested * 1.0 / goldResult.size();
			double fscore = (2.0 * precision * recall) / (precision + recall);
			if (precision == 0 && recall == 0)
				fscore = 0;

			System.out.println(simMeasurement + "\t" + numStages + "\t" + precision + "\t" + recall + "\t" + fscore
					+ "\t" + executionTime);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

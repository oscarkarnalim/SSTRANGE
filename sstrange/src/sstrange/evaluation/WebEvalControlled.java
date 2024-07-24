package sstrange.evaluation;

import java.io.File;
import java.util.ArrayList;

import sstrange.MainFrame;
import sstrange.matchgenerator.ComparisonPairTuple;

public class WebEvalControlled {
	
	// add these on mainframe after printing execution time
	
	// WebEvalStdAsmt.times.add((double)(System.nanoTime() - before));
	
	// calculate effectiveness for web detection
	// will be removed in production
	// please also uncomment HTML generation in fast comparer web
	// WebEvalControlled.calculateSimDegreeAndMAP(result);
	// WebEvalStdAsmt.calculateSimDegreeAndMAP(result);
	
	public static void main(String[] args) {
		File root = new File("D:\\- Plagiarisme web Meli\\journal\\student exam");
		// root = new File("C:\\Users\\master\\Desktop\\php test");
		File[] submissions = root.listFiles();
		// process(root.getAbsolutePath(),"Cosine", true, submissions.length);
		process(root.getAbsolutePath(),"Super-Bit", true, submissions.length);
	}
	
	public static void process(String assignmentPath, String similarityMeasurement, boolean isSensitive, int totalSubs) {
		MainFrame.process(assignmentPath, "Multiple files in a directory", "html", "en",
				75, 15, totalSubs * totalSubs, 10, "none",
				similarityMeasurement, 2, 1, isSensitive); 
	}
	
	public static void calculateSimDegreeAndMAP(ArrayList<ComparisonPairTuple> result) {
		// this method is called inside main frame process method after printing the execution time
		
		int relevantMatches = 0;
		double MAP = 0.0d;
		// printing similarity degrees and calculating MAP
		for (int i = 0; i < result.size(); i++) {
			ComparisonPairTuple s = result.get(i);
			String[] tmp = s.getAssignmentName1().split("_");
			String nrp = tmp[tmp.length - 1];
			if(s.getAssignmentName2().endsWith(nrp)) {
				System.out.println(s.getAssignmentName1() + "\t" + s.getAssignmentName2() + "\t" + (int)s.getSimResult());
				// calculate MAP
				relevantMatches++;
				MAP += (1.0 * relevantMatches / (i+1));
				// System.out.println(relevantMatches / (i+1));
			}
		}
		
		System.out.println("MAP\t" + MAP/14);
		
	}
}

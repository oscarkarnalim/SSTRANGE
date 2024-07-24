package sstrange.evaluation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;

import sstrange.matchgenerator.ComparisonPairTuple;

public class WebEvalStdAsmt {
	private static ArrayList<String> relevantPairs;
	public static ArrayList<Double> times = new ArrayList<>();
	private static ArrayList<Double> MAPS = new ArrayList<>();
	private static ArrayList<Double> simDegrees = new ArrayList<>();
	private static ArrayList<String> asmtNames = new ArrayList<>();
	
	// add these on mainframe after printing execution time
	
	// WebEvalStdAsmt.times.add((double)(System.nanoTime() - before));
	
	// calculate effectiveness for web detection
	// will be removed in production
	// please also uncomment HTML generation in fast comparer web
	// WebEvalControlled.calculateSimDegreeAndMAP(result);
	// WebEvalStdAsmt.calculateSimDegreeAndMAP(result);

	public static void main(String[] args) throws Exception {
		FileOutputStream fos = new FileOutputStream(new File("output.txt"), true);
		PrintStream ps = new PrintStream(fos);
		System.setOut(ps);
		
		File root = new File("D:\\- Plagiarisme web Meli\\journal\\student weekly asmt");
		File[] asmts = root.listFiles();
		for (File asmt : asmts) {
			if (asmt.isDirectory()) {
				// if a directory, it is for assessment
				System.err.println(asmt);
				System.out.println(asmt);
				asmtNames.add(asmt.getName());

				// obtaining the relevant pairs
				relevantPairs = new ArrayList<>();
				File goldStandard = new File(asmt.getAbsolutePath() + ".txt");
				if (goldStandard.exists()) {
					Scanner sc = new Scanner(goldStandard);
					while (sc.hasNextLine()) {
						String line = sc.nextLine();
						if (line.length() > 0) {
							String[] pair = line.split(" ");
							relevantPairs.add(pair[0].trim() + "," + pair[1].trim());
						}
					}
					sc.close();
				}

				// process
				File[] submissions = asmt.listFiles();
				WebEvalControlled.process(asmt.getAbsolutePath(), "Cosine", false, submissions.length);
			}
		}
		
		// print 
		System.out.println("Summary");
		for(int i=0;i< asmtNames.size();i++) {
			String name = asmtNames.get(i);
			System.out.println(name + "\t" + times.get(i) + "\t" + MAPS.get(i) + "\t" + simDegrees.get(i));
		}

	}

	public static void calculateSimDegreeAndMAP(ArrayList<ComparisonPairTuple> result) {
		// this method is called inside main frame process method after printing the
		// execution time

		int relevantMatches = 0;
		double MAP = 0.0d;
		double simDegree = 0.0d;
		// printing similarity degrees and calculating MAP
		for (int i = 0; i < result.size(); i++) {
			ComparisonPairTuple s = result.get(i);

			for (String pair : relevantPairs) {
				String[] ids = pair.split(",");
				if ((s.getAssignmentName1().contains(ids[0]) && s.getAssignmentName2().contains(ids[1]))
						|| (s.getAssignmentName1().contains(ids[1]) && s.getAssignmentName2().contains(ids[0]))) {
					// relevant
					System.out.println(
							s.getAssignmentName1() + "\t" + s.getAssignmentName2() + "\t" + s.getSimResult());
					// calculate MAP
					relevantMatches++;
					MAP += (1.0 * relevantMatches / (i + 1));
					// update sim degree
					simDegree += s.getSimResult();
				}
			}
		}

		System.out.println("MAP\t" + MAP / relevantPairs.size());

		MAPS.add(MAP / relevantPairs.size());
		simDegrees.add(simDegree / relevantPairs.size());
	}

}

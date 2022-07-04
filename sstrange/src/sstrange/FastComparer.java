package sstrange;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import info.debatty.java.lsh.LSHMinHash;
import info.debatty.java.lsh.LSHSuperBit;
import p3.feedbackgenerator.comparison.Comparer;
import p3.feedbackgenerator.language.java.JavaFeedbackGenerator;
import p3.feedbackgenerator.language.python.PythonFeedbackGenerator;
import p3.feedbackgenerator.message.FeedbackMessageGenerator;
import p3.feedbackgenerator.token.FeedbackToken;
import sstrange.evaluation.SOCOEffectivenessEval;
import sstrange.lshcalculator.CosineCalculator;
import sstrange.lshcalculator.IndexGenerator;
import sstrange.lshcalculator.JaccardCalculator;
import sstrange.matchgenerator.ComparisonPairTuple;
import sstrange.matchgenerator.MatchGenerator;
import support.AdditionalKeywordsManager;
import support.stringmatching.GSTMatchTuple;

public class FastComparer {
	public static ArrayList<ComparisonPairTuple> doSyntacticComparison(String assignmentPath, String progLang, String humanLang, int simThreshold,
			int minMatchingLength, int maxPairs, String templateDirPath, String similarityMeasurement,
			File assignmentFile, String assignmentParentDirPath, String assignmentName, boolean isMultipleFiles,
			boolean isCommonCodeAllowed, String additionalKeywordsPath, ArrayList<File> filesToBeDeleted) {

		// if multiple files, merge them
		if (isMultipleFiles) {
			String newAssignmentPath = assignmentParentDirPath + File.separator + "[merged] " + assignmentName;
			CodeMerger.mergeCode(assignmentPath, progLang, newAssignmentPath);
			assignmentPath = newAssignmentPath;
			// mark new assignment path to be deleted after the whole process
			filesToBeDeleted.add(new File(newAssignmentPath));
		}

		// remove common and template code for java and python if needed
		assignmentPath = JavaPyCommonTemplateRemover.removeCommonAndTemplateCodeJavaPython(assignmentPath,
				minMatchingLength, templateDirPath, isCommonCodeAllowed, progLang, assignmentParentDirPath,
				assignmentName, filesToBeDeleted, additionalKeywordsPath);

		// get additional keywords for STRANGE
		ArrayList<ArrayList<String>> additionalKeywords = new ArrayList<ArrayList<String>>();
		additionalKeywords = AdditionalKeywordsManager.readAdditionalKeywords(additionalKeywordsPath);
		// set result dir based on the name of assignment
		String resultPath = assignmentParentDirPath + File.separator + "[out] " + assignmentName;

		// generate STRANGE reports
		return compareAndGenerateHTMLReports(assignmentPath, resultPath, progLang, humanLang, simThreshold, minMatchingLength,
				maxPairs, humanLang, additionalKeywords, similarityMeasurement);

	}

	private static ArrayList<ComparisonPairTuple> compareAndGenerateHTMLReports(String dirPath, String resultPath, String progLang,
			String humanLang, int simThreshold, int minMatchLength, int maxPairs, String languageCode,
			ArrayList<ArrayList<String>> additionalKeywords, String similarityMeasurement) {
		// create the output dir
		File resultDir = new File(resultPath);
		resultDir.mkdir();

		File[] assignments = (new File(dirPath)).listFiles();

		// tokenise all programs and generate the indexes
		ArrayList<ArrayList<FeedbackToken>> tokenStrings = new ArrayList<ArrayList<FeedbackToken>>();
		ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();
		for (int i = 0; i < assignments.length; i++) {
			// for each code, tokenise and generalise
			File code = Comparer.getCode(assignments[i], progLang);

			if (code == null) {
				tokenStrings.add(new ArrayList<FeedbackToken>());
				tokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
			} else {
				// generate token string
				ArrayList<FeedbackToken> tokenString = STRANGEPairGenerator.getTokenString(code.getAbsolutePath(),
						progLang);

				// generalise some tokens
				if (progLang.equals("java")) {
					tokenString = JavaFeedbackGenerator.syntaxTokenStringPreprocessing(tokenString, additionalKeywords,
							true);
				} else if (progLang.equals("py")) {
					PythonFeedbackGenerator.syntaxTokenStringPreprocessing(tokenString, additionalKeywords);
				}

				// add the processed token string
				tokenStrings.add(tokenString);

				// generate the index
				HashMap<String, ArrayList<Integer>> tokenIndex = IndexGenerator.generateIndex(tokenString,
						minMatchLength);

				// add the token index
				tokenIndexes.add(tokenIndex);
			}
		}

		

		if (similarityMeasurement.equalsIgnoreCase("MinHash")) {
			// generate vector header
			ArrayList<String> vectorHeader = IndexGenerator.generateVectorHeader(tokenIndexes);
			
			// calculate minhash
			return _compareAndGenerateHTMLReportsMinHash(assignments, tokenStrings, tokenIndexes, vectorHeader, dirPath,
					resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs, languageCode,
					additionalKeywords);
		} else if (similarityMeasurement.equalsIgnoreCase("Super-Bit")) {
			// generate vector header
			ArrayList<String> vectorHeader = IndexGenerator.generateVectorHeader(tokenIndexes);
			
			// calculate super-bit
			return _compareAndGenerateHTMLReportsSuoerBit(assignments, tokenStrings, tokenIndexes, vectorHeader, dirPath,
					resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs, languageCode,
					additionalKeywords);
		}else if (similarityMeasurement.equalsIgnoreCase("Jaccard")) {
			return _compareAndGenerateHTMLReportsJaccard(assignments, tokenStrings, tokenIndexes, dirPath,
					resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs, languageCode,
					additionalKeywords);
		} else if (similarityMeasurement.equalsIgnoreCase("Cosine")) {
			return _compareAndGenerateHTMLReportsCosine(assignments, tokenStrings, tokenIndexes, dirPath,
					resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs, languageCode,
					additionalKeywords);
		} else {
			// RKRGST
			return _compareAndGenerateHTMLReportsRKRGST(assignments, tokenStrings, dirPath, resultPath, progLang, humanLang,
					simThreshold, minMatchLength, maxPairs, languageCode, additionalKeywords);
		}

	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsRKRGST(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, String dirPath, String resultPath, String progLang,
			String humanLang, int simThreshold, int minMatchLength, int maxPairs, String languageCode,
			ArrayList<ArrayList<String>> additionalKeywords) {
		// Jaccard
		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();

			// do the comparison
			for (int j = 0; j < tokenStrings.size(); j++) {
				for (int k = j + 1; k < tokenStrings.size(); k++) {
					// get matched tiles with RKRGST
					ArrayList<GSTMatchTuple> simTuples = FeedbackMessageGenerator.generateMatchedTuples(tokenStrings.get(j), tokenStrings.get(k),
							minMatchLength);
					
					// get the sim degree for RKRGST
					int simDegree =  (int)(MatchGenerator.calcAverageSimilarity(simTuples, tokenStrings.get(j).size(), tokenStrings.get(k).size())*100);

					//if (simDegree >= simThreshold) {
						String dirname1 = assignments[j].getName();
						String dirname2 = assignments[k].getName();

						File code1 = Comparer.getCode(assignments[j], progLang);
						File code2 = Comparer.getCode(assignments[k], progLang);
						
						// to deal with non-code directories and files
						if(code1 == null || code2 == null)
							continue;

						// add the comparison pair
						codePairs.add(new ComparisonPairTuple(code1.getAbsolutePath(), code2.getAbsolutePath(),
								dirname1, dirname2, simDegree, 1, simTuples));
					//}
				}
			}

			// sort in descending order based on average syntax
			Collections.sort(codePairs);

			// remove extra pairs
			while (codePairs.size() > maxPairs) {
				codePairs.remove(codePairs.size() - 1);
			}

//			// generate similarity reports
//			for (int i = 0; i < codePairs.size(); i++) {
//				ComparisonPairTuple ct = codePairs.get(i);
//				String syntacticFilename = "obs" + i + ".html";
//				String syntacticFilepath = resultPath + File.separator + syntacticFilename;
//
//				// set the path to comparison pair tuple
//				ct.setResultedHTMLFilename(syntacticFilename);
//
//				if (progLang.equals("java")) {
//					JavaHtmlGenerator.generateHtmlForSSTRANGE(ct.getCodePath1(), ct.getCodePath2(),
//							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
//							syntacticFilepath, minMatchLength, ct.getClusterID(), humanLang, additionalKeywords);
//				} else if (progLang.equals("py")) {
//					PythonHtmlGenerator.generateHtmlForSSTRANGE(ct.getCodePath1(), ct.getCodePath2(),
//							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
//							syntacticFilepath, minMatchLength, ct.getClusterID(), humanLang, additionalKeywords);
//				}
//			}
//
//			// generate the index HTML
//			IndexHTMLGenerator.generateHtml(dirPath, codePairs, MainFrame.indexTemplatePath, resultPath, simThreshold,
//					humanLang);
//			// generate additional files
//			STRANGEPairGenerator.generateAdditionalDir(resultPath);
			
			// return the codepairs as the result
			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsJaccard(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes, String dirPath,
			String resultPath, String progLang, String humanLang, int simThreshold, int minMatchLength, int maxPairs,
			String languageCode, ArrayList<ArrayList<String>> additionalKeywords) {
		// Jaccard
		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();

			// do the comparison
			for (int j = 0; j < tokenIndexes.size(); j++) {
				for (int k = j + 1; k < tokenIndexes.size(); k++) {
					// get the sim degree for jaccard
					int simDegree = (int) (JaccardCalculator.calculateJaccardSimilarity(tokenIndexes.get(j), tokenIndexes.get(k)) * 100);

					//if (simDegree >= simThreshold) {
						String dirname1 = assignments[j].getName();
						String dirname2 = assignments[k].getName();

						File code1 = Comparer.getCode(assignments[j], progLang);
						File code2 = Comparer.getCode(assignments[k], progLang);
						
						// to deal with non-code directories and files
						if(code1 == null || code2 == null)
							continue;
						
						// generate the matches
						ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndexes.get(j), tokenIndexes.get(k),
								minMatchLength);

						// add the comparison pair
						codePairs.add(new ComparisonPairTuple(code1.getAbsolutePath(), code2.getAbsolutePath(),
								dirname1, dirname2, simDegree, 1, matches));
					//}
				}
			}

			// sort in descending order based on average syntax
			Collections.sort(codePairs);

			// remove extra pairs
			while (codePairs.size() > maxPairs) {
				codePairs.remove(codePairs.size() - 1);
			}

//			// generate similarity reports
//			for (int i = 0; i < codePairs.size(); i++) {
//				ComparisonPairTuple ct = codePairs.get(i);
//				String syntacticFilename = "obs" + i + ".html";
//				String syntacticFilepath = resultPath + File.separator + syntacticFilename;
//
//				// set the path to comparison pair tuple
//				ct.setResultedHTMLFilename(syntacticFilename);
//
//				if (progLang.equals("java")) {
//					JavaHtmlGenerator.generateHtmlForSSTRANGE(ct.getCodePath1(), ct.getCodePath2(),
//							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
//							syntacticFilepath, minMatchLength, ct.getClusterID(), humanLang, additionalKeywords);
//				} else if (progLang.equals("py")) {
//					PythonHtmlGenerator.generateHtmlForSSTRANGE(ct.getCodePath1(), ct.getCodePath2(),
//							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
//							syntacticFilepath, minMatchLength, ct.getClusterID(), humanLang, additionalKeywords);
//				}
//			}
//
//			// generate the index HTML
//			IndexHTMLGenerator.generateHtml(dirPath, codePairs, MainFrame.indexTemplatePath, resultPath, simThreshold,
//					humanLang);
//			// generate additional files
//			STRANGEPairGenerator.generateAdditionalDir(resultPath);
			
			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsCosine(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes, String dirPath,
			String resultPath, String progLang, String humanLang, int simThreshold, int minMatchLength, int maxPairs,
			String languageCode, ArrayList<ArrayList<String>> additionalKeywords) {
		// Jaccard
		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();

			// do the comparison
			for (int j = 0; j < tokenIndexes.size(); j++) {
				for (int k = j + 1; k < tokenIndexes.size(); k++) {
					// get the sim degree for jaccard
					int simDegree = (int) (CosineCalculator.calculateCosineSimilarity(tokenIndexes.get(j),
							tokenIndexes.get(k)) * 100);

					//if (simDegree >= simThreshold) {
						String dirname1 = assignments[j].getName();
						String dirname2 = assignments[k].getName();

						File code1 = Comparer.getCode(assignments[j], progLang);
						File code2 = Comparer.getCode(assignments[k], progLang);
						
						// to deal with non-code directories and files
						if(code1 == null || code2 == null)
							continue;
						
						// generate the matches
						ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndexes.get(j), tokenIndexes.get(k),
								minMatchLength);

						// add the comparison pair
						codePairs.add(new ComparisonPairTuple(code1.getAbsolutePath(), code2.getAbsolutePath(),
								dirname1, dirname2, simDegree, 1, matches));
					//}
				}
			}

			// sort in descending order based on average syntax
			Collections.sort(codePairs);

			// remove extra pairs
			while (codePairs.size() > maxPairs) {
				codePairs.remove(codePairs.size() - 1);
			}

//			// generate similarity reports
//			for (int i = 0; i < codePairs.size(); i++) {
//				ComparisonPairTuple ct = codePairs.get(i);
//				String syntacticFilename = "obs" + i + ".html";
//				String syntacticFilepath = resultPath + File.separator + syntacticFilename;
//
//				// set the path to comparison pair tuple
//				ct.setResultedHTMLFilename(syntacticFilename);
//
//				if (progLang.equals("java")) {
//					JavaHtmlGenerator.generateHtmlForSSTRANGE(ct.getCodePath1(), ct.getCodePath2(),
//							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
//							syntacticFilepath, minMatchLength, ct.getClusterID(), humanLang, additionalKeywords);
//				} else if (progLang.equals("py")) {
//					PythonHtmlGenerator.generateHtmlForSSTRANGE(ct.getCodePath1(), ct.getCodePath2(),
//							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
//							syntacticFilepath, minMatchLength, ct.getClusterID(), humanLang, additionalKeywords);
//				}
//			}
//
//			// generate the index HTML
//			IndexHTMLGenerator.generateHtml(dirPath, codePairs, MainFrame.indexTemplatePath, resultPath, simThreshold,
//					humanLang);
//			// generate additional files
//			STRANGEPairGenerator.generateAdditionalDir(resultPath);
			
			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsMinHash(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes, ArrayList<String> vectorHeader, String dirPath,
			String resultPath, String progLang, String humanLang, int simThreshold, int minMatchLength, int maxPairs,
			String languageCode, ArrayList<ArrayList<String>> additionalKeywords) {
		// MinHash algorithm
		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();

			// generate Jaccard vectors
			ArrayList<boolean[]> tokenVectors = new ArrayList<boolean[]>();
			for (int i = 0; i < tokenIndexes.size(); i++) {
				tokenVectors
						.add(JaccardCalculator.generateBooleanVectorFromTokenString(tokenIndexes.get(i), vectorHeader));
			}

			// create MinHash object with best default setting
			int lshstages = SOCOEffectivenessEval.numStages;
			int lshbuckets = 2;
			int lshn = vectorHeader.size();
			LSHMinHash lsh = new LSHMinHash(lshstages, lshbuckets, lshn);

			// to store clusters; the index refers to cluster ID while the list in the value
			// refers to submission IDs
			ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>();
			for (int i = 0; i < lshbuckets; i++)
				clusters.add(new ArrayList<Integer>());

			// binning the vectors
			for (int i = 0; i < tokenVectors.size(); i++) {
				// get the hash
				int[] hash = lsh.hash(tokenVectors.get(i));
				// get the cluster ID
				int clusterID = hash[lshstages-1];
				// System.out.println(clusterID + " " + assignments[i].getAbsolutePath());
				// add the submission index to given cluster
				clusters.get(clusterID).add(i);
			}

			// start generating similarity report per bucket/cluster
			for (int i = 0; i < clusters.size(); i++) {
				// per cluster
				ArrayList<Integer> submissionIDs = clusters.get(i);
				for (int j = 0; j < submissionIDs.size(); j++) {
					int submissionID1 = submissionIDs.get(j);
					for (int k = j + 1; k < submissionIDs.size(); k++) {
						int submissionID2 = submissionIDs.get(k);

						HashMap<String, ArrayList<Integer>> tokenIndex1 = tokenIndexes.get(submissionID1);
						HashMap<String, ArrayList<Integer>> tokenIndex2 = tokenIndexes.get(submissionID2);

						// generate the matches
						ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndex1, tokenIndex2,
								minMatchLength);

						// get the sim degree
						int simDegree = (int) (MatchGenerator.calcAverageSimilarity(matches,
								tokenStrings.get(submissionID2).size(), tokenStrings.get(submissionID2).size()) * 100);

						//if (simDegree >= simThreshold) {
							String dirname1 = assignments[submissionID1].getName();
							String dirname2 = assignments[submissionID2].getName();

							File code1 = Comparer.getCode(assignments[submissionID1], progLang);
							File code2 = Comparer.getCode(assignments[submissionID2], progLang);
							
							// to deal with non-code directories and files
							if(code1 == null || code2 == null)
								continue;

							// add the comparison pair
							codePairs.add(new ComparisonPairTuple(code1.getAbsolutePath(), code2.getAbsolutePath(),
									dirname1, dirname2, simDegree, i, matches));
						//}
					}
				}
			}

			// sort in descending order based on average syntax
			Collections.sort(codePairs);

			// remove extra pairs
			while (codePairs.size() > maxPairs) {
				codePairs.remove(codePairs.size() - 1);
			}

//			// generate similarity reports
//			for (int i = 0; i < codePairs.size(); i++) {
//				ComparisonPairTuple ct = codePairs.get(i);
//				String syntacticFilename = "obs" + i + ".html";
//				String syntacticFilepath = resultPath + File.separator + syntacticFilename;
//
//				// set the path to comparison pair tuple
//				ct.setResultedHTMLFilename(syntacticFilename);
//
//				if (progLang.equals("java")) {
//					JavaHtmlGenerator.generateHtmlForSSTRANGE(ct.getCodePath1(), ct.getCodePath2(),
//							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
//							syntacticFilepath, minMatchLength, ct.getClusterID(), humanLang, additionalKeywords, ct.getMatches());
//				} else if (progLang.equals("py")) {
//					PythonHtmlGenerator.generateHtmlForSSTRANGE(ct.getCodePath1(), ct.getCodePath2(),
//							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
//							syntacticFilepath, minMatchLength, ct.getClusterID(), humanLang, additionalKeywords, ct.getMatches());
//				}
//			}
//
//			// generate the index HTML
//			IndexHTMLGenerator.generateHtml(dirPath, codePairs, MainFrame.indexTemplatePath, resultPath, simThreshold,
//					humanLang);
//			// generate additional files
//			STRANGEPairGenerator.generateAdditionalDir(resultPath);
			
			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsSuoerBit(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes, ArrayList<String> vectorHeader, String dirPath,
			String resultPath, String progLang, String humanLang, int simThreshold, int minMatchLength, int maxPairs,
			String languageCode, ArrayList<ArrayList<String>> additionalKeywords) {
		// Super-Bit algorithm
		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();

			// generate Cosine vectors
			ArrayList<double[]> tokenVectors = new ArrayList<double[]>();
			for (int i = 0; i < tokenIndexes.size(); i++) {
				tokenVectors.add(
						CosineCalculator.generateOccurrenceVectorFromTokenString(tokenIndexes.get(i), vectorHeader));
			}

			// create SuperBit object with best default setting
			int lshstages = SOCOEffectivenessEval.numStages;
			int lshbuckets = 2;
			int lshn = vectorHeader.size();
			LSHSuperBit lsh = new LSHSuperBit(lshstages, lshbuckets, lshn);

			// to store clusters; the index refers to cluster ID while the list in the value
			// refers to submission IDs
			ArrayList<ArrayList<Integer>> clusters = new ArrayList<ArrayList<Integer>>();
			for (int i = 0; i < lshbuckets; i++)
				clusters.add(new ArrayList<Integer>());

			// binning the vectors
			for (int i = 0; i < tokenVectors.size(); i++) {
				// get the hash
				int[] hash = lsh.hash(tokenVectors.get(i));
				// get the cluster ID
				int clusterID = hash[lshstages-1];
				// System.out.println(clusterID + " " + assignments[i].getAbsolutePath());
				// add the submission index to given cluster
				clusters.get(clusterID).add(i);
			}

			// start generating similarity report per bucket/cluster
			for (int i = 0; i < clusters.size(); i++) {
				// per cluster
				ArrayList<Integer> submissionIDs = clusters.get(i);
				for (int j = 0; j < submissionIDs.size(); j++) {
					int submissionID1 = submissionIDs.get(j);
					for (int k = j + 1; k < submissionIDs.size(); k++) {
						int submissionID2 = submissionIDs.get(k);

						HashMap<String, ArrayList<Integer>> tokenIndex1 = tokenIndexes.get(submissionID1);
						HashMap<String, ArrayList<Integer>> tokenIndex2 = tokenIndexes.get(submissionID2);

						// generate the matches
						ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndex1, tokenIndex2,
								minMatchLength);

						// get the sim degree
						int simDegree = (int) (MatchGenerator.calcAverageSimilarity(matches,
								tokenStrings.get(submissionID2).size(), tokenStrings.get(submissionID2).size()) * 100);

						//if (simDegree >= simThreshold) {
							String dirname1 = assignments[submissionID1].getName();
							String dirname2 = assignments[submissionID2].getName();

							File code1 = Comparer.getCode(assignments[submissionID1], progLang);
							File code2 = Comparer.getCode(assignments[submissionID2], progLang);
							
							// to deal with non-code directories and files
							if(code1 == null || code2 == null)
								continue;

							// add the comparison pair
							codePairs.add(new ComparisonPairTuple(code1.getAbsolutePath(), code2.getAbsolutePath(),
									dirname1, dirname2, simDegree, i, matches));
						//}
					}
				}
			}

			// sort in descending order based on average syntax
			Collections.sort(codePairs);

			// remove extra pairs
			while (codePairs.size() > maxPairs) {
				codePairs.remove(codePairs.size() - 1);
			}

//			// generate similarity reports
//			for (int i = 0; i < codePairs.size(); i++) {
//				ComparisonPairTuple ct = codePairs.get(i);
//				String syntacticFilename = "obs" + i + ".html";
//				String syntacticFilepath = resultPath + File.separator + syntacticFilename;
//
//				// set the path to comparison pair tuple
//				ct.setResultedHTMLFilename(syntacticFilename);
//
//				if (progLang.equals("java")) {
//					JavaHtmlGenerator.generateHtmlForSSTRANGE(ct.getCodePath1(), ct.getCodePath2(),
//							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
//							syntacticFilepath, minMatchLength, ct.getClusterID(), humanLang, additionalKeywords);
//				} else if (progLang.equals("py")) {
//					PythonHtmlGenerator.generateHtmlForSSTRANGE(ct.getCodePath1(), ct.getCodePath2(),
//							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
//							syntacticFilepath, minMatchLength, ct.getClusterID(), humanLang, additionalKeywords);
//				}
//			}
//
//			// generate the index HTML
//			IndexHTMLGenerator.generateHtml(dirPath, codePairs, MainFrame.indexTemplatePath, resultPath, simThreshold,
//					humanLang);
//			// generate additional files
//			STRANGEPairGenerator.generateAdditionalDir(resultPath);
			
			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
}

package sstrange;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import info.debatty.java.lsh.LSHMinHash;
import info.debatty.java.lsh.LSHSuperBit;
import sstrange.htmlgenerator.CodeReader;
import sstrange.language.java.JavaFeedbackGenerator;
import sstrange.language.java.JavaHtmlGenerator;
import sstrange.language.python.PythonFeedbackGenerator;
import sstrange.language.python.PythonHtmlGenerator;
import sstrange.lshcalculator.CosineCalculator;
import sstrange.lshcalculator.IndexGenerator;
import sstrange.lshcalculator.JaccardCalculator;
import sstrange.matchgenerator.ComparisonPairTuple;
import sstrange.matchgenerator.MatchGenerator;
import sstrange.message.FeedbackMessageGenerator;
import sstrange.token.FeedbackToken;
import sstrange.token.FeedbackTokenComparator;
import support.AdditionalKeywordsManager;
import support.stringmatching.GSTMatchTuple;

public class FastComparer {

	public static ArrayList<ComparisonPairTuple> doSyntacticComparison(String assignmentPath, String progLang,
			String humanLang, int simThreshold, int minMatchingLength, int maxPairs, String templateDirPath,
			String similarityMeasurement, File assignmentFile, String assignmentParentDirPath, String assignmentName,
			boolean isMultipleFiles, boolean isCommonCodeAllowed, String additionalKeywordsPath,
			ArrayList<File> filesToBeDeleted, int numClusters, int numStages) {

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
		return compareAndGenerateHTMLReports(assignmentPath, resultPath, progLang, humanLang, simThreshold,
				minMatchingLength, maxPairs, humanLang, additionalKeywords, similarityMeasurement, numClusters,
				numStages);

	}

	private static ArrayList<ComparisonPairTuple> compareAndGenerateHTMLReports(String dirPath, String resultPath,
			String progLang, String humanLang, int simThreshold, int minMatchLength, int maxPairs, String languageCode,
			ArrayList<ArrayList<String>> additionalKeywords, String similarityMeasurement, int numClusters,
			int numStages) {
		// create the output dir
		File resultDir = new File(resultPath);
		resultDir.mkdir();

		File[] assignments = (new File(dirPath)).listFiles();

		// tokenise all programs and generate the indexes
		ArrayList<ArrayList<FeedbackToken>> tokenStrings = new ArrayList<ArrayList<FeedbackToken>>();
		ArrayList<ArrayList<FeedbackToken>> rawTokenStrings = new ArrayList<ArrayList<FeedbackToken>>();
		ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();
		for (int i = 0; i < assignments.length; i++) {
			// for each code, tokenise and generalise
			File code = CodeReader.getCode(assignments[i], progLang);

			if (code == null) {
				tokenStrings.add(new ArrayList<FeedbackToken>());
				tokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
			} else {
				// generate token string
				ArrayList<FeedbackToken> rawTokenString = null, tokenString = null;

				if (progLang.equals("java")) {
					rawTokenString = JavaFeedbackGenerator.generateFeedbackTokenString(code.getAbsolutePath());
					JavaFeedbackGenerator.mergeAdjacentCommentsOnTokenString(rawTokenString);

					tokenString = JavaFeedbackGenerator.syntaxTokenStringPreprocessing(rawTokenString,
							additionalKeywords);
				} else if (progLang.equals("py")) {
					rawTokenString = new ArrayList<>();
					tokenString = PythonFeedbackGenerator.generateSyntaxTokenString(code.getAbsolutePath());
					rawTokenString.addAll(tokenString);
					ArrayList<FeedbackToken> commentWhitespaceString = PythonFeedbackGenerator
							.generateCommentAndWhitespaceTokens(code.getAbsolutePath());
					rawTokenString.addAll(commentWhitespaceString);
					Collections.sort(rawTokenString, new FeedbackTokenComparator());
					PythonFeedbackGenerator.mergeAdjacentCommentsAndWhitespacesOnTokenString(rawTokenString);

					PythonFeedbackGenerator.syntaxTokenStringPreprocessing(tokenString, additionalKeywords);
				}

				// add the token string and its raw form
				tokenStrings.add(tokenString);
				rawTokenStrings.add(rawTokenString);

				// if not RKRGST
				if (similarityMeasurement.equalsIgnoreCase("RKRGST") == false) {
					// generate the index
					HashMap<String, ArrayList<Integer>> tokenIndex = IndexGenerator.generateIndex(tokenString,
							minMatchLength);

					// add the token index
					tokenIndexes.add(tokenIndex);
				}
			}
		}

		if (similarityMeasurement.equalsIgnoreCase("MinHash")) {
			// generate vector header
			ArrayList<String> vectorHeader = IndexGenerator.generateVectorHeader(tokenIndexes);

			// calculate minhash
			return _compareAndGenerateHTMLReportsMinHash(assignments, tokenStrings, rawTokenStrings, tokenIndexes,
					vectorHeader, dirPath, resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs,
					languageCode, additionalKeywords, numClusters, numStages);
		} else if (similarityMeasurement.equalsIgnoreCase("Super-Bit")) {
			// generate vector header
			ArrayList<String> vectorHeader = IndexGenerator.generateVectorHeader(tokenIndexes);

			// calculate super-bit
			return _compareAndGenerateHTMLReportsSuoerBit(assignments, tokenStrings, rawTokenStrings, tokenIndexes,
					vectorHeader, dirPath, resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs,
					languageCode, additionalKeywords, numClusters, numStages);
		} else if (similarityMeasurement.equalsIgnoreCase("Jaccard")) {
			return _compareAndGenerateHTMLReportsJaccard(assignments, tokenStrings, rawTokenStrings, tokenIndexes,
					dirPath, resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs, languageCode,
					additionalKeywords);
		} else if (similarityMeasurement.equalsIgnoreCase("Cosine")) {
			return _compareAndGenerateHTMLReportsCosine(assignments, tokenStrings, rawTokenStrings, tokenIndexes,
					dirPath, resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs, languageCode,
					additionalKeywords);
		} else {
			// RKRGST
			return _compareAndGenerateHTMLReportsRKRGST(assignments, tokenStrings, rawTokenStrings, dirPath, resultPath,
					progLang, humanLang, simThreshold, minMatchLength, maxPairs, languageCode, additionalKeywords);
		}

	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsRKRGST(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, ArrayList<ArrayList<FeedbackToken>> rawTokenStrings,
			String dirPath, String resultPath, String progLang, String humanLang, int simThreshold, int minMatchLength,
			int maxPairs, String languageCode, ArrayList<ArrayList<String>> additionalKeywords) {
		// RKRGST
		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();

			// do the comparison
			for (int j = 0; j < tokenStrings.size(); j++) {
				for (int k = j + 1; k < tokenStrings.size(); k++) {
					// get matched tiles with RKRGST
					ArrayList<GSTMatchTuple> simTuples = FeedbackMessageGenerator
							.generateMatchedTuples(tokenStrings.get(j), tokenStrings.get(k), minMatchLength);

					// get the sim degree for RKRGST
					int simDegree = (int) (MatchGenerator.calcAverageSimilarity(simTuples, tokenStrings.get(j).size(),
							tokenStrings.get(k).size()) * 100);

					if (simDegree >= simThreshold) {
						String dirname1 = assignments[j].getName();
						String dirname2 = assignments[k].getName();

						File code1 = CodeReader.getCode(assignments[j], progLang);
						File code2 = CodeReader.getCode(assignments[k], progLang);

						// to deal with non-code directories and files
						if (code1 == null || code2 == null)
							continue;

						// add the comparison pair
						codePairs.add(new ComparisonPairTuple(j, k, dirname1, dirname2, simDegree, 1, simTuples));
					}
				}
			}

			// sort in descending order based on average syntax
			Collections.sort(codePairs);

			// remove extra pairs
			while (codePairs.size() > maxPairs) {
				codePairs.remove(codePairs.size() - 1);
			}

			// generate similarity reports
			for (int i = 0; i < codePairs.size(); i++) {
				ComparisonPairTuple ct = codePairs.get(i);
				String syntacticFilename = "obs" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], progLang);
				File code2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], progLang);

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				if (progLang.equals("java")) {
					JavaHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							rawTokenStrings.get(ct.getSubmissionID1()), rawTokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							additionalKeywords, ct.getMatches());
				} else if (progLang.equals("py")) {
					PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							rawTokenStrings.get(ct.getSubmissionID1()), rawTokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							additionalKeywords, ct.getMatches());
				}
			}

			// generate the index HTML
			IndexHTMLGenerator.generateHtml(dirPath, codePairs, MainFrame.indexTemplatePath, resultPath, simThreshold,
					humanLang);
			// generate additional files
			STRANGEPairGenerator.generateAdditionalDir(resultPath);

			// return the codepairs as the result
			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsJaccard(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, ArrayList<ArrayList<FeedbackToken>> rawTokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes, String dirPath, String resultPath,
			String progLang, String humanLang, int simThreshold, int minMatchLength, int maxPairs, String languageCode,
			ArrayList<ArrayList<String>> additionalKeywords) {
		// Jaccard
		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();

			// do the comparison
			for (int j = 0; j < tokenIndexes.size(); j++) {
				for (int k = j + 1; k < tokenIndexes.size(); k++) {
					// get the sim degree for jaccard
					int simDegree = (int) (JaccardCalculator.calculateJaccardSimilarity(tokenIndexes.get(j),
							tokenIndexes.get(k)) * 100);

					if (simDegree >= simThreshold) {
						String dirname1 = assignments[j].getName();
						String dirname2 = assignments[k].getName();

						File code1 = CodeReader.getCode(assignments[j], progLang);
						File code2 = CodeReader.getCode(assignments[k], progLang);

						// to deal with non-code directories and files
						if (code1 == null || code2 == null)
							continue;

						// generate the matches
						ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndexes.get(j),
								tokenIndexes.get(k), minMatchLength);

						// add the comparison pair
						codePairs.add(new ComparisonPairTuple(j, k, dirname1, dirname2, simDegree, 1, matches));
					}
				}
			}

			// sort in descending order based on average syntax
			Collections.sort(codePairs);

			// remove extra pairs
			while (codePairs.size() > maxPairs) {
				codePairs.remove(codePairs.size() - 1);
			}

			// generate similarity reports
			for (int i = 0; i < codePairs.size(); i++) {
				ComparisonPairTuple ct = codePairs.get(i);
				String syntacticFilename = "obs" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], progLang);
				File code2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], progLang);

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				if (progLang.equals("java")) {
					JavaHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							rawTokenStrings.get(ct.getSubmissionID1()), rawTokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							additionalKeywords, ct.getMatches());
				} else if (progLang.equals("py")) {
					PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							rawTokenStrings.get(ct.getSubmissionID1()), rawTokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							additionalKeywords, ct.getMatches());
				}
			}

			// generate the index HTML
			IndexHTMLGenerator.generateHtml(dirPath, codePairs, MainFrame.indexTemplatePath, resultPath, simThreshold,
					humanLang);
			// generate additional files
			STRANGEPairGenerator.generateAdditionalDir(resultPath);

			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsCosine(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, ArrayList<ArrayList<FeedbackToken>> rawTokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes, String dirPath, String resultPath,
			String progLang, String humanLang, int simThreshold, int minMatchLength, int maxPairs, String languageCode,
			ArrayList<ArrayList<String>> additionalKeywords) {
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

					if (simDegree >= simThreshold) {
						String dirname1 = assignments[j].getName();
						String dirname2 = assignments[k].getName();

						File code1 = CodeReader.getCode(assignments[j], progLang);
						File code2 = CodeReader.getCode(assignments[k], progLang);

						// to deal with non-code directories and files
						if (code1 == null || code2 == null)
							continue;

						// generate the matches
						ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndexes.get(j),
								tokenIndexes.get(k), minMatchLength);

						// add the comparison pair
						codePairs.add(new ComparisonPairTuple(j, k, dirname1, dirname2, simDegree, 1, matches));
					}
				}
			}

			// sort in descending order based on average syntax
			Collections.sort(codePairs);

			// remove extra pairs
			while (codePairs.size() > maxPairs) {
				codePairs.remove(codePairs.size() - 1);
			}

			// generate similarity reports
			for (int i = 0; i < codePairs.size(); i++) {
				ComparisonPairTuple ct = codePairs.get(i);
				String syntacticFilename = "obs" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], progLang);
				File code2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], progLang);

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				if (progLang.equals("java")) {
					JavaHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							rawTokenStrings.get(ct.getSubmissionID1()), rawTokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							additionalKeywords, ct.getMatches());
				} else if (progLang.equals("py")) {
					PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							rawTokenStrings.get(ct.getSubmissionID1()), rawTokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							additionalKeywords, ct.getMatches());
				}
			}

			// generate the index HTML
			IndexHTMLGenerator.generateHtml(dirPath, codePairs, MainFrame.indexTemplatePath, resultPath, simThreshold,
					humanLang);
			// generate additional files
			STRANGEPairGenerator.generateAdditionalDir(resultPath);

			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsMinHash(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, ArrayList<ArrayList<FeedbackToken>> rawTokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes, ArrayList<String> vectorHeader, String dirPath,
			String resultPath, String progLang, String humanLang, int simThreshold, int minMatchLength, int maxPairs,
			String languageCode, ArrayList<ArrayList<String>> additionalKeywords, int numClusters, int numStages) {
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
			int lshn = vectorHeader.size();
			LSHMinHash lsh = new LSHMinHash(numStages, numClusters, lshn);

			// store all the hashes (all stages)
			int[][] lshHashes = new int[tokenVectors.size()][];
			for (int i = 0; i < tokenVectors.size(); i++) {
				lshHashes[i] = lsh.hash(tokenVectors.get(i));
			}

			// to store the pairs and their occurrences
			HashMap<String, Integer> similarPairs = new HashMap<String, Integer>();

			// for each vector, check each stage and mark all programs fall to the same
			// bucket at least once
			for (int i = 0; i < tokenVectors.size(); i++) {
				// for each stage, mark all programs fall to the same bucket
				for (int j = 0; j < lshHashes[i].length; j++) {
					// search the counterpart of i
					for (int k = i + 1; k < tokenVectors.size(); k++) {
						if (lshHashes[i][j] == lshHashes[k][j]) {
							// if the hash is similar, put that on similar pairs

							// set the key
							String key = k + " " + i;
							if (i < k)
								key = i + " " + k;

							// get current value if any
							Integer val = similarPairs.get(key);
							if (val == null)
								val = 0;

							// update the new value
							similarPairs.put(key, val + 1);
						}
					}
				}
			}

			// for each similar pair, do the comparison
			Iterator<Entry<String, Integer>> itSimilarPairs = similarPairs.entrySet().iterator();
			while (itSimilarPairs.hasNext()) {
				Entry<String, Integer> en = itSimilarPairs.next();

				String[] cur = en.getKey().split(" ");

				// get the pair
				int submissionID1 = Integer.parseInt(cur[0]);
				int submissionID2 = Integer.parseInt(cur[1]);

				HashMap<String, ArrayList<Integer>> tokenIndex1 = tokenIndexes.get(submissionID1);
				HashMap<String, ArrayList<Integer>> tokenIndex2 = tokenIndexes.get(submissionID2);

				// generate the matches
				ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndex1, tokenIndex2,
						minMatchLength);

				// get the sim degree
				int simDegree = (int) (MatchGenerator.calcAverageSimilarity(matches,
						tokenStrings.get(submissionID1).size(), tokenStrings.get(submissionID2).size()) * 100);

				if (simDegree >= simThreshold) {
					String dirname1 = assignments[submissionID1].getName();
					String dirname2 = assignments[submissionID2].getName();

					File code1 = CodeReader.getCode(assignments[submissionID1], progLang);
					File code2 = CodeReader.getCode(assignments[submissionID2], progLang);

					// to deal with non-code directories and files
					if (code1 == null || code2 == null)
						continue;

					// add the comparison pair
					ComparisonPairTuple ct = new ComparisonPairTuple(submissionID1, submissionID2, dirname1, dirname2,
							simDegree, en.getValue(), matches);
					codePairs.add(ct);
				}
			}

			// sort in descending order based on average syntax
			Collections.sort(codePairs);

			// remove extra pairs
			while (codePairs.size() > maxPairs) {
				codePairs.remove(codePairs.size() - 1);
			}

			// generate similarity reports
			for (int i = 0; i < codePairs.size(); i++) {
				ComparisonPairTuple ct = codePairs.get(i);
				String syntacticFilename = "obs" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], progLang);
				File code2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], progLang);

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				if (progLang.equals("java")) {
					JavaHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							rawTokenStrings.get(ct.getSubmissionID1()), rawTokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							additionalKeywords, ct.getMatches());
				} else if (progLang.equals("py")) {
					PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							rawTokenStrings.get(ct.getSubmissionID1()), rawTokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							additionalKeywords, ct.getMatches());
				}
			}

			// generate the index HTML
			IndexHTMLGenerator.generateHtml(dirPath, codePairs, MainFrame.indexTemplatePath, resultPath, simThreshold,
					humanLang);
			// generate additional files
			STRANGEPairGenerator.generateAdditionalDir(resultPath);

			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsSuoerBit(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, ArrayList<ArrayList<FeedbackToken>> rawTokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes, ArrayList<String> vectorHeader, String dirPath,
			String resultPath, String progLang, String humanLang, int simThreshold, int minMatchLength, int maxPairs,
			String languageCode, ArrayList<ArrayList<String>> additionalKeywords, int numClusters, int numStages) {
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
			int lshn = vectorHeader.size();
			LSHSuperBit lsh = new LSHSuperBit(numStages, numClusters, lshn);

			// store all the hashes (all stages)
			int[][] lshHashes = new int[tokenVectors.size()][];
			for (int i = 0; i < tokenVectors.size(); i++) {
				lshHashes[i] = lsh.hash(tokenVectors.get(i));
			}

			// to store the pairs and their occurrences
			HashMap<String, Integer> similarPairs = new HashMap<String, Integer>();

			// for each vector, check each stage and mark all programs fall to the same
			// bucket at least once
			for (int i = 0; i < tokenVectors.size(); i++) {
				// for each stage, mark all programs fall to the same bucket
				for (int j = 0; j < lshHashes[i].length; j++) {
					// search the counterpart of i
					for (int k = i + 1; k < tokenVectors.size(); k++) {
						if (lshHashes[i][j] == lshHashes[k][j]) {
							// if the hash is similar, put that on similar pairs

							// set the key
							String key = k + " " + i;
							if (i < k)
								key = i + " " + k;

							// get current value if any
							Integer val = similarPairs.get(key);
							if (val == null)
								val = 0;

							// update the new value
							similarPairs.put(key, val + 1);
						}
					}
				}
			}

			// for each similar pair, do the comparison
			Iterator<Entry<String, Integer>> itSimilarPairs = similarPairs.entrySet().iterator();
			while (itSimilarPairs.hasNext()) {
				Entry<String, Integer> en = itSimilarPairs.next();

				String[] cur = en.getKey().split(" ");

				// get the pair
				int submissionID1 = Integer.parseInt(cur[0]);
				int submissionID2 = Integer.parseInt(cur[1]);

				HashMap<String, ArrayList<Integer>> tokenIndex1 = tokenIndexes.get(submissionID1);
				HashMap<String, ArrayList<Integer>> tokenIndex2 = tokenIndexes.get(submissionID2);

				// generate the matches
				ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndex1, tokenIndex2,
						minMatchLength);

				// get the sim degree
				int simDegree = (int) (MatchGenerator.calcAverageSimilarity(matches,
						tokenStrings.get(submissionID1).size(), tokenStrings.get(submissionID2).size()) * 100);

				if (simDegree >= simThreshold) {
					String dirname1 = assignments[submissionID1].getName();
					String dirname2 = assignments[submissionID2].getName();

					File code1 = CodeReader.getCode(assignments[submissionID1], progLang);
					File code2 = CodeReader.getCode(assignments[submissionID2], progLang);

					// to deal with non-code directories and files
					if (code1 == null || code2 == null)
						continue;

					// add the comparison pair
					ComparisonPairTuple ct = new ComparisonPairTuple(submissionID1, submissionID2, dirname1, dirname2,
							simDegree, en.getValue(), matches);
					codePairs.add(ct);
				}
			}

			// sort in descending order based on average syntax
			Collections.sort(codePairs);

			// remove extra pairs
			while (codePairs.size() > maxPairs) {
				codePairs.remove(codePairs.size() - 1);
			}

			// generate similarity reports
			for (int i = 0; i < codePairs.size(); i++) {
				ComparisonPairTuple ct = codePairs.get(i);
				String syntacticFilename = "obs" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], progLang);
				File code2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], progLang);

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				if (progLang.equals("java")) {
					JavaHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							rawTokenStrings.get(ct.getSubmissionID1()), rawTokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							additionalKeywords, ct.getMatches());
				} else if (progLang.equals("py")) {
					PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							rawTokenStrings.get(ct.getSubmissionID1()), rawTokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							additionalKeywords, ct.getMatches());
				}
			}

			// generate the index HTML
			IndexHTMLGenerator.generateHtml(dirPath, codePairs, MainFrame.indexTemplatePath, resultPath, simThreshold,
					humanLang);
			// generate additional files
			STRANGEPairGenerator.generateAdditionalDir(resultPath);

			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}

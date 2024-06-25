package sstrange;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;

import info.debatty.java.lsh.LSHMinHash;
import info.debatty.java.lsh.LSHSuperBit;
import sstrange.anomaly.AICodeManipulator;
import sstrange.anomaly.AnomalyTuple;
import sstrange.htmlgenerator.CodeReader;
import sstrange.htmlgenerator.HtmlGenerator;
import sstrange.htmlgenerator.UniqueCodeHtmlGenerator;
import sstrange.language.csharp.CSharpFeedbackGenerator;
import sstrange.language.dart.DartFeedbackGenerator;
import sstrange.language.java.JavaFeedbackGenerator;
import sstrange.language.python.PythonFeedbackGenerator;
import sstrange.language.python.PythonHtmlGenerator;
import sstrange.lshcalculator.CosineCalculator;
import sstrange.lshcalculator.IndexGenerator;
import sstrange.lshcalculator.JaccardCalculator;
import sstrange.matchgenerator.ComparisonPairTuple;
import sstrange.matchgenerator.MatchGenerator;
import sstrange.message.FeedbackMessageGenerator;
import sstrange.token.FeedbackToken;
import support.stringmatching.GSTMatchTuple;

public class FastComparer {

	public static ArrayList<ComparisonPairTuple> doSyntacticComparison(String assignmentPath, String progLang,
			String humanLang, int simThreshold, int dissimThreshold, int minMatchingLength, int maxPairs,
			String templateDirPath, String similarityMeasurement, File assignmentFile, String assignmentParentDirPath,
			String assignmentName, boolean isMultipleFiles, boolean isCommonCodeAllowed, String aiSubPath,
			ArrayList<File> filesToBeDeleted, int numClusters, int numStages, boolean isSensitive) {

		// add AI submission as one of the submissions if exist
		String aiSubName = "";
		if (aiSubPath.length() > 0 && aiSubPath.equals("none") == false)
			aiSubName = AICodeManipulator.moveAISubToMainAssignmentFolder(aiSubPath, assignmentPath, filesToBeDeleted);

		// if multiple files, merge them
		if (isMultipleFiles) {
			String newAssignmentPath = assignmentParentDirPath + File.separator + "[merged] " + assignmentName;
			CodeMerger.mergeCode(assignmentPath, progLang, newAssignmentPath);
			assignmentPath = newAssignmentPath;
			// mark new assignment path to be deleted after the whole process
			filesToBeDeleted.add(new File(newAssignmentPath));
		}

		if (progLang.equalsIgnoreCase("java") || progLang.equalsIgnoreCase("py")) {
			/*
			 * For now, these only work on Java and Python
			 */

			// remove common and template code for java and python if needed
			assignmentPath = JavaPyCommonTemplateRemover.removeCommonAndTemplateCodeJavaPython(assignmentPath,
					minMatchingLength, templateDirPath, isCommonCodeAllowed, progLang, assignmentParentDirPath,
					assignmentName, filesToBeDeleted);
		}

		// set result dir based on the name of assignment
		String resultPath = assignmentParentDirPath + File.separator + "[out] " + assignmentName;

		// generate STRANGE reports
		return compareAndGenerateHTMLReports(assignmentPath, resultPath, progLang, humanLang, simThreshold,
				dissimThreshold, minMatchingLength, maxPairs, humanLang, similarityMeasurement, aiSubName, numClusters,
				numStages, isSensitive);

	}

	private static ArrayList<ComparisonPairTuple> compareAndGenerateHTMLReports(String dirPath, String resultPath,
			String progLang, String humanLang, int simThreshold, int dissimThreshold, int minMatchLength, int maxPairs,
			String languageCode, String similarityMeasurement, String aiSubName, int numClusters, int numStages,
			boolean isSensitive) {
		// create the output dir
		File resultDir = new File(resultPath);
		resultDir.mkdir();

		File[] assignments = (new File(dirPath)).listFiles();

		// tokenise all programs and generate the indexes
		ArrayList<ArrayList<FeedbackToken>> tokenStrings = new ArrayList<ArrayList<FeedbackToken>>();
		ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();

		// storing surface indexes
		ArrayList<HashMap<String, ArrayList<Integer>>> surfaceTokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();

		for (int i = 0; i < assignments.length; i++) {
			// for each code, tokenise and generalise
			File code = CodeReader.getCode(assignments[i], progLang);

			if (code == null) {
				tokenStrings.add(new ArrayList<FeedbackToken>());
				// if not RKGST, generate empty index
				if (similarityMeasurement.equalsIgnoreCase("RKRGST") == false) {
					tokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
					if (isSensitive)
						// if sensitive, add sensitive index
						surfaceTokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
				}
			} else {
				// generate token string
				ArrayList<FeedbackToken> tokenString = null;

				if (progLang.equals("java")) {
					tokenString = JavaFeedbackGenerator.generateFeedbackTokenString(code.getAbsolutePath());
					tokenString = JavaFeedbackGenerator.syntaxTokenStringPreprocessing(tokenString);
				} else if (progLang.equals("py")) {
					tokenString = PythonFeedbackGenerator.generateSyntaxTokenString(code.getAbsolutePath());
					PythonFeedbackGenerator.syntaxTokenStringPreprocessing(tokenString);
				} else if (progLang.equals("dart")) {
					tokenString = DartFeedbackGenerator.generateFeedbackTokenString(code.getAbsolutePath());
					tokenString = DartFeedbackGenerator.syntaxTokenStringPreprocessing(tokenString);
				} else if (progLang.equals("cs")) {
					tokenString = CSharpFeedbackGenerator.generateFeedbackTokenString(code.getAbsolutePath());
					tokenString = CSharpFeedbackGenerator.syntaxTokenStringPreprocessing(tokenString);
				}

				// add the token string
				tokenStrings.add(tokenString);

				// generate the indexes if not RKRGST
				if (similarityMeasurement.equalsIgnoreCase("RKRGST") == false) {
					HashMap<String, ArrayList<Integer>> tokenIndex = IndexGenerator.generateIndex(tokenString,
							minMatchLength, false);
					// add the token index
					tokenIndexes.add(tokenIndex);

					// if sensitive mode is applied, calculate the surface index, and add it
					if (isSensitive) {
						HashMap<String, ArrayList<Integer>> surfaceTokenIndex = IndexGenerator
								.generateIndex(tokenString, minMatchLength, true);
						surfaceTokenIndexes.add(surfaceTokenIndex);
					}
				}
			}
		}

		if (similarityMeasurement.equalsIgnoreCase("MinHash")) {
			// generate vector header
			ArrayList<String> vectorHeader = IndexGenerator.generateVectorHeader(tokenIndexes);

			// calculate minhash
			return _compareAndGenerateHTMLReportsMinHash(assignments, tokenStrings, tokenIndexes, surfaceTokenIndexes,
					vectorHeader, dirPath, resultPath, progLang, humanLang, simThreshold, dissimThreshold, aiSubName,
					minMatchLength, maxPairs, languageCode, numClusters, numStages, isSensitive);
		} else if (similarityMeasurement.equalsIgnoreCase("Super-Bit")) {
			// generate vector header
			ArrayList<String> vectorHeader = IndexGenerator.generateVectorHeader(tokenIndexes);

			// calculate super-bit
			return _compareAndGenerateHTMLReportsSuoerBit(assignments, tokenStrings, tokenIndexes, surfaceTokenIndexes,
					vectorHeader, dirPath, resultPath, progLang, humanLang, simThreshold, dissimThreshold, aiSubName,
					minMatchLength, maxPairs, languageCode, numClusters, numStages, isSensitive);
		} else if (similarityMeasurement.equalsIgnoreCase("Jaccard")) {
			return _compareAndGenerateHTMLReportsJaccard(assignments, tokenStrings, tokenIndexes, surfaceTokenIndexes,
					dirPath, resultPath, progLang, humanLang, simThreshold, dissimThreshold, aiSubName, minMatchLength,
					maxPairs, languageCode, isSensitive);
		} else if (similarityMeasurement.equalsIgnoreCase("Cosine")) {
			return _compareAndGenerateHTMLReportsCosine(assignments, tokenStrings, tokenIndexes, surfaceTokenIndexes,
					dirPath, resultPath, progLang, humanLang, simThreshold, dissimThreshold, aiSubName, minMatchLength,
					maxPairs, languageCode, isSensitive);
		} else {
			// RKRGST
			return _compareAndGenerateHTMLReportsRKRGST(assignments, tokenStrings, dirPath, resultPath, progLang,
					humanLang, simThreshold, dissimThreshold, aiSubName, minMatchLength, maxPairs, languageCode,
					isSensitive);
		}

	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsRKRGST(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, String dirPath, String resultPath, String progLang,
			String humanLang, int simThreshold, int dissimThreshold, String aiSubName, int minMatchLength, int maxPairs,
			String languageCode, boolean isSensitive) {
		// RKRGST

		// to calculate dissimilarity and anomaly
		ArrayList<AnomalyTuple> anomalies = new ArrayList<>();
		int[] simPerSubmission = new int[tokenStrings.size()];
		int[] surSimPerSubmission = new int[tokenStrings.size()];

		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();
			ArrayList<ComparisonPairTuple> aiCodePairs = new ArrayList<>();

			// do the comparison
			for (int j = 0; j < tokenStrings.size(); j++) {
				for (int k = j + 1; k < tokenStrings.size(); k++) {

					String dirname1 = assignments[j].getName();
					String dirname2 = assignments[k].getName();

					// get matched tiles with RKRGST
					ArrayList<GSTMatchTuple> simTuples = FeedbackMessageGenerator
							.generateMatchedTuples(tokenStrings.get(j), tokenStrings.get(k), minMatchLength, false);

					// get the sim degree for RKRGST
					int simDegree = (int) (MatchGenerator.calcAverageSimilarity(simTuples, tokenStrings.get(j).size(),
							tokenStrings.get(k).size()) * 100);

					// add the sim degrees except for AI sample
					if (!(dirname1.equals(aiSubName) || dirname2.equals(aiSubName))) {
						simPerSubmission[j] += simDegree;
						simPerSubmission[k] += simDegree;
					}

					// calculate surface sim if needed
					int surfaceSimDegree = -1;
					if (isSensitive) {
						ArrayList<GSTMatchTuple> surfaceSimTuples = FeedbackMessageGenerator
								.generateMatchedTuples(tokenStrings.get(j), tokenStrings.get(k), minMatchLength, true);

						surfaceSimDegree = (int) (MatchGenerator.calcAverageSimilarity(surfaceSimTuples,
								tokenStrings.get(j).size(), tokenStrings.get(k).size()) * 100);
						// add the surface sim degree except for AI sample
						if (!(dirname1.equals(aiSubName) || dirname2.equals(aiSubName))) {
							surSimPerSubmission[j] += surfaceSimDegree;
							surSimPerSubmission[k] += surfaceSimDegree;
						}
					}

					int overallSimDegree = simDegree;
					if (isSensitive)
						overallSimDegree = (simDegree + surfaceSimDegree) / 2;

					if (overallSimDegree >= simThreshold) {

						File code1 = CodeReader.getCode(assignments[j], progLang);
						File code2 = CodeReader.getCode(assignments[k], progLang);

						// to deal with non-code directories and files
						if (code1 == null || code2 == null)
							continue;

						if (dirname1.equals(aiSubName) || dirname2.equals(aiSubName)) {
							// if one of them is AI sample, direct the result to anomaly list
							/*
							 * if submission j is AI sample, the anomaly submission is k. Otherwise the
							 * anomaly is j.
							 */
							int anomalySubIdx = k;
							dirname1 = "AI sample";
							if (dirname2.equals(aiSubName)) {
								anomalySubIdx = j;
								dirname2 = "AI sample";
							}
							anomalies.add(new AnomalyTuple(anomalySubIdx, assignments[anomalySubIdx].getName(), -1,
									overallSimDegree));

							// add the comparison pair for AI
							aiCodePairs.add(new ComparisonPairTuple(j, k, dirname1, dirname2, simDegree,
									surfaceSimDegree, 1, simTuples));

						} else {
							// add the comparison pair if no AI sample submission
							codePairs.add(new ComparisonPairTuple(j, k, dirname1, dirname2, simDegree, surfaceSimDegree,
									1, simTuples));
						}
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
				ct.setResultedHTMLFilename(syntacticFilename);

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], progLang);
				File code2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], progLang);

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				if (progLang.equals("py")) {
					PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							ct.getMatches());
				} else {
					HtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							ct.getMatches());
				}
			}

			// create a list of anomalies (overly unique submissions)
			for (int j = 0; j < simPerSubmission.length; j++) {
				// exclude AI code sample
				if (assignments[j].getName().equals(aiSubName))
					continue;

				int overallDissim = 100 - (simPerSubmission[j] / simPerSubmission.length);
				if (isSensitive) {
					overallDissim = 100
							- ((simPerSubmission[j] + surSimPerSubmission[j]) / (2 * simPerSubmission.length));
				}
				if (overallDissim >= dissimThreshold) {
					/*
					 * if there are anomaly tuples resulted from comparison with AI, just update the
					 * syntax dissim
					 */
					boolean isFound = false;
					for (int i = 0; i < anomalies.size(); i++) {
						AnomalyTuple a = anomalies.get(i);
						if (a.getSubmissionID() == j) {
							isFound = true;
							a.setSyntaxDissim(overallDissim);
						}
					}

					if (isFound == false)
						anomalies.add(new AnomalyTuple(j, assignments[j].getName(), overallDissim, -1));
				}
			}

			// sort
			Collections.sort(anomalies);
			// remove extra submissions
			while (anomalies.size() > maxPairs) {
				anomalies.remove(anomalies.size() - 1);
			}

			// generate anomaly reports
			for (int i = 0; i < anomalies.size(); i++) {
				AnomalyTuple ct = anomalies.get(i);

				String syntacticFilename = "uni" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID()], progLang);

				UniqueCodeHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), ct.getAssignmentName(),
						MainFrame.dissimTemplatePath, syntacticFilepath, humanLang, ct.getDissimResult());

				if (ct.getAiSim() != -1) {
					// generate similarity report comparing with AI
					syntacticFilename = "oai" + i + ".html";
					syntacticFilepath = resultPath + File.separator + syntacticFilename;

					// search the code pair calculated before
					ComparisonPairTuple selected = null;
					for (int j = 0; j < aiCodePairs.size(); j++) {
						ComparisonPairTuple ct2 = aiCodePairs.get(j);
						if (ct2.getSubmissionID1() == ct.getSubmissionID()
								|| ct2.getSubmissionID2() == ct.getSubmissionID()) {
							selected = ct2;
						}
					}

					if (selected != null) {
						// generate the ai comparison
						selected.setResultedHTMLFilename(syntacticFilename);

						code1 = CodeReader.getCode(assignments[selected.getSubmissionID1()], progLang);
						File code2 = CodeReader.getCode(assignments[selected.getSubmissionID2()], progLang);

						// set the path to comparison pair tuple
						ct.setResultedAIHTMLFilename(syntacticFilename);

						if (progLang.equals("py")) {
							PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(),
									code2.getAbsolutePath(), tokenStrings.get(selected.getSubmissionID1()),
									tokenStrings.get(selected.getSubmissionID2()), selected.getAssignmentName1(),
									selected.getAssignmentName2(), MainFrame.pairTemplatePath, syntacticFilepath,
									minMatchLength, selected.getSameClusterOccurrences(), humanLang,
									selected.getMatches());
						} else {
							HtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
									tokenStrings.get(selected.getSubmissionID1()),
									tokenStrings.get(selected.getSubmissionID2()), selected.getAssignmentName1(),
									selected.getAssignmentName2(), MainFrame.pairTemplatePath, syntacticFilepath,
									minMatchLength, selected.getSameClusterOccurrences(), humanLang,
									selected.getMatches());
						}
					}
				}
			}

			// generate the index HTML
			IndexHTMLGenerator.generateHtml(dirPath, codePairs, anomalies, MainFrame.indexTemplatePath, resultPath,
					simThreshold, dissimThreshold, humanLang);
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
			ArrayList<ArrayList<FeedbackToken>> tokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceTokenIndexes, String dirPath, String resultPath,
			String progLang, String humanLang, int simThreshold, int dissimThreshold, String aiSubName,
			int minMatchLength, int maxPairs, String languageCode, boolean isSensitive) {
		// Jaccard

		// to calculate dissimilarity and anomaly
		ArrayList<AnomalyTuple> anomalies = new ArrayList<>();
		int[] simPerSubmission = new int[tokenStrings.size()];
		int[] surSimPerSubmission = new int[tokenStrings.size()];

		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();
			ArrayList<ComparisonPairTuple> aiCodePairs = new ArrayList<>();

			// do the comparison
			for (int j = 0; j < tokenIndexes.size(); j++) {
				for (int k = j + 1; k < tokenStrings.size(); k++) {
					// get matched tiles with RKRGST

					String dirname1 = assignments[j].getName();
					String dirname2 = assignments[k].getName();

					// get the sim degree for jaccard
					int simDegree = (int) (JaccardCalculator.calculateJaccardSimilarity(tokenIndexes.get(j),
							tokenIndexes.get(k)) * 100);

					// add the sim degrees except for AI sample
					if (!(dirname1.equals(aiSubName) || dirname2.equals(aiSubName))) {
						simPerSubmission[j] += simDegree;
						simPerSubmission[k] += simDegree;
					}

					// calculate surface sim
					double surfaceSimDegree = -1;
					if (surfaceTokenIndexes.size() > 0) {
						surfaceSimDegree = (int) (JaccardCalculator.calculateJaccardSimilarity(
								surfaceTokenIndexes.get(j), surfaceTokenIndexes.get(k)) * 100);

						// add the sim degrees except for AI sample
						if (!(dirname1.equals(aiSubName) || dirname2.equals(aiSubName))) {
							surSimPerSubmission[j] += surfaceSimDegree;
							surSimPerSubmission[k] += surfaceSimDegree;
						}
					}

					int overallSimDegree = simDegree;
					if (isSensitive)
						overallSimDegree = (int) ((simDegree + surfaceSimDegree) / 2);

					if (overallSimDegree >= simThreshold) {

						File code1 = CodeReader.getCode(assignments[j], progLang);
						File code2 = CodeReader.getCode(assignments[k], progLang);

						// to deal with non-code directories and files
						if (code1 == null || code2 == null)
							continue;

						// generate the matches
						ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndexes.get(j),
								tokenIndexes.get(k), minMatchLength);

						if (dirname1.equals(aiSubName) || dirname2.equals(aiSubName)) {
							// if one of them is AI sample, direct the result to anomaly list
							/*
							 * if submission j is AI sample, the anomaly submission is k. Otherwise the
							 * anomaly is j.
							 */
							int anomalySubIdx = k;
							if (dirname2.equals(aiSubName))
								anomalySubIdx = j;
							anomalies.add(new AnomalyTuple(anomalySubIdx, assignments[anomalySubIdx].getName(), -1,
									overallSimDegree));

							// add the comparison pair for AI
							aiCodePairs.add(new ComparisonPairTuple(j, k, dirname1, dirname2, simDegree,
									surfaceSimDegree, 1, matches));

						} else {
							// add the comparison pair
							codePairs.add(new ComparisonPairTuple(j, k, dirname1, dirname2, simDegree, surfaceSimDegree,
									1, matches));
						}
					}
				}
			}

			// sort in descending order based on average syntax
			Collections.sort(codePairs);

			// remove extra pairs
			while (codePairs.size() > maxPairs) {
				codePairs.remove(codePairs.size() - 1);
			}

			for (int j = 0; j < simPerSubmission.length; j++) {
				// exclude AI code sample
				if (assignments[j].getName().equals(aiSubName))
					continue;

				int overallDissim = 100 - (simPerSubmission[j] / simPerSubmission.length);
				if (isSensitive)
					overallDissim = 100
							- ((simPerSubmission[j] + surSimPerSubmission[j]) / (2 * simPerSubmission.length));
				if (overallDissim >= dissimThreshold) {
					/*
					 * if there are anomaly tuples resulted from comparison with AI, just update the
					 * syntax dissim
					 */
					boolean isFound = false;
					for (int i = 0; i < anomalies.size(); i++) {
						AnomalyTuple a = anomalies.get(i);
						if (a.getSubmissionID() == j) {
							isFound = true;
							a.setSyntaxDissim(overallDissim);
						}
					}

					if (isFound == false)
						anomalies.add(new AnomalyTuple(j, assignments[j].getName(), overallDissim, -1));
				}
			}

			// sort
			Collections.sort(anomalies);
			// remove extra submissions
			while (anomalies.size() > maxPairs) {
				anomalies.remove(anomalies.size() - 1);
			}

			// generate anomaly reports
			for (int i = 0; i < anomalies.size(); i++) {
				AnomalyTuple ct = anomalies.get(i);
				String syntacticFilename = "uni" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;
				ct.setResultedHTMLFilename(syntacticFilename);

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID()], progLang);

				UniqueCodeHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), ct.getAssignmentName(),
						MainFrame.dissimTemplatePath, syntacticFilepath, humanLang, ct.getDissimResult());

				if (ct.getAiSim() != -1) {
					// generate similarity report comparing with AI
					syntacticFilename = "oai" + i + ".html";
					syntacticFilepath = resultPath + File.separator + syntacticFilename;

					// search the code pair calculated before
					ComparisonPairTuple selected = null;
					for (int j = 0; j < aiCodePairs.size(); j++) {
						ComparisonPairTuple ct2 = aiCodePairs.get(j);
						if (ct2.getSubmissionID1() == ct.getSubmissionID()
								|| ct2.getSubmissionID2() == ct.getSubmissionID()) {
							selected = ct2;
						}
					}

					if (selected != null) {
						// generate the ai comparison
						selected.setResultedHTMLFilename(syntacticFilename);

						code1 = CodeReader.getCode(assignments[selected.getSubmissionID1()], progLang);
						File code2 = CodeReader.getCode(assignments[selected.getSubmissionID2()], progLang);

						// set the path to comparison pair tuple
						ct.setResultedAIHTMLFilename(syntacticFilename);

						if (progLang.equals("py")) {
							PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(),
									code2.getAbsolutePath(), tokenStrings.get(selected.getSubmissionID1()),
									tokenStrings.get(selected.getSubmissionID2()), selected.getAssignmentName1(),
									selected.getAssignmentName2(), MainFrame.pairTemplatePath, syntacticFilepath,
									minMatchLength, selected.getSameClusterOccurrences(), humanLang,
									selected.getMatches());
						} else {
							HtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
									tokenStrings.get(selected.getSubmissionID1()),
									tokenStrings.get(selected.getSubmissionID2()), selected.getAssignmentName1(),
									selected.getAssignmentName2(), MainFrame.pairTemplatePath, syntacticFilepath,
									minMatchLength, selected.getSameClusterOccurrences(), humanLang,
									selected.getMatches());
						}
					}
				}

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

				if (progLang.equals("py")) {
					PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							ct.getMatches());
				} else {
					HtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							ct.getMatches());
				}
			}

			// generate the index HTML
			IndexHTMLGenerator.generateHtml(dirPath, codePairs, anomalies, MainFrame.indexTemplatePath, resultPath,
					simThreshold, dissimThreshold, humanLang);
			// generate additional files
			STRANGEPairGenerator.generateAdditionalDir(resultPath);

			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsCosine(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceTokenIndexes, String dirPath, String resultPath,
			String progLang, String humanLang, int simThreshold, int dissimThreshold, String aiSubName,
			int minMatchLength, int maxPairs, String languageCode, boolean isSensitive) {
		// Cosine

		// to calculate dissimilarity and anomaly
		ArrayList<AnomalyTuple> anomalies = new ArrayList<>();
		int[] simPerSubmission = new int[tokenStrings.size()];
		int[] surSimPerSubmission = new int[tokenStrings.size()];

		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();
			ArrayList<ComparisonPairTuple> aiCodePairs = new ArrayList<>();

			// do the comparison
			for (int j = 0; j < tokenIndexes.size(); j++) {

				for (int k = j + 1; k < tokenStrings.size(); k++) {
					// get matched tiles with Cosine
					String dirname1 = assignments[j].getName();
					String dirname2 = assignments[k].getName();

					// get the sim degree for Cosine
					int simDegree = (int) (CosineCalculator.calculateCosineSimilarity(tokenIndexes.get(j),
							tokenIndexes.get(k)) * 100);

					// add the sim degrees except for AI sample
					if (!(dirname1.equals(aiSubName) || dirname2.equals(aiSubName))) {
						simPerSubmission[j] += simDegree;
						simPerSubmission[k] += simDegree;
					}

					// if the surface index is not empty, then calculate surface sim
					double surfaceSimDegree = -1;
					if (surfaceTokenIndexes.size() > 0) {
						surfaceSimDegree = (int) (CosineCalculator.calculateCosineSimilarity(surfaceTokenIndexes.get(j),
								surfaceTokenIndexes.get(k)) * 100);

						// add the sim degrees except for AI sample
						if (!(dirname1.equals(aiSubName) || dirname2.equals(aiSubName))) {
							surSimPerSubmission[j] += surfaceSimDegree;
							surSimPerSubmission[k] += surfaceSimDegree;
						}
					}

					int overallSimDegree = simDegree;
					if (isSensitive)
						overallSimDegree = (int) ((simDegree + surfaceSimDegree) / 2);

					if (overallSimDegree >= simThreshold) {
						File code1 = CodeReader.getCode(assignments[j], progLang);
						File code2 = CodeReader.getCode(assignments[k], progLang);

						// to deal with non-code directories and files
						if (code1 == null || code2 == null)
							continue;

						// generate the matches
						ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndexes.get(j),
								tokenIndexes.get(k), minMatchLength);
						if (dirname1.equals(aiSubName) || dirname2.equals(aiSubName)) {
							// if one of them is AI sample, direct the result to anomaly list
							/*
							 * if submission j is AI sample, the anomaly submission is k. Otherwise the
							 * anomaly is j.
							 */
							int anomalySubIdx = k;
							if (dirname2.equals(aiSubName))
								anomalySubIdx = j;
							anomalies.add(new AnomalyTuple(anomalySubIdx, assignments[anomalySubIdx].getName(), -1,
									overallSimDegree));
							// add the comparison pair for AI
							aiCodePairs.add(new ComparisonPairTuple(j, k, dirname1, dirname2, simDegree,
									surfaceSimDegree, 1, matches));
						} else {
							// add the comparison pair
							codePairs.add(new ComparisonPairTuple(j, k, dirname1, dirname2, simDegree, surfaceSimDegree,
									1, matches));
						}

					}
				}
			}

			// sort in descending order based on average syntax
			Collections.sort(codePairs);

			// remove extra pairs
			while (codePairs.size() > maxPairs) {
				codePairs.remove(codePairs.size() - 1);
			}

			for (int j = 0; j < simPerSubmission.length; j++) {
				// exclude AI code sample
				if (assignments[j].getName().equals(aiSubName))
					continue;

				int overallDissim = 100 - (simPerSubmission[j] / simPerSubmission.length);
				if (isSensitive)
					overallDissim = 100
							- ((simPerSubmission[j] + surSimPerSubmission[j]) / (2 * simPerSubmission.length));
				if (overallDissim >= dissimThreshold) {
					/*
					 * if there are anomaly tuples resulted from comparison with AI, just update the
					 * syntax dissim
					 */
					boolean isFound = false;
					for (int i = 0; i < anomalies.size(); i++) {
						AnomalyTuple a = anomalies.get(i);
						if (a.getSubmissionID() == j) {
							isFound = true;
							a.setSyntaxDissim(overallDissim);
						}
					}

					if (isFound == false)
						anomalies.add(new AnomalyTuple(j, assignments[j].getName(), overallDissim, -1));
				}
			}

			// sort
			Collections.sort(anomalies);
			// remove extra submissions
			while (anomalies.size() > maxPairs) {
				anomalies.remove(anomalies.size() - 1);
			}

			// generate anomaly reports
			for (int i = 0; i < anomalies.size(); i++) {
				AnomalyTuple ct = anomalies.get(i);
				String syntacticFilename = "uni" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;
				ct.setResultedHTMLFilename(syntacticFilename);

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID()], progLang);

				UniqueCodeHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), ct.getAssignmentName(),
						MainFrame.dissimTemplatePath, syntacticFilepath, humanLang, ct.getDissimResult());

				if (ct.getAiSim() != -1) {
					// generate similarity report comparing with AI
					syntacticFilename = "oai" + i + ".html";
					syntacticFilepath = resultPath + File.separator + syntacticFilename;

					// search the code pair calculated before
					ComparisonPairTuple selected = null;
					for (int j = 0; j < aiCodePairs.size(); j++) {
						ComparisonPairTuple ct2 = aiCodePairs.get(j);
						if (ct2.getSubmissionID1() == ct.getSubmissionID()
								|| ct2.getSubmissionID2() == ct.getSubmissionID()) {
							selected = ct2;
						}
					}

					if (selected != null) {
						// generate the ai comparison
						selected.setResultedHTMLFilename(syntacticFilename);

						code1 = CodeReader.getCode(assignments[selected.getSubmissionID1()], progLang);
						File code2 = CodeReader.getCode(assignments[selected.getSubmissionID2()], progLang);

						// set the path to comparison pair tuple
						ct.setResultedAIHTMLFilename(syntacticFilename);

						if (progLang.equals("py")) {
							PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(),
									code2.getAbsolutePath(), tokenStrings.get(selected.getSubmissionID1()),
									tokenStrings.get(selected.getSubmissionID2()), selected.getAssignmentName1(),
									selected.getAssignmentName2(), MainFrame.pairTemplatePath, syntacticFilepath,
									minMatchLength, selected.getSameClusterOccurrences(), humanLang,
									selected.getMatches());
						} else {
							HtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
									tokenStrings.get(selected.getSubmissionID1()),
									tokenStrings.get(selected.getSubmissionID2()), selected.getAssignmentName1(),
									selected.getAssignmentName2(), MainFrame.pairTemplatePath, syntacticFilepath,
									minMatchLength, selected.getSameClusterOccurrences(), humanLang,
									selected.getMatches());
						}
					}
				}
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

				if (progLang.equals("py")) {
					PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							ct.getMatches());
				} else {
					HtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
							tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
							ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
							syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
							ct.getMatches());
				}
			}

			// generate the index HTML
			IndexHTMLGenerator.generateHtml(dirPath, codePairs, anomalies, MainFrame.indexTemplatePath, resultPath,
					simThreshold, dissimThreshold, humanLang);
			// generate additional files
			STRANGEPairGenerator.generateAdditionalDir(resultPath);

			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsMinHash(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceTokenIndexes, ArrayList<String> vectorHeader,
			String dirPath, String resultPath, String progLang, String humanLang, int simThreshold, int dissimThreshold,
			String aiSubName, int minMatchLength, int maxPairs, String languageCode, int numClusters, int numStages,
			boolean isSensitive) {
		// MinHash algorithm

		// to calculate dissimilarity and anomaly
		ArrayList<AnomalyTuple> anomalies = new ArrayList<>();
		int[] simPerSubmission = new int[tokenStrings.size()];
		int[] surSimPerSubmission = new int[tokenStrings.size()];

		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();
			ArrayList<ComparisonPairTuple> aiCodePairs = new ArrayList<>();

			// generate Jaccard vectors
			ArrayList<boolean[]> tokenVectors = new ArrayList<boolean[]>();
			for (int i = 0; i < tokenIndexes.size(); i++) {
				tokenVectors
						.add(JaccardCalculator.generateBooleanVectorFromTokenString(tokenIndexes.get(i), vectorHeader));
			}

			// create MinHash object with best default setting
			int lshn = vectorHeader.size();
			if (lshn > 0) {
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

					String dirname1 = assignments[submissionID1].getName();
					String dirname2 = assignments[submissionID2].getName();

					HashMap<String, ArrayList<Integer>> tokenIndex1 = tokenIndexes.get(submissionID1);
					HashMap<String, ArrayList<Integer>> tokenIndex2 = tokenIndexes.get(submissionID2);

					// generate the matches
					ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndex1, tokenIndex2,
							minMatchLength);

					// get the sim degree
					int simDegree = (int) (MatchGenerator.calcAverageSimilarity(matches,
							tokenStrings.get(submissionID1).size(), tokenStrings.get(submissionID2).size()) * 100);

					// add the sim degrees except for AI sample
					if (!(dirname1.equals(aiSubName) || dirname2.equals(aiSubName))) {
						simPerSubmission[submissionID1] += simDegree;
						simPerSubmission[submissionID2] += simDegree;
					}

					// if the surface index is not empty, then calculate surface sim
					double surfaceSimDegree = -1;
					if (surfaceTokenIndexes.size() > 0) {
						HashMap<String, ArrayList<Integer>> surfaceTokenIndex1 = surfaceTokenIndexes.get(submissionID1);
						HashMap<String, ArrayList<Integer>> surfaceTokenIndex2 = surfaceTokenIndexes.get(submissionID2);

						ArrayList<GSTMatchTuple> surfaceMatches = MatchGenerator.generateMatches(surfaceTokenIndex1,
								surfaceTokenIndex2, minMatchLength);

						surfaceSimDegree = (int) (MatchGenerator.calcAverageSimilarity(surfaceMatches,
								tokenStrings.get(submissionID1).size(), tokenStrings.get(submissionID2).size()) * 100);

						// add the sim degrees except for AI sample
						if (!(dirname1.equals(aiSubName) || dirname2.equals(aiSubName))) {
							surSimPerSubmission[submissionID1] += surfaceSimDegree;
							surSimPerSubmission[submissionID2] += surfaceSimDegree;
						}
					}

					int overallSimDegree = simDegree;
					if (isSensitive)
						overallSimDegree = (int) ((simDegree + surfaceSimDegree) / 2);

					if (overallSimDegree >= simThreshold) {

						File code1 = CodeReader.getCode(assignments[submissionID1], progLang);
						File code2 = CodeReader.getCode(assignments[submissionID2], progLang);

						// to deal with non-code directories and files
						if (code1 == null || code2 == null)
							continue;

						if (dirname1.equals(aiSubName) || dirname2.equals(aiSubName)) {
							// if one of them is AI sample, direct the result to anomaly list
							/*
							 * if submission j is AI sample, the anomaly submission is k. Otherwise the
							 * anomaly is j.
							 */
							int anomalySubIdx = submissionID2;
							if (dirname2.equals(aiSubName))
								anomalySubIdx = submissionID1;
							anomalies.add(new AnomalyTuple(anomalySubIdx, assignments[anomalySubIdx].getName(), -1,
									overallSimDegree));

							// add the comparison pair for AI
							aiCodePairs.add(new ComparisonPairTuple(submissionID1, submissionID2, dirname1, dirname2,
									simDegree, surfaceSimDegree, 1, matches));

						} else {
							// add the comparison pair
							ComparisonPairTuple ct = new ComparisonPairTuple(submissionID1, submissionID2, dirname1,
									dirname2, simDegree, surfaceSimDegree, en.getValue(), matches);
							codePairs.add(ct);
						}
					}
				}

				// sort in descending order based on average syntax
				Collections.sort(codePairs);

				// remove extra pairs
				while (codePairs.size() > maxPairs) {
					codePairs.remove(codePairs.size() - 1);
				}

				for (int j = 0; j < simPerSubmission.length; j++) {
					// exclude AI code sample
					if (assignments[j].getName().equals(aiSubName))
						continue;

					int overallDissim = 100 - (simPerSubmission[j] / simPerSubmission.length);
					if (isSensitive)
						overallDissim = 100
								- ((simPerSubmission[j] + surSimPerSubmission[j]) / (2 * simPerSubmission.length));
					if (overallDissim >= dissimThreshold) {
						/*
						 * if there are anomaly tuples resulted from comparison with AI, just update the
						 * syntax dissim
						 */
						boolean isFound = false;
						for (int i = 0; i < anomalies.size(); i++) {
							AnomalyTuple a = anomalies.get(i);
							if (a.getSubmissionID() == j) {
								isFound = true;
								a.setSyntaxDissim(overallDissim);
							}
						}

						if (isFound == false)
							anomalies.add(new AnomalyTuple(j, assignments[j].getName(), overallDissim, -1));
					}
				}

				// sort
				Collections.sort(anomalies);
				// remove extra submissions
				while (anomalies.size() > maxPairs) {
					anomalies.remove(anomalies.size() - 1);
				}

				// generate anomaly reports
				for (int i = 0; i < anomalies.size(); i++) {
					AnomalyTuple ct = anomalies.get(i);
					String syntacticFilename = "uni" + i + ".html";
					String syntacticFilepath = resultPath + File.separator + syntacticFilename;
					ct.setResultedHTMLFilename(syntacticFilename);

					File code1 = CodeReader.getCode(assignments[ct.getSubmissionID()], progLang);

					UniqueCodeHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), ct.getAssignmentName(),
							MainFrame.dissimTemplatePath, syntacticFilepath, humanLang, ct.getDissimResult());
					if (ct.getAiSim() != -1) {
						// generate similarity report comparing with AI
						syntacticFilename = "oai" + i + ".html";
						syntacticFilepath = resultPath + File.separator + syntacticFilename;

						// search the code pair calculated before
						ComparisonPairTuple selected = null;
						for (int j = 0; j < aiCodePairs.size(); j++) {
							ComparisonPairTuple ct2 = aiCodePairs.get(j);
							if (ct2.getSubmissionID1() == ct.getSubmissionID()
									|| ct2.getSubmissionID2() == ct.getSubmissionID()) {
								selected = ct2;
							}
						}

						if (selected != null) {
							// generate the ai comparison
							selected.setResultedHTMLFilename(syntacticFilename);

							code1 = CodeReader.getCode(assignments[selected.getSubmissionID1()], progLang);
							File code2 = CodeReader.getCode(assignments[selected.getSubmissionID2()], progLang);

							// set the path to comparison pair tuple
							ct.setResultedAIHTMLFilename(syntacticFilename);

							if (progLang.equals("py")) {
								PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(),
										code2.getAbsolutePath(), tokenStrings.get(selected.getSubmissionID1()),
										tokenStrings.get(selected.getSubmissionID2()), selected.getAssignmentName1(),
										selected.getAssignmentName2(), MainFrame.pairTemplatePath, syntacticFilepath,
										minMatchLength, selected.getSameClusterOccurrences(), humanLang,
										selected.getMatches());
							} else {
								HtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
										tokenStrings.get(selected.getSubmissionID1()),
										tokenStrings.get(selected.getSubmissionID2()), selected.getAssignmentName1(),
										selected.getAssignmentName2(), MainFrame.pairTemplatePath, syntacticFilepath,
										minMatchLength, selected.getSameClusterOccurrences(), humanLang,
										selected.getMatches());
							}
						}
					}
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

					if (progLang.equals("py")) {
						PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
								tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
								ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
								syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
								ct.getMatches());
					} else {
						HtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
								tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
								ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
								syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
								ct.getMatches());
					}
				}
			}

			// generate the index HTML
			IndexHTMLGenerator.generateHtml(dirPath, codePairs, anomalies, MainFrame.indexTemplatePath, resultPath,
					simThreshold, dissimThreshold, humanLang);
			// generate additional files
			STRANGEPairGenerator.generateAdditionalDir(resultPath);

			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsSuoerBit(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceTokenIndexes, ArrayList<String> vectorHeader,
			String dirPath, String resultPath, String progLang, String humanLang, int simThreshold, int dissimThreshold,
			String aiSubName, int minMatchLength, int maxPairs, String languageCode, int numClusters, int numStages,
			boolean isSensitive) {
		// Super-Bit algorithm

		// to calculate dissimilarity and anomaly
		ArrayList<AnomalyTuple> anomalies = new ArrayList<>();
		int[] simPerSubmission = new int[tokenStrings.size()];
		int[] surSimPerSubmission = new int[tokenStrings.size()];

		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();
			ArrayList<ComparisonPairTuple> aiCodePairs = new ArrayList<>();

			// generate Cosine vectors
			ArrayList<double[]> tokenVectors = new ArrayList<double[]>();
			for (int i = 0; i < tokenIndexes.size(); i++) {
				tokenVectors.add(
						CosineCalculator.generateOccurrenceVectorFromTokenString(tokenIndexes.get(i), vectorHeader));
			}

			// create SuperBit object with best default setting
			int lshn = vectorHeader.size();
			if (lshn > 0) {
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

					String dirname1 = assignments[submissionID1].getName();
					String dirname2 = assignments[submissionID2].getName();

					HashMap<String, ArrayList<Integer>> tokenIndex1 = tokenIndexes.get(submissionID1);
					HashMap<String, ArrayList<Integer>> tokenIndex2 = tokenIndexes.get(submissionID2);

					// generate the matches
					ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndex1, tokenIndex2,
							minMatchLength);

					// get the sim degree
					int simDegree = (int) (MatchGenerator.calcAverageSimilarity(matches,
							tokenStrings.get(submissionID1).size(), tokenStrings.get(submissionID2).size()) * 100);

					// add the sim degrees except for AI sample
					if (!(dirname1.equals(aiSubName) || dirname2.equals(aiSubName))) {
						simPerSubmission[submissionID1] += simDegree;
						simPerSubmission[submissionID2] += simDegree;
					}

					// if the surface index is not empty, then calculate surface sim
					double surfaceSimDegree = -1;
					if (surfaceTokenIndexes.size() > 0) {
						HashMap<String, ArrayList<Integer>> surfaceTokenIndex1 = surfaceTokenIndexes.get(submissionID1);
						HashMap<String, ArrayList<Integer>> surfaceTokenIndex2 = surfaceTokenIndexes.get(submissionID2);

						ArrayList<GSTMatchTuple> surfaceMatches = MatchGenerator.generateMatches(surfaceTokenIndex1,
								surfaceTokenIndex2, minMatchLength);

						surfaceSimDegree = (int) (MatchGenerator.calcAverageSimilarity(surfaceMatches,
								tokenStrings.get(submissionID1).size(), tokenStrings.get(submissionID2).size()) * 100);

						// add the sim degrees except for AI sample
						if (!(dirname1.equals(aiSubName) || dirname2.equals(aiSubName))) {
							surSimPerSubmission[submissionID1] += surfaceSimDegree;
							surSimPerSubmission[submissionID2] += surfaceSimDegree;
						}
					}

					int overallSimDegree = simDegree;
					if (isSensitive)
						overallSimDegree = (int) ((simDegree + surfaceSimDegree) / 2);

					if (overallSimDegree >= simThreshold) {

						File code1 = CodeReader.getCode(assignments[submissionID1], progLang);
						File code2 = CodeReader.getCode(assignments[submissionID2], progLang);

						// to deal with non-code directories and files
						if (code1 == null || code2 == null)
							continue;

						if (dirname1.equals(aiSubName) || dirname2.equals(aiSubName)) {
							// if one of them is AI sample, direct the result to anomaly list
							/*
							 * if submission j is AI sample, the anomaly submission is k. Otherwise the
							 * anomaly is j.
							 */
							int anomalySubIdx = submissionID2;
							if (dirname2.equals(aiSubName))
								anomalySubIdx = submissionID1;
							anomalies.add(new AnomalyTuple(anomalySubIdx, assignments[anomalySubIdx].getName(), -1,
									overallSimDegree));

							// add the comparison pair for AI
							aiCodePairs.add(new ComparisonPairTuple(submissionID1, submissionID2, dirname1, dirname2,
									simDegree, surfaceSimDegree, 1, matches));

						} else {
							// add the comparison pair
							ComparisonPairTuple ct = new ComparisonPairTuple(submissionID1, submissionID2, dirname1,
									dirname2, simDegree, surfaceSimDegree, en.getValue(), matches);
							codePairs.add(ct);
						}
					}
				}

				// sort in descending order based on average syntax
				Collections.sort(codePairs);

				// remove extra pairs
				while (codePairs.size() > maxPairs) {
					codePairs.remove(codePairs.size() - 1);
				}

				for (int j = 0; j < simPerSubmission.length; j++) {
					// exclude AI code sample
					if (assignments[j].getName().equals(aiSubName))
						continue;

					int overallDissim = 100 - (simPerSubmission[j] / simPerSubmission.length);
					if (isSensitive)
						overallDissim = 100
								- ((simPerSubmission[j] + surSimPerSubmission[j]) / (2 * simPerSubmission.length));
					if (overallDissim >= dissimThreshold) {
						/*
						 * if there are anomaly tuples resulted from comparison with AI, just update the
						 * syntax dissim
						 */
						boolean isFound = false;
						for (int i = 0; i < anomalies.size(); i++) {
							AnomalyTuple a = anomalies.get(i);
							if (a.getSubmissionID() == j) {
								isFound = true;
								a.setSyntaxDissim(overallDissim);
							}
						}

						if (isFound == false)
							anomalies.add(new AnomalyTuple(j, assignments[j].getName(), overallDissim, -1));
					}
				}
				// sort
				Collections.sort(anomalies);
				// remove extra submissions
				while (anomalies.size() > maxPairs) {
					anomalies.remove(anomalies.size() - 1);
				}

				// generate anomaly reports
				for (int i = 0; i < anomalies.size(); i++) {
					AnomalyTuple ct = anomalies.get(i);
					String syntacticFilename = "uni" + i + ".html";
					String syntacticFilepath = resultPath + File.separator + syntacticFilename;
					ct.setResultedHTMLFilename(syntacticFilename);

					File code1 = CodeReader.getCode(assignments[ct.getSubmissionID()], progLang);

					UniqueCodeHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), ct.getAssignmentName(),
							MainFrame.dissimTemplatePath, syntacticFilepath, humanLang, ct.getDissimResult());
					if (ct.getAiSim() != -1) {
						// generate similarity report comparing with AI
						syntacticFilename = "oai" + i + ".html";
						syntacticFilepath = resultPath + File.separator + syntacticFilename;

						// search the code pair calculated before
						ComparisonPairTuple selected = null;
						for (int j = 0; j < aiCodePairs.size(); j++) {
							ComparisonPairTuple ct2 = aiCodePairs.get(j);
							if (ct2.getSubmissionID1() == ct.getSubmissionID()
									|| ct2.getSubmissionID2() == ct.getSubmissionID()) {
								selected = ct2;
							}
						}

						if (selected != null) {
							// generate the ai comparison
							selected.setResultedHTMLFilename(syntacticFilename);

							code1 = CodeReader.getCode(assignments[selected.getSubmissionID1()], progLang);
							File code2 = CodeReader.getCode(assignments[selected.getSubmissionID2()], progLang);

							// set the path to comparison pair tuple
							ct.setResultedAIHTMLFilename(syntacticFilename);

							if (progLang.equals("py")) {
								PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(),
										code2.getAbsolutePath(), tokenStrings.get(selected.getSubmissionID1()),
										tokenStrings.get(selected.getSubmissionID2()), selected.getAssignmentName1(),
										selected.getAssignmentName2(), MainFrame.pairTemplatePath, syntacticFilepath,
										minMatchLength, selected.getSameClusterOccurrences(), humanLang,
										selected.getMatches());
							} else {
								HtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
										tokenStrings.get(selected.getSubmissionID1()),
										tokenStrings.get(selected.getSubmissionID2()), selected.getAssignmentName1(),
										selected.getAssignmentName2(), MainFrame.pairTemplatePath, syntacticFilepath,
										minMatchLength, selected.getSameClusterOccurrences(), humanLang,
										selected.getMatches());
							}
						}
					}
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

					if (progLang.equals("py")) {
						PythonHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
								tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
								ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
								syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
								ct.getMatches());
					} else {
						HtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
								tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
								ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairTemplatePath,
								syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang,
								ct.getMatches());
					}
				}
			}

			// generate the index HTML
			IndexHTMLGenerator.generateHtml(dirPath, codePairs, anomalies, MainFrame.indexTemplatePath, resultPath,
					simThreshold, dissimThreshold, humanLang);
			// generate additional files
			STRANGEPairGenerator.generateAdditionalDir(resultPath);

			return codePairs;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}

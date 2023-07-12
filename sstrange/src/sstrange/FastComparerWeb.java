package sstrange;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Scanner;

import info.debatty.java.lsh.LSHMinHash;
import info.debatty.java.lsh.LSHSuperBit;
import sstrange.htmlgenerator.CodeReader;
import sstrange.language.css.CSSFeedbackGenerator;
import sstrange.language.html.HTMLFeedbackGenerator;
import sstrange.language.html.HTMLHtmlGenerator;
import sstrange.language.js.JSFeedbackGenerator;
import sstrange.lshcalculator.CosineCalculator;
import sstrange.lshcalculator.IndexGenerator;
import sstrange.lshcalculator.JaccardCalculator;
import sstrange.matchgenerator.ComparisonPairTuple;
import sstrange.matchgenerator.ComparisonPairTupleWeb;
import sstrange.matchgenerator.MatchGenerator;
import sstrange.message.FeedbackMessageGenerator;
import sstrange.token.FeedbackToken;
import support.stringmatching.GSTMatchTuple;

public class FastComparerWeb {

	public static ArrayList<ComparisonPairTuple> doSyntacticComparison(String assignmentPath, String humanLang,
			int simThreshold, int minMatchingLength, int maxPairs, String templateDirPath, String similarityMeasurement,
			File assignmentFile, String assignmentParentDirPath, String assignmentName, boolean isMultipleFiles,
			boolean isCommonCodeAllowed, ArrayList<File> filesToBeDeleted,
			int numClusters, int numStages) {

		// if multiple files, merge them
		if (isMultipleFiles) {
			String newAssignmentPath = assignmentParentDirPath + File.separator + "[merged] " + assignmentName;
			CodeMerger.mergeCode(assignmentPath, "html", newAssignmentPath);
			assignmentPath = newAssignmentPath;
			// mark new assignment path to be deleted after the whole process
			filesToBeDeleted.add(new File(newAssignmentPath));
		}

		// set result dir based on the name of assignment
		String resultPath = assignmentParentDirPath + File.separator + "[out] " + assignmentName;

		// generate STRANGE reports
		return compareAndGenerateHTMLReports(assignmentPath, resultPath, humanLang, simThreshold, minMatchingLength,
				maxPairs, humanLang, isMultipleFiles, similarityMeasurement, numClusters,
				numStages, filesToBeDeleted);

	}

	private static ArrayList<ComparisonPairTuple> compareAndGenerateHTMLReports(String dirPath, String resultPath,
			String humanLang, int simThreshold, int minMatchLength, int maxPairs, String languageCode,
			boolean isMultipleFiles, String similarityMeasurement,
			int numClusters, int numStages, ArrayList<File> filesToBeDeleted) {
		// create the output dir
		File resultDir = new File(resultPath);
		resultDir.mkdir();

		File[] assignments = (new File(dirPath)).listFiles();

		// tokenise all programs and generate the indexes
		ArrayList<ArrayList<FeedbackToken>> tokenStrings = new ArrayList<ArrayList<FeedbackToken>>();
		// for javascript
		ArrayList<ArrayList<FeedbackToken>> jsTokenStrings = new ArrayList<ArrayList<FeedbackToken>>();
		// for css
		ArrayList<ArrayList<FeedbackToken>> cssTokenStrings = new ArrayList<ArrayList<FeedbackToken>>();

		// the indexes
		ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();
		ArrayList<HashMap<String, ArrayList<Integer>>> jsTokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();
		ArrayList<HashMap<String, ArrayList<Integer>>> cssTokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();
		for (int i = 0; i < assignments.length; i++) {
			// for each code, tokenise and generalise
			File code = CodeReader.getCode(assignments[i], "html");

			if (code == null) {
				tokenStrings.add(new ArrayList<FeedbackToken>());
				tokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
				jsTokenStrings.add(new ArrayList<FeedbackToken>());
				jsTokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
				cssTokenStrings.add(new ArrayList<FeedbackToken>());
				cssTokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
			} else {

				// generate HTML token strings
				ArrayList<FeedbackToken> tokenString = null;
				tokenString = HTMLFeedbackGenerator.generateFeedbackTokenString(code.getAbsolutePath(),
						isMultipleFiles);
				tokenString = HTMLFeedbackGenerator.syntaxTokenStringPreprocessing(tokenString);
				// prepare for deletion
				filesToBeDeleted.add(new File(code.getAbsolutePath() + ".js"));
				filesToBeDeleted.add(new File(code.getAbsolutePath() + ".css"));

				File jscode, csscode;

				if (isMultipleFiles) {
					jscode = CodeReader.getCodeButNotThis(assignments[i], "js", code.getName() + ".js");
					csscode = CodeReader.getCodeButNotThis(assignments[i], "css", code.getName() + ".css");

					// merge js obtained from html to the merged javascript file and remove the file
					try {
						FileWriter fw = new FileWriter(jscode, true);
						File jsinhtml = new File(code.getAbsolutePath() + ".js");
						if (jsinhtml.exists()) {
							// take all the content
							Scanner sc = new Scanner(jsinhtml);
							while (sc.hasNextLine()) {
								fw.write(sc.nextLine() + System.lineSeparator());
							}
							sc.close();
							fw.close();
							// delete the file
							jsinhtml.delete();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					// merge css obtained from html to the merged css file and remove the file
					try {
						FileWriter fw = new FileWriter(csscode, true);
						File cssinhtml = new File(code.getAbsolutePath() + ".css");
						if (cssinhtml.exists()) {
							// take all the content
							Scanner sc = new Scanner(cssinhtml);
							while (sc.hasNextLine()) {
								fw.write(sc.nextLine() + System.lineSeparator());
							}
							sc.close();
							fw.close();
							// delete the file
							cssinhtml.delete();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					jscode = CodeReader.getCode(assignments[i], "js");
					csscode = CodeReader.getCode(assignments[i], "css");
				}

				// generate token strings
				ArrayList<FeedbackToken> jsTokenString = null, 
						cssTokenString = null;

				if (jscode == null) {
					jsTokenString = new ArrayList<FeedbackToken>();
				} else {
					jsTokenString = JSFeedbackGenerator.generateFeedbackTokenString(jscode.getAbsolutePath());
					jsTokenString = HTMLFeedbackGenerator.syntaxTokenStringPreprocessing(jsTokenString);
				}

				if (csscode == null) {
					cssTokenString = new ArrayList<FeedbackToken>();
				} else {
					cssTokenString = CSSFeedbackGenerator.generateFeedbackTokenString(csscode.getAbsolutePath());
					cssTokenString = CSSFeedbackGenerator.syntaxTokenStringPreprocessing(cssTokenString);
				}

				// add the token string and its raw form
				tokenStrings.add(tokenString);
				jsTokenStrings.add(jsTokenString);
				cssTokenStrings.add(cssTokenString);

				// if not RKRGST
				if (similarityMeasurement.equalsIgnoreCase("RKRGST") == false) {
					// generate the index
					HashMap<String, ArrayList<Integer>> tokenIndex = IndexGenerator.generateIndex(tokenString,
							minMatchLength);
					HashMap<String, ArrayList<Integer>> jsTokenIndex = IndexGenerator.generateIndex(jsTokenString,
							minMatchLength);
					HashMap<String, ArrayList<Integer>> cssTokenIndex = IndexGenerator.generateIndex(cssTokenString,
							minMatchLength);

					// add the token index
					tokenIndexes.add(tokenIndex);
					jsTokenIndexes.add(jsTokenIndex);
					cssTokenIndexes.add(cssTokenIndex);
				}
			}
		}

		String progLang = "html";
		if (similarityMeasurement.equalsIgnoreCase("MinHash")) {
			// generate vector header (html, js, dan css)
			ArrayList<String> vectorHeader = IndexGenerator.generateVectorHeader(tokenIndexes);
			vectorHeader.addAll(IndexGenerator.generateVectorHeader(jsTokenIndexes));
			vectorHeader.addAll(IndexGenerator.generateVectorHeader(cssTokenIndexes));

			// calculate minhash
			return _compareAndGenerateHTMLReportsMinHash(assignments, tokenStrings, jsTokenStrings,
					cssTokenStrings, tokenIndexes, jsTokenIndexes,
					cssTokenIndexes, vectorHeader, dirPath, resultPath, progLang, humanLang, simThreshold,
					minMatchLength, maxPairs, languageCode, numClusters, numStages);
		} else if (similarityMeasurement.equalsIgnoreCase("Super-Bit")) {
			// generate vector header (html, js, dan css)
			ArrayList<String> vectorHeader = IndexGenerator.generateVectorHeader(tokenIndexes);
			vectorHeader.addAll(IndexGenerator.generateVectorHeader(jsTokenIndexes));
			vectorHeader.addAll(IndexGenerator.generateVectorHeader(cssTokenIndexes));

			// calculate super-bit
			return _compareAndGenerateHTMLReportsSuoerBit(assignments, tokenStrings, jsTokenStrings,
					cssTokenStrings, tokenIndexes, jsTokenIndexes,
					cssTokenIndexes, vectorHeader, dirPath, resultPath, progLang, humanLang, simThreshold,
					minMatchLength, maxPairs, languageCode, numClusters, numStages);
		} else if (similarityMeasurement.equalsIgnoreCase("Jaccard")) {
			return _compareAndGenerateHTMLReportsJaccard(assignments, tokenStrings, jsTokenStrings,
					 cssTokenStrings,  tokenIndexes, jsTokenIndexes,
					cssTokenIndexes, dirPath, resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs,
					languageCode);
		} else if (similarityMeasurement.equalsIgnoreCase("Cosine")) {
			return _compareAndGenerateHTMLReportsCosine(assignments, tokenStrings, jsTokenStrings,
					cssTokenStrings, tokenIndexes, jsTokenIndexes,
					cssTokenIndexes, dirPath, resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs,
					languageCode);
		} else {
			// RKRGST
			return _compareAndGenerateHTMLReportsRKRGST(assignments, tokenStrings, jsTokenStrings,
					cssTokenStrings,  dirPath, resultPath, progLang, humanLang,
					simThreshold, minMatchLength, maxPairs, languageCode);
		}

	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsRKRGST(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, 
			ArrayList<ArrayList<FeedbackToken>> jsTokenStrings, 
			ArrayList<ArrayList<FeedbackToken>> cssTokenStrings, 
			String dirPath, String resultPath, String progLang, String humanLang, int simThreshold, int minMatchLength,
			int maxPairs, String languageCode) {
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
					// javascript
					ArrayList<GSTMatchTuple> jsSimTuples = FeedbackMessageGenerator
							.generateMatchedTuples(jsTokenStrings.get(j), jsTokenStrings.get(k), minMatchLength);
					// CSS
					ArrayList<GSTMatchTuple> cssSimTuples = FeedbackMessageGenerator
							.generateMatchedTuples(cssTokenStrings.get(j), cssTokenStrings.get(k), minMatchLength);

					// get the sim degree for RKRGST
					int simDegree = (int) (((double) (2 * (MatchGenerator.coverage(simTuples)
							+ MatchGenerator.coverage(jsSimTuples) + MatchGenerator.coverage(cssSimTuples)))
							/ (double) (tokenStrings.get(j).size() + jsTokenStrings.get(j).size()
									+ cssTokenStrings.get(j).size() + tokenStrings.get(k).size()
									+ jsTokenStrings.get(k).size() + cssTokenStrings.get(k).size()))
							* 100);

					if (simDegree >= simThreshold) {
						String dirname1 = assignments[j].getName();
						String dirname2 = assignments[k].getName();

						File code1 = CodeReader.getCode(assignments[j], progLang);
						File code2 = CodeReader.getCode(assignments[k], progLang);

						// to deal with non-code directories and files
						if (code1 == null || code2 == null)
							continue;

						// add the comparison pair
						codePairs.add(new ComparisonPairTupleWeb(j, k, dirname1, dirname2, simDegree, 1, simTuples,
								jsSimTuples, cssSimTuples));
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
				ComparisonPairTupleWeb ct = (ComparisonPairTupleWeb) codePairs.get(i);
				String syntacticFilename = "obs" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], progLang);
				File code2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], progLang);
				File jsCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "js");
				File jsCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "js");
				File cssCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "css");
				File cssCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "css");

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				HTMLHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
						jsCode1.getAbsolutePath(), jsCode2.getAbsolutePath(), cssCode1.getAbsolutePath(),
						cssCode2.getAbsolutePath(), tokenStrings.get(ct.getSubmissionID1()),
						tokenStrings.get(ct.getSubmissionID2()), jsTokenStrings.get(ct.getSubmissionID1()),
						jsTokenStrings.get(ct.getSubmissionID2()), cssTokenStrings.get(ct.getSubmissionID1()),
						cssTokenStrings.get(ct.getSubmissionID2()), ct.getAssignmentName1(), ct.getAssignmentName2(),
						MainFrame.pairWebTemplatePath, syntacticFilepath, minMatchLength,
						ct.getSameClusterOccurrences(), humanLang, ct.getMatches(), ct.getJsMatches(),
						ct.getCssMatches());
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
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, 
			ArrayList<ArrayList<FeedbackToken>> jsTokenStrings, 
			ArrayList<ArrayList<FeedbackToken>> cssTokenStrings, 
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> jsTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> cssTokenIndexes, String dirPath, String resultPath,
			String progLang, String humanLang, int simThreshold, int minMatchLength, int maxPairs,
			String languageCode) {
		// Jaccard
		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();

			// do the comparison
			for (int j = 0; j < tokenIndexes.size(); j++) {
				for (int k = j + 1; k < tokenIndexes.size(); k++) {
					HashMap<String, ArrayList<Integer>> tokenIndex1 = new HashMap<String, ArrayList<Integer>>();
					HashMap<String, ArrayList<Integer>> tokenIndex2 = new HashMap<String, ArrayList<Integer>>();

					// add all html, js, and cs for token index 1
					tokenIndex1.putAll(tokenIndexes.get(j));
					tokenIndex1.putAll(jsTokenIndexes.get(j));
					tokenIndex1.putAll(cssTokenIndexes.get(j));

					// add all html, js, and cs for token index 2
					tokenIndex2.putAll(tokenIndexes.get(k));
					tokenIndex2.putAll(jsTokenIndexes.get(k));
					tokenIndex2.putAll(cssTokenIndexes.get(k));

					// get the sim degree for jaccard
					int simDegree = (int) (JaccardCalculator.calculateJaccardSimilarity(tokenIndex1, tokenIndex2)
							* 100);

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
						ArrayList<GSTMatchTuple> jsMatches = MatchGenerator.generateMatches(jsTokenIndexes.get(j),
								jsTokenIndexes.get(k), minMatchLength);
						ArrayList<GSTMatchTuple> cssMatches = MatchGenerator.generateMatches(cssTokenIndexes.get(j),
								cssTokenIndexes.get(k), minMatchLength);

						// add the comparison pair
						codePairs.add(new ComparisonPairTupleWeb(j, k, dirname1, dirname2, simDegree, 1, matches,
								jsMatches, cssMatches));
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
				ComparisonPairTupleWeb ct = (ComparisonPairTupleWeb) codePairs.get(i);
				String syntacticFilename = "obs" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], progLang);
				File code2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], progLang);
				File jsCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "js");
				File jsCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "js");
				File cssCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "css");
				File cssCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "css");

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				HTMLHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
						jsCode1.getAbsolutePath(), jsCode2.getAbsolutePath(), cssCode1.getAbsolutePath(),
						cssCode2.getAbsolutePath(), tokenStrings.get(ct.getSubmissionID1()),
						tokenStrings.get(ct.getSubmissionID2()), jsTokenStrings.get(ct.getSubmissionID1()),
						jsTokenStrings.get(ct.getSubmissionID2()), cssTokenStrings.get(ct.getSubmissionID1()),
						cssTokenStrings.get(ct.getSubmissionID2()), ct.getAssignmentName1(), ct.getAssignmentName2(),
						MainFrame.pairWebTemplatePath, syntacticFilepath, minMatchLength,
						ct.getSameClusterOccurrences(), humanLang, ct.getMatches(), ct.getJsMatches(),
						ct.getCssMatches());
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
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, 
			ArrayList<ArrayList<FeedbackToken>> jsTokenStrings, 
			ArrayList<ArrayList<FeedbackToken>> cssTokenStrings, 
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> jsTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> cssTokenIndexes, String dirPath, String resultPath,
			String progLang, String humanLang, int simThreshold, int minMatchLength, int maxPairs,
			String languageCode) {
		// Jaccard
		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();

			// do the comparison
			for (int j = 0; j < tokenIndexes.size(); j++) {
				for (int k = j + 1; k < tokenIndexes.size(); k++) {
					HashMap<String, ArrayList<Integer>> tokenIndex1 = new HashMap<String, ArrayList<Integer>>();
					HashMap<String, ArrayList<Integer>> tokenIndex2 = new HashMap<String, ArrayList<Integer>>();

					// add all html, js, and cs for token index 1
					tokenIndex1.putAll(tokenIndexes.get(j));
					tokenIndex1.putAll(jsTokenIndexes.get(j));
					tokenIndex1.putAll(cssTokenIndexes.get(j));

					// add all html, js, and cs for token index 2
					tokenIndex2.putAll(tokenIndexes.get(k));
					tokenIndex2.putAll(jsTokenIndexes.get(k));
					tokenIndex2.putAll(cssTokenIndexes.get(k));

					// get the sim degree for cosine
					int simDegree = (int) (CosineCalculator.calculateCosineSimilarity(tokenIndex1, tokenIndex2) * 100);

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
						ArrayList<GSTMatchTuple> jsMatches = MatchGenerator.generateMatches(jsTokenIndexes.get(j),
								jsTokenIndexes.get(k), minMatchLength);
						ArrayList<GSTMatchTuple> cssMatches = MatchGenerator.generateMatches(cssTokenIndexes.get(j),
								cssTokenIndexes.get(k), minMatchLength);

						// add the comparison pair
						codePairs.add(new ComparisonPairTupleWeb(j, k, dirname1, dirname2, simDegree, 1, matches,
								jsMatches, cssMatches));
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
				ComparisonPairTupleWeb ct = (ComparisonPairTupleWeb) codePairs.get(i);
				String syntacticFilename = "obs" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], progLang);
				File code2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], progLang);
				File jsCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "js");
				File jsCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "js");
				File cssCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "css");
				File cssCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "css");

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				HTMLHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
						jsCode1.getAbsolutePath(), jsCode2.getAbsolutePath(), cssCode1.getAbsolutePath(),
						cssCode2.getAbsolutePath(), tokenStrings.get(ct.getSubmissionID1()),
						tokenStrings.get(ct.getSubmissionID2()), jsTokenStrings.get(ct.getSubmissionID1()),
						jsTokenStrings.get(ct.getSubmissionID2()), cssTokenStrings.get(ct.getSubmissionID1()),
						cssTokenStrings.get(ct.getSubmissionID2()),ct.getAssignmentName1(), ct.getAssignmentName2(),
						MainFrame.pairWebTemplatePath, syntacticFilepath, minMatchLength,
						ct.getSameClusterOccurrences(), humanLang, ct.getMatches(), ct.getJsMatches(),
						ct.getCssMatches());
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
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, 
			ArrayList<ArrayList<FeedbackToken>> jsTokenStrings, 
			ArrayList<ArrayList<FeedbackToken>> cssTokenStrings, 
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> jsTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> cssTokenIndexes, ArrayList<String> vectorHeader,
			String dirPath, String resultPath, String progLang, String humanLang, int simThreshold, int minMatchLength,
			int maxPairs, String languageCode, int numClusters, int numStages) {
		// MinHash algorithm

		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();

			// generate Jaccard vectors
			ArrayList<boolean[]> tokenVectors = new ArrayList<boolean[]>();
			for (int i = 0; i < tokenIndexes.size(); i++) {
				// generate the vector from three indexes, vectorHeader already merge three
				// indexes
				boolean[] v = JaccardCalculator.generateBooleanVectorFromTokenString(tokenIndexes.get(i), vectorHeader);
				v = JaccardCalculator.updateBooleanVectorFromTokenString(v, jsTokenIndexes.get(i), vectorHeader);
				v = JaccardCalculator.updateBooleanVectorFromTokenString(v, cssTokenIndexes.get(i), vectorHeader);
				tokenVectors.add(v);
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

				// generate the matches
				ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndexes.get(submissionID1),
						tokenIndexes.get(submissionID2), minMatchLength);
				ArrayList<GSTMatchTuple> jsmatches = MatchGenerator.generateMatches(jsTokenIndexes.get(submissionID1),
						jsTokenIndexes.get(submissionID2), minMatchLength);
				ArrayList<GSTMatchTuple> cssmatches = MatchGenerator.generateMatches(cssTokenIndexes.get(submissionID1),
						cssTokenIndexes.get(submissionID2), minMatchLength);

				// get the sim degree
				int simDegree = (int) (((double) (2 * (MatchGenerator.coverage(matches)
						+ MatchGenerator.coverage(jsmatches) + MatchGenerator.coverage(cssmatches)))
						/ (double) (tokenStrings.get(submissionID1).size() + jsTokenStrings.get(submissionID1).size()
								+ cssTokenStrings.get(submissionID1).size() + tokenStrings.get(submissionID2).size()
								+ jsTokenStrings.get(submissionID2).size() + cssTokenStrings.get(submissionID2).size()))
						* 100);
				if (simDegree >= simThreshold) {
					String dirname1 = assignments[submissionID1].getName();
					String dirname2 = assignments[submissionID2].getName();

					File code1 = CodeReader.getCode(assignments[submissionID1], progLang);
					File code2 = CodeReader.getCode(assignments[submissionID2], progLang);

					// to deal with non-code directories and files
					if (code1 == null || code2 == null)
						continue;

					// generate the matches
					matches = MatchGenerator.generateMatches(tokenIndexes.get(submissionID1),
							tokenIndexes.get(submissionID2), minMatchLength);
					ArrayList<GSTMatchTuple> jsMatches = MatchGenerator.generateMatches(
							jsTokenIndexes.get(submissionID1), jsTokenIndexes.get(submissionID2), minMatchLength);
					ArrayList<GSTMatchTuple> cssMatches = MatchGenerator.generateMatches(
							cssTokenIndexes.get(submissionID1), cssTokenIndexes.get(submissionID2), minMatchLength);

					// add the comparison pair
					ComparisonPairTuple ct = new ComparisonPairTupleWeb(submissionID1, submissionID2, dirname1,
							dirname2, simDegree, en.getValue(), matches, jsMatches, cssMatches);
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
				ComparisonPairTupleWeb ct = (ComparisonPairTupleWeb) (codePairs.get(i));
				String syntacticFilename = "obs" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], progLang);
				File code2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], progLang);
				File jsCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "js");
				File jsCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "js");
				File cssCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "css");
				File cssCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "css");

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				HTMLHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
						jsCode1.getAbsolutePath(), jsCode2.getAbsolutePath(), cssCode1.getAbsolutePath(),
						cssCode2.getAbsolutePath(), tokenStrings.get(ct.getSubmissionID1()),
						tokenStrings.get(ct.getSubmissionID2()), jsTokenStrings.get(ct.getSubmissionID1()),
						jsTokenStrings.get(ct.getSubmissionID2()), cssTokenStrings.get(ct.getSubmissionID1()),
						cssTokenStrings.get(ct.getSubmissionID2()), ct.getAssignmentName1(), ct.getAssignmentName2(),
						MainFrame.pairWebTemplatePath, syntacticFilepath, minMatchLength,
						ct.getSameClusterOccurrences(), humanLang, ct.getMatches(), ct.getJsMatches(),
						ct.getCssMatches());
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
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, 
			ArrayList<ArrayList<FeedbackToken>> jsTokenStrings, 
			ArrayList<ArrayList<FeedbackToken>> cssTokenStrings, 
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> jsTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> cssTokenIndexes, ArrayList<String> vectorHeader,
			String dirPath, String resultPath, String progLang, String humanLang, int simThreshold, int minMatchLength,
			int maxPairs, String languageCode, int numClusters, int numStages) {
		// Super-Bit algorithm
		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();

			// generate Cosine vectors
			ArrayList<double[]> tokenVectors = new ArrayList<double[]>();
			for (int i = 0; i < tokenIndexes.size(); i++) {
				double[] v = CosineCalculator.generateOccurrenceVectorFromTokenString(tokenIndexes.get(i),
						vectorHeader);
				v = CosineCalculator.updateOccurrenceVectorFromTokenString(v, jsTokenIndexes.get(i), vectorHeader);
				v = CosineCalculator.updateOccurrenceVectorFromTokenString(v, cssTokenIndexes.get(i), vectorHeader);
				tokenVectors.add(v);
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

				// generate the matches
				ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndexes.get(submissionID1),
						tokenIndexes.get(submissionID2), minMatchLength);
				ArrayList<GSTMatchTuple> jsmatches = MatchGenerator.generateMatches(jsTokenIndexes.get(submissionID1),
						jsTokenIndexes.get(submissionID2), minMatchLength);
				ArrayList<GSTMatchTuple> cssmatches = MatchGenerator.generateMatches(cssTokenIndexes.get(submissionID1),
						cssTokenIndexes.get(submissionID2), minMatchLength);

				// get the sim degree
				int simDegree = (int) (((double) (2 * (MatchGenerator.coverage(matches)
						+ MatchGenerator.coverage(jsmatches) + MatchGenerator.coverage(cssmatches)))
						/ (double) (tokenStrings.get(submissionID1).size() + jsTokenStrings.get(submissionID1).size()
								+ cssTokenStrings.get(submissionID1).size() + tokenStrings.get(submissionID2).size()
								+ jsTokenStrings.get(submissionID2).size() + cssTokenStrings.get(submissionID2).size()))
						* 100);

				if (simDegree >= simThreshold) {
					String dirname1 = assignments[submissionID1].getName();
					String dirname2 = assignments[submissionID2].getName();

					File code1 = CodeReader.getCode(assignments[submissionID1], progLang);
					File code2 = CodeReader.getCode(assignments[submissionID2], progLang);

					// to deal with non-code directories and files
					if (code1 == null || code2 == null)
						continue;

					// generate the matches
					matches = MatchGenerator.generateMatches(tokenIndexes.get(submissionID1),
							tokenIndexes.get(submissionID2), minMatchLength);
					ArrayList<GSTMatchTuple> jsMatches = MatchGenerator.generateMatches(
							jsTokenIndexes.get(submissionID1), jsTokenIndexes.get(submissionID2), minMatchLength);
					ArrayList<GSTMatchTuple> cssMatches = MatchGenerator.generateMatches(
							cssTokenIndexes.get(submissionID1), cssTokenIndexes.get(submissionID2), minMatchLength);

					// add the comparison pair
					ComparisonPairTuple ct = new ComparisonPairTupleWeb(submissionID1, submissionID2, dirname1,
							dirname2, simDegree, en.getValue(), matches, jsMatches, cssMatches);
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
				ComparisonPairTupleWeb ct = (ComparisonPairTupleWeb) codePairs.get(i);
				String syntacticFilename = "obs" + i + ".html";
				String syntacticFilepath = resultPath + File.separator + syntacticFilename;

				File code1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], progLang);
				File code2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], progLang);
				File jsCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "js");
				File jsCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "js");
				File cssCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "css");
				File cssCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "css");

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				HTMLHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
						jsCode1.getAbsolutePath(), jsCode2.getAbsolutePath(), cssCode1.getAbsolutePath(),
						cssCode2.getAbsolutePath(), tokenStrings.get(ct.getSubmissionID1()),
						tokenStrings.get(ct.getSubmissionID2()), jsTokenStrings.get(ct.getSubmissionID1()),
						jsTokenStrings.get(ct.getSubmissionID2()), cssTokenStrings.get(ct.getSubmissionID1()),
						cssTokenStrings.get(ct.getSubmissionID2()), ct.getAssignmentName1(), ct.getAssignmentName2(),
						MainFrame.pairWebTemplatePath, syntacticFilepath, minMatchLength,
						ct.getSameClusterOccurrences(), humanLang, ct.getMatches(), ct.getJsMatches(),
						ct.getCssMatches());
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

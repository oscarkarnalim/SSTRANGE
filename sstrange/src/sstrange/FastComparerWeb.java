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
import sstrange.language.php.PHPFeedbackGenerator;
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
			int simThreshold, int minMatchingLength, int maxPairs, String similarityMeasurement, File assignmentFile,
			String assignmentParentDirPath, String assignmentName, boolean isMultipleFiles, boolean isCommonCodeAllowed,
			ArrayList<File> filesToBeDeleted, int numClusters, int numStages, boolean isSensitive) {

		// if multiple files, merge them
		if (isMultipleFiles) {
			String newAssignmentPath = assignmentParentDirPath + File.separator + "[merged] " + assignmentName;
			CodeMerger.mergeCode(assignmentPath, "html", newAssignmentPath); // including PHP, external CSS and external
																				// JS
			assignmentPath = newAssignmentPath;
			// mark new assignment path to be deleted after the whole process
			filesToBeDeleted.add(new File(newAssignmentPath));
		}

		// set result dir based on the name of assignment
		String resultPath = assignmentParentDirPath + File.separator + "[out] " + assignmentName;

		// generate STRANGE reports
		return compareAndGenerateHTMLReports(assignmentPath, resultPath, humanLang, simThreshold, minMatchingLength,
				maxPairs, humanLang, isMultipleFiles, similarityMeasurement, numClusters, numStages, filesToBeDeleted,
				isSensitive);

	}

	private static ArrayList<ComparisonPairTuple> compareAndGenerateHTMLReports(String dirPath, String resultPath,
			String humanLang, int simThreshold, int minMatchLength, int maxPairs, String languageCode,
			boolean isMultipleFiles, String similarityMeasurement, int numClusters, int numStages,
			ArrayList<File> filesToBeDeleted, boolean isSensitive) {
		// create the output dir
		File resultDir = new File(resultPath);
		resultDir.mkdir();

		File[] assignments = (new File(dirPath)).listFiles();

		// tokenise all programs and generate the indexes
		ArrayList<ArrayList<FeedbackToken>> tokenStrings = new ArrayList<ArrayList<FeedbackToken>>();
		// for PHP
		ArrayList<ArrayList<FeedbackToken>> phpTokenStrings = new ArrayList<ArrayList<FeedbackToken>>();
		// for javascript
		ArrayList<ArrayList<FeedbackToken>> jsTokenStrings = new ArrayList<ArrayList<FeedbackToken>>();
		// for css
		ArrayList<ArrayList<FeedbackToken>> cssTokenStrings = new ArrayList<ArrayList<FeedbackToken>>();

		// the indexes
		ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();
		ArrayList<HashMap<String, ArrayList<Integer>>> phpTokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();
		ArrayList<HashMap<String, ArrayList<Integer>>> jsTokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();
		ArrayList<HashMap<String, ArrayList<Integer>>> cssTokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();

		// surface indexes
		ArrayList<HashMap<String, ArrayList<Integer>>> surfaceTokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();
		ArrayList<HashMap<String, ArrayList<Integer>>> surfacePhpTokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();
		ArrayList<HashMap<String, ArrayList<Integer>>> surfaceJsTokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();
		ArrayList<HashMap<String, ArrayList<Integer>>> surfaceCssTokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();

		for (int i = 0; i < assignments.length; i++) {
			// for each code, tokenise and generalise
			File code = CodeReader.getCode(assignments[i], "html");
			File codePHP = CodeReader.getCode(assignments[i], "php");

			if (code == null && codePHP == null) {
				tokenStrings.add(new ArrayList<FeedbackToken>());
				phpTokenStrings.add(new ArrayList<FeedbackToken>());
				jsTokenStrings.add(new ArrayList<FeedbackToken>());
				cssTokenStrings.add(new ArrayList<FeedbackToken>());

				// if not RKGST, generate empty index
				if (similarityMeasurement.equalsIgnoreCase("RKRGST") == false) {
					tokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
					phpTokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
					jsTokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
					cssTokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
					if (isSensitive) {
						// if sensitive, add sensitive index
						surfaceTokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
						surfacePhpTokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
						surfaceJsTokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
						surfaceCssTokenIndexes.add(new HashMap<String, ArrayList<Integer>>());
					}
				}
			} else {
				// generate HTML token strings
				ArrayList<FeedbackToken> tokenString = null;
				if (code != null) {
					tokenString = HTMLFeedbackGenerator.generateFeedbackTokenString(code.getAbsolutePath(),
							isMultipleFiles);
					tokenString = HTMLFeedbackGenerator.syntaxTokenStringPreprocessing(tokenString);
					// prepare for deletion
					filesToBeDeleted.add(new File(code.getAbsolutePath() + ".js"));
					filesToBeDeleted.add(new File(code.getAbsolutePath() + ".css"));
				} else {
					tokenString = new ArrayList<>();
				}

				// generate PHP token strings
				ArrayList<FeedbackToken> phpTokenString = null;
				if (codePHP != null) {
					phpTokenString = PHPFeedbackGenerator.generateFeedbackTokenString(codePHP.getAbsolutePath(),
							isMultipleFiles);
					phpTokenString = PHPFeedbackGenerator.syntaxTokenStringPreprocessing(phpTokenString);
					// prepare for deletion
					filesToBeDeleted.add(new File(codePHP.getAbsolutePath() + ".js"));
					filesToBeDeleted.add(new File(codePHP.getAbsolutePath() + ".css"));
				} else {
					phpTokenString = new ArrayList<>();
				}

				File jscode, csscode;

				if (isMultipleFiles) {
					ArrayList<String> excludedJS = new ArrayList<>();
					excludedJS.add(code.getName() + ".js");
					excludedJS.add(codePHP.getName() + ".js");
					jscode = CodeReader.getCodeButNotThese(assignments[i], "js", excludedJS);

					ArrayList<String> excludedCSS = new ArrayList<>();
					excludedCSS.add(code.getName() + ".css");
					excludedCSS.add(codePHP.getName() + ".css");
					csscode = CodeReader.getCodeButNotThese(assignments[i], "css", excludedCSS);

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

					// merge js obtained from php to the merged javascript file and remove the file
					try {
						FileWriter fw = new FileWriter(jscode, true);
						File jsinphp = new File(codePHP.getAbsolutePath() + ".js");
						if (jsinphp.exists()) {
							// take all the content
							Scanner sc = new Scanner(jsinphp);
							while (sc.hasNextLine()) {
								fw.write(sc.nextLine() + System.lineSeparator());
							}
							sc.close();
							fw.close();
							// delete the file
							jsinphp.delete();
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

					// merge css obtained from php to the merged css file and remove the file
					try {
						FileWriter fw = new FileWriter(csscode, true);
						File cssinphp = new File(codePHP.getAbsolutePath() + ".css");
						if (cssinphp.exists()) {
							// take all the content
							Scanner sc = new Scanner(cssinphp);
							while (sc.hasNextLine()) {
								fw.write(sc.nextLine() + System.lineSeparator());
							}
							sc.close();
							fw.close();
							// delete the file
							cssinphp.delete();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				} else {
					jscode = CodeReader.getCode(assignments[i], "js");
					csscode = CodeReader.getCode(assignments[i], "css");
				}

				// generate token strings
				ArrayList<FeedbackToken> jsTokenString = null, cssTokenString = null;

				if (jscode == null) {
					jsTokenString = new ArrayList<FeedbackToken>();
				} else {
					jsTokenString = JSFeedbackGenerator.generateFeedbackTokenString(jscode.getAbsolutePath());
					jsTokenString = JSFeedbackGenerator.syntaxTokenStringPreprocessing(jsTokenString);
				}

				if (csscode == null) {
					cssTokenString = new ArrayList<FeedbackToken>();
				} else {
					cssTokenString = CSSFeedbackGenerator.generateFeedbackTokenString(csscode.getAbsolutePath());
					cssTokenString = CSSFeedbackGenerator.syntaxTokenStringPreprocessing(cssTokenString);
				}

				// add the token string and its raw form
				tokenStrings.add(tokenString);
				phpTokenStrings.add(phpTokenString);
				jsTokenStrings.add(jsTokenString);
				cssTokenStrings.add(cssTokenString);

				// if not RKRGST
				if (similarityMeasurement.equalsIgnoreCase("RKRGST") == false) {
					// generate the index
					HashMap<String, ArrayList<Integer>> tokenIndex = IndexGenerator.generateIndex(tokenString,
							minMatchLength, false);
					HashMap<String, ArrayList<Integer>> phpTokenIndex = IndexGenerator.generateIndex(phpTokenString,
							minMatchLength, false);
					HashMap<String, ArrayList<Integer>> jsTokenIndex = IndexGenerator.generateIndex(jsTokenString,
							minMatchLength, false);
					HashMap<String, ArrayList<Integer>> cssTokenIndex = IndexGenerator.generateIndex(cssTokenString,
							minMatchLength, false);

					// add the token index
					tokenIndexes.add(tokenIndex);
					phpTokenIndexes.add(phpTokenIndex);
					jsTokenIndexes.add(jsTokenIndex);
					cssTokenIndexes.add(cssTokenIndex);

					if (isSensitive) {
						// generate the sensitive index
						HashMap<String, ArrayList<Integer>> surfaceTokenIndex = IndexGenerator
								.generateIndex(tokenString, minMatchLength, true);
						HashMap<String, ArrayList<Integer>> surfacePhpTokenIndex = IndexGenerator
								.generateIndex(phpTokenString, minMatchLength, true);
						HashMap<String, ArrayList<Integer>> surfaceJsTokenIndex = IndexGenerator
								.generateIndex(jsTokenString, minMatchLength, true);
						HashMap<String, ArrayList<Integer>> surfaceCssTokenIndex = IndexGenerator
								.generateIndex(cssTokenString, minMatchLength, true);

						// add the token index
						surfaceTokenIndexes.add(surfaceTokenIndex);
						surfacePhpTokenIndexes.add(surfacePhpTokenIndex);
						surfaceJsTokenIndexes.add(surfaceJsTokenIndex);
						surfaceCssTokenIndexes.add(surfaceCssTokenIndex);
					}
				}
			}
		}

		String progLang = "html";
		if (similarityMeasurement.equalsIgnoreCase("MinHash")) {
			// generate vector header (html, php, js, dan css)
			ArrayList<String> vectorHeader = IndexGenerator.generateVectorHeader(tokenIndexes);
			vectorHeader.addAll(IndexGenerator.generateVectorHeader(phpTokenIndexes));
			vectorHeader.addAll(IndexGenerator.generateVectorHeader(jsTokenIndexes));
			vectorHeader.addAll(IndexGenerator.generateVectorHeader(cssTokenIndexes));

			// calculate minhash
			return _compareAndGenerateHTMLReportsMinHash(assignments, tokenStrings, phpTokenStrings, jsTokenStrings,
					cssTokenStrings, tokenIndexes, phpTokenIndexes, jsTokenIndexes, cssTokenIndexes,
					surfaceTokenIndexes, surfacePhpTokenIndexes, surfaceJsTokenIndexes, surfaceCssTokenIndexes,
					vectorHeader, dirPath, resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs,
					languageCode, numClusters, numStages);
		} else if (similarityMeasurement.equalsIgnoreCase("Super-Bit")) {
			// generate vector header (html, js, dan css)
			ArrayList<String> vectorHeader = IndexGenerator.generateVectorHeader(tokenIndexes);
			vectorHeader.addAll(IndexGenerator.generateVectorHeader(phpTokenIndexes));
			vectorHeader.addAll(IndexGenerator.generateVectorHeader(jsTokenIndexes));
			vectorHeader.addAll(IndexGenerator.generateVectorHeader(cssTokenIndexes));

			// calculate super-bit
			return _compareAndGenerateHTMLReportsSuoerBit(assignments, tokenStrings, phpTokenStrings, jsTokenStrings,
					cssTokenStrings, tokenIndexes, phpTokenIndexes, jsTokenIndexes, cssTokenIndexes,
					surfaceTokenIndexes, surfacePhpTokenIndexes, surfaceJsTokenIndexes, surfaceCssTokenIndexes,
					vectorHeader, dirPath, resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs,
					languageCode, numClusters, numStages);
		} else if (similarityMeasurement.equalsIgnoreCase("Jaccard")) {
			return _compareAndGenerateHTMLReportsJaccard(assignments, tokenStrings, phpTokenStrings, jsTokenStrings,
					cssTokenStrings, tokenIndexes, phpTokenIndexes, jsTokenIndexes, cssTokenIndexes,
					surfaceTokenIndexes, surfacePhpTokenIndexes, surfaceJsTokenIndexes, surfaceCssTokenIndexes, dirPath,
					resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs, languageCode);
		} else if (similarityMeasurement.equalsIgnoreCase("Cosine")) {
			return _compareAndGenerateHTMLReportsCosine(assignments, tokenStrings, phpTokenStrings, jsTokenStrings,
					cssTokenStrings, tokenIndexes, phpTokenIndexes, jsTokenIndexes, cssTokenIndexes,
					surfaceTokenIndexes, surfacePhpTokenIndexes, surfaceJsTokenIndexes, surfaceCssTokenIndexes, dirPath,
					resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs, languageCode);
		} else {
			// RKRGST
			return _compareAndGenerateHTMLReportsRKRGST(assignments, tokenStrings, phpTokenStrings, jsTokenStrings,
					cssTokenStrings, dirPath, resultPath, progLang, humanLang, simThreshold, minMatchLength, maxPairs,
					languageCode, isSensitive);
		}

	}

	private static ArrayList<ComparisonPairTuple> _compareAndGenerateHTMLReportsRKRGST(File[] assignments,
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, ArrayList<ArrayList<FeedbackToken>> phpTokenStrings,
			ArrayList<ArrayList<FeedbackToken>> jsTokenStrings, ArrayList<ArrayList<FeedbackToken>> cssTokenStrings,
			String dirPath, String resultPath, String progLang, String humanLang, int simThreshold, int minMatchLength,
			int maxPairs, String languageCode, boolean isSensitive) {
		// RKRGST
		try {
			// to store the result
			ArrayList<ComparisonPairTuple> codePairs = new ArrayList<>();

			// do the comparison
			for (int j = 0; j < tokenStrings.size(); j++) {
				for (int k = j + 1; k < tokenStrings.size(); k++) {

					// get matched tiles with RKRGST
					ArrayList<GSTMatchTuple> simTuples = FeedbackMessageGenerator
							.generateMatchedTuples(tokenStrings.get(j), tokenStrings.get(k), minMatchLength, false);
					// php
					ArrayList<GSTMatchTuple> phpSimTuples = FeedbackMessageGenerator.generateMatchedTuples(
							phpTokenStrings.get(j), phpTokenStrings.get(k), minMatchLength, false);
					// javascript
					ArrayList<GSTMatchTuple> jsSimTuples = FeedbackMessageGenerator
							.generateMatchedTuples(jsTokenStrings.get(j), jsTokenStrings.get(k), minMatchLength, false);
					// CSS
					ArrayList<GSTMatchTuple> cssSimTuples = FeedbackMessageGenerator.generateMatchedTuples(
							cssTokenStrings.get(j), cssTokenStrings.get(k), minMatchLength, false);

					// get the sim degree for RKRGST
					int simDegree = (int) (((double) (2
							* (MatchGenerator.coverage(simTuples) + MatchGenerator.coverage(phpSimTuples)
									+ MatchGenerator.coverage(jsSimTuples) + MatchGenerator.coverage(cssSimTuples)))
							/ (double) (tokenStrings.get(j).size() + phpTokenStrings.get(j).size()
									+ jsTokenStrings.get(j).size() + cssTokenStrings.get(j).size()
									+ tokenStrings.get(k).size() + phpTokenStrings.get(k).size()
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

						// calculate surface sim if needed
						int surfaceSimDegree = -1;
						if (isSensitive) {
							// get surface matched tiles with RKRGST
							ArrayList<GSTMatchTuple> surfaceSimTuples = FeedbackMessageGenerator.generateMatchedTuples(
									tokenStrings.get(j), tokenStrings.get(k), minMatchLength, true);
							// surface php
							ArrayList<GSTMatchTuple> surfacePhpSimTuples = FeedbackMessageGenerator
									.generateMatchedTuples(phpTokenStrings.get(j), phpTokenStrings.get(k),
											minMatchLength, true);
							// surface javascript
							ArrayList<GSTMatchTuple> surfaceJsSimTuples = FeedbackMessageGenerator
									.generateMatchedTuples(jsTokenStrings.get(j), jsTokenStrings.get(k), minMatchLength,
											true);
							// surface CSS
							ArrayList<GSTMatchTuple> surfaceCssSimTuples = FeedbackMessageGenerator
									.generateMatchedTuples(cssTokenStrings.get(j), cssTokenStrings.get(k),
											minMatchLength, true);

							// get the surface sim degree for RKRGST
							surfaceSimDegree = (int) (((double) (2 * (MatchGenerator.coverage(surfaceSimTuples)
									+ MatchGenerator.coverage(surfacePhpSimTuples)
									+ MatchGenerator.coverage(surfaceJsSimTuples)
									+ MatchGenerator.coverage(surfaceCssSimTuples)))
									/ (double) (tokenStrings.get(j).size() + phpTokenStrings.get(j).size()
											+ jsTokenStrings.get(j).size() + cssTokenStrings.get(j).size()
											+ tokenStrings.get(k).size() + phpTokenStrings.get(k).size()
											+ jsTokenStrings.get(k).size() + cssTokenStrings.get(k).size()))
									* 100);
						}

						// add the comparison pair
						codePairs.add(new ComparisonPairTupleWeb(j, k, dirname1, dirname2, simDegree, surfaceSimDegree,
								1, simTuples, phpSimTuples, jsSimTuples, cssSimTuples));
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
				File phpCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "php");
				File phpCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "php");
				File jsCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "js");
				File jsCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "js");
				File cssCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "css");
				File cssCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "css");

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				HTMLHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
						phpCode1.getAbsolutePath(), phpCode2.getAbsolutePath(), jsCode1.getAbsolutePath(),
						jsCode2.getAbsolutePath(), cssCode1.getAbsolutePath(), cssCode2.getAbsolutePath(),
						tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
						phpTokenStrings.get(ct.getSubmissionID1()), phpTokenStrings.get(ct.getSubmissionID2()),
						jsTokenStrings.get(ct.getSubmissionID1()), jsTokenStrings.get(ct.getSubmissionID2()),
						cssTokenStrings.get(ct.getSubmissionID1()), cssTokenStrings.get(ct.getSubmissionID2()),
						ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairWebTemplatePath,
						syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang, ct.getMatches(),
						ct.getPhpMatches(), ct.getJsMatches(), ct.getCssMatches());
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
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, ArrayList<ArrayList<FeedbackToken>> phpTokenStrings,
			ArrayList<ArrayList<FeedbackToken>> jsTokenStrings, ArrayList<ArrayList<FeedbackToken>> cssTokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> phpTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> jsTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> cssTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfacePhpTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceJsTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceCssTokenIndexes, String dirPath, String resultPath,
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

					// add all html, php, js, and cs for token index 1
					tokenIndex1.putAll(tokenIndexes.get(j));
					tokenIndex1.putAll(phpTokenIndexes.get(j));
					tokenIndex1.putAll(jsTokenIndexes.get(j));
					tokenIndex1.putAll(cssTokenIndexes.get(j));

					// add all html, php, js, and cs for token index 2
					tokenIndex2.putAll(tokenIndexes.get(k));
					tokenIndex2.putAll(phpTokenIndexes.get(k));
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

						// if the surface index is not empty, then calculate surface sim
						double surfaceSimDegree = -1;
						if (surfaceTokenIndexes.size() > 0) {
							// generate the matches
							ArrayList<GSTMatchTuple> surfaceMatches = MatchGenerator.generateMatches(
									surfaceTokenIndexes.get(j), surfaceTokenIndexes.get(k), minMatchLength);
							ArrayList<GSTMatchTuple> surfacePhpMatches = MatchGenerator.generateMatches(
									surfacePhpTokenIndexes.get(j), surfacePhpTokenIndexes.get(k), minMatchLength);
							ArrayList<GSTMatchTuple> surfaceJsmatches = MatchGenerator.generateMatches(
									surfaceJsTokenIndexes.get(j), surfaceJsTokenIndexes.get(k), minMatchLength);
							ArrayList<GSTMatchTuple> surfaceCssmatches = MatchGenerator.generateMatches(
									surfaceCssTokenIndexes.get(j), surfaceCssTokenIndexes.get(k), minMatchLength);

							// get the sim degree
							surfaceSimDegree = (int) (((double) (2 * (MatchGenerator.coverage(surfaceMatches)
									+ MatchGenerator.coverage(surfacePhpMatches)
									+ MatchGenerator.coverage(surfaceJsmatches)
									+ MatchGenerator.coverage(surfaceCssmatches)))
									/ (double) (tokenStrings.get(j).size() + phpTokenStrings.get(j).size()
											+ jsTokenStrings.get(j).size() + cssTokenStrings.get(j).size()
											+ tokenStrings.get(k).size() + phpTokenStrings.get(k).size()
											+ jsTokenStrings.get(k).size() + cssTokenStrings.get(k).size()))
									* 100);
						}

						// generate the matches
						ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndexes.get(j),
								tokenIndexes.get(k), minMatchLength);
						ArrayList<GSTMatchTuple> phpmatches = MatchGenerator.generateMatches(phpTokenIndexes.get(j),
								phpTokenIndexes.get(k), minMatchLength);
						ArrayList<GSTMatchTuple> jsMatches = MatchGenerator.generateMatches(jsTokenIndexes.get(j),
								jsTokenIndexes.get(k), minMatchLength);
						ArrayList<GSTMatchTuple> cssMatches = MatchGenerator.generateMatches(cssTokenIndexes.get(j),
								cssTokenIndexes.get(k), minMatchLength);

						// add the comparison pair
						codePairs.add(new ComparisonPairTupleWeb(j, k, dirname1, dirname2, simDegree, surfaceSimDegree,
								1, matches, phpmatches, jsMatches, cssMatches));
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
				File phpCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "php");
				File phpCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "php");
				File jsCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "js");
				File jsCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "js");
				File cssCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "css");
				File cssCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "css");

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				HTMLHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
						phpCode1.getAbsolutePath(), phpCode2.getAbsolutePath(), jsCode1.getAbsolutePath(),
						jsCode2.getAbsolutePath(), cssCode1.getAbsolutePath(), cssCode2.getAbsolutePath(),
						tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
						phpTokenStrings.get(ct.getSubmissionID1()), phpTokenStrings.get(ct.getSubmissionID2()),
						jsTokenStrings.get(ct.getSubmissionID1()), jsTokenStrings.get(ct.getSubmissionID2()),
						cssTokenStrings.get(ct.getSubmissionID1()), cssTokenStrings.get(ct.getSubmissionID2()),
						ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairWebTemplatePath,
						syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang, ct.getMatches(),
						ct.getPhpMatches(), ct.getJsMatches(), ct.getCssMatches());
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
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, ArrayList<ArrayList<FeedbackToken>> phpTokenStrings,
			ArrayList<ArrayList<FeedbackToken>> jsTokenStrings, ArrayList<ArrayList<FeedbackToken>> cssTokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> phpTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> jsTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> cssTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfacePhpTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceJsTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceCssTokenIndexes, String dirPath, String resultPath,
			String progLang, String humanLang, int simThreshold, int minMatchLength, int maxPairs,
			String languageCode) {
		// Cosine
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
					tokenIndex1.putAll(phpTokenIndexes.get(j));
					tokenIndex1.putAll(jsTokenIndexes.get(j));
					tokenIndex1.putAll(cssTokenIndexes.get(j));

					// add all html, js, and cs for token index 2
					tokenIndex2.putAll(tokenIndexes.get(k));
					tokenIndex2.putAll(phpTokenIndexes.get(k));
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

						// if the surface index is not empty, then calculate surface sim
						double surfaceSimDegree = -1;
						if (surfaceTokenIndexes.size() > 0) {
							// generate the matches
							ArrayList<GSTMatchTuple> surfaceMatches = MatchGenerator.generateMatches(
									surfaceTokenIndexes.get(j), surfaceTokenIndexes.get(k), minMatchLength);
							ArrayList<GSTMatchTuple> surfacePhpMatches = MatchGenerator.generateMatches(
									surfacePhpTokenIndexes.get(j), surfacePhpTokenIndexes.get(k), minMatchLength);
							ArrayList<GSTMatchTuple> surfaceJsmatches = MatchGenerator.generateMatches(
									surfaceJsTokenIndexes.get(j), surfaceJsTokenIndexes.get(k), minMatchLength);
							ArrayList<GSTMatchTuple> surfaceCssmatches = MatchGenerator.generateMatches(
									surfaceCssTokenIndexes.get(j), surfaceCssTokenIndexes.get(k), minMatchLength);

							// get the sim degree
							surfaceSimDegree = (int) (((double) (2 * (MatchGenerator.coverage(surfaceMatches)
									+ MatchGenerator.coverage(surfacePhpMatches)
									+ MatchGenerator.coverage(surfaceJsmatches)
									+ MatchGenerator.coverage(surfaceCssmatches)))
									/ (double) (tokenStrings.get(j).size() + phpTokenStrings.get(j).size()
											+ jsTokenStrings.get(j).size() + cssTokenStrings.get(j).size()
											+ tokenStrings.get(k).size() + phpTokenStrings.get(k).size()
											+ jsTokenStrings.get(k).size() + cssTokenStrings.get(k).size()))
									* 100);
						}

						// generate the matches
						ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndexes.get(j),
								tokenIndexes.get(k), minMatchLength);
						ArrayList<GSTMatchTuple> phpMatches = MatchGenerator.generateMatches(phpTokenIndexes.get(j),
								phpTokenIndexes.get(k), minMatchLength);
						ArrayList<GSTMatchTuple> jsMatches = MatchGenerator.generateMatches(jsTokenIndexes.get(j),
								jsTokenIndexes.get(k), minMatchLength);
						ArrayList<GSTMatchTuple> cssMatches = MatchGenerator.generateMatches(cssTokenIndexes.get(j),
								cssTokenIndexes.get(k), minMatchLength);

						// add the comparison pair
						codePairs.add(new ComparisonPairTupleWeb(j, k, dirname1, dirname2, simDegree, surfaceSimDegree,
								1, matches, phpMatches, jsMatches, cssMatches));
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
				File phpCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "php");
				File phpCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "php");
				File jsCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "js");
				File jsCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "js");
				File cssCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "css");
				File cssCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "css");

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				HTMLHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
						phpCode1.getAbsolutePath(), phpCode2.getAbsolutePath(), jsCode1.getAbsolutePath(),
						jsCode2.getAbsolutePath(), cssCode1.getAbsolutePath(), cssCode2.getAbsolutePath(),
						tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
						phpTokenStrings.get(ct.getSubmissionID1()), phpTokenStrings.get(ct.getSubmissionID2()),
						jsTokenStrings.get(ct.getSubmissionID1()), jsTokenStrings.get(ct.getSubmissionID2()),
						cssTokenStrings.get(ct.getSubmissionID1()), cssTokenStrings.get(ct.getSubmissionID2()),
						ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairWebTemplatePath,
						syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang, ct.getMatches(), ct.getPhpMatches(),
						ct.getJsMatches(), ct.getCssMatches());
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
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, ArrayList<ArrayList<FeedbackToken>> phpTokenStrings,
			ArrayList<ArrayList<FeedbackToken>> jsTokenStrings, ArrayList<ArrayList<FeedbackToken>> cssTokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> phpTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> jsTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> cssTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfacePhpTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceJsTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceCssTokenIndexes, ArrayList<String> vectorHeader,
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
				v = JaccardCalculator.updateBooleanVectorFromTokenString(v, phpTokenIndexes.get(i), vectorHeader);
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
				ArrayList<GSTMatchTuple> phpmatches = MatchGenerator.generateMatches(phpTokenIndexes.get(submissionID1),
						phpTokenIndexes.get(submissionID2), minMatchLength);
				ArrayList<GSTMatchTuple> jsmatches = MatchGenerator.generateMatches(jsTokenIndexes.get(submissionID1),
						jsTokenIndexes.get(submissionID2), minMatchLength);
				ArrayList<GSTMatchTuple> cssmatches = MatchGenerator.generateMatches(cssTokenIndexes.get(submissionID1),
						cssTokenIndexes.get(submissionID2), minMatchLength);

				// get the sim degree
				int simDegree = (int) (((double) (2
						* (MatchGenerator.coverage(matches) + MatchGenerator.coverage(phpmatches)
								+ MatchGenerator.coverage(jsmatches) + MatchGenerator.coverage(cssmatches)))
						/ (double) (tokenStrings.get(submissionID1).size() + phpTokenStrings.get(submissionID1).size()
								+ jsTokenStrings.get(submissionID1).size() + cssTokenStrings.get(submissionID1).size()
								+ tokenStrings.get(submissionID2).size() + phpTokenStrings.get(submissionID2).size()
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

					// if the surface index is not empty, then calculate surface sim
					double surfaceSimDegree = -1;
					if (surfaceTokenIndexes.size() > 0) {
						// generate the matches
						ArrayList<GSTMatchTuple> surfaceMatches = MatchGenerator.generateMatches(
								surfaceTokenIndexes.get(submissionID1), surfaceTokenIndexes.get(submissionID2),
								minMatchLength);
						ArrayList<GSTMatchTuple> surfacePhpMatches = MatchGenerator.generateMatches(
								surfacePhpTokenIndexes.get(submissionID1), surfacePhpTokenIndexes.get(submissionID2),
								minMatchLength);
						ArrayList<GSTMatchTuple> surfaceJsmatches = MatchGenerator.generateMatches(
								surfaceJsTokenIndexes.get(submissionID1), surfaceJsTokenIndexes.get(submissionID2),
								minMatchLength);
						ArrayList<GSTMatchTuple> surfaceCssmatches = MatchGenerator.generateMatches(
								surfaceCssTokenIndexes.get(submissionID1), surfaceCssTokenIndexes.get(submissionID2),
								minMatchLength);

						// get the sim degree
						surfaceSimDegree = (int) (((double) (2 * (MatchGenerator.coverage(surfaceMatches)
								+ MatchGenerator.coverage(surfacePhpMatches) + MatchGenerator.coverage(surfaceJsmatches)
								+ MatchGenerator.coverage(surfaceCssmatches)))
								/ (double) (tokenStrings.get(submissionID1).size()
										+ phpTokenStrings.get(submissionID1).size()
										+ jsTokenStrings.get(submissionID1).size()
										+ cssTokenStrings.get(submissionID1).size()
										+ tokenStrings.get(submissionID2).size()
										+ phpTokenStrings.get(submissionID2).size()
										+ jsTokenStrings.get(submissionID2).size()
										+ cssTokenStrings.get(submissionID2).size()))
								* 100);
					}
					// add the comparison pair
					ComparisonPairTuple ct = new ComparisonPairTupleWeb(submissionID1, submissionID2, dirname1,
							dirname2, simDegree, surfaceSimDegree, en.getValue(), matches, phpmatches, jsmatches,
							cssmatches);
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
				File phpCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "php");
				File phpCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "php");
				File jsCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "js");
				File jsCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "js");
				File cssCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "css");
				File cssCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "css");

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				HTMLHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
						phpCode1.getAbsolutePath(), phpCode2.getAbsolutePath(), jsCode1.getAbsolutePath(),
						jsCode2.getAbsolutePath(), cssCode1.getAbsolutePath(), cssCode2.getAbsolutePath(),
						tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
						phpTokenStrings.get(ct.getSubmissionID1()), phpTokenStrings.get(ct.getSubmissionID2()),
						jsTokenStrings.get(ct.getSubmissionID1()), jsTokenStrings.get(ct.getSubmissionID2()),
						cssTokenStrings.get(ct.getSubmissionID1()), cssTokenStrings.get(ct.getSubmissionID2()),
						ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairWebTemplatePath,
						syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang, ct.getMatches(),
						ct.getPhpMatches(), ct.getJsMatches(), ct.getCssMatches());
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
			ArrayList<ArrayList<FeedbackToken>> tokenStrings, ArrayList<ArrayList<FeedbackToken>> phpTokenStrings,
			ArrayList<ArrayList<FeedbackToken>> jsTokenStrings, ArrayList<ArrayList<FeedbackToken>> cssTokenStrings,
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> phpTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> jsTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> cssTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfacePhpTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceJsTokenIndexes,
			ArrayList<HashMap<String, ArrayList<Integer>>> surfaceCssTokenIndexes, ArrayList<String> vectorHeader,
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
				v = CosineCalculator.updateOccurrenceVectorFromTokenString(v, phpTokenIndexes.get(i), vectorHeader);
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
				ArrayList<GSTMatchTuple> phpmatches = MatchGenerator.generateMatches(phpTokenIndexes.get(submissionID1),
						phpTokenIndexes.get(submissionID2), minMatchLength);
				ArrayList<GSTMatchTuple> jsmatches = MatchGenerator.generateMatches(jsTokenIndexes.get(submissionID1),
						jsTokenIndexes.get(submissionID2), minMatchLength);
				ArrayList<GSTMatchTuple> cssmatches = MatchGenerator.generateMatches(cssTokenIndexes.get(submissionID1),
						cssTokenIndexes.get(submissionID2), minMatchLength);

				// get the sim degree
				int simDegree = (int) (((double) (2
						* (MatchGenerator.coverage(matches) + MatchGenerator.coverage(phpmatches)
								+ MatchGenerator.coverage(jsmatches) + MatchGenerator.coverage(cssmatches)))
						/ (double) (tokenStrings.get(submissionID1).size() + phpTokenStrings.get(submissionID1).size()
								+ jsTokenStrings.get(submissionID1).size() + cssTokenStrings.get(submissionID1).size()
								+ tokenStrings.get(submissionID2).size() + phpTokenStrings.get(submissionID2).size()
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

					// if the surface index is not empty, then calculate surface sim
					double surfaceSimDegree = -1;
					if (surfaceTokenIndexes.size() > 0) {
						// generate the matches
						ArrayList<GSTMatchTuple> surfaceMatches = MatchGenerator.generateMatches(
								surfaceTokenIndexes.get(submissionID1), surfaceTokenIndexes.get(submissionID2),
								minMatchLength);
						ArrayList<GSTMatchTuple> surfacePhpMatches = MatchGenerator.generateMatches(
								surfacePhpTokenIndexes.get(submissionID1), surfacePhpTokenIndexes.get(submissionID2),
								minMatchLength);
						ArrayList<GSTMatchTuple> surfaceJsmatches = MatchGenerator.generateMatches(
								surfaceJsTokenIndexes.get(submissionID1), surfaceJsTokenIndexes.get(submissionID2),
								minMatchLength);
						ArrayList<GSTMatchTuple> surfaceCssmatches = MatchGenerator.generateMatches(
								surfaceCssTokenIndexes.get(submissionID1), surfaceCssTokenIndexes.get(submissionID2),
								minMatchLength);

						// get the sim degree
						surfaceSimDegree = (int) (((double) (2 * (MatchGenerator.coverage(surfaceMatches)
								+ MatchGenerator.coverage(surfacePhpMatches) + MatchGenerator.coverage(surfaceJsmatches)
								+ MatchGenerator.coverage(surfaceCssmatches)))
								/ (double) (tokenStrings.get(submissionID1).size()
										+ phpTokenStrings.get(submissionID1).size()
										+ jsTokenStrings.get(submissionID1).size()
										+ cssTokenStrings.get(submissionID1).size()
										+ tokenStrings.get(submissionID2).size()
										+ phpTokenStrings.get(submissionID2).size()
										+ jsTokenStrings.get(submissionID2).size()
										+ cssTokenStrings.get(submissionID2).size()))
								* 100);
					}

					// add the comparison pair
					ComparisonPairTuple ct = new ComparisonPairTupleWeb(submissionID1, submissionID2, dirname1,
							dirname2, simDegree, surfaceSimDegree, en.getValue(), matches, phpmatches, jsmatches,
							cssmatches);
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
				File phpCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "php");
				File phpCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "php");
				File jsCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "js");
				File jsCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "js");
				File cssCode1 = CodeReader.getCode(assignments[ct.getSubmissionID1()], "css");
				File cssCode2 = CodeReader.getCode(assignments[ct.getSubmissionID2()], "css");

				// set the path to comparison pair tuple
				ct.setResultedHTMLFilename(syntacticFilename);

				// need to be fixed
				HTMLHtmlGenerator.generateHtmlForSSTRANGE(code1.getAbsolutePath(), code2.getAbsolutePath(),
						phpCode1.getAbsolutePath(), phpCode2.getAbsolutePath(), jsCode1.getAbsolutePath(),
						jsCode2.getAbsolutePath(), cssCode1.getAbsolutePath(), cssCode2.getAbsolutePath(),
						tokenStrings.get(ct.getSubmissionID1()), tokenStrings.get(ct.getSubmissionID2()),
						phpTokenStrings.get(ct.getSubmissionID1()), phpTokenStrings.get(ct.getSubmissionID2()),
						jsTokenStrings.get(ct.getSubmissionID1()), jsTokenStrings.get(ct.getSubmissionID2()),
						cssTokenStrings.get(ct.getSubmissionID1()), cssTokenStrings.get(ct.getSubmissionID2()),
						ct.getAssignmentName1(), ct.getAssignmentName2(), MainFrame.pairWebTemplatePath,
						syntacticFilepath, minMatchLength, ct.getSameClusterOccurrences(), humanLang, ct.getMatches(),
						ct.getPhpMatches(), ct.getJsMatches(), ct.getCssMatches());
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

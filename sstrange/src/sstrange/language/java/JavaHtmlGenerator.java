package sstrange.language.java;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;

import sstrange.htmlgenerator.Code1FeedbackComparator;
import sstrange.htmlgenerator.Code2FeedbackComparator;
import sstrange.htmlgenerator.HtmlTableStringGenerator;
import sstrange.message.FeedbackMessage;
import sstrange.message.FeedbackMessageGenerator;
import sstrange.message.SyntaxFeedbackMessage;
import sstrange.token.FeedbackToken;
import support.stringmatching.GSTMatchTuple;

public class JavaHtmlGenerator {
	public static void generateHtmlForSSTRANGE(String codepath1, String codepath2,
			ArrayList<FeedbackToken> tokenString1, ArrayList<FeedbackToken> tokenString2,
			ArrayList<FeedbackToken> rawTokenString1, ArrayList<FeedbackToken> rawTokenString2, String dirname1,
			String dirname2, String templateHTMLPath, String outputHTMLPath, int minimumMatchLength,
			int sameClusterOccurrences, String languageCode, ArrayList<ArrayList<String>> additionalKeywords,
			ArrayList<GSTMatchTuple> matches) throws Exception {

		// set to global variables
		gRawTokenString1 = rawTokenString1;
		gRawTokenString2 = rawTokenString2;

		// get syntax reordering messages
		gSyntaxMessage = FeedbackMessageGenerator.generateSimilarityMessages(tokenString1, tokenString2,
				gRawTokenString1, gRawTokenString2, minimumMatchLength, languageCode, matches);

		String[] defaultInfoStrings = generateInformationStrings(codepath1, codepath2, "origtablecontent", "",
				languageCode);

		/*
		 * for code without comments. The preprocessing is not required anymore as it
		 * uses the same data as code with comments.
		 */
		String[] formattedInfoStringsWithoutComments = generateInformationStrings(codepath1, codepath2,
				"syontablecontent", "syon", languageCode);

		File templateFile = new File(templateHTMLPath);
		File outputFile = new File(outputHTMLPath);
		BufferedReader fr = new BufferedReader(new FileReader(templateFile));
		BufferedWriter fw = new BufferedWriter(new FileWriter(outputFile));
		String line;
		while ((line = fr.readLine()) != null) {
			if (line.contains("@codepath1")) {
				line = line.replace("@codepath1", dirname1);
			}
			if (line.contains("@codepath2")) {
				line = line.replace("@codepath2", dirname2);
			}

			// for default data
			if (line.contains("@code1")) {
				line = line.replace("@code1", defaultInfoStrings[0]);
			}
			if (line.contains("@code2")) {
				line = line.replace("@code2", defaultInfoStrings[1]);
			}
			if (line.contains("@tablecontent")) {
				line = line.replace("@tablecontent", defaultInfoStrings[2]);
			}
			if (line.contains("@explanation")) {
				line = line.replace("@explanation", defaultInfoStrings[3]);
			}

			// for formatted code without comments
			if (line.contains("@syoncode1")) {
				line = line.replace("@syoncode1", formattedInfoStringsWithoutComments[0]);
			}
			if (line.contains("@syoncode2")) {
				line = line.replace("@syoncode2", formattedInfoStringsWithoutComments[1]);
			}
			if (line.contains("@syontablecontent")) {
				line = line.replace("@syontablecontent", formattedInfoStringsWithoutComments[2]);
			}
			if (line.contains("@syonexplanation")) {
				line = line.replace("@syonexplanation", formattedInfoStringsWithoutComments[3]);
			}

			fw.write(line);
			fw.write(System.lineSeparator());
		}
		fr.close();
		fw.close();
	}

	private static ArrayList<FeedbackToken> gRawTokenString1, gRawTokenString2;
	private static ArrayList<FeedbackMessage> gSyntaxMessage;

	private static String[] generateInformationStrings(String filepath1, String filepath2, String tableId, String mode,
			String humanLanguageId) throws Exception {
		// get the html strings
		String code1, code2;

		// sort syntax messages
		Collections.sort(gSyntaxMessage);
		// set visual ID for syntax message
		for (int i = 0; i < gSyntaxMessage.size(); i++) {
			FeedbackMessage fm = gSyntaxMessage.get(i);
			fm.setVisualId("s" + mode + (i + 1));
		}

		ArrayList<FeedbackToken> commentString1 = new ArrayList<>();
		for (int i = 0; i < gRawTokenString1.size(); i++) {
			if (gRawTokenString1.get(i).getType().equals("COMMENT"))
				commentString1.add(gRawTokenString1.get(i));
		}

		ArrayList<FeedbackToken> commentString2 = new ArrayList<>();
		for (int i = 0; i < gRawTokenString2.size(); i++) {
			if (gRawTokenString2.get(i).getType().equals("COMMENT"))
				commentString2.add(gRawTokenString2.get(i));
		}

		if (mode.equals("syon")) {
			code1 = readCode1WithoutCommentWhiteSpace(filepath1, gSyntaxMessage, commentString1, tableId);
			code2 = readCode2WithoutCommentWhiteSpace(filepath2, gSyntaxMessage, commentString2, tableId);
		} else {
			code1 = readCode1(filepath1, tableId, commentString1);
			code2 = readCode2(filepath2, tableId, commentString2);
		}

		// get table contents
		ArrayList<FeedbackMessage> messages = new ArrayList<>();
		messages.addAll(gSyntaxMessage);
		String tableContent = HtmlTableStringGenerator.getTableContentForMatchesForSSTRANGE(messages, tableId,
				humanLanguageId);

		// get natural language explanation content
		String explanationContent = getExplanationContent(gSyntaxMessage, mode);

		return new String[] { code1, code2, tableContent, explanationContent };
	}

	/*
	 * these two methods generate strings representing the code files with some tags
	 * embedded. It applies a little about deterministic finite automata.
	 */
	private static String readCode1(String filepath, String tableId, ArrayList<FeedbackToken> commentString)
			throws Exception {
		// for source and target id on code
		char sourceId = 'a';
		char targetId = 'b';
		// sort all list based on tow then col on code 1
		Collections.sort(gSyntaxMessage, new Code1FeedbackComparator());
		Collections.sort(commentString);

		// embedding comment tags
		String code = "";
		BufferedReader fr = new BufferedReader(new FileReader(filepath));
		String line;
		int row = 1;
		// the first position from syntax message list
		int syntaxMessageIndex = 0;
		// the first position on comment list
		int commentIndex = 0;
		// mark that it is not in comment
		boolean isInComment = false;
		// refers to the ID that will be written
		String syntaxMessageId = null;

		// refers to following rows for a particular match
		int syntaxCounter = 1;
		while ((line = fr.readLine()) != null) {
			// to mark whether </a> tag is required at the end of line
			boolean closeTagRequired = false;
			for (int col = 0; col < line.length(); col++) {
				String c = HTMLSafeStringFormat(line.charAt(col));

				if (commentIndex < commentString.size() && row == commentString.get(commentIndex).getStartRow()
						&& col == commentString.get(commentIndex).getStartCol()) {
					// if the position matches with comment message index but
					// not on matches, start to mark

					// close previous tag if any
					if (closeTagRequired) {
						/*
						 * This mechanism to guarantee no whitespaces between syntax and comment are
						 * highlighted. Quite messy.
						 */
						// get the trimmed version of the code
						String codeTemp = code.trim();
						// get last non-whitespace char
						char lastChar = codeTemp.charAt(codeTemp.length() - 1);
						// get the position of the last non-whitespace char
						int indexOfLastNonWhitespace = code.lastIndexOf(lastChar);
						// put the closing tag after that last char
						code = code.substring(0, indexOfLastNonWhitespace + 1) + "</a>"
								+ code.substring(indexOfLastNonWhitespace + 1);
						// set no need for close tag
						closeTagRequired = false;
					}

					// mark that it is in comment
					isInComment = true;
					// add the char
					code += c;
				} else if (commentIndex < commentString.size() && row == commentString.get(commentIndex).getFinishRow()
						&& col == commentString.get(commentIndex).getFinishCol() - 1) {
					// if the end is found, mark that it is not comment anymore
					isInComment = false;
					commentIndex++;
					// add the char
					code += c;
				} else if (syntaxMessageIndex < gSyntaxMessage.size()
						&& row == gSyntaxMessage.get(syntaxMessageIndex).getStartRowCode1()
						&& col == gSyntaxMessage.get(syntaxMessageIndex).getStartColCode1()) {
					// set the syntax ID
					syntaxMessageId = gSyntaxMessage.get(syntaxMessageIndex).getVisualId();
					// prepare the beginning tag
					String syntaxTagHeader = "<a class='syntaxsim' id='" + syntaxMessageId + sourceId + "' href=\"#"
							+ syntaxMessageId + targetId + "\" onclick=\"markSelected('" + syntaxMessageId + "','"
							+ tableId + "')\" >";
					// put the header tag along with current character
					code += (syntaxTagHeader + c);
					// inform that </a> needed to close
					closeTagRequired = true;
				} else if (syntaxMessageIndex < gSyntaxMessage.size()
						&& row == gSyntaxMessage.get(syntaxMessageIndex).getFinishRowCode1()
						&& col == gSyntaxMessage.get(syntaxMessageIndex).getFinishColCode1() - 1) {
					// if the end is found

					if (closeTagRequired == false) {
						/*
						 * if the beginning has not given any link open tag, add it
						 */
						String syntaxTagHeader = "<a class='syntaxsim' id='" + syntaxMessageId + sourceId
								+ syntaxCounter + "' href=\"#" + syntaxMessageId + targetId
								+ "\" onclick=\"markSelected('" + syntaxMessageId + "','" + tableId + "')\" >";
						code += syntaxTagHeader;
						syntaxCounter++;
					}

					// add the end
					code += (c + "</a>");
					// set null
					syntaxMessageId = null;
					// increment syntax index
					syntaxMessageIndex++;
					// set syntax counter to 1
					syntaxCounter = 1;
					// inform that no </a> needed to close
					closeTagRequired = false;
				} else {
					// this to re-highlight new line if it is still a part of
					// matched tokens
					if (syntaxMessageId != null) {
						if (closeTagRequired == false) {
							if (c.trim().length() > 0 && isInComment == false) {
								String syntaxTagHeader = "<a class='syntaxsim' id='" + syntaxMessageId + sourceId
										+ syntaxCounter + "' href=\"#" + syntaxMessageId + targetId
										+ "\" onclick=\"markSelected('" + syntaxMessageId + "','" + tableId + "')\" >";
								code += syntaxTagHeader;
								closeTagRequired = true;
								syntaxCounter++;
							}
						}
						code += c;
					} else {
						code += c;
					}
				}
			}
			if (closeTagRequired == true) {
				/*
				 * This mechanism to guarantee no remaining whitespaces at the end of the line
				 * are highlighted.
				 */
				// get the trimmed version of the code
				String codeTemp = code.trim();
				// get last non-whitespace char
				char lastChar = codeTemp.charAt(codeTemp.length() - 1);
				// get the position of the last non-whitespace char
				int indexOfLastNonWhitespace = code.lastIndexOf(lastChar);
				// put the closing tag after that last char
				code = code.substring(0, indexOfLastNonWhitespace + 1) + "</a>"
						+ code.substring(indexOfLastNonWhitespace + 1) + System.lineSeparator();
			} else {
				code += System.lineSeparator();
			}
			row++;
		}
		fr.close();
		return code;
	}

	private static String readCode2(String filepath, String tableId, ArrayList<FeedbackToken> commentString)
			throws Exception {
		/*
		 * this function cannot be merged to readCode1 since all get start and finish
		 * pos methods are different.
		 */
		// for source and target id on code
		char sourceId = 'b';
		char targetId = 'a';
		// sort all list based on tow then col on code 1
		Collections.sort(gSyntaxMessage, new Code2FeedbackComparator());
		Collections.sort(commentString);

		// embedding comment tags
		String code = "";
		BufferedReader fr = new BufferedReader(new FileReader(filepath));
		String line;
		int row = 1;
		// the first position from syntax list
		int syntaxMessageIndex = 0;
		// the first position on comment list
		int commentIndex = 0;
		// mark that it is not in comment
		boolean isInComment = false;
		// refers to the ID that will be written
		String syntaxMessageId = null;

		// refers to following rows for a particular match
		int syntaxCounter = 1;
		while ((line = fr.readLine()) != null) {
			// to mark whether </a> tag is required at the end of line
			boolean closeTagRequired = false;
			for (int col = 0; col < line.length(); col++) {
				String c = HTMLSafeStringFormat(line.charAt(col));
				if (commentIndex < commentString.size() && row == commentString.get(commentIndex).getStartRow()
						&& col == commentString.get(commentIndex).getStartCol()) {
					// if the position matches with comment message index but
					// not on matched message, start to mark

					// close previous tag if any
					if (closeTagRequired) {
						/*
						 * This mechanism to guarantee no whitespaces between syntax and comment are
						 * highlighted. Quite messy.
						 */
						// get the trimmed version of the code
						String codeTemp = code.trim();
						// get last non-whitespace char
						char lastChar = codeTemp.charAt(codeTemp.length() - 1);
						// get the position of the last non-whitespace char
						int indexOfLastNonWhitespace = code.lastIndexOf(lastChar);
						// put the closing tag after that last char
						code = code.substring(0, indexOfLastNonWhitespace + 1) + "</a>"
								+ code.substring(indexOfLastNonWhitespace + 1);
						// set no need for close tag
						closeTagRequired = false;
					}

					// mark that it is in comment
					isInComment = true;
					// add the char
					code += c;
				} else if (commentIndex < commentString.size() && row == commentString.get(commentIndex).getFinishRow()
						&& col == commentString.get(commentIndex).getFinishCol() - 1) {
					// if the end is found, mark that it is not comment anymore
					isInComment = false;
					commentIndex++;
					// add the char
					code += c;
				} else if (syntaxMessageIndex < gSyntaxMessage.size()
						&& row == gSyntaxMessage.get(syntaxMessageIndex).getStartRowCode2()
						&& col == gSyntaxMessage.get(syntaxMessageIndex).getStartColCode2()) {
					// set the syntax ID
					syntaxMessageId = gSyntaxMessage.get(syntaxMessageIndex).getVisualId();
					// prepare the beginning tag
					String syntaxTagHeader = "<a class='syntaxsim' id='" + syntaxMessageId + sourceId + "' href=\"#"
							+ syntaxMessageId + targetId + "\" onclick=\"markSelected('" + syntaxMessageId + "','"
							+ tableId + "')\" >";
					// put the header tag along with current character
					code += (syntaxTagHeader + c);
					// inform that </a> needed to close
					closeTagRequired = true;
				} else if (syntaxMessageIndex < gSyntaxMessage.size()
						&& row == gSyntaxMessage.get(syntaxMessageIndex).getFinishRowCode2()
						&& col == gSyntaxMessage.get(syntaxMessageIndex).getFinishColCode2() - 1) {
					// if the end is found

					if (closeTagRequired == false) {
						/*
						 * if the beginning has not given any link open tag, add it
						 */
						String syntaxTagHeader = "<a class='syntaxsim' id='" + syntaxMessageId + sourceId
								+ syntaxCounter + "' href=\"#" + syntaxMessageId + targetId
								+ "\" onclick=\"markSelected('" + syntaxMessageId + "','" + tableId + "')\" >";
						code += syntaxTagHeader;
						syntaxCounter++;
					}

					// add the end
					code += (c + "</a>");
					// set null
					syntaxMessageId = null;
					// increment syntax index
					syntaxMessageIndex++;
					syntaxCounter = 1;
					// inform that no </a> needed to close
					closeTagRequired = false;
				} else {
					// this to re-highlight new line if it is still a part of
					// matched tokens
					if (syntaxMessageId != null) {
						if (closeTagRequired == false) {
							if (c.trim().length() > 0 && isInComment == false) {
								String syntaxTagHeader = "<a class='syntaxsim' id='" + syntaxMessageId + sourceId
										+ syntaxCounter + "' href=\"#" + syntaxMessageId + targetId
										+ "\" onclick=\"markSelected('" + syntaxMessageId + "','" + tableId + "')\" >";
								code += syntaxTagHeader;
								closeTagRequired = true;
								syntaxCounter++;
							}
						}
						code += c;
					} else {
						code += c;
					}
				}
			}
			if (closeTagRequired == true) {
				/*
				 * This mechanism to guarantee no remaining whitespaces at the end of the line
				 * are highlighted.
				 */
				// get the trimmed version of the code
				String codeTemp = code.trim();
				// get last non-whitespace char
				char lastChar = codeTemp.charAt(codeTemp.length() - 1);
				// get the position of the last non-whitespace char
				int indexOfLastNonWhitespace = code.lastIndexOf(lastChar);
				// put the closing tag after that last char
				code = code.substring(0, indexOfLastNonWhitespace + 1) + "</a>"
						+ code.substring(indexOfLastNonWhitespace + 1) + System.lineSeparator();
			} else {
				code += System.lineSeparator();
			}
			row++;
		}
		fr.close();
		return code;
	}

	/*
	 * This is the same as the previous two except that these methods automatically
	 * filter the comments so that they are not embedded on the strings.
	 */
	private static String readCode1WithoutCommentWhiteSpace(String filepath, ArrayList<FeedbackMessage> syntaxMessage,
			ArrayList<FeedbackToken> comments, String tableId) throws Exception {
		// for source and target id on code
		char sourceId = 'a';
		char targetId = 'b';
		// sort all list based on tow then col on code 1
		Collections.sort(syntaxMessage, new Code1FeedbackComparator());
		Collections.sort(comments);

		// embedding comment tags
		String code = "";
		BufferedReader fr = new BufferedReader(new FileReader(filepath));
		String line;
		int row = 1;
		int rowWithoutCommentWhitespace = 1;
		// the first position from comment list
		int commentIndex = 0;
		// the first position from syntax message list
		int syntaxMessageIndex = 0;
		// refers to the ID that will be written
		String syntaxMessageId = null;

		boolean isInComment = false;

		// refers to following rows for a particular match
		int syntaxCounter = 1;
		while ((line = fr.readLine()) != null) {
			// string to store the row
			String rcode = "";
			// to mark whether </a> tag is required at the end of line
			boolean closeTagRequired = false;
			for (int col = 0; col < line.length(); col++) {
				String c = HTMLSafeStringFormat(line.charAt(col));

				// if the position matches with a comment
				if (commentIndex < comments.size() && row == comments.get(commentIndex).getStartRow()
						&& col == comments.get(commentIndex).getStartCol()) {
					// mark that it is in comment
					isInComment = true;
					// close previous tag if any
					if (closeTagRequired) {
						/*
						 * This mechanism to guarantee that per line, no whitespaces after the last
						 * matched syntax are highlighted. Quite messy.
						 */
						// get the trimmed version of the code
						String codeTemp = rcode.trim();
						// get last non-whitespace char
						char lastChar = codeTemp.charAt(codeTemp.length() - 1);
						// get the position of the last non-whitespace char
						int indexOfLastNonWhitespace = rcode.lastIndexOf(lastChar);
						// put the closing tag after that last char
						rcode = rcode.substring(0, indexOfLastNonWhitespace + 1) + "</a>"
								+ rcode.substring(indexOfLastNonWhitespace + 1);
						// set no need for close tag
						closeTagRequired = false;
					}
				} else if (commentIndex < comments.size() && row == comments.get(commentIndex).getFinishRow()
						&& col == comments.get(commentIndex).getFinishCol() - 1) {
					// if the end is found, mark that it is not comment anymore
					isInComment = false;
					commentIndex++;
				} else if (syntaxMessageIndex < syntaxMessage.size()
						&& row == syntaxMessage.get(syntaxMessageIndex).getStartRowCode1()
						&& col == syntaxMessage.get(syntaxMessageIndex).getStartColCode1()) {
					// set the syntax ID
					syntaxMessageId = syntaxMessage.get(syntaxMessageIndex).getVisualId();
					// prepare the beginning tag
					String syntaxTagHeader = "<a class='syntaxsim' id='" + syntaxMessageId + sourceId + "' href=\"#"
							+ syntaxMessageId + targetId + "\" onclick=\"markSelected('" + syntaxMessageId + "','"
							+ tableId + "')\" >";
					// put the header tag along with current character
					rcode += (syntaxTagHeader + c);
					// inform that </a> needed to close
					closeTagRequired = true;
					// set the line number
					syntaxMessage.get(syntaxMessageIndex).setStartRowCode1(rowWithoutCommentWhitespace);
				} else if (syntaxMessageIndex < syntaxMessage.size()
						&& row == syntaxMessage.get(syntaxMessageIndex).getFinishRowCode1()
						&& col == syntaxMessage.get(syntaxMessageIndex).getFinishColCode1() - 1) {
					// if the end is found

					if (closeTagRequired == false) {
						/*
						 * if the beginning has not given any link open tag, add it
						 */
						String syntaxTagHeader = "<a class='syntaxsim' id='" + syntaxMessageId + sourceId
								+ syntaxCounter + "' href=\"#" + syntaxMessageId + targetId
								+ "\" onclick=\"markSelected('" + syntaxMessageId + "','" + tableId + "')\" >";
						rcode += syntaxTagHeader;
						syntaxCounter++;
					}

					// add the end
					rcode += (c + "</a>");
					// set null
					syntaxMessageId = null;
					// increment syntax index
					syntaxMessageIndex++;
					// set syntax counter to 1
					syntaxCounter = 1;
					// inform that no </a> needed to close
					closeTagRequired = false;
				} else {
					// this to re-highlight new line if it is still a part of
					// matched tokens
					if (isInComment) {
						// do nothing
					} else if (syntaxMessageId != null) {
						if (closeTagRequired == false) {
							if (c.trim().length() > 0) {
								String syntaxTagHeader = "<a class='syntaxsim' id='" + syntaxMessageId + sourceId
										+ syntaxCounter + "' href=\"#" + syntaxMessageId + targetId
										+ "\" onclick=\"markSelected('" + syntaxMessageId + "','" + tableId + "')\" >";
								rcode += syntaxTagHeader;
								closeTagRequired = true;
								syntaxCounter++;
							}
						}
						rcode += c;
					} else {
						rcode += c;
					}
				}
			}
			if (closeTagRequired == true) {
				rcode += ("</a>" + System.lineSeparator());
			} else {
				rcode += System.lineSeparator();
			}
			// if the line is not empty, add it
			if (rcode.trim().length() > 0) {
				code += rcode;
				rowWithoutCommentWhitespace++;
			}

			row++;
		}
		fr.close();
		return code;
	}

	private static String readCode2WithoutCommentWhiteSpace(String filepath, ArrayList<FeedbackMessage> syntaxMessage,
			ArrayList<FeedbackToken> comments, String tableId) throws Exception {
		/*
		 * this function cannot be merged to readCode1 since all get start and finish
		 * pos methods are different.
		 */
		// for source and target id on code
		char sourceId = 'b';
		char targetId = 'a';
		// sort all list based on tow then col on code 1
		Collections.sort(syntaxMessage, new Code2FeedbackComparator());
		Collections.sort(comments);

		// embedding comment tags
		String code = "";
		BufferedReader fr = new BufferedReader(new FileReader(filepath));
		String line;
		int row = 1;
		int rowWithoutCommentWhitespace = 1;
		// the first position from comment list
		int commentIndex = 0;
		// the first position from syntax message list
		int syntaxMessageIndex = 0;
		// refers to the ID that will be written
		String syntaxMessageId = null;

		boolean isInComment = false;

		// refers to following rows for a particular match
		int syntaxCounter = 1;
		while ((line = fr.readLine()) != null) {

			// string to store the row
			String rcode = "";
			// to mark whether </a> tag is required at the end of line
			boolean closeTagRequired = false;
			for (int col = 0; col < line.length(); col++) {
				String c = HTMLSafeStringFormat(line.charAt(col));
				// if the position matches with a comment
				if (commentIndex < comments.size() && row == comments.get(commentIndex).getStartRow()
						&& col == comments.get(commentIndex).getStartCol()) {
					// mark that it is in comment
					isInComment = true;
					// close previous tag if any
					if (closeTagRequired) {
						/*
						 * This mechanism to guarantee that per line, no whitespaces after the last
						 * matched syntax are highlighted. Quite messy.
						 */
						// get the trimmed version of the code
						String codeTemp = rcode.trim();
						// get last non-whitespace char
						char lastChar = codeTemp.charAt(codeTemp.length() - 1);
						// get the position of the last non-whitespace char
						int indexOfLastNonWhitespace = rcode.lastIndexOf(lastChar);
						// put the closing tag after that last char
						rcode = rcode.substring(0, indexOfLastNonWhitespace + 1) + "</a>"
								+ rcode.substring(indexOfLastNonWhitespace + 1);
						// set no need for close tag
						closeTagRequired = false;
					}
				} else if (commentIndex < comments.size() && row == comments.get(commentIndex).getFinishRow()
						&& col == comments.get(commentIndex).getFinishCol() - 1) {
					// if the end is found, mark that it is not comment anymore
					isInComment = false;
					commentIndex++;
				} else if (syntaxMessageIndex < syntaxMessage.size()
						&& row == syntaxMessage.get(syntaxMessageIndex).getStartRowCode2()
						&& col == syntaxMessage.get(syntaxMessageIndex).getStartColCode2()) {
					// set the syntax ID
					syntaxMessageId = syntaxMessage.get(syntaxMessageIndex).getVisualId();
					// prepare the beginning tag
					String syntaxTagHeader = "<a class='syntaxsim' id='" + syntaxMessageId + sourceId + "' href=\"#"
							+ syntaxMessageId + targetId + "\" onclick=\"markSelected('" + syntaxMessageId + "','"
							+ tableId + "')\" >";
					// put the header tag along with current character
					rcode += (syntaxTagHeader + c);
					// inform that </a> needed to close
					closeTagRequired = true;
					// set the line number
					syntaxMessage.get(syntaxMessageIndex).setStartRowCode2(rowWithoutCommentWhitespace);
				} else if (syntaxMessageIndex < syntaxMessage.size()
						&& row == syntaxMessage.get(syntaxMessageIndex).getFinishRowCode2()
						&& col == syntaxMessage.get(syntaxMessageIndex).getFinishColCode2() - 1) {
					// if the end is found

					if (closeTagRequired == false) {
						/*
						 * if the beginning has not given any link open tag, add it
						 */
						String syntaxTagHeader = "<a class='syntaxsim' id='" + syntaxMessageId + sourceId
								+ syntaxCounter + "' href=\"#" + syntaxMessageId + targetId
								+ "\" onclick=\"markSelected('" + syntaxMessageId + "','" + tableId + "')\" >";
						rcode += syntaxTagHeader;
						syntaxCounter++;
					}

					// add the end
					rcode += (c + "</a>");
					// set null
					syntaxMessageId = null;
					// increment syntax index
					syntaxMessageIndex++;
					// set syntax counter to 1
					syntaxCounter = 1;
					// inform that no </a> needed to close
					closeTagRequired = false;
				} else {
					// this to re-highlight new line if it is still a part of
					// matched tokens
					if (isInComment) {
						// do nothing
					} else if (syntaxMessageId != null) {
						if (closeTagRequired == false) {
							if (c.trim().length() > 0) {
								String syntaxTagHeader = "<a class='syntaxsim' id='" + syntaxMessageId + sourceId
										+ syntaxCounter + "' href=\"#" + syntaxMessageId + targetId
										+ "\" onclick=\"markSelected('" + syntaxMessageId + "','" + tableId + "')\" >";
								rcode += syntaxTagHeader;
								closeTagRequired = true;
								syntaxCounter++;
							}
						}
						rcode += c;
					} else {
						rcode += c;
					}
				}
			}
			if (closeTagRequired == true) {
				rcode += ("</a>" + System.lineSeparator());
			} else {
				rcode += System.lineSeparator();
			}
			// if the line is not empty, add it
			if (rcode.trim().length() > 0) {
				code += rcode;
				rowWithoutCommentWhitespace++;
			}

			row++;
		}
		fr.close();
		return code;
	}

	private static String getExplanationContent(ArrayList<FeedbackMessage> syntaxMessage, String mode) {
		String s = "";

		// add for syntax messages
		for (FeedbackMessage m : syntaxMessage) {
			s = s + "<div class=\"explanationcontent\" id=\"" + m.getVisualId() + "he\">";
			s = s + "\n\t" + ((SyntaxFeedbackMessage) m).toString(mode);
			s = s + "\n</div>\n";
		}

		return s;
	}

	public static String HTMLSafeStringFormat(char in) {
		String out = "";
		if (in == '<')
			out = "&lt;";
		else if (in == '>')
			out = "&gt;";
		else if (in == '\"')
			out = "&quot;";
		else if (in == '&')
			out = "&amp;";
		else
			out = in + "";
		return out;
	}

	public static String HTMLSafeStringFormatWithSpace(char in) {
		String out = HTMLSafeStringFormat(in);
		if (in == ' ')
			out = "&nbsp;";
		return out;
	}

	public static String HTMLSafeStringFormatWithSpace(String in) {
		String out = "";
		for (int i = 0; i < in.length(); i++) {
			out += HTMLSafeStringFormatWithSpace(in.charAt(i));
		}
		return out;
	}
}

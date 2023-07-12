package sstrange.language.python;

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
import sstrange.token.FeedbackTokenComparator;
import support.stringmatching.GSTMatchTuple;

public class PythonHtmlGenerator {
	/*
	 * This class is different to HtmlGenerator as Python tokenisation is quite unique (see readCode methods)
	 */
	
	public static void generateHtmlForSSTRANGE(String codepath1, String codepath2,
			ArrayList<FeedbackToken> tokenString1, ArrayList<FeedbackToken> tokenString2, String dirname1,
			String dirname2, String templateHTMLPath, String outputHTMLPath, int minimumMatchLength, int cluster,
			String languageCode, ArrayList<GSTMatchTuple> matches)
			throws Exception {


		// get syntax reordering messages
		gSyntaxMessage = FeedbackMessageGenerator.generateSimilarityMessages(tokenString1, tokenString2,
				 minimumMatchLength, languageCode, matches);

		String[] defaultInfoStrings = generateInformationStrings(codepath1, codepath2, "origtablecontent", "", true,
				languageCode);

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

			if (line.contains("@cluster")) {
				line = line.replace("@cluster", cluster + "");
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

			fw.write(line);
			fw.write(System.lineSeparator());
		}
		fr.close();
		fw.close();
	}

	private static ArrayList<FeedbackMessage> gSyntaxMessage;

	private static String[] generateInformationStrings(String filepath1, String filepath2, String tableId, String mode,
			boolean isCommentIncluded, String humanLanguageId) throws Exception {
		// get the html strings
		String code1, code2;

		// sort syntax messages
		Collections.sort(gSyntaxMessage);
		// set visual ID for syntax message
		for (int i = 0; i < gSyntaxMessage.size(); i++) {
			FeedbackMessage fm = gSyntaxMessage.get(i);
			fm.setVisualId("s" + mode + (i + 1));
		}

		code1 = readCode1(filepath1, tableId);
		code2 = readCode2(filepath2, tableId);

		// get table contents
		ArrayList<FeedbackMessage> messages = new ArrayList<>();
		messages.addAll(gSyntaxMessage);
		String tableContent = HtmlTableStringGenerator.getTableContentForMatchesForSSTRANGE(messages, tableId,
				humanLanguageId);

		// get natural language explanation content
		String explanationContent = getExplanationContent(gSyntaxMessage);

		return new String[] { code1, code2, tableContent, explanationContent };
	}

	/*
	 * THESE METHODS ARE DIFFERENT WITH JAVA'S METHODS WITH THE SAME NAME AS THEY
	 * DEAL WITH ONE CHARACTER COMMENT (#). They generate strings representing the
	 * code files with some tags embedded. It applies a little about deterministic
	 * finite automata.
	 */

	private static String readCode1(String filepath, String tableId) throws Exception {
		// for source and target id on code
		char sourceId = 'a';
		char targetId = 'b';
		// sort all list based on tow then col on code 1
		Collections.sort(gSyntaxMessage, new Code1FeedbackComparator());

		// embedding comment tags
		String code = "";
		BufferedReader fr = new BufferedReader(new FileReader(filepath));
		String line;
		int row = 1;
		// the first position from syntax message list
		int syntaxMessageIndex = 0;
		// refers to the ID that will be written
		String syntaxMessageId = null;

		// refers to following rows for a particular match
		int syntaxCounter = 1;
		while ((line = fr.readLine()) != null) {
			// to mark whether </a> tag is required at the end of line
			boolean closeTagRequired = false;
			for (int col = 0; col < line.length(); col++) {
				String c = HTMLSafeStringFormat(line.charAt(col));

				if (syntaxMessageIndex < gSyntaxMessage.size()
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
							if (c.trim().length() > 0) {
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

	private static String readCode2(String filepath, String tableId) throws Exception {
		/*
		 * this function cannot be merged to readCode1 since all get start and finish
		 * pos methods are different.
		 */
		// for source and target id on code
		char sourceId = 'b';
		char targetId = 'a';
		// sort all list based on tow then col on code 2
		Collections.sort(gSyntaxMessage, new Code2FeedbackComparator());

		// embedding comment tags
		String code = "";
		BufferedReader fr = new BufferedReader(new FileReader(filepath));
		String line;
		int row = 1;
		// the first position from syntax list
		int syntaxMessageIndex = 0;
		// refers to the ID that will be written
		String syntaxMessageId = null;

		// refers to following rows for a particular match
		int syntaxCounter = 1;
		while ((line = fr.readLine()) != null) {
			// to mark whether </a> tag is required at the end of line
			boolean closeTagRequired = false;
			for (int col = 0; col < line.length(); col++) {
				String c = HTMLSafeStringFormat(line.charAt(col));
				if (syntaxMessageIndex < gSyntaxMessage.size()
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
							if (c.trim().length() > 0) {
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

	private static String getExplanationContent(ArrayList<FeedbackMessage> syntaxMessage) {
		String s = "";

		// add for syntax messages
		for (FeedbackMessage m : syntaxMessage) {
			s = s + "<div class=\"explanationcontent\" id=\"" + m.getVisualId() + "he\">";
			s = s + "\n\t" + ((SyntaxFeedbackMessage) m).toString();
			s = s + "\n</div>";
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

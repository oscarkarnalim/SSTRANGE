package sstrange.language.html;

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

public class HTMLHtmlGenerator {
	/*
	 * This class is different from HTMLGenerator as it is dedicated to HTML/PHP
	 * with JS and CSS
	 */

	public static void generateHtmlForSSTRANGE(String codepath1, String codepath2, String phpCodepath1,
			String phpCodepath2, String jsCodepath1, String jsCodepath2, String cssCodepath1, String cssCodepath2,
			ArrayList<FeedbackToken> tokenString1, ArrayList<FeedbackToken> tokenString2,
			ArrayList<FeedbackToken> phpTokenString1, ArrayList<FeedbackToken> phpTokenString2,
			ArrayList<FeedbackToken> jsTokenString1, ArrayList<FeedbackToken> jsTokenString2,
			ArrayList<FeedbackToken> cssTokenString1, ArrayList<FeedbackToken> cssTokenString2, String dirname1,
			String dirname2, String templateHTMLPath, String outputHTMLPath, int minimumMatchLength,
			int sameClusterOccurrences, String languageCode, ArrayList<GSTMatchTuple> matches,
			ArrayList<GSTMatchTuple> phpMatches, ArrayList<GSTMatchTuple> jsMatches,
			ArrayList<GSTMatchTuple> cssMatches) throws Exception {
		// HTML

		// get syntax messages
		ArrayList<FeedbackMessage> gSyntaxMessage = FeedbackMessageGenerator.generateSimilarityMessages(tokenString1,
				tokenString2, minimumMatchLength, languageCode, matches);

		// generate the information strings
		String[] defaultInfoStrings = generateInformationStrings(gSyntaxMessage, codepath1, codepath2,
				"origtablecontent", "", languageCode);

		// PHP
		gSyntaxMessage = FeedbackMessageGenerator.generateSimilarityMessages(phpTokenString1, phpTokenString2,
				minimumMatchLength, languageCode, phpMatches);

		String[] phpInfoStrings = generateInformationStrings(gSyntaxMessage, phpCodepath1, phpCodepath2,
				"phporigtablecontent", "php", languageCode);

		// JS
		gSyntaxMessage = FeedbackMessageGenerator.generateSimilarityMessages(jsTokenString1, jsTokenString2,
				minimumMatchLength, languageCode, jsMatches);

		String[] jsInfoStrings = generateInformationStrings(gSyntaxMessage, jsCodepath1, jsCodepath2,
				"jsorigtablecontent", "js", languageCode);

		// CSS
		gSyntaxMessage = FeedbackMessageGenerator.generateSimilarityMessages(cssTokenString1, cssTokenString2,
				minimumMatchLength, languageCode, cssMatches);

		String[] cssInfoStrings = generateInformationStrings(gSyntaxMessage, cssCodepath1, cssCodepath2,
				"cssorigtablecontent", "css", languageCode);

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

			// for html
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

			// for php
			if (line.contains("@phpcode1")) {
				line = line.replace("@phpcode1", phpInfoStrings[0]);
			}
			if (line.contains("@phpcode2")) {
				line = line.replace("@phpcode2", phpInfoStrings[1]);
			}
			if (line.contains("@phptablecontent")) {
				line = line.replace("@phptablecontent", phpInfoStrings[2]);
			}
			if (line.contains("@phpexplanation")) {
				line = line.replace("@phpexplanation", phpInfoStrings[3]);
			}

			// for js
			if (line.contains("@jscode1")) {
				line = line.replace("@jscode1", jsInfoStrings[0]);
			}
			if (line.contains("@jscode2")) {
				line = line.replace("@jscode2", jsInfoStrings[1]);
			}
			if (line.contains("@jstablecontent")) {
				line = line.replace("@jstablecontent", jsInfoStrings[2]);
			}
			if (line.contains("@jsexplanation")) {
				line = line.replace("@jsexplanation", jsInfoStrings[3]);
			}

			// for css
			if (line.contains("@csscode1")) {
				line = line.replace("@csscode1", cssInfoStrings[0]);
			}
			if (line.contains("@csscode2")) {
				line = line.replace("@csscode2", cssInfoStrings[1]);
			}
			if (line.contains("@csstablecontent")) {
				line = line.replace("@csstablecontent", cssInfoStrings[2]);
			}
			if (line.contains("@cssexplanation")) {
				line = line.replace("@cssexplanation", cssInfoStrings[3]);
			}

			fw.write(line);
			fw.write(System.lineSeparator());
		}
		fr.close();
		fw.close();
	}

	private static String[] generateInformationStrings(ArrayList<FeedbackMessage> gSyntaxMessage, String filepath1,
			String filepath2, String tableId, String mode, String humanLanguageId) throws Exception {

		String noteForEmptyFiletype = "No files in such format detected. \nKindly check other file types.";
		if (humanLanguageId.equals("id"))
			noteForEmptyFiletype = "Tidak ada berkas dengan format terkait terdeteksi. \nMohon mengecek tipe berkas lainnya.";

		// get the html strings
		String code1, code2;

		// sort syntax messages
		Collections.sort(gSyntaxMessage);
		// set visual ID for syntax message
		for (int i = 0; i < gSyntaxMessage.size(); i++) {
			FeedbackMessage fm = gSyntaxMessage.get(i);
			fm.setVisualId("s" + mode + (i + 1));
		}
		
		code1 = readCode1(filepath1, tableId, gSyntaxMessage);
		if (code1.trim().length() == 0)
			code1 = noteForEmptyFiletype;
		code2 = readCode2(filepath2, tableId, gSyntaxMessage);
		if (code2.trim().length() == 0)
			code2 = noteForEmptyFiletype;

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
	 * these two methods generate strings representing the code files with some tags
	 * embedded. It applies a little about deterministic finite automata.
	 */
	private static String readCode1(String filepath, String tableId, ArrayList<FeedbackMessage> gSyntaxMessage)
			throws Exception {
		// if the file does not exist, return empty string
		if(filepath == null) {
			return "";
		}
		
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

	private static String readCode2(String filepath, String tableId, ArrayList<FeedbackMessage> gSyntaxMessage)
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

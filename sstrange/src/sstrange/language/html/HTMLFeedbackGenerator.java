package sstrange.language.html;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Scanner;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import jdk.internal.org.jline.reader.impl.completer.FileNameCompleter;
import sstrange.CodeMerger;
import sstrange.support.htmlantlr.HTMLLexer;
import sstrange.token.FeedbackToken;

public class HTMLFeedbackGenerator {
	public static ArrayList<FeedbackToken> syntaxTokenStringPreprocessing(ArrayList<FeedbackToken> tokenString) {
		/*
		 * Generalise all attribute values (ATTVALUE_VALUE) and html text (HTML_TEXT).
		 * This automatically generalises all constants.
		 * 
		 * Generalise all script and style as they will be handled separately.
		 * 
		 * Lowercase all tag names
		 */
		ArrayList<FeedbackToken> result = new ArrayList<>();

		for (int i = 0; i < tokenString.size(); i++) {
			FeedbackToken cur = tokenString.get(i);
			String type = cur.getType();

			if (type.equals("ATTVALUE_VALUE")) {
				cur.setContentForComparison("ATTVALUE_VALUE");
			} else if (type.equals("HTML_TEXT")) {
				cur.setContentForComparison("HTML_TEXT");
			} else if (type.equals("SCRIPT_OPEN")) {
				cur.setContentForComparison("SCRIPT_TAG_NAME");
			} else if (type.equals("SCRIPT_BODY") || type.equals("SCRIPT_SHORT_BODY")) {
				cur.setContentForComparison("SCRIPT_BODY");
			} else if (type.equals("STYLE_OPEN")) {
				cur.setContentForComparison("STYLE_TAG_NAME");
			} else if (type.equals("STYLE_BODY") || type.equals("STYLE_SHORT_BODY")) {
				cur.setContentForComparison("STYLE_BODY");
			} else if (type.equals("TAG_NAME")) {
				cur.setContentForComparison(cur.getContent().toLowerCase());
			}

			if (type.equals("WS") == false && type.equals("COMMENT") == false) {
				result.add(cur);
			}

		}

		return result;
	}

	public static ArrayList<FeedbackToken> generateFeedbackTokenString(String filePath, boolean isMultipleFiles) {
		/*
		 * generate token strings for given html file. Create separate files for all CSS
		 * (style) and JS (script) in the same location but with additional extension \
		 * (either .css or .js). This excludes inline CSS and JS as they are treated as
		 * attributes.
		 */

		try {

			// generate CSS file
			FileWriter cssWriter = new FileWriter(new File(filePath + ".css"));

			// set CSS header
			String header = "CSS code from " + filePath;
			// if the file is from merging
			header = header.replaceAll("\\[merged\\] ", "");
			String pattern = "/* ";
			for (int i = 0; i < header.length(); i++)
				pattern += "=";
			pattern += " */" + System.lineSeparator();
			cssWriter.write(pattern);
			cssWriter.write("/* " + header + " */" + System.lineSeparator());
			cssWriter.write(pattern);

			// generate JS file
			FileWriter jsWriter = new FileWriter(new File(filePath + ".js"));

			// set JS header
			header = "JS code from " + filePath;
			// if the file is from merging
			header = header.replaceAll("\\[merged\\] ", "");
			pattern = "/* ";
			for (int i = 0; i < header.length(); i++)
				pattern += "=";
			pattern += " */" + System.lineSeparator();
			jsWriter.write(pattern);
			jsWriter.write("/* " + header + " */" + System.lineSeparator());
			jsWriter.write(pattern);

			// markers to know whether js and css are not empty
			boolean isJSEmpty = true;
			boolean isCSSEmpty = true;
			
			// including comment and whitespaces
			ArrayList<FeedbackToken> result = new ArrayList<>();
			// build the lexer
			Lexer lexer = new HTMLLexer(CharStreams.fromFileName(filePath));
			lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
			// extract the tokens
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			tokens.fill();
			// for each element including eof, form the token and embed start
			// finish location
			for (int index = 0; index < tokens.size(); index++) {
				Token token = tokens.get(index);

				// set the finish row and col for previously added token
				if (result.size() >= 1) {
					FeedbackToken pastToken = result.get(index - 1);
					pastToken.setFinishRow(token.getLine());
					pastToken.setFinishCol(token.getCharPositionInLine());
				}
				String type = HTMLLexer.VOCABULARY.getDisplayName(token.getType());

				// save script and style to separate files
				if (type.equals("SCRIPT_BODY")) {
					String content = token.getText().substring(0, token.getText().lastIndexOf("</script>"));
					if (content.trim().length() > 0) {
						jsWriter.write("/* Taken from line " + token.getLine() + " */" + System.lineSeparator());
						jsWriter.write(content + System.lineSeparator());
						isJSEmpty = false;
					}
				} else if (type.equals("SCRIPT_SHORT_BODY")) {
					String content = token.getText().substring(0, token.getText().lastIndexOf("</>"));
					if (content.trim().length() > 0) {
						jsWriter.write("/* Taken from line " + token.getLine() + " */" + System.lineSeparator());
						jsWriter.write(content + System.lineSeparator());
						isJSEmpty = false;
					}
				} else if (type.equals("STYLE_BODY")) {
					String content = token.getText().substring(0, token.getText().lastIndexOf("</style>"));
					if (content.trim().length() > 0) {
						cssWriter.write("/* Taken from line " + token.getLine() + " */" + System.lineSeparator());
						cssWriter.write(content + System.lineSeparator());
						isCSSEmpty = false;
					}
				} else if (type.equals("STYLE_SHORT_BODY")) {
					String content = token.getText().substring(0, token.getText().lastIndexOf("</>"));
					if (content.trim().length() > 0) {
						cssWriter.write("/* Taken from line " + token.getLine() + " */" + System.lineSeparator());
						cssWriter.write(content + System.lineSeparator());
						isCSSEmpty = false;
					}
				}

				result.add(new HTMLFeedbackToken(token.getText(), type, token.getLine(), token.getCharPositionInLine(),
						0, 0, token));
			}
			/*
			 * for the last token which is eof. this is intentionally created just to set
			 * the finish position of the previous token
			 */
			result.remove(result.size() - 1);

			// close all css and js files
			cssWriter.close();
			jsWriter.close();
			
			// remove the js or css files if empty
			if(isJSEmpty) 
				new File(filePath + ".js").delete();
			if(isCSSEmpty)
				new File(filePath + ".css").delete();

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

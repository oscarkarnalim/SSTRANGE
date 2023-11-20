package sstrange.language.php;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import sstrange.language.html.HTMLFeedbackToken;
import sstrange.support.htmlantlr.HTMLLexer;
import sstrange.support.phpantlr.PhpLexer;
import sstrange.token.FeedbackToken;

public class PHPFeedbackGenerator {

	public static ArrayList<FeedbackToken> syntaxTokenStringPreprocessing(ArrayList<FeedbackToken> tokenString) {
		/*
		 * Generalise all attribute values and text in both PHP and HTML. This
		 * automatically generalises all constants.
		 * 
		 * Generalize any identifiers
		 * 
		 * Generalise all script and style as they will be handled separately.
		 * 
		 * Lowercase all tag names
		 */
		ArrayList<FeedbackToken> result = new ArrayList<>();

		for (int i = 0; i < tokenString.size(); i++) {
			FeedbackToken cur = tokenString.get(i);
			String type = cur.getType();

			if (type.equals("HTMLIdentifier")) {
				// tag name and attribute names
				cur.setContentForComparison(cur.getContent().toLowerCase());
			} else if (type.equals("HtmlText")) {
				cur.setContentForComparison("HtmlText");
			} else if (type.equals("ScriptText")) {
				cur.setContentForComparison("ScriptBody");
			} else if (type.equals("StyleBody")) {
				cur.setContentForComparison("StyleBody");
			} else if (type.equals("HTMLStringLiteral")) {
				cur.setContentForComparison("HtmlStringLiteral");
			} else if (type.equals("PHPStringLiteral")) {
				cur.setContentForComparison("PhpStringLiteral");
			} else if (type.equals("PHPVariable")) {
				cur.setContentForComparison("PhpVariable");
			} else if (type.equals("PHPNonVarIdentifier")) {
				cur.setContentForComparison("PhpNonVarIdentifier");
			}

			if (type.equals("WS") == false && type.equals("COMMENT") == false) {
				result.add(cur);
			}

		}

		return result;
	}

	public static ArrayList<FeedbackToken> generateFeedbackTokenString(String filePath, boolean isMultipleFiles) {
		/*
		 * generate token strings for given php file. HTML is included as part of PHP
		 * syntax so long as it is not in echo. Create separate files for all CSS
		 * (style) and JS (script) in the same location but with additional extension \
		 * (either .css, or .js). This excludes inline CSS, and JS as they are treated
		 * as attributes.
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
			Lexer lexer = new PhpLexer(CharStreams.fromFileName(filePath));
			lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
			// extract the tokens
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			tokens.fill();
			// for each element including eof, form the token and embed start
			// finish location

			FeedbackToken pastToken = null;
			for (int index = 0; index < tokens.size(); index++) {
				Token token = tokens.get(index);
				String text = token.getText();
				String type = PhpLexer.VOCABULARY.getDisplayName(token.getType());

				/*
				 * Dealing with single line comment that is separated to three parts:
				 * SingleLineComment, Comment, and CommentEnd. Remove the first and the third,
				 * keep the second with text appended.
				 */
				if (type.equals("Comment")) {
					text = "// " + text + "\r\n";
				} else if (type.equals("SingleLineComment") || type.equals("CommentEnd")) {
					continue;
				}

				// string literals
				if (type.equals("DoubleQuote") || type.equals("SingleQuote") || type.equals("StringPart")) {
					type = "PHPStringLiteral";
				}
				if (type.equals("HtmlStartDoubleQuoteString") || type.equals("HtmlStartSingleQuoteString")
						|| type.equals("HtmlEndDoubleQuoteString") || type.equals("HtmlEndSingleQuoteString")
						|| type.equals("HtmlDoubleQuoteString") || type.equals("HtmlSingleQuoteString")) {
					type = "HTMLStringLiteral";
				}

				// variables and identifiers
				if (type.equals("VarName")) {
					type = "PHPVariable";
				}
				if (type.equals("Label")) {
					type = "PHPNonVarIdentifier";
				}
				if (type.equals("HtmlName")) {
					type = "HTMLIdentifier";
				}

				// set the finish row and col for previously added token
				if (result.size() >= 1 && pastToken != null) {
					pastToken.setFinishRow(token.getLine());
					pastToken.setFinishCol(token.getCharPositionInLine());
				}

				// save script and style to separate files
				if (type.equals("ScriptText")) {
					String content = token.getText();
					if (content.trim().length() > 0) {
						jsWriter.write("/* Taken from line " + token.getLine() + " */" + System.lineSeparator());
						jsWriter.write(content + System.lineSeparator());
						isJSEmpty = false;
					}
				} else if (type.equals("StyleBody")) {
					String content = "";
					if (token.getText().endsWith("</style>"))
						content = token.getText().substring(0, token.getText().lastIndexOf("</style>"));
					else if (token.getText().endsWith("</>"))
						content = token.getText().substring(0, token.getText().lastIndexOf("</>"));
					if (content.trim().length() > 0) {
						cssWriter.write("/* Taken from line " + token.getLine() + " */" + System.lineSeparator());
						cssWriter.write(content + System.lineSeparator());
						isCSSEmpty = false;
					}
				}

				pastToken = new PHPFeedbackToken(text, type, token.getLine(), token.getCharPositionInLine(), 0, 0,
						token);
				result.add(pastToken);
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
			if (isJSEmpty)
				new File(filePath + ".js").delete();
			if (isCSSEmpty)
				new File(filePath + ".css").delete();

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

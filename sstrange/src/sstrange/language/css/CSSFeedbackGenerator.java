package sstrange.language.css;

import java.util.ArrayList;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import sstrange.support.cssantlr.css3Lexer;
import sstrange.token.FeedbackToken;

public class CSSFeedbackGenerator {
	public static ArrayList<FeedbackToken> generateFeedbackTokenString(String filePath) {
		/*
		 * generate token strings for given css file
		 */

		try {
			// including comment and whitespaces
			ArrayList<FeedbackToken> result = new ArrayList<>();
			// build the lexer
			Lexer lexer = new css3Lexer(CharStreams.fromFileName(filePath));
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
					FeedbackToken pastToken = result.get(result.size() - 1);
					pastToken.setFinishRow(token.getLine());
					pastToken.setFinishCol(token.getCharPositionInLine());
				}
				String type = css3Lexer.VOCABULARY.getDisplayName(token.getType());

				// if current is "." and the next token type is "Ident", merge to this
				if (token.getText().equals(".") && index + 1 < tokens.size()
						&& css3Lexer.VOCABULARY.getDisplayName(tokens.get(index + 1).getType()).equals("Ident")) {
					// generate new token merging "." and the class name
					result.add(new CSSFeedbackToken(token.getText() + tokens.get(index + 1).getText(), "Class", token.getLine(),
							token.getCharPositionInLine(), 0, 0, token));
					// skip next token
					index++;
				} else {
					result.add(new CSSFeedbackToken(token.getText(), type, token.getLine(),
							token.getCharPositionInLine(), 0, 0, token));
				}
			}
			/*
			 * for the last token which is eof. this is intentionally created just to set
			 * the finish position of the previous token
			 */
			result.remove(result.size() - 1);

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	public static ArrayList<FeedbackToken> syntaxTokenStringPreprocessing(ArrayList<FeedbackToken> tokenString) {
		/*
		 * Generalise all variables, class selectors, and hash selectors
		 * 
		 * Generalise all constants (dimension, number, percentage, string, and unknown
		 * dimension)
		 */
		ArrayList<FeedbackToken> result = new ArrayList<>();

		for (int i = 0; i < tokenString.size(); i++) {
			FeedbackToken cur = tokenString.get(i);
			String type = cur.getType();

			if (type.equals("Class")) {
				cur.setContentForComparison("Class");
			}else if (type.equals("Variable")) {
				cur.setContentForComparison("Variable");
			} else if (type.equals("Hash")) {
				cur.setContentForComparison("Hash");
			} else if (type.equals("Dimension") || type.equals("Number") || type.equals("Percentage")) {
				cur.setContentForComparison("NumConstants");
			} else if (type.equals("String_")) {
				cur.setContentForComparison("StringConstants");
			} else if (type.equals("UnknownDimension")) {
				cur.setContentForComparison("UnknownDim");
			}

			if (type.equals("WS") == false && type.equals("COMMENT") == false) {
				result.add(cur);
			}

		}

		return result;
	}
}

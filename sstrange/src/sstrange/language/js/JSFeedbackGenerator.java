package sstrange.language.js;

import java.util.ArrayList;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import sstrange.support.jsantlr.JavaScriptLexer;
import sstrange.token.FeedbackToken;

public class JSFeedbackGenerator {
	
	public static ArrayList<FeedbackToken> syntaxTokenStringPreprocessing(ArrayList<FeedbackToken> tokenString) {
		/*
		 * Generalise all identifiers and constants
		 */
		ArrayList<FeedbackToken> result = new ArrayList<>();

		for (int i = 0; i < tokenString.size(); i++) {
			FeedbackToken cur = tokenString.get(i);
			String type = cur.getType();

			if (type.equals("Identifier")) {
				cur.setContentForComparison("Identifier");
			} else if (type.contains("Literal")) {
				cur.setContentForComparison("Constants");
			} 

			if (type.equals("WS") == false && type.equals("COMMENT") == false) {
				result.add(cur);
			}

		}

		return result;
	}

	public static ArrayList<FeedbackToken> generateFeedbackTokenString(String filePath) {
		/*
		 * generate token strings for given js file
		 */

		try {
			// including comment and whitespaces
			ArrayList<FeedbackToken> result = new ArrayList<>();
			// build the lexer
			Lexer lexer = new JavaScriptLexer(CharStreams.fromFileName(filePath));
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
				String type = JavaScriptLexer.VOCABULARY.getDisplayName(token.getType());

				result.add(new JSFeedbackToken(token.getText(), type, token.getLine(), token.getCharPositionInLine(),
						0, 0, token));
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
}

package sstrange.language.dart;

import java.util.ArrayList;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import sstrange.language.js.JSFeedbackToken;
import sstrange.support.dartantlr.Dart2Lexer;
import sstrange.token.FeedbackToken;

public class DartFeedbackGenerator {
	public static ArrayList<FeedbackToken> syntaxTokenStringPreprocessing(ArrayList<FeedbackToken> tokenString) {
		/*
		 * Generalise all identifiers and constants. This function does not need to
		 * filter syntax tokens as Dart default tokeniser already ignores both comments
		 * and white space.
		 */
		ArrayList<FeedbackToken> result = new ArrayList<>();

		for (int i = 0; i < tokenString.size(); i++) {
			FeedbackToken cur = tokenString.get(i);
			String type = cur.getType();

			/*
			 * The first three conditions are special cases for Dart Lexer
			 */
			if (cur.getContent().equals("int") || cur.getContent().equals("double") || cur.getContent().equals("num")) {
				cur.setContentForComparison("NumType");
			} else if (cur.getContent().equals("String")) {
				cur.setContentForComparison("StringType");
			} else if (cur.getContent().equals("bool")) {
				cur.setContentForComparison("BoolType");
			} else if (type.contains("IDENTIFIER")) {
				cur.setContentForComparison("Identifier");
			} else if (type.equals("MultiLineString") || type.equals("SingleLineString")) {
				cur.setContentForComparison("StringConstant");
			} else if (type.contains("NUMBER") || type.contains("HEX_NUMBER")) {
				cur.setContentForComparison("NumConstant");
			} 
			
			result.add(cur);

		}

		return result;
	}

	public static ArrayList<FeedbackToken> generateFeedbackTokenString(String filePath) {
		/*
		 * generate token strings for given dart file
		 */

		try {
			// WITHOUT COMMENTS AND WHITE SPACE
			ArrayList<FeedbackToken> result = new ArrayList<>();
			// build the lexer
			Lexer lexer = new Dart2Lexer(CharStreams.fromFileName(filePath));
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
				String type = Dart2Lexer.VOCABULARY.getDisplayName(token.getType());

				result.add(new DartFeedbackToken(token.getText(), type, token.getLine(), token.getCharPositionInLine(),
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

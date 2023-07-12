package sstrange.language.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Token;

import sstrange.CodeMerger;
import sstrange.support.javaantlr.JavaLexer;
import sstrange.support.stringmatching.AdditionalKeywordsManager;
import sstrange.token.FeedbackToken;

public class JavaFeedbackGenerator {

	public static ArrayList<FeedbackToken> syntaxTokenStringPreprocessing(ArrayList<FeedbackToken> tokenString) {
		/*
		 * rename all Identifiers as their respective type +
		 * string and character literals as string literals + considering String data
		 * type as string data type instead of identifier + merging all IntegerLiteral
		 * and FloatingPointLiteral as number + merging all numeric data types to
		 * NumberType, including the object version.
		 */
		ArrayList<FeedbackToken> result = new ArrayList<>();

		for (int i = 0; i < tokenString.size(); i++) {
			FeedbackToken cur = tokenString.get(i);
			String type = cur.getType();

			if (type.equals("Identifier")) {
				if (cur.getContent().equals("Integer") || cur.getContent().equals("Short")
						|| cur.getContent().equals("Long") || cur.getContent().equals("Byte")
						|| cur.getContent().equals("Float") || cur.getContent().equals("Double")) {
					cur.setContentForComparison("number data type");
				} else if (cur.getContent().equals("String") || cur.getContent().equals("Character")) {
					cur.setContentForComparison("string data type");
				} else
					cur.setContentForComparison("identifier");
			} else if (type.equals("StringLiteral") || type.equals("CharacterLiteral")) {
				cur.setContentForComparison("string literal");
			} else if (type.equals("IntegerLiteral") || type.equals("FloatingPointLiteral"))
				cur.setContentForComparison("number literal");
			else if (type.equals("'char'"))
				cur.setContentForComparison("string data type");
			else if (type.equals("'int'") || type.equals("'short'") || type.equals("'long'") || type.equals("'byte'")
					|| type.equals("'float'") || type.equals("'double'"))
				cur.setContentForComparison("number data type");

			if (type.equals("WS") == false && type.equals("COMMENT") == false) {
				result.add(cur);
			}
		}

		return result;
	}

	public static ArrayList<FeedbackToken> generateFeedbackTokenString(String filePath) {

		// include comments and white space
		try {

			ArrayList<FeedbackToken> result = new ArrayList<>();
			// build the lexer
			Lexer lexer = new JavaLexer(CharStreams.fromFileName(filePath));
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
				String type = JavaLexer.VOCABULARY.getDisplayName(token.getType());
				
				//System.out.println(token.getText() + " " + type);

				result.add(new JavaFeedbackToken(token.getText(), type, token.getLine(), token.getCharPositionInLine(),
						0, 0, token));
			}
			/*
			 * for the last token which is eof. this is intentionally created just to set
			 * the finish position of the previous token
			 */
			result.remove(result.size() - 1);
			return result;
		} catch (

		Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

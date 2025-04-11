package sstrange.language.python;

import java.util.ArrayList;
import java.util.Stack;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ConsoleErrorListener;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import sstrange.support.pythonantlr.Python3Lexer;
import sstrange.support.pythonantlr.Python3Parser;
import sstrange.token.FeedbackToken;

public class PythonFeedbackGenerator {

	public static void syntaxTokenStringPreprocessing(ArrayList<FeedbackToken> syntaxTokenString) {
		/*
		 * rename all Identifiers and StringLiterals as their respective type + merging
		 * all IntegerLiteral and FloatingPointLiteral as number
		 */
		for (int i = 0; i < syntaxTokenString.size(); i++) {

			FeedbackToken cur = syntaxTokenString.get(i);
			String type = cur.getType();
			if (type.equals("NAME")) {
				cur.setContentForComparison("identifier");
			} else if (type.equals("STRING_LITERAL"))
				cur.setContentForComparison("string literal");
			else if (type.equals("DECIMAL_INTEGER") || type.equals("FLOAT_NUMBER"))
				cur.setContentForComparison("number literal");
		}
	}

	public static ArrayList<FeedbackToken> generateSyntaxTokenString(String filePath) {
		// excluding comment and whitespaces
		try {
			ArrayList<FeedbackToken> result = new ArrayList<>();
			// build the lexer
			Lexer lexer = new Python3Lexer(CharStreams.fromFileName(filePath));
			lexer.removeErrorListener(ConsoleErrorListener.INSTANCE);
			// extract the tokens
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			tokens.fill();
			// for each element excluding eof, form the token and embed start
			// finish location
			for (int index = 0; index < tokens.size() - 1; index++) {
				Token token = tokens.get(index);
				String type = Python3Lexer.VOCABULARY.getDisplayName(token.getType());
				// remove all whitespace tokens as these tokens are the
				// summarised version of whitespaces
				if (type.equals("93") || type.equals("94") || type.equals("NEWLINE"))
					continue;
				result.add(
						new PythonFeedbackToken(token.getText(), type, token.getLine(), token.getCharPositionInLine(),
								token.getLine(), token.getCharPositionInLine() + token.getText().length(), token));
			}

			return result;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
}

package sstrange.language.dart;

import org.antlr.v4.runtime.Token;

import sstrange.token.FeedbackToken;

public class DartFeedbackToken extends FeedbackToken{
	public DartFeedbackToken(String content, String type, int startRow,
			int startCol, int finishRow, int finishCol, Token antlrToken) {
		super(content, type, startRow, startCol, finishRow, finishCol, antlrToken);
	}
	public String toString(){		
		return "[" + content + " " + type + " " + startRow + " " + startCol + " " + finishRow + " " + finishCol + "]";
	}
}
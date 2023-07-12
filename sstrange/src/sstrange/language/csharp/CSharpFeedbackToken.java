package sstrange.language.csharp;

import org.antlr.v4.runtime.Token;

import sstrange.token.FeedbackToken;

public class CSharpFeedbackToken extends FeedbackToken{
	public CSharpFeedbackToken(String content, String type, int startRow,
			int startCol, int finishRow, int finishCol, Token antlrToken) {
		super(content, type, startRow, startCol, finishRow, finishCol, antlrToken);
		if(this.type.endsWith("comment"))
			this.type = "COMMENT";
		else if(this.type.equalsIgnoreCase("WHITESPACES"))
			this.type = "WS";
	}
	public String toString(){
		String lcontent = content;
		// this is used just to pretty print the output
		if(type.equals("WS"))
			lcontent = "whitepace";
		else if(type.equals("COMMENT"))
			lcontent = "comment";
		
		return "[" + lcontent + " " + type + " " + startRow + " " + startCol + " " + finishRow + " " + finishCol + "]";
	}
}

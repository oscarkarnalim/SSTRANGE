package sstrange.language.js;

import org.antlr.v4.runtime.Token;

import sstrange.token.FeedbackToken;

public class JSFeedbackToken extends FeedbackToken{
	public JSFeedbackToken(String content, String type, int startRow,
			int startCol, int finishRow, int finishCol, Token antlrToken) {
		super(content, type, startRow, startCol, finishRow, finishCol, antlrToken);
		if (this.type.equals("WhiteSpaces") || this.type.equals("LineTerminator"))
			this.type = "WS";
		else if (this.type.contains("Comment"))
			this.type = "COMMENT";
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

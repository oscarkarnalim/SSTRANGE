package sstrange.language.html;

import org.antlr.v4.runtime.Token;

import sstrange.token.FeedbackToken;

public class HTMLFeedbackToken extends FeedbackToken{
	public HTMLFeedbackToken(String content, String type, int startRow,
			int startCol, int finishRow, int finishCol, Token antlrToken) {
		super(content, type, startRow, startCol, finishRow, finishCol, antlrToken);
		if (this.type.equals("TAG_WHITESPACE") || this.type.equals("SEA_WS"))
			this.type = "WS";
		else if (this.type.equals("HTML_COMMENT") || this.type.equals("HTML_CONDITIONAL_COMMENT"))
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

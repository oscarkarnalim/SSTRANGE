package sstrange.language.php;

import org.antlr.v4.runtime.Token;

import sstrange.token.FeedbackToken;

public class PHPFeedbackToken extends FeedbackToken{
	public PHPFeedbackToken(String content, String type, int startRow,
			int startCol, int finishRow, int finishCol, Token antlrToken) {
		super(content, type, startRow, startCol, finishRow, finishCol, antlrToken);
		if (this.type.equals("Whitespace") || this.type.equals("SeaWhitespace"))
			this.type = "WS";
		else if (this.type.equals("MultiLineComment") || this.type.equals("Comment") || this.type.equals("HtmlComment"))
			this.type = "COMMENT";
		else if(this.type.equals(""))
			this.type = "STRING_CONTENT";
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

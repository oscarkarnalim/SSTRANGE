package sstrange.language.css;

import org.antlr.v4.runtime.Token;

import sstrange.token.FeedbackToken;

public class CSSFeedbackToken extends FeedbackToken {
	public CSSFeedbackToken(String content, String type, int startRow, int startCol, int finishRow, int finishCol,
			Token antlrToken) {
		super(content, type, startRow, startCol, finishRow, finishCol, antlrToken);
		if (this.type.equals("Space") || this.type.equals("NewlineOrSpace") || this.type.equals("Newline")
				|| this.type.equals("Whitespace"))
			this.type = "WS";
		else if (this.type.equals("Comment"))
			this.type = "COMMENT";
	}

	public String toString() {
		String lcontent = content;
		// this is used just to pretty print the output
		if (type.equals("WS"))
			lcontent = "whitepace";
		else if (type.equals("COMMENT"))
			lcontent = "comment";

		return "[" + lcontent + " " + type + " " + startRow + " " + startCol + " " + finishRow + " " + finishCol + "]";
	}
}

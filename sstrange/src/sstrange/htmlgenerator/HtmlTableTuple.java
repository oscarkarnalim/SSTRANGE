package sstrange.htmlgenerator;

import java.util.ArrayList;

import sstrange.message.FeedbackMessage;
import sstrange.message.SyntaxFeedbackMessage;
import sstrange.token.FeedbackToken;

public class HtmlTableTuple implements Comparable<HtmlTableTuple> {
	protected FeedbackMessage entity;
	// to store how many matched characters
	protected int minCharacterLength;
	// to store how many matched tokens
	protected int matchedTokenLength;

	public HtmlTableTuple(FeedbackMessage entity) {
		super();
		this.entity = entity;
		calculateImportanceScoreAndSetMatches();
	}

	private void calculateImportanceScoreAndSetMatches() {
		// simply count how many characters involved.
		SyntaxFeedbackMessage sf = (SyntaxFeedbackMessage) entity;
		// set the number of char as 0
		int minCharacterLength1 = 0;
		int minCharacterLength2 = 0;
		// get the lists
		ArrayList<FeedbackToken> list1 = sf.getTokenList1();
		ArrayList<FeedbackToken> list2 = sf.getTokenList2();
		for (int i = 0; i < list1.size(); i++) {
			FeedbackToken ft1 = list1.get(i);
			FeedbackToken ft2 = list2.get(i);
			minCharacterLength1 += ft1.getContent().length();
			minCharacterLength2 += ft2.getContent().length();
		}
		// set the number of min char
		minCharacterLength = Math.min(minCharacterLength1, minCharacterLength2);
		// set the number of matched token
		matchedTokenLength = list1.size();
	}

	@Override
	public int compareTo(HtmlTableTuple arg0) {
		// TODO Auto-generated method stub
		return -this.getMinCharacterLength() + arg0.getMinCharacterLength();
	}

	public FeedbackMessage getEntity() {
		return entity;
	}

	public void setEntity(FeedbackMessage entity) {
		this.entity = entity;
	}

	public int getMinCharacterLength() {
		return minCharacterLength;
	}

	public int getMatchedTokenLength() {
		return matchedTokenLength;
	}

}

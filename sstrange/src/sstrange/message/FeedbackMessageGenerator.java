package sstrange.message;

import java.util.ArrayList;

import sstrange.token.FeedbackToken;
import support.stringmatching.GSTMatchTuple;
import support.stringmatching.GreedyStringTiling;

public class FeedbackMessageGenerator {

	public static ArrayList<FeedbackMessage> generateSimilarityMessages(
			ArrayList<FeedbackToken> syntaxTokenString1, ArrayList<FeedbackToken> syntaxTokenString2,
			ArrayList<FeedbackToken> rawTokenString1, ArrayList<FeedbackToken> rawTokenString2, int minimumMatchLength, String humanLanguageCode,
			ArrayList<GSTMatchTuple> simTuples) {

		// create a list to store the results
		ArrayList<FeedbackMessage> messages = new ArrayList<>();

		// form the messages
		for (int i = 0; i < simTuples.size(); i++) {
			GSTMatchTuple cur = simTuples.get(i);

			// create syntax token lists for storing selected syntax tokens
			ArrayList<FeedbackToken> syntaxTokenList1 = new ArrayList<>();
			ArrayList<FeedbackToken> syntaxTokenList2 = new ArrayList<>();

			// get the syntax tokens
			for (int j = 0; j < cur.length; j++) {
				syntaxTokenList1.add(syntaxTokenString1.get(cur.patternPosition + j));
				syntaxTokenList2.add(syntaxTokenString2.get(cur.textPosition + j));
			}

			// calculate how many modifications on the tokens' whitespaces.
			boolean areWhitespacesModified = false;
			int syntaxwhitespaceStartIdx1 = rawTokenString1.indexOf(syntaxTokenList1.get(0));
			int syntaxwhitespaceFinishIdx1 = rawTokenString1.indexOf(syntaxTokenList1.get(syntaxTokenList1.size() - 1));
			int syntaxwhitespaceStartIdx2 = rawTokenString2.indexOf(syntaxTokenList2.get(0));
			int syntaxwhitespaceFinishIdx2 = rawTokenString2.indexOf(syntaxTokenList2.get(syntaxTokenList2.size() - 1));

			/*
			 * if the first code has different number of tokens than the counterpart, then
			 * the whitespaces are modified.
			 */
			if (syntaxwhitespaceFinishIdx1 - syntaxwhitespaceStartIdx1 != syntaxwhitespaceFinishIdx2
					- syntaxwhitespaceStartIdx2)
				areWhitespacesModified = true;
			else {
				/*
				 * otherwise, check for each whitespace pair
				 */
				for (int j = 0; j < syntaxwhitespaceFinishIdx1 - syntaxwhitespaceStartIdx1; j++) {
					FeedbackToken f1 = rawTokenString1.get(syntaxwhitespaceStartIdx1 + j);
					FeedbackToken f2 = rawTokenString2.get(syntaxwhitespaceStartIdx2 + j);

					if (f1.getType().equals("WS") && f2.getType().equals("WS")
							&& f1.getContent().equals(f2.getContent()) == false) {
						/*
						 * if both are whitespaces but in different forms, count as a modification
						 */
						areWhitespacesModified = true;
						break;
					} else if (f1.getType().equals("WS") && f2.getType().equals("WS") == false) {
						/*
						 * if one of them is whitespace while another is not, count as a modification
						 */
						areWhitespacesModified = true;
						break;
					} else if (f1.getType().equals("WS") == false && f2.getType().equals("WS")) {
						/*
						 * if one of them is whitespace while another is not, count as a modification
						 */
						areWhitespacesModified = true;
						break;
					}
				}
			}

			// create the message and add it
			SyntaxFeedbackMessage m = new SyntaxFeedbackMessage("copied", "syntax token", syntaxTokenList1,
					syntaxTokenList2, areWhitespacesModified, humanLanguageCode);
			messages.add(m);
		}

		return messages;
	}
	
	public static ArrayList<GSTMatchTuple> generateMatchedTuples(ArrayList<FeedbackToken> tokenString1,
			ArrayList<FeedbackToken> tokenString2, int minimumMatchLength) {
		// create array of string for both whitespace strings
		String[] obj1 = new String[tokenString1.size()];
		String[] obj2 = new String[tokenString2.size()];

		for (int i = 0; i < tokenString1.size(); i++) {
			obj1[i] = tokenString1.get(i).getContentForComparison();
		}
		for (int i = 0; i < tokenString2.size(); i++) {
			obj2[i] = tokenString2.get(i).getContentForComparison();
		}
		// get matched tiles with RKRGST to remaining unmatched regions
		ArrayList<GSTMatchTuple> simTuples = GreedyStringTiling.getMatchedTiles(obj1, obj2, minimumMatchLength);

		return simTuples;
	}

}

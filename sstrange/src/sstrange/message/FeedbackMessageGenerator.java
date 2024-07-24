package sstrange.message;

import java.util.ArrayList;

import sstrange.support.stringmatching.GSTMatchTuple;
import sstrange.support.stringmatching.GreedyStringTiling;
import sstrange.token.FeedbackToken;

public class FeedbackMessageGenerator {

	public static ArrayList<FeedbackMessage> generateSimilarityMessages(ArrayList<FeedbackToken> syntaxTokenString1,
			ArrayList<FeedbackToken> syntaxTokenString2, int minimumMatchLength, String humanLanguageCode,
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

			// create the message and add it
			SyntaxFeedbackMessage m = new SyntaxFeedbackMessage("copied", "syntax token", syntaxTokenList1,
					syntaxTokenList2, humanLanguageCode);
			messages.add(m);
		}

		return messages;
	}

	public static void markUnmatchedTokensinString1(ArrayList<GSTMatchTuple> matches,
			ArrayList<ArrayList<Integer>> tokenString1Mismatch, int minimumMatchLength, int submission2ID,
			boolean isSWapped) {
		/*
		 * update mismatches in the tokenString1Mismatch but only the long ones. If
		 * swapped, then the position is text instead of pattern
		 */

		// generate the matches
		boolean[] tokenString1Match = new boolean[tokenString1Mismatch.size()];
		if (isSWapped == false) {
			// submission 1 is on the text
			for (GSTMatchTuple m : matches) {
				for (int i = m.textPosition; i < m.textPosition + m.length; i++) {
					tokenString1Match[i] = true;
				}
			}
		} else {
			// submission 1 is on the pattern
			for (GSTMatchTuple m : matches) {
				for (int i = m.patternPosition; i < m.patternPosition + m.length; i++) {
					tokenString1Match[i] = true;
				}
			}
		}
		// update the mismatches
		int startMismatch = 0;
		for (int i = 0; i < tokenString1Match.length; i++) {
			if (tokenString1Match[i] == true) {
				if (i - startMismatch >= minimumMatchLength) {
					// if length is enough, mark the mismatch array
					// i is excluded as it is a match
					for (int j = startMismatch; j < i; j++) {
						tokenString1Mismatch.get(j).add(submission2ID);
					}
				}

				// update the startMismatch
				while (i < tokenString1Match.length && tokenString1Match[i] == true) {
					i++;
				}
				startMismatch = i;
			}
		}
	}

	public static ArrayList<GSTMatchTuple> generateMatchedTuples(ArrayList<FeedbackToken> tokenString1,
			ArrayList<FeedbackToken> tokenString2, int minimumMatchLength, boolean isSensitive) {
		// create array of string for both whitespace strings
		String[] obj1 = new String[tokenString1.size()];
		String[] obj2 = new String[tokenString2.size()];

		if (isSensitive) {
			// if sensitive mode is applied, use raw tokens
			for (int i = 0; i < tokenString1.size(); i++) {
				obj1[i] = tokenString1.get(i).getContent();
			}
			for (int i = 0; i < tokenString2.size(); i++) {
				obj2[i] = tokenString2.get(i).getContent();
			}
		} else {
			// otherwise, use the generalised one
			for (int i = 0; i < tokenString1.size(); i++) {
				obj1[i] = tokenString1.get(i).getContentForComparison();
			}
			for (int i = 0; i < tokenString2.size(); i++) {
				obj2[i] = tokenString2.get(i).getContentForComparison();
			}
		}
		// get matched tiles with RKRGST to remaining unmatched regions
		ArrayList<GSTMatchTuple> simTuples = GreedyStringTiling.getMatchedTiles(obj1, obj2, minimumMatchLength);

		return simTuples;
	}

}

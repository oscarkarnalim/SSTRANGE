package sstrange;

import java.util.ArrayList;
import java.util.HashMap;

import sstrange.matchgenerator.MatchGenerator;
import sstrange.token.FeedbackToken;
import support.stringmatching.GSTMatchTuple;

public class SimWeighter {

	/*
	 * index lists terms in the collection and how many documents have them. This
	 * field will be assigned with either indexSurface or indexSyntactic. 
	 * n is the total documents
	 */
	private static HashMap<String, Double> index;
	private static int n;
	// bikin mekanisme biar bisa calculate this once for all

	public static double calcIDFWeightedSim(ArrayList<GSTMatchTuple> tiles, ArrayList<FeedbackToken> tokenString1,
			ArrayList<FeedbackToken> tokenString2) {
		// calculate total possible weight for the first token string
		double weighttot1 = 0;
		for (int i = 0; i < tokenString1.size(); i++) {
			if (index == null) {
				weighttot1 += 1;
				continue;
			}

			Double d = index.get(tokenString1.get(i));
			if (d == null)
				d = 0d;
			weighttot1 += ((n + 1) / (d + 1));
		}

		// calculate total possible weight for the second token string
		double weighttot2 = 0;
		for (int i = 0; i < tokenString2.size(); i++) {
			if (index == null) {
				weighttot2 += 1;
				continue;
			}

			Double d = index.get(tokenString2.get(i));
			if (d == null)
				d = 0d;
			weighttot2 += ((n + 1) / (d + 1));
		}

		// count the weight of matched tokens
		double weightmatch1 = 0;
		double weightmatch2 = 0;
		for (int i = 0; i < tiles.size(); i++) {
			GSTMatchTuple tile = tiles.get(i);

			for (int j = tile.patternPosition; j < tile.patternPosition + tile.length; j++) {
				if (index == null) {
					weightmatch1 += 1;
					continue;
				}
				Double d = index.get(tokenString1.get(j));
				if (d == null)
					d = 0d;
				weightmatch1 += ((n + 1) / (d + 1));
			}
			for (int j = tile.textPosition; j < tile.textPosition + tile.length; j++) {
				if (index == null) {
					weightmatch2 += 1;
					continue;
				}
				Double d = index.get(tokenString2.get(j));
				if (d == null)
					d = 0d;
				weightmatch2 += ((n + 1) / (d + 1));
			}
		}

		double similarity = ((weightmatch1 / weighttot1) + (weightmatch2 / weighttot2)) / 2;
		return similarity;
	}

	public static double calcLengthWeightedSim(ArrayList<GSTMatchTuple> tiles, ArrayList<FeedbackToken> tokenString1,
			ArrayList<FeedbackToken> tokenString2) {
		/*
		 * Long similar segments are more obvious
		 */

		double initSim = MatchGenerator.calcAverageSimilarity(tiles, tokenString1.size(), tokenString2.size());

		double similarity = initSim * (1 - tiles.size() * 1.0 / Math.min(tokenString1.size(), tokenString2.size()));
		return similarity;
	}

	public static double calcIDFLengthWeightedSim(ArrayList<GSTMatchTuple> tiles, ArrayList<FeedbackToken> tokenString1,
			ArrayList<FeedbackToken> tokenString2) {
		// combine idf and length weighted at once

		double initSim = calcIDFWeightedSim(tiles, tokenString1, tokenString2);

		double similarity = initSim * (1 - tiles.size() * 1.0 / Math.min(tokenString1.size(), tokenString2.size()));
		return similarity;
	}
}

package sstrange.lshcalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import sstrange.token.FeedbackToken;

public class IndexGenerator {
	public static HashMap<String, ArrayList<Integer>> generateIndex(ArrayList<FeedbackToken> in, int ngram,
			boolean isSensitive) {
		/*
		 * Convert given token tuple list to index-like representation. Return a hash
		 * map with the overlapping n-gram contents as the keys. Per key, the value is a
		 * list of integer representing starting index of that n-gram in the token list.
		 * If it is sensitive mode, return the default content.
		 */

		HashMap<String, ArrayList<Integer>> out = new HashMap<String, ArrayList<Integer>>();
		for (int i = 0; i < in.size() - ngram + 1; i++) {
			// get key as a form of ngram
			String key = "";
			for (int j = 0; j < ngram; j++) {
				if (isSensitive)
					key = key + in.get(i + j).getContent();
				else
					key = key + in.get(i + j).getContentForComparison();
				if (j != ngram - 1)
					key = key + "|";
			}
			key = key + "";

			// get the list of positions
			ArrayList<Integer> positions = out.get(key);
			if (positions == null) {
				// if null, create a new one
				positions = new ArrayList<Integer>();
				out.put(key, positions);
			}
			// add the position
			positions.add(i);
		}

		// return the result
		return out;
	}

	public static ArrayList<String> generateVectorHeader(ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes) {
		// create a hashset to store all unique tokens
		HashSet<String> uniqueTokens = new HashSet<String>();

		// combine all unique tokens
		for (int i = 0; i < tokenIndexes.size(); i++) {
			uniqueTokens.addAll(tokenIndexes.get(i).keySet());
		}

		// turn into arraylist
		return new ArrayList<String>(uniqueTokens);

	}
}

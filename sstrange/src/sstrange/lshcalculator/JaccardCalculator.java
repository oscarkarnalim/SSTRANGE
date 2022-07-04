package sstrange.lshcalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

public class JaccardCalculator {
	public static boolean[] generateBooleanVectorFromTokenString(HashMap<String, ArrayList<Integer>> tokenIndex,
			ArrayList<String> vectorHeader) {
		// generate a boolean vector based on given token index based on vectorheader

		// generate the default vector
		boolean[] vector = new boolean[vectorHeader.size()];

		// convert to true if the header is found in the index
		for (int i = 0; i < vectorHeader.size(); i++) {
			if (tokenIndex.get(vectorHeader.get(i)) != null) {
				vector[i] = true;
			}
		}

		// return the vector
		return vector;
	}

	public static double calculateJaccardSimilarity(HashMap<String, ArrayList<Integer>> tokenIndex1,
			HashMap<String, ArrayList<Integer>> tokenIndex2) {
		/*
		 * calculate jaccard similarity
		 */

		// to store A u B tokens
		HashSet<String> aubmap = new HashSet<String>();

		// calculate A n B
		double anb = 0;
		Iterator<String> it = tokenIndex1.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();

			// if key exist in the counterpart, increment anb
			ArrayList<Integer> as2 = tokenIndex2.get(key);
			if (as2 != null)
				anb++;

			// add to A u B map
			aubmap.add(key);
		}

		// for each key in the second index
		it = tokenIndex2.keySet().iterator();
		while (it.hasNext()) {
			String key = it.next();

			// add to A u B map
			aubmap.add(key);
		}
		
		if(aubmap.size() == 0)
			return 0;
		else return anb*1.0/aubmap.size();
	}
}

package sstrange.lshcalculator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class CosineCalculator {
	public static double[] generateOccurrenceVectorFromTokenString(HashMap<String, ArrayList<Integer>> tokenIndex,
			ArrayList<String> vectorHeader) {
		// generate an occurrence vector based on given token index based on
		// vectorheader

		// generate the default vector
		double[] vector = new double[vectorHeader.size()];

		// set the value if the header is found in the index
		for (int i = 0; i < vectorHeader.size(); i++) {
			ArrayList<Integer> val = tokenIndex.get(vectorHeader.get(i));
			if (val != null) {
				vector[i] = val.size();
			} else {
				vector[i] = 0;
			}
		}

		// return the vector
		return vector;
	}
	
	public static double[] updateOccurrenceVectorFromTokenString(double[] vector, HashMap<String, ArrayList<Integer>> tokenIndex,
			ArrayList<String> vectorHeader) {
		// update vector based on content

		// set the value if the header is found in the index
		for (int i = 0; i < vectorHeader.size(); i++) {
			ArrayList<Integer> val = tokenIndex.get(vectorHeader.get(i));
			if (val != null) {
				vector[i] = val.size();
			} else {
				vector[i] = 0;
			}
		}

		// return the vector
		return vector;
	}

	public static double calculateCosineSimilarity(HashMap<String, ArrayList<Integer>> tokenIndex1,
			HashMap<String, ArrayList<Integer>> tokenIndex2) {
		/*
		 * calculate cosine similarity
		 */

		// calculate dq
		double dq = 0;
		Iterator<Entry<String, ArrayList<Integer>>> it = tokenIndex1.entrySet().iterator();
		while (it.hasNext()) {
			Entry<String, ArrayList<Integer>> entry = it.next();
			String key = entry.getKey();
			ArrayList<Integer> as1 = entry.getValue();
			int s1 = as1.size();
			int s2 = 0;
			ArrayList<Integer> as2 = tokenIndex2.get(key);
			if (as2 != null)
				s2 = as2.size();
			dq += (s1 * s2);
		}

		// calculate d2
		double d2 = 0;
		Iterator<ArrayList<Integer>> iti = tokenIndex1.values().iterator();
		while (iti.hasNext()) {
			Integer val = iti.next().size();
			d2 += (val * val);
		}

		// calculate q2
		double q2 = 0;
		iti = tokenIndex2.values().iterator();
		while (iti.hasNext()) {
			Integer val = iti.next().size();
			q2 += (val * val);
		}

		// pembagi
		double bawah = Math.sqrt(d2 * q2);

		if (bawah == 0)
			return 0;
		else
			return (dq / bawah);
	}

}

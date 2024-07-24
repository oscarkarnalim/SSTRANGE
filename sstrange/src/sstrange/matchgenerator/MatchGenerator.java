package sstrange.matchgenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import sstrange.support.stringmatching.GSTMatchTuple;

public class MatchGenerator {
	public static ArrayList<GSTMatchTuple> generateMatches(HashMap<String, ArrayList<Integer>> tokenIndex1,
			HashMap<String, ArrayList<Integer>> tokenIndex2, int ngram) {
		// generate matches based on two indexes

		ArrayList<GSTMatchTuple> matches = new ArrayList<GSTMatchTuple>();

		Iterator<Entry<String, ArrayList<Integer>>> it = tokenIndex1.entrySet().iterator();
		while (it.hasNext()) {
			// for each key, search on tokenindex2
			Entry<String, ArrayList<Integer>> entry = it.next();
			String key = entry.getKey();
			ArrayList<Integer> positions1 = entry.getValue();
			// get the second list from tokenindex2
			ArrayList<Integer> positions2 = tokenIndex2.get(key);
			// if none, skip to next iteration
			if (positions2 == null)
				continue;

			// get all shared tokens as matches
			for (int i = 0; i < positions1.size() && i < positions2.size(); i++) {
				GSTMatchTuple m = new GSTMatchTuple(positions1.get(i), positions2.get(i), ngram);
				matches.add(m);
			}
		}

		// merge overlapping matches
		// conditions:
		// 1. AABB -> automatically handled by STRANGE
		// 2. AA
		//     BB
		// 3. Reverse of 1 -> automatically handled by STRANGE
		// 4. Reverse of 2
		// Other than these cases, nullify the shortest.
		for (int i = 0; i < matches.size(); i++) {
			GSTMatchTuple m = matches.get(i);
			int mtstart = m.textPosition;
			int mtfinish = m.textPosition + m.length;
			int mpstart = m.patternPosition;
			int mpfinish = m.patternPosition + m.length;
			for (int j = i + 1; j < matches.size(); j++) {
				GSTMatchTuple n = matches.get(j);
				int ntstart = n.textPosition;
				int ntfinish = n.textPosition + n.length;
				int npstart = n.patternPosition;
				int npfinish = n.patternPosition + n.length;

				if (mtstart <= ntstart && ntstart <= mtfinish) {
					// Case 1: N starts in between M for text

					if (mpstart <= npstart && npstart <= mpfinish && ntstart-mtstart == npstart - mtstart) {
						// if that also happens on pattern with relative position of n toward m is the same
						
						// calculate new start and finish
						int otstart = mtstart;
						int otfinish = Math.max(mtfinish, ntfinish);
						int opstart = mpstart;
						
						// set it to n
						n.textPosition = otstart;
						n.patternPosition = opstart;
						n.length = otfinish - otstart;
						
						// remove m
						matches.remove(i);
						// decrement i by one
						i--;
					}else {
						// overlap in text but not in pattern
						
						// set N attributes with M if M is longer
						if(n.length < m.length) {
							n.textPosition = m.textPosition;
							n.patternPosition = m.patternPosition;
							n.length = m.length;
						}
						
						// remove m
						matches.remove(i);
						// decrement i by one
						i--;
					}
					
					// get out from j loop
					break;
				}else if (ntstart <= mtstart && mtstart <= ntfinish) {
					// Case 2: M starts in between M for text
					
					if (npstart <= mpstart && mpstart <= npfinish && ntstart-mtstart == npstart - mtstart) {
						// if that also happens on pattern with relative position of n toward m is the same
						
						// calculate new start and finish
						int otstart = ntstart;
						int otfinish = Math.max(mtfinish, ntfinish);
						int opstart = npstart;
						
						// set it to n
						n.textPosition = otstart;
						n.patternPosition = opstart;
						n.length = otfinish - otstart;
						
						// remove m
						matches.remove(i);
						// decrement i by one
						i--;
					}else {
						// overlap in text but not in pattern
						
						// set N attributes with M if M is longer
						if(n.length < m.length) {
							n.textPosition = m.textPosition;
							n.patternPosition = m.patternPosition;
							n.length = m.length;
						}
						
						// remove m
						matches.remove(i);
						// decrement i by one
						i--;
					}
					
					// get out from j loop
					break;
				}else if((mpstart <= npstart && npstart <= mpfinish) || (npstart <= mpstart && mpstart <= npfinish)) {
					// overlap in pattern but not in text
					
					// set N attributes with M if M is longer
					if(n.length < m.length) {
						n.textPosition = m.textPosition;
						n.patternPosition = m.patternPosition;
						n.length = m.length;
					}
					
					// remove m
					matches.remove(i);
					// decrement i by one
					i--;
					
					// get out from j loop
					break;
				}

			}
		}

		return matches;
	}
	
	// return average similarity based on the tiles
	public static double calcAverageSimilarity(ArrayList<GSTMatchTuple> tiles, int tokenSize1, int tokenSize2) {
		double similarity = ((double) (2 * coverage(tiles)) / (double) (tokenSize1 + tokenSize2));
		return similarity;
	}
	
	// generate total number of matched tokens
	public static int coverage(ArrayList<GSTMatchTuple> tiles) {
		int accu = 0;
		for (int i = 0; i < tiles.size(); i++) {
			GSTMatchTuple tile = tiles.get(i);
			accu += tile.length;
		}
		return accu;
	}
}

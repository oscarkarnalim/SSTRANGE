package sstrange.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Stack;

import info.debatty.java.lsh.LSHMinHash;
import sstrange.language.dart.DartFeedbackGenerator;
import sstrange.lshcalculator.IndexGenerator;
import sstrange.lshcalculator.JaccardCalculator;
import sstrange.matchgenerator.MatchGenerator;
import sstrange.support.stringmatching.GSTMatchTuple;
import sstrange.token.FeedbackToken;

public class DartEvaluation {
	public static void main(String[] args) {
		int minMatchLength = 10;
		String assessmentPath = "C:\\Users\\oscar\\OneDrive\\Desktop\\Dart Dataset\\Mobile Programming\\Dataset\\UTS";
		File[] copied = new File(assessmentPath + "\\copied").listFiles();
		File[] orig = new File(assessmentPath + "\\orig").listFiles();

		long before = System.nanoTime();
		for (File c : copied) {
			if (c.isDirectory() == false)
				continue;

			// for each copied
			String cName = c.getName().substring(4);
			System.out.println(cName);
			ArrayList<FeedbackToken> cTokenString = generateTokenString(c);
			// System.out.println(cTokenString.size());

			ArrayList<String> submissionIDs = new ArrayList<>();
			submissionIDs.add(cName);
			ArrayList<ArrayList<FeedbackToken>> tokenStrings = new ArrayList<>();
			tokenStrings.add(cTokenString);
			ArrayList<HashMap<String, ArrayList<Integer>>> tokenIndexes = new ArrayList<HashMap<String, ArrayList<Integer>>>();
			tokenIndexes.add(IndexGenerator.generateIndex(cTokenString, minMatchLength, false));

			ArrayList<PairTuple> arr = new ArrayList<>();
			for (int i = 0; i < orig.length; i++) {
				File o = orig[i];
				if (o.isDirectory() == false)
					continue;

				// compare that with each orig
				String oName = o.getName().substring(4);
				ArrayList<FeedbackToken> oTokenString = generateTokenString(o);

				// add the submission name, the token string, and the token index
				submissionIDs.add(oName);
				tokenStrings.add(oTokenString);
				tokenIndexes.add(IndexGenerator.generateIndex(oTokenString, minMatchLength, false));
			}

			// generate vector header
			ArrayList<String> vectorHeader = IndexGenerator.generateVectorHeader(tokenIndexes);
			// generate Jaccard vectors
			ArrayList<boolean[]> tokenVectors = new ArrayList<boolean[]>();
			for (int i = 0; i < tokenIndexes.size(); i++) {
				tokenVectors
						.add(JaccardCalculator.generateBooleanVectorFromTokenString(tokenIndexes.get(i), vectorHeader));
			}

			// create MinHash object with best default setting
			int lshn = vectorHeader.size();
			LSHMinHash lsh = new LSHMinHash(1, 2, lshn);
			// store all the hashes (all stages)
			int[][] lshHashes = new int[tokenVectors.size()][];
			for (int i = 0; i < tokenVectors.size(); i++) {
				lshHashes[i] = lsh.hash(tokenVectors.get(i));
			}

			// to store the pairs and their occurrences
			HashMap<String, Integer> similarPairs = new HashMap<String, Integer>();

			// for each vector, check each stage and mark all programs fall to the same
			// bucket at least once
			for (int i = 0; i < tokenVectors.size(); i++) {
				// for each stage, mark all programs fall to the same bucket
				for (int j = 0; j < lshHashes[i].length; j++) {
					// search the counterpart of i
					for (int k = i + 1; k < tokenVectors.size(); k++) {
						if (lshHashes[i][j] == lshHashes[k][j]) {
							// if the hash is similar, put that on similar pairs

							// set the key
							String key = k + " " + i;
							if (i < k)
								key = i + " " + k;

							// get current value if any
							Integer val = similarPairs.get(key);
							if (val == null)
								val = 0;

							// update the new value
							similarPairs.put(key, val + 1);
						}
					}
				}
			}

			// for each similar pair with given query, do the comparison
			Iterator<Entry<String, Integer>> itSimilarPairs = similarPairs.entrySet().iterator();
			while (itSimilarPairs.hasNext()) {
				Entry<String, Integer> en = itSimilarPairs.next();

				String[] cur = en.getKey().split(" ");

				// get the pair
				int submissionID1 = Integer.parseInt(cur[0]);
				int submissionID2 = Integer.parseInt(cur[1]);

				HashMap<String, ArrayList<Integer>> tokenIndex1 = tokenIndexes.get(submissionID1);
				HashMap<String, ArrayList<Integer>> tokenIndex2 = tokenIndexes.get(submissionID2);

				// generate the matches
				ArrayList<GSTMatchTuple> matches = MatchGenerator.generateMatches(tokenIndex1, tokenIndex2,
						minMatchLength);

				// get the sim degree
				int simDegree = (int) (MatchGenerator.calcAverageSimilarity(matches,
						tokenStrings.get(submissionID1).size(), tokenStrings.get(submissionID2).size()) * 100);

				// add the comparison pair
				PairTuple pt = new PairTuple(simDegree, submissionIDs.get(submissionID1),
						submissionIDs.get(submissionID2));
				arr.add(pt);
			}
			Collections.sort(arr);
			int posCopied = -1;
			for (int i = 0; i < arr.size(); i++) {
				System.out.println(arr.get(i));
				if (arr.get(i).name1.startsWith(arr.get(i).name2) || arr.get(i).name2.startsWith(arr.get(i).name1))
					posCopied = i;
			}
			if (posCopied != -1)
				System.out.println("precision\t\t\t" + 1.0 / (posCopied + 1));
			else
				System.out.println("precision\t\t\t0");
		}
		
		System.out.println("Time\t" + (System.nanoTime()- before));

	}

	public static ArrayList<FeedbackToken> generateTokenString(File f) {
		// generate token string based on given filepath
		ArrayList<FeedbackToken> tokenString = new ArrayList<>();

		// iterate each file inside
		Stack<File> s = new Stack<>();
		s.push(f);
		while (s.isEmpty() == false) {
			File c = s.pop();
			if (c.isDirectory()) {
				// if directory, add files contained from that directory
				File[] children = c.listFiles();
				for (File ch : children) {
					s.push(ch);
				}
			} else if (c.getName().endsWith(".dart")) {
				// if it is a dart file, preprocess and take the content
				ArrayList<FeedbackToken> localTokenString = DartFeedbackGenerator
						.generateFeedbackTokenString(c.getAbsolutePath());
				localTokenString = DartFeedbackGenerator.syntaxTokenStringPreprocessing(localTokenString);
				// add to the global token string
				tokenString.addAll(localTokenString);
			}
		}
		return tokenString;
	}

}

class PairTuple implements Comparable<PairTuple> {
	double simDegree;
	String name1, name2;

	public PairTuple(double simDegree, String name1, String name2) {
		super();
		this.simDegree = simDegree;
		this.name1 = name1;
		this.name2 = name2;
	}

	@Override
	public int compareTo(PairTuple o) {
		// TODO Auto-generated method stub
		return (int) ((o.simDegree - simDegree) * 100000);
	}

	public String toString() {
		return name1 + "\t" + name2 + "\t" + simDegree;
	}
}
